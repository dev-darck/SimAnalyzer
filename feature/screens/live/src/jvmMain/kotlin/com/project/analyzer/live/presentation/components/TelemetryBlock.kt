package com.project.analyzer.live.presentation.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private val TOP_BAR_ANIMATION_MS = 160.milliseconds.inWholeMilliseconds.toInt()

@Composable
internal fun TelemetryBlock(
    speedKmh: Int,
    rpmInt: Int,
    rpmScale: Float,
    maxScale: Int,
    gear: Int,
    modifier: Modifier = Modifier,
) {
    val speedStr = remember(speedKmh) { "%03d".format(speedKmh.coerceIn(0, 999)) }
    val rpmText = remember(rpmInt) { rpmInt.toString() }

    Box(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(color = SimAnalyzerTheme.material.surface)
    ) {
        TopIntegratedProgressBar(
            value = rpmScale,
            max = maxScale,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            ScaleLabels(
                maxScale = maxScale,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 130.dp)
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AxisValueWithUnit(
                        value = speedStr,
                        unit = "km/h",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    GearBadge(
                        gear = gear,
                        size = 130.dp,
                        baseColor = SimAnalyzerTheme.material.primaryContainer,
                        modifier = Modifier.size(130.dp)
                    )

                    AxisValueWithUnit(
                        value = rpmText,
                        unit = "rpm",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun TopIntegratedProgressBar(
    value: Float,
    max: Int,
    modifier: Modifier = Modifier
) {
    val emptyColor = SimAnalyzerTheme.material.surfaceVariant
    val brush = SimAnalyzerTheme.horizontalGradient

    val safeMax = max.coerceAtLeast(1).toFloat()
    val targetFrac = (value / safeMax).coerceIn(0f, 1f)

    val frac by animateFloatAsState(
        targetValue = targetFrac,
        animationSpec = tween(durationMillis = TOP_BAR_ANIMATION_MS, easing = LinearOutSlowInEasing),
        label = "TopBar"
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val r = h / 2f

        drawRoundRect(
            color = emptyColor,
            size = Size(w, h),
            cornerRadius = CornerRadius(r, r)
        )

        val progressW = w * frac
        if (progressW > 0.5f) {
            val rightRounded = progressW >= w - 0.5f
            val rr = RoundRect(
                left = 0f, top = 0f, right = progressW, bottom = h,
                topLeftCornerRadius = CornerRadius(r, r),
                bottomLeftCornerRadius = CornerRadius(r, r),
                topRightCornerRadius = CornerRadius(if (rightRounded) r else 0f, if (rightRounded) r else 0f),
                bottomRightCornerRadius = CornerRadius(if (rightRounded) r else 0f, if (rightRounded) r else 0f)
            )
            val clip = Path().apply { addRoundRect(rr) }
            clipPath(clip) {
                drawRect(brush = brush, size = Size(w, h))
            }
        }

        val markerW = 4.dp.toPx()
        val markerX = (progressW - markerW / 2f).coerceIn(0f, w - markerW)
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(markerX, 0f),
            size = Size(markerW, h),
            cornerRadius = CornerRadius(r, r)
        )
    }
}

@Composable
private fun ScaleLabels(
    maxScale: Int,
    labelCount: Int = 6,
    modifier: Modifier = Modifier
) {
    val safeMax = maxScale.coerceAtLeast(1)
    val n = labelCount.coerceAtLeast(2)

    val labels = remember(safeMax, n) {
        List(n) { i ->
            val v = safeMax * (i / (n - 1f))
            v
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach { v ->
            Text(
                text = v.roundToInt().toString(),
                color = SimAnalyzerTheme.extended.surface50,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AxisValueWithUnit(
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = TextStyle(
        color = SimAnalyzerTheme.material.onSurface,
        fontSize = 58.sp,
        fontWeight = FontWeight.Bold
    ),
    unitStyle: TextStyle = TextStyle(
        color = SimAnalyzerTheme.material.onSurfaceVariant,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ),
    axisBiasY: Dp = 0.dp
) {
    Layout(
        modifier = modifier,
        content = {
            Text(
                text = value,
                style = valueStyle,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis
            )
            Text(
                text = unit,
                style = unitStyle,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis
            )
        }
    ) { measurables, constraints ->
        val valueP = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val unitP = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))

        val biasPx = axisBiasY.roundToPx()

        val width = max(constraints.minWidth, max(valueP.width, unitP.width))

        val height = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            max(
                constraints.minHeight,
                valueP.height + unitP.height
            )
        }

        val axisY = height / 2 + biasPx

        val valueX = ((width - valueP.width) / 2f).roundToInt()
        val unitX = ((width - unitP.width) / 2f).roundToInt()

        val valueY = (axisY - valueP.height / 2f).roundToInt()
            .coerceIn(0, height - valueP.height)

        val unitY = (axisY + valueP.height / 2f).roundToInt()
            .coerceIn(0, height - unitP.height)

        layout(width, height) {
            valueP.place(valueX, valueY)
            unitP.place(unitX, unitY)
        }
    }
}

@Composable
fun GearBadge(
    gear: Int,
    size: Dp,
    baseColor: Color,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val textColor = SimAnalyzerTheme.material.onPrimary
    val style = remember(textColor) {
        TextStyle(
            color = textColor,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold
        )
    }

    Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension / 2f

        drawCircle(color = baseColor.copy(alpha = 0.2f), radius = r)
        drawCircle(
            color = baseColor,
            radius = r,
            style = Stroke(width = 4.dp.toPx())
        )

        val text = AnnotatedString(if (gear == -1) "R" else gear.toString())
        val layout = textMeasurer.measure(text = text, style = style)

        val x = (this.size.width - layout.size.width) / 2f
        val y = (this.size.height - layout.size.height) / 2f
        drawText(layout, topLeft = Offset(x, y))
    }
}

@Preview
@Composable
private fun TelemetryBlockPreview() {
    SimAnalyzerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(SimAnalyzerTheme.material.background),
            contentAlignment = Alignment.Center
        ) {
            TelemetryBlock(
                speedKmh = 20,
                rpmInt = 7250,
                rpmScale = 7.25f,
                maxScale = 13,
                gear = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 16.dp)
            )
        }
    }
}
