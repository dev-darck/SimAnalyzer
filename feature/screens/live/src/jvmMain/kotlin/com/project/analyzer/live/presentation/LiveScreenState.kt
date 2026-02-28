package com.project.analyzer.live.presentation

import com.project.analyzer.live.presentation.components.ElectronicItemLabel
import com.project.analyzer.live.presentation.components.ElectronicItemUi
import com.project.analyzer.live.presentation.components.ElectronicsBlockUi
import com.project.analyzer.live.presentation.components.ElectronicsTitle
import com.project.analyzer.live.presentation.components.Sector
import com.project.analyzer.live.presentation.components.ValueStatus
import com.project.analyzer.live.presentation.components.WheelPos
import com.project.analyzer.live.presentation.components.WheelUi

internal data class LiveScreenState(
    val speedKmh: Int = 0,
    val rpmInt: Int = 0,
    val rpmScale: Float = 0f,
    val maxRpmScale: Int = 13,
    val gear: Int = 0,

    val lapCount: Int = 0,
    val bestLapTime: String = "0:00.000",
    val currentLapTime: String = "0:00.000",
    val lastLapTime: String = "0:00.000",
    val deltaCurrentTime: String = "-0.000",
    val deltaLastTime: String = "-0.000",
    val deltaCurrentIsPositive: Boolean = false,
    val deltaLastIsPositive: Boolean = false,

    val clutch: Float = 0f,
    val brake: Float = 0f,
    val throttle: Float = 0f,
    val steerDeg: Float = 0f,

    val fuelLiters: Float = 0f,
    val estLaps: Float = 0f,
    val fuelPerLap: Float = 0f,

    val sectors: List<Sector> = listOf(
        Sector(1, "--.--", ValueStatus.NORMAL),
        Sector(2, "--.--", ValueStatus.NORMAL),
        Sector(3, "--.--", ValueStatus.NORMAL),
    ),

    val electronics: ElectronicsBlockUi = ElectronicsBlockUi(
        title = ElectronicsTitle.Electronics,
        items = listOf(
            ElectronicItemUi(ElectronicItemLabel.TC, "0"),
            ElectronicItemUi(ElectronicItemLabel.ABS, "0"),
            ElectronicItemUi(ElectronicItemLabel.Map, "0"),
            ElectronicItemUi(ElectronicItemLabel.BrakeBias, "0%"),
        ),
    ),

    val wheels: List<WheelUi> = listOf(
        WheelUi(WheelPos.FL, psi = 0f, tyreTempC = 0f),
        WheelUi(WheelPos.FR, psi = 0f, tyreTempC = 0f),
        WheelUi(WheelPos.RL, psi = 0f, tyreTempC = 0f),
        WheelUi(WheelPos.RR, psi = 0f, tyreTempC = 0f),
    ),
)
