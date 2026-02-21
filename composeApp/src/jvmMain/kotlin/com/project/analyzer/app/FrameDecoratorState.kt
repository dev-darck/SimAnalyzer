@file:OptIn(ExperimentalComposeUiApi::class)

package com.project.analyzer.app

import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.analyzer.app.win.WindowsFrameController

@Stable
class FrameDecoratorState internal constructor(val captionHeight: Dp = 32.dp, val win: WindowsFrameController)
