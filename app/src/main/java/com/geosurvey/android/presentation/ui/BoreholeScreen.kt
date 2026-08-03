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
import androidx.compose.ui.window.Dialog
import com.geosurvey.android.presentation.theme.*
import com.geosurvey.android.presentation.ui.components.GlassCard
import kotlin.math.*

@Composable
fun BoreholeScreen() {
    var showFullDialog by remember { mutableStateOf(false) }
    
    // 输入参数
    var collarX by remember { mutableStateOf("") }
    var collarY by remember { mutableStateOf("") }
    var azimuth by remember { mutableStateOf("") }
    var dipAngle by remember { mutableStateOf("") }
    var depth by remember { mutableStateOf("") }
    
    // 计算结果
    var resultBottomX by remember { mutableStateOf<String?>(null) }
    var resultBottomY by remember { mutableStateOf<String?>(null) }
    var resultDepth by remember { mutableStateOf<String?>(null) }
    var resultHorizontal by remember { mutableStateOf<String?>(null) }
    var resultVertical by remember { mutableStateOf<String?>(null) }
    
    // 柱状图数据
    val layers = listOf(
        "表土层" to 0.5,
        "粘土层" to 2.0,
        "砂层" to 3.5,
        "风化岩" to 5.0,
        "基岩" to 8.0
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "🔧 钻孔计算",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 输入卡片
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📥 输入参数",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = collarX,
                onValueChange = { collarX = it },
                label = { Text("孔口X坐标") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = collarY,
                onValueChange = { collarY = it },
                label = { Text("孔口Y坐标") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = azimuth,
                    onValueChange = { azimuth = it },
                    label = { Text("方位角 (°)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dipAngle,
                    onValueChange = { dipAngle = it },
                    label = { Text("倾角 (°)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = depth,
                onValueChange = { depth = it },
                label = { Text("钻孔深度 (m)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    try {
                        val azi = azimuth.toDoubleOrNull() ?: 0.0
                        val dip = dipAngle.toDoubleOrNull() ?: 90.0
                        val dep = depth.toDoubleOrNull() ?: 10.0
                        val x = collarX.toDoubleOrNull() ?: 0.0
                        val y = collarY.toDoubleOrNull() ?: 0.0

                        val dipRad = Math.toRadians(dip)
                        val aziRad = Math.toRadians(azi)

                        val horizontal = dep * cos(dipRad)
                        val vertical = dep * sin(dipRad)

                        resultBottomX = String.format("%.3f", x + horizontal * sin(aziRad))
                        resultBottomY = String.format("%.3f", y + horizontal * cos(aziRad))
                        resultDepth = String.format("%.2f", dep)
                        resultHorizontal = String.format("%.2f", horizontal)
                        resultVertical = String.format("%.2f", vertical)

                    } catch (e: Exception) {
                        // 计算失败
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🔄 计算")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 结果显示
        if (resultBottomX != null) {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📊 计算结果",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "孔底X: ${resultBottomX}",
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "孔底Y: ${resultBottomY}",
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "钻孔深度: ${resultDepth}m",
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "水平投影: ${resultHorizontal}m",
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "垂直投影: ${resultVertical}m",
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // ⭐ 柱状图
                Text(
                    text = "📊 简易柱状图",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 柱状图
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFFF1F5F9))
                        .clip(RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    // 绘制柱状图
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val chartWidth = size.width
                        val chartHeight = size.height
                        val padding = 20f
                        val barWidth = 30f
                        val maxDepth = layers.maxOfOrNull { it.second } ?: 10.0
                        
                        layers.forEachIndexed { index, (name, depthValue) ->
                            val x = padding + index * (barWidth + 10f)
                            val barHeight = (depthValue / maxDepth * (chartHeight - padding * 2)).toFloat()
                            val y = padding + chartHeight - padding - barHeight
                            
                            // 绘制柱体
                            drawRect(
                                color = when (index) {
                                    0 -> Color(0xFF8D6E63)
                                    1 -> Color(0xFFFFB74D)
                                    2 -> Color(0xFFFFD54F)
                                    3 -> Color(0xFFA1887F)
                                    else -> Color(0xFF78909C)
                                },
                                topLeft = Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                            )
                            
                            // 绘制标签
                            // 使用文字标签（在Canvas中简化处理）
                        }
                    }
                    
                    // 图例
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        layers.forEach { (name, _) ->
                            Text(
                                text = name,
                                fontSize = 8.sp,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 使用说明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF1F5F9)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "💡 输入孔口坐标、方位角、倾角和深度，计算孔底位置",
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                color = Color(0xFF475569)
            )
        }
    }

    // 全屏对话框
    if (showFullDialog) {
        Dialog(
            onDismissRequest = { showFullDialog = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔧 钻孔计算 - 全屏",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        IconButton(onClick = { showFullDialog = false }) {
                            Text("✕", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 重新显示输入和结果
                    Text(
                        text = "📥 输入参数",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = collarX,
                        onValueChange = { collarX = it },
                        label = { Text("孔口X坐标") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    OutlinedTextField(
                        value = collarY,
                        onValueChange = { collarY = it },
                        label = { Text("孔口Y坐标") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = azimuth,
                            onValueChange = { azimuth = it },
                            label = { Text("方位角 (°)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = dipAngle,
                            onValueChange = { dipAngle = it },
                            label = { Text("倾角 (°)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    OutlinedTextField(
                        value = depth,
                        onValueChange = { depth = it },
                        label = { Text("钻孔深度 (m)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            try {
                                val azi = azimuth.toDoubleOrNull() ?: 0.0
                                val dip = dipAngle.toDoubleOrNull() ?: 90.0
                                val dep = depth.toDoubleOrNull() ?: 10.0
                                val x = collarX.toDoubleOrNull() ?: 0.0
                                val y = collarY.toDoubleOrNull() ?: 0.0

                                val dipRad = Math.toRadians(dip)
                                val aziRad = Math.toRadians(azi)

                                val horizontal = dep * cos(dipRad)
                                val vertical = dep * sin(dipRad)

                                resultBottomX = String.format("%.3f", x + horizontal * sin(aziRad))
                                resultBottomY = String.format("%.3f", y + horizontal * cos(aziRad))
                                resultDepth = String.format("%.2f", dep)
                                resultHorizontal = String.format("%.2f", horizontal)
                                resultVertical = String.format("%.2f", vertical)

                            } catch (e: Exception) { }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🔄 计算")
                    }
                    
                    if (resultBottomX != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "📊 计算结果",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "孔底X: ${resultBottomX}",
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "孔底Y: ${resultBottomY}",
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "钻孔深度: ${resultDepth}m",
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "水平投影: ${resultHorizontal}m",
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "垂直投影: ${resultVertical}m",
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}
