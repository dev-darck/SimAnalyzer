package com.analyzer.session.analysis.presentation.builder.coach

import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_apex_speed
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_balance_oversteer
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_balance_oversteer_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_balance_stable
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_balance_stable_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_balance_understeer
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_balance_understeer_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_braking_starts_earlier
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_braking_starts_later
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_corner_prefix
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_focus_balance_control_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_focus_balance_control_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_focus_brake_timing_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_focus_exit_drive_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_focus_line_placement_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_focus_line_placement_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_focus_loss_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_focus_loss_speed_down
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_focus_loss_speed_up
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_focus_reference_close_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_focus_reference_close_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_insight_balance_pattern
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_insight_biggest_loss_zone
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_insight_brake_marker_trend
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_insight_corner_exit_trend
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_insight_line_discipline
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_insight_to_reference
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_line_gap_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_metric_balance
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_metric_brake_timing
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_metric_coasting
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_metric_consistency
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_metric_line_deviation
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_metric_setup_confidence
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_metric_throttle_pickup
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_metric_trail_braking
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_metric_tyre_management
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_phase_entry_timing
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_phase_exit_drive
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_phase_mid_corner_speed
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_reference_lap_unavailable
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_speed_faster
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_speed_slower
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_this_section
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_throttle_commit_earlier
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_throttle_commit_later
import org.jetbrains.compose.resources.getString
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class SessionAnalysisCoachStrings(
    val brakeTiming: String,
    val throttlePickup: String,
    val lineDeviation: String,
    val balance: String,
    val trailBraking: String,
    val coasting: String,
    val tyreManagement: String,
    val consistency: String,
    val setupConfidence: String,
    val referenceLapUnavailable: String,
    val entryTiming: String,
    val exitDrive: String,
    val midCornerSpeed: String,
    val exitDriveDescription: String,
    val brakeTimingDescription: String,
    val linePlacement: String,
    val linePlacementDescription: String,
    val balanceControl: String,
    val balanceControlDescription: String,
    val referenceIsClose: String,
    val referenceIsCloseDescription: String,
    val biggestLossZone: String,
    val toReference: String,
    val slower: String,
    val faster: String,
    val brakeMarkerTrend: String,
    val cornerExitTrend: String,
    val apexSpeed: String,
    val lineDiscipline: String,
    val balancePattern: String,
    val balanceStable: String,
    val balanceStableDescription: String,
    val thisSection: String,
    private val focusLossDescriptionTemplate: String,
    private val focusLossSpeedDownTemplate: String,
    private val focusLossSpeedUpTemplate: String,
    private val brakeStartsEarlierTemplate: String,
    private val brakeStartsLaterTemplate: String,
    private val throttleCommitEarlierTemplate: String,
    private val throttleCommitLaterTemplate: String,
    private val lineGapDescriptionTemplate: String,
    private val understeerBalanceTemplate: String,
    private val oversteerBalanceTemplate: String,
    private val understeerBalanceDescriptionTemplate: String,
    private val oversteerBalanceDescriptionTemplate: String,
    private val cornerPrefixTemplate: String,
) {

    fun focusLossDescription(trackPosition: String, delta: String, speedDelta: Float?): String {
        val speedSuffix = speedDelta?.let { value ->
            if (value < 0f) {
                focusLossSpeedDownTemplate.format(Locale.US, abs(value).roundToInt())
            } else {
                focusLossSpeedUpTemplate.format(Locale.US, abs(value).roundToInt())
            }
        }.orEmpty()
        return focusLossDescriptionTemplate.format(Locale.US, trackPosition, delta, speedSuffix)
    }

    fun brakeTimingInsight(delta: String, earlier: Boolean): String = if (earlier) {
        brakeStartsEarlierTemplate.format(Locale.US, delta)
    } else {
        brakeStartsLaterTemplate.format(Locale.US, delta)
    }

    fun throttleCommitEarlier(delta: String): String = throttleCommitEarlierTemplate.format(Locale.US, delta)

    fun throttleCommitLater(delta: String): String = throttleCommitLaterTemplate.format(Locale.US, delta)

    fun lineGapDescription(value: String): String = lineGapDescriptionTemplate.format(Locale.US, value)

    fun understeerBalance(value: Int): String = understeerBalanceTemplate.format(Locale.US, value)

    fun oversteerBalance(value: Int): String = oversteerBalanceTemplate.format(Locale.US, value)

    fun understeerBalanceDescription(value: Int): String = understeerBalanceDescriptionTemplate.format(Locale.US, value)

    fun oversteerBalanceDescription(value: Int): String = oversteerBalanceDescriptionTemplate.format(Locale.US, value)

    fun cornerPrefix(cornerNumber: Int, body: String): String =
        cornerPrefixTemplate.format(Locale.US, cornerNumber, body)

    companion object {

        suspend fun resolve(): SessionAnalysisCoachStrings = SessionAnalysisCoachStrings(
            brakeTiming = getString(Res.string.session_analysis_coach_metric_brake_timing),
            throttlePickup = getString(Res.string.session_analysis_coach_metric_throttle_pickup),
            lineDeviation = getString(Res.string.session_analysis_coach_metric_line_deviation),
            balance = getString(Res.string.session_analysis_coach_metric_balance),
            trailBraking = getString(Res.string.session_analysis_coach_metric_trail_braking),
            coasting = getString(Res.string.session_analysis_coach_metric_coasting),
            tyreManagement = getString(Res.string.session_analysis_coach_metric_tyre_management),
            consistency = getString(Res.string.session_analysis_coach_metric_consistency),
            setupConfidence = getString(Res.string.session_analysis_coach_metric_setup_confidence),
            referenceLapUnavailable = getString(Res.string.session_analysis_coach_reference_lap_unavailable),
            entryTiming = getString(Res.string.session_analysis_coach_phase_entry_timing),
            exitDrive = getString(Res.string.session_analysis_coach_phase_exit_drive),
            midCornerSpeed = getString(Res.string.session_analysis_coach_phase_mid_corner_speed),
            exitDriveDescription = getString(Res.string.session_analysis_coach_focus_exit_drive_description),
            brakeTimingDescription = getString(Res.string.session_analysis_coach_focus_brake_timing_description),
            linePlacement = getString(Res.string.session_analysis_coach_focus_line_placement_title),
            linePlacementDescription = getString(Res.string.session_analysis_coach_focus_line_placement_description),
            balanceControl = getString(Res.string.session_analysis_coach_focus_balance_control_title),
            balanceControlDescription = getString(Res.string.session_analysis_coach_focus_balance_control_description),
            referenceIsClose = getString(Res.string.session_analysis_coach_focus_reference_close_title),
            referenceIsCloseDescription = getString(
                Res.string.session_analysis_coach_focus_reference_close_description,
            ),
            biggestLossZone = getString(Res.string.session_analysis_coach_insight_biggest_loss_zone),
            toReference = getString(Res.string.session_analysis_coach_insight_to_reference),
            slower = getString(Res.string.session_analysis_coach_speed_slower),
            faster = getString(Res.string.session_analysis_coach_speed_faster),
            brakeMarkerTrend = getString(Res.string.session_analysis_coach_insight_brake_marker_trend),
            cornerExitTrend = getString(Res.string.session_analysis_coach_insight_corner_exit_trend),
            apexSpeed = getString(Res.string.session_analysis_coach_apex_speed),
            lineDiscipline = getString(Res.string.session_analysis_coach_insight_line_discipline),
            balancePattern = getString(Res.string.session_analysis_coach_insight_balance_pattern),
            balanceStable = getString(Res.string.session_analysis_coach_balance_stable),
            balanceStableDescription = getString(Res.string.session_analysis_coach_balance_stable_description),
            thisSection = getString(Res.string.session_analysis_coach_this_section),
            focusLossDescriptionTemplate = getString(Res.string.session_analysis_coach_focus_loss_description),
            focusLossSpeedDownTemplate = getString(Res.string.session_analysis_coach_focus_loss_speed_down),
            focusLossSpeedUpTemplate = getString(Res.string.session_analysis_coach_focus_loss_speed_up),
            brakeStartsEarlierTemplate = getString(Res.string.session_analysis_coach_braking_starts_earlier),
            brakeStartsLaterTemplate = getString(Res.string.session_analysis_coach_braking_starts_later),
            throttleCommitEarlierTemplate = getString(Res.string.session_analysis_coach_throttle_commit_earlier),
            throttleCommitLaterTemplate = getString(Res.string.session_analysis_coach_throttle_commit_later),
            lineGapDescriptionTemplate = getString(Res.string.session_analysis_coach_line_gap_description),
            understeerBalanceTemplate = getString(Res.string.session_analysis_coach_balance_understeer),
            oversteerBalanceTemplate = getString(Res.string.session_analysis_coach_balance_oversteer),
            understeerBalanceDescriptionTemplate =
            getString(Res.string.session_analysis_coach_balance_understeer_description),
            oversteerBalanceDescriptionTemplate =
            getString(Res.string.session_analysis_coach_balance_oversteer_description),
            cornerPrefixTemplate = getString(Res.string.session_analysis_coach_corner_prefix),
        )
    }
}
