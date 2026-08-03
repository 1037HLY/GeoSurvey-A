package com.geosurvey.android.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.geosurvey.android.presentation.ui.components.GlassCard
import com.geosurvey.android.presentation.viewmodel.LocationState
import kotlin.math.*

@Composable
fun SatelliteDetailScreen(
    state: LocationState,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛰️ 卫星详情",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            IconButton(onClick = onDismiss) {
                Text("✕", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 统计信息
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("总数", fontSize = 11.sp, color = Color(0xFF475569))
                    Text("${state.satelliteCount}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("可用", fontSize = 11.sp, color = Color(0xFF475569))
                    Text("${state.usedSatelliteCount}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SecondaryGreen)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("质量", fontSize = 11.sp, color = Color(0xFF475569))
                    Text(state.qualityText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = state.qualityColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 极坐标图
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F172A)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                PolarPlot(state)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 星座图例
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color(0xFF4CAF50), label = "GPS", count = state.gpsCount)
                LegendItem(color = Color(0xFF2196F3), label = "GLONASS", count = state.glonassCount)
                LegendItem(color = Color(0xFFF44336), label = "北斗", count = state.beidouCount)
                LegendItem(color = Color(0xFFFFC107), label = "Galileo", count = state.galileoCount)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "💡 圆点位置表示卫星在天空中的分布",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun PolarPlot(state: LocationState) {
    Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = minOf(size.width, size.height) / 2 * 0.85f

        // 绘制同心圆
        for (i in 1..3) {
            val r = radius * i / 3
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = r,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )
        }

        // 绘制十字线
        for (angle in 0..360 step 45) {
            val rad = Math.toRadians(angle.toDouble())
            val endX = centerX + radius * cos(rad).toFloat()
            val endY = centerY + radius * sin(rad).toFloat()
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 1f
            )
        }

        // 绘制模拟卫星点
        val total = max(state.satelliteCount, 1)
        val gpsRatio = state.gpsCount.toFloat() / total
        val glonassRatio = state.glonassCount.toFloat() / total
        val beidouRatio = state.beidouCount.toFloat() / total
        val galileoRatio = state.galileoCount.toFloat() / total

        val satelliteColors = listOf(
            Color(0xFF4CAF50) to gpsRatio,
            Color(0xFF2196F3) to glonassRatio,
            Color(0xFFF44336) to beidouRatio,
            Color(0xFFFFC107) to galileoRatio
        )

        satelliteColors.forEachIndexed { index, (color, ratio) ->
            val count = (ratio * 12).toInt().coerceIn(0, 12)
            for (i in 0 until count) {
                val angle = (i * 30 + index * 7) % 360
                val rad = Math.toRadians(angle.toDouble())
                val distance = (0.2 + 0.7 * (i.toFloat() / max(count, 1))) * radius
                // ⭐ 修复：使用 .toFloat() 确保类型匹配
                val x = centerX + (distance * cos(rad).toFloat())
                val y = centerY + (distance * sin(rad).toFloat())
                val size = 6f + (i % 3) * 2f
                drawCircle(
                    color = color.copy(alpha = 0.7f + 0.3f * (i.toFloat() / max(count, 1))),
                    radius = size,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = color.copy(alpha = 0.15f),
                    radius = size * 2f,
                    center = Offset(x, y)
                )
            }
        }

        // 绘制中心点
        drawCircle(
            color = Color.White,
            radius = 4f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = 12f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1f)
        )
    }
}

@Composable
fun LegendItem(color: Color, label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF475569)
        )
        Text(
            text = "$count",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
