package com.project.analyzer.ui.format

import java.util.Locale

public fun formatDecimal(value: Number, decimals: Int = 1): String =
    String.format(Locale.US, "%.${decimals}f", value.toDouble())

public fun formatPercent(value: Int): String = "$value%"
