package org.ergoplatform.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.ergoplatform.android.databinding.FragmentComposeBinding

abstract class AbstractComposeFragment : Fragment() {
    private var _binding: FragmentComposeBinding? = null
    protected val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComposeBinding.inflate(inflater)
        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppComposeTheme {
                    FragmentContent()
                }
            }
        }

        return binding.root
    }

    @Composable
    abstract fun FragmentContent()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}