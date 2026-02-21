package com.project.analyzer.telemetry.impl

import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.game.impl.GameDetector
import com.project.analyzer.game.impl.GameProfiles
import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.TelemetryGameSettings
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent.SessionEnded
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.utils.logger
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Inject
@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class, binding = binding<TelemetryLifecycle>())
class TelemetryLifecycleRouter(
    private val gameSettings: TelemetryGameSettings,
    private val gameLifecycles: Map<String, TelemetryLifecycle>,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryLifecycle {

    private val scope = CoroutineScope(
        SupervisorJob() + ioDispatcher + CoroutineExceptionHandler { _, e ->
            logger.error(e) { "[telemetry] router uncaught exception" }
        },
    )

    private val _events = MutableSharedFlow<TelemetryLifecycleEvent>(
        replay = 1,
        extraBufferCapacity = 32,
    )
    override val events: Flow<TelemetryLifecycleEvent> = _events.asSharedFlow()

    private val _frames = MutableSharedFlow<TelemetryFrame>(
        replay = 1,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val frames: SharedFlow<TelemetryFrame> = _frames.asSharedFlow()

    private val autoDetector = GameDetector(
        configs = GameProfiles.detectorConfigs(),
        requireForeground = false,
        coroutineDispatcher = ioDispatcher,
    )

    private val stateMutex = Mutex()
    private var monitorJob: Job? = null
    private var forwardJob: Job? = null
    private var activeLifecycle: TelemetryLifecycle? = null
    private var activeGameId: GameId? = null
    private var activeSessionId: Long? = null

    @Volatile
    private var currentGameId: GameId? = null

    private data class CleanupAction(val lifecycle: TelemetryLifecycle?, val forwardJob: Job?, val sessionId: Long?)

    override suspend fun launchTelemetry() {
        stateMutex.withLock {
            if (monitorJob != null) return
            monitorJob = scope.launch {
                var autoJob: Job? = null
                gameSettings.observeSelection().distinctUntilChanged().collect { selection ->
                    autoJob?.cancelAndJoin()
                    autoJob = null

                    when (selection) {
                        GameSelection.Auto -> {
                            switchTo(null)
                            autoJob = launch {
                                autoGameIdFlow()
                                    .distinctUntilChanged()
                                    .filter { gameId -> currentGameId != gameId }
                                    .collect { gameId ->
                                        currentGameId = gameId
                                        switchTo(gameId)
                                    }
                            }
                        }

                        is GameSelection.Manual -> {
                            switchTo(selection.game)
                        }
                    }
                }
            }
        }
    }

    override suspend fun finishTelemetry() {
        val monitor = stateMutex.withLock {
            val current = monitorJob
            monitorJob = null
            current
        }
        monitor?.cancelAndJoin()

        val cleanup = stateMutex.withLock { prepareCleanup() }
        executeCleanup(cleanup)

        scope.cancel()
    }

    private fun autoGameIdFlow(): Flow<GameId?> = autoDetector.observeGameWindow().map { info ->
        info?.let { GameProfiles.match(it)?.id }
    }

    private suspend fun switchTo(gameId: GameId?) {
        val cleanup = stateMutex.withLock {
            if (gameId == activeGameId) return@withLock null
            prepareCleanup()
        }

        executeCleanup(cleanup)

        if (gameId == null) return

        val next = gameLifecycles[gameId.id]
        if (next == null) {
            logger.warn { "[telemetry] no lifecycle registered for game=$gameId" }
            return
        }

        stateMutex.withLock {
            if (monitorJob == null) return
            activeLifecycle = next
            activeGameId = gameId
            activeSessionId = null
            attachForwarders(next)
        }
        next.launchTelemetry()
    }

    private fun prepareCleanup(): CleanupAction? {
        val lifecycle = activeLifecycle ?: return null
        val forwarder = forwardJob
        val sessionId = activeSessionId

        activeLifecycle = null
        activeGameId = null
        activeSessionId = null
        forwardJob = null

        return CleanupAction(
            lifecycle = lifecycle,
            forwardJob = forwarder,
            sessionId = sessionId,
        )
    }

    private suspend fun executeCleanup(action: CleanupAction?) {
        if (action == null) return

        action.lifecycle?.finishTelemetry()
        action.forwardJob?.cancelAndJoin()

        val sessionId = action.sessionId
        if (sessionId != null) {
            _events.emit(SessionEnded(sessionId, SessionEndReason.SIM_DISCONNECTED))
        }
        _events.emit(TelemetryLifecycleEvent.SimDisconnected)
    }

    private fun attachForwarders(source: TelemetryLifecycle) {
        forwardJob?.cancel()
        forwardJob = scope.launch {
            launch {
                source.frames.collect { frame ->
                    _frames.emit(frame)
                }
            }
            launch {
                source.events.collect { event ->
                    handleLifecycleEvent(event)
                }
            }
        }
    }

    private suspend fun handleLifecycleEvent(event: TelemetryLifecycleEvent) {
        stateMutex.withLock {
            when (event) {
                is TelemetryLifecycleEvent.SessionStarted -> {
                    activeSessionId = event.session.sessionId
                }

                is TelemetryLifecycleEvent.SessionResumed -> {
                    activeSessionId = event.sessionId
                }

                is SessionEnded,
                TelemetryLifecycleEvent.SimDisconnected,
                    -> {
                    activeSessionId = null
                }

                else -> Unit
            }
        }
        _events.emit(event)
    }
}
