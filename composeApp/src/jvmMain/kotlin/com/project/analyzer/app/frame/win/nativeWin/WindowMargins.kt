package com.project.analyzer.app.frame.win.nativeWin

import com.sun.jna.Structure
import com.sun.jna.Structure.ByReference
import com.sun.jna.Structure.FieldOrder

@FieldOrder(
    "leftBorderWidth",
    "rightBorderWidth",
    "topBorderHeight",
    "bottomBorderHeight",
)
data class WindowMargins(
    @JvmField
    var leftBorderWidth: Int,
    @JvmField
    var rightBorderWidth: Int,
    @JvmField
    var topBorderHeight: Int,
    @JvmField
    var bottomBorderHeight: Int,
) : Structure(),
    ByReference
