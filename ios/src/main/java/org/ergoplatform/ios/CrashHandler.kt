package org.ergoplatform.ios

import org.robovm.apple.foundation.NSBundle
import org.robovm.apple.foundation.NSDictionary
import org.robovm.apple.foundation.NSException
import org.robovm.apple.foundation.NSObject
import org.robovm.apple.uikit.UIDevice
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CrashHandler {

    private const val LAST_CRASH_FILE_NAME = "lastcrash.txt"

    fun registerUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            var exceptionAsString: String? = null
            try {
                exceptionAsString = e.stackTraceToString()
                writeToDebugFile(exceptionAsString)

            } catch (throwable: Throwable) {
                // do nothing
            } finally {
                // crash the app in iOS
                val exception = NSException(
                    e.javaClass.name,
                    exceptionAsString ?: "(no stacktrace)", NSDictionary<NSObject, NSObject>()
                )
                exception.raise()
            }
        }
    }

    fun writeToDebugFile(exceptionAsString: String) {
        val file = getCrashFile()

        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.US)
        val nowAsString: String = df.format(Date())
        val osVersion = UIDevice.getCurrentDevice().systemVersion
        val device = UIDevice.getCurrentDevice().name

        file.writeText(
            "$nowAsString\n" +
                    "App: ${getAppVersion()}\n" +
                    "OS: $osVersion\n" +
                    "Device: $device\n\n" +
                    exceptionAsString
        )
    }

    private fun getCrashFile(): File {
        val libraryPath = File(System.getenv("HOME"), "Library")
        return File(libraryPath, LAST_CRASH_FILE_NAME)
    }

    fun getAppVersion(): String? {
        return NSBundle.getMainBundle().infoDictionary?.getString("CFBundleShortVersionString")
    }

    fun getLastCrashInformation(): String? {
        val crashFile = getCrashFile()
        if (crashFile.exists()) {
            return crashFile.readText()
        } else
            return null
    }
}