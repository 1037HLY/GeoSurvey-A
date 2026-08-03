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
    data: List<Double>,
    color: Color,
    unit: String = "",
    maxValue: Double = 100.0
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 标题
            Text(
                text = "$title ${if (data.isNotEmpty()) String.format("%.1f", data.last()) else "--"} $unit",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            // 曲线图
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
                    // 取最近50个点
                    val points = data.takeLast(50)
                    val step = chartWidth / (points.size - 1)
                    
                    // 找最大值
                    var maxVal = 1.0
                    for (value in points) {
                        if (value > maxVal) maxVal = value
                    }
                    if (maxVal < maxValue) maxVal = maxValue

                    // 绘制网格线
                    for (i in 0..4) {
                        val y = padding + chartHeight * (1f - i / 4f)
                        drawLine(
                            color = Color(0xFFE2E8F0).copy(alpha = 0.3f),
                            start = Offset(padding, y),
                            end = Offset(padding + chartWidth, y),
                            strokeWidth = 1f
                        )
                    }

                    // 绘制曲线
                    var first = true
                    val path = androidx.compose.ui.graphics.Path()
                    for (i in points.indices) {
                        val x = padding + i * step
                        val ratio = (points[i] / maxVal).toFloat()
                        val y = padding + chartHeight * (1f - ratio)
                        if (first) {
                            path.moveTo(x, y)
                            first = false
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 2f)
                    )

                    // 绘制填充
                    if (points.isNotEmpty()) {
                        val fillPath = androidx.compose.ui.graphics.Path()
                        val lastX = padding + (points.size - 1) * step
                        fillPath.moveTo(padding, padding + chartHeight)
                        for (i in points.indices) {
                            val x = padding + i * step
                            val ratio = (points[i] / maxVal).toFloat()
                            val y = padding + chartHeight * (1f - ratio)
                            fillPath.lineTo(x, y)
                        }
                        fillPath.lineTo(lastX, padding + chartHeight)
                        fillPath.close()
                        drawPath(
                            path = fillPath,
                            color = color.copy(alpha = 0.15f),
                            style = Stroke(width = 0f)
                        )
                    }
                } else {
                    // 无数据时显示虚线
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
