package com.vocabee.android.core.platform

import java.util.Calendar

actual fun currentEpochMillis(): Long = System.currentTimeMillis()

actual fun startOfDayEpochMillis(epochMillis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = epochMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
