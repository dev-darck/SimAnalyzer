package com.project.analyzer.utils.shm

public fun CharArray.toKString(): String {
    val end = indexOf('\u0000').let { if (it == -1) size else it }
    return String(this, 0, end)
}

public fun CharArray.writeWString(value: String) {
    for (i in indices) this[i] = '\u0000'
    val n = minOf(size - 1, value.length)
    for (i in 0 until n) this[i] = value[i]
    if (n < size) this[n] = '\u0000'
}

public fun ByteArray.toCString(): String {
    val end = indexOf(0).let { if (it == -1) size else it }
    return String(this, 0, end, Charsets.UTF_8)
}

public fun Int.toBoolean(): Boolean = this != 0
