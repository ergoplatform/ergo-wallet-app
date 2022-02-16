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
import org.ergoplatform.android.R
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.android.databinding.FragmentReceiveToWalletBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.copyStringToClipboard
import org.ergoplatform.android.ui.setQrCodeToImageView
import org.ergoplatform.android.wallet.addresses.AddressChooserCallback
import org.ergoplatform.android.wallet.addresses.ChooseAddressListDialogFragment
import org.ergoplatform.uilogic.wallet.ReceiveToWalletUiLogic
import org.ergoplatform.utils.inputTextToDouble
import org.ergoplatform.wallet.addresses.getAddressLabel


/**
 * Shows information and QR for receiving funds to a wallet
 */
class ReceiveToWalletFragment : Fragment(), AddressChooserCallback {

    private var _binding: FragmentReceiveToWalletBinding? = null
    private val binding get() = _binding!!

    private val args: ReceiveToWalletFragmentArgs by navArgs()
    val uiLogic = ReceiveToWalletUiLogic()

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

        uiLogic.derivationIdx = args.derivationIdx
        viewLifecycleOwner.lifecycleScope.launch {
            uiLogic.loadWallet(
                args.walletId,
                RoomWalletDbProvider(AppDatabase.getInstance(requireContext()))
            )
            uiLogic.wallet?.let { wallet ->
                binding.walletName.text = wallet.walletConfig.displayName
                refreshAddressInformation()
            }
        }

        binding.buttonCopy.setOnClickListener {
            uiLogic.address?.publicAddress?.let {
                copyStringToClipboard(it, requireContext(), requireView())
            }
        }
        binding.addressLabel.setOnClickListener {
            uiLogic.wallet?.let { wallet ->
                ChooseAddressListDialogFragment.newInstance(
                    wallet.walletConfig.id
                ).show(childFragmentManager, null)
            }
        }
    }

    override fun onAddressChosen(addressDerivationIdx: Int?) {
        uiLogic.derivationIdx = addressDerivationIdx ?: 0
        refreshAddressInformation()
    }

    private fun refreshAddressInformation() {
        val address = uiLogic.address
        binding.addressLabel.text =
            address?.getAddressLabel(AndroidStringProvider(requireContext()))
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

    private fun getTextToShare() =
        uiLogic.getTextToShare(getInputAmount(), binding.purpose.editText?.text.toString())

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
            val fiatString = uiLogic.getFiatAmount(
                getInputAmount(), AndroidStringProvider(requireContext())
            )

            binding.tvFiat.visibility = if (!fiatString.isNullOrEmpty()) View.VISIBLE else View.GONE
            binding.tvFiat.text = fiatString
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}