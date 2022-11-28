package org.ergoplatform.android.transactions

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import org.ergoplatform.SigningSecrets
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentColdWalletSigningBinding
import org.ergoplatform.android.ui.*
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.compose.transactions.SignTransactionInfoLayout
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.transactions.QR_DATA_LENGTH_LIMIT
import org.ergoplatform.transactions.QR_DATA_LENGTH_LOW_RES
import org.ergoplatform.transactions.coldSigningResponseToQrChunks

/**
 * Scans cold wallet signing request qr codes, signs the transaction, presents a qr code to go back
 */
class ColdWalletSigningFragment : AbstractAuthenticationFragment() {

    var _binding: FragmentColdWalletSigningBinding? = null
    val binding get() = _binding!!

    private val args: ColdWalletSigningFragmentArgs by navArgs()

    private var scaleDown = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColdWalletSigningBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    private val viewModel: ColdWalletSigningViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = viewModel

        addQrCodeChunk(args.qrCode)
        viewModel.setWalletId(args.walletId, requireContext())

        viewModel.lockInterface.observe(viewLifecycleOwner, {
            if (it)
                ProgressBottomSheetDialogFragment.showProgressDialog(childFragmentManager)
            else
                ProgressBottomSheetDialogFragment.dismissProgressDialog(childFragmentManager)
        })

        viewModel.signingResult.observe(viewLifecycleOwner) {
            if (it?.success == true && viewModel.signedQrCode != null) {
                binding.transactionInfo.visibility = View.GONE
                binding.cardSigningResult.root.visibility = View.VISIBLE
                binding.cardScanMore.root.visibility = View.GONE

                binding.cardSigningResult.switchResolution.visibility =
                    if (viewModel.signedQrCode!!.length > QR_DATA_LENGTH_LOW_RES) View.VISIBLE else View.GONE
                setQrData()

            } else {
                binding.cardSigningResult.root.visibility = View.GONE

                it?.let {
                    val snackbar = Snackbar.make(
                        requireView(),
                        R.string.error_prepare_transaction,
                        Snackbar.LENGTH_LONG
                    )
                    it.errorMsg?.let { errorMsg ->
                        snackbar.setAction(
                            R.string.label_details
                        ) {
                            showDialogWithCopyOption(requireContext(), errorMsg)
                        }
                    }
                    snackbar.setAnchorView(R.id.nav_view).show()
                }
            }
        }

        // Button click listeners
        binding.cardScanMore.buttonScanMore.setOnClickListener {
            IntentIntegrator.forSupportFragment(this).initiateScan(setOf(IntentIntegrator.QR_CODE))
        }

        setupSigningResultCardBinding(
            binding.cardSigningResult,
            onSwitchRes = {
                scaleDown = !scaleDown
                setQrData()
            },
        )
    }

    private fun setQrData() {
        viewModel.signedQrCode?.let {
            binding.cardSigningResult.qrCodePager.adapter = QrPagerAdapter(
                coldSigningResponseToQrChunks(
                    it,
                    if (scaleDown) QR_DATA_LENGTH_LOW_RES else QR_DATA_LENGTH_LIMIT
                )
            )
            binding.cardSigningResult.refreshButtonState()
        }
    }

    private fun addQrCodeChunk(qrCode: String?) {
        qrCode?.let {
            viewModel.uiLogic.addQrCodeChunk(
                qrCode,
                AndroidStringProvider(requireContext())
            )
        }

        val transactionInfo = viewModel.uiLogic.transactionInfo

        // don't show transaction info when we already have a signing result
        if (viewModel.signedQrCode != null)
            return

        if (transactionInfo == null) {
            // refresh information on scanned codes
            refreshScanMoreCardInfo(
                binding.cardScanMore,
                viewModel.uiLogic.qrPagesCollector,
                viewModel.uiLogic.lastErrorMessage
            )
        }

        transactionInfo?.let { ti ->
            binding.transactionInfo.visibility = View.VISIBLE
            binding.cardScanMore.root.visibility = View.GONE

            val context = requireContext()
            binding.tiComposeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AppComposeTheme {
                        SignTransactionInfoLayout(
                            modifier = Modifier.padding(defaultPadding),
                            origTransactionInfo = ti,
                            onConfirm = {
                                viewModel.wallet?.let {
                                    startAuthFlow()
                                }
                            },
                            onTokenClick = null,
                            texts = AndroidStringProvider(context),
                            getDb = { AppDatabase.getInstance(context) }
                        )
                    }
                }
            }
        }
    }

    override val authenticationWalletConfig: WalletConfig?
        get() = viewModel.wallet?.walletConfig

    override fun proceedFromAuthFlow(secrets: SigningSecrets) {
        viewModel.uiLogic.signTxWithMnemonicAsync(secrets, AndroidStringProvider(requireContext()))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let {
                addQrCodeChunk(it)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}