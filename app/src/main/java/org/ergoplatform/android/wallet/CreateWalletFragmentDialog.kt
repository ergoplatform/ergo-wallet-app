package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import org.ergoplatform.android.databinding.FragmentCreateWalletDialogBinding
import org.ergoplatform.android.ui.FullScreenFragmentDialog
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.appkit.Mnemonic

/**
 * Create a new wallet, step 1
 */
class CreateWalletFragmentDialog : FullScreenFragmentDialog() {

    private var _binding: FragmentCreateWalletDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentCreateWalletDialogBinding.inflate(inflater, container, false)

        binding.buttonNextStep.setOnClickListener {
            binding.tvMnemonic.text?.toString()?.let {
                NavHostFragment.findNavController(requireParentFragment())
                    .navigateSafe(
                        CreateWalletFragmentDialogDirections.actionCreateWalletDialogToConfirmCreateWalletFragment(
                            it
                        )
                    )
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvMnemonic.text = Mnemonic.generateEnglishMnemonic()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}