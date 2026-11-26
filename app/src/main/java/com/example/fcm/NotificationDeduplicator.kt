package com.example.fcm

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.OrderStatus
import org.json.JSONObject

class NotificationDeduplicator private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val cache = DeduplicationCache(maxEntries = 150, ttlMs = 10 * 60 * 1000L)

    init {
        loadFromPrefs()
    }

    @Synchronized
    fun shouldProcessPush(message: ParsedPushMessage): Boolean {
        val accepted = cache.checkAndMark(DeduplicationCache.keysForPush(message))
        if (accepted) persistLocked()
        return accepted
    }

    @Synchronized
    fun shouldProcessOrderAlert(orderId: String, status: OrderStatus, notifId: String = ""): Boolean {
        val keys = buildList {
            addAll(DeduplicationCache.keysForOrderStatus(orderId, status))
            if (notifId.isNotBlank()) add("notif:$notifId")
        }
        val accepted = cache.checkAndMark(keys)
        if (accepted) persistLocked()
        return accepted
    }

    @Synchronized
    fun markOrderStatusHandled(orderId: String, status: OrderStatus) {
        val keys = DeduplicationCache.keysForOrderStatus(orderId, status)
        if (keys.isNotEmpty()) {
            cache.checkAndMark(keys)
            persistLocked()
        }
    }

    @Synchronized
    fun shouldProcessNotificationId(notifId: String): Boolean {
        val clean = notifId.trim()
        if (clean.isBlank()) return true
        val accepted = cache.checkAndMark(listOf("notif:$clean"))
        if (accepted) persistLocked()
        return accepted
    }

    @Synchronized
    fun markNotificationHandled(notifId: String) {
        val clean = notifId.trim()
        if (clean.isNotBlank()) {
            cache.checkAndMark(listOf("notif:$clean"))
            persistLocked()
        }
    }

    @Synchronized
    fun clearAll() {
        prefs.edit().remove(KEY_CACHE_JSON).apply()
    }

    private fun loadFromPrefs() {
        val raw = prefs.getString(KEY_CACHE_JSON, null) ?: return
        try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, Long>()
            val iter = obj.keys()
            while (iter.hasNext()) {
                val k = iter.next()
                map[k] = obj.optLong(k, 0L)
            }
            cache.restore(map)
        } catch (_: Exception) {
        }
    }

    private fun persistLocked() {
        try {
            val obj = JSONObject()
            cache.snapshot().forEach { (k, v) -> obj.put(k, v) }
            prefs.edit().putString(KEY_CACHE_JSON, obj.toString()).apply()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val PREFS_NAME = "lakemark_push_dedup"
        private const val KEY_CACHE_JSON = "dedup_entries_json"

        @Volatile
        private var instance: NotificationDeduplicator? = null

        fun getInstance(context: Context): NotificationDeduplicator =
            instance ?: synchronized(this) {
                instance ?: NotificationDeduplicator(context.applicationContext).also { instance = it }
            }
    }
}
