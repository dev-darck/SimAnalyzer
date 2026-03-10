package com.project.analyzer.calibration.presentation.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.project.analyzer.calibration.presentation.overlay.state.CapturePoint
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
fun MiniMap(
    carPos: Vec2,
    carDir: Vec2,
    gates: List<Pair<String, Gate>>,
    lastCapturePoint: CapturePoint? = null,
    pendingCapturePosition: Vec2? = null,
    modifier: Modifier = Modifier,
) {
    val material = SimAnalyzerTheme.material
    val chrome = SimAnalyzerTheme.chrome
    val extended = SimAnalyzerTheme.extended
    val pxPerMeter = 4.5f
    val radiusMeters = 40f

    Box(
        modifier
            .size(420.dp)
            .background(chrome.fillOverlay)
            .padding(10.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.5f

            fun worldVecToScreenVec(v: Vec2): Offset = Offset(v.x, v.y)

            fun worldToScreen(p: Vec2): Offset {
                val d = p - carPos
                val dv = worldVecToScreenVec(d)
                return Offset(
                    x = cx + dv.x * pxPerMeter,
                    y = cy + dv.y * pxPerMeter,
                )
            }

            fun drawArrow(from: Offset, dir: Vec2, lenMeters: Float, color: Color) {
                val dWorld = dir.safeNormalized(Vec2(0f, 1f))
                val dScreen = worldVecToScreenVec(dWorld)

                val lenPx = lenMeters * pxPerMeter
                val to = Offset(
                    from.x + dScreen.x * lenPx,
                    from.y + dScreen.y * lenPx,
                )

                drawLine(color, from, to, strokeWidth = 3f)

                val leftScreen = Offset(-dScreen.y, dScreen.x)

                val headLen = 2f * pxPerMeter
                val headWidth = 1.5f * pxPerMeter

                val head1 = Offset(
                    to.x - dScreen.x * headLen + leftScreen.x * headWidth,
                    to.y - dScreen.y * headLen + leftScreen.y * headWidth,
                )
                val head2 = Offset(
                    to.x - dScreen.x * headLen - leftScreen.x * headWidth,
                    to.y - dScreen.y * headLen - leftScreen.y * headWidth,
                )

                drawLine(color, to, head1, strokeWidth = 3f)
                drawLine(color, to, head2, strokeWidth = 3f)
            }

            drawCircle(
                color = material.onSurface.copy(alpha = 0.14f),
                radius = radiusMeters * pxPerMeter,
                center = Offset(cx, cy),
                style = Stroke(width = 1f),
            )

            val carS = Offset(cx, cy)
            drawCircle(extended.cyan, radius = 6f, center = carS)
            drawArrow(carS, carDir, lenMeters = 12f, color = extended.cyan)

            pendingCapturePosition?.let { pending ->
                val pendingS = worldToScreen(pending)
                drawCircle(extended.orange, radius = 10f, center = pendingS, style = Stroke(width = 2f))
                drawCircle(extended.orange.copy(alpha = 0.55f), radius = 6f, center = pendingS)
            }

            lastCapturePoint?.let { capture ->
                val captureS = worldToScreen(capture.position)
                drawCircle(extended.pink, radius = 8f, center = captureS)
                drawArrow(captureS, capture.forward, lenMeters = 10f, color = extended.pink)
            }

            val filtered = gates
                .map { (key, g) ->
                    val d = (g.centerV2() - carPos).len()
                    Triple(key, g, d)
                }
                .filter { (_, g, d) ->
                    d <= (radiusMeters + g.halfWidthMeters + 8f)
                }
                .sortedBy { it.third }
                .take(4)

            filtered.forEach { (_, g) ->
                val c = g.centerV2()
                val f = g.forwardV2().safeNormalized(Vec2(0f, 1f))

                var n = g.normalV2()
                n = (n - f * n.dot(f)).safeNormalized(f.perpLeft())

                val a = c + n * g.halfWidthMeters
                val b = c - n * g.halfWidthMeters

                val cs = worldToScreen(c)
                val ascr = worldToScreen(a)
                val bscr = worldToScreen(b)

                drawLine(extended.yellow, ascr, bscr, strokeWidth = 4f)
                drawCircle(extended.yellow, radius = 4f, center = cs)
                drawArrow(cs, f, lenMeters = 8f, color = extended.lightGreen)
            }
        }
    }
}
