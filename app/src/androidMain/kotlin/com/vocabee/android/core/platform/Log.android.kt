package com.vocabee.android.core.platform

actual fun debugLog(tag: String, message: String) {
    android.util.Log.d(tag, message)
}
