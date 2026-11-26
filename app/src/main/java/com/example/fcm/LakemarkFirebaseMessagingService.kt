package com.example.fcm

import android.util.Log
import com.example.LakemarkApplication
import com.example.data.preferences.UserAlertPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.util.AlertAudioAndNotificationManager
import com.example.util.AlertSoundKind
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LakemarkFirebaseMessagingService : FirebaseMessagingService() {

    private val fallbackScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "LakemarkFcmService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM token refreshed")
        val tokenManager = FcmTokenManager(applicationContext)
        val appScope = (applicationContext as? LakemarkApplication)?.applicationScope ?: fallbackScope
        appScope.launch {
            tokenManager.onTokenRefreshed(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val parsed = FcmPayloadParser.parse(
            data = remoteMessage.data,
            fallbackTitle = remoteMessage.notification?.title,
            fallbackBody = remoteMessage.notification?.body,
            fcmMessageId = remoteMessage.messageId.orEmpty(),
            sentTimeMs = remoteMessage.sentTime,
        ) ?: run {
            Log.w(TAG, "Ignored empty FCM message (${remoteMessage.messageId})")
            return
        }

        val historyStore = PushHistoryStore.getInstance(applicationContext)
        val activeUid = historyStore.getActiveUid().ifBlank {
            Firebase.auth.currentUser?.uid.orEmpty()
        }
        val activeRole = historyStore.getActiveRole()

        if (!PushRoutingPolicy.shouldDeliverToSession(parsed, activeUid, activeRole)) {
            Log.d(TAG, "Filtered push notif=${parsed.notifId} not targeted for uid=$activeUid role=${activeRole?.key}")
            return
        }

        val deduplicator = NotificationDeduplicator.getInstance(applicationContext)
        if (!deduplicator.shouldProcessPush(parsed)) {
            Log.d(TAG, "Deduplicated push notif=${parsed.notifId} order=${parsed.orderId}:${parsed.orderStatus}")
            return
        }

        // Persist immediately so NotificationsSheet and unread badge include background/terminated pushes.
        historyStore.appendPushMessage(parsed)

        if (AppForegroundTracker.isAppInForeground()) {
            InAppPushCoordinator.emitForegroundPush(parsed)
        } else {
            val prefs = runCatching {
                runBlocking { UserPreferencesRepository(applicationContext).preferences.firstOrNull() }
            }.getOrNull() ?: UserAlertPreferences()

            val alertManager = AlertAudioAndNotificationManager(applicationContext)
            val posted = alertManager.showBrandedPushNotification(parsed)
            if (!posted) {
                Log.w(TAG, "System notification not posted: ${alertManager.lastNotificationError}")
            }

            val channelHasSound = posted && alertManager.isChannelSoundEnabled(parsed.channelCategory.channelId)
            val soundKind = when (parsed.kind) {
                "new" -> AlertSoundKind.NEW_ORDER
                "ready" -> AlertSoundKind.ORDER_READY
                else -> AlertSoundKind.PING
            }
            if ((prefs.soundEnabled && !channelHasSound) || (prefs.vibrationEnabled && !posted)) {
                alertManager.triggerAlert(
                    kind = soundKind,
                    soundEnabled = prefs.soundEnabled && !channelHasSound,
                    vibrationEnabled = prefs.vibrationEnabled && !posted,
                    volume = prefs.volume,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fallbackScope.cancel()
    }
}
