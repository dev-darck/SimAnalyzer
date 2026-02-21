package com.project.analyzer.navigation.impl

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene
import com.project.analyzer.navigation.api.Route

internal fun fadeForward(): AnimatedContentTransitionScope<Scene<Route>>.() -> ContentTransform = {
    val enter = fadeIn()
    val exit = fadeOut()

    (enter togetherWith exit)
}

internal fun fadeBackward(): AnimatedContentTransitionScope<Scene<Route>>.() -> ContentTransform = {
    val enter = fadeIn()
    val exit = fadeOut()

    (enter togetherWith exit)
}
