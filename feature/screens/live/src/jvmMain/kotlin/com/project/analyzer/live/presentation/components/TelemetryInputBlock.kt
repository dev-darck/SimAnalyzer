package com.project.analyzer.live.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlin.math.roundToInt

@Composable
fun TelemetryInputsBlock(
    clutch: Float,
    brake: Float,
    throttle: Float,
    steerDeg: Float,
    modifier: Modifier = Modifier,
) {
    val cardBg = SimAnalyzerTheme.material.surface
    val barBg = SimAnalyzerTheme.material.surfaceVariant
    val textPrimary = SimAnalyzerTheme.material.onSurface
    val textMuted = SimAnalyzerTheme.material.onSurfaceVariant

    val brakeColor = SimAnalyzerTheme.extended.red
    val throttleColor = SimAnalyzerTheme.extended.teal
    val clutchColor = SimAnalyzerTheme.extended.cyan
    val knobColor = SimAnalyzerTheme.material.primary

    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(cardBg)
            .padding(22.dp),
    ) {
        Text(
            text = "Inputs",
            color = textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(16.dp))

        HorizontalInputBar(
            label = "THR",
            progress = throttle,
            fillColor = throttleColor,
            barBg = barBg,
            textPrimary = textPrimary,
            textMuted = textMuted,
        )

        Spacer(Modifier.height(8.dp))

        HorizontalInputBar(
            label = "BRK",
            progress = brake,
            fillColor = brakeColor,
            barBg = barBg,
            textPrimary = textPrimary,
            textMuted = textMuted,
        )

        Spacer(Modifier.height(8.dp))

        HorizontalInputBar(
            label = "CLT",
            progress = clutch,
            fillColor = clutchColor,
            barBg = barBg,
            textPrimary = textPrimary,
            textMuted = textMuted,
        )

        Spacer(Modifier.height(12.dp))

        SteeringSlider(
            steerDeg = steerDeg,
            modifier = Modifier.fillMaxWidth(),
            textMuted = textMuted,
            trackColor = barBg,
            knobColor = knobColor,
        )
    }
}

@Composable
private fun HorizontalInputBar(
    label: String,
    progress: Float,
    fillColor: Color,
    barBg: Color,
    textPrimary: Color,
    textMuted: Color,
    modifier: Modifier = Modifier,
) {
    val p = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(8.dp)
    val barHeight = 24.dp

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(barHeight)
                .clip(shape)
                .background(barBg),
        ) {
            if (p > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(p)
                        .height(barHeight)
                        .clip(shape)
                        .background(fillColor),
                )
            }
        }

        Text(
            text = "${(p * 100f).roundToInt()}%",
            color = textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .width(42.dp)
                .padding(start = 8.dp),
        )
    }
}

@Composable
private fun SteeringSlider(
    steerDeg: Float,
    modifier: Modifier = Modifier,
    textMuted: Color,
    trackColor: Color,
    knobColor: Color,
) {
    val s = steerDeg.coerceIn(-180f, 180f)
    val frac = (s + 180f) / 360f

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("L", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text("${s.roundToInt()}°", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text("R", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(6.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
        ) {
            val w = size.width
            val h = size.height

            val trackH = 6.dp.toPx()
            val y = (h - trackH) / 2f
            val r = trackH / 2f

            drawRoundRect(
                color = trackColor.copy(alpha = 0.70f),
                topLeft = Offset(0f, y),
                size = Size(w, trackH),
                cornerRadius = CornerRadius(r, r),
            )

            val centerX = w / 2f
            drawLine(
                color = textMuted.copy(alpha = 0.5f),
                start = Offset(centerX, y - 2.dp.toPx()),
                end = Offset(centerX, y + trackH + 2.dp.toPx()),
                strokeWidth = 1.5.dp.toPx(),
            )

            val cx = (w * frac).coerceIn(0f, w)
            val cy = h / 2f
            val knobR = 7.dp.toPx()

            drawCircle(
                color = knobColor.copy(alpha = 0.25f),
                radius = knobR + 3.dp.toPx(),
                center = Offset(cx, cy),
            )

            drawCircle(
                color = knobColor,
                radius = knobR,
                center = Offset(cx, cy),
            )

            drawCircle(
                color = Color.Black.copy(alpha = 0.20f),
                radius = knobR,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

@Preview
@Composable
private fun TelemetryInputsBlockPreview() {
    SimAnalyzerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SimAnalyzerTheme.material.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            TelemetryInputsBlock(
                clutch = 0.0f,
                brake = 0.15f,
                throttle = 1.0f,
                steerDeg = 45f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
