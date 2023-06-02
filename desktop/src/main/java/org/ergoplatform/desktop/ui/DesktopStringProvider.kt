package org.ergoplatform.desktop.ui

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.uilogic.StringProvider
import java.util.*

class DesktopStringProvider(private val i18NBundle: I18NBundle): StringProvider {
    override fun getString(stringId: String): String = i18NBundle.get(stringId)

    override fun getString(stringId: String, vararg formatArgs: Any): String {
        return i18NBundle.format(stringId, *formatArgs)
    }

    override val locale: Locale
        get() = i18NBundle.locale
}