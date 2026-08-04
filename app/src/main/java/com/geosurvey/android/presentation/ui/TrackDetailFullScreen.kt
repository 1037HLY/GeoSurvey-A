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

@Composable
fun TrackDetailFullScreen(
    points: List<TrackPoint>,
    isRecording: Boolean,
    onDismiss: () -> Unit
) {
    val stats = calculateTrackStats(points)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
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

        // 轨迹地图投影
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
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
                TrackMapProjection(points = points, height = 200)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TrackStatItem("总里程", String.format("%.2f km", stats.totalDistance / 1000), PrimaryBlue)
                    TrackStatItem("用时", stats.totalTime, SecondaryGreen)
                    TrackStatItem("点数", "${stats.pointCount}", AccentPurple)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TrackStatItem("平均速度", String.format("%.1f km/h", stats.avgSpeed), Color(0xFF0F172A))
                    TrackStatItem("最大速度", String.format("%.1f km/h", stats.maxSpeed), Color(0xFF0F172A))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TrackStatItem("最大海拔", String.format("%.1f m", stats.maxAltitude), Color(0xFFEF4444))
                    TrackStatItem("最小海拔", String.format("%.1f m", stats.minAltitude), Color(0xFF0EA5E9))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TrackStatItem("累计爬升", String.format("%.1f m", stats.totalAscent), Color(0xFF10B981))
                    TrackStatItem("累计下降", String.format("%.1f m", stats.totalDescent), Color(0xFFEF4444))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
fun TrackStatItem(label: String, value: String, color: Color) {
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

    var totalDistance = 0.0
    var lastValidPoint: TrackPoint? = null
    for (point in points) {
        if (lastValidPoint != null) {
            val p1 = lastValidPoint!!
            val p2 = point
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
        lastValidPoint = point
    }

    val startTime = points.first().timestamp
    val endTime = points.last().timestamp
    val diff = endTime - startTime
    val hours = diff / 3600000
    val minutes = (diff % 3600000) / 60000
    val seconds = (diff % 60000) / 1000
    val totalTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    val timeHours = diff / 3600000.0
    val avgSpeed = if (timeHours > 0) (totalDistance / 1000) / timeHours else 0.0

    var maxSpeed = 0.0
    for (point in points) {
        point.speed?.let {
            val speedKmh = it * 3.6
            if (speedKmh > maxSpeed) maxSpeed = speedKmh
        }
    }

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

    var totalAscent = 0.0
    var totalDescent = 0.0
    for (i in 1 until points.size) {
        val p1 = points[i - 1]
        val p2 = points[i]
        if (p1.altitude != null && p2.altitude != null) {
            val diffAlt = p2.altitude - p1.altitude
            if (diffAlt > 1.0) {
                totalAscent += diffAlt
            } else if (diffAlt < -1.0) {
                totalDescent += kotlin.math.abs(diffAlt)
            }
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
