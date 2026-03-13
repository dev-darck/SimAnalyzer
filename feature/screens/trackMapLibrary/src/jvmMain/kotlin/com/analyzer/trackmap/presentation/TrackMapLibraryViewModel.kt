package com.analyzer.trackmap.presentation

import androidx.lifecycle.viewModelScope
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.analyzer.trackmap.domain.usecase.TrackMapLibraryUseCase
import com.analyzer.trackmap.presentation.mapper.TrackMapLibraryCardUiMapper
import com.analyzer.trackmap.presentation.model.TrackMapLibraryCardUi
import com.project.analyzer.leak.api.LeakAwareViewModel
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
internal class TrackMapLibraryViewModel(
    private val useCase: TrackMapLibraryUseCase,
    private val cardUiMapper: TrackMapLibraryCardUiMapper,
) : LeakAwareViewModel() {

    private val _items =
        MutableStateFlow<ImmutableList<TrackMapLibraryCardUi>>(persistentListOf())
    val items: StateFlow<ImmutableList<TrackMapLibraryCardUi>> = _items.asStateFlow()
    private var itemsByKey: Map<String, TrackMapLibraryItem> = emptyMap()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val loadedItems = useCase.loadItems()
            val page = cardUiMapper.map(loadedItems)
            itemsByKey = page.itemsByKey
            _items.value = page.cards
        }
    }

    fun rememberSelection(mapKey: String) {
        itemsByKey[mapKey]?.let(useCase::rememberSelection)
    }
}
