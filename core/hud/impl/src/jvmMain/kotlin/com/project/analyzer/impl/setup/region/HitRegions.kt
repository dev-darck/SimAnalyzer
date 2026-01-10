package com.project.analyzer.impl.setup.region

import androidx.compose.ui.unit.IntRect
import kotlinx.coroutines.flow.Flow

interface HitRegions {

    fun snapshot(): List<IntRect>
    fun observeChanges(): Flow<List<IntRect>>
    fun put(key: String, rect: IntRect)
    fun remove(key: String)
    fun clear()
}
