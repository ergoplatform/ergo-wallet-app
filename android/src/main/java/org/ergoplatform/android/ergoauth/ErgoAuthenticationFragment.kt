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

        viewModel.authRequest.observe(viewLifecycleOwner) { authRequest ->
            binding.layoutProgress.visibility = View.GONE
            binding.layoutDoneInfo.visibility = if (authRequest == null) View.VISIBLE else View.GONE
            binding.layoutAuthenticate.visibility =
                if (authRequest != null) View.VISIBLE else View.GONE

            if (authRequest == null)
                refreshDoneScreen()
            else
                refreshAuthPrompt()
        }

        binding.buttonDismiss.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.walletLabel.setOnClickListener {
            ChooseWalletListBottomSheetDialog().show(childFragmentManager, null)
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
        TODO("Not yet implemented")
    }

    override fun proceedAuthFlowWithPassword(password: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}