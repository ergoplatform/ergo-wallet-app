package org.ergoplatform

import org.ergoplatform.uilogic.StringProvider

class TestStringProvider: StringProvider {
    override fun getString(stringId: String): String {
        return stringId
    }

    override fun getString(stringId: String, vararg formatArgs: Any): String {
        return stringId
    }
}