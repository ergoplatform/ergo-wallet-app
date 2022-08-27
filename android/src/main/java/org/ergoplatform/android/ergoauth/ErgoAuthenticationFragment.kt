package org.ergoplatform.android.ergoauth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.ergoplatform.SigningSecrets
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentErgoAuthenticationBinding
import org.ergoplatform.android.ui.AbstractAuthenticationFragment
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.getSeverityDrawableResId
import org.ergoplatform.android.wallet.ChooseWalletListBottomSheetDialog
import org.ergoplatform.android.wallet.WalletChooserCallback
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.uilogic.ergoauth.ErgoAuthUiLogic
import org.ergoplatform.wallet.isReadOnly

class ErgoAuthenticationFragment : AbstractAuthenticationFragment(), WalletChooserCallback {
    private var _binding: FragmentErgoAuthenticationBinding? = null
    private val binding: FragmentErgoAuthenticationBinding get() = _binding!!

    private val args: ErgoAuthenticationFragmentArgs by navArgs()
    private val viewModel: ErgoAuthenticationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentErgoAuthenticationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        viewModel.uiLogic.init(
            args.ergoAuthUrl,
            args.walletId,
            AndroidStringProvider(context),
            AppDatabase.getInstance(context)
        )

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.layoutProgress.visibility =
                if (state == ErgoAuthUiLogic.State.FETCHING_DATA) View.VISIBLE else View.GONE
            binding.layoutDoneInfo.visibility =
                if (state == ErgoAuthUiLogic.State.DONE) View.VISIBLE else View.GONE
            binding.layoutAuthenticate.visibility =
                if (state == ErgoAuthUiLogic.State.WAIT_FOR_AUTH) View.VISIBLE else View.GONE

            if (state == ErgoAuthUiLogic.State.DONE) {
                refreshDoneScreen()
                parentFragmentManager.setFragmentResult(
                    ergoAuthActionRequestKey, bundleOf(
                        ergoAuthActionCompletedBundleKey to true
                    )
                )
            } else if (state == ErgoAuthUiLogic.State.WAIT_FOR_AUTH)
                refreshAuthPrompt()
        }

        binding.buttonDismiss.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.walletLabel.setOnClickListener {
            ChooseWalletListBottomSheetDialog().show(childFragmentManager, null)
        }
        binding.buttonAuthenticate.setOnClickListener {
            startAuthFlow()
        }
    }

    override fun startAuthFlow() {
        if (authenticationWalletConfig?.isReadOnly() != false) {
            // read only wallet not supported (yet)
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.error_wallet_type_ergoauth_not_avail)
                .setPositiveButton(R.string.zxing_button_ok, null)
                .show()
        } else {
            super.startAuthFlow()
        }
    }

    override val authenticationWalletConfig: WalletConfig?
        get() = viewModel.uiLogic.walletConfig

    override fun onWalletChosen(walletConfig: WalletConfig) {
        viewModel.uiLogic.walletConfig = walletConfig
        refreshWalletLabel()
    }

    private fun refreshAuthPrompt() {
        refreshWalletLabel()
        val uiLogic = viewModel.uiLogic
        binding.layoutAuthMessage.visibility = View.VISIBLE
        binding.tvAuthMessage.text =
            uiLogic.getAuthenticationMessage(AndroidStringProvider(requireContext()))
        val severityResId =
            (uiLogic.ergAuthRequest?.messageSeverity
                ?: MessageSeverity.NONE).getSeverityDrawableResId()
        binding.imageAuthMessage.setImageResource(severityResId)
        binding.imageAuthMessage.visibility =
            if (severityResId == 0) View.GONE else View.VISIBLE

    }

    private fun refreshDoneScreen() {
        val uiLogic = viewModel.uiLogic
        binding.tvMessage.text = uiLogic.getDoneMessage(AndroidStringProvider(requireContext()))
        binding.tvMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            uiLogic.getDoneSeverity().getSeverityDrawableResId(),
            0, 0
        )
    }

    private fun refreshWalletLabel() {
        binding.walletLabel.text = viewModel.uiLogic.walletConfig?.displayName
    }

    override fun proceedFromAuthFlow(secrets: SigningSecrets) {
        viewModel.uiLogic.startResponse(secrets, AndroidStringProvider(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        val ergoAuthActionRequestKey = "KEY_ERGOAUTH_FRAGMENT"
        val ergoAuthActionCompletedBundleKey = "KEY_ERGOAUTH_COMPLETED"
    }
}