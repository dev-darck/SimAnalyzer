package com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone

import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class TrackMapCornerZoneDetectorTest {

    private val detector = TrackMapCornerZoneDetector()

    @Test
    fun `detect returns no zones for straight track`() {
        val zones = detector.detect(buildStraightTrackMap())

        assertTrue("zones=$zones", zones.isEmpty())
    }

    @Test
    fun `detect finds two corners on stadium track`() {
        val zones = detector.detect(buildStadiumTrackMap())

        assertEquals("zones=$zones", 2, zones.size)
        assertTrue(zones.all { zone -> zone.peakCurvature > 0f })
        assertTrue(zones.zipWithNext().all { (left, right) -> left.apexTrackPosition < right.apexTrackPosition })
    }

    @Test
    fun `detect preserves corner near the start line when the track is rotated`() {
        val zones = detector.detect(buildStadiumTrackMap(rotateBy = 53))

        assertEquals("zones=$zones", 2, zones.size)
        assertTrue("zones=$zones", zones.first().apexTrackPosition < 0.15f)
    }

    @Test
    fun `detect ignores gentle bend on the straight`() {
        val zones = detector.detect(buildStadiumTrackMap(topStraightBendAmplitude = 1.2f))

        assertEquals("zones=$zones", 2, zones.size)
    }

    @Test
    fun `detect resolves strong chicane as separate turns`() {
        val zones = detector.detect(buildChicaneTrackMap())

        assertTrue("zones=$zones", zones.size in 4..5)
    }

    @Test
    fun `detect keeps long low curvature sweeper as corner`() {
        val zones = detector.detect(
            buildStadiumTrackMap(
                leftArcRadius = 420f,
                rightArcRadius = 420f,
                halfStraight = 60f,
            ),
        )

        assertEquals("zones=$zones", 2, zones.size)
        assertTrue(zones.any { zone -> zone.spanFraction() > 0.22f })
    }

    @Test
    fun `detect merges noisy hairpin into a single corner complex`() {
        val zones = detector.detect(
            buildStadiumTrackMap(
                rightArcRadiusWobbleAmplitude = 3.5f,
            ),
        )

        assertEquals("zones=$zones", 2, zones.size)
    }

    @Test
    fun `detect keeps low resolution brands hatch indy outline split into several corners`() {
        val zones = detector.detect(buildBrandsHatchIndyTrackMap())

        assertTrue("zones=$zones", zones.size >= 5)
    }

    @Test
    fun `detect keeps interpolated brands hatch indy split into several corners`() {
        val zones = detector.detect(buildInterpolatedBrandsHatchIndyTrackMap())

        assertEquals("zones=$zones", 7, zones.size)
    }
}

private fun buildStraightTrackMap(): SessionAnalysisTrackMap {
    val points = (0..40).map { index ->
        SessionAnalysisTrackMapPoint(
            x = index * 5f,
            y = 0f,
        )
    }
    return points.toTrackMap()
}

