package com.geosurvey.android.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.geosurvey.android.presentation.theme.*
import com.geosurvey.android.presentation.viewmodel.LocationState
import kotlin.math.*

@Composable
fun SatelliteFullScreen(
    state: LocationState,
    onDismiss: () -> Unit
) {
    val satelliteList = generateSatelliteData(state)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛰️ 卫星实时状态",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = onDismiss) {
                Text("✕", fontSize = 24.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 统计信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItemFull("总数", "${state.satelliteCount}", Color.White)
            StatItemFull("可用", "${state.usedSatelliteCount}", SecondaryGreen)
            StatItemFull("质量", state.qualityText, state.qualityColor)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 上半部分：极坐标图
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
                PolarPlotFull(state)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 图例
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItemFull(color = Color(0xFF4CAF50), label = "GPS", count = state.gpsCount)
            LegendItemFull(color = Color(0xFF2196F3), label = "GLONASS", count = state.glonassCount)
            LegendItemFull(color = Color(0xFFF44336), label = "北斗", count = state.beidouCount)
            LegendItemFull(color = Color(0xFFFFC107), label = "Galileo", count = state.galileoCount)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 下半部分：卫星列表
        Text(
            text = "📋 卫星详细信息",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A2332).copy(alpha = 0.5f))
                .clip(RoundedCornerShape(12.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(satelliteList) { satellite ->
                SatelliteListItem(satellite)
            }
        }
    }
}

@Composable
fun StatItemFull(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun PolarPlotFull(state: LocationState) {
    Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = minOf(size.width, size.height) / 2 * 0.85f

        for (i in 1..3) {
            val r = radius * i / 3
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = r,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )
        }

        for (angle in 0..360 step 45) {
            val rad = Math.toRadians(angle.toDouble())
            val endX = centerX + (radius * cos(rad)).toFloat()
            val endY = centerY + (radius * sin(rad)).toFloat()
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 1f
            )
        }

        val total = max(state.satelliteCount, 1)
        val ratios = listOf(
            state.gpsCount.toFloat() / total,
            state.glonassCount.toFloat() / total,
            state.beidouCount.toFloat() / total,
            state.galileoCount.toFloat() / total
        )
        val colors = listOf(
            Color(0xFF4CAF50),
            Color(0xFF2196F3),
            Color(0xFFF44336),
            Color(0xFFFFC107)
        )

        colors.forEachIndexed { index, color ->
            val count = (ratios[index] * 16).toInt().coerceIn(0, 16)
            for (i in 0 until count) {
                val angle = (i * 22 + index * 9) % 360
                val rad = Math.toRadians(angle.toDouble())
                val distance = (0.15 + 0.75 * (i.toFloat() / max(count, 1))) * radius
                val x = centerX + (distance * cos(rad)).toFloat()
                val y = centerY + (distance * sin(rad)).toFloat()
                val size = 5f + (i % 3) * 1.5f
                drawCircle(
                    color = color.copy(alpha = 0.8f + 0.2f * (i.toFloat() / max(count, 1))),
                    radius = size,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = color.copy(alpha = 0.1f),
                    radius = size * 2.5f,
                    center = Offset(x, y)
                )
            }
        }

        drawCircle(
            color = Color.White,
            radius = 4f,
            center = Offset(centerX, centerY)
        )
    }
}

data class SatelliteInfo(
    val name: String,
    val constellation: String,
    val snr: Float,
    val elevation: Float,
    val azimuth: Float,
    val isUsed: Boolean,
    val color: Color
)

fun generateSatelliteData(state: LocationState): List<SatelliteInfo> {
    val list = mutableListOf<SatelliteInfo>()
    val constellations = listOf(
        "GPS" to Color(0xFF4CAF50),
        "GLONASS" to Color(0xFF2196F3),
        "北斗" to Color(0xFFF44336),
        "Galileo" to Color(0xFFFFC107)
    )
    val counts = listOf(
        state.gpsCount,
        state.glonassCount,
        state.beidouCount,
        state.galileoCount
    )
    
    var index = 1
    constellations.forEachIndexed { ci, (name, color) ->
        val count = counts[ci]
        for (i in 0 until count) {
            val snr = 20f + (Math.random() * 25).toFloat()
            val elevation = 10f + (Math.random() * 80).toFloat()
            val azimuth = (Math.random() * 360).toFloat()
            val isUsed = Math.random() > 0.3
            list.add(
                SatelliteInfo(
                    name = "${name}${String.format("%02d", index)}",
                    constellation = name,
                    snr = snr,
                    elevation = elevation,
                    azimuth = azimuth,
                    isUsed = isUsed,
                    color = color
                )
            )
            index++
        }
    }
    return list
}

@Composable
fun LegendItemFull(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: $count",
            fontSize = 11.sp,
            color = Color.White
        )
    }
}

@Composable
fun SatelliteListItem(satellite: SatelliteInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (satellite.isUsed) Color(0xFF1A2332).copy(alpha = 0.5f)
                else Color(0xFF1A2332).copy(alpha = 0.2f)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(satellite.color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = satellite.name,
                fontSize = 12.sp,
                fontWeight = if (satellite.isUsed) FontWeight.Bold else FontWeight.Normal,
                color = if (satellite.isUsed) Color.White else Color(0xFF94A3B8)
            )
        }
        Text(
            text = "SNR: ${String.format("%.1f", satellite.snr)}",
            fontSize = 11.sp,
            color = if (satellite.snr > 35) SecondaryGreen else Color(0xFF94A3B8)
        )
        Text(
            text = "仰角: ${String.format("%.0f", satellite.elevation)}°",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = "方位: ${String.format("%.0f", satellite.azimuth)}°",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = if (satellite.isUsed) "✅" else "⏳",
            fontSize = 12.sp
        )
    }
}
