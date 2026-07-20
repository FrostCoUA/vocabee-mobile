package com.vocabee.android.core.analytics

import com.posthog.PostHog

/**
 * Тонка обгортка над статичним PostHog SDK. Якщо `PostHogAndroid.setup` не
 * викликано (порожній ключ у збірці), SDK сам мовчки ігнорує виклики.
 */
class PostHogAnalyticsTracker : AnalyticsTracker {
    override fun track(event: String, properties: Map<String, Any?>) {
        PostHog.capture(event = event, properties = properties.withoutNulls())
    }

    override fun identify(userId: String, properties: Map<String, Any?>) {
        PostHog.identify(distinctId = userId, userProperties = properties.withoutNulls())
    }

    override fun reset() {
        PostHog.reset()
    }
}

private fun Map<String, Any?>.withoutNulls(): Map<String, Any> =
    buildMap {
        for ((key, value) in this@withoutNulls) {
            if (value != null) put(key, value)
        }
    }
