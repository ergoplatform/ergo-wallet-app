package org.ergoplatform.android.transactions

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.NodeConnector
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentReceiveToWalletBinding
import org.ergoplatform.android.ui.*
import org.ergoplatform.android.wallet.WalletDbEntity
import org.ergoplatform.android.wallet.addresses.AddressChooserCallback
import org.ergoplatform.android.wallet.addresses.ChooseAddressListDialogFragment
import org.ergoplatform.android.wallet.getDerivedAddress
import org.ergoplatform.android.wallet.getDerivedAddressEntity
import org.ergoplatform.getExplorerPaymentRequestAddress
import org.ergoplatform.wallet.addresses.getAddressLabel


/**
 * Shows information and QR for receiving funds to a wallet
 */
class ReceiveToWalletFragment : Fragment(), AddressChooserCallback {

    private var _binding: FragmentReceiveToWalletBinding? = null
    private val binding get() = _binding!!

    private val args: ReceiveToWalletFragmentArgs by navArgs()
    private var derivationIdx: Int = 0
    private var wallet: WalletDbEntity? = null

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

        derivationIdx = args.derivationIdx

        lifecycleScope.launch {
            val walletDao = AppDatabase.getInstance(requireContext()).walletDao()
            wallet = walletDao.loadWalletWithStateById(args.walletId)

            wallet?.let { wallet ->
                binding.walletName.text = wallet.walletConfig.displayName
                refreshAddressInformation()
            }
        }

        binding.buttonCopy.setOnClickListener {
            wallet?.getDerivedAddress(derivationIdx)?.let {
                copyStringToClipboard(it, requireContext(), requireView())
            }
        }
        binding.addressLabel.setOnClickListener {
            wallet?.let { wallet ->
                ChooseAddressListDialogFragment.newInstance(
                    wallet.walletConfig.id
                ).show(childFragmentManager, null)
            }
        }
    }

    override fun onAddressChosen(addressDerivationIdx: Int?) {
        derivationIdx = addressDerivationIdx ?: 0
        refreshAddressInformation()
    }

    private fun refreshAddressInformation() {
        val address = wallet?.getDerivedAddressEntity(derivationIdx)
        binding.addressLabel.text = address?.getAddressLabel(AndroidStringProvider(requireContext()))
        binding.publicAddress.text = address?.publicAddress

        refreshQrCode()
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
                        getInputAmount() * nodeConnector.fiatValue.value.toDouble(),
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