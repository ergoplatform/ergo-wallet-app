package org.ergoplatform.utils

import org.ergoplatform.uilogic.*

fun getTimeSpanString(
    seconds: Long,
    stringProvider: StringProvider
): String {
    val timeSpanString: String
    if (seconds < 60) {
        timeSpanString = stringProvider.getString(STRING_LABEL_TIME_SPAN_JUST_NOW)
    } else {
        val minuteTimeSpan = seconds / 60
        if (minuteTimeSpan < 60) {
            timeSpanString =
                stringProvider.getString(STRING_LABEL_TIME_SPAN_MINUTES_AGO, minuteTimeSpan)
        } else {
            val hourTimeSpan = minuteTimeSpan / 24
            if (hourTimeSpan < 24)
                timeSpanString =
                    stringProvider.getString(STRING_LABEL_TIME_SPAN_HOURS_AGO, hourTimeSpan)
            else {
                val dayTimeSpan = hourTimeSpan / 24
                timeSpanString =
                    stringProvider.getString(STRING_LABEL_TIME_SPAN_DAYS_AGO, dayTimeSpan)
            }
        }
    }
    return timeSpanString
}