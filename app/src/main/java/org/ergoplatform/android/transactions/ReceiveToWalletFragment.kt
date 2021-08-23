package org.ergoplatform.android.transactions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.ergoplatform.android.*
import org.ergoplatform.android.databinding.FragmentReceiveToWalletBinding
import org.ergoplatform.android.ui.inputTextToDouble


/**
 * Shows information and QR for receiving funds to a wallet
 */
class ReceiveToWalletFragment : Fragment() {

    private var _binding: FragmentReceiveToWalletBinding? = null
    private val binding get() = _binding!!

    private val args: ReceiveToWalletFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentReceiveToWalletBinding.inflate(inflater, container, false)

        binding.amount.editText?.addTextChangedListener(MyTextWatcher())
        binding.purpose.editText?.addTextChangedListener(MyTextWatcher())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val wallet =
                AppDatabase.getInstance(requireContext()).walletDao().loadWalletById(args.walletId)

            wallet?.let {
                binding.publicAddress.text = wallet.firstAddress
                binding.walletName.text = wallet.displayName

                refreshQrCode()

                binding.buttonCopy.setOnClickListener {
                    val clipboard = getSystemService(
                        requireContext(),
                        ClipboardManager::class.java
                    )
                    val clip = ClipData.newPlainText("", wallet.firstAddress)
                    clipboard?.setPrimaryClip(clip)

                    Snackbar.make(requireView(), R.string.label_copied, Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.nav_view).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_receive_to_wallet, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_share) {

            getTextToShare()?.let {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, it)
                    type = "text/plain"
                }

                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }

            return true
        } else
            return super.onOptionsItemSelected(item)
    }

    private fun refreshQrCode() {
        getTextToShare()?.let {
            setQrCodeToImageView(
                binding.qrCode,
                it,
                400,
                400
            )
        }
    }

    private fun getTextToShare(): String? {
        binding.publicAddress.text?.let {
            val amountVal = getInputAmount()

            return getExplorerPaymentRequestAddress(
                it.toString(),
                amountVal,
                binding.purpose.editText?.text.toString()
            )
        }
        return null
    }

    private fun getInputAmount(): Double {
        val amountStr = binding.amount.editText?.text.toString()
        val amountVal = inputTextToDouble(amountStr)
        return amountVal
    }

    inner class MyTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            refreshQrCode()
            val nodeConnector = NodeConnector.getInstance()
            binding.tvFiat.visibility =
                if (nodeConnector.fiatCurrency.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvFiat.setText(
                getString(
                    R.string.label_fiat_amount,
                    formatFiatToString(
                        getInputAmount() * (nodeConnector.fiatValue.value ?: 0f).toDouble(),
                        nodeConnector.fiatCurrency, requireContext()
                    ),
                )
            )
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}