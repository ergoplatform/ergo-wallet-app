package org.ergoplatform.android.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.ergoplatform.android.BuildConfig
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentSettingsBinding
import org.ergoplatform.android.getPrefDisplayCurrency
import java.util.*

class SettingsFragment : Fragment() {

    private lateinit var settingsViewModel: SettingsViewModel

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingsViewModel =
            ViewModelProvider(this).get(SettingsViewModel::class.java)
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        binding.labelVersion.text = BuildConfig.VERSION_NAME

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setDisplayCurrency()

        binding.displayCurrency.setOnClickListener {
            DisplayCurrencyListDialogFragment().show(childFragmentManager, null)
        }
    }

    fun setDisplayCurrency() {
        val displayCurrency =
            getPrefDisplayCurrency(requireContext()).toUpperCase(Locale.getDefault())
        binding.displayCurrency.setText(
            getString(
                R.string.button_display_currency,
                if (displayCurrency.isNotEmpty()) displayCurrency else getString(R.string.label_none)
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}