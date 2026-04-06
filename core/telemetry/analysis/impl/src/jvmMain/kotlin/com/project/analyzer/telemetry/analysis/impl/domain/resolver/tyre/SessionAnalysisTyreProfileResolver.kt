package com.project.analyzer.telemetry.analysis.impl.domain.resolver.tyre

import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreCompoundFamily
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass
import dev.zacsweers.metro.Inject

/**
 * Resolves class-aware tyre operating windows from sparse telemetry labels so tyre diagnostics stay useful
 * even when the sim exposes only partial compound metadata.
 */
@Inject
class SessionAnalysisTyreProfileResolver {

    /**
     * Picks the most plausible tyre profile for the current car class, falling back to stable defaults
     * when the telemetry label is too vague to identify an exact compound.
     */
    fun resolve(
        vehicleClass: SessionAnalysisVehicleClass,
        tyreCompoundLabel: String?,
        isRainTyres: Boolean?,
    ): SessionAnalysisTyreProfile {
        val compoundFamily = resolveCompoundFamily(
            tyreCompoundLabel = tyreCompoundLabel,
            isRainTyres = isRainTyres,
        )

        return when (vehicleClass) {
            SessionAnalysisVehicleClass.F1,
            SessionAnalysisVehicleClass.Formula,
                -> formulaProfile(compoundFamily, tyreCompoundLabel)

            SessionAnalysisVehicleClass.GT3 -> gt3Profile(compoundFamily, tyreCompoundLabel)

            SessionAnalysisVehicleClass.GT4 -> gt4Profile(compoundFamily, tyreCompoundLabel)

            SessionAnalysisVehicleClass.GT2 -> gt2Profile(compoundFamily, tyreCompoundLabel)

            SessionAnalysisVehicleClass.GTE -> gteProfile(compoundFamily, tyreCompoundLabel)

            SessionAnalysisVehicleClass.TCR -> tcrProfile(compoundFamily, tyreCompoundLabel)

            SessionAnalysisVehicleClass.Prototype,
            SessionAnalysisVehicleClass.LMP2,
            SessionAnalysisVehicleClass.LMH,
                -> prototypeProfile(compoundFamily, tyreCompoundLabel, vehicleClass)

            SessionAnalysisVehicleClass.Unknown -> genericProfile(compoundFamily, tyreCompoundLabel)
        }
    }

    /**
     * Normalizes raw sim-specific compound labels into a broad family that the rest of the resolver can reason about.
     */
    fun resolveCompoundFamily(tyreCompoundLabel: String?, isRainTyres: Boolean?): SessionAnalysisTyreCompoundFamily {
        if (isRainTyres == true) return SessionAnalysisTyreCompoundFamily.Wet

        val raw = tyreCompoundLabel.orEmpty().lowercase()
        return when {
            raw.contains("inter") -> SessionAnalysisTyreCompoundFamily.Intermediate
            raw.contains("wet") || raw.contains("rain") -> SessionAnalysisTyreCompoundFamily.Wet
            raw.contains("soft") || raw.contains("c5") || raw.contains("c4") -> SessionAnalysisTyreCompoundFamily.Soft
            raw.contains("medium") || raw.contains("c3") -> SessionAnalysisTyreCompoundFamily.Medium
            raw.contains("hard") || raw.contains("c2") || raw.contains("c1") -> SessionAnalysisTyreCompoundFamily.Hard
            raw.contains("slick") || raw.contains("dry") -> SessionAnalysisTyreCompoundFamily.Slick
            else -> SessionAnalysisTyreCompoundFamily.Unknown
        }
    }

    fun genericProfile(
        compoundFamily: SessionAnalysisTyreCompoundFamily,
        tyreCompoundLabel: String?,
    ): SessionAnalysisTyreProfile = when (compoundFamily) {
        SessionAnalysisTyreCompoundFamily.Wet -> buildProfile(
            vehicleClass = SessionAnalysisVehicleClass.Unknown,
            compoundFamily = compoundFamily,
            compoundLabel = tyreCompoundLabel,
            surfaceMin = 45f,
            surfaceMax = 70f,
            coreMin = 50f,
            coreMax = 72f,
            brakeMin = 220f,
            brakeMax = 520f,
            pressureMin = 21.5f,
            pressureMax = 28.5f,
            estimated = true,
        )

        SessionAnalysisTyreCompoundFamily.Intermediate -> buildProfile(
            vehicleClass = SessionAnalysisVehicleClass.Unknown,
            compoundFamily = compoundFamily,
            compoundLabel = tyreCompoundLabel,
            surfaceMin = 58f,
            surfaceMax = 82f,
            coreMin = 62f,
            coreMax = 84f,
            brakeMin = 250f,
            brakeMax = 560f,
            pressureMin = 22f,
            pressureMax = 29f,
            estimated = true,
        )

        else -> buildProfile(
            vehicleClass = SessionAnalysisVehicleClass.Unknown,
            compoundFamily = compoundFamily,
            compoundLabel = tyreCompoundLabel,
            surfaceMin = 72f,
            surfaceMax = 94f,
            coreMin = 76f,
            coreMax = 98f,
            brakeMin = 280f,
            brakeMax = 620f,
            pressureMin = 23f,
            pressureMax = 29f,
            estimated = true,
        )
    }

