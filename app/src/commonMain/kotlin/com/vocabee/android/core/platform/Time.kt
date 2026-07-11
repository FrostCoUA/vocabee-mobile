package com.vocabee.android.core.platform

/** Wall-clock time in epoch milliseconds (System.currentTimeMillis on Android, NSDate on iOS). */
expect fun currentEpochMillis(): Long

/** Local-midnight epoch millis for the day containing [epochMillis]. */
expect fun startOfDayEpochMillis(epochMillis: Long): Long
