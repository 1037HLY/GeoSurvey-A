package com.geosurvey.android.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class LayerData(
    val name: String,
    val depth: String,
    val color: Color
)

@Composable
fun BoreholeScreen() {
    var showFullDialog by remember { mutableStateOf(false) }
    
    var collarX by remember { mutableStateOf("") }
    var collarY by remember { mutableStateOf("") }
    var azimuth by remember { mutableStateOf("") }
    var dipAngle by remember { mutableStateOf("") }
    var boreholeDepth by remember { mutableStateOf("") }
    var boreholeDiameter by remember { mutableStateOf("75") }
    
    var layers by remember { mutableStateOf(listOf<LayerData>()) }
    var layerName by remember { mutableStateOf("") }
    var layerDepth by remember { mutableStateOf("") }
    
    var resultBottomX by remember { mutableStateOf<String?>(null) }
    var resultBottomY by remember { mutableStateOf<String?>(null) }
    var resultDepth by remember { mutableStateOf<String?>(null) }
    var resultHorizontal by remember { mutableStateOf<String?>(null) }
    var resultVertical by remember { mutableStateOf<String?>(null) }
    var resultDipAngle by remember { mutableStateOf<String?>(null) }
    
    val layerColors = listOf(
        Color(0xFF8D6E63),
        Color(0xFFFFB74D),
        Color(0xFFFFD54F),
        Color(0xFFA1887F),
        Color(0xFF78909C),
        Color(0xFF4DB6AC),
        Color(0xFFBA68C8),
        Color(0xFF4FC3F7),
        Color(0xFF81C784),
        Color(0xFFFF8A65)
    )

    fun calculate() {
        try {
            val azi = azimuth.toDoubleOrNull() ?: 0.0
            val dip = dipAngle.toDoubleOrNull() ?: 90.0
            val dep = boreholeDepth.toDoubleOrNull() ?: 10.0
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
            resultDipAngle = String.format("%.1f", dip)

        } catch (e: Exception) { }
    }

    fun addLayer() {
        if (layerName.isNotEmpty() && layerDepth.isNotEmpty()) {
            val depthVal = layerDepth.toDoubleOrNull()
            if (depthVal != null && depthVal > 0) {
                val colorIndex = layers.size % layerColors.size
                layers = layers + LayerData(
                    name = layerName,
                    depth = layerDepth,
                    color = layerColors[colorIndex]
                )
                layerName = ""
                layerDepth = ""
            }
        }
    }

    fun removeLayer(index: Int) {
        layers = layers.toMutableList().apply { removeAt(index) }
    }

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

        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📥 钻孔参数",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = collarX,
                    onValueChange = { collarX = it },
                    label = { Text("孔口X") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = collarY,
                    onValueChange = { collarY = it },
                    label = { Text("孔口Y") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }
            
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = boreholeDepth,
                    onValueChange = { boreholeDepth = it },
                    label = { Text("孔深 (m)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = boreholeDiameter,
                    onValueChange = { boreholeDiameter = it },
                    label = { Text("孔径 (mm)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { calculate() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🔄 计算钻孔参数")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📋 地层信息",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = layerName,
                    onValueChange = { layerName = it },
                    label = { Text("层名") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = layerDepth,
                    onValueChange = { layerDepth = it },
                    label = { Text("厚度 (m)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Button(
                    onClick = { addLayer() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryGreen
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("添加")
                }
            }

            if (layers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFFF1F5F9))
                        .clip(RoundedCornerShape(8.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(layers) { layer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(layer.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${layer.name} (${layer.depth}m)",
                                    fontSize = 12.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            IconButton(
                                onClick = { 
                                    val index = layers.indexOf(layer)
                                    removeLayer(index)
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Text("✕", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (resultBottomX != null) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFullDialog = true }
            ) {
                Text(
                    text = "📊 计算结果 (点击放大)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("孔底X", fontSize = 10.sp, color = Color(0xFF475569))
                        Text("${resultBottomX}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("孔底Y", fontSize = 10.sp, color = Color(0xFF475569))
                        Text("${resultBottomY}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("深度", fontSize = 10.sp, color = Color(0xFF475569))
                        Text("${resultDepth}m", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("水平投影", fontSize = 10.sp, color = Color(0xFF475569))
                        Text("${resultHorizontal}m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("垂直投影", fontSize = 10.sp, color = Color(0xFF475569))
                        Text("${resultVertical}m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("倾角", fontSize = 10.sp, color = Color(0xFF475569))
                        Text("${resultDipAngle}°", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "📊 钻孔柱状图",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                BoreholeColumnChart(
                    layers = layers,
                    totalDepth = boreholeDepth.toDoubleOrNull() ?: 10.0,
                    diameter = boreholeDiameter.toDoubleOrNull() ?: 75.0
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF1F5F9)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "💡 输入钻孔参数和地层信息，点击「计算钻孔参数」生成柱状图\n点击结果卡片可全屏查看",
                modifier = Modifier.padding(12.dp),
                fontSize = 11.sp,
                color = Color(0xFF475569),
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showFullDialog) {
        Dialog(
            onDismissRequest = { showFullDialog = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
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
                            text = "🔧 钻孔柱状图 - 全屏",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        IconButton(onClick = { showFullDialog = false }) {
                            Text("✕", fontSize = 20.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (resultBottomX != null) {
                        Text(
                            text = "📊 钻孔参数",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF1F5F9)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "孔口坐标: (${collarX}, ${collarY})",
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "方位角: ${azimuth}° | 倾角: ${dipAngle}°",
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "孔深: ${boreholeDepth}m | 孔径: ${boreholeDiameter}mm",
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "孔底坐标: (${resultBottomX}, ${resultBottomY})",
                                    fontSize = 13.sp,
                                    color = SecondaryGreen
                                )
                                Text(
                                    text = "水平投影: ${resultHorizontal}m | 垂直投影: ${resultVertical}m",
                                    fontSize = 13.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "📊 钻孔柱状图",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                BoreholeColumnChartFull(
                                    layers = layers,
                                    totalDepth = boreholeDepth.toDoubleOrNull() ?: 10.0,
                                    diameter = boreholeDiameter.toDoubleOrNull() ?: 75.0,
                                    dipAngle = dipAngle.toDoubleOrNull() ?: 90.0,
                                    horizontal = resultHorizontal?.toDoubleOrNull() ?: 0.0,
                                    vertical = resultVertical?.toDoubleOrNull() ?: 0.0
                                )
                            }
                        }
                        
                        if (layers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "📋 地层图例",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                layers.forEach { layer ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(layer.color)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${layer.name} (${layer.depth}m)",
                                            fontSize = 10.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoreholeColumnChart(
    layers: List<LayerData>,
    totalDepth: Double,
    diameter: Double
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val chartWidth = size.width
        val chartHeight = size.height
        val padding = 20f
        val barWidth = 40f
        val maxDepth = totalDepth.toFloat()
        
        if (layers.isNotEmpty()) {
            var currentDepth = 0f
            layers.forEach { layer ->
                val depthVal = layer.depth.toFloatOrNull() ?: 0f
                val barHeight = (depthVal / maxDepth * (chartHeight - padding * 2)).toFloat()
                val x = (chartWidth - barWidth) / 2
                val y = padding + chartHeight - padding - barHeight - currentDepth * (chartHeight - padding * 2) / maxDepth
                
                drawRect(
                    color = layer.color,
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )
                
                drawRect(
                    color = Color(0xFFE2E8F0),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    style = Stroke(width = 1f)
                )
                
                currentDepth += depthVal
            }
            
            drawLine(
                color = Color(0xFF94A3B8),
                start = Offset(padding, padding),
                end = Offset(padding, padding + chartHeight - padding * 2),
                strokeWidth = 1f
            )
            
            for (i in 0..4) {
                val y = padding + (chartHeight - padding * 2) * (1f - i / 4f)
                drawCircle(
                    color = Color(0xFF94A3B8),
                    radius = 2f,
                    center = Offset(padding - 4f, y)
                )
            }
        } else {
            drawLine(
                color = Color(0xFF94A3B8),
                start = Offset(padding, padding + chartHeight / 2),
                end = Offset(padding + chartWidth - padding, padding + chartHeight / 2),
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

@Composable
fun BoreholeColumnChartFull(
    layers: List<LayerData>,
    totalDepth: Double,
    diameter: Double,
    dipAngle: Double,
    horizontal: Double,
    vertical: Double
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val chartWidth = size.width
        val chartHeight = size.height
        val padding = 40f
        val barWidth = 60f
        val maxDepth = totalDepth.toFloat()
        
        if (layers.isNotEmpty()) {
            var currentDepth = 0f
            layers.forEach { layer ->
                val depthVal = layer.depth.toFloatOrNull() ?: 0f
                val barHeight = (depthVal / maxDepth * (chartHeight - padding * 2)).toFloat()
                val x = (chartWidth - barWidth) / 2
                val y = padding + chartHeight - padding - barHeight - currentDepth * (chartHeight - padding * 2) / maxDepth
                
                drawRect(
                    color = layer.color,
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )
                
                drawRect(
                    color = Color(0xFFE2E8F0),
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    style = Stroke(width = 1f)
                )
                
                currentDepth += depthVal
            }
            
            drawLine(
                color = Color(0xFF475569),
                start = Offset(padding, padding),
                end = Offset(padding, padding + chartHeight - padding * 2),
                strokeWidth = 2f
            )
            
            for (i in 0..5) {
                val y = padding + (chartHeight - padding * 2) * (1f - i / 5f)
                drawCircle(
                    color = Color(0xFF475569),
                    radius = 3f,
                    center = Offset(padding - 8f, y)
                )
            }
        } else {
            drawLine(
                color = Color(0xFF94A3B8),
                start = Offset(padding, padding + chartHeight / 2),
                end = Offset(padding + chartWidth - padding, padding + chartHeight / 2),
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
            )
        }
    }
}
