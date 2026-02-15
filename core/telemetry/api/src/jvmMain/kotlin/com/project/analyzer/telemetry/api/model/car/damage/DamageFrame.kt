package com.project.analyzer.telemetry.api.model.car.damage

public data class DamageFrame(
    // Raw damage values from game [front, rear, left, right, centre]
    val rawDamage5: FloatArray? = null,

    // Parsed damage
    val aero: AeroDamageFrame? = null,
    val suspension: SuspensionDamageFrame? = null,
    val body: BodyDamageFrame? = null,
    val tyres: TyreDamageFrame? = null,

    val numberOfTyresOut: Int? = null, // Tyres off track
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DamageFrame) return false
        if (rawDamage5 != null) {
            if (other.rawDamage5 == null) return false
            if (!rawDamage5.contentEquals(other.rawDamage5)) return false
        } else if (other.rawDamage5 != null) return false
        return aero == other.aero &&
            suspension == other.suspension &&
            body == other.body &&
            tyres == other.tyres &&
            numberOfTyresOut == other.numberOfTyresOut
    }

    override fun hashCode(): Int {
        var result = rawDamage5?.contentHashCode() ?: 0
        result = 31 * result + (aero?.hashCode() ?: 0)
        result = 31 * result + (suspension?.hashCode() ?: 0)
        result = 31 * result + (body?.hashCode() ?: 0)
        result = 31 * result + (tyres?.hashCode() ?: 0)
        result = 31 * result + (numberOfTyresOut ?: 0)
        return result
    }
}
