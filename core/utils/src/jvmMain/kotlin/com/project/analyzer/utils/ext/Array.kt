package com.project.analyzer.utils.ext

public fun IntArray.formatIntArray(max: Int): String {
    val n = kotlin.math.min(size, max)
    val sb = StringBuilder()
    sb.append("[")
    for (i in 0 until n) {
        if (i > 0) sb.append(", ")
        sb.append(get(i))
    }
    if (size > max) sb.append(", \u2026 +").append(size - max)
    sb.append("]")
    return sb.toString()
}

public fun FloatArray.formatFloatArray(max: Int, decimals: Int): String {
    val n = kotlin.math.min(size, max)
    val sb = StringBuilder()
    sb.append("[")
    for (i in 0 until n) {
        if (i > 0) sb.append(", ")
        sb.append(get(i).fmt(decimals))
    }
    if (size > max) sb.append(", \u2026 +").append(size - max)
    sb.append("]")
    return sb.toString()
}
