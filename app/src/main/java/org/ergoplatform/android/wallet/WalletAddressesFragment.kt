package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import org.ergoplatform.android.databinding.FragmentWalletAddressesBinding


/**
 * Manages wallet derived addresses
 */
class WalletAddressesFragment : Fragment() {

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

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}