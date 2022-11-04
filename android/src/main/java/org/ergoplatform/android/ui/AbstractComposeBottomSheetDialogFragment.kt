package org.ergoplatform.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class AbstractComposeBottomSheetDialogFragment: BottomSheetDialogFragment() {

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
                    DialogContent()
                }
            }
        })

        return nestedScroll
    }

    @Composable
    abstract fun DialogContent()
}