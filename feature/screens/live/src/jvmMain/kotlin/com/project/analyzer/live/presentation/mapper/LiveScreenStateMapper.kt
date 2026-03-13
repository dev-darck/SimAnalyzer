package com.project.analyzer.live.presentation.mapper

import com.project.analyzer.live.presentation.LiveScreenState
import com.project.analyzer.live.presentation.components.ElectronicItemLabel
import com.project.analyzer.live.presentation.components.ElectronicItemUi
import com.project.analyzer.live.presentation.components.ElectronicsBlockUi
import com.project.analyzer.live.presentation.components.ElectronicsTitle
import com.project.analyzer.live.presentation.components.Sector
import com.project.analyzer.live.presentation.components.ValueStatus
import com.project.analyzer.live.presentation.components.WheelPos
import com.project.analyzer.live.presentation.components.WheelUi
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.api.model.car.wheels.WheelFrame
import com.project.analyzer.telemetry.api.model.lap.SectorFrame
import com.project.analyzer.telemetry.api.model.lap.SectorStatus
import com.project.analyzer.utils.ext.formatDeltaTime
import com.project.analyzer.utils.ext.formatSectorTime
import com.project.analyzer.utils.ext.fromMsToLapTime
import com.project.analyzer.utils.ext.isTyrePressureOptimal
import com.project.analyzer.utils.ext.toDisplayGear
import com.project.analyzer.utils.ext.toMaxRpmScale
import com.project.analyzer.utils.ext.toRpmScale
import com.project.analyzer.utils.ext.toSteerDegrees
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Inject
internal class LiveScreenStateMapper {

    private val logger = logger()
    private val gearFilter = GearFilter(100.milliseconds)
    private val sectorFilter = SectorsFilter(500.milliseconds)

    fun map(frame: TelemetryFrame): LiveScreenState? {
        val car = frame.car ?: return null
        val rpm = car.engine?.rpm ?: 0
        val sessionMaxRpm = frame.session?.car?.maxRpm ?: 0
        val engineMaxRpm = car.engine?.maxRpm ?: 0

        val safeSessionMax = sessionMaxRpm.takeIf { it in 1..MAX_SANE_RPM } ?: 0
        val safeEngineMax = engineMaxRpm.takeIf { it in 1..MAX_SANE_RPM } ?: 0
        val maxRpm = maxOf(safeEngineMax, safeSessionMax).takeIf { it > 0 } ?: DEFAULT_MAX_RPM

        logger.atInfo(RATE_LIMITED) {
            message = "rpm=$rpm maxRpm=$maxRpm (engine=$engineMaxRpm session=$sessionMaxRpm)"
        }

        val fuel = car.fuel
        val (deltaLastLap, isDeltaLastLapPositive) = calculateLastLapDelta(frame)

        return LiveScreenState(
            speedKmh = car.speedKmh?.roundToInt() ?: 0,
            rpmInt = rpm,
            rpmScale = rpm.toRpmScale(),
            maxRpmScale = maxRpm.toMaxRpmScale(),
            gear = gearFilter.filter((car.engine?.gear ?: 1).toDisplayGear()),

            lapCount = frame.lap?.currentLapIndex
                ?: frame.lap?.completedLaps?.let { it + 1 }
                ?: frame.session?.completedLaps?.let { it + 1 }
                ?: 0,
            bestLapTime = (frame.lap?.bestLapTimeMs)?.fromMsToLapTime() ?: "0:00.000",
            currentLapTime = frame.lap?.currentLapTimeMs?.fromMsToLapTime() ?: "0:00.000",
            lastLapTime = frame.lap?.lastLapTimeMs?.fromMsToLapTime() ?: "0:00.000",
            deltaCurrentTime = formatDeltaTime(frame.lap?.deltaLapTimeMs, frame.lap?.isDeltaPositive),
            deltaCurrentIsPositive = frame.lap?.isDeltaPositive ?: false,
            deltaLastTime = deltaLastLap,
            deltaLastIsPositive = isDeltaLastLapPositive,

            clutch = car.controls?.clutch ?: 0f,
            brake = car.controls?.brake ?: 0f,
            throttle = car.controls?.throttle ?: 0f,

            steerDeg = (car.controls?.steerAngle ?: 0f).toSteerDegrees(),

            fuelLiters = fuel?.fuelLiters ?: 0f,
            estLaps = fuel?.fuelEstimatedLaps ?: 0f,
            fuelPerLap = fuel?.fuelPerLapLiters ?: 0f,

            sectors = sectorFilter
                .filter(mapSectors(frame.lap?.sectors), frame.lap?.currentLapIndex ?: 0)
                .toImmutableList(),
            electronics = mapElectronics(frame),
            wheels = mapWheels(frame),
        )
    }

