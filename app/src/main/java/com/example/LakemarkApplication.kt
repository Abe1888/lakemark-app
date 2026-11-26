package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.fcm.AppForegroundTracker
import com.example.util.AlertAudioAndNotificationManager
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LakemarkApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Ensure notification channels exist immediately even when woken in background by FCM.
        AlertAudioAndNotificationManager(this)

        runCatching {
            Firebase.crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            Firebase.analytics.setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = AppForegroundTracker.onActivityStarted()
            override fun onActivityResumed(activity: Activity) = AppForegroundTracker.onActivityResumed()
            override fun onActivityPaused(activity: Activity) = AppForegroundTracker.onActivityPaused()
            override fun onActivityStopped(activity: Activity) = AppForegroundTracker.onActivityStopped()
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
