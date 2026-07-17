package com.vocabee.android.di

import platform.Foundation.NSBundle

/**
 * The Xcode Debug/Release configuration writes this value into the app's
 * Info.plist. The fallback keeps command-line framework builds usable when
 * Info.plist substitution is not available.
 */
internal expect val vocabeeIosBaseUrl: String

internal fun configuredIosApiBaseUrl(fallback: String): String =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("VocabeeApiBaseUrl") as? String)
        ?.trim()
        ?.takeIf { it.isNotEmpty() && !it.startsWith("$(") }
        ?: fallback
