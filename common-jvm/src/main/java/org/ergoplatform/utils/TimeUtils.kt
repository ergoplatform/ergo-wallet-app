package org.ergoplatform.utils

import org.ergoplatform.uilogic.*

private const val MAX_SECONDS_JUST_NOW = 30
private const val MAX_SECONDS_MOMENTS_AGO = 120
private const val MAX_MINUTES_TO_SHOW = 120
private const val MAX_HOURS_TO_SHOW = 48

private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24

fun getTimeSpanString(
    seconds: Long,
    stringProvider: StringProvider
): String {
    val timeSpanString: String
    if (seconds < MAX_SECONDS_JUST_NOW) {
        timeSpanString = stringProvider.getString(STRING_LABEL_TIME_SPAN_JUST_NOW)
    } else if (seconds < MAX_SECONDS_MOMENTS_AGO) {
        timeSpanString = stringProvider.getString(STRING_LABEL_TIME_SPAN_MOMENTS_AGO)
    } else {
        val minuteTimeSpan = seconds / SECONDS_PER_MINUTE
        if (minuteTimeSpan < MAX_MINUTES_TO_SHOW) {
            timeSpanString =
                stringProvider.getString(STRING_LABEL_TIME_SPAN_MINUTES_AGO, minuteTimeSpan)
        } else {
            val hourTimeSpan = minuteTimeSpan / MINUTES_PER_HOUR
            if (hourTimeSpan < MAX_HOURS_TO_SHOW)
                timeSpanString =
                    stringProvider.getString(STRING_LABEL_TIME_SPAN_HOURS_AGO, hourTimeSpan)
            else {
                val dayTimeSpan = hourTimeSpan / HOURS_PER_DAY
                timeSpanString =
                    stringProvider.getString(STRING_LABEL_TIME_SPAN_DAYS_AGO, dayTimeSpan)
            }
        }
    }
    return timeSpanString
}