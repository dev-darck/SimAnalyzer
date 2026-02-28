package com.project.analyzer.leak.api

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public abstract class LeakAwareMviViewModel<Intent : Any, State : Any>(
    initialState: State,
    intentBufferCapacity: Int = Channel.BUFFERED,
) : LeakAwareViewModel() {

    private val intents = Channel<Intent>(capacity = intentBufferCapacity)
    private val _state = MutableStateFlow(initialState)

    public val state: StateFlow<State> = _state.asStateFlow()

    init {
        addCloseable { intents.close() }

        viewModelScope.launch {
            for (intent in intents) {
                handleIntent(intent)
            }
        }
    }

    public fun dispatch(intent: Intent) {
        val result = intents.trySend(intent)
        if (result.isSuccess || result.isClosed) return
        viewModelScope.launch {
            runCatching { intents.send(intent) }
        }
    }

    public fun dispatch(intentsBatch: Iterable<Intent>) {
        intentsBatch.forEach(::dispatch)
    }

    protected val currentState: State
        get() = _state.value

    protected fun setState(newState: State) {
        _state.value = newState
    }

    protected fun updateState(reducer: State.() -> State) {
        _state.update(reducer)
    }

    protected abstract suspend fun handleIntent(intent: Intent)
}
