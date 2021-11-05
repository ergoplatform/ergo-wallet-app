package org.ergoplatform.utils

import org.ergoplatform.uilogic.STRING_LABEL_LAST_SYNC_JUST_NOW
import org.ergoplatform.uilogic.StringProvider

fun getTimeSpanString(
    seconds: Long,
    stringProvider: StringProvider
): String {
    val timeSpanString: String
    if (seconds < 60) {
        timeSpanString = stringProvider.getString(STRING_LABEL_LAST_SYNC_JUST_NOW)
    } else {
        // TODO use StringProvider here, too
        val minuteTimeSpan = seconds / 60
        if (minuteTimeSpan < 60) {
            timeSpanString = "$minuteTimeSpan minutes ago"
        } else {
            val hourTimeSpan = minuteTimeSpan / 24
            if (hourTimeSpan < 24)
                timeSpanString = "$hourTimeSpan hours ago"
            else {
                val dayTimeSpan = hourTimeSpan / 24
                timeSpanString = "$dayTimeSpan days ago"
            }
        }
    }
    return timeSpanString
}