package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "tcLevelRaw",
    "tcCutLevelRaw",
    "absLevelRaw",
    "escLevelRaw",
    "ebbLevelRaw",
    "padding0",
    "brakeBiasRaw",
    "engineMapLevelRaw",
    "padding1",
    "turboLevelRaw",
    "ersDeploymentMapRaw",
    "padding2",
    "ersRechargeMapRaw",
    "isErsHeatChargingOnRaw",
    "isErsOvertakeModeOnRaw",
    "isDrsOpenRaw",
    "diffPowerLevelRaw",
    "diffCoastLevelRaw",
    "frontBumpDamperLevelRaw",
    "frontReboundDamperLevelRaw",
    "rearBumpDamperLevelRaw",
    "rearReboundDamperLevelRaw",
    "isIgnitionOnRaw",
    "isPitLimiterOnRaw",
    "activePerformanceModeRaw",
    "padding3",
)
public class AcEvoElectronicsView : Pack4Structure(Pointer.NULL) {

    @JvmField
    var tcLevelRaw: Byte = 0

    @JvmField
    var tcCutLevelRaw: Byte = 0

    @JvmField
    var absLevelRaw: Byte = 0

    @JvmField
    var escLevelRaw: Byte = 0

    @JvmField
    var ebbLevelRaw: Byte = 0

    @JvmField
    var padding0: ByteArray = ByteArray(3)

    @JvmField
    var brakeBiasRaw: Float = 0f

    @JvmField
    var engineMapLevelRaw: Byte = 0

    @JvmField
    var padding1: ByteArray = ByteArray(3)

    @JvmField
    var turboLevelRaw: Float = 0f

    @JvmField
    var ersDeploymentMapRaw: Byte = 0

    @JvmField
    var padding2: ByteArray = ByteArray(3)

    @JvmField
    var ersRechargeMapRaw: Float = 0f

    @JvmField
    var isErsHeatChargingOnRaw: Byte = 0

    @JvmField
    var isErsOvertakeModeOnRaw: Byte = 0

    @JvmField
    var isDrsOpenRaw: Byte = 0

    @JvmField
    var diffPowerLevelRaw: Byte = 0

    @JvmField
    var diffCoastLevelRaw: Byte = 0

    @JvmField
    var frontBumpDamperLevelRaw: Byte = 0

    @JvmField
    var frontReboundDamperLevelRaw: Byte = 0

    @JvmField
    var rearBumpDamperLevelRaw: Byte = 0

    @JvmField
    var rearReboundDamperLevelRaw: Byte = 0

    @JvmField
    var isIgnitionOnRaw: Byte = 0

    @JvmField
    var isPitLimiterOnRaw: Byte = 0

    @JvmField
    var activePerformanceModeRaw: Byte = 0

    @JvmField
    var padding3: ByteArray = ByteArray(88)

    val tcLevel: Byte get() = tcLevelRaw
    val tcCutLevel: Byte get() = tcCutLevelRaw
    val absLevel: Byte get() = absLevelRaw
    val escLevel: Byte get() = escLevelRaw
    val ebbLevel: Byte get() = ebbLevelRaw
    val brakeBias: Float get() = brakeBiasRaw
    val engineMapLevel: Byte get() = engineMapLevelRaw
    val turboLevel: Float get() = turboLevelRaw
    val ersDeploymentMap: Byte get() = ersDeploymentMapRaw
    val ersRechargeMap: Float get() = ersRechargeMapRaw
    val isErsHeatChargingOn: Boolean get() = isErsHeatChargingOnRaw.toAceBool()
    val isErsOvertakeModeOn: Boolean get() = isErsOvertakeModeOnRaw.toAceBool()
    val isDrsOpen: Boolean get() = isDrsOpenRaw.toAceBool()
    val diffPowerLevel: Byte get() = diffPowerLevelRaw
    val diffCoastLevel: Byte get() = diffCoastLevelRaw
    val frontBumpDamperLevel: Byte get() = frontBumpDamperLevelRaw
    val frontReboundDamperLevel: Byte get() = frontReboundDamperLevelRaw
    val rearBumpDamperLevel: Byte get() = rearBumpDamperLevelRaw
    val rearReboundDamperLevel: Byte get() = rearReboundDamperLevelRaw
    val isIgnitionOn: Boolean get() = isIgnitionOnRaw.toAceBool()
    val isPitLimiterOn: Boolean get() = isPitLimiterOnRaw.toAceBool()
    val activePerformanceMode: Byte get() = activePerformanceModeRaw

    companion object {

        const val SIZE_BYTES: Int = 128
    }
}
