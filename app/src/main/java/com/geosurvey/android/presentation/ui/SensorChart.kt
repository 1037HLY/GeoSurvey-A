package com.geosurvey.android.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geosurvey.android.presentation.ui.components.GlassCard

@Composable
fun SensorChart(
    title: String,
    data: List<Float>,
    color: Color,
    unit: String = "",
    maxValue: Float = 100f
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "$title ${if (data.isNotEmpty()) String.format("%.1f", data.last()) else "--"} $unit",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 4f
                val chartWidth = width - padding * 2
                val chartHeight = height - padding * 2

                if (data.size > 1) {
                    val points = data.takeLast(50)
                    val step = chartWidth / (points.size - 1)
                    val maxVal = max(maxValue, points.maxOrNull() ?: 1f)

                    for (i in 0..4) {
                        val y = padding + chartHeight * (1 - i / 4f)
                        drawLine(
                            color = Color(0xFFE2E8F0).copy(alpha = 0.3f),
                            start = Offset(padding, y),
                            end = Offset(padding + chartWidth, y),
                            strokeWidth = 1f
                        )
                    }

                    val path = androidx.compose.ui.graphics.Path().apply {
                        points.forEachIndexed { index, value ->
                            val x = padding + index * step
                            val y = padding + chartHeight * (1 - value / maxVal)
                            if (index == 0) {
                                moveTo(x, y)
                            } else {
                                lineTo(x, y)
                            }
                        }
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 2f)
                    )

                    val fillPath = androidx.compose.ui.graphics.Path().apply {
                        val lastX = padding + (points.size - 1) * step
                        moveTo(padding, padding + chartHeight)
                        points.forEachIndexed { index, value ->
                            val x = padding + index * step
                            val y = padding + chartHeight * (1 - value / maxVal)
                            lineTo(x, y)
                        }
                        lineTo(lastX, padding + chartHeight)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        color = color.copy(alpha = 0.15f),
                        style = Stroke(width = 0f)
                    )
                } else {
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(padding, padding + chartHeight / 2),
                        end = Offset(padding + chartWidth, padding + chartHeight / 2),
                        strokeWidth = 1f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                    )
                }
            }
        }
    }
}
