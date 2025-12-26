package com.project.analyzer.calibration.presentation.overlay

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class IntRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun contains(x: Int, y: Int): Boolean = x in left..right && y in top..bottom
}

interface HitRegions {
    fun put(key: String, rect: IntRect)
    fun remove(key: String)
    fun snapshot(): List<IntRect>
    fun version(): Long
}

class InMemoryHitRegions : HitRegions {
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
