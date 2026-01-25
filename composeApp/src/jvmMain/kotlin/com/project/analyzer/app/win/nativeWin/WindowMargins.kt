package com.project.analyzer.app.win.nativeWin

import com.sun.jna.Structure

@Structure.FieldOrder(
    "leftBorderWidth",
    "rightBorderWidth",
    "topBorderHeight",
    "bottomBorderHeight"
)
data class WindowMargins(
    @JvmField
    var leftBorderWidth: Int,
    @JvmField
    var rightBorderWidth: Int,
    @JvmField
    var topBorderHeight: Int,
    @JvmField
    var bottomBorderHeight: Int
) : Structure(), Structure.ByReference
