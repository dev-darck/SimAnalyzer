package com.project.analyzer.utils.ext

import java.util.Locale

public fun Double.fmt(decimals: Int = 3): String = String.format(Locale.US, "%.${decimals}f", this)
