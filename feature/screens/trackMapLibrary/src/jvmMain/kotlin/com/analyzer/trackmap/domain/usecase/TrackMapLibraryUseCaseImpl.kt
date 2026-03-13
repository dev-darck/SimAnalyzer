package com.analyzer.trackmap.domain.usecase

import com.analyzer.trackmap.data.library.TrackMapLibraryRepository
import com.analyzer.trackmap.data.selection.TrackMapEditorSelectionCache
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@SingleIn(ScreenScope::class)
@ContributesBinding(ScreenScope::class, binding = binding<TrackMapLibraryUseCase>())
@Inject
class TrackMapLibraryUseCaseImpl(
    private val repository: TrackMapLibraryRepository,
    private val selectionCache: TrackMapEditorSelectionCache,
) : TrackMapLibraryUseCase {

    override suspend fun loadItems(): List<TrackMapLibraryItem> = repository.loadItems()

    override suspend fun loadItem(gameId: String, trackId: String, layoutId: String?): TrackMapLibraryItem? =
        repository.loadItem(
            gameId = gameId,
            trackId = trackId,
            layoutId = layoutId,
        )

    override fun rememberSelection(item: TrackMapLibraryItem) {
        selectionCache.remember(item)
    }
}
