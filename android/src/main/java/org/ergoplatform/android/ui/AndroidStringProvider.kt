package org.ergoplatform.android.ui

import android.content.Context
import android.content.res.Resources
import org.ergoplatform.uilogic.StringProvider

class AndroidStringProvider(val context: Context) : StringProvider {
    override fun getString(stringId: String): String {
        val resources = context.resources
        return resources.getString(getAndroidStringId(resources, stringId))
    }

    private fun getAndroidStringId(resources: Resources, stringId: String) =
        resources.getIdentifier(stringId, "string", context.packageName)

    override fun getString(stringId: String, vararg formatArgs: Any): String {
        val resources = context.resources
        return resources.getString(getAndroidStringId(resources, stringId), *formatArgs)
    }
}