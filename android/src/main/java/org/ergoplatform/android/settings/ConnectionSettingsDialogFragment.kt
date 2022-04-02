package org.ergoplatform.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.ErgoApiService
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.databinding.FragmentConnectionSettingsBinding
import org.ergoplatform.getDefaultExplorerApiUrl

/**
 *
 * Shows connection settings for Explorer API URL and Node URL
 */
class ConnectionSettingsDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentConnectionSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentConnectionSettingsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferences = Preferences(requireContext())
        binding.editNodeUrl.editText?.setText(preferences.prefNodeUrl)
        binding.editExplorerApiUrl.editText?.setText(preferences.prefExplorerApiUrl)
        binding.inputIpfsGateway.setText(preferences.prefIpfsGatewayUrl)
        binding.buttonApply.setOnClickListener { buttonApply() }
        binding.inputIpfsGateway.setOnEditorActionListener { _, _, _ ->
            buttonApply()
            true
        }
        binding.buttonDefaults.setOnClickListener {
            binding.editNodeUrl.editText?.setText(preferences.getDefaultNodeApiUrl())
            binding.editExplorerApiUrl.editText?.setText(getDefaultExplorerApiUrl())
            binding.inputIpfsGateway.setText(preferences.defaultIpfsGatewayUrl)
        }
    }

    private fun buttonApply() {
        val nodeUrl = binding.editNodeUrl.editText?.text?.toString() ?: ""
        val explorerApiUrl = binding.editExplorerApiUrl.editText?.text?.toString() ?: ""

        val preferences = Preferences(requireContext())
        preferences.prefExplorerApiUrl = explorerApiUrl
        preferences.prefNodeUrl = nodeUrl
        preferences.prefIpfsGatewayUrl = binding.inputIpfsGateway.text?.toString() ?: ""

        // reset api service of NodeConnector to load new settings
        ErgoApiService.resetApiService()

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}