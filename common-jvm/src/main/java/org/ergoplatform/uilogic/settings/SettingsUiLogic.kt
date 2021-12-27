package org.ergoplatform.uilogic.settings

import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.uilogic.STRING_BUTTON_DISPLAY_CURRENCY
import org.ergoplatform.uilogic.STRING_LABEL_NONE
import org.ergoplatform.uilogic.StringProvider
import java.util.*

class SettingsUiLogic {
    fun getFiatCurrencyButtonText(preferences: PreferencesProvider, texts: StringProvider): String {
        val displayCurrency =
            preferences.prefDisplayCurrency.toUpperCase(Locale.getDefault())
        return texts.getString(
            STRING_BUTTON_DISPLAY_CURRENCY,
            if (displayCurrency.isNotEmpty()) displayCurrency else texts.getString(STRING_LABEL_NONE)
        )
    }
}