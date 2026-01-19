package com.project.analyzer.utils.ext

import java.util.Locale

public fun Int.fromMsToLapTime(): String {
    val totalSec = this / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val millis = this % 1000
    return String.format(Locale.US, "%d:%02d.%03d", minutes, seconds, millis)
}
