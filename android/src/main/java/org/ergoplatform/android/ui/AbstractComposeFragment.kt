package org.ergoplatform.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment

abstract class AbstractComposeFragment : Fragment() {
    // TODO 167 needs title bar and centering of compose view, AppProgressIndicator is placed wrong

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
                    FragmentContent()
                }
            }
        })

        return nestedScroll
    }

    @Composable
    abstract fun FragmentContent()
}