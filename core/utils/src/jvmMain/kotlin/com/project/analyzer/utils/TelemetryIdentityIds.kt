package com.project.analyzer.utils

import java.util.Locale

public object TelemetryIdentityIds {

    public fun stableCarId(carModel: String?): Int? {
        val normalized = carModel
            ?.trim()
            ?.lowercase(Locale.US)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        var hash = FNV_OFFSET_BASIS
        normalized.forEach { char ->
            hash = hash xor char.code
            hash *= FNV_PRIME
        }

        val positive = hash and Int.MAX_VALUE
        return if (positive != 0) positive else FALLBACK_STABLE_ID
    }

    private const val FNV_OFFSET_BASIS: Int = -0x7ee3623b
    private const val FNV_PRIME: Int = 16777619
    private const val FALLBACK_STABLE_ID: Int = 1
}
