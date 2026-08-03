package com.geosurvey.android.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geosurvey.android.data.model.TrackPoint
import com.geosurvey.android.presentation.theme.*
import com.geosurvey.android.presentation.ui.components.GlassCard

@Composable
fun TrackPreviewCard(
    points: List<TrackPoint>,
    onClick: () -> Unit,
    isRecording: Boolean
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🗺️ 轨迹预览",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isRecording) "● 记录中" else "○ 已停止",
                        fontSize = 11.sp,
                        color = if (isRecording) SecondaryGreen else Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${points.size}点",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "点击放大 →",
                        fontSize = 10.sp,
                        color = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 轨迹曲线预览
            TrackMiniChart(points = points)
        }
    }
}

@Composable
fun TrackMiniChart(points: List<TrackPoint>) {
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

        if (points.size > 1) {
            // 取最近100个点
            val displayPoints = points.takeLast(100)
            val step = chartWidth / (displayPoints.size - 1)

            // 找经纬度范围
            var minLat = displayPoints[0].latitude
            var maxLat = displayPoints[0].latitude
            var minLon = displayPoints[0].longitude
            var maxLon = displayPoints[0].longitude

            for (point in displayPoints) {
                if (point.latitude < minLat) minLat = point.latitude
                if (point.latitude > maxLat) maxLat = point.latitude
                if (point.longitude < minLon) minLon = point.longitude
                if (point.longitude > maxLon) maxLon = point.longitude
            }

            val latRange = maxLat - minLat
            val lonRange = maxLon - minLon
            val range = if (latRange > lonRange) latRange else lonRange

            if (range > 0) {
                // 绘制网格线
                for (i in 0..2) {
                    val y = padding + chartHeight * (1f - i / 2f)
                    drawLine(
                        color = Color(0xFFE2E8F0).copy(alpha = 0.2f),
                        start = Offset(padding, y),
                        end = Offset(padding + chartWidth, y),
                        strokeWidth = 1f
                    )
                }

                // 绘制轨迹线
                var first = true
                val path = androidx.compose.ui.graphics.Path()
                for (i in displayPoints.indices) {
                    val x = padding + i * step
                    val normalizedY = (displayPoints[i].latitude - minLat) / range
                    val y = padding + chartHeight * (1f - normalizedY.toFloat())
                    if (first) {
                        path.moveTo(x, y)
                        first = false
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = PrimaryBlue,
                    style = Stroke(width = 2f)
                )

                // 起点标记
                val startX = padding
                val startY = padding + chartHeight * (1f - ((displayPoints.first().latitude - minLat) / range).toFloat())
                drawCircle(
                    color = SecondaryGreen,
                    radius = 4f,
                    center = Offset(startX, startY)
                )

                // 终点标记
                val endX = padding + (displayPoints.size - 1) * step
                val endY = padding + chartHeight * (1f - ((displayPoints.last().latitude - minLat) / range).toFloat())
                drawCircle(
                    color = ErrorRed,
                    radius = 4f,
                    center = Offset(endX, endY)
                )
            }
        } else if (points.size == 1) {
            // 单点
            drawCircle(
                color = PrimaryBlue,
                radius = 4f,
                center = Offset(padding + chartWidth / 2, padding + chartHeight / 2)
            )
        } else {
            // 无数据
            drawLine(
                color = Color(0xFF94A3B8),
                start = Offset(padding, padding + chartHeight / 2),
                end = Offset(padding + chartWidth, padding + chartHeight / 2),
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
            )
            drawCircle(
                color = Color(0xFF94A3B8),
                radius = 3f,
                center = Offset(padding + chartWidth / 2, padding + chartHeight / 2)
            )
        }
    }
}