private fun buildStadiumTrackMap(
    rotateBy: Int = 0,
    halfStraight: Float = 45f,
    leftArcRadius: Float = 20f,
    rightArcRadius: Float = 20f,
    topStraightBendAmplitude: Float = 0f,
    rightArcRadiusWobbleAmplitude: Float = 0f,
): SessionAnalysisTrackMap {
    val straightSteps = 32
    val arcSteps = 42

    val points = mutableListOf<SessionAnalysisTrackMapPoint>()

    for (step in 0 until straightSteps) {
        val ratio = step / straightSteps.toFloat()
        points += SessionAnalysisTrackMapPoint(
            x = -halfStraight + (ratio * halfStraight * 2f),
            y = lerp(leftArcRadius, rightArcRadius, ratio) +
                (sin(ratio * PI) * topStraightBendAmplitude).toFloat(),
        )
    }
    for (step in 0 until arcSteps) {
        val angle = (PI / 2.0) - (step / arcSteps.toDouble() * PI)
        val wobble = sin(step / arcSteps.toDouble() * PI * 3.0) * rightArcRadiusWobbleAmplitude
        points += SessionAnalysisTrackMapPoint(
            x = halfStraight + (cos(angle) * (rightArcRadius + wobble)).toFloat(),
            y = (sin(angle) * (rightArcRadius + wobble)).toFloat(),
        )
    }
    for (step in 0 until straightSteps) {
        val ratio = step / straightSteps.toFloat()
        points += SessionAnalysisTrackMapPoint(
            x = halfStraight - (ratio * halfStraight * 2f),
            y = -lerp(rightArcRadius, leftArcRadius, ratio),
        )
    }
    for (step in 0 until arcSteps) {
        val angle = -(PI / 2.0) - (step / arcSteps.toDouble() * PI)
        points += SessionAnalysisTrackMapPoint(
            x = -halfStraight + (cos(angle) * leftArcRadius).toFloat(),
            y = (sin(angle) * leftArcRadius).toFloat(),
        )
    }

    val normalizedRotateBy = rotateBy
        .takeIf { points.isNotEmpty() }
        ?.mod(points.size)
        ?: 0
    val rotated = if (normalizedRotateBy == 0) {
        points
    } else {
        points.drop(normalizedRotateBy) + points.take(normalizedRotateBy)
    }
    return rotated.toTrackMap()
}

private fun List<SessionAnalysisTrackMapPoint>.toTrackMap(): SessionAnalysisTrackMap = SessionAnalysisTrackMap(
    points = this,
    minX = minOf(SessionAnalysisTrackMapPoint::x),
    minY = minOf(SessionAnalysisTrackMapPoint::y),
    maxX = maxOf(SessionAnalysisTrackMapPoint::x),
    maxY = maxOf(SessionAnalysisTrackMapPoint::y),
)

private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction

private fun buildChicaneTrackMap(): SessionAnalysisTrackMap {
    val pose = PathPose(
        x = -58f,
        y = 26f,
        headingRad = 0.0,
    )
    val points = mutableListOf<SessionAnalysisTrackMapPoint>()
    points += pose.toPoint()
    points.appendLine(pose, lengthMeters = 24f)
    points.appendArc(pose, radiusMeters = 18f, angleDeg = -36f)
    points.appendArc(pose, radiusMeters = 18f, angleDeg = 44f)
    points.appendLine(pose, lengthMeters = 70f)
    points.appendArc(pose, radiusMeters = 24f, angleDeg = -180f)
    points.appendLine(pose, lengthMeters = 102f)
    points.appendArc(pose, radiusMeters = 24f, angleDeg = -180f)
    points.appendLine(pose, lengthMeters = 10f)
    val rotateBy = points.size / 2
    val rotated = points.drop(rotateBy) + points.take(rotateBy)
    return rotated.toTrackMap()
}

private fun buildBrandsHatchIndyTrackMap(): SessionAnalysisTrackMap = listOf(
    SessionAnalysisTrackMapPoint(x = -144.996f, y = -386.959f),
    SessionAnalysisTrackMapPoint(x = -20.785f, y = -436.296f),
    SessionAnalysisTrackMapPoint(x = 62.316f, y = -448.397f),
    SessionAnalysisTrackMapPoint(x = 120.748f, y = -422.469f),
    SessionAnalysisTrackMapPoint(x = 159.788f, y = -358.761f),
    SessionAnalysisTrackMapPoint(x = 164.111f, y = -302.252f),
    SessionAnalysisTrackMapPoint(x = 157.326f, y = -199.514f),
    SessionAnalysisTrackMapPoint(x = 153.191f, y = -167.930f),
    SessionAnalysisTrackMapPoint(x = 133.399f, y = -127.424f),
    SessionAnalysisTrackMapPoint(x = 103.584f, y = -123.950f),
    SessionAnalysisTrackMapPoint(x = 78.127f, y = -189.799f),
    SessionAnalysisTrackMapPoint(x = 82.384f, y = -228.891f),
    SessionAnalysisTrackMapPoint(x = 76.733f, y = -289.506f),
    SessionAnalysisTrackMapPoint(x = 68.838f, y = -310.203f),
    SessionAnalysisTrackMapPoint(x = 41.720f, y = -340.220f),
    SessionAnalysisTrackMapPoint(x = -30.741f, y = -343.683f),
    SessionAnalysisTrackMapPoint(x = -127.635f, y = -311.834f),
    SessionAnalysisTrackMapPoint(x = -171.113f, y = -292.713f),
    SessionAnalysisTrackMapPoint(x = -226.434f, y = -263.663f),
    SessionAnalysisTrackMapPoint(x = -263.433f, y = -224.178f),
    SessionAnalysisTrackMapPoint(x = -288.279f, y = -130.061f),
    SessionAnalysisTrackMapPoint(x = -312.711f, y = -77.915f),
    SessionAnalysisTrackMapPoint(x = -385.117f, y = -68.745f),
    SessionAnalysisTrackMapPoint(x = -412.749f, y = -105.118f),
    SessionAnalysisTrackMapPoint(x = -414.537f, y = -176.761f),
    SessionAnalysisTrackMapPoint(x = -361.526f, y = -257.253f),
    SessionAnalysisTrackMapPoint(x = -319.161f, y = -294.342f),
    SessionAnalysisTrackMapPoint(x = -273.287f, y = -323.357f),
    SessionAnalysisTrackMapPoint(x = -176.629f, y = -372.019f),
).toTrackMap()

