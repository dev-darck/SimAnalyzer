package com.project.analyzer.calibration.presentation.trackmap

import androidx.lifecycle.viewModelScope
import com.project.analyzer.calibration.domain.interactor.TrackMapLibraryUseCase
import com.project.analyzer.leak.api.LeakAwareViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Inject
internal class TrackMapLibraryViewModel(private val useCase: TrackMapLibraryUseCase) : LeakAwareViewModel() {

    private val _items = MutableStateFlow<List<TrackMapLibraryItem>>(emptyList())
    val items: StateFlow<List<TrackMapLibraryItem>> = _items

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _items.value = useCase.loadItems()
        }
    }
}
