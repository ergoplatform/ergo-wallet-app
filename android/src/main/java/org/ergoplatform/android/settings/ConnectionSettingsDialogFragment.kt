package org.ergoplatform.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.AppComposeTheme
import org.ergoplatform.compose.settings.ConnectionSettingsLayout
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle

/**
 *
 * Shows connection settings for Explorer API URL and Node URL
 */
class ConnectionSettingsDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppComposeTheme {
                    Column(Modifier.padding(defaultPadding)) {

                        Text(
                            text = stringResource(id = R.string.button_connection_settings),
                            modifier = Modifier.padding(bottom = defaultPadding),
                            style = labelStyle(LabelStyle.BODY1),
                        )

                        ConnectionSettingsLayout(
                            preferences = Preferences(requireContext()),
                            stringProvider = AndroidStringProvider(requireContext()),
                            onDismissRequest = { dismiss() }
                        )
                    }
                }
            }
        }
    }
}