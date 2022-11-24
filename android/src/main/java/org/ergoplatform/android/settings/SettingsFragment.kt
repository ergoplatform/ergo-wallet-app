package org.ergoplatform.android.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import org.ergoplatform.android.*
import org.ergoplatform.android.databinding.FragmentSettingsBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.enableLinks
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.android.ui.showDialogWithCopyOption

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by navGraphViewModels(R.id.navigation_settings)

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        binding.labelVersion.text = BuildConfig.VERSION_NAME
        binding.labelBuildBy.text = getString(R.string.desc_about, getString(R.string.about_year))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // makes the links clickable
        binding.labelMoreInfo.enableLinks()
        binding.labelCoingecko.enableLinks()
        binding.labelTokenVerification.enableLinks()

        showDisplayCurrency()

        binding.displayCurrency.setOnClickListener {
            DisplayCurrencyListDialogFragment().show(childFragmentManager, null)
        }

        setBalanceNotifButtonTitle()
        binding.balanceNotification.setOnClickListener {
            val context = requireContext()
            val prefs = Preferences(context)
            viewModel.uiLogic.changeBalanceSyncInterval(prefs)
            BackgroundSync.rescheduleJob(context)
            setBalanceNotifButtonTitle()
        }

        setDayNightModeButtonColor(AppCompatDelegate.getDefaultNightMode())
        binding.darkModeSystem.setOnClickListener { changeDayNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
        binding.darkModeDay.setOnClickListener { changeDayNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
        binding.darkModeNight.setOnClickListener { changeDayNightMode(AppCompatDelegate.MODE_NIGHT_YES) }

        binding.buttonConnectionSettings.setOnClickListener {
            findNavController().navigateSafe(SettingsFragmentDirections.actionNavigationSettingsToConnectionSettingsDialogFragment())
        }

        binding.containerDebugInformation.visibility =
            if (App.lastStackTrace.isNullOrBlank()) View.GONE else View.VISIBLE

        binding.buttonDebugInformation.setOnClickListener {
            App.lastStackTrace?.let {
                showDialogWithCopyOption(requireContext(), it)
            }
        }

        setButtonDownloadContentText()
        binding.buttonDownloadContent.setOnClickListener {
            val preferences = Preferences(requireContext())
            preferences.downloadNftContent = !preferences.downloadNftContent
            setButtonDownloadContentText()
        }

        setAppLockButtonText()
        binding.buttonAppLock.setOnClickListener {
            val preferences = Preferences(requireContext())
            preferences.enableAppLock = !preferences.enableAppLock
            setAppLockButtonText()
        }
    }

    private fun setButtonDownloadContentText() {
        binding.buttonDownloadContent.setText(
            if (Preferences(requireContext()).downloadNftContent) R.string.button_download_content_off
            else R.string.button_download_content_on
        )
    }

    private fun setAppLockButtonText() {
        binding.buttonAppLock.setText(
            if (Preferences(requireContext()).enableAppLock) R.string.button_download_content_off
            else R.string.button_download_content_on
        )
    }

    private fun setBalanceNotifButtonTitle() {
        val context = requireContext()
        binding.balanceNotification.text = viewModel.uiLogic.getBalanceSyncButtonText(
            Preferences(context),
            AndroidStringProvider(context)
        )
    }

    private fun changeDayNightMode(mode: Int) {
        Preferences(requireContext()).dayNightMode = mode
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

    fun showDisplayCurrency() {
        binding.displayCurrency.text = viewModel.uiLogic.getFiatCurrencyButtonText(
            Preferences(requireContext()), AndroidStringProvider(requireContext())
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}