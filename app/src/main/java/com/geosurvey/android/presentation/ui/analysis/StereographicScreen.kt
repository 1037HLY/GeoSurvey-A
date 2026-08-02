package com.geosurvey.android.presentation.ui.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geosurvey.android.data.model.AttitudeRecord
import com.geosurvey.android.utils.StereographicProjection

@Composable
fun StereographicScreen(
    records: List<AttitudeRecord>,
    onBack: () -> Unit
) {
    val projectionPoints = StereographicProjection.projectRecords(records)
    val statistics = StereographicProjection.getStatistics(records)
    val greatCircle = StereographicProjection.getGreatCirclePoints()

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
                text = "📐 赤平投影图",
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
                    Text("${statistics.count}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("平均倾向", fontSize = 11.sp, color = Color(0xFF475569))
                    Text(
                        String.format("%.1f°", statistics.avgDirection),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0EA5E9)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("平均倾角", fontSize = 11.sp, color = Color(0xFF475569))
                    Text(
                        String.format("%.1f°", statistics.avgAngle),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("优势方向", fontSize = 11.sp, color = Color(0xFF475569))
                    Text(
                        String.format("%.1f°", statistics.dominantDirection),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5CF6)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 赤平投影图
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
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = minOf(size.width, size.height) / 2 * 0.9f

                    // 绘制大圆
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = radius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 2f)
                    )

                    // 绘制网格线（十字线）
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(centerX - radius, centerY),
                        end = Offset(centerX + radius, centerY),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(centerX, centerY - radius),
                        end = Offset(centerX, centerY + radius),
                        strokeWidth = 1f
                    )

                    // 绘制方向标记
                    val markers = StereographicProjection.getDirectionMarkers()
                    val labels = StereographicProjection.getDirectionLabels()
                    markers.forEachIndexed { index, (x, y) ->
                        val px = centerX + x * radius
                        val py = centerY + y * radius
                        // 在Canvas上无法直接绘制文字，使用DrawScope的drawContext
                        // 这里简化，只画点
                    }

                    // 绘制投影点
                    if (records.isNotEmpty()) {
                        projectionPoints.forEach { point ->
                            val px = centerX + point.x * radius
                            val py = centerY + point.y * radius
                            drawCircle(
                                color = Color(0xFF0EA5E9),
                                radius = 6f,
                                center = Offset(px, py)
                            )
                            // 绘制外圈
                            drawCircle(
                                color = Color(0xFF0EA5E9).copy(alpha = 0.3f),
                                radius = 10f,
                                center = Offset(px, py),
                                style = Stroke(width = 1f)
                            )
                        }

                        // 绘制优势方向标记
                        val avgX = centerX + 0.7f * radius * kotlin.math.sin(
                            Math.toRadians(statistics.avgDirection.toDouble())
                        ).toFloat()
                        val avgY = centerY + 0.7f * radius * kotlin.math.cos(
                            Math.toRadians(statistics.avgDirection.toDouble())
                        ).toFloat()
                        drawCircle(
                            color = Color(0xFFEF4444),
                            radius = 8f,
                            center = Offset(avgX, avgY)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0EA5E9))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("产状数据", fontSize = 10.sp, color = Color(0xFF475569))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("平均方向", fontSize = 10.sp, color = Color(0xFF475569))
                    }
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
                text = "💡 蓝色圆点：各产状数据投影 | 红色圆点：平均倾向方向",
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                color = Color(0xFF475569)
            )
        }
    }
}

// 扩展函数：在Canvas上绘制文字
fun DrawScope.drawText(text: String, x: Float, y: Float, color: Color = Color.Black) {
    // 使用drawContext.canvas.nativeCanvas绘制文字需要Android Canvas
    // 这里简化，实际项目中可使用drawIntoCanvas
}
