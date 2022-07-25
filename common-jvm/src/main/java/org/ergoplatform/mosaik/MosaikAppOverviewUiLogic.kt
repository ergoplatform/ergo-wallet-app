package org.ergoplatform.mosaik

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.ergoplatform.persistance.IAppDatabase

abstract class MosaikAppOverviewUiLogic {
    abstract val coroutineScope: CoroutineScope

    // TODO Mosaik way to delete mosaik_host entries and non-favorite apps
    //  TODO delete cached icon files not linked any more

    val lastVisitedFlow = MutableStateFlow<List<MosaikAppEntry>>(emptyList())
    val favoritesFlow = MutableStateFlow<List<MosaikAppEntry>>(emptyList())
    // TODO Mosaik suggestions list fetched from GH

    private var initialized = false

    fun init(db: IAppDatabase) {
        if (initialized)
            return

        initialized = true

        coroutineScope.launch(Dispatchers.IO) {
            db.mosaikDbProvider.getAllAppsByLastVisited(5).collect { lastVisited ->
                lastVisited.lastOrNull()?.lastVisited?.let { oldestShownEntry ->
                    db.mosaikDbProvider.deleteAppsNotFavoriteVisitedBefore(oldestShownEntry)
                }

                lastVisitedFlow.value = lastVisited
            }
        }
        coroutineScope.launch {
            db.mosaikDbProvider.getAllAppFavoritesByLastVisited().collect { favorites ->
                favoritesFlow.value = favorites
            }
        }
    }

}