    fun formulaProfile(
        compoundFamily: SessionAnalysisTyreCompoundFamily,
        tyreCompoundLabel: String?,
    ): SessionAnalysisTyreProfile = when (compoundFamily) {
        SessionAnalysisTyreCompoundFamily.Wet -> buildProfile(
            vehicleClass = SessionAnalysisVehicleClass.F1,
            compoundFamily = compoundFamily,
            compoundLabel = tyreCompoundLabel,
            surfaceMin = 45f,
            surfaceMax = 68f,
            coreMin = 48f,
            coreMax = 72f,
            brakeMin = 320f,
            brakeMax = 760f,
            pressureMin = 19f,
            pressureMax = 24f,
        )

        SessionAnalysisTyreCompoundFamily.Intermediate -> buildProfile(
            vehicleClass = SessionAnalysisVehicleClass.F1,
            compoundFamily = compoundFamily,
            compoundLabel = tyreCompoundLabel,
            surfaceMin = 60f,
            surfaceMax = 88f,
            coreMin = 65f,
            coreMax = 92f,
            brakeMin = 350f,
            brakeMax = 820f,
            pressureMin = 20f,
            pressureMax = 25f,
        )

        SessionAnalysisTyreCompoundFamily.Hard -> buildProfile(
            vehicleClass = SessionAnalysisVehicleClass.F1,
            compoundFamily = compoundFamily,
            compoundLabel = tyreCompoundLabel,
            surfaceMin = 88f,
            surfaceMax = 108f,
            coreMin = 92f,
            coreMax = 112f,
            brakeMin = 360f,
            brakeMax = 920f,
            pressureMin = 20f,
            pressureMax = 26f,
        )

        else -> buildProfile(
            vehicleClass = SessionAnalysisVehicleClass.F1,
            compoundFamily = if (compoundFamily == SessionAnalysisTyreCompoundFamily.Unknown) {
                SessionAnalysisTyreCompoundFamily.Soft
            } else {
                compoundFamily
            },
            compoundLabel = tyreCompoundLabel,
            surfaceMin = 92f,
            surfaceMax = 112f,
            coreMin = 96f,
            coreMax = 116f,
            brakeMin = 360f,
            brakeMax = 920f,
            pressureMin = 20f,
            pressureMax = 26f,
        )
    }

    fun gt3Profile(
        compoundFamily: SessionAnalysisTyreCompoundFamily,
        tyreCompoundLabel: String?,
    ): SessionAnalysisTyreProfile = when (compoundFamily) {
        SessionAnalysisTyreCompoundFamily.Wet -> buildProfile(
            vehicleClass = SessionAnalysisVehicleClass.GT3,
            compoundFamily = compoundFamily,
            compoundLabel = tyreCompoundLabel,
            surfaceMin = 45f,
            surfaceMax = 68f,
            coreMin = 50f,
            coreMax = 72f,
            brakeMin = 260f,
            brakeMax = 580f,
            pressureMin = 25f,
            pressureMax = 29f,
        )

        else -> buildProfile(
            vehicleClass = SessionAnalysisVehicleClass.GT3,
            compoundFamily = if (compoundFamily == SessionAnalysisTyreCompoundFamily.Unknown) {
                SessionAnalysisTyreCompoundFamily.Slick
            } else {
                compoundFamily
            },
            compoundLabel = tyreCompoundLabel,
            surfaceMin = 74f,
            surfaceMax = 95f,
            coreMin = 78f,
            coreMax = 98f,
            brakeMin = 300f,
            brakeMax = 680f,
            pressureMin = 26f,
            pressureMax = 28.4f,
        )
    }

    fun gt4Profile(
        compoundFamily: SessionAnalysisTyreCompoundFamily,
        tyreCompoundLabel: String?,
    ): SessionAnalysisTyreProfile = when (compoundFamily) {
        SessionAnalysisTyreCompoundFamily.Wet -> buildProfile(
            vehicleClass = SessionAnalysisVehicleClass.GT4,
            compoundFamily = compoundFamily,
            compoundLabel = tyreCompoundLabel,
            surfaceMin = 45f,
            surfaceMax = 66f,
            coreMin = 50f,
            coreMax = 70f,
            brakeMin = 220f,
            brakeMax = 520f,
            pressureMin = 24.5f,
            pressureMax = 28.5f,
        )

        else -> buildProfile(
            vehicleClass = SessionAnalysisVehicleClass.GT4,
            compoundFamily = if (compoundFamily == SessionAnalysisTyreCompoundFamily.Unknown) {
                SessionAnalysisTyreCompoundFamily.Slick
            } else {
                compoundFamily
            },
            compoundLabel = tyreCompoundLabel,
            surfaceMin = 70f,
            surfaceMax = 90f,
            coreMin = 74f,
            coreMax = 94f,
            brakeMin = 260f,
            brakeMax = 620f,
            pressureMin = 25f,
            pressureMax = 29f,
        )
    }