private fun buildInterpolatedBrandsHatchIndyTrackMap(): SessionAnalysisTrackMap {
    val controlPoints = listOf(
        BrandsHatchControlPoint(-144.996f, -386.959f, 0.911f, -0.411f),
        BrandsHatchControlPoint(-20.785f, -436.296f, 0.957f, -0.289f),
        BrandsHatchControlPoint(62.316f, -448.397f, 0.994f, 0.104f),
        BrandsHatchControlPoint(120.748f, -422.469f, 0.745f, 0.657f),
        BrandsHatchControlPoint(159.788f, -358.761f, 0.141f, 0.989f),
        BrandsHatchControlPoint(164.111f, -302.252f, -0.090f, 0.992f),
        BrandsHatchControlPoint(157.326f, -199.514f, -0.094f, 0.989f),
        BrandsHatchControlPoint(153.191f, -167.930f, -0.187f, 0.977f),
        BrandsHatchControlPoint(133.399f, -127.424f, -0.805f, 0.593f),
        BrandsHatchControlPoint(103.584f, -123.950f, -0.904f, -0.425f),
        BrandsHatchControlPoint(78.127f, -189.799f, 0.066f, -0.993f),
        BrandsHatchControlPoint(82.384f, -228.891f, 0.090f, -0.991f),
        BrandsHatchControlPoint(76.733f, -289.506f, -0.279f, -0.960f),
        BrandsHatchControlPoint(68.838f, -310.203f, -0.446f, -0.894f),
        BrandsHatchControlPoint(41.720f, -340.220f, -0.846f, -0.533f),
        BrandsHatchControlPoint(-30.741f, -343.683f, -0.964f, 0.265f),
        BrandsHatchControlPoint(-127.635f, -311.834f, -0.926f, 0.377f),
        BrandsHatchControlPoint(-171.113f, -292.713f, -0.908f, 0.418f),
        BrandsHatchControlPoint(-226.434f, -263.663f, -0.815f, 0.578f),
        BrandsHatchControlPoint(-263.433f, -224.178f, -0.511f, 0.859f),
        BrandsHatchControlPoint(-288.279f, -130.061f, -0.253f, 0.967f),
        BrandsHatchControlPoint(-312.711f, -77.915f, -0.650f, 0.758f),
        BrandsHatchControlPoint(-385.117f, -68.745f, -0.818f, -0.575f),
        BrandsHatchControlPoint(-412.749f, -105.118f, -0.376f, -0.924f),
        BrandsHatchControlPoint(-414.537f, -176.761f, 0.315f, -0.949f),
        BrandsHatchControlPoint(-361.526f, -257.253f, 0.689f, -0.725f),
        BrandsHatchControlPoint(-319.161f, -294.342f, 0.808f, -0.589f),
        BrandsHatchControlPoint(-273.287f, -323.357f, 0.875f, -0.484f),
        BrandsHatchControlPoint(-176.629f, -372.019f, 0.899f, -0.438f),
    )
    return interpolateBrandsHatchControlPoints(controlPoints).toTrackMap()
}

