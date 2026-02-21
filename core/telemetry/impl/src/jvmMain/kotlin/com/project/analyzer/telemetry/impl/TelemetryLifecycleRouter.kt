package com.project.analyzer.telemetry.impl

import com.project.analyzer.api.di.IO
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.game.impl.GameDetector
import com.project.analyzer.game.impl.GameProfiles
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.TelemetryGameSettings
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent.SessionEnded
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.AppScope
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TelemetryLifecycle>())
class TelemetryLifecycleRouter(
    private val gameSettings: TelemetryGameSettings,
    private val gameLifecycles: Map<String, Lazy<TelemetryLifecycle>>,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryLifecycle {

    private val logger = logger()

    private val scope = CoroutineScope(
        SupervisorJob() + ioDispatcher + CoroutineExceptionHandler { _, e ->
            logger.error(e) { "uncaught exception" }
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
        coroutineDispatcher = ioDispatcher.limitedParallelism(1, "GameDetector telemetry"),
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
        if (monitorJob != null) {
            logger.debug { "launchTelemetry: already running, skipping" }
            return
        }
        logger.info { "launchTelemetry: starting monitor" }

        monitorJob = scope.launch {
            var autoJob: Job? = null

            gameSettings
                .observeSelection()
                .distinctUntilChanged()
                .collectLatest { selection ->
                    logger.info { "selection changed: $selection" }

                    autoJob?.cancelAndJoin()
                    autoJob = null

                    when (selection) {
                        GameSelection.Auto -> {
                            logger.info { "mode=Auto, clearing active game and starting detector" }
                            switchTo(null)

                            autoJob = launch {
                                autoGameIdFlow()
                                    .distinctUntilChanged()
                                    .filter { gameId -> currentGameId != gameId }
                                    .collect { gameId ->
                                        logger.info { "auto-detected game change: $currentGameId -> $gameId" }
                                        currentGameId = gameId
                                        switchTo(gameId)
                                    }
                            }
                        }

                        is GameSelection.Manual -> {
                            logger.info { "mode=Manual, switching to game=${selection.game}" }
                            switchTo(selection.game)
                        }
                    }
                }
        }
    }

    override suspend fun finishTelemetry() {
        logger.info { "finishTelemetry: stopping" }

        val monitor = stateMutex.withLock {
            val current = monitorJob
            monitorJob = null
            current
        }
        monitor?.cancelAndJoin()

        val cleanup = stateMutex.withLock { prepareCleanup() }
        executeCleanup(cleanup)

        scope.cancel()
        logger.info { "finishTelemetry: scope cancelled" }

        LeakCanaryRuntime.watch(this, "TelemetryLifecycleRouter")
    }

    private fun autoGameIdFlow(): Flow<GameId?> = autoDetector.observeGameWindow().map { info ->
        info?.let { GameProfiles.match(it)?.id }
    }

    private suspend fun switchTo(gameId: GameId?) {
        val cleanup = stateMutex.withLock {
            if (gameId == activeGameId) {
                logger.debug { "switchTo: gameId=$gameId is already active, skipping" }
                return@withLock null
            }
            logger.info { "switchTo: $activeGameId -> $gameId, preparing cleanup" }
            prepareCleanup()
        }

        executeCleanup(cleanup)

        if (gameId == null) {
            logger.debug { "switchTo: gameId=null, no new lifecycle to attach" }
            return
        }

        val next = gameLifecycles[gameId.id]?.value
        if (next == null) {
            logger.warn {
                "switchTo: no lifecycle registered for game=$gameId (available: ${gameLifecycles.keys})"
            }
            return
        }

        stateMutex.withLock {
            if (monitorJob == null) {
                logger.warn { "switchTo: monitorJob is null after cleanup, aborting attach for game=$gameId" }
                return
            }
            activeLifecycle = next
            activeGameId = gameId
            activeSessionId = null
            attachForwarders(next)
            logger.info { "switchTo: attached lifecycle for game=$gameId" }
        }
        next.launchTelemetry()
        logger.info { "switchTo: launched telemetry for game=$gameId" }
    }

    private fun prepareCleanup(): CleanupAction? {
        val lifecycle = activeLifecycle ?: return null
        val forwarder = forwardJob
        val sessionId = activeSessionId

        logger.info {
            "prepareCleanup: game=$activeGameId sessionId=$sessionId " +
                "forwardJob.active=${forwarder?.isActive}"
        }

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

        logger.info { "executeCleanup: finishing lifecycle, sessionId=${action.sessionId}" }

        action.lifecycle?.finishTelemetry()
        action.forwardJob?.cancelAndJoin()

        val sessionId = action.sessionId
        if (sessionId != null) {
            logger.info { "executeCleanup: emitting SessionEnded id=$sessionId reason=SIM_DISCONNECTED" }
            _events.emit(SessionEnded(sessionId, SessionEndReason.SIM_DISCONNECTED))
        }
        _events.emit(TelemetryLifecycleEvent.SimDisconnected)
        logger.info { "executeCleanup: emitted SimDisconnected" }
    }

    private fun attachForwarders(source: TelemetryLifecycle) {
        forwardJob?.cancel()
        forwardJob = scope.launch {
            logger.debug { "forwarders: started for source=$source" }

            launch {
                var frameCount = 0L
                source.frames.collect { frame ->
                    val emitted = _frames.tryEmit(frame)
                    frameCount++
                    if (!emitted) {
                        logger.atWarn(RATE_LIMITED) {
                            message = "frame dropped by router buffer (total forwarded=$frameCount)"
                        }
                    }
                    if (frameCount % 5000 == 0L) {
                        logger.atDebug(RATE_LIMITED) {
                            message = "frame forwarding heartbeat: forwarded=$frameCount " +
                                "subscribers=${_frames.subscriptionCount.value}"
                        }
                    }
                }
            }
            launch {
                source.events.collect { event ->
                    logger.debug { "forwarders: received event=${event::class.simpleName}" }
                    handleLifecycleEvent(event)
                }
                logger.warn { "forwarders: event collection ended" }
            }
        }.also { job ->
            job.invokeOnCompletion { cause ->
                if (cause != null) {
                    logger.warn(cause) { "forwardJob completed with exception" }
                } else {
                    logger.info { "forwardJob completed normally" }
                }
            }
        }
    }

    private suspend fun handleLifecycleEvent(event: TelemetryLifecycleEvent) {
        stateMutex.withLock {
            when (event) {
                is TelemetryLifecycleEvent.SessionStarted -> {
                    activeSessionId = event.session.sessionId
                    logger.info { "event: SessionStarted id=${event.session.sessionId}" }
                }

                is TelemetryLifecycleEvent.SessionResumed -> {
                    activeSessionId = event.sessionId
                    logger.info { "event: SessionResumed id=${event.sessionId}" }
                }

                is SessionEnded -> {
                    logger.info { "event: SessionEnded id=${event.sessionId} reason=${event.reason}" }
                    activeSessionId = null
                }

                is TelemetryLifecycleEvent.SimDisconnected -> {
                    logger.info { "event: SimDisconnected (activeSession=$activeSessionId)" }
                    activeSessionId = null
                }

                else -> {
                    logger.debug { "event: ${event::class.simpleName}" }
                }
            }
        }
        _events.emit(event)
    }
}
