package com.project.analyzer.telemetry.impl

import com.project.analyzer.api.di.IO
import com.project.analyzer.game.api.GameDetectorFactory
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameProfiles
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.game.api.GameWindowDetector
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.SessionInfo
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Root telemetry orchestrator.
 *
 * Responsibilities:
 * - chooses active game lifecycle (auto/manual mode)
 * - forwards lifecycle events/frames to shared app streams
 * - guarantees cleanup ordering on game switch / app stop
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TelemetryLifecycle>())
class TelemetryLifecycleRouter(
    private val gameSettings: TelemetryGameSettings,
    private val gameLifecycles: Map<String, Lazy<TelemetryLifecycle>>,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
    gameDetectorFactory: GameDetectorFactory,
) : TelemetryLifecycle {

    private val logger = logger()
    private val routerDispatcher: CoroutineDispatcher = ioDispatcher.limitedParallelism(
        parallelism = 2,
        name = "TelemetryRouter",
    )
    private val detectorDispatcher: CoroutineDispatcher = ioDispatcher.limitedParallelism(
        parallelism = 1,
        name = "GameDetectorTelemetry",
    )
    private val forwardDispatcher: CoroutineDispatcher = ioDispatcher.limitedParallelism(
        parallelism = 2,
        name = "TelemetryRouterForward",
    )

    private val scope = CoroutineScope(
        SupervisorJob() + routerDispatcher + CoroutineExceptionHandler { _, e ->
            logger.error(e) { "uncaught exception" }
        },
    )

    private val _events = MutableSharedFlow<TelemetryLifecycleEvent>(
        replay = 1,
        extraBufferCapacity = 32,
    )
    override val events: Flow<TelemetryLifecycleEvent> = flow {
        stateMutex.withLock {
            bootstrapLifecycleEventsLocked()
        }.forEach { event ->
            emit(event)
        }
        emitAll(_events.asSharedFlow())
    }

    private val _frames = MutableSharedFlow<TelemetryFrame>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val frames: SharedFlow<TelemetryFrame> = _frames.asSharedFlow()

    private val autoDetector: GameWindowDetector = gameDetectorFactory.create(
        requireForeground = false,
        coroutineDispatcher = detectorDispatcher,
    )

    private val stateMutex = Mutex()
    private var monitorJob: Job? = null
    private var forwardJob: Job? = null
    private var activeLifecycle: TelemetryLifecycle? = null
    private var activeGameId: GameId? = null
    private var activeSessionId: Long? = null
    private var activeSessionInfo: SessionInfo? = null
    private var activeSessionPaused: Boolean = false
    private var simConnected: Boolean = false
    private var lastLifecycleEvent: TelemetryLifecycleEvent? = null

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
                    logger.debug { "selection changed: $selection" }

                    autoJob?.cancelAndJoin()
                    autoJob = null

                    when (selection) {
                        GameSelection.Auto -> {
                            logger.debug { "mode=Auto, clearing active game and starting detector" }
                            switchTo(null)

                            autoJob = launch {
                                autoGameIdFlow()
                                    .distinctUntilChanged()
                                    .collect { gameId ->
                                        logger.info { "auto-detected game change -> $gameId" }
                                        switchTo(gameId)
                                    }
                            }
                        }

                        is GameSelection.Manual -> {
                            logger.debug { "mode=Manual, switching to game=${selection.game}" }
                            switchTo(selection.game)
                        }
                    }
                }
        }
    }

    override suspend fun finishTelemetry() {
        logger.info { "finishTelemetry: stopping" }

        val monitor = stateMutex.withLock {
            monitorJob.also { monitorJob = null }
        }
        monitor?.cancelAndJoin()

        val cleanup = stateMutex.withLock { prepareCleanupLocked() }
        executeCleanup(
            lifecycle = cleanup.first,
            forwarder = cleanup.second,
            sessionId = cleanup.third,
        )

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
            prepareCleanupLocked()
        }

        executeCleanup(
            lifecycle = cleanup?.first,
            forwarder = cleanup?.second,
            sessionId = cleanup?.third,
        )

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

    private fun prepareCleanupLocked(): Triple<TelemetryLifecycle?, Job?, Long?> {
        val lifecycle = activeLifecycle
        val forwarder = forwardJob
        val sessionId = activeSessionId

        logger.debug {
            "prepareCleanup: game=$activeGameId sessionId=$sessionId " +
                "forwardJob.active=${forwarder?.isActive}"
        }

        activeLifecycle = null
        activeGameId = null
        activeSessionId = null
        activeSessionInfo = null
        activeSessionPaused = false
        simConnected = false
        lastLifecycleEvent = null
        forwardJob = null

        return Triple(lifecycle, forwarder, sessionId)
    }

    private suspend fun executeCleanup(lifecycle: TelemetryLifecycle?, forwarder: Job?, sessionId: Long?) {
        if (lifecycle == null && forwarder == null && sessionId == null) return

        logger.debug { "executeCleanup: finishing lifecycle, sessionId=$sessionId" }

        forwarder?.cancelAndJoin()
        lifecycle?.finishTelemetry()

        if (sessionId != null) {
            logger.debug { "executeCleanup: emitting SessionEnded id=$sessionId reason=SIM_DISCONNECTED" }
            emitEvent(SessionEnded(sessionId, SessionEndReason.SIM_DISCONNECTED))
        }
        emitEvent(TelemetryLifecycleEvent.SimDisconnected)
        logger.debug { "executeCleanup: emitted SimDisconnected" }
    }

    private fun attachForwarders(source: TelemetryLifecycle) {
        forwardJob?.cancel()
        forwardJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            logger.debug {
                "forwarders: started for source=$source " +
                    "framesPolicy=TRY_DROP " +
                    "eventsPolicy=SUSPEND"
            }

            launch(forwardDispatcher, start = CoroutineStart.UNDISPATCHED) {
                var frameCount = 0L
                source.frames.collect { frame ->
                    val emitted = try {
                        emitFrame(frame)
                    } catch (t: Throwable) {
                        logger.atError(RATE_LIMITED) {
                            message = "frame forwarding error"
                            cause = t
                        }
                        false
                    }
                    frameCount++
                    if (frameCount % 5000 == 0L) {
                        logger.atDebug(RATE_LIMITED) {
                            message = "frame forwarding heartbeat: forwarded=$frameCount " +
                                "subscribers=${_frames.subscriptionCount.value}"
                        }
                    }
                    if (!emitted) Unit
                }
            }
            launch(forwardDispatcher, start = CoroutineStart.UNDISPATCHED) {
                source.events.collect { event ->
                    logger.debug { "forwarders: received event=${event::class.simpleName}" }
                    handleLifecycleEvent(event)
                }
                logger.debug { "forwarders: event collection ended" }
            }
        }.also { job ->
            job.invokeOnCompletion { cause ->
                when (cause) {
                    is CancellationException -> logger.debug { "forwardJob cancelled" }
                    null -> logger.debug { "forwardJob completed normally" }
                    else -> logger.warn(cause) { "forwardJob completed with exception" }
                }
            }
        }
    }

    private suspend fun handleLifecycleEvent(event: TelemetryLifecycleEvent) {
        stateMutex.withLock {
            when (event) {
                TelemetryLifecycleEvent.SimConnected -> {
                    simConnected = true
                    logger.debug { "event: SimConnected" }
                }

                is TelemetryLifecycleEvent.SessionStarted -> {
                    activeSessionId = event.session.sessionId
                    activeSessionInfo = event.session
                    activeSessionPaused = false
                    simConnected = true
                    logger.info { "event: SessionStarted id=${event.session.sessionId}" }
                }

                is TelemetryLifecycleEvent.SessionUpdated -> {
                    if (activeSessionId == null || activeSessionId == event.session.sessionId) {
                        activeSessionId = event.session.sessionId
                        activeSessionInfo = mergeStickySessionInfo(activeSessionInfo, event.session)
                    }
                    simConnected = true
                    logger.debug { "event: SessionUpdated id=${event.session.sessionId}" }
                }

                is TelemetryLifecycleEvent.SessionPaused -> {
                    if (activeSessionId == null || activeSessionId == event.sessionId) {
                        activeSessionId = event.sessionId
                    }
                    activeSessionPaused = true
                    simConnected = true
                    logger.debug { "event: SessionPaused id=${event.sessionId}" }
                }

                is TelemetryLifecycleEvent.SessionResumed -> {
                    activeSessionId = event.sessionId
                    activeSessionPaused = false
                    simConnected = true
                    logger.debug { "event: SessionResumed id=${event.sessionId}" }
                }

                is SessionEnded -> {
                    logger.info { "event: SessionEnded id=${event.sessionId} reason=${event.reason}" }
                    activeSessionId = null
                    activeSessionInfo = null
                    activeSessionPaused = false
                }

                is TelemetryLifecycleEvent.SimDisconnected -> {
                    logger.info { "event: SimDisconnected (activeSession=$activeSessionId)" }
                    activeSessionId = null
                    activeSessionInfo = null
                    activeSessionPaused = false
                    simConnected = false
                }

                else -> {
                    logger.debug { "event: ${event::class.simpleName}" }
                }
            }
            lastLifecycleEvent = event
        }
        try {
            emitEvent(event)
        } catch (t: Throwable) {
            logger.atError(RATE_LIMITED) {
                message = "event forwarding error event=${event::class.simpleName}"
                cause = t
            }
            throw t
        }
    }

    private suspend fun emitEvent(event: TelemetryLifecycleEvent) {
        _events.emit(event)
    }

    private fun bootstrapLifecycleEventsLocked(): List<TelemetryLifecycleEvent> {
        val session = activeSessionInfo
        val lastEvent = lastLifecycleEvent
        if (!simConnected || session == null) return emptyList()

        val events = mutableListOf<TelemetryLifecycleEvent>()
        val lastStartedSessionId = (lastEvent as? TelemetryLifecycleEvent.SessionStarted)?.session?.sessionId
        if (lastStartedSessionId != session.sessionId) {
            events += TelemetryLifecycleEvent.SessionStarted(session)
        }

        if (activeSessionPaused) {
            val lastPausedSessionId = (lastEvent as? TelemetryLifecycleEvent.SessionPaused)?.sessionId
            if (lastPausedSessionId != session.sessionId) {
                events += TelemetryLifecycleEvent.SessionPaused(session.sessionId)
            }
        }

        return events
    }

    private fun mergeStickySessionInfo(current: SessionInfo?, incoming: SessionInfo): SessionInfo {
        if (current == null) return incoming

        fun pick(currentValue: String, incomingValue: String): String = incomingValue.ifBlank { currentValue }

        return current.copy(
            sessionId = incoming.sessionId,
            sessionType = incoming.sessionType.takeUnless { it == current.sessionType || it.name == "UNKNOWN" }
                ?: current.sessionType,
            carModel = pick(current.carModel, incoming.carModel),
            trackId = pick(current.trackId, incoming.trackId),
            carId = incoming.carId ?: current.carId,
            layoutId = incoming.layoutId ?: current.layoutId,
        )
    }

    private fun emitFrame(frame: TelemetryFrame): Boolean {
        val emitted = _frames.tryEmit(frame)
        if (!emitted) {
            logger.atWarn(RATE_LIMITED) {
                message = "router sink dropped item sink=frames policy=TRY_DROP " +
                    "subscribers=${_frames.subscriptionCount.value}"
            }
        }
        return emitted
    }
}
