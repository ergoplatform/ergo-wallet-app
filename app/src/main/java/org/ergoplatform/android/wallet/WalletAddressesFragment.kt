package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.CardWalletAddressBinding
import org.ergoplatform.android.databinding.FragmentWalletAddressesBinding
import org.ergoplatform.android.nanoErgsToErgs
import org.ergoplatform.android.ui.PasswordDialogCallback
import org.ergoplatform.android.ui.PasswordDialogFragment


/**
 * Manages wallet derived addresses
 */
class WalletAddressesFragment : Fragment(), PasswordDialogCallback {

    var _binding: FragmentWalletAddressesBinding? = null
    val binding: FragmentWalletAddressesBinding get() = _binding!!

    private val args: WalletAddressesFragmentArgs by navArgs()
    private lateinit var viewModel: WalletAddressesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentWalletAddressesBinding.inflate(layoutInflater, container, false)

        viewModel = ViewModelProvider(this).get(WalletAddressesViewModel::class.java)
        viewModel.init(requireContext(), args.walletId)
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val walletAddressesAdapter = WalletAddressesAdapter()
        binding.recyclerview.adapter = walletAddressesAdapter

        viewModel.addresses.observe(viewLifecycleOwner, {
            binding.walletName.text = viewModel.wallet?.walletConfig?.displayName
            walletAddressesAdapter.wallet = viewModel.wallet
            walletAddressesAdapter.addressList = it
        })
    }

    fun showBiometricPrompt() {
        // setDeviceCredentialAllowed is deprecated, but needed for older SDK level
        @Suppress("DEPRECATION") val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.title_authenticate))
            .setConfirmationRequired(true) // don't display immediately when face is recognized
            .setDeviceCredentialAllowed(true)
            .build()

        val context = requireContext()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    viewModel.addAddressWithBiometricAuth(context)
                } catch (t: Throwable) {
                    view?.let {
                        Snackbar.make(
                            it,
                            getString(R.string.error_device_security, t.message),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                view?.let {
                    Snackbar.make(
                        it,
                        getString(R.string.error_device_security, errString),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        BiometricPrompt(this, callback).authenticate(promptInfo)
    }

    override fun onPasswordEntered(password: String?): String? {
        password?.let {
            if (!viewModel.addAddressWithPass(requireContext(), password)) {
                return getString(R.string.error_password_wrong)
            } else {
                return null
            }
        }
        return getString(R.string.error_password_empty)
    }

    inner class WalletAddressesAdapter : RecyclerView.Adapter<WalletAddressViewHolder>() {
        var wallet: WalletDbEntity? = null
        var addressList: List<WalletAddressDbEntity> = emptyList()
            set(value) {
                val diffCallback = WalletAddressDiffCallback(field, value)
                field = value
                val diffResult = DiffUtil.calculateDiff(diffCallback)
                diffResult.dispatchUpdatesTo(this)
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletAddressViewHolder {
            return WalletAddressViewHolder(
                CardWalletAddressBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: WalletAddressViewHolder, position: Int) {
            if (position == addressList.size) {
                holder.bindAddAddress()
            } else {
                holder.bindAddress(addressList[position], wallet!!)
            }
        }

        override fun getItemCount(): Int {
            // we always have main address, so when this is empty db has not loaded yet
            return if (addressList.isEmpty()) 0 else addressList.size + 1
        }
    }

    inner class WalletAddressViewHolder(val binding: CardWalletAddressBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindAddress(dbEntity: WalletAddressDbEntity, wallet: WalletDbEntity) {
            val ctx = binding.root.context
            val isDerivedAddress = dbEntity.derivationIndex > 0

            binding.layoutNewAddress.visibility = View.GONE
            binding.cardView.isClickable = true
            binding.buttonMoreMenu.visibility = View.VISIBLE

            binding.addressInformation.apply {
                root.visibility = View.VISIBLE

                addressIndex.visibility =
                    if (isDerivedAddress) View.VISIBLE else View.GONE
                addressIndex.text = dbEntity.derivationIndex.toString()
                addressLabel.text = dbEntity.label
                    ?: (if (isDerivedAddress) ctx.getString(
                        R.string.label_wallet_address_derived,
                        dbEntity.derivationIndex.toString()
                    ) else ctx.getString(R.string.label_wallet_main_address))
                publicAddress.text = dbEntity.publicAddress

                val state = wallet.getStateForAddress(dbEntity.publicAddress)
                val tokens = wallet.getTokensForAddress(dbEntity.publicAddress)
                addressBalance.amount = nanoErgsToErgs(state?.balance ?: 0)
                labelTokenNum.visibility =
                    if (tokens.isNullOrEmpty()) View.GONE else View.VISIBLE
                labelTokenNum.text =
                    ctx.getString(R.string.label_wallet_token_balance, tokens.size.toString())
            }
        }

        fun bindAddAddress() {
            binding.cardView.isClickable = false
            binding.buttonMoreMenu.visibility = View.GONE
            binding.layoutNewAddress.visibility = View.VISIBLE
            binding.addressInformation.root.visibility = View.GONE

            binding.buttonAddAddress.setOnClickListener {
                viewModel.numAddressesToAdd = getNumAddressesToAdd()
                viewModel.wallet?.walletConfig?.let {
                    if (it.encryptionType == ENC_TYPE_PASSWORD) {
                        PasswordDialogFragment().show(
                            this@WalletAddressesFragment.childFragmentManager,
                            null
                        )
                    } else if (it.encryptionType == ENC_TYPE_DEVICE) {
                        showBiometricPrompt()
                    }
                }
            }
            binding.sliderNumAddresses.progress = 0
            refreshAddButtonLabel()
            binding.sliderNumAddresses.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    refreshAddButtonLabel()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }

            })
        }

        private fun refreshAddButtonLabel() {
            val numAddresses = getNumAddressesToAdd()
            binding.buttonAddAddress.text =
                if (numAddresses <= 1) getString(R.string.button_add_address) else
                    getString(R.string.button_add_addresses, numAddresses)
        }

        private fun getNumAddressesToAdd() = binding.sliderNumAddresses.progress + 1
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

class WalletAddressDiffCallback(
    val oldList: List<WalletAddressDbEntity>,
    val newList: List<WalletAddressDbEntity>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList.get(oldItemPosition).derivationIndex == newList.get(newItemPosition).derivationIndex
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // always redraw
        return false
    }
}
