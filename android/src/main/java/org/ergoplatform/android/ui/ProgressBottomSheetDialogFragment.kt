package org.ergoplatform.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.android.databinding.FragmentProgressDialogBinding

/**
 * Shows a blocking progress dialog
 */
class ProgressBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentProgressDialogBinding? = null
    private val binding get() = _binding!!

    init {
        // make blocking by default, why would we need a dialog when not blocking?
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressDialogBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG_PROGRESS_DIALOG = "FRGMT_PROGRESS_DIALOG"

        fun showProgressDialog(fragmentManager: FragmentManager): ProgressBottomSheetDialogFragment {
            val dialogShown =
                fragmentManager.findFragmentByTag(TAG_PROGRESS_DIALOG) as? ProgressBottomSheetDialogFragment

            if (dialogShown == null) {
                val dialog = ProgressBottomSheetDialogFragment()
                dialog.show(fragmentManager, TAG_PROGRESS_DIALOG)

                return dialog
            } else {
                if (!dialogShown.isAdded)
                    dialogShown.show(fragmentManager, TAG_PROGRESS_DIALOG)

                return dialogShown
            }
        }

        fun dismissProgressDialog(fragmentManager: FragmentManager) {
            val dialogShown =
                fragmentManager.findFragmentByTag(TAG_PROGRESS_DIALOG) as? ProgressBottomSheetDialogFragment

            dialogShown?.dismiss()
        }
    }
}