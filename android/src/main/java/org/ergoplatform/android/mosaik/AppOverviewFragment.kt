package org.ergoplatform.android.mosaik

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.databinding.FragmentAppOverviewBinding
import org.ergoplatform.android.databinding.FragmentAppOverviewItemBinding
import org.ergoplatform.android.ui.hideForcedSoftKeyboard
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.mosaik.MosaikAppEntry

class AppOverviewFragment : Fragment() {
    private var _binding: FragmentAppOverviewBinding? = null
    private val binding get() = _binding!!

    // TODO Mosaik way to delete mosaik_host entries and non-favorite apps
    //  TODO delete cached icon files not linked any more

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

        fillAppLists()
    }

    private fun fillAppLists() {
        val db = AppDatabase.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    db.mosaikDbProvider.getAllAppsByLastVisited(5).collect { lastVisited ->
                        lastVisited.lastOrNull()?.lastVisited?.let { oldestShownEntry ->
                            db.mosaikDbProvider.deleteAppsNotFavoriteVisitedBefore(oldestShownEntry)
                        }

                        refreshLastVisited(lastVisited)
                    }
                }
                launch {
                    db.mosaikDbProvider.getAllAppFavorites().collect { favorites ->
                        refreshFavorites(favorites.sortedBy { it.name.lowercase() })
                    }
                }
            }
        }
    }

    private fun refreshFavorites(favorites: List<MosaikAppEntry>) {
        if (favorites.isNotEmpty())
            binding.descEmpty.visibility = View.GONE
        binding.descFavoritesEmpty.visibility =
            if (favorites.isEmpty()) View.VISIBLE else View.GONE

        binding.layoutFavorites.apply {
            removeAllViews()
            favorites.forEach { addAppEntry(this, it) }
        }
    }

    private fun refreshLastVisited(lastVisited: List<MosaikAppEntry>) {
        if (lastVisited.isNotEmpty())
            binding.descEmpty.visibility = View.GONE
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