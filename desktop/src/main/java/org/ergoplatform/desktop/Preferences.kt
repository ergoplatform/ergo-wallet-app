package org.ergoplatform.desktop

import org.ergoplatform.isErgoMainNet
import org.ergoplatform.persistance.PreferencesProvider
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

const val windowX = "windowX"
const val windowY = "windowY"
const val windowWidth = "windowWidth"
const val windowHeight = "windowHeight"

class Preferences(private val file: File) : PreferencesProvider() {
    private val properties = Properties()

    init {
        if (file.exists())
            file.inputStream().use {
                properties.loadFromXML(it)
            }
    }


    override fun getString(key: String, default: String): String {
        return properties.getProperty(key, default)
    }

    override fun saveString(key: String, value: String) {
        properties[key] = value
        flush()
    }

    override fun getLong(key: String, default: Long): Long {
        return properties.getProperty(key, default.toString()).toLong()
    }

    override fun saveLong(key: String, value: Long) {
        properties[key] = value.toString()
        flush()
    }

    override fun getFloat(key: String, default: Float): Float {
        return properties.getProperty(key, default.toString()).toFloat()
    }

    override fun saveFloat(key: String, value: Float) {
        properties[key] = value.toString()
        flush()
    }

    private fun flush() {
        FileOutputStream(file, false).use {
            val out = BufferedOutputStream(it)
            properties.storeToXML(out, null)
        }
    }

    companion object {
        fun getPrefsFor(dataDir: String): Preferences {
            val name = if (isErgoMainNet) "wallet_prefs" else "wallet_test"
            val libraryPath = File(dataDir)
            libraryPath.mkdirs()
            val prefFile = File(libraryPath, "$name.xml")

            return Preferences(prefFile)
        }
    }

    var windowSize: Pair<Float, Float>
        get() = Pair(
            getFloat(windowWidth, 1200f), getFloat(
                windowHeight, 800f
            )
        )
        set(value) {
            saveFloat(windowWidth, value.first)
            saveFloat(windowHeight, value.second)
        }

    var windowPos: Pair<Float, Float>
        get() = Pair(
            getFloat(windowX, 0f), getFloat(
                windowY, -1f
            )
        )
        set(value) {
            saveFloat(windowX, value.first)
            saveFloat(windowY, value.second)
        }
}