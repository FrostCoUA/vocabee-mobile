package com.vocabee.android.core.platform

/** Дебаг-лог (`android.util.Log.d` на Android, `println` на iOS). */
expect fun debugLog(tag: String, message: String)
