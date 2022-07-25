package org.ergoplatform.android.persistence

import android.content.Context
import org.ergoplatform.persistance.AbstractCacheFileManager

class AndroidCacheFiles(context: Context) : AbstractCacheFileManager(context.cacheDir)