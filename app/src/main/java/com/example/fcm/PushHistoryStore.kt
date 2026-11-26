package com.example.fcm

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppNotification
import com.example.data.model.BrandConfig
import com.example.data.model.UserRole
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID

sealed interface FcmRegistrationState {
    data object Idle : FcmRegistrationState
    data object Syncing : FcmRegistrationState
    data class Active(val isFirestoreSynced: Boolean, val topics: Set<String>) : FcmRegistrationState
    data object PermissionDenied : FcmRegistrationState
    data class Error(val message: String) : FcmRegistrationState
}

data class FcmRegistrationInfo(
    val deviceId: String = "",
    val token: String = "",
    val activeRole: UserRole = UserRole.CUSTOMER,
    val subscribedTopics: Set<String> = emptySet(),
    val state: FcmRegistrationState = FcmRegistrationState.Idle,
    val lastSyncedAtMs: Long = 0L,
) {
    val hasToken: Boolean get() = token.isNotBlank()
}

data class StoredPushEntry(
    val notification: AppNotification,
    val brandConfig: BrandConfig,
)

class PushHistoryStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _history = MutableStateFlow<List<StoredPushEntry>>(emptyList())
    val historyEntries: StateFlow<List<StoredPushEntry>> = _history.asStateFlow()

    private val _registrationInfo = MutableStateFlow(FcmRegistrationInfo(deviceId = getOrCreateDeviceId()))
    val registrationInfo: StateFlow<FcmRegistrationInfo> = _registrationInfo.asStateFlow()

    init {
        loadState()
    }

    @Synchronized
    fun getOrCreateDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "android_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    @Synchronized
    fun setSessionContext(uid: String, role: UserRole?) {
        prefs.edit()
            .putString(KEY_ACTIVE_UID, uid)
            .putString(KEY_ACTIVE_ROLE, role?.key ?: "")
            .apply()
        if (role != null) {
            _registrationInfo.value = _registrationInfo.value.copy(activeRole = role)
        }
    }

    @Synchronized
    fun getActiveUid(): String = prefs.getString(KEY_ACTIVE_UID, "").orEmpty()

    @Synchronized
    fun getActiveRole(): UserRole? {
        val raw = prefs.getString(KEY_ACTIVE_ROLE, "").orEmpty()
        return if (raw.isBlank()) null else UserRole.fromKey(raw)
    }

    @Synchronized
    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        _registrationInfo.value = _registrationInfo.value.copy(token = token)
    }

    @Synchronized
    fun getSavedFcmToken(): String = prefs.getString(KEY_FCM_TOKEN, "").orEmpty()

    @Synchronized
    fun saveSubscribedTopics(topics: Set<String>) {
        prefs.edit().putStringSet(KEY_SUBSCRIBED_TOPICS, topics).apply()
        _registrationInfo.value = _registrationInfo.value.copy(subscribedTopics = topics)
    }

    @Synchronized
    fun getSubscribedTopics(): Set<String> =
        prefs.getStringSet(KEY_SUBSCRIBED_TOPICS, emptySet())?.toSet() ?: emptySet()

    fun updateRegistrationState(
        state: FcmRegistrationState,
        syncedAtMs: Long? = null,
        topics: Set<String>? = null,
    ) {
        val current = _registrationInfo.value
        _registrationInfo.value = current.copy(
            state = state,
            lastSyncedAtMs = syncedAtMs ?: current.lastSyncedAtMs,
            subscribedTopics = topics ?: current.subscribedTopics,
        )
    }

    @Synchronized
    fun appendPushMessage(message: ParsedPushMessage) {
        val notif = message.toAppNotification()
        val entry = StoredPushEntry(
            notification = notif,
            brandConfig = message.senderBrandConfig(),
        )
        val current = _history.value.toMutableList()
        current.removeAll { it.notification.id == notif.id }
        current.add(0, entry)
        val trimmed = current.take(MAX_HISTORY)
        _history.value = trimmed
        persistHistoryLocked(trimmed)
    }

    @Synchronized
    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove(KEY_HISTORY_JSON).apply()
    }

    private fun loadState() {
        val token = prefs.getString(KEY_FCM_TOKEN, "").orEmpty()
        val topics = prefs.getStringSet(KEY_SUBSCRIBED_TOPICS, emptySet())?.toSet() ?: emptySet()
        val activeRole = getActiveRole() ?: UserRole.CUSTOMER
        _registrationInfo.value = FcmRegistrationInfo(
            deviceId = getOrCreateDeviceId(),
            token = token,
            activeRole = activeRole,
            subscribedTopics = topics,
            state = if (token.isNotBlank()) FcmRegistrationState.Active(isFirestoreSynced = false, topics = topics) else FcmRegistrationState.Idle,
        )

        val rawHistory = prefs.getString(KEY_HISTORY_JSON, null) ?: return
        try {
            val arr = JSONArray(rawHistory)
            val loaded = mutableListOf<StoredPushEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val targetsArr = obj.optJSONArray("targetRoles")
                val targets = buildList {
                    if (targetsArr != null) {
                        for (j in 0 until targetsArr.length()) {
                            add(targetsArr.optString(j))
                        }
                    }
                }.ifEmpty { listOf("all") }

                val senderRole = UserRole.fromKey(obj.optString("senderRole", "manager"))
                val defBrand = senderRole.defaultBrandConfig()
                val notif = AppNotification(
                    id = obj.optString("id"),
                    senderUid = obj.optString("senderUid"),
                    senderRole = senderRole.key,
                    senderBrandName = obj.optString("senderBrandName", senderRole.displayName),
                    message = obj.optString("message"),
                    targetRoles = targets,
                    targetUserId = obj.optString("targetUserId"),
                    orderId = obj.optString("orderId"),
                    kind = obj.optString("kind", "ping"),
                    createdAt = Timestamp(Date(obj.optLong("createdAtMs", System.currentTimeMillis()))),
                )
                val brand = BrandConfig(
                    roleKey = senderRole.key,
                    primaryHex = obj.optString("primaryHex", defBrand.primaryHex),
                    secondaryHex = obj.optString("secondaryHex", defBrand.secondaryHex),
                    accentHex = obj.optString("accentHex", defBrand.accentHex),
                    brandName = notif.senderBrandName.take(18),
                    mark = obj.optString("mark", ""),
                )
                loaded.add(StoredPushEntry(notification = notif, brandConfig = brand))
            }
            _history.value = loaded
        } catch (_: Exception) {
        }
    }

    private fun persistHistoryLocked(entries: List<StoredPushEntry>) {
        try {
            val arr = JSONArray()
            entries.forEach { (n, b) ->
                arr.put(
                    JSONObject().apply {
                        put("id", n.id)
                        put("senderUid", n.senderUid)
                        put("senderRole", n.senderRole)
                        put("senderBrandName", n.senderBrandName)
                        put("message", n.message)
                        put("targetRoles", JSONArray(n.targetRoles))
                        put("targetUserId", n.targetUserId)
                        put("orderId", n.orderId)
                        put("kind", n.kind)
                        put("createdAtMs", n.createdAt?.toDate()?.time ?: System.currentTimeMillis())
                        put("primaryHex", b.primaryHex)
                        put("secondaryHex", b.secondaryHex)
                        put("accentHex", b.accentHex)
                        put("mark", b.mark)
                    }
                )
            }
            prefs.edit().putString(KEY_HISTORY_JSON, arr.toString()).apply()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val PREFS_NAME = "lakemark_push_store"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ACTIVE_UID = "active_uid"
        private const val KEY_ACTIVE_ROLE = "active_role"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_SUBSCRIBED_TOPICS = "subscribed_topics"
        private const val KEY_HISTORY_JSON = "push_history_json"
        private const val MAX_HISTORY = 40

        @Volatile
        private var instance: PushHistoryStore? = null

        fun getInstance(context: Context): PushHistoryStore =
            instance ?: synchronized(this) {
                instance ?: PushHistoryStore(context.applicationContext).also { instance = it }
            }
    }
}
