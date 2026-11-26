package com.example.fcm

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

object AppForegroundTracker {
    private val startedCount = AtomicInteger(0)
    private val resumedCount = AtomicInteger(0)

    private val _isForeground = MutableStateFlow(false)
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    fun onActivityStarted() {
        startedCount.incrementAndGet()
        updateForegroundState()
    }

    fun onActivityResumed() {
        resumedCount.incrementAndGet()
        updateForegroundState()
    }

    fun onActivityPaused() {
        resumedCount.updateAndGet { (it - 1).coerceAtLeast(0) }
        updateForegroundState()
    }

    fun onActivityStopped() {
        startedCount.updateAndGet { (it - 1).coerceAtLeast(0) }
        updateForegroundState()
    }

    private fun updateForegroundState() {
        _isForeground.value = startedCount.get() > 0 || resumedCount.get() > 0
    }

    fun isAppInForeground(): Boolean = _isForeground.value

    internal fun resetForTest() {
        startedCount.set(0)
        resumedCount.set(0)
        _isForeground.value = false
    }
}

object InAppPushCoordinator {
    private val _foregroundEvents = MutableSharedFlow<ParsedPushMessage>(extraBufferCapacity = 32)
    val foregroundEvents: SharedFlow<ParsedPushMessage> = _foregroundEvents.asSharedFlow()

    private val _pendingDeepLink = MutableStateFlow<PushDeepLink?>(null)
    val pendingDeepLink: StateFlow<PushDeepLink?> = _pendingDeepLink.asStateFlow()

    fun emitForegroundPush(message: ParsedPushMessage) {
        _foregroundEvents.tryEmit(message)
    }

    fun enqueueDeepLink(deepLink: PushDeepLink) {
        _pendingDeepLink.value = deepLink
    }

    fun consumeDeepLink(): PushDeepLink? {
        val current = _pendingDeepLink.value
        if (current != null) {
            _pendingDeepLink.value = null
        }
        return current
    }
}
