package org.ergoplatform.android.transactions

import StageConstants
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.descendants
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.integration.android.IntentIntegrator
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.ergoplatform.android.*
import org.ergoplatform.android.databinding.FragmentSendFundsBinding
import org.ergoplatform.android.databinding.FragmentSendFundsTokenItemBinding
import org.ergoplatform.android.ui.*
import org.ergoplatform.android.wallet.WalletTokenDbEntity
import org.ergoplatform.android.wallet.addresses.AddressChooserCallback
import org.ergoplatform.android.wallet.addresses.ChooseAddressListDialogFragment
import org.ergoplatform.android.wallet.addresses.getAddressLabel
import org.ergoplatform.android.wallet.getNumOfAddresses
import kotlin.math.max


/**
 * Here's the place to send transactions
 */
class SendFundsFragmentDialog : FullScreenFragmentDialog(), PasswordDialogCallback,
    AddressChooserCallback {
    private var _binding: FragmentSendFundsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SendFundsViewModel
    private val args: SendFundsFragmentDialogArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel =
            ViewModelProvider(this).get(SendFundsViewModel::class.java)

        // Inflate the layout for this fragment
        _binding = FragmentSendFundsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.initWallet(
            requireContext(),
            args.walletId,
            args.derivationIdx,
            args.paymentRequest
        )

        // Add observers
        viewModel.walletName.observe(viewLifecycleOwner, {
            binding.walletName.text = getString(R.string.label_send_from, it)
        })
        viewModel.address.observe(viewLifecycleOwner, {
            binding.addressLabel.text = it?.getAddressLabel(requireContext()) ?: getString(
                R.string.label_all_addresses,
                viewModel.wallet?.getNumOfAddresses()
            )
        })
        viewModel.walletBalance.observe(viewLifecycleOwner, {
            binding.tvBalance.text = getString(
                R.string.label_wallet_balance,
                it.toStringWithScale(4)
            )
        })
        viewModel.feeAmount.observe(viewLifecycleOwner, {
            binding.tvFee.text = getString(
                R.string.desc_fee,
                it.toStringWithScale(4)
            )
        })
        viewModel.grossAmount.observe(viewLifecycleOwner, {
            binding.grossAmount.amount = it.toDouble()
            val nodeConnector = NodeConnector.getInstance()
            binding.tvFiat.visibility =
                if (nodeConnector.fiatCurrency.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvFiat.setText(
                getString(
                    R.string.label_fiat_amount,
                    formatFiatToString(
                        viewModel.amountToSend.toDouble() * (nodeConnector.fiatValue.value
                            ?: 0f).toDouble(),
                        nodeConnector.fiatCurrency, requireContext()
                    ),
                )
            )
        })
        viewModel.tokensChosenLiveData.observe(viewLifecycleOwner, {
            refreshTokensList()
        })
        viewModel.lockInterface.observe(viewLifecycleOwner, {
            binding.lockProgress.visibility = if (it) View.VISIBLE else View.GONE
            dialog?.setCancelable(!it)
        })
        viewModel.paymentDoneLiveData.observe(viewLifecycleOwner, {
            if (!it.success) {
                val snackbar = Snackbar.make(
                    requireView(),
                    R.string.error_transaction,
                    Snackbar.LENGTH_LONG
                )
                it.errorMsg?.let { errorMsg ->
                    snackbar.setAction(
                        R.string.label_details
                    ) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage(errorMsg)
                            .setPositiveButton(R.string.button_copy) { _, _ ->
                                val clipboard = ContextCompat.getSystemService(
                                    requireContext(),
                                    ClipboardManager::class.java
                                )
                                val clip = ClipData.newPlainText("", errorMsg)
                                clipboard?.setPrimaryClip(clip)
                            }
                            .setNegativeButton(R.string.label_dismiss, null)
                            .show()
                    }
                }
                snackbar.show()
            }
        })
        viewModel.txId.observe(viewLifecycleOwner, {
            binding.cardviewTxEdit.visibility = View.GONE
            binding.cardviewTxDone.visibility = View.VISIBLE
            binding.labelTxId.text = it
        })

        // Add click listeners
        binding.addressLabel.setOnClickListener {
            viewModel.wallet?.let { wallet ->
                ChooseAddressListDialogFragment.newInstance(
                    wallet.walletConfig.id, true
                ).show(childFragmentManager, null)
            }
        }
        binding.buttonShareTx.setOnClickListener {
            val txUrl =
                StageConstants.EXPLORER_WEB_ADDRESS + "en/transactions/" + binding.labelTxId.text.toString()
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, txUrl)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
        binding.buttonDismiss.setOnClickListener { dismiss() }

        binding.buttonSend.setOnClickListener {
            startPayment()
        }

        binding.buttonScan.setOnClickListener {
            IntentIntegrator.forSupportFragment(this).initiateScan(setOf(IntentIntegrator.QR_CODE))
        }
        binding.buttonAddToken.setOnClickListener {
            ChooseTokenListDialogFragment().show(childFragmentManager, null)
        }
        binding.amount.setEndIconOnClickListener {
            setAmountEdittext(
                ErgoAmount(
                    max(
                        0,
                        (viewModel.walletBalance.value?.nanoErgs ?: 0)
                                - (viewModel.feeAmount.value?.nanoErgs ?: 0)
                    )
                )
            )
        }

        // Init other stuff
        binding.tvReceiver.editText?.setText(viewModel.receiverAddress)
        if (viewModel.amountToSend.nanoErgs > 0) {
            setAmountEdittext(viewModel.amountToSend)
        }

        binding.amount.editText?.addTextChangedListener(MyTextWatcher(binding.amount))
        binding.tvReceiver.editText?.addTextChangedListener(MyTextWatcher(binding.tvReceiver))

        // this triggers an automatic scroll so the amount field is visible when soft keyboard is
        // opened or when amount edittext gets focus
        binding.amount.editText?.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus)
                ensureAmountVisibleDelayed()
        }
        KeyboardVisibilityEvent.setEventListener(
            requireActivity(),
            viewLifecycleOwner,
            { keyboardOpen ->
                if (keyboardOpen && binding.amount.editText?.hasFocus() == true) {
                    ensureAmountVisibleDelayed()
                }
            })
    }

    private fun ensureAmountVisibleDelayed() {
        // delay 200 to make sure that smart keyboard is already open
        Handler().postDelayed(
            { _binding?.let { it.scrollView.smoothScrollTo(0, it.amount.top) } },
            200
        )
    }

    override fun onAddressChosen(addressDerivationIdx: Int?) {
        viewModel.derivedAddressIdx = addressDerivationIdx
    }

    private fun refreshTokensList() {
        val tokensAvail = viewModel.tokensAvail
        val tokensChosen = viewModel.tokensChosen

        binding.buttonAddToken.visibility =
            if (tokensAvail.size > tokensChosen.size) View.VISIBLE else View.INVISIBLE
        binding.labelTokenAmountError.visibility = View.GONE
        binding.tokensList.apply {
            this.visibility =
                if (tokensChosen.isNotEmpty()) View.VISIBLE else View.GONE
            this.removeAllViews()
            tokensChosen.forEach {
                val ergoId = it.key
                tokensAvail.filter { it.tokenId.equals(ergoId) }
                    .firstOrNull()?.let { tokenDbEntity ->
                        val itemBinding =
                            FragmentSendFundsTokenItemBinding.inflate(layoutInflater, this, true)
                        itemBinding.tvTokenName.text = tokenDbEntity.name
                        itemBinding.inputTokenAmount.inputType =
                            if (tokenDbEntity.decimals!! > 0) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                            else InputType.TYPE_CLASS_NUMBER
                        itemBinding.inputTokenAmount.addTextChangedListener(
                            TokenAmountWatcher(tokenDbEntity)
                        )
                        itemBinding.inputTokenAmount.setText(
                            tokenAmountToText(it.value.value, tokenDbEntity.decimals)
                        )
                        itemBinding.buttonTokenRemove.setOnClickListener {
                            if (itemBinding.inputTokenAmount.text.isEmpty()) {
                                viewModel.removeToken(ergoId)
                            } else {
                                itemBinding.inputTokenAmount.text = null
                            }
                        }
                        itemBinding.buttonTokenAll.setOnClickListener {
                            itemBinding.inputTokenAmount.setText(
                                tokenAmountToText(tokenDbEntity.amount!!, tokenDbEntity.decimals)
                            )
                        }
                    }
            }
        }
    }

    private fun tokenAmountToText(amount: Long, decimals: Int) =
        if (amount > 0)
            formatTokenAmounts(
                amount,
                decimals
            ).replace(",", "")
        else ""

    private fun setAmountEdittext(amountToSend: ErgoAmount) {
        binding.amount.editText?.setText(amountToSend.toString())
    }

    private fun startPayment() {
        if (!viewModel.checkReceiverAddress()) {
            binding.tvReceiver.error = getString(R.string.error_receiver_address)
            binding.tvReceiver.editText?.requestFocus()
        } else if (!viewModel.checkAmount()) {
            binding.amount.error = getString(R.string.error_amount)
            binding.amount.editText?.requestFocus()
        } else if (!viewModel.checkTokens()) {
            binding.labelTokenAmountError.visibility = View.VISIBLE
            binding.tokensList.descendants.filter { it is EditText && it.text.isEmpty() }
                .firstOrNull()
                ?.requestFocus()
        } else {
            viewModel.preparePayment(this)
        }
    }

    override fun onPasswordEntered(password: String?): String? {
        password?.let {
            val success = viewModel.startPaymentWithPassword(password, requireContext())
            if (!success) {
                return getString(R.string.error_password_wrong)
            } else
            // okay, transaction is started. ViewModel will handle waiting dialog for us
                return null
        }
        return getString(R.string.error_password_empty)
    }

    fun showBiometricPrompt() {
        // setDeviceCredentialAllowed is deprecated on API 29, but needed for older levels
        @Suppress("DEPRECATION") val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.title_authenticate))
            .setConfirmationRequired(true) // don't send funds immediately when face is recognized
            .setDeviceCredentialAllowed(true)
            .build()

        val context = requireContext()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    viewModel.startPaymentUserAuth(context)
                } catch (t: Throwable) {
                    hideForcedSoftKeyboard(context, binding.amount.editText!!)
                    Snackbar.make(
                        requireView(),
                        getString(R.string.error_device_security, t.message),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                hideForcedSoftKeyboard(context, binding.amount.editText!!)
                Snackbar.make(
                    requireView(),
                    getString(R.string.error_device_security, errString),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        BiometricPrompt(this, callback).authenticate(promptInfo)
    }

    private fun inputChangesToViewModel() {
        viewModel.receiverAddress = binding.tvReceiver.editText?.text?.toString() ?: ""

        val amountStr = binding.amount.editText?.text.toString()
        viewModel.amountToSend = amountStr.toErgoAmount() ?: ErgoAmount.ZERO
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let {
                val content = parseContentFromQrCode(it)
                content?.let {
                    binding.tvReceiver.editText?.setText(content.address)
                    content.amount.let { amount -> if (amount.nanoErgs > 0) setAmountEdittext(amount) }
                    viewModel.addTokensFromQr(content.tokens)
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class MyTextWatcher(private val textInputLayout: TextInputLayout) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            textInputLayout.error = null
            inputChangesToViewModel()
        }

    }

    inner class TokenAmountWatcher(private val token: WalletTokenDbEntity) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            viewModel.setTokenAmount(
                token.tokenId!!,
                doubleToLongWithDecimals(inputTextToDouble(s?.toString()), token.decimals!!)
            )
            binding.labelTokenAmountError.visibility = View.GONE
        }

    }
}