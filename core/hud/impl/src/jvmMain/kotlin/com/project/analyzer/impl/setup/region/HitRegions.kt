package com.project.analyzer.hud.setup.region

import androidx.compose.ui.unit.IntRect

public interface HitRegions {

    public fun put(key: String, rect: IntRect)
    public fun remove(key: String)
    public fun snapshot(): List<IntRect>
    public fun version(): Long
}
