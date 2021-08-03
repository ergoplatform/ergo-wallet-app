package org.ergoplatform.android.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
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

        // makes the links clickable
        binding.labelMoreInfo.movementMethod = LinkMovementMethod.getInstance()

        setDisplayCurrency()

        binding.displayCurrency.setOnClickListener {
            DisplayCurrencyListDialogFragment().show(childFragmentManager, null)
        }

        setDayNightModeButtonColor(AppCompatDelegate.getDefaultNightMode())
        binding.darkModeSystem.setOnClickListener { changeDayNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
        binding.darkModeDay.setOnClickListener { changeDayNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
        binding.darkModeNight.setOnClickListener { changeDayNightMode(AppCompatDelegate.MODE_NIGHT_YES) }

        binding.buttonConnectionSettings.setOnClickListener {
            ConnectionSettingsDialogFragment().show(childFragmentManager, null)
        }
    }

    private fun changeDayNightMode(mode: Int) {
        org.ergoplatform.android.changeDayNightMode(requireContext(), mode)
        setDayNightModeButtonColor(mode)
    }

    private fun setDayNightModeButtonColor(mode: Int) {
        binding.darkModeSystem.backgroundTintList =
            ResourcesCompat.getColorStateList(
                resources,
                if (mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) R.color.primary else R.color.secondary,
                null
            )
        binding.darkModeDay.backgroundTintList =
            ResourcesCompat.getColorStateList(
                resources,
                if (mode == AppCompatDelegate.MODE_NIGHT_NO) R.color.primary else R.color.secondary,
                null
            )
        binding.darkModeNight.backgroundTintList =
            ResourcesCompat.getColorStateList(
                resources,
                if (mode == AppCompatDelegate.MODE_NIGHT_YES) R.color.primary else R.color.secondary,
                null
            )
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