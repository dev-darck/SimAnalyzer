package com.project.analyzer.game.impl.display

internal data class DisplayTopology(val devices: List<DisplayDeviceSnapshot>, val updatedAtNs: Long) {
    fun defaultDevice(): DisplayDeviceSnapshot = devices.firstOrNull { it.isDefault } ?: devices.first()
}
