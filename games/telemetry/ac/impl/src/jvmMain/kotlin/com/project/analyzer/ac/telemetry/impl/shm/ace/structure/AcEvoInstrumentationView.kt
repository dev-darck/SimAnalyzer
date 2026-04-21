package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "mainLightStageRaw",
    "specialLightStageRaw",
    "cockpitLightStageRaw",
    "wiperLevelRaw",
    "rainLightsRaw",
    "directionLightLeftRaw",
    "directionLightRightRaw",
    "flashingLightsRaw",
    "warningLightsRaw",
    "selectedDisplayIndexRaw",
    "displayCurrentPageIndexRaw",
    "areHeadlightsVisibleRaw",
    "padding0",
)
public class AcEvoInstrumentationView : Pack4Structure(Pointer.NULL) {

    @JvmField
    var mainLightStageRaw: Byte = 0

    @JvmField
    var specialLightStageRaw: Byte = 0

    @JvmField
    var cockpitLightStageRaw: Byte = 0

    @JvmField
    var wiperLevelRaw: Byte = 0

    @JvmField
    var rainLightsRaw: Byte = 0

    @JvmField
    var directionLightLeftRaw: Byte = 0

    @JvmField
    var directionLightRightRaw: Byte = 0

    @JvmField
    var flashingLightsRaw: Byte = 0

    @JvmField
    var warningLightsRaw: Byte = 0

    @JvmField
    var selectedDisplayIndexRaw: Byte = 0

    @JvmField
    var displayCurrentPageIndexRaw: ByteArray = ByteArray(16)

    @JvmField
    var areHeadlightsVisibleRaw: Byte = 0

    @JvmField
    var padding0: ByteArray = ByteArray(101)

    val mainLightStage: Byte get() = mainLightStageRaw
    val specialLightStage: Byte get() = specialLightStageRaw
    val cockpitLightStage: Byte get() = cockpitLightStageRaw
    val wiperLevel: Byte get() = wiperLevelRaw
    val rainLights: Boolean get() = rainLightsRaw.toAceBool()
    val directionLightLeft: Boolean get() = directionLightLeftRaw.toAceBool()
    val directionLightRight: Boolean get() = directionLightRightRaw.toAceBool()
    val flashingLights: Boolean get() = flashingLightsRaw.toAceBool()
    val warningLights: Boolean get() = warningLightsRaw.toAceBool()
    val selectedDisplayIndex: Byte get() = selectedDisplayIndexRaw
    val displayCurrentPageIndex: IntArray get() = displayCurrentPageIndexRaw.map(Byte::toAceUInt8).toIntArray()
    val areHeadlightsVisible: Boolean get() = areHeadlightsVisibleRaw.toAceBool()
    val currentDisplayPage: Int
        get() {
            val index = selectedDisplayIndex.toInt()
            if (index !in 0 until 16) return 0
            return displayCurrentPageIndex[index]
        }

    companion object {

        const val SIZE_BYTES: Int = 128
    }
}
