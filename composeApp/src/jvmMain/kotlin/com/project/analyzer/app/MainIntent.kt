package com.project.analyzer.app

internal sealed interface MainIntent {
    data class ChangeRememberCloseBehaviorDecision(val remember: Boolean) : MainIntent
    data object DismissCloseBehaviorDialog : MainIntent
}
