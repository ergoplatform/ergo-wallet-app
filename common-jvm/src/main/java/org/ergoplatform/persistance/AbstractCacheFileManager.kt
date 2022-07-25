package org.ergoplatform.persistance

import java.io.File

abstract class AbstractCacheFileManager(protected val cacheDir: File) : CacheFileManager {
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