package org.ergoplatform.android.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import org.ergoplatform.android.R

open class FullScreenFragmentDialog : DialogFragment() {
    override fun getTheme(): Int {
        return R.style.FullScreenDialogTheme
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), theme) {
            override fun onBackPressed() {
                // On backpress, do your stuff here.
                if (!this@FullScreenFragmentDialog.onBackPressed()) {
                    super.onBackPressed()
                }
            }
        }
    }

    fun onBackPressed() : Boolean {
        return false;
    }
}
