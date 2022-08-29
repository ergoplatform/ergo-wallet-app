package org.ergoplatform.android.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.uilogic.settings.SettingsUiLogic

class SettingsViewModel : ViewModel() {
    val uiLogic = SettingsUiLogic()

    fun startNodeDetection(prefs: PreferencesProvider) {
        viewModelScope.launch(Dispatchers.IO) { uiLogic.checkAvailableNodes(prefs) }
    }
}