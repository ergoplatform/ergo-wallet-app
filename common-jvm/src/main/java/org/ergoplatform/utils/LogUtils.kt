package org.ergoplatform.utils

import java.text.SimpleDateFormat
import java.util.*

object LogUtils {
    var logDebug = false
    var stackTraceLogger: ((String) -> Unit)? = null

    fun logDebug(tag: String, msg: String, t: Throwable? = null) {
        if (logDebug) {
            println(SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date()) + " [" + tag + "] " + msg)
            t?.let { println(t.message) }
            t?.printStackTrace()
        }
        t?.let { stackTraceLogger?.invoke(it.stackTraceToString()) }
    }
}