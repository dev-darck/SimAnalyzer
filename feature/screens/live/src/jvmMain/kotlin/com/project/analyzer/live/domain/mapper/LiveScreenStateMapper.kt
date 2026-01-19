package com.project.analyzer.live.domain.mapper

import com.project.analyzer.live.presentation.LiveScreenState
import com.project.analyzer.live.presentation.components.ElectronicItemUi
import com.project.analyzer.live.presentation.components.ElectronicsBlockUi
import com.project.analyzer.live.presentation.components.Sector
import com.project.analyzer.live.presentation.components.ValueStatus
import com.project.analyzer.live.presentation.components.WheelPos
import com.project.analyzer.live.presentation.components.WheelUi
import com.project.analyzer.telemetry.ac.api.model.TelemetryFrame
import com.project.analyzer.telemetry.ac.api.model.car.wheels.WheelFrame
import com.project.analyzer.telemetry.ac.api.model.lap.SectorFrame
import com.project.analyzer.telemetry.ac.api.model.lap.SectorStatus
import com.project.analyzer.utils.ext.formatDeltaTime
import com.project.analyzer.utils.ext.formatSectorTime
import com.project.analyzer.utils.ext.fromMsToLapTime
import com.project.analyzer.utils.ext.isTyrePressureOptimal
import com.project.analyzer.utils.ext.toDisplayGear
import com.project.analyzer.utils.ext.toMaxRpmScale
import com.project.analyzer.utils.ext.toRpmScale
import com.project.analyzer.utils.ext.toSteerDegrees
import dev.zacsweers.metro.Inject
import java.util.Locale
import kotlin.math.roundToInt

@Inject
internal class LiveScreenStateMapper {

    fun map(frame: TelemetryFrame): LiveScreenState? {
        val car = frame.car ?: return null
        val rpm = car.engine?.rpm ?: 0
        val sessionMaxRpm = frame.session?.car?.maxRpm ?: 0
        val engineMaxRpm = car.engine?.maxRpm ?: 0
        val maxRpm = maxOf(engineMaxRpm, sessionMaxRpm).takeIf { it > 0 } ?: 8000

        val fuel = car.fuel

        return LiveScreenState(
            speedKmh = car.speedKmh?.roundToInt() ?: 0,
            rpmInt = rpm,
            rpmScale = rpm.toRpmScale(),
            maxRpmScale = maxRpm.toMaxRpmScale(),
            gear = (car.engine?.gear ?: 1).toDisplayGear(),

            lapCount = frame.lap?.completedLaps ?: frame.session?.completedLaps ?: 0,
            bestLapTime = frame.lap?.bestLapTimeMs?.fromMsToLapTime() ?: "0:00.000",
            currentLapTime = frame.lap?.currentLapTimeMs?.fromMsToLapTime() ?: "0:00.000",
            lastLapTime = frame.lap?.lastLapTimeMs?.fromMsToLapTime() ?: "0:00.000",
            deltaCurrentTime = formatDeltaTime(frame.lap?.deltaLapTimeMs, frame.lap?.isDeltaPositive),
            deltaLastTime = "-0.000",

            clutch = car.controls?.clutch ?: 0f,
            brake = car.controls?.brake ?: 0f,
            throttle = car.controls?.throttle ?: 0f,

            steerDeg = (car.controls?.steerAngle ?: 0f).toSteerDegrees(),

            fuelLiters = fuel?.fuelLiters ?: 0f,
            estLaps = fuel?.fuelEstimatedLaps ?: 0f,
            fuelPerLap = fuel?.fuelPerLapLiters ?: 0f,

            sectors = mapSectors(frame.lap?.sectors),
            electronics = mapElectronics(frame),
            wheels = mapWheels(frame),
        )
    }

    private fun mapSectors(sectors: List<SectorFrame>?): List<Sector> {
        if (sectors.isNullOrEmpty()) {
            return listOf(
                Sector(1, "--.--", ValueStatus.NORMAL),
                Sector(2, "--.--", ValueStatus.NORMAL),
                Sector(3, "--.--", ValueStatus.NORMAL)
            )
        }

        return sectors.map { sector ->
            Sector(
                index = sector.index + 1,
                value = sector.timeMs?.formatSectorTime() ?: "--.--",
                status = mapSectorStatus(sector)
            )
        }
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
            tcInAction && absInAction -> "⚠ TC + ABS"
            tcInAction -> "⚠ TC Active"
            absInAction -> "⚠ ABS Active"
            else -> "Dynamics"
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

        val items = listOf(
            ElectronicItemUi(
                title = "SLIP (max)",
                value = "%.2f".format(Locale.US, maxSlip),
                highlighted = slipWarning
            ),
            ElectronicItemUi(
                title = "LOAD",
                value = "${"%.1f".format(Locale.US, maxLoadKn)}kN",
                highlighted = false
            ),
            ElectronicItemUi(
                title = "TYRE (avg)",
                value = "${"%.1f".format(Locale.US, avgTyreTemp)}°",
                highlighted = avgTyreTemp !in 60f..100f
            ),
            ElectronicItemUi(
                title = "BB",
                value = "${"%.1f".format(Locale.US, brakeBiasPct)}%",
                highlighted = false
            ),
        )

        return ElectronicsBlockUi(title = title, items = items)
    }

    private fun mapWheels(frame: TelemetryFrame): List<WheelUi> {
        val wheels = frame.wheels
        return listOf(
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
            brakeTempC = brakeTemp
        )
    }
}
