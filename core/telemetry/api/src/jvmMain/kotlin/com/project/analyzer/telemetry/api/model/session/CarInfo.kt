package com.project.analyzer.telemetry.api.model.session

public data class CarInfo(
    val carModel: String? = null,
    val carSkin: String? = null,
    val carId: Int? = null, // playerCarID

    // Static car specs
    val maxTorqueNm: Float? = null,
    val maxPowerW: Float? = null,
    val maxRpm: Int? = null,
    val maxFuelLiters: Float? = null,
    val maxTurboBoost: Float? = null,

    // Tyre info
    val tyreRadius: FloatArray? = null, // [FL, FR, RL, RR] meters
    val suspensionMaxTravel: FloatArray? = null, // [FL, FR, RL, RR] meters

    val dryTyresName: String? = null,
    val wetTyresName: String? = null,

    // Features
    val hasDRS: Boolean? = null,
    val hasERS: Boolean? = null,
    val hasKERS: Boolean? = null,
    val engineBrakeSettingsCount: Int? = null,

    // Ballast
    val ballast: Float? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarInfo) return false
        if (tyreRadius != null) {
            if (other.tyreRadius == null) return false
            if (!tyreRadius.contentEquals(other.tyreRadius)) return false
        } else if (other.tyreRadius != null) {
            return false
        }
        if (suspensionMaxTravel != null) {
            if (other.suspensionMaxTravel == null) return false
            if (!suspensionMaxTravel.contentEquals(other.suspensionMaxTravel)) return false
        } else if (other.suspensionMaxTravel != null) {
            return false
        }
        return carModel == other.carModel &&
            carSkin == other.carSkin &&
            carId == other.carId &&
            maxTorqueNm == other.maxTorqueNm &&
            maxPowerW == other.maxPowerW &&
            maxRpm == other.maxRpm &&
            maxFuelLiters == other.maxFuelLiters
    }

    override fun hashCode(): Int {
        var result = carModel?.hashCode() ?: 0
        result = 31 * result + (carSkin?.hashCode() ?: 0)
        result = 31 * result + (carId ?: 0)
        result = 31 * result + (maxTorqueNm?.hashCode() ?: 0)
        result = 31 * result + (maxPowerW?.hashCode() ?: 0)
        result = 31 * result + (maxRpm ?: 0)
        result = 31 * result + (maxFuelLiters?.hashCode() ?: 0)
        result = 31 * result + (tyreRadius?.contentHashCode() ?: 0)
        result = 31 * result + (suspensionMaxTravel?.contentHashCode() ?: 0)
        return result
    }
}
