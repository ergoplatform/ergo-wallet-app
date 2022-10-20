package org.ergoplatform.desktop.ui

import org.ergoplatform.utils.LogUtils
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URI
import java.util.*


private val osName by lazy(LazyThreadSafetyMode.NONE) {
    System.getProperty("os.name").lowercase(Locale.getDefault())
}

private val jvmFullPath by lazy(LazyThreadSafetyMode.NONE) {
    System.getProperty("java.home")
}

fun isWindows() = "windows" in osName
fun isMacOS() = "mac" in osName

fun openBrowser(url: String) {
    val desktop = Desktop.getDesktop()
    when {
        Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE) -> {
            desktop.browse(URI(url))
        }
        isMacOS() -> {
            Runtime.getRuntime().exec("open $url")
        }
        "nix" in osName || "nux" in osName -> {
            Runtime.getRuntime().exec("xdg-open $url")
        }
    }
}

fun String.copyToClipboard() {
    val selection = StringSelection(this)
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(selection, selection)
}

fun canRegisterUriScheme(): Boolean = isWindows()

fun registerUriSchemes() {

    try {
        if (isWindows()) {
            val applicationPath = ProcessHandle.current().getCommandLineWindows().insertFullJvmDir()
            registerUriSchemesWindows(
                applicationPath,
                listOf("ergopay", "ergo", "ergoauth", "mosaikapp")
            )
        } else {
            // val applicationPath = ProcessHandle.current().info().commandLine().get()
        }
    } catch (e: Throwable) {
        LogUtils.logDebug("PlatformUtils", "Error registering uri: ${e.message}", e)
    }
}

private fun String.insertFullJvmDir(): String {
    // on Windows, the process can be started from CLI with java -jar etc - but this does not
    // work for URI schemes, here we need the full path
    return if (!this.startsWith("java ") && !this.startsWith("javaw "))
        return this
    else
        return "\"$jvmFullPath\\bin\\" + this.substringBefore(' ') + "\" " + this.substringAfter(' ')
}

/**
 * Returns the full command-line of the process.
 * <p>
 * This is a workaround for
 * <a href="https://stackoverflow.com/a/46768046/14731">https://stackoverflow.com/a/46768046/14731</a>
 */
private fun ProcessHandle.getCommandLineWindows(): String {
    val desiredProcessid = pid()
    val process = ProcessBuilder(
        "wmic", "process", "where",
        "ProcessID=$desiredProcessid", "get",
        "commandline", "/format:list"
    ).redirectErrorStream(true).start()
    InputStreamReader(process.inputStream).use { inputStreamReader ->
        BufferedReader(inputStreamReader).use { reader ->
            while (true) {
                val line: String = reader.readLine()
                if (!line.startsWith("CommandLine=")) {
                    continue
                }
                return line.substring("CommandLine=".length)
            }
        }
    }
}

private fun registerUriSchemesWindows(applicationPath: String, uriSchemes: List<String>) {
    LogUtils.logDebug("PlatformUtils", "Writing reg file to register $applicationPath")
    val escapedApplicationPath: String = applicationPath.replace("\\", "\\\\").replace("\"", "\\\"")
    val regFilePref = "Windows Registry Editor Version 5.00\n"
    val schemes = uriSchemes.map { schemeName ->
        """           
[HKEY_CURRENT_USER\SOFTWARE\Classes\$schemeName]
@="$schemeName URI"
"URL Protocol"=""
"Content Type"="application/x-$schemeName"

[HKEY_CURRENT_USER\SOFTWARE\Classes\$schemeName\shell]
@="open"

[HKEY_CURRENT_USER\SOFTWARE\Classes\$schemeName\shell\open]

[HKEY_CURRENT_USER\SOFTWARE\Classes\$schemeName\shell\open\command]
@="$escapedApplicationPath \"%1\""
"""
    }

    val tempFile = File.createTempFile("URLPROTOCOLHANDLERCREATION", ".reg")
    tempFile.writeText(regFilePref + schemes.joinToString(""))
    val commandStrings = arrayOf("cmd", "/c", "regedit", "/s", tempFile.absolutePath)
    LogUtils.logDebug("PlaformUtils", "Registering ${tempFile.absolutePath}")
    Runtime.getRuntime().exec(commandStrings)
}