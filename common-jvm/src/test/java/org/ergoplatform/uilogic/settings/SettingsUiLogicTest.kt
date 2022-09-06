package org.ergoplatform.uilogic.settings

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.ergoplatform.TestPreferencesProvider

class SettingsUiLogicTest: TestCase() {

    fun testCheckAvailableNodes() {
        runBlocking {
            val settingsUiLogic = SettingsUiLogic()
            settingsUiLogic.checkAvailableNodes(TestPreferencesProvider())
            println(settingsUiLogic.lastNodeList)
        }
    }
}