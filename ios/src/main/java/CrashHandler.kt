import org.robovm.apple.foundation.NSDictionary
import org.robovm.apple.foundation.NSException
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CrashHandler {

    private const val LAST_CRASH_FILE_NAME = "lastcrash.txt"

    fun registerUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            var exceptionAsString: String? = null
            try {
                exceptionAsString = e.stackTraceToString()
                val file = getCrashFile()

                val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ")
                val nowAsString: String = df.format(Date())

                file.writeText("$nowAsString\n\n$exceptionAsString")

            } catch (throwable: Throwable) {
                // do nothing
            } finally {
                // crash the app in iOS
                val exception = NSException(
                    e.javaClass.name,
                    exceptionAsString ?: "(no stacktrace)", NSDictionary<Any?, Any?>()
                )
                exception.raise()
            }
        }
    }

    private fun getCrashFile(): File {
        val libraryPath = File(System.getenv("HOME"), "Library")
        return File(libraryPath, LAST_CRASH_FILE_NAME)
    }

    fun getLastCrashInformation(): String {
        return getCrashFile().readText()
    }
}