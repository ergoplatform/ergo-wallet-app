package org.ergoplatform.android.ergoauth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.zxing.integration.android.IntentIntegrator
import org.ergoplatform.SigningSecrets
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentErgoAuthenticationBinding
import org.ergoplatform.android.transactions.ISigningPromptDialogParent
import org.ergoplatform.android.transactions.SigningPromptDialogFragment
import org.ergoplatform.android.ui.*
import org.ergoplatform.android.wallet.ChooseWalletListBottomSheetDialog
import org.ergoplatform.android.wallet.WalletChooserCallback
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.transactions.QR_DATA_LENGTH_LIMIT
import org.ergoplatform.transactions.QR_DATA_LENGTH_LOW_RES
import org.ergoplatform.transactions.ergoAuthResponseToQrChunks
import org.ergoplatform.uilogic.ergoauth.ErgoAuthUiLogic
import org.ergoplatform.uilogic.transactions.SigningPromptDialogDataSource
import org.ergoplatform.wallet.isReadOnly

class ErgoAuthenticationFragment : AbstractAuthenticationFragment(), WalletChooserCallback,
    ISigningPromptDialogParent {
    private var _binding: FragmentErgoAuthenticationBinding? = null
    private val binding: FragmentErgoAuthenticationBinding get() = _binding!!

    private val args: ErgoAuthenticationFragmentArgs by navArgs()
    private val viewModel: ErgoAuthenticationViewModel by viewModels()

    private var scaleDown = false

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
                if (state == ErgoAuthUiLogic.State.DONE && viewModel.uiLogic.coldSerializedAuthResponse == null)
                    View.VISIBLE else View.GONE
            binding.cardSigningResult.root.visibility =
                if (state == ErgoAuthUiLogic.State.DONE && viewModel.uiLogic.coldSerializedAuthResponse != null)
                    View.VISIBLE else View.GONE
            binding.layoutAuthenticate.visibility =
                if (state == ErgoAuthUiLogic.State.WAIT_FOR_AUTH) View.VISIBLE else View.GONE
            binding.cardScanMore.root.visibility =
                if (state == ErgoAuthUiLogic.State.SCANNING) View.VISIBLE else View.GONE

            if (state == ErgoAuthUiLogic.State.DONE) {
                refreshDoneScreen()
                if (viewModel.uiLogic.getDoneSeverity() != MessageSeverity.ERROR)
                    parentFragmentManager.setFragmentResult(
                        ergoAuthActionRequestKey, bundleOf(
                            ergoAuthActionCompletedBundleKey to true
                        )
                    )
            } else if (state == ErgoAuthUiLogic.State.WAIT_FOR_AUTH)
                refreshAuthPrompt()
            else if (state == ErgoAuthUiLogic.State.SCANNING)
                refreshScanMoreCardInfo(
                    binding.cardScanMore,
                    viewModel.uiLogic.requestPagesCollector!!,
                    viewModel.uiLogic.lastMessage
                )
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

        setupSigningResultCardBinding(
            binding.cardSigningResult,
            onSwitchRes = {
                scaleDown = !scaleDown
                setResultQrCodeData()
            },
            lastPageDesc = R.string.desc_response_cold_auth,
            pageDesc = R.string.desc_response_cold_auth_multiple,
        )

        binding.cardScanMore.buttonScanMore.setOnClickListener {
            QrScannerActivity.startFromFragment(this)
        }
    }

    private fun setResultQrCodeData() {
        viewModel.uiLogic.coldSerializedAuthResponse?.let {
            binding.cardSigningResult.qrCodePager.adapter = QrPagerAdapter(
                ergoAuthResponseToQrChunks(
                    it,
                    if (scaleDown) QR_DATA_LENGTH_LOW_RES else QR_DATA_LENGTH_LIMIT
                )
            )
            binding.cardSigningResult.refreshButtonState()
        }
    }

    override fun startAuthFlow() {
        if (authenticationWalletConfig?.isReadOnly() != false) {
            SigningPromptDialogFragment().show(childFragmentManager, null)
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
        setResultQrCodeData()
    }

    private fun refreshWalletLabel() {
        binding.walletLabel.text = viewModel.uiLogic.walletConfig?.displayName
    }

    override fun proceedFromAuthFlow(secrets: SigningSecrets) {
        viewModel.uiLogic.startResponse(secrets, AndroidStringProvider(requireContext()))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let {
                viewModel.uiLogic.addRequestQrPage(it, AndroidStringProvider(requireContext()))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSigningPromptResponseScanComplete() {
        viewModel.uiLogic.startResponseFromCold(AndroidStringProvider(requireContext()))
    }

    override val signingPromptDataSource: SigningPromptDialogDataSource
        get() = viewModel.uiLogic.signingPromptDialogConfig

    companion object {
        val ergoAuthActionRequestKey = "KEY_ERGOAUTH_FRAGMENT"
        val ergoAuthActionCompletedBundleKey = "KEY_ERGOAUTH_COMPLETED"
    }
}