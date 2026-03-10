package com.project.analyzer.ac.telemetry.impl.trackmap.evo

import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class AcEvoTrackMapAssetParsersTest {

    private val parsers = AcEvoTrackMapAssetParsers(
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        },
    )

    @Test
    fun `parseSplineJson reads XZ positions`() {
        val root = createTempDirectory("acevo-spline-json")
        try {
            val file = root.resolve("layout_gp.splinedata.json")
            Files.writeString(
                file,
                """
                {
                  "spline": {
                    "data": {
                      "nodes": [
                        {"transform": {"position": {"x": 1.5, "y": 0.0, "z": 10.0}}},
                        {"transform": {"position": {"x": 2.5, "y": 0.0, "z": 11.0}}},
                        {"transform": {"position": {"x": 3.5, "y": 0.0, "z": 12.0}}}
                      ],
                      "is_closed": false
                    }
                  }
                }
                """.trimIndent(),
            )

            val points = parsers.parseSplineJson(file)

            assertEquals(3, points.size)
            assertEquals(1.5f, points[0].x)
            assertEquals(10.0f, points[0].y)
            assertEquals(3.5f, points[2].x)
            assertEquals(12.0f, points[2].y)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `parseAiSpline selects longest position sequence`() {
        val root = createTempDirectory("acevo-ai-spline")
        try {
            val file = root.resolve("layout_gp.ideal_line.aisplinedata")
            val point1 = lenField(1, vec3(10f, 0f, 100f)) + lenField(2, vec3(0f, 1f, 0f))
            val point2 = lenField(1, vec3(20f, 0f, 200f)) + lenField(2, vec3(0f, 1f, 0f))
            val point3 = lenField(1, vec3(30f, 0f, 300f)) + lenField(2, vec3(0f, 1f, 0f))
            val point4 = lenField(1, vec3(40f, 0f, 400f)) + lenField(2, vec3(0f, 1f, 0f))
            val payload = lenField(1, point1 + point2 + point3 + point4)
            Files.write(file, payload)

            val points = parsers.parseAiSpline(file)

            assertEquals(4, points.size)
            assertEquals(10f, points[0].x)
            assertEquals(100f, points[0].y)
            assertEquals(40f, points[3].x)
            assertEquals(400f, points[3].y)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `parseTrackControlPoints prefers track widths from protobuf payload`() {
        val root = createTempDirectory("acevo-control-points")
        try {
            val file = root.resolve("layout_gp.trackcontrolpoints")
            val point1 = lenField(1, vec3(1f, 0f, 10f)) +
                fixed32Field(3, 11.5f) +
                fixed32Field(4, 12.5f) +
                fixed32Field(5, 31.5f) +
                fixed32Field(6, 32.5f)
            val point2 = lenField(1, vec3(2f, 0f, 20f)) +
                fixed32Field(3, 21.5f) +
                fixed32Field(4, 22.5f)
            Files.write(file, lenField(1, point1) + lenField(1, point2))

            val points = parsers.parseTrackControlPoints(file)

            assertEquals(2, points.size)
            assertEquals(1f, points[0].x)
            assertEquals(10f, points[0].y)
            assertEquals(31.5f, points[0].leftWidthMeters)
            assertEquals(32.5f, points[0].rightWidthMeters)
            assertEquals(21.5f, points[1].leftWidthMeters)
            assertEquals(22.5f, points[1].rightWidthMeters)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun vec3(x: Float, y: Float, z: Float): ByteArray =
        fixed32Field(1, x) + fixed32Field(2, y) + fixed32Field(3, z)

    private fun lenField(fieldNumber: Int, payload: ByteArray): ByteArray =
        tag(fieldNumber, 2) + varint(payload.size.toLong()) + payload

    private fun fixed32Field(fieldNumber: Int, value: Float): ByteArray =
        tag(fieldNumber, 5) + ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value.toRawBits()).array()

    private fun tag(fieldNumber: Int, wireType: Int): ByteArray = varint(((fieldNumber shl 3) or wireType).toLong())

    private fun varint(value: Long): ByteArray {
        var current = value
        val out = ArrayList<Byte>()
        do {
            var next = (current and 0x7F).toInt()
            current = current ushr 7
            if (current != 0L) {
                next = next or 0x80
            }
            out += next.toByte()
        } while (current != 0L)
        return out.toByteArray()
    }

    private operator fun ByteArray.plus(other: ByteArray): ByteArray {
        val out = ByteArray(size + other.size)
        copyInto(out, 0)
        other.copyInto(out, size)
        return out
    }
}
