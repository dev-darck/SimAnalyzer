package com.project.analyzer.ac.telemetry.impl.shm.structure


internal fun CharArray.toKString(): String {
    val end = indexOf('\u0000').let { if (it == -1) size else it }
    return String(this, 0, end)
}

internal fun Int.toBoolean(): Boolean = this != 0
