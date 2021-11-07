package org.ergoplatform.utils

import java.text.SimpleDateFormat
import java.util.*

object LogUtils {
    var logDebug = false

    fun logDebug(tag: String, msg: String, t: Throwable? = null) {
        if (logDebug) {
            println(SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date()) + " [" + tag + "] " + msg)
            t?.printStackTrace()
        }
    }
}