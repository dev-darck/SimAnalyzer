package com.project.analyzer.navigation.impl

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene
import com.project.analyzer.navigation.api.Route

private const val ANIMATION_DURATION: Int = 200

internal fun sharedAxisZForward(): AnimatedContentTransitionScope<Scene<Route>>.() -> ContentTransform =
    {
        val enter: EnterTransition =
            fadeIn(animationSpec = tween(ANIMATION_DURATION, easing = LinearEasing)) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(ANIMATION_DURATION, easing = EaseIn)
                )

        val exit: ExitTransition =
            fadeOut(animationSpec = tween(ANIMATION_DURATION, easing = LinearEasing)) +
                scaleOut(
                    targetScale = 1.02f,
                    animationSpec = tween(ANIMATION_DURATION, easing = EaseOut)
                )

        enter togetherWith exit
    }

internal fun sharedAxisZBackward(): AnimatedContentTransitionScope<Scene<Route>>.() -> ContentTransform =
    {
        val enter: EnterTransition =
            fadeIn(animationSpec = tween(ANIMATION_DURATION, easing = LinearEasing)) +
                scaleIn(
                    initialScale = 1.02f,
                    animationSpec = tween(ANIMATION_DURATION, easing = EaseIn)
                )

        val exit: ExitTransition =
            fadeOut(animationSpec = tween(ANIMATION_DURATION, easing = LinearEasing)) +
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(ANIMATION_DURATION, easing = EaseOut)
                )

        enter togetherWith exit
    }
