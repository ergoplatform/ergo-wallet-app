package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import org.ergoplatform.android.databinding.FragmentCreateWalletDialogBinding
import org.ergoplatform.android.ui.FullScreenFragmentDialog
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.android.ui.showSensitiveDataCopyDialog

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
        dialog?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

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

        binding.buttonCopy.setOnClickListener {
            binding.tvMnemonic.text?.toString()
                ?.let { showSensitiveDataCopyDialog(requireContext(), it) }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvMnemonic.text =
            ViewModelProvider(this).get(CreateWalletViewModel::class.java).mnemonic
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}