package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentCreateWalletDialogBinding
import org.ergoplatform.android.ui.FullScreenFragmentDialog

/**
 * Create a new wallet
 */
class CreateWalletFragmentDialog : FullScreenFragmentDialog() {

    private var _binding: FragmentCreateWalletDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCreateWalletDialogBinding.inflate(inflater, container, false)

        binding.ergoLogo.setOnClickListener {
            NavHostFragment.findNavController(requireParentFragment())
                .navigate(CreateWalletFragmentDialogDirections.actionCreateWalletDialogToNavigationWallet())
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}