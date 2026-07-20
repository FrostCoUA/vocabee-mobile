package com.vocabee.android.core.platform

actual fun debugLog(tag: String, message: String) {
    println("$tag: $message")
}
