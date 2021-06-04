package org.ergoplatform.android.ui.wallet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentCreateWalletDialogBinding

/**
 * A simple [Fragment] subclass.
 * Use the [CreateWalletFragmentDialog.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateWalletFragmentDialog : DialogFragment() {

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
                .navigate(R.id.action_createWalletDialog_to_navigation_wallet)
        }

        return binding.root
    }

    override fun getTheme(): Int {
        return R.style.FullScreenDialogTheme
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), theme) {
            override fun onBackPressed() {
                // On backpress, do your stuff here.
            }
        }
    }
}