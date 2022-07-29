package org.ergoplatform.desktop.ui

import org.ergoplatform.utils.LogUtils
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.util.*


private val osName by lazy(LazyThreadSafetyMode.NONE) {
    System.getProperty("os.name").lowercase(Locale.getDefault())
}

fun openBrowser(url: String) {
    val desktop = Desktop.getDesktop()
    when {
        Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE) -> {
            desktop.browse(URI(url))
        }
        "mac" in osName -> {
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

fun canRegisterUriScheme(): Boolean = "windows" in osName

fun registerUriSchemes() {
    val applicationPath = ProcessHandle.current().info().commandLine().get()

    try {
        if ("windows" in osName) {
            registerUriSchemesWindows(applicationPath, listOf("ergopay", "ergo", "ergoauth"))
        }
    } catch (e: Throwable) {
        LogUtils.logDebug("PlatformUtils", "Error registering uri: ${e.message}", e)
    }
}

private fun registerUriSchemesWindows(applicationPath: String, uriSchemes: List<String>) {
    val escapedApplicationPath: String = applicationPath.replace("\\", "\\\\")
    val regFilePref = "Windows Registry Editor Version 5.00\n"
    val schemes = uriSchemes.map { schemeName ->
        """           
[HKEY_CURRENT_USER\$schemeName]
@="$schemeName URI"
"URL Protocol"=""
"Content Type"="application/x-$schemeName"

[HKEY_CURRENT_USER\$schemeName\shell]
@="open"

[HKEY_CURRENT_USER\$schemeName\shell\open]

[HKEY_CURRENT_USER\$schemeName\shell\open\command]
@="\"$escapedApplicationPath\" \"%1\""
"""
    }

    val tempFile = File.createTempFile("URLPROTOCOLHANDLERCREATION", ".reg")
    tempFile.writeText(regFilePref + schemes.joinToString(""))
    val commandStrings = arrayOf("cmd", "/c", "regedit", "/s", tempFile.absolutePath)
    LogUtils.logDebug("PlaformUtils", "Registering ${tempFile.absolutePath}")
    Runtime.getRuntime().exec(commandStrings)
}