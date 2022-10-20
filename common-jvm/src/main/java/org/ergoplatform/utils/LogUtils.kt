package org.ergoplatform.utils

import java.text.SimpleDateFormat
import java.util.*

object LogUtils {
    private const val maxToPrint = 5000
    var logDebug = false
    var stackTraceLogger: ((String) -> Unit)? = null

    fun logDebug(tag: String, msg: String, t: Throwable? = null) {
        if (logDebug) {
            println(SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date()) +
                    " [" + tag + "] " + msg.take(maxToPrint))
            if (msg.length > maxToPrint)
                println("(shortened content with original length ${msg.length})")
            t?.let { println(t.message) }
            t?.printStackTrace()
        }
        t?.let { stackTraceLogger?.invoke(it.stackTraceToString()) }
    }
}