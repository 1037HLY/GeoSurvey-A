package com.geosurvey.android.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrackDetailFullScreen(
    points: List<TrackPoint>,
    isRecording: Boolean,
    onDismiss: () -> Unit
) {
    // 计算统计信息
    val stats = calculateTrackStats(points)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🗺️ 轨迹详情",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isRecording) "● 记录中" else "○ 已停止",
                    fontSize = 12.sp,
                    color = if (isRecording) SecondaryGreen else Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 20.sp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 上半部分：轨迹曲线
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A2332)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A2332)),
                contentAlignment = Alignment.Center
            ) {
                TrackFullChart(points = points)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 统计信息卡片
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📊 轨迹统计",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 第一行：里程 + 用时 + 点数
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItemFull("总里程", String.format("%.2f km", stats.totalDistance / 1000), PrimaryBlue)
                    StatItemFull("用时", stats.totalTime, SecondaryGreen)
                    StatItemFull("点数", "${stats.pointCount}", AccentPurple)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 第二行：平均速度 + 最大速度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItemFull("平均速度", String.format("%.1f km/h", stats.avgSpeed), Color(0xFF0F172A))
                    StatItemFull("最大速度", String.format("%.1f km/h", stats.maxSpeed), Color(0xFF0F172A))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 第三行：海拔数据
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItemFull("最大海拔", String.format("%.1f m", stats.maxAltitude), Color(0xFFEF4444))
                    StatItemFull("最小海拔", String.format("%.1f m", stats.minAltitude), Color(0xFF0EA5E9))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 第四行：累计爬升 + 累计下降
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItemFull("累计爬升", String.format("%.1f m", stats.totalAscent), Color(0xFF10B981))
                    StatItemFull("累计下降", String.format("%.1f m", stats.totalDescent), Color(0xFFEF4444))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 说明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A2332).copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "💡 蓝色曲线为轨迹路径 | 绿点=起点 红点=终点\n卡尔曼滤波 + 移动平均滤波 已启用",
                modifier = Modifier.padding(12.dp),
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun StatItemFull(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun TrackFullChart(points: List<TrackPoint>) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val width = size.width
        val height = size.height
        val padding = 8f
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        if (points.size > 1) {
            val displayPoints = points.takeLast(200)
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
            val range = max(latRange, lonRange)

            if (range > 0) {
                // 绘制网格线
                for (i in 0..4) {
                    val y = padding + chartHeight * (1f - i / 4f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
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
                    val normalizedX = (displayPoints[i].longitude - minLon) / range
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
                    style = Stroke(width = 3f)
                )

                // 填充区域
                val fillPath = androidx.compose.ui.graphics.Path()
                val lastX = padding + (displayPoints.size - 1) * step
                fillPath.moveTo(padding, padding + chartHeight)
                for (i in displayPoints.indices) {
                    val x = padding + i * step
                    val normalizedY = (displayPoints[i].latitude - minLat) / range
                    val y = padding + chartHeight * (1f - normalizedY.toFloat())
                    fillPath.lineTo(x, y)
                }
                fillPath.lineTo(lastX, padding + chartHeight)
                fillPath.close()
                drawPath(
                    path = fillPath,
                    color = PrimaryBlue.copy(alpha = 0.1f),
                    style = Stroke(width = 0f)
                )

                // 起点标记
                val startX = padding
                val startY = padding + chartHeight * (1f - ((displayPoints.first().latitude - minLat) / range).toFloat())
                drawCircle(
                    color = SecondaryGreen,
                    radius = 6f,
                    center = Offset(startX, startY)
                )
                drawCircle(
                    color = SecondaryGreen.copy(alpha = 0.3f),
                    radius = 10f,
                    center = Offset(startX, startY)
                )

                // 终点标记
                val endX = padding + (displayPoints.size - 1) * step
                val endY = padding + chartHeight * (1f - ((displayPoints.last().latitude - minLat) / range).toFloat())
                drawCircle(
                    color = ErrorRed,
                    radius = 6f,
                    center = Offset(endX, endY)
                )
                drawCircle(
                    color = ErrorRed.copy(alpha = 0.3f),
                    radius = 10f,
                    center = Offset(endX, endY)
                )

                // 位置标签
                // 使用点代替文字
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = 2f,
                    center = Offset(padding + chartWidth * 0.1f, padding + chartHeight * 0.05f)
                )
            }
        } else if (points.size == 1) {
            drawCircle(
                color = PrimaryBlue,
                radius = 6f,
                center = Offset(padding + chartWidth / 2, padding + chartHeight / 2)
            )
        } else {
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(padding, padding + chartHeight / 2),
                end = Offset(padding + chartWidth, padding + chartHeight / 2),
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
            )
        }
    }
}

