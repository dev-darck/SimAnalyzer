package com.project.analyzer.ac.telemetry.impl.trackmap.evo

import dev.zacsweers.metro.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.hypot

@Inject
internal class AcEvoTrackMapAssetParsers(private val json: Json) {

    fun parseSplineJson(path: Path): List<AcEvoTrackSample> = runCatching {
        val payload = Files.readString(path)
        val decoded = json.decodeFromString(SplinedataRoot.serializer(), payload)
        sanitizeSamples(
            decoded.spline?.data?.nodes.orEmpty().mapNotNull { node ->
                node.transform?.position?.let { position ->
                    AcEvoTrackSample(
                        x = position.x,
                        y = position.z,
                    )
                }
            },
        )
    }.getOrDefault(emptyList())

    fun parseAiSpline(path: Path): List<AcEvoTrackSample> {
        val root = parseProtoNode(Files.readAllBytes(path)) ?: return emptyList()
        val points = selectBestVec3Sequence(root).map { vec ->
            AcEvoTrackSample(x = vec.x, y = vec.z)
        }
        return sanitizeSamples(points)
    }

    fun parseTrackControlPoints(path: Path): List<AcEvoTrackSample> {
        val bytes = Files.readAllBytes(path)
        val reader = ProtoReader(bytes)
        val samples = mutableListOf<AcEvoTrackSample>()
        while (true) {
            val header = reader.nextField() ?: break
            if (header.fieldNumber != 1 || header.wireType != WIRE_LENGTH_DELIMITED) {
                reader.skipField(header)
                continue
            }
            parseTrackControlPoint(reader.readBytesValue())?.let(samples::add)
        }
        return sanitizeSamples(samples)
    }

