package com.project.analyzer.calibration.presentation.trackmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.trackmap.TrackMapStatsCalculator
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.trackmap.TrackMapRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Inject
@ViewModelKey(TrackMapLibraryViewModel::class)
@ContributesIntoMap(ScreenScope::class)
class TrackMapLibraryViewModel(private val repository: TrackMapRepository) : ViewModel() {

    private val stats = TrackMapStatsCalculator()

    private val _items = MutableStateFlow<List<TrackMapLibraryItem>>(emptyList())
    val items: StateFlow<List<TrackMapLibraryItem>> = _items

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val maps = withContext(Dispatchers.IO) {
                repository.loadAll().sortedByDescending { it.createdAtEpochMs }
            }
            _items.value = maps.map { map ->
                val points = map.points.map { Vec2(it.x, it.y) }
                val leftWidths = map.points.map { it.leftWidthMeters }
                val rightWidths = map.points.map { it.rightWidthMeters }
                val pitPoints = map.pitPoints.map { Vec2(it.x, it.y) }
                val bounds = map.bounds ?: stats.computeBounds(points)
                val distance = stats.computeDistance(points)
                val pitEntryPoint = map.pitEntryIndex.takeIf { it in points.indices }?.let { points[it] }
                val pitExitPoint = map.pitExitIndex.takeIf { it in points.indices }?.let { points[it] }
                TrackMapLibraryItem(
                    map = map,
                    points = points,
                    leftWidthsMeters = leftWidths,
                    rightWidthsMeters = rightWidths,
                    pitPoints = pitPoints,
                    bounds = bounds,
                    distanceMeters = distance,
                    pitEntryPoint = pitEntryPoint,
                    pitExitPoint = pitExitPoint,
                )
            }
        }
    }
}
