package org.ergoplatform.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.NavHostFragment
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentAddWalletChooserBinding

/**
 * Add fragment step 1: Chooser to import or add a new fragment
 */
class AddWalletChooserFragmentDialog : DialogFragment() {

    private var _binding: FragmentAddWalletChooserBinding? = null

    // This property is only valid between onCreateDialog and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddWalletChooserBinding.inflate(inflater, container, false)

        binding.cardCreateWallet.setOnClickListener { view ->
            NavHostFragment.findNavController(requireParentFragment())
                .navigate(R.id.action_to_createWalletDialog)
        }
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun getTheme(): Int {
        return R.style.FullScreenDialogTheme
    }
}