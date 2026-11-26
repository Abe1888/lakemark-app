package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lakemark_prefs")

data class UserAlertPreferences(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val volume: Float = 0.8f,
    val preferredRoleKey: String = "manager",
    val lastNotifReadAtMs: Long = 0L,
    val onboardingSeen: Boolean = false,
    val languageCode: String = "en",
    val cachedTranslationsJson: String = "",
)

class UserPreferencesRepository(private val context: Context) {

    private val quickPrefs = context.applicationContext.getSharedPreferences(
        "lakemark_quick_prefs",
        Context.MODE_PRIVATE,
    )

    private object Keys {
        val SOUND = booleanPreferencesKey("sound_enabled")
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
        val VOLUME = floatPreferencesKey("alert_volume")
        val PREFERRED_ROLE = stringPreferencesKey("preferred_role")
        val LAST_NOTIF_READ_MS = longPreferencesKey("last_notif_read_ms")
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val CACHED_TRANSLATIONS_JSON = stringPreferencesKey("cached_translations_json")
    }

    val preferences: Flow<UserAlertPreferences> = context.dataStore.data.map { prefs ->
        val lang = prefs[Keys.LANGUAGE_CODE]
            ?: quickPrefs.getString("language_code", "en")
            ?: "en"
        UserAlertPreferences(
            soundEnabled = prefs[Keys.SOUND] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION] ?: true,
            volume = prefs[Keys.VOLUME] ?: 0.8f,
            preferredRoleKey = prefs[Keys.PREFERRED_ROLE] ?: "manager",
            lastNotifReadAtMs = prefs[Keys.LAST_NOTIF_READ_MS] ?: 0L,
            onboardingSeen = prefs[Keys.ONBOARDING_SEEN] ?: false,
            languageCode = lang,
            cachedTranslationsJson = prefs[Keys.CACHED_TRANSLATIONS_JSON].orEmpty(),
        )
    }

    fun getQuickLanguageCode(): String =
        quickPrefs.getString("language_code", "en") ?: "en"

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION] = enabled }
    }

    suspend fun setVolume(volume: Float) {
        context.dataStore.edit { it[Keys.VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setPreferredRoleKey(roleKey: String) {
        context.dataStore.edit { it[Keys.PREFERRED_ROLE] = roleKey }
    }

    suspend fun setLastNotifReadAtMs(timestampMs: Long) {
        context.dataStore.edit { it[Keys.LAST_NOTIF_READ_MS] = timestampMs }
    }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_SEEN] = seen }
    }

    suspend fun setLanguageCode(languageCode: String) {
        val clean = if (languageCode.equals("am", ignoreCase = true)) "am" else "en"
        quickPrefs.edit().putString("language_code", clean).apply()
        context.dataStore.edit { it[Keys.LANGUAGE_CODE] = clean }
    }

    suspend fun setCachedTranslationsJson(json: String) {
        context.dataStore.edit { it[Keys.CACHED_TRANSLATIONS_JSON] = json }
    }
}