private data class PathPose(
    var x: Float,
    var y: Float,
    var headingRad: Double,
) {

    fun toPoint(): SessionAnalysisTrackMapPoint = SessionAnalysisTrackMapPoint(
        x = x,
        y = y,
    )
}

private data class BrandsHatchControlPoint(
    val x: Float,
    val y: Float,
    val forwardX: Float,
    val forwardY: Float,
)

private fun MutableList<SessionAnalysisTrackMapPoint>.appendLine(
    pose: PathPose,
    lengthMeters: Float,
    stepMeters: Float = 4f,
) {
    val steps = maxOf(2, kotlin.math.ceil(lengthMeters / stepMeters).toInt())
    val segmentLength = lengthMeters / steps.toFloat()
    repeat(steps) {
        pose.x += (cos(pose.headingRad) * segmentLength).toFloat()
        pose.y += (sin(pose.headingRad) * segmentLength).toFloat()
        add(pose.toPoint())
    }
}

private fun MutableList<SessionAnalysisTrackMapPoint>.appendArc(
    pose: PathPose,
    radiusMeters: Float,
    angleDeg: Float,
) {
    val totalAngleRad = Math.toRadians(angleDeg.toDouble())
    val steps = maxOf(8, kotlin.math.ceil(kotlin.math.abs(angleDeg) / 4f).toInt())
    val stepAngle = totalAngleRad / steps.toDouble()
    val stepLength = kotlin.math.abs(radiusMeters * stepAngle).toFloat()
    repeat(steps) {
        pose.headingRad += stepAngle
        pose.x += (cos(pose.headingRad) * stepLength).toFloat()
        pose.y += (sin(pose.headingRad) * stepLength).toFloat()
        add(pose.toPoint())
    }
}

private fun interpolateBrandsHatchControlPoints(
    points: List<BrandsHatchControlPoint>,
    stepMeters: Float = 4f,
): List<SessionAnalysisTrackMapPoint> {
    if (points.size < 2) return points.map { point -> SessionAnalysisTrackMapPoint(x = point.x, y = point.y) }

    val result = ArrayList<SessionAnalysisTrackMapPoint>(points.size * 4)
    for (index in 0 until points.lastIndex) {
        val start = points[index]
        val end = points[index + 1]
        val distance = hypot(end.x - start.x, end.y - start.y)
        val segments = maxOf(1, ceil(distance / stepMeters).toInt())
        repeat(segments) { segmentIndex ->
            val t = segmentIndex.toFloat() / segments.toFloat()
            val startTangent = start.forwardX * distance to start.forwardY * distance
            val endTangent = end.forwardX * distance to end.forwardY * distance
            result += hermiteSample(
                start = start,
                end = end,
                startTangent = startTangent,
                endTangent = endTangent,
                t = t,
            )
        }
    }
    result += SessionAnalysisTrackMapPoint(x = points.last().x, y = points.last().y)
    return result.fold(mutableListOf()) { acc, point ->
        val previous = acc.lastOrNull()
        if (previous == null || hypot(point.x - previous.x, point.y - previous.y) >= 0.15f) {
            acc += point
        }
        acc
    }
}

private fun hermiteSample(
    start: BrandsHatchControlPoint,
    end: BrandsHatchControlPoint,
    startTangent: Pair<Float, Float>,
    endTangent: Pair<Float, Float>,
    t: Float,
): SessionAnalysisTrackMapPoint {
    val tt = t * t
    val ttt = tt * t
    val h00 = 2f * ttt - 3f * tt + 1f
    val h10 = ttt - 2f * tt + t
    val h01 = -2f * ttt + 3f * tt
    val h11 = ttt - tt
    return SessionAnalysisTrackMapPoint(
        x = h00 * start.x + h10 * startTangent.first + h01 * end.x + h11 * endTangent.first,
        y = h00 * start.y + h10 * startTangent.second + h01 * end.y + h11 * endTangent.second,
    )
}

