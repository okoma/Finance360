package com.i2medier.financialpro.planner.domain

import java.util.Calendar
import java.util.TimeZone
private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
fun Long.toUtcMidnight(): Long {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = this
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
fun monthStartUtc(timestamp: Long): Long {
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.DAY_OF_MONTH, 1)
fun monthEndExclusiveUtc(timestamp: Long): Long {
    calendar.timeInMillis = monthStartUtc(timestamp)
    calendar.add(Calendar.MONTH, 1)
fun daysBetweenUtc(startUtcMidnight: Long, endUtcMidnight: Long): Int {
    return ((endUtcMidnight - startUtcMidnight) / DAY_IN_MILLIS).toInt()
