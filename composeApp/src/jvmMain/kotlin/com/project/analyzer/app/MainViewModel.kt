package com.project.analyzer.app

import androidx.lifecycle.viewModelScope
import com.analyzer.settings.api.AppCloseBehavior
import com.analyzer.settings.api.AppCloseBehaviorRepository
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class MainViewModel(
    private val closeBehaviorRepository: AppCloseBehaviorRepository,
    initialCloseBehavior: AppCloseBehavior,
) : LeakAwareMviViewModel<MainIntent, MainState>(
    MainState(closeBehavior = initialCloseBehavior),
) {

    init {
        closeBehaviorRepository.observeCloseBehavior()
            .onEach { behavior ->
                updateState { copy(closeBehavior = behavior) }
            }
            .launchIn(viewModelScope)
    }

    fun handleWindowCloseRequest(isSystemTraySupported: Boolean): AppCloseAction =
        when (val action = resolveAppCloseAction(currentState.closeBehavior, isSystemTraySupported)) {
            AppCloseAction.Ask -> {
                updateState {
                    copy(
                        showCloseBehaviorDialog = true,
                        rememberCloseBehaviorDecision = false,
                    )
                }
                AppCloseAction.Ask
            }

            else -> action
        }

    suspend fun confirmCloseBehavior(behavior: AppCloseBehavior): AppCloseAction {
        if (currentState.rememberCloseBehaviorDecision) {
            closeBehaviorRepository.setCloseBehavior(behavior)
        }
        updateState {
            copy(
                showCloseBehaviorDialog = false,
                rememberCloseBehaviorDecision = false,
            )
        }
        return when (behavior) {
            AppCloseBehavior.Exit -> AppCloseAction.Exit
            AppCloseBehavior.MinimizeToTray -> AppCloseAction.MinimizeToTray
            AppCloseBehavior.AskEveryTime -> AppCloseAction.Ask
        }
    }

    override suspend fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.ChangeRememberCloseBehaviorDecision -> updateState {
                copy(rememberCloseBehaviorDecision = intent.remember)
            }

            MainIntent.DismissCloseBehaviorDialog -> updateState {
                copy(
                    showCloseBehaviorDialog = false,
                    rememberCloseBehaviorDecision = false,
                )
            }
        }
    }
}