    private fun calculateLastLapDelta(frame: TelemetryFrame): Pair<String, Boolean> {
        val last = frame.lap?.lastLapTimeMs ?: return "+0.000" to true
        val best = frame.lap?.bestLapTimeMs ?: return "+0.000" to true
        if (last <= 0 || best <= 0) return "+0.000" to true

        val delta = last - best
        return formatDeltaTime(delta, delta >= 0) to (delta >= 0)
    }

    private fun mapSectors(sectors: List<SectorFrame>?): ImmutableList<Sector> {
        val byIndex = sectors.orEmpty().associateBy { it.index }

        return (0 until 3).map { idx ->
            val sector = byIndex[idx]
            Sector(
                index = idx + 1,
                value = sector?.timeMs?.formatSectorTime() ?: "--.--",
                status = sector?.let(::mapSectorStatus) ?: ValueStatus.NORMAL,
            )
        }.toImmutableList()
    }

    private fun mapSectorStatus(sector: SectorFrame): ValueStatus = when (sector.status) {
        SectorStatus.COMPLETED if sector.deltaToBestMs != null &&
            (sector.deltaToBestMs ?: 0) <= 0 -> ValueStatus.BEST

        SectorStatus.COMPLETED -> ValueStatus.COMPLETED

        else -> ValueStatus.NORMAL
    }

    private fun mapElectronics(frame: TelemetryFrame): ElectronicsBlockUi {
        val car = frame.car
        val assists = car?.assists
        val wheels = frame.wheels

        val tcInAction = assists?.tcInAction ?: false
        val absInAction = assists?.absInAction ?: false

        val title = when {
            tcInAction && absInAction -> ElectronicsTitle.TcAbsActive
            tcInAction -> ElectronicsTitle.TcActive
            absInAction -> ElectronicsTitle.AbsActive
            else -> ElectronicsTitle.Dynamics
        }

        val brakeBiasPct = (car?.controls?.brakeBias ?: 0f) * 100f

        val wheelsList = listOf(wheels?.fl, wheels?.fr, wheels?.rl, wheels?.rr)
        val maxSlip = wheelsList.maxOfOrNull { it?.slip ?: 0f } ?: 0f
        val slipWarning = maxSlip > 0.10f

        val maxLoadN = wheelsList.maxOfOrNull { it?.load ?: 0f } ?: 0f
        val maxLoadKn = maxLoadN / 1000f

        val avgTyreTemp = wheelsList
            .mapNotNull { it?.avgTempC }
            .takeIf { it.isNotEmpty() }
            ?.average()?.toFloat() ?: 0f

        val items = persistentListOf(
            ElectronicItemUi(
                label = ElectronicItemLabel.SlipMax,
                value = "%.2f".format(Locale.US, maxSlip),
                highlighted = slipWarning,
            ),
            ElectronicItemUi(
                label = ElectronicItemLabel.Load,
                value = "${"%.1f".format(Locale.US, maxLoadKn)}kN",
                highlighted = false,
            ),
            ElectronicItemUi(
                label = ElectronicItemLabel.TyreAverage,
                value = "${"%.1f".format(Locale.US, avgTyreTemp)}°",
                highlighted = avgTyreTemp !in 60f..100f,
            ),
            ElectronicItemUi(
                label = ElectronicItemLabel.BrakeBias,
                value = "${"%.1f".format(Locale.US, brakeBiasPct)}%",
                highlighted = false,
            ),
        )

        return ElectronicsBlockUi(title = title, items = items)
    }

    private fun mapWheels(frame: TelemetryFrame): ImmutableList<WheelUi> {
        val wheels = frame.wheels
        return persistentListOf(
            mapWheel(WheelPos.FL, wheels?.fl),
            mapWheel(WheelPos.FR, wheels?.fr),
            mapWheel(WheelPos.RL, wheels?.rl),
            mapWheel(WheelPos.RR, wheels?.rr),
        )
    }

    private fun mapWheel(pos: WheelPos, wheel: WheelFrame?): WheelUi {
        val psi = wheel?.pressurePsi ?: 0f
        val tyreTemp = wheel?.coreTempC ?: wheel?.avgTempC ?: 0f
        val susMm = ((wheel?.suspensionTravel ?: 0f) * 1000f).roundToInt().coerceAtLeast(0)
        val slip = wheel?.slip ?: 0f
        val brakeTemp = wheel?.brakeTempC ?: 0f

        return WheelUi(
            pos = pos,
            psi = psi,
            tyreTempC = tyreTemp,
            susMm = susMm,
            psiOk = psi.isTyrePressureOptimal(),
            slip = slip,
            brakeTempC = brakeTemp,
        )
    }

    private companion object {

        const val MAX_SANE_RPM = 25_000

        const val DEFAULT_MAX_RPM = 8000
    }
}
