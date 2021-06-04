package org.ergoplatform.android.ui.wallet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentCreateWalletDialogBinding
import org.ergoplatform.android.databinding.FragmentRestoreWalletBinding
import org.ergoplatform.android.ui.FullScreenFragmentDialog

/**
 * Restores a formerly generated wallet from mnemonic
 */
class RestoreWalletFragmentDialog : FullScreenFragmentDialog() {

    private var _binding: FragmentRestoreWalletBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRestoreWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

}