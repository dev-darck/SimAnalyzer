package com.project.analyzer.utils.ext

public fun CharArray.toKString(): String =
    String(this).trimEnd('\u0000').trimEnd()
