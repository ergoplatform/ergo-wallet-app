package org.ergoplatform.uilogic

interface StringProvider {
    fun getString(stringId: String): String
    fun getString(stringId: String, vararg formatArgs: Any): String
}