package org.ergoplatform

import org.ergoplatform.uilogic.StringProvider
import java.util.*

class TestStringProvider: StringProvider {
    override fun getString(stringId: String): String {
        return stringId
    }

    override fun getString(stringId: String, vararg formatArgs: Any): String {
        return stringId
    }

    override val locale: Locale
        get() = Locale.getDefault()
}