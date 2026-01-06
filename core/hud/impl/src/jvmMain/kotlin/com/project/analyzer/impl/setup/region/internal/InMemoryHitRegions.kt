package com.project.analyzer.hud.setup.region.internal

import androidx.compose.ui.unit.IntRect
import com.project.analyzer.hud.setup.region.HitRegions
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal class InMemoryHitRegions : HitRegions {
    private val map = ConcurrentHashMap<String, IntRect>()
    private val ver = AtomicLong(0)

    override fun put(key: String, rect: IntRect) {
        map[key] = rect
        ver.incrementAndGet()
    }

    override fun remove(key: String) {
        map.remove(key)
        ver.incrementAndGet()
    }

    override fun snapshot(): List<IntRect> = map.values.toList()
    override fun version(): Long = ver.get()
}
