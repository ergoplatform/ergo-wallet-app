package org.ergoplatform.android.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.navGraphViewModels
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.ui.AbstractComposeBottomSheetDialogFragment
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.compose.settings.ConnectionSettingsLayout
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle

/**
 *
 * Shows connection settings for Explorer API URL and Node URL
 */
class ConnectionSettingsDialogFragment : AbstractComposeBottomSheetDialogFragment() {

    private val viewModel: SettingsViewModel by navGraphViewModels(R.id.navigation_settings)

    @Composable
    override fun DialogContent() {
        val context = LocalContext.current

        Column(Modifier.padding(defaultPadding)) {

            Text(
                text = stringResource(id = R.string.button_connection_settings),
                modifier = Modifier.padding(bottom = defaultPadding),
                style = labelStyle(LabelStyle.BODY1),
            )

            ConnectionSettingsLayout(
                viewModel.uiLogic,
                onStartNodeDetection = {
                    viewModel.startNodeDetection(Preferences(context))
                },
                preferences = Preferences(context),
                stringProvider = AndroidStringProvider(context),
                onDismissRequest = { dismiss() }
            )
        }
    }
}