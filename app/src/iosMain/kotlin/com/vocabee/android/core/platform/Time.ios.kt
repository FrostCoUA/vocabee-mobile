package com.vocabee.android.core.platform

import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

actual fun currentEpochMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}

actual fun startOfDayEpochMillis(epochMillis: Long): Long {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    val startOfDay = NSCalendar.currentCalendar.startOfDayForDate(date)
    return (startOfDay.timeIntervalSince1970 * 1000.0).toLong()
}
