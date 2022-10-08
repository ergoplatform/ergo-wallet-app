package org.ergoplatform.mosaik

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.ergoplatform.api.OkHttpSingleton
import org.ergoplatform.persistance.IAppDatabase
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

abstract class MosaikAppOverviewUiLogic {
    abstract val coroutineScope: CoroutineScope

    // TODO Mosaik way to delete mosaik_host entries and non-favorite apps
    //  TODO delete cached icon files not linked any more

    val lastVisitedFlow = MutableStateFlow<List<MosaikAppEntry>>(emptyList())
    val favoritesFlow = MutableStateFlow<List<MosaikAppEntry>>(emptyList())
    private var downloadedAppSuggestions = emptyList<MosaikAppSuggestion>()
    val suggestionFlow = MutableStateFlow<List<MosaikAppSuggestion>>(emptyList())

    private var initialized = false

    fun init(db: IAppDatabase): Boolean {
        if (initialized)
            return false

        initialized = true

        setupDbFlows(db)
        coroutineScope.launch(Dispatchers.IO) {
            try {
                downloadedAppSuggestions = getSuggestionRetrofit().getAppSuggestions().execute().body()!!
                filterAppSuggestions()
            } catch (t: Throwable) {
                // swallow
            }

        }

        return true
    }

    fun setupDbFlows(db: IAppDatabase) {
        coroutineScope.launch(Dispatchers.IO) {
            db.mosaikDbProvider.getAllAppsByLastVisited(5).collect { lastVisited ->
                lastVisited.lastOrNull()?.lastVisited?.let { oldestShownEntry ->
                    db.mosaikDbProvider.deleteAppsNotFavoriteVisitedBefore(oldestShownEntry)
                }

                lastVisitedFlow.value = lastVisited
                filterAppSuggestions()
            }
        }
        coroutineScope.launch {
            db.mosaikDbProvider.getAllAppFavoritesByLastVisited().collect { favorites ->
                favoritesFlow.value = favorites
                filterAppSuggestions()
            }
        }
    }

    private fun filterAppSuggestions() {
        // this makes sure app suggestions will not show elements already listed in favorites
        // or last visited lists
        val isSameUrl: (x: String, y: String) -> Boolean =
            { x, y -> x.trimEnd('/') == y.trimEnd('/') }

        suggestionFlow.value =
            downloadedAppSuggestions.filter { suggestion ->
                favoritesFlow.value.none { isSameUrl(it.url, suggestion.appUrl) }
                        && lastVisitedFlow.value.none { isSameUrl(it.url, suggestion.appUrl) }
            }
    }

    private fun getSuggestionRetrofit(): AppSuggestionApi {
        val retrofit = Retrofit.Builder().baseUrl("https://raw.githubusercontent.com/MrStahlfelge/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpSingleton.getInstance())
            .build()

        return retrofit.create(AppSuggestionApi::class.java)
    }

}