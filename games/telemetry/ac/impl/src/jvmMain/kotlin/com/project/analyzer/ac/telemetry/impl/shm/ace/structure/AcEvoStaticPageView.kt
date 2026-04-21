package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "smVersionRaw",
    "acEvoVersionRaw",
    "padding0",
    "sessionRaw",
    "sessionNameRaw",
    "eventIdRaw",
    "sessionIdRaw",
    "padding1",
    "startingGripRaw",
    "startingAmbientTemperatureCRaw",
    "startingGroundTemperatureCRaw",
    "isStaticWeatherRaw",
    "isTimedRaceRaw",
    "isOnlineRaw",
    "padding2",
    "numberOfSessionsRaw",
    "nationRaw",
    "padding3",
    "longitudeRaw",
    "latitudeRaw",
    "trackRaw",
    "trackConfigurationRaw",
    "padding4",
    "trackLengthMRaw",
    "padding5",
)
public class AcEvoStaticPageView : Pack4Structure(Pointer.NULL) {

    @JvmField
    var smVersionRaw: ByteArray = ByteArray(15)

    @JvmField
    var acEvoVersionRaw: ByteArray = ByteArray(15)

    @JvmField
    var padding0: ByteArray = ByteArray(2)

    @JvmField
    var sessionRaw: Int = 0

    @JvmField
    var sessionNameRaw: ByteArray = ByteArray(33)

    @JvmField
    var eventIdRaw: Byte = 0

    @JvmField
    var sessionIdRaw: Byte = 0

    @JvmField
    var padding1: Byte = 0

    @JvmField
    var startingGripRaw: Int = 0

    @JvmField
    var startingAmbientTemperatureCRaw: Float = 0f

    @JvmField
    var startingGroundTemperatureCRaw: Float = 0f

    @JvmField
    var isStaticWeatherRaw: Byte = 0

    @JvmField
    var isTimedRaceRaw: Byte = 0

    @JvmField
    var isOnlineRaw: Byte = 0

    @JvmField
    var padding2: Byte = 0

    @JvmField
    var numberOfSessionsRaw: Int = 0

    @JvmField
    var nationRaw: ByteArray = ByteArray(33)

    @JvmField
    var padding3: ByteArray = ByteArray(3)

    @JvmField
    var longitudeRaw: Float = 0f

    @JvmField
    var latitudeRaw: Float = 0f

    @JvmField
    var trackRaw: ByteArray = ByteArray(33)

    @JvmField
    var trackConfigurationRaw: ByteArray = ByteArray(33)

    @JvmField
    var padding4: ByteArray = ByteArray(2)

    @JvmField
    var trackLengthMRaw: Float = 0f

    @JvmField
    var padding5: ByteArray = ByteArray(48)

    val smVersion: String get() = smVersionRaw.toAceCString()
    val acEvoVersion: String get() = acEvoVersionRaw.toAceCString()
    val session: AcEvoSessionType get() = AcEvoSessionType.fromRaw(sessionRaw)
    val sessionName: String get() = sessionNameRaw.toAceCString()
    val eventId: Int get() = eventIdRaw.toAceUInt8()
    val sessionId: Int get() = sessionIdRaw.toAceUInt8()
    val startingGrip: AcEvoStartingGrip? get() = AcEvoStartingGrip.fromRaw(startingGripRaw)
    val startingAmbientTemperatureC: Float get() = startingAmbientTemperatureCRaw
    val startingGroundTemperatureC: Float get() = startingGroundTemperatureCRaw
    val isStaticWeather: Boolean get() = isStaticWeatherRaw.toAceBool()
    val isTimedRace: Boolean get() = isTimedRaceRaw.toAceBool()
    val isOnline: Boolean get() = isOnlineRaw.toAceBool()
    val numberOfSessions: Int get() = numberOfSessionsRaw
    val nation: String get() = nationRaw.toAceCString()
    val longitude: Float get() = longitudeRaw
    val latitude: Float get() = latitudeRaw
    val track: String get() = trackRaw.toAceCString()
    val trackConfiguration: String get() = trackConfigurationRaw.toAceCString()
    val trackLengthM: Float get() = trackLengthMRaw

    public companion object {

        public const val SIZE_BYTES: Int = 256
    }
}
