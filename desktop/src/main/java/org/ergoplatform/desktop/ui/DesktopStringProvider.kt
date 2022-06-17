package org.ergoplatform.desktop.ui

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.uilogic.StringProvider

class DesktopStringProvider(val i18NBundle: I18NBundle): StringProvider {
    override fun getString(stringId: String): String = i18NBundle.get(stringId)

    override fun getString(stringId: String, vararg formatArgs: Any): String {
        return i18NBundle.format(stringId, *formatArgs)
    }
}