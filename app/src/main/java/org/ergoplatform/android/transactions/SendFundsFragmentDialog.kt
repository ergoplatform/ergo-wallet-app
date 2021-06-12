package org.ergoplatform.android.transactions

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentSendFundsBinding
import org.ergoplatform.android.formatErgsToString
import org.ergoplatform.android.ui.FullScreenFragmentDialog
import org.ergoplatform.android.ui.PasswordDialogCallback
import org.ergoplatform.android.ui.inputTextToFloat

/**
 * Here's the place to send transactions
 */
class SendFundsFragmentDialog : FullScreenFragmentDialog(), PasswordDialogCallback {
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

        viewModel.initWallet(requireContext(), args.walletId)

        viewModel.walletName.observe(viewLifecycleOwner, { binding.walletName.text = getString(R.string.label_send_from, it) })
        viewModel.feeAmount.observe(viewLifecycleOwner, {
            binding.tvFee.text = getString(
                R.string.desc_fee,
                formatErgsToString(
                    it,
                    requireContext()
                )

            )
        })
        viewModel.grossAmount.observe(viewLifecycleOwner, { binding.grossAmount.amount = it })
        viewModel.lockInterface.observe(viewLifecycleOwner, {
            binding.lockProgress.visibility = if (it) View.VISIBLE else View.GONE
            dialog?.setCancelable(!it)
        })
        viewModel.paymentDoneLiveData.observe(viewLifecycleOwner, {
            if (it == PaymentResult.ERROR) {
                Snackbar.make(
                    requireView(),
                    R.string.error_transaction,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        })
        viewModel.txId.observe(viewLifecycleOwner, {
            binding.cardviewTxEdit.visibility = View.GONE
            binding.cardviewTxDone.visibility = View.VISIBLE
            binding.labelTxId.text = it
        })
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

        binding.tvReceiver.editText?.setText(viewModel.receiverAddress)
        if (viewModel.amountToSend > 0) {
            binding.amount.editText?.setText(
                formatErgsToString(
                    viewModel.amountToSend,
                    requireContext()
                )
            )
        }

        binding.amount.editText?.addTextChangedListener(MyTextWatcher(binding.amount))
        binding.tvReceiver.editText?.addTextChangedListener(MyTextWatcher(binding.tvReceiver))

    }

    private fun startPayment() {
        if (!viewModel.checkReceiverAddress()) {
            binding.tvReceiver.error = getString(R.string.error_receiver_address)
            binding.tvReceiver.editText?.requestFocus()
        } else if (!viewModel.checkAmount()) {
            binding.amount.error = getString(R.string.error_amount)
            binding.amount.editText?.requestFocus()
        } else {
            viewModel.preparePayment(this)
        }
    }

    override fun onPasswordEntered(password: String?): String? {
        password?.let {
            val success = viewModel.startPaymentWithPassword(password)
            if (!success) {
                return getString(R.string.error_password_wrong)
            } else
            // okay, transaction is started. ViewModel will handle waiting dialog for us
                return null
        }
        return getString(R.string.error_password_empty)
    }

    private fun inputChangesToViewModel() {
        viewModel.receiverAddress = binding.tvReceiver.editText?.text?.toString() ?: ""

        val amountStr = binding.amount.editText?.text.toString()
        viewModel.amountToSend = inputTextToFloat(amountStr)

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
}