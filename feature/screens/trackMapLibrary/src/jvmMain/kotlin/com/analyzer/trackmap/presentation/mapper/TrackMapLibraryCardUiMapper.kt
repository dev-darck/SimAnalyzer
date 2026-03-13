package com.analyzer.trackmap.presentation.mapper

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.analyzer.trackmap.domain.model.mapKey
import com.analyzer.trackmap.presentation.model.TrackMapLibraryCardUi
import com.project.analyzer.api.di.Default
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal data class TrackMapLibraryCardUiPage(
    val itemsByKey: Map<String, TrackMapLibraryItem>,
    val cards: ImmutableList<TrackMapLibraryCardUi>,
)

@Inject
@SingleIn(ScreenScope::class)
internal class TrackMapLibraryCardUiMapper(
    @param:Default
    private val defaultDispatcher: CoroutineDispatcher,
) {

    suspend fun map(items: List<TrackMapLibraryItem>): TrackMapLibraryCardUiPage = withContext(defaultDispatcher) {
        TrackMapLibraryCardUiPage(
            itemsByKey = items.associateBy(TrackMapLibraryItem::mapKey),
            cards = items.map(TrackMapLibraryItem::toTrackMapLibraryCardUi).toPersistentList(),
        )
    }
}
