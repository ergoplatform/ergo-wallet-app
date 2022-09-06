package org.ergoplatform.android.mosaik

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.databinding.FragmentAppOverviewBinding
import org.ergoplatform.android.databinding.FragmentAppOverviewItemBinding
import org.ergoplatform.android.persistence.AndroidCacheFiles
import org.ergoplatform.android.ui.decodeSampledBitmapFromByteArray
import org.ergoplatform.android.ui.hideForcedSoftKeyboard
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.mosaik.MosaikAppEntry
import org.ergoplatform.mosaik.MosaikAppSuggestion
import org.ergoplatform.utils.LogUtils

class AppOverviewFragment : Fragment() {
    private var _binding: FragmentAppOverviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MosaikAppOverviewViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.inputAppUrl.setEndIconOnClickListener { navigateToApp() }
        binding.inputAppUrl.editText?.setOnEditorActionListener { _, _, _ ->
            navigateToApp()
            true
        }

        viewModel.uiLogic.init(AppDatabase.getInstance(requireContext()))
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiLogic.lastVisitedFlow.collect {
                        refreshLastVisited(it)
                    }
                }
                launch {
                    viewModel.uiLogic.favoritesFlow.collect { favorites ->
                        refreshFavorites(favorites)
                    }
                }
                launch {
                    viewModel.uiLogic.suggestionFlow.collect { suggestions ->
                        refreshSuggestions(suggestions)
                    }
                }
            }
        }

        setTermsAndOverviewVisibility()

        binding.buttonAccept.setOnClickListener {
            Preferences(requireContext()).mosaikEnabled = true
            setTermsAndOverviewVisibility()
        }
    }

    private fun setTermsAndOverviewVisibility() {
        val enabled = Preferences(requireContext()).mosaikEnabled
        binding.mosaikAppOverview.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.mosaikAcceptTerms.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun refreshSuggestions(suggestions: List<MosaikAppSuggestion>) {
        binding.titleSuggestions.visibility = if (suggestions.isEmpty()) View.GONE else View.VISIBLE

        binding.layoutSuggestions.apply {
            removeAllViews()
            suggestions.forEach {
                addAppEntry(
                    this,
                    MosaikAppEntry(it.appUrl, it.appName, it.appDescription, null, 0, false)
                )
            }
        }
    }

    private fun refreshFavorites(favorites: List<MosaikAppEntry>) {
        binding.descFavoritesEmpty.visibility =
            if (favorites.isEmpty()) View.VISIBLE else View.GONE

        binding.layoutFavorites.apply {
            removeAllViews()
            favorites.forEach { addAppEntry(this, it) }
        }
    }

    private fun refreshLastVisited(lastVisited: List<MosaikAppEntry>) {
        binding.descLastVisitedEmpty.visibility =
            if (lastVisited.isEmpty()) View.VISIBLE else View.GONE

        binding.layoutLastVisited.apply {
            removeAllViews()
            lastVisited.forEach { addAppEntry(this, it) }
        }
    }

    private fun addAppEntry(linearLayout: LinearLayout, mosaikApp: MosaikAppEntry) {
        val binding = FragmentAppOverviewItemBinding.inflate(layoutInflater, linearLayout, true)
        binding.labelAppTitle.text = mosaikApp.name
        binding.labelAppDesc.text =
            if (mosaikApp.description.isNullOrBlank()) mosaikApp.url else mosaikApp.description
        binding.labelAppDesc.maxLines = if (mosaikApp.description.isNullOrBlank()) 1 else 3
        binding.root.setOnClickListener { navigateToApp(mosaikApp.url, mosaikApp.name) }

        // set icon, if we have one
        mosaikApp.iconFile?.let { fileId ->
            AndroidCacheFiles(requireContext()).readFileContent(fileId)?.let { iconContent ->
                try {
                    binding.imageAppIcon.setImageBitmap(
                        decodeSampledBitmapFromByteArray(iconContent, 500, 500)
                    )
                } catch (t: Throwable) {
                    LogUtils.logDebug("AppOverView", "Could not read icon file", t)
                }
            }
        }
    }

    private fun navigateToApp() {
        val url = binding.inputAppUrl.editText?.text.toString()

        if (url.isNotBlank()) {
            navigateToApp(url, null)
        }
    }

    private fun navigateToApp(url: String, title: String?) {
        hideForcedSoftKeyboard(requireContext(), binding.inputAppUrl.editText!!)
        findNavController().navigateSafe(
            AppOverviewFragmentDirections.actionAppOverviewFragmentToMosaik(
                url, title
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}