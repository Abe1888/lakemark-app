package com.example.fcm

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.UserRole
import com.example.data.repository.LakemarkRepository
import com.example.util.AlertAudioAndNotificationManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FcmTokenManager(
    context: Context,
    private val repository: LakemarkRepository = LakemarkRepository(context.applicationContext),
) {
    private val appContext = context.applicationContext
    private val historyStore = PushHistoryStore.getInstance(appContext)
    private val alertManager = AlertAudioAndNotificationManager(appContext)

    companion object {
        private const val TAG = "FcmTokenManager"
    }

    suspend fun syncSessionPushRegistration(
        uid: String,
        role: UserRole,
        isFirebaseUser: Boolean,
    ): Result<String> {
        historyStore.setSessionContext(uid = uid, role = role)
        historyStore.updateRegistrationState(FcmRegistrationState.Syncing)

        val hasPermission = alertManager.hasNotificationPermission()
        val token = fetchTokenWithRetry().getOrElse { err ->
            val msg = err.localizedMessage ?: "Unable to fetch FCM token"
            historyStore.updateRegistrationState(FcmRegistrationState.Error(msg))
            return Result.failure(err)
        }

        historyStore.saveFcmToken(token)

        val desiredTopics = PushRoutingPolicy.topicsForSession(uid = uid, role = role)
        val previousTopics = historyStore.getSubscribedTopics()
        val topicErrors = syncTopics(previousTopics = previousTopics, desiredTopics = desiredTopics)
        if (topicErrors.isEmpty()) {
            historyStore.saveSubscribedTopics(desiredTopics)
        }

        var firestoreSynced = false
        if (isFirebaseUser && Firebase.auth.currentUser != null) {
            val deviceId = historyStore.getOrCreateDeviceId()
            val syncRes = repository.upsertFcmToken(
                deviceId = deviceId,
                token = token,
                role = role,
                appVersion = BuildConfig.VERSION_NAME,
                notificationsEnabled = hasPermission,
            )
            if (syncRes.isSuccess) {
                firestoreSynced = true
            } else {
                val errMsg = syncRes.exceptionOrNull()?.localizedMessage ?: "Token sync failed"
                historyStore.updateRegistrationState(FcmRegistrationState.Error(errMsg), topics = desiredTopics)
                return Result.failure(syncRes.exceptionOrNull() ?: IllegalStateException(errMsg))
            }
        }

        if (!hasPermission) {
            historyStore.updateRegistrationState(FcmRegistrationState.PermissionDenied, topics = desiredTopics)
        } else if (topicErrors.isNotEmpty()) {
            historyStore.updateRegistrationState(
                FcmRegistrationState.Error(topicErrors.first()),
                topics = desiredTopics,
            )
        } else {
            historyStore.updateRegistrationState(
                state = FcmRegistrationState.Active(isFirestoreSynced = firestoreSynced, topics = desiredTopics),
                syncedAtMs = System.currentTimeMillis(),
                topics = desiredTopics,
            )
        }

        return Result.success(token)
    }

    suspend fun onTokenRefreshed(newToken: String) {
        val clean = newToken.trim()
        if (clean.isBlank()) return
        historyStore.saveFcmToken(clean)
        val firebaseUid = Firebase.auth.currentUser?.uid.orEmpty()
        val uid = historyStore.getActiveUid().ifBlank { firebaseUid }
        if (uid.isBlank()) {
            Log.d(TAG, "Deferred refreshed FCM token sync until user signs in")
            return
        }
        val role = historyStore.getActiveRole() ?: UserRole.CUSTOMER
        val topics = PushRoutingPolicy.topicsForSession(uid = uid, role = role)
        syncTopics(previousTopics = historyStore.getSubscribedTopics(), desiredTopics = topics)
        historyStore.saveSubscribedTopics(topics)

        if (firebaseUid.isNotBlank()) {
            val deviceId = historyStore.getOrCreateDeviceId()
            val res = repository.upsertFcmToken(
                deviceId = deviceId,
                token = clean,
                role = role,
                appVersion = BuildConfig.VERSION_NAME,
                notificationsEnabled = alertManager.hasNotificationPermission(),
            )
            if (res.isSuccess) {
                historyStore.updateRegistrationState(
                    FcmRegistrationState.Active(isFirestoreSynced = true, topics = topics),
                    syncedAtMs = System.currentTimeMillis(),
                    topics = topics,
                )
            }
        }
    }

    suspend fun unregisterOnSignOut(expectedFirebaseUid: String? = null) {
        withContext(NonCancellable) {
            val existingTopics = historyStore.getSubscribedTopics()
            existingTopics.forEach { topic ->
                try {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to unsubscribe topic $topic on sign-out: ${e.message}")
                }
            }
            historyStore.saveSubscribedTopics(emptySet())

            val fbUid = expectedFirebaseUid?.ifBlank { null } ?: Firebase.auth.currentUser?.uid
            if (!fbUid.isNullOrBlank()) {
                repository.removeFcmToken(
                    deviceId = historyStore.getOrCreateDeviceId(),
                    expectedUid = fbUid,
                )
            }
            historyStore.setSessionContext(uid = "", role = null)
            historyStore.updateRegistrationState(FcmRegistrationState.Idle, topics = emptySet())
        }
    }

    private suspend fun fetchTokenWithRetry(): Result<String> {
        var lastError: Exception? = null
        val backoffs = listOf(0L, 600L, 1500L)
        for (waitMs in backoffs) {
            if (waitMs > 0L) delay(waitMs)
            try {
                val token = FirebaseMessaging.getInstance().token.await().trim()
                if (token.isNotBlank()) return Result.success(token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "FCM token attempt failed: ${e.message}")
            }
        }
        return Result.failure(lastError ?: IllegalStateException("Empty FCM token returned"))
    }

    private suspend fun syncTopics(
        previousTopics: Set<String>,
        desiredTopics: Set<String>,
    ): List<String> {
        val errors = mutableListOf<String>()
        val messaging = FirebaseMessaging.getInstance()

        (previousTopics - desiredTopics).forEach { obsolete ->
            try {
                messaging.unsubscribeFromTopic(obsolete).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Topic unsubscribe warning ($obsolete): ${e.message}")
            }
        }

        desiredTopics.forEach { topic ->
            try {
                messaging.subscribeToTopic(topic).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Topic subscribe failed ($topic): ${e.message}")
                errors.add("Topic $topic sync failed")
            }
        }
        return errors
    }
}
