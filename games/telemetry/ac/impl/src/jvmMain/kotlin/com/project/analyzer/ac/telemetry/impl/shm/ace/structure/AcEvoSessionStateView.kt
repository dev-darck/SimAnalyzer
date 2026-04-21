package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "phaseNameRaw",
    "timeLeftRaw",
    "timeLeftMsRaw",
    "waitTimeRaw",
    "padding0",
    "totalLapRaw",
    "currentLapRaw",
    "lightsOnRaw",
    "lightsModeRaw",
    "lapLengthKmRaw",
    "endSessionFlagRaw",
    "timeToNextSessionRaw",
    "disconnectedFromServerRaw",
    "restartSeasonEnabledRaw",
    "uiEnableDriveRaw",
    "uiEnableSetupRaw",
    "isReadyToNextBlinkingRaw",
    "showWaitingForPlayersRaw",
    "padding1",
)
public class AcEvoSessionStateView : Pack4Structure(Pointer.NULL) {

    @JvmField
    var phaseNameRaw: ByteArray = ByteArray(33)

    @JvmField
    var timeLeftRaw: ByteArray = ByteArray(15)

    @JvmField
    var timeLeftMsRaw: Int = 0

    @JvmField
    var waitTimeRaw: ByteArray = ByteArray(15)

    @JvmField
    var padding0: Byte = 0

    @JvmField
    var totalLapRaw: Int = 0

    @JvmField
    var currentLapRaw: Int = 0

    @JvmField
    var lightsOnRaw: Int = 0

    @JvmField
    var lightsModeRaw: Int = 0

    @JvmField
    var lapLengthKmRaw: Float = 0f

    @JvmField
    var endSessionFlagRaw: Int = 0

    @JvmField
    var timeToNextSessionRaw: ByteArray = ByteArray(15)

    @JvmField
    var disconnectedFromServerRaw: Byte = 0

    @JvmField
    var restartSeasonEnabledRaw: Byte = 0

    @JvmField
    var uiEnableDriveRaw: Byte = 0

    @JvmField
    var uiEnableSetupRaw: Byte = 0

    @JvmField
    var isReadyToNextBlinkingRaw: Byte = 0

    @JvmField
    var showWaitingForPlayersRaw: Byte = 0

    @JvmField
    var padding1: ByteArray = ByteArray(143)

    val phaseName: String get() = phaseNameRaw.toAceCString()
    val timeLeft: String get() = timeLeftRaw.toAceCString()
    val timeLeftMs: Int get() = timeLeftMsRaw
    val waitTime: String get() = waitTimeRaw.toAceCString()
    val totalLap: Int get() = totalLapRaw
    val currentLap: Int get() = currentLapRaw
    val lightsOn: Int get() = lightsOnRaw
    val lightsMode: Int get() = lightsModeRaw
    val lapLengthKm: Float get() = lapLengthKmRaw
    val endSessionFlag: Int get() = endSessionFlagRaw
    val timeToNextSession: String get() = timeToNextSessionRaw.toAceCString()
    val disconnectedFromServer: Boolean get() = disconnectedFromServerRaw.toAceBool()
    val restartSeasonEnabled: Boolean get() = restartSeasonEnabledRaw.toAceBool()
    val uiEnableDrive: Boolean get() = uiEnableDriveRaw.toAceBool()
    val uiEnableSetup: Boolean get() = uiEnableSetupRaw.toAceBool()
    val isReadyToNextBlinking: Boolean get() = isReadyToNextBlinkingRaw.toAceBool()
    val showWaitingForPlayers: Boolean get() = showWaitingForPlayersRaw.toAceBool()

    companion object {

        const val SIZE_BYTES: Int = 256
    }
}
