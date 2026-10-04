package com.offlineplayer.core.model

import android.util.Log

/**
 * Stub for Crashlytics and general analytics reporting.
 */
object AnalyticsLogger {
    fun logEvent(eventName: String, params: Map<String, Any> = emptyMap()) {
        Log.d("Analytics", "Event: $eventName | Params: $params")
    }

    fun logError(throwable: Throwable, message: String? = null) {
        Log.e("Analytics", "Error: ${message ?: throwable.message}", throwable)
    }
}
