package org.ergoplatform.android.transactions

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.android.databinding.FragmentReceiveToWalletBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.copyStringToClipboard
import org.ergoplatform.android.ui.setQrCodeToImageView
import org.ergoplatform.android.ui.shareText
import org.ergoplatform.android.wallet.addresses.AddressChooserCallback
import org.ergoplatform.android.wallet.addresses.ChooseAddressListDialogFragment
import org.ergoplatform.uilogic.wallet.ReceiveToWalletUiLogic
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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val amountToReceive = uiLogic.amountToReceive

        if (Preferences(requireContext()).isSendInputFiatAmount != amountToReceive.inputIsFiat) {
            amountToReceive.switchInputAmountMode()
        }
        setInputAmountLabel()

        val myTextWatcher = MyTextWatcher()
        binding.amount.editText?.addTextChangedListener(myTextWatcher)
        binding.purpose.editText?.addTextChangedListener(myTextWatcher)
        myTextWatcher.afterTextChanged(null) // makes fiat label visible before first editing

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
        binding.tvFiat.setOnClickListener {
            val modeChanged = amountToReceive.switchInputAmountMode()
            if (modeChanged) {
                Preferences(requireContext()).isSendInputFiatAmount = amountToReceive.inputIsFiat
                binding.amount.editText?.setText(
                    amountToReceive.getInputAmountString()
                )
                setInputAmountLabel()
                val snackbar = Snackbar.make(
                    view,
                    if (amountToReceive.inputIsFiat) R.string.message_switched_input_mode_fiat
                    else R.string.message_switched_input_mode_erg,
                    Snackbar.LENGTH_SHORT
                )
                if (requireActivity().findViewById<View?>(R.id.nav_view)?.visibility == View.VISIBLE)
                    snackbar.setAnchorView(R.id.nav_view)

                snackbar.show()
            }
        }
    }

    private fun setInputAmountLabel() {
        binding.amount.hint = if (uiLogic.amountToReceive.inputIsFiat)
            getString(
                R.string.hint_amount_currency,
                WalletStateSyncManager.getInstance().fiatCurrency.uppercase()
            )
        else getString(R.string.label_amount)
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

            getTextToShare()?.let { shareText(it) }

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
        uiLogic.getTextToShare(binding.purpose.editText?.text.toString())

    inner class MyTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable?) {
            uiLogic.amountToReceive.inputAmountChanged(binding.amount.editText?.text.toString())
            refreshQrCode()
            val fiatString = uiLogic.getOtherCurrencyLabel(
                AndroidStringProvider(requireContext())
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