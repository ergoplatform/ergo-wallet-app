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
import androidx.core.widget.NestedScrollView
import androidx.navigation.navGraphViewModels
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

    private val viewModel: SettingsViewModel by navGraphViewModels(R.id.navigation_settings)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val nestedScroll = NestedScrollView(context)
        nestedScroll.addView(ComposeView(context).apply {
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
        })

        return nestedScroll
    }
}