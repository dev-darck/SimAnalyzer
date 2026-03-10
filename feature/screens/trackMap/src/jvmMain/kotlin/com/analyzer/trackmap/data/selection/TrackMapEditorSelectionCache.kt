package com.analyzer.trackmap.data.selection

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.analyzer.trackmap.domain.model.buildTrackMapLibraryItemKey
import com.analyzer.trackmap.domain.model.mapKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.ConcurrentHashMap

@SingleIn(AppScope::class)
@Inject
class TrackMapEditorSelectionCache {

    private val items = ConcurrentHashMap<String, TrackMapLibraryItem>()

    fun remember(item: TrackMapLibraryItem) {
        items[item.mapKey()] = item
    }

    fun find(gameId: String, trackId: String, layoutId: String?): TrackMapLibraryItem? = items[
        buildTrackMapLibraryItemKey(
            gameId = gameId,
            trackId = trackId,
            layoutId = layoutId,
        ),
    ]
}
