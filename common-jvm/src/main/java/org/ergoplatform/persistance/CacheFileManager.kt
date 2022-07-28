package org.ergoplatform.persistance

/**
 * Manager to save, read or remove cache files
 */
interface CacheFileManager {

    fun fileExists(id: String): Boolean

    fun readFileContent(id: String): ByteArray?

    fun saveFileContent(id: String, fileContent: ByteArray)

    fun deleteFile(id: String)
}