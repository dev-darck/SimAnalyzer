package com.project.analyzer.impl.setup.region.internal

import androidx.compose.ui.unit.IntRect
import com.project.analyzer.impl.setup.region.HitRegions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

class InMemoryHitRegions : HitRegions {

    private val regions = ConcurrentHashMap<String, IntRect>()
    private val _changes = MutableStateFlow(0L)

    override fun snapshot(): List<IntRect> = regions.values.toList()

    override fun observeChanges(): Flow<List<IntRect>> = _changes.asStateFlow().map { snapshot() }

    override fun put(key: String, rect: IntRect) {
        regions[key] = rect
        _changes.value++
    }

    override fun remove(key: String) {
        if (regions.remove(key) != null) {
            _changes.value++
        }
    }

    override fun clear() {
        if (regions.isNotEmpty()) {
            regions.clear()
            _changes.value++
        }
    }
}
