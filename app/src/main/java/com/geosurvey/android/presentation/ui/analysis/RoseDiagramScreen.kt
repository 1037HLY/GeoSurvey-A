package com.geosurvey.android.presentation.ui.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.geosurvey.android.data.model.AttitudeRecord
import com.geosurvey.android.utils.RoseDiagram
import kotlin.math.*

@Composable
fun RoseDiagramScreen(
    records: List<AttitudeRecord>,
    onBack: () -> Unit
) {
    val roseData = RoseDiagram.calculate(records)
    val dominantStrike = RoseDiagram.getDominantStrike(records)

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
                text = "🌹 走向玫瑰花图",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            TextButton(onClick = onBack) {
                Text("返回")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 统计信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("数据点", fontSize = 11.sp, color = Color(0xFF475569))
                    Text("${roseData.totalCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("最大频数", fontSize = 11.sp, color = Color(0xFF475569))
                    Text("${roseData.maxCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("优势走向", fontSize = 11.sp, color = Color(0xFF475569))
                    Text(
                        dominantStrike?.let { String.format("%.1f°", it) } ?: "--",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5CF6)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 玫瑰花图
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF8FAFC)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val maxRadius = minOf(size.width, size.height) / 2 * 0.8f

                    if (roseData.bins.isNotEmpty() && roseData.maxCount > 0) {
                        // 绘制每个扇形
                        val binSize = 360f / roseData.bins.size
                        roseData.bins.forEach { bin ->
                            val radius = maxRadius * bin.normalizedValue
                            if (radius > 1f) {
                                val startAngle = bin.angle - binSize / 2
                                val endAngle = bin.angle + binSize / 2

                                val startRad = Math.toRadians(startAngle.toDouble())
                                val endRad = Math.toRadians(endAngle.toDouble())
                                val steps = 20

                                // 绘制从中心到边缘的线（近似扇形）
                                val colorIntensity = 0.3f + 0.7f * bin.normalizedValue
                                val color = Color(
                                    red = 0.05f,
                                    green = 0.6f + 0.4f * bin.normalizedValue,
                                    blue = 0.9f - 0.5f * bin.normalizedValue,
                                    alpha = colorIntensity
                                )

                                for (i in 0..steps) {
                                    val t = startRad + (endRad - startRad) * i / steps
                                    val x = centerX + radius * sin(t).toFloat()
                                    val y = centerY + radius * cos(t).toFloat()
                                    drawLine(
                                        color = color,
                                        start = Offset(centerX, centerY),
                                        end = Offset(x, y),
                                        strokeWidth = 3f
                                    )
                                }
                            }
                        }
                    }

                    // 绘制同心圆网格
                    for (i in 1..4) {
                        val gridRadius = maxRadius * i / 4
                        drawCircle(
                            color = Color(0xFF94A3B8).copy(alpha = 0.3f),
                            radius = gridRadius,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 1f)
                        )
                    }

                    // 绘制十字线
                    drawLine(
                        color = Color(0xFF94A3B8).copy(alpha = 0.5f),
                        start = Offset(centerX - maxRadius, centerY),
                        end = Offset(centerX + maxRadius, centerY),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0xFF94A3B8).copy(alpha = 0.5f),
                        start = Offset(centerX, centerY - maxRadius),
                        end = Offset(centerX, centerY + maxRadius),
                        strokeWidth = 1f
                    )

                    // 绘制方向标签
                    val labelRadius = maxRadius * 1.1f
                    val directions = listOf(
                        "N" to 0f,
                        "E" to 90f,
                        "S" to 180f,
                        "W" to 270f
                    )
                    // 在Canvas上无法直接绘制文字，用点代替
                    directions.forEach { (_, angle) ->
                        val rad = Math.toRadians(angle.toDouble())
                        val x = centerX + labelRadius * sin(rad).toFloat()
                        val y = centerY + labelRadius * cos(rad).toFloat()
                        drawCircle(
                            color = Color(0xFF0F172A),
                            radius = 3f,
                            center = Offset(x, y)
                        )
                    }

                    // 绘制中心点
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = 3f,
                        center = Offset(centerX, centerY)
                    )
                }

                // 图例
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        "颜色越深 = 频数越高",
                        fontSize = 10.sp,
                        color = Color(0xFF475569)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 说明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF1F5F9)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "💡 玫瑰花图展示走向方向分布，长度表示该方向出现的频率",
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                color = Color(0xFF475569)
            )
        }
    }
}
