package org.ergoplatform.persistance

/**
 * Manager to save, read or remove cache files
 */
interface CacheFileManager {

    fun fileExists(id: String): Boolean

    fun saveFile(id: String, fileContent: ByteArray)

    fun deleteFile(iconFile: String)
}