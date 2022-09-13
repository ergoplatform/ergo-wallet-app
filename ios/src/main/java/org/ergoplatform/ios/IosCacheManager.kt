package org.ergoplatform.ios

import org.ergoplatform.persistance.AbstractCacheFileManager
import org.robovm.apple.foundation.NSFileManager
import org.robovm.apple.foundation.NSSearchPathDirectory
import org.robovm.apple.foundation.NSSearchPathDomainMask
import java.io.File

class IosCacheManager : AbstractCacheFileManager(
    File(
        NSFileManager.getDefaultManager()
            .getURLsForDirectory(NSSearchPathDirectory.CachesDirectory, NSSearchPathDomainMask.AllDomainsMask)
            .first().path
    )
)