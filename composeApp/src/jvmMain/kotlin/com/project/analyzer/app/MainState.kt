package com.project.analyzer.app

import com.analyzer.settings.api.AppCloseBehavior

internal data class MainState(
    val closeBehavior: AppCloseBehavior = AppCloseBehavior.AskEveryTime,
    val showCloseBehaviorDialog: Boolean = false,
    val rememberCloseBehaviorDecision: Boolean = false,
)