    fun gt2Profile(
        compoundFamily: SessionAnalysisTyreCompoundFamily,
        tyreCompoundLabel: String?,
    ): SessionAnalysisTyreProfile = buildProfile(
        vehicleClass = SessionAnalysisVehicleClass.GT2,
        compoundFamily = if (compoundFamily == SessionAnalysisTyreCompoundFamily.Unknown) {
            SessionAnalysisTyreCompoundFamily.Slick
        } else {
            compoundFamily
        },
        compoundLabel = tyreCompoundLabel,
        surfaceMin = 72f,
        surfaceMax = 92f,
        coreMin = 76f,
        coreMax = 96f,
        brakeMin = 280f,
        brakeMax = 640f,
        pressureMin = 25.5f,
        pressureMax = 29f,
    )

    fun gteProfile(
        compoundFamily: SessionAnalysisTyreCompoundFamily,
        tyreCompoundLabel: String?,
    ): SessionAnalysisTyreProfile = buildProfile(
        vehicleClass = SessionAnalysisVehicleClass.GTE,
        compoundFamily = if (compoundFamily == SessionAnalysisTyreCompoundFamily.Unknown) {
            SessionAnalysisTyreCompoundFamily.Slick
        } else {
            compoundFamily
        },
        compoundLabel = tyreCompoundLabel,
        surfaceMin = 75f,
        surfaceMax = 96f,
        coreMin = 78f,
        coreMax = 98f,
        brakeMin = 300f,
        brakeMax = 700f,
        pressureMin = 25.5f,
        pressureMax = 28.5f,
    )

    fun tcrProfile(
        compoundFamily: SessionAnalysisTyreCompoundFamily,
        tyreCompoundLabel: String?,
    ): SessionAnalysisTyreProfile = buildProfile(
        vehicleClass = SessionAnalysisVehicleClass.TCR,
        compoundFamily = if (compoundFamily == SessionAnalysisTyreCompoundFamily.Unknown) {
            SessionAnalysisTyreCompoundFamily.Slick
        } else {
            compoundFamily
        },
        compoundLabel = tyreCompoundLabel,
        surfaceMin = 70f,
        surfaceMax = 92f,
        coreMin = 74f,
        coreMax = 95f,
        brakeMin = 240f,
        brakeMax = 580f,
        pressureMin = 25.5f,
        pressureMax = 29.5f,
    )

    fun prototypeProfile(
        compoundFamily: SessionAnalysisTyreCompoundFamily,
        tyreCompoundLabel: String?,
        vehicleClass: SessionAnalysisVehicleClass,
    ): SessionAnalysisTyreProfile = buildProfile(
        vehicleClass = vehicleClass,
        compoundFamily = if (compoundFamily == SessionAnalysisTyreCompoundFamily.Unknown) {
            SessionAnalysisTyreCompoundFamily.Slick
        } else {
            compoundFamily
        },
        compoundLabel = tyreCompoundLabel,
        surfaceMin = 74f,
        surfaceMax = 96f,
        coreMin = 78f,
        coreMax = 98f,
        brakeMin = 350f,
        brakeMax = 900f,
        pressureMin = 24f,
        pressureMax = 28f,
    )

    fun buildProfile(
        vehicleClass: SessionAnalysisVehicleClass,
        compoundFamily: SessionAnalysisTyreCompoundFamily,
        compoundLabel: String?,
        surfaceMin: Float,
        surfaceMax: Float,
        coreMin: Float,
        coreMax: Float,
        brakeMin: Float,
        brakeMax: Float,
        pressureMin: Float,
        pressureMax: Float,
        estimated: Boolean = false,
    ): SessionAnalysisTyreProfile = SessionAnalysisTyreProfile(
        vehicleClass = vehicleClass,
        compoundFamily = compoundFamily,
        compoundLabel = compoundLabel.orEmpty(),
        surfaceOptimalMinC = surfaceMin,
        surfaceOptimalMaxC = surfaceMax,
        coreOptimalMinC = coreMin,
        coreOptimalMaxC = coreMax,
        brakeOptimalMinC = brakeMin,
        brakeOptimalMaxC = brakeMax,
        pressureOptimalMinPsi = pressureMin,
        pressureOptimalMaxPsi = pressureMax,
        innerOuterSpreadWarnC = 8f,
        innerOuterSpreadCriticalC = 14f,
        estimated = estimated,
    )
}
