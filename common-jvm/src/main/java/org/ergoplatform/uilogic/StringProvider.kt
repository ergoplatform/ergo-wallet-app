package org.ergoplatform.uilogic

import java.util.Locale

interface StringProvider {
    fun getString(stringId: String): String
    fun getString(stringId: String, vararg formatArgs: Any): String
    val locale: Locale
}