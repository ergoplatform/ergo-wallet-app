package org.ergoplatform.ios.ui

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.uilogic.StringProvider

class IosStringProvider(private val texts: I18NBundle): StringProvider {
    override fun getString(stringId: String): String = texts.get(stringId)

    override fun getString(stringId: String, vararg formatArgs: Any): String {
        return texts.format(stringId, *formatArgs)
    }
}