package org.ergoplatform.desktop.ui

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.util.*

fun openBrowser(url: String) {
    val osName by lazy(LazyThreadSafetyMode.NONE) {
        System.getProperty("os.name").lowercase(Locale.getDefault())
    }
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

fun String.copyToClipoard() {
    val selection = StringSelection(this)
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(selection, selection)
}