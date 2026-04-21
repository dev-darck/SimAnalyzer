package com.project.analyzer.app

import com.analyzer.settings.api.AppCloseBehavior

internal enum class AppCloseAction {
    Ask,
    Exit,
    MinimizeToTray,
}

internal fun resolveAppCloseAction(behavior: AppCloseBehavior, isSystemTraySupported: Boolean): AppCloseAction = when {
    !isSystemTraySupported -> AppCloseAction.Exit
    behavior == AppCloseBehavior.Exit -> AppCloseAction.Exit
    behavior == AppCloseBehavior.MinimizeToTray -> AppCloseAction.MinimizeToTray
    else -> AppCloseAction.Ask
}