// ========== 统计计算函数 ==========
data class TrackStats(
    val totalDistance: Double,
    val totalTime: String,
    val pointCount: Int,
    val avgSpeed: Double,
    val maxSpeed: Double,
    val maxAltitude: Double,
    val minAltitude: Double,
    val totalAscent: Double,
    val totalDescent: Double
)

fun calculateTrackStats(points: List<TrackPoint>): TrackStats {
    if (points.isEmpty()) {
        return TrackStats(
            totalDistance = 0.0,
            totalTime = "00:00:00",
            pointCount = 0,
            avgSpeed = 0.0,
            maxSpeed = 0.0,
            maxAltitude = 0.0,
            minAltitude = 0.0,
            totalAscent = 0.0,
            totalDescent = 0.0
        )
    }

    // 总距离
    var totalDistance = 0.0
    for (i in 1 until points.size) {
        val p1 = points[i - 1]
        val p2 = points[i]
        val lat1 = Math.toRadians(p1.latitude)
        val lon1 = Math.toRadians(p1.longitude)
        val lat2 = Math.toRadians(p2.latitude)
        val lon2 = Math.toRadians(p2.longitude)
        val dLat = lat2 - lat1
        val dLon = lon2 - lon1
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        totalDistance += 6378137.0 * c
    }

    // 用时
    val startTime = points.first().timestamp
    val endTime = points.last().timestamp
    val diff = endTime - startTime
    val hours = diff / 3600000
    val minutes = (diff % 3600000) / 60000
    val seconds = (diff % 60000) / 1000
    val totalTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    // 速度
    var maxSpeed = 0.0
    var totalSpeed = 0.0
    var speedCount = 0
    for (point in points) {
        point.speed?.let {
            val speedKmh = it * 3.6
            totalSpeed += speedKmh
            speedCount++
            if (speedKmh > maxSpeed) maxSpeed = speedKmh
        }
    }
    val avgSpeed = if (speedCount > 0) totalSpeed / speedCount else 0.0

    // 海拔
    var maxAltitude = -Double.MAX_VALUE
    var minAltitude = Double.MAX_VALUE
    var hasAltitude = false
    for (point in points) {
        point.altitude?.let {
            hasAltitude = true
            if (it > maxAltitude) maxAltitude = it
            if (it < minAltitude) minAltitude = it
        }
    }
    if (!hasAltitude) {
        maxAltitude = 0.0
        minAltitude = 0.0
    }

    // 累计爬升/下降
    var totalAscent = 0.0
    var totalDescent = 0.0
    for (i in 1 until points.size) {
        val p1 = points[i - 1]
        val p2 = points[i]
        if (p1.altitude != null && p2.altitude != null) {
            val diffAlt = p2.altitude - p1.altitude
            if (diffAlt > 0) totalAscent += diffAlt
            else totalDescent += kotlin.math.abs(diffAlt)
        }
    }

    return TrackStats(
        totalDistance = totalDistance,
        totalTime = totalTime,
        pointCount = points.size,
        avgSpeed = avgSpeed,
        maxSpeed = maxSpeed,
        maxAltitude = maxAltitude,
        minAltitude = minAltitude,
        totalAscent = totalAscent,
        totalDescent = totalDescent
    )
}
