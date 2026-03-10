package com.analyzer.trackmap.presentation

import androidx.lifecycle.viewModelScope
import com.analyzer.trackmap.data.selection.TrackMapEditorSelectionCache
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.analyzer.trackmap.domain.model.mapKey
import com.analyzer.trackmap.domain.usecase.TrackMapLibraryUseCase
import com.analyzer.trackmap.presentation.mapper.toTrackMapLibraryCardUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryCardUi
import com.project.analyzer.leak.api.LeakAwareViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
internal class TrackMapLibraryViewModel(
    private val useCase: TrackMapLibraryUseCase,
    private val selectionCache: TrackMapEditorSelectionCache,
) : LeakAwareViewModel() {

    private val _items = MutableStateFlow<List<TrackMapLibraryCardUi>>(emptyList())
    val items: StateFlow<List<TrackMapLibraryCardUi>> = _items.asStateFlow()
    private var itemsByKey: Map<String, TrackMapLibraryItem> = emptyMap()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val loadedItems = useCase.loadItems()
            itemsByKey = loadedItems.associateBy(TrackMapLibraryItem::mapKey)
            _items.value = loadedItems.map(TrackMapLibraryItem::toTrackMapLibraryCardUi)
        }
    }

    fun rememberSelection(mapKey: String) {
        itemsByKey[mapKey]?.let(selectionCache::remember)
    }
}
