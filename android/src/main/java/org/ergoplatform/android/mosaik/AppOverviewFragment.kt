package org.ergoplatform.android.mosaik

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.ergoplatform.android.databinding.FragmentAppOverviewBinding
import org.ergoplatform.android.ui.hideForcedSoftKeyboard
import org.ergoplatform.android.ui.navigateSafe

class AppOverviewFragment : Fragment() {
    private var _binding: FragmentAppOverviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.inputAppUrl.setEndIconOnClickListener { navigateToApp() }
        binding.inputAppUrl.editText?.setOnEditorActionListener { _, _, _ ->
            navigateToApp()
            true
        }
    }

    private fun navigateToApp() {
        val url = binding.inputAppUrl.editText?.text.toString()

        if (url.isNotBlank())
            hideForcedSoftKeyboard(requireContext(), binding.inputAppUrl.editText!!)
            findNavController().navigateSafe(
                AppOverviewFragmentDirections.actionAppOverviewFragmentToMosaik(
                    url
                )
            )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}