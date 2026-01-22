package com.project.analyzer.navigation.impl

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene
import com.project.analyzer.navigation.api.Route

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
private const val DURATION_ENTER = 400
private const val DURATION_EXIT = 200
private const val FADE_DURATION = 150

internal fun sharedAxisZForward(): AnimatedContentTransitionScope<Scene<Route>>.() -> ContentTransform =
    {
        val enter: EnterTransition =
            fadeIn(
                animationSpec = tween(
                    durationMillis = FADE_DURATION,
                    delayMillis = DURATION_EXIT - FADE_DURATION,
                    easing = EmphasizedDecelerate
                )
            ) + slideInHorizontally(
                initialOffsetX = { fullWidth -> (fullWidth * 0.1f).toInt() },
                animationSpec = tween(
                    durationMillis = DURATION_ENTER,
                    easing = EmphasizedDecelerate
                )
            )

        val exit: ExitTransition =
            fadeOut(
                animationSpec = tween(
                    durationMillis = FADE_DURATION,
                    easing = EmphasizedAccelerate
                )
            ) + slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * 0.05f).toInt() },
                animationSpec = tween(
                    durationMillis = DURATION_EXIT,
                    easing = EmphasizedAccelerate
                )
            )

        enter togetherWith exit
    }

internal fun sharedAxisZBackward(): AnimatedContentTransitionScope<Scene<Route>>.() -> ContentTransform =
    {
        val enter: EnterTransition =
            fadeIn(
                animationSpec = tween(
                    durationMillis = FADE_DURATION,
                    delayMillis = DURATION_EXIT - FADE_DURATION,
                    easing = EmphasizedDecelerate
                )
            ) + slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * 0.05f).toInt() },
                animationSpec = tween(
                    durationMillis = DURATION_ENTER,
                    easing = EmphasizedDecelerate
                )
            )

        val exit: ExitTransition =
            fadeOut(
                animationSpec = tween(
                    durationMillis = FADE_DURATION,
                    easing = EmphasizedAccelerate
                )
            ) + slideOutHorizontally(
                targetOffsetX = { fullWidth -> (fullWidth * 0.1f).toInt() },
                animationSpec = tween(
                    durationMillis = DURATION_EXIT,
                    easing = EmphasizedAccelerate
                )
            )

        enter togetherWith exit
    }
