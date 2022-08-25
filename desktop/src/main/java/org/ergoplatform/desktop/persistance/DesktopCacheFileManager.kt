package org.ergoplatform.desktop.persistance

import org.ergoplatform.persistance.AbstractCacheFileManager
import java.io.File

class DesktopCacheFileManager(dir: String): AbstractCacheFileManager(File(dir)) {
    init {
        cacheDir.mkdirs()
    }
}