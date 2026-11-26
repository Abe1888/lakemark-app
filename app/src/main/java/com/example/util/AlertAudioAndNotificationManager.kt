package com.example.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.BrandConfig
import com.example.data.model.UserRole
import com.example.data.preferences.UserPreferencesRepository
import com.example.fcm.AppForegroundTracker
import com.example.fcm.ParsedPushMessage
import com.example.fcm.PushChannelCategory
import com.example.fcm.PushDeepLink
import com.example.localization.AppLanguage
import com.example.localization.TranslationCatalog
import com.example.ui.theme.deriveBrandPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

enum class AlertSoundKind { NEW_ORDER, ORDER_READY, PING }

data class NotificationSystemStatus(
    val hasPermission: Boolean = false,
    val notificationsEnabledInSystem: Boolean = false,
    val kitchenChannelSoundEnabled: Boolean = false,
    val readyChannelSoundEnabled: Boolean = false,
    val broadcastChannelSoundEnabled: Boolean = false,
    val deviceMediaMuted: Boolean = false,
    val deviceNotificationMuted: Boolean = false,
) {
    val allChannelsSoundEnabled: Boolean
        get() = kitchenChannelSoundEnabled && readyChannelSoundEnabled && broadcastChannelSoundEnabled
}

class AlertAudioAndNotificationManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var lastNotificationError: String? = null
        private set

    companion object {
        const val CHANNEL_ID = "lakemark_kitchen_alerts_v3"
        const val CHANNEL_READY_ID = "lakemark_ready_alerts_v3"
        const val CHANNEL_BROADCAST_ID = "lakemark_broadcast_alerts_v3"
        private val LEGACY_CHANNEL_IDS = listOf(
            "lakemark_kitchen_alerts",
            "lakemark_ready_alerts",
            "lakemark_broadcast_alerts",
            "lakemark_kitchen_alerts_v2",
            "lakemark_ready_alerts_v2",
            "lakemark_broadcast_alerts_v2",
        )
        private const val DEFAULT_SAMPLE_RATE = 44100
        private const val TAG = "LakemarkAlertMgr"
        private val chimeMutex = Mutex()

        /**
         * Resolves the AudioAttributes usage for in-app alert chimes.
         * In foreground, prefers USAGE_MEDIA (STREAM_MUSIC) whenever media volume is audible
         * or notification/ringer volume is suppressed (common on MIUI/HyperOS devices where
         * notification stream is silent even while Alert Sound is enabled in-app).
         */
        fun selectAlertAudioUsage(
            isForeground: Boolean,
            mediaVolume: Int,
            notificationVolume: Int,
            ringerMode: Int,
        ): Int {
            return if (isForeground) {
                if (mediaVolume > 0 || notificationVolume <= 0 || ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                    AudioAttributes.USAGE_MEDIA
                } else {
                    AudioAttributes.USAGE_NOTIFICATION
                }
            } else {
                if (ringerMode == AudioManager.RINGER_MODE_NORMAL && notificationVolume > 0) {
                    AudioAttributes.USAGE_NOTIFICATION
                } else {
                    AudioAttributes.USAGE_MEDIA
                }
            }
        }

        fun openAppNotificationSettings(context: Context) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }

        fun openChannelNotificationSettings(context: Context, channelId: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val launched = runCatching { context.startActivity(intent) }.isSuccess
                if (!launched) openAppNotificationSettings(context)
            } else {
                openAppNotificationSettings(context)
            }
        }
    }

    init {
        createNotificationChannels()
    }

    private fun rawSoundUri(resId: Int): Uri =
        Uri.parse("android.resource://${appContext.packageName}/$resId")

    private fun soundUriForChannel(channelId: String): Uri = when (channelId) {
        CHANNEL_ID -> rawSoundUri(R.raw.lakemark_new_order)
        CHANNEL_READY_ID -> rawSoundUri(R.raw.lakemark_order_ready)
        else -> rawSoundUri(R.raw.lakemark_ping)
    }

    private fun soundUriForKind(kind: AlertSoundKind): Uri = when (kind) {
        AlertSoundKind.NEW_ORDER -> rawSoundUri(R.raw.lakemark_new_order)
        AlertSoundKind.ORDER_READY -> rawSoundUri(R.raw.lakemark_order_ready)
        AlertSoundKind.PING -> rawSoundUri(R.raw.lakemark_ping)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            // Remove legacy v1/v2 channels so upgrades inherit custom SOT-1 raw sounds & vibration settings.
            LEGACY_CHANNEL_IDS.forEach { oldId ->
                runCatching { manager.deleteNotificationChannel(oldId) }
            }

            val soundAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID,
                    appContext.getString(R.string.channel_kitchen_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = appContext.getString(R.string.channel_kitchen_desc)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 300)
                    setSound(soundUriForChannel(CHANNEL_ID), soundAttributes)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    CHANNEL_READY_ID,
                    appContext.getString(R.string.channel_ready_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = appContext.getString(R.string.channel_ready_desc)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 150)
                    setSound(soundUriForChannel(CHANNEL_READY_ID), soundAttributes)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    CHANNEL_BROADCAST_ID,
                    appContext.getString(R.string.channel_broadcast_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = appContext.getString(R.string.channel_broadcast_desc)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 150)
                    setSound(soundUriForChannel(CHANNEL_BROADCAST_ID), soundAttributes)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
            )
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        }
    }

    fun isChannelSoundEnabled(channelId: String): Boolean {
        val notifCompat = NotificationManagerCompat.from(appContext)
        if (!notifCompat.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return false
            val channel = manager.getNotificationChannel(channelId) ?: return false
            return channel.importance >= NotificationManager.IMPORTANCE_DEFAULT && channel.sound != null
        }
        return true
    }

    fun getNotificationSystemStatus(): NotificationSystemStatus {
        val hasPerm = hasNotificationPermission()
        val sysEnabled = runCatching {
            NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        }.getOrDefault(hasPerm)
        val audioManager = runCatching {
            appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        }.getOrNull()
        val mediaVol = runCatching { audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 1 }.getOrDefault(1)
        val notifVol = runCatching { audioManager?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: 1 }.getOrDefault(1)
        val ringerMode = runCatching { audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL }
            .getOrDefault(AudioManager.RINGER_MODE_NORMAL)

        return NotificationSystemStatus(
            hasPermission = hasPerm,
            notificationsEnabledInSystem = sysEnabled,
            kitchenChannelSoundEnabled = isChannelSoundEnabled(CHANNEL_ID),
            readyChannelSoundEnabled = isChannelSoundEnabled(CHANNEL_READY_ID),
            broadcastChannelSoundEnabled = isChannelSoundEnabled(CHANNEL_BROADCAST_ID),
            deviceMediaMuted = mediaVol <= 0,
            deviceNotificationMuted = notifVol <= 0 || ringerMode != AudioManager.RINGER_MODE_NORMAL,
        )
    }

    fun triggerAlert(
        kind: AlertSoundKind,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        volume: Float,
        forceForegroundStream: Boolean = false,
    ) {
        if (soundEnabled && volume > 0.01f) {
            val foreground = forceForegroundStream || AppForegroundTracker.isAppInForeground()
            scope.launch {
                chimeMutex.withLock {
                    playSynthesizedChime(
                        kind = kind,
                        volume = volume.coerceIn(0f, 1f),
                        isForeground = foreground,
                    )
                }
            }
        }
        if (vibrationEnabled) {
            triggerVibration(kind)
        }
    }

    fun showBrandedPushNotification(message: ParsedPushMessage): Boolean {
        val brandConfig = message.senderBrandConfig()
        val notifId = stableNotificationId(
            message.notifId.ifBlank { message.orderId.ifBlank { message.fcmMessageId } },
            message.orderNumber,
        )
        return postBrandedNotification(
            notificationId = notifId,
            channelId = message.channelCategory.channelId,
            title = message.title,
            body = message.body,
            brandConfig = brandConfig,
            senderRole = message.senderRole,
            deepLink = message.deepLink,
        )
    }

    fun showLocalNotification(
        title: String,
        body: String,
        notificationId: Int,
        brandConfig: BrandConfig = UserRole.MANAGER.defaultBrandConfig(),
        senderRole: UserRole = UserRole.fromKey(brandConfig.roleKey),
        deepLink: PushDeepLink? = null,
        channelCategory: PushChannelCategory = PushChannelCategory.KITCHEN_ALERTS,
    ): Boolean {
        return postBrandedNotification(
            notificationId = notificationId,
            channelId = channelCategory.channelId,
            title = title,
            body = body,
            brandConfig = brandConfig,
            senderRole = senderRole,
            deepLink = deepLink,
        )
    }

    private fun postBrandedNotification(
        notificationId: Int,
        channelId: String,
        title: String,
        body: String,
        brandConfig: BrandConfig,
        senderRole: UserRole,
        deepLink: PushDeepLink?,
    ): Boolean {
        if (!hasNotificationPermission()) {
            lastNotificationError = "Notification permission denied"
            return false
        }
        return try {
            val palette = deriveBrandPalette(brandConfig)
            val contentIntent = createDeepLinkPendingIntent(
                requestCode = notificationId,
                deepLink = deepLink ?: PushDeepLink(openNotificationsSheet = true),
            )
            val largeIconBitmap = createBrandMonogramBitmap(
                plumArgb = palette.plum.toArgb(),
                accentArgb = palette.acc.toArgb(),
                onPlumArgb = palette.onPlum.toArgb(),
                mark = palette.mark.ifBlank {
                    brandConfig.brandName.ifBlank { senderRole.displayName }.take(2).uppercase()
                },
            )
            val customSoundUri = soundUriForChannel(channelId)
            val vibPattern = if (channelId == CHANNEL_ID) {
                longArrayOf(0, 300, 150, 300, 150, 300)
            } else {
                longArrayOf(0, 150)
            }

            val builder = NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(R.drawable.ic_stat_lakemark)
                .setLargeIcon(largeIconBitmap)
                .setColor(palette.plum.toArgb())
                .setContentTitle(title)
                .setContentText(body)
                .setSubText("LAKEMARK · ${brandConfig.brandName.ifBlank { senderRole.displayName }}")
                .setStyle(NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(customSoundUri)
                .setVibrate(vibPattern)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)

            val lang = AppLanguage.fromCode(
                runCatching { UserPreferencesRepository(appContext).getQuickLanguageCode() }.getOrNull()
            )
            val appStrings = TranslationCatalog.buildStrings(lang, emptyMap())

            when {
                deepLink != null && deepLink.kind == "app_update" -> {
                    val updateIntent = createDeepLinkPendingIntent(
                        requestCode = notificationId + 60000,
                        deepLink = deepLink,
                    )
                    builder.addAction(
                        R.drawable.ic_stat_lakemark,
                        appStrings.get("notif_update_btn", appContext.getString(R.string.notif_action_update)),
                        updateIntent,
                    )
                }
                deepLink != null && deepLink.orderId.isNotBlank() -> {
                    builder.addAction(
                        R.drawable.ic_stat_lakemark,
                        appStrings.get("home_action_open_orders", appContext.getString(R.string.notif_action_view_order)),
                        contentIntent,
                    )
                }
                else -> {
                    val alertsIntent = createDeepLinkPendingIntent(
                        requestCode = notificationId + 50000,
                        deepLink = PushDeepLink(openNotificationsSheet = true),
                    )
                    builder.addAction(
                        R.drawable.ic_stat_lakemark,
                        appStrings.get("notif_sheet_title", appContext.getString(R.string.notif_action_open_alerts)),
                        alertsIntent,
                    )
                }
            }

            NotificationManagerCompat.from(appContext).notify(notificationId, builder.build())
            lastNotificationError = null
            true
        } catch (se: SecurityException) {
            lastNotificationError = se.localizedMessage ?: "Notification permission missing"
            Log.w(TAG, "SecurityException posting notification", se)
            false
        } catch (e: Exception) {
            lastNotificationError = e.localizedMessage ?: "Failed to post notification"
            Log.e(TAG, "Error posting notification", e)
            false
        }
    }

    private fun createDeepLinkPendingIntent(requestCode: Int, deepLink: PushDeepLink): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            setPackage(appContext.packageName)
            action = Intent.ACTION_VIEW
            data = Uri.parse(deepLink.toUriString())
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PushDeepLink.EXTRA_TAB, deepLink.tab.key)
            putExtra(PushDeepLink.EXTRA_ORDER_ID, deepLink.orderId)
            putExtra(PushDeepLink.EXTRA_NOTIF_ID, deepLink.notifId)
            putExtra(PushDeepLink.EXTRA_OPEN_SHEET, deepLink.openNotificationsSheet)
            putExtra(PushDeepLink.EXTRA_KIND, deepLink.kind)
            putExtra(PushDeepLink.EXTRA_ORDER_STATUS, deepLink.orderStatus)
            putExtra(PushDeepLink.EXTRA_APK_URL, deepLink.apkUrl)
        }
        return PendingIntent.getActivity(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun createBrandMonogramBitmap(
        plumArgb: Int,
        accentArgb: Int,
        onPlumArgb: Int,
        mark: String,
    ): Bitmap {
        val sizePx = 128
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val rect = RectF(6f, 6f, sizePx - 6f, sizePx - 6f)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = plumArgb
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 34f, 34f, fillPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            style = Paint.Style.STROKE
            strokeWidth = 7f
        }
        canvas.drawRoundRect(rect, 34f, 34f, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onPlumArgb
            textAlign = Paint.Align.CENTER
            textSize = if (mark.length > 1) 46f else 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val y = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(mark.take(2), canvas.width / 2f, y, textPaint)
        return bmp
    }

    private fun stableNotificationId(key: String, orderNumber: Long): Int {
        return if (key.isNotBlank()) {
            (abs(key.hashCode()) % 900000) + 1000
        } else if (orderNumber > 0L) {
            (orderNumber % 900000L).toInt() + 1000
        } else {
            (System.currentTimeMillis() % 900000L).toInt() + 1000
        }
    }

    private fun triggerVibration(kind: AlertSoundKind) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return
            val pattern = if (kind == AlertSoundKind.NEW_ORDER) {
                longArrayOf(0, 300, 150, 300, 150, 300)
            } else {
                longArrayOf(0, 150)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration trigger failed: ${e.message}")
        }
    }

    private fun playSynthesizedChime(
        kind: AlertSoundKind,
        volume: Float,
        isForeground: Boolean,
    ) {
        val audioManager = runCatching {
            appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        }.getOrNull()

        val mediaVol = runCatching { audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 1 }.getOrDefault(1)
        val notifVol = runCatching { audioManager?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: 1 }.getOrDefault(1)
        val ringerMode = runCatching { audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL }
            .getOrDefault(AudioManager.RINGER_MODE_NORMAL)

        // When backgrounded and device ringer is intentionally Silent/Vibrate, do not play media audio.
        if (!isForeground && ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            return
        }

        val usage = selectAlertAudioUsage(
            isForeground = isForeground,
            mediaVolume = mediaVol,
            notificationVolume = notifVol,
            ringerMode = ringerMode,
        )
        val streamType = if (usage == AudioAttributes.USAGE_MEDIA) {
            AudioManager.STREAM_MUSIC
        } else {
            AudioManager.STREAM_NOTIFICATION
        }

        val attributes = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        var focusRequest: AudioFocusRequest? = null
        if (audioManager != null) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(attributes)
                        .setAcceptsDelayedFocusGain(false)
                        .build()
                    focusRequest = req
                    audioManager.requestAudioFocus(req)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.requestAudioFocus(
                        null,
                        streamType,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                    )
                }
            }
        }

        var track: AudioTrack? = null
        try {
            val sampleRate = runCatching {
                AudioTrack.getNativeOutputSampleRate(streamType).coerceIn(22050, 48000)
            }.getOrDefault(DEFAULT_SAMPLE_RATE)

            // Exact SOT-1 synthesis: (freqHz, stepSec, durSec, gain, isSquare)
            val (notes, stepSec, durSec, gain, isSquare) = when (kind) {
                AlertSoundKind.NEW_ORDER -> listOf(880.0, 660.0, 880.0, 660.0) to 0.20
                AlertSoundKind.ORDER_READY -> listOf(523.0, 659.0, 784.0, 1047.0) to 0.13
                AlertSoundKind.PING -> listOf(660.0, 880.0) to 0.12
            }.let { (freqs, step) ->
                when (kind) {
                    AlertSoundKind.NEW_ORDER -> Array<Any>(5) { i -> when (i) { 0 -> freqs; 1 -> step; 2 -> 0.17; 3 -> 0.7; else -> true } }
                    AlertSoundKind.ORDER_READY -> Array<Any>(5) { i -> when (i) { 0 -> freqs; 1 -> step; 2 -> 0.40; 3 -> 1.0; else -> false } }
                    AlertSoundKind.PING -> Array<Any>(5) { i -> when (i) { 0 -> freqs; 1 -> step; 2 -> 0.25; 3 -> 0.9; else -> false } }
                }
            }
            @Suppress("UNCHECKED_CAST")
            val freqList = notes as List<Double>
            val step = stepSec as Double
            val dur = durSec as Double
            val noteGain = gain as Double
            val squareWave = isSquare as Boolean

            val totalSec = (freqList.size - 1) * step + dur + 0.06
            val totalSamples = (totalSec * sampleRate).toInt()
            val mix = DoubleArray(totalSamples)
            val attackSamples = (0.02 * sampleRate).toInt().coerceAtLeast(1)

            freqList.forEachIndexed { idx, freq ->
                val startSample = (idx * step * sampleRate).toInt()
                val noteSamples = (dur * sampleRate).toInt()
                val peak = volume * noteGain * 0.5
                for (i in 0 until noteSamples) {
                    val pos = startSample + i
                    if (pos >= totalSamples) break
                    val t = i.toDouble() / sampleRate
                    val env = if (i < attackSamples) {
                        (i.toDouble() / attackSamples) * peak
                    } else {
                        peak * Math.pow(0.001 / (peak + 1e-6), (t - 0.02) / (dur - 0.02).coerceAtLeast(0.01))
                    }
                    val phase = 2.0 * PI * freq * t
                    val osc = if (squareWave) {
                        if (sin(phase) >= 0.0) 1.0 else -1.0
                    } else {
                        sin(phase)
                    }
                    mix[pos] += osc * env
                }
            }
            val pcm = ShortArray(totalSamples) { idx ->
                (mix[idx].coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val minBuf = runCatching {
                AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                ).coerceAtLeast(4096)
            }.getOrDefault(8192)
            val bufferBytes = maxOf(pcm.size * 2, minBuf)

            track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (track.state != AudioTrack.STATE_INITIALIZED) {
                Log.w(TAG, "AudioTrack failed to initialize (state=${track.state}); falling back to system tone")
                playSystemNotificationToneFallback(kind, usage, streamType, volume)
                return
            }

            runCatching { track.setVolume(volume.coerceIn(0.05f, 1f)) }
            track.play()

            var writtenTotal = 0
            while (writtenTotal < pcm.size) {
                val written = track.write(pcm, writtenTotal, pcm.size - writtenTotal)
                if (written <= 0) {
                    Log.w(TAG, "AudioTrack write returned $written; falling back to system tone")
                    playSystemNotificationToneFallback(kind, usage, streamType, volume)
                    return
                }
                writtenTotal += written
            }

            val expectedDurationMs = (totalSamples * 1000L) / sampleRate
            val deadlineMs = System.currentTimeMillis() + expectedDurationMs + 350L
            while (System.currentTimeMillis() < deadlineMs) {
                if (track.playbackHeadPosition >= totalSamples) break
                Thread.sleep(25L)
            }
            runCatching { track.stop() }
        } catch (e: Exception) {
            Log.w(TAG, "Synthesized chime failed, playing system notification tone fallback", e)
            playSystemNotificationToneFallback(kind, usage, streamType, volume)
        } finally {
            runCatching { track?.release() }
            if (audioManager != null) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                    } else {
                        @Suppress("DEPRECATION")
                        audioManager.abandonAudioFocus(null)
                    }
                }
            }
        }
    }

    private fun playSystemNotificationToneFallback(
        kind: AlertSoundKind,
        usage: Int,
        streamType: Int,
        volume: Float,
    ) {
        val ringtonePlayed = runCatching {
            val uri = soundUriForKind(kind)
            val ringtone = RingtoneManager.getRingtone(appContext, uri) ?: return@runCatching false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.volume = volume.coerceIn(0.1f, 1f)
            }
            ringtone.play()
            Thread.sleep(550L)
            true
        }.getOrDefault(false)

        if (!ringtonePlayed) {
            runCatching {
                val gain = (volume * 100).toInt().coerceIn(30, 100)
                val gen = ToneGenerator(streamType, gain)
                try {
                    gen.startTone(ToneGenerator.TONE_PROP_BEEP2, 320)
                    Thread.sleep(360L)
                } finally {
                    gen.release()
                }
            }.onFailure { err ->
                Log.w(TAG, "Fallback tone failed: ${err.message}")
            }
        }
    }
}
