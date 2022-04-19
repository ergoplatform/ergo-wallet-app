package org.ergoplatform.android.ergoauth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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

            if (state == ErgoAuthUiLogic.State.DONE)
                refreshDoneScreen()
            else if (state == ErgoAuthUiLogic.State.WAIT_FOR_AUTH)
                refreshAuthPrompt()
        }

        binding.buttonDismiss.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.walletLabel.setOnClickListener {
            ChooseWalletListBottomSheetDialog().show(childFragmentManager, null)
        }
        binding.buttonAuthenticate.setOnClickListener {
            startAuthFlow(viewModel.uiLogic.walletConfig!!)
        }
    }

    override fun onWalletChosen(walletConfig: WalletConfig) {
        viewModel.uiLogic.walletConfig = walletConfig
        refreshWalletLabel()
    }

    private fun refreshAuthPrompt() {
        refreshWalletLabel()
        val uiLogic = viewModel.uiLogic
        binding.layoutAuthMessage.visibility = uiLogic.ergAuthRequest?.userMessage?.let {
            binding.tvAuthMessage.text = getString(R.string.label_message_from_dapp, it)
            val severityResId =
                (uiLogic.ergAuthRequest?.messageSeverity
                    ?: MessageSeverity.NONE).getSeverityDrawableResId()
            binding.imageAuthMessage.setImageResource(severityResId)
            binding.imageAuthMessage.visibility =
                if (severityResId == 0) View.GONE else View.VISIBLE
            View.VISIBLE
        } ?: View.GONE

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

    override fun proceedAuthFlowFromBiometrics() {
        viewModel.startAuthenticationFromBiometrics(requireContext())
    }

    override fun proceedAuthFlowWithPassword(password: String): Boolean {
        return viewModel.startAuthenticationFromPassword(
            password,
            AndroidStringProvider(requireContext())
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}