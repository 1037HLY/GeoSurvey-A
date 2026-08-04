package com.geosurvey.android.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geosurvey.android.data.model.TrackPoint
import com.geosurvey.android.presentation.theme.*

@Composable
fun TrackMapProjection(
    points: List<TrackPoint>,
    modifier: Modifier = Modifier,
    height: Int = 150
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A2332)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val width = size.width
            val height = size.height
            val padding = 16f
            val chartWidth = width - padding * 2
            val chartHeight = height - padding * 2

            if (points.size > 1) {
                val displayPoints = points.takeLast(200)
                
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
                        val x = padding + ((displayPoints[i].longitude - minLon) / range).toFloat() * chartWidth
                        val y = padding + chartHeight * (1f - ((displayPoints[i].latitude - minLat) / range).toFloat())
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
                    val lastX = padding + ((displayPoints.last().longitude - minLon) / range).toFloat() * chartWidth
                    fillPath.moveTo(padding, padding + chartHeight)
                    for (i in displayPoints.indices) {
                        val x = padding + ((displayPoints[i].longitude - minLon) / range).toFloat() * chartWidth
                        val y = padding + chartHeight * (1f - ((displayPoints[i].latitude - minLat) / range).toFloat())
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

                    // 终点标记
                    val endX = padding + ((displayPoints.last().longitude - minLon) / range).toFloat() * chartWidth
                    val endY = padding + chartHeight * (1f - ((displayPoints.last().latitude - minLat) / range).toFloat())
                    drawCircle(
                        color = ErrorRed,
                        radius = 6f,
                        center = Offset(endX, endY)
                    )
                }
            } else if (points.size == 1) {
                // 单点
                val point = points[0]
                val x = padding + chartWidth / 2
                val y = padding + chartHeight / 2
                drawCircle(
                    color = PrimaryBlue,
                    radius = 6f,
                    center = Offset(x, y)
                )
            } else {
                // 无数据
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(padding, padding + chartHeight / 2),
                    end = Offset(padding + chartWidth, padding + chartHeight / 2),
                    strokeWidth = 1f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = 3f,
                    center = Offset(padding + chartWidth / 2, padding + chartHeight / 2)
                )
            }
        }
    }
}