    private fun parseTrackControlPoint(bytes: ByteArray): AcEvoTrackSample? = try {
        val reader = ProtoReader(bytes)
        var position: ProtoVec3? = null
        var forward: ProtoVec3? = null
        var fallbackLeftWidthMeters: Float? = null
        var fallbackRightWidthMeters: Float? = null
        var trackLeftWidthMeters: Float? = null
        var trackRightWidthMeters: Float? = null
        var markerFlagsRaw: ByteArray? = null
        var hasMarker17: Boolean = false
        var hasMarker18: Boolean = false
        var markerHalfWidthMeters: Float? = null
        while (true) {
            val header = reader.nextField() ?: break
            when {
                header.fieldNumber == 1 && header.wireType == WIRE_LENGTH_DELIMITED -> {
                    position = parseProtoNode(reader.readBytesValue())?.primaryVec3 ?: position
                }

                header.fieldNumber == 2 && header.wireType == WIRE_LENGTH_DELIMITED -> {
                    forward = parseProtoNode(reader.readBytesValue())?.primaryVec3 ?: forward
                }

                header.fieldNumber == 3 && header.wireType == WIRE_FIXED32 -> {
                    fallbackLeftWidthMeters = Float.fromBits(reader.readFixed32Value())
                }

                header.fieldNumber == 4 && header.wireType == WIRE_FIXED32 -> {
                    fallbackRightWidthMeters = Float.fromBits(reader.readFixed32Value())
                }

                header.fieldNumber == 5 && header.wireType == WIRE_FIXED32 -> {
                    trackLeftWidthMeters = Float.fromBits(reader.readFixed32Value())
                }

                header.fieldNumber == 6 && header.wireType == WIRE_FIXED32 -> {
                    trackRightWidthMeters = Float.fromBits(reader.readFixed32Value())
                }

                header.fieldNumber == 14 && header.wireType == WIRE_LENGTH_DELIMITED -> {
                    markerFlagsRaw = reader.readBytesValue().takeIf { it.isNotEmpty() }
                }

                header.fieldNumber == 17 && header.wireType == WIRE_LENGTH_DELIMITED -> {
                    hasMarker17 = true
                    reader.readBytesValue()
                }

                header.fieldNumber == 18 && header.wireType == WIRE_LENGTH_DELIMITED -> {
                    hasMarker18 = true
                    reader.readBytesValue()
                }

                header.fieldNumber == 21 && header.wireType == WIRE_FIXED32 -> {
                    markerHalfWidthMeters = Float.fromBits(reader.readFixed32Value())
                }

                else -> reader.skipField(header)
            }
        }
        val pos = position ?: return null
        val resolvedLeftWidthMeters = sanitizeWidth(trackLeftWidthMeters)
            .takeIf { it > 0f }
            ?: sanitizeWidth(fallbackLeftWidthMeters)
        val resolvedRightWidthMeters = sanitizeWidth(trackRightWidthMeters)
            .takeIf { it > 0f }
            ?: sanitizeWidth(fallbackRightWidthMeters)
        AcEvoTrackSample(
            x = pos.x,
            y = pos.z,
            leftWidthMeters = resolvedLeftWidthMeters,
            rightWidthMeters = resolvedRightWidthMeters,
            forwardX = forward?.x,
            forwardY = forward?.z,
            markerFlagsRaw = markerFlagsRaw,
            hasMarker17 = hasMarker17,
            hasMarker18 = hasMarker18,
            markerHalfWidthMeters = sanitizeWidth(markerHalfWidthMeters),
        )
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun sanitizeSamples(samples: List<AcEvoTrackSample>): List<AcEvoTrackSample> {
        val sanitized = ArrayList<AcEvoTrackSample>(samples.size)
        samples.forEach { sample ->
            if (!sample.x.isFinite() || !sample.y.isFinite()) return@forEach
            if (sanitized.lastOrNull()?.let { last -> isSamePoint(last, sample) } == true) return@forEach
            sanitized += sample.copy(
                leftWidthMeters = sanitizeWidth(sample.leftWidthMeters),
                rightWidthMeters = sanitizeWidth(sample.rightWidthMeters),
                markerHalfWidthMeters = sanitizeWidth(sample.markerHalfWidthMeters),
            )
        }
        return sanitized
    }

    private fun isSamePoint(a: AcEvoTrackSample, b: AcEvoTrackSample): Boolean {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy <= DUPLICATE_POINT_EPSILON_SQ
    }

    private fun sanitizeWidth(value: Float?): Float = value
        ?.takeIf { it.isFinite() && it >= 0f && it <= MAX_TRACK_WIDTH_METERS }
        ?: 0f

    private fun parseProtoNode(bytes: ByteArray, depth: Int = 0): ProtoNode? {
        if (depth > MAX_PROTO_DEPTH) return null
        return try {
            val reader = ProtoReader(bytes)
            val floatFields = mutableMapOf<Int, Float>()
            val children = mutableListOf<ProtoChild>()
            while (true) {
                val header = reader.nextField() ?: break
                when (header.wireType) {
                    WIRE_VARINT -> reader.readVarintValue()

                    WIRE_FIXED64 -> reader.skipBytes(8)

                    WIRE_LENGTH_DELIMITED -> {
                        val payload = reader.readBytesValue()
                        parseProtoNode(payload, depth + 1)?.let { child ->
                            children += ProtoChild(
                                fieldNumber = header.fieldNumber,
                                node = child,
                            )
                        }
                    }

                    WIRE_FIXED32 -> {
                        val bits = reader.readFixed32Value()
                        if (header.fieldNumber in 1..3) {
                            floatFields[header.fieldNumber] = Float.fromBits(bits)
                        }
                    }

                    else -> throw IllegalArgumentException("Unsupported wire type: ${header.wireType}")
                }
            }
            ProtoNode(
                directVec3 = buildVec3(floatFields),
                children = children,
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun buildVec3(fields: Map<Int, Float>): ProtoVec3? {
        val x = fields[1] ?: return null
        val y = fields[2] ?: return null
        val z = fields[3] ?: return null
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
        if (listOf(x, y, z).any { kotlin.math.abs(it) > MAX_ABS_COORDINATE }) return null
        return ProtoVec3(x = x, y = y, z = z)
    }

    private fun selectBestVec3Sequence(node: ProtoNode): List<ProtoVec3> {
        var best = emptyList<ProtoVec3>()
        node.children.groupBy { child -> child.fieldNumber }.values.forEach { group ->
            val candidate = group.mapNotNull { child -> child.node.primaryVec3 }
            if (scoreOf(candidate) > scoreOf(best)) {
                best = candidate
            }
        }
        node.children.forEach { child ->
            val candidate = selectBestVec3Sequence(child.node)
            if (scoreOf(candidate) > scoreOf(best)) {
                best = candidate
            }
        }
        return best
    }

    private fun scoreOf(points: List<ProtoVec3>): Double {
        if (points.size < MIN_SEQUENCE_SIZE) return 0.0
        var minX = points.first().x
        var minZ = points.first().z
        var maxX = minX
        var maxZ = minZ
        points.forEach { point ->
            if (point.x < minX) minX = point.x
            if (point.z < minZ) minZ = point.z
            if (point.x > maxX) maxX = point.x
            if (point.z > maxZ) maxZ = point.z
        }
        val extent = hypot((maxX - minX).toDouble(), (maxZ - minZ).toDouble())
        return points.size * 1_000_000.0 + extent
    }

    internal companion object {

        const val WIRE_VARINT = 0
        const val WIRE_FIXED64 = 1
        const val WIRE_LENGTH_DELIMITED = 2
        const val WIRE_FIXED32 = 5

        const val MAX_PROTO_DEPTH = 10
        const val MIN_SEQUENCE_SIZE = 4
        const val MAX_ABS_COORDINATE = 100_000f
        const val MAX_TRACK_WIDTH_METERS = 80f
        const val DUPLICATE_POINT_EPSILON_SQ = 0.01f
    }
}

internal data class AcEvoTrackSample(
    val x: Float,
    val y: Float,
    val leftWidthMeters: Float = 0f,
    val rightWidthMeters: Float = 0f,
    val forwardX: Float? = null,
    val forwardY: Float? = null,
    val markerFlagsRaw: ByteArray? = null,
    val hasMarker17: Boolean = false,
    val hasMarker18: Boolean = false,
    val markerHalfWidthMeters: Float? = null,
)

private data class ProtoNode(val directVec3: ProtoVec3?, val children: List<ProtoChild>) {

    val primaryVec3: ProtoVec3?
        get() = directVec3 ?: children.firstNotNullOfOrNull { child -> child.node.primaryVec3 }
}

private data class ProtoChild(val fieldNumber: Int, val node: ProtoNode)

private data class ProtoVec3(val x: Float, val y: Float, val z: Float)

private data class ProtoFieldHeader(val fieldNumber: Int, val wireType: Int)

private class ProtoReader(private val bytes: ByteArray) {

    private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

    fun nextField(): ProtoFieldHeader? {
        if (!buffer.hasRemaining()) return null
        val tag = readVarint()
        val fieldNumber = (tag ushr 3).toInt()
        val wireType = (tag and 0x07).toInt()
        require(fieldNumber > 0) { "Invalid field number: $fieldNumber" }
        return ProtoFieldHeader(fieldNumber = fieldNumber, wireType = wireType)
    }

    fun readVarintValue(): Long = readVarint()

    fun readFixed32Value(): Int {
        require(buffer.remaining() >= 4) { "Not enough bytes for fixed32" }
        return buffer.int
    }

    fun readBytesValue(): ByteArray {
        val length = readVarint().toInt()
        require(length >= 0) { "Negative length-delimited field size: $length" }
        require(buffer.remaining() >= length) { "Not enough bytes for length-delimited field" }
        return ByteArray(length).also(buffer::get)
    }

    fun skipBytes(length: Int) {
        require(length >= 0) { "skipBytes length must be non-negative" }
        require(buffer.remaining() >= length) { "Not enough bytes to skip $length" }
        buffer.position(buffer.position() + length)
    }

    fun skipField(header: ProtoFieldHeader) {
        when (header.wireType) {
            AcEvoTrackMapAssetParsers.WIRE_VARINT -> readVarintValue()

            AcEvoTrackMapAssetParsers.WIRE_FIXED64 -> skipBytes(8)

            AcEvoTrackMapAssetParsers.WIRE_LENGTH_DELIMITED -> {
                val length = readVarint().toInt()
                skipBytes(length)
            }

            AcEvoTrackMapAssetParsers.WIRE_FIXED32 -> skipBytes(4)

            else -> throw IllegalArgumentException("Unsupported wire type: ${header.wireType}")
        }
    }

    private fun readVarint(): Long {
        var shift = 0
        var result = 0L
        while (true) {
            require(buffer.hasRemaining()) { "Unexpected EOF while reading varint" }
            val raw = buffer.get().toInt() and 0xFF
            result = result or (((raw and 0x7F).toLong()) shl shift)
            if ((raw and 0x80) == 0) return result
            shift += 7
            require(shift < 64) { "Varint is too long" }
        }
    }
}

@Serializable
private data class SplinedataRoot(val spline: SplineContainer? = null)

@Serializable
private data class SplineContainer(val data: SplineData? = null)

@Serializable
private data class SplineData(
    val nodes: List<SplineNode> = emptyList(),
    @SerialName("is_closed")
    val isClosed: Boolean = false,
)

@Serializable
private data class SplineNode(val transform: SplineTransform? = null)

@Serializable
private data class SplineTransform(val position: SplinePosition? = null)

@Serializable
private data class SplinePosition(val x: Float, val y: Float, val z: Float)
