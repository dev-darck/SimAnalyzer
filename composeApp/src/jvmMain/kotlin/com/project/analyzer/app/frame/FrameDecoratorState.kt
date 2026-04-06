package com.project.analyzer.app.frame

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.analyzer.app.frame.win.WindowsFrameController

@Stable
class FrameDecoratorState internal constructor(val captionHeight: Dp = 32.dp, val win: WindowsFrameController)
