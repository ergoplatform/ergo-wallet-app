package org.ergoplatform.android.ui.wallet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import org.ergoplatform.android.R

/**
 * A simple [Fragment] subclass.
 * Use the [CreateWalletFragmentDialog.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateWalletFragmentDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(
            R.layout.fragment_create_wallet_dialog,
            container,
            false
        )
    }


    override fun getTheme(): Int {
        return R.style.FullScreenDialogTheme
    }

}