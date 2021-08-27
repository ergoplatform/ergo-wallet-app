package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.ergoplatform.android.databinding.FragmentWalletDetailsBinding

class WalletDetailsFragment : Fragment() {

    private lateinit var walletDetailsViewModel: WalletDetailsViewModel

    private var _binding: FragmentWalletDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        walletDetailsViewModel =
                ViewModelProvider(this).get(WalletDetailsViewModel::class.java)
        _binding = FragmentWalletDetailsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}