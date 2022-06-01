package org.ergoplatform.android.persistence

import android.content.Context
import org.ergoplatform.persistance.CacheFileManager
import java.io.File

class AndroidCacheFiles(context: Context) : CacheFileManager {
    private val cacheDir = context.cacheDir

    override fun fileExists(id: String): Boolean {
        val cacheFile = File(cacheDir, id)
        return cacheFile.exists()
    }

    override fun readFileContent(id: String): ByteArray? {
        val cacheFile = File(cacheDir, id)
        return if (cacheFile.exists())
            cacheFile.readBytes()
        else null
    }

    override fun saveFileContent(id: String, fileContent: ByteArray) {
        val cacheFile = File(cacheDir, id)
        cacheFile.writeBytes(fileContent)
    }

    override fun deleteFile(id: String) {
        val cacheFile = File(cacheDir, id)
        if (cacheFile.exists())
            cacheFile.delete()
    }
}