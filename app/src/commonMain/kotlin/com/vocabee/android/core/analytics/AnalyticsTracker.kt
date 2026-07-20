package com.vocabee.android.core.analytics

/**
 * Продуктова аналітика (PostHog на Android). Спільний код знає лише цей
 * інтерфейс; платформа без SDK підставляє [NoAnalyticsTracker].
 */
interface AnalyticsTracker {
    fun track(event: String, properties: Map<String, Any?> = emptyMap())

    /** Прив'язує події до серверного user id (той самий distinct id, що й у бекенд-подіях). */
    fun identify(userId: String, properties: Map<String, Any?> = emptyMap())

    /** Скидає особу після виходу з акаунта. */
    fun reset()
}

object NoAnalyticsTracker : AnalyticsTracker {
    override fun track(event: String, properties: Map<String, Any?>) = Unit
    override fun identify(userId: String, properties: Map<String, Any?>) = Unit
    override fun reset() = Unit
}
