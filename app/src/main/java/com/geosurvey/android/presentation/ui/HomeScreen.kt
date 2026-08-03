package com.geosurvey.android.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.presentation.theme.*
import com.geosurvey.android.presentation.ui.components.GlassCard
import com.geosurvey.android.presentation.viewmodel.LocationState
import com.geosurvey.android.presentation.viewmodel.LocationViewModel
import com.geosurvey.android.presentation.viewmodel.TrackViewModel

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication

    val locationViewModel: LocationViewModel = viewModel()
    val trackViewModel = remember { TrackViewModel.getInstance(application) }

    LaunchedEffect(Unit) {
        locationViewModel.init(context)
    }

    val state by locationViewModel.state.collectAsState()
    val isRecording by trackViewModel.isRecording.collectAsState()

    var showSatelliteDetail by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val altitudeHistory = remember { mutableStateListOf<Float>() }
    val speedHistory = remember { mutableStateListOf<Float>() }
    val snrHistory = remember { mutableStateListOf<Float>() }

    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // ⭐ 第73行已修复：使用 .toFloat() 转换
    LaunchedEffect(state.location) {
        state.location?.let { location ->
            val alt = location.altitude?.toFloat() ?: 0f
            val speed = location.speed?.let { it * 3.6 } ?: 0f
            val snr = state.averageSnr.toFloat()  // ⭐ 第73行修复
            
            altitudeHistory.add(alt)
            speedHistory.add(speed)
            snrHistory.add(snr)
            
            if (altitudeHistory.size > 50) {
                altitudeHistory.removeAt(0)
                speedHistory.removeAt(0)
                snrHistory.removeAt(0)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocation && coarseLocation) {
            locationViewModel.startLocation()
        }
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (fine == PackageManager.PERMISSION_GRANTED &&
            coarse == PackageManager.PERMISSION_GRANTED
        ) {
            locationViewModel.startLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            )
        }
    }

    LaunchedEffect(state.location) {
        state.location?.let { location ->
            if (isRecording) {
                trackViewModel.addTrackPoint(location)
            }
        }
    }

    // 垂直滚动
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // 标题行 - 添加关于按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🏔️ 地质勘查工具箱",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(top = 8.dp)
            )
            // 关于按钮
            TextButton(
                onClick = { showAboutDialog = true },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("📋", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "GeoSurvey Toolbox v1.0.0",
            fontSize = 14.sp,
            color = Color(0xFF475569)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 定位信息卡片
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            LocationInfoContent(state)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 卫星状态卡片
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    if (state.satelliteCount > 0) {
                        showSatelliteDetail = true 
                    }
                }
        ) {
            SatelliteStatusContent(state)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 轨迹记录状态
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isRecording) "🟢 轨迹记录中" else "⏸️ 轨迹未记录",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isRecording) SecondaryGreen else Color(0xFF94A3B8)
                )
                if (isRecording) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "●",
                        fontSize = 12.sp,
                        color = SecondaryGreen,
                        modifier = Modifier.scale(pulse)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { locationViewModel.startLocation() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("开始定位")
            }

            Button(
                onClick = { locationViewModel.stopLocation() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("停止定位")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 传感器实时曲线
        Text(
            text = "📊 传感器实时曲线",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        SensorChart(
            title = "海拔",
            data = altitudeHistory,
            color = Color(0xFF0EA5E9),
            unit = "m",
            maxValue = 2000f
        )

        Spacer(modifier = Modifier.height(6.dp))

        SensorChart(
            title = "速度",
            data = speedHistory,
            color = Color(0xFF10B981),
            unit = "km/h",
            maxValue = 50f
        )

        Spacer(modifier = Modifier.height(6.dp))

        SensorChart(
            title = "信噪比",
            data = snrHistory,
            color = Color(0xFF8B5CF6),
            unit = "dBHz",
            maxValue = 50f
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "🔧 开发中... 请等待后续版本",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )
        
        // 底部留白
        Spacer(modifier = Modifier.height(80.dp))
    }

    // 卫星详情对话框
    if (showSatelliteDetail) {
        Dialog(
            onDismissRequest = { showSatelliteDetail = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A)
            ) {
                SatelliteFullScreen(
                    state = state,
                    onDismiss = { showSatelliteDetail = false }
                )
            }
        }
    }

    // 关于对话框
    if (showAboutDialog) {
        Dialog(
            onDismissRequest = { showAboutDialog = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC)
            ) {
                AboutScreen(onBack = { showAboutDialog = false })
            }
        }
    }
}

@Composable
fun LocationInfoContent(state: LocationState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "📍 定位信息",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryBlue
        )
        Text(
            text = when {
                state.isSearching -> "🔍 搜索中..."
                state.location != null -> "● 定位中"
                else -> "○ 已停止"
            },
            fontSize = 14.sp,
            color = when {
                state.isSearching -> AccentOrange
                state.location != null -> SecondaryGreen
                else -> Color(0xFF94A3B8)
            }
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (state.isSearching) {
        Text(
            text = "⏳ 搜索GPS信号... (${state.searchTime / 1000}s)",
            fontSize = 12.sp,
            color = AccentOrange
        )
    }

    if (state.errorMessage.isNotEmpty()) {
        Text(
            text = "⚠️ ${state.errorMessage}",
            fontSize = 12.sp,
            color = ErrorRed
        )
    }

    val location = state.location
    if (location != null) {
        Column {
            Text(
                text = "纬度: ${String.format("%.6f", location.latitude)}",
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "经度: ${String.format("%.6f", location.longitude)}",
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "海拔: ${location.altitude?.let { String.format("%.1f", it) } ?: "--"} m",
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
            val speedKmh = location.speed?.let { it * 3.6 } ?: 0.0
            val displaySpeed = if (speedKmh < 0.5) 0.0 else speedKmh
            Text(
                text = "速度: ${String.format("%.1f", displaySpeed)} km/h",
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "精度: ±${location.accuracy?.let { String.format("%.1f", it) } ?: "--"} m",
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
        }
    } else {
        Text(
            text = when {
                state.isActive && state.isSearching -> "🔍 正在搜索GPS信号..."
                state.isActive -> "等待定位..."
                else -> "点击「开始定位」获取位置"
            },
            fontSize = 14.sp,
            color = when {
                state.isSearching -> AccentOrange
                state.isActive -> Color(0xFF94A3B8)
                else -> Color(0xFF94A3B8)
            }
        )
    }
}

@Composable
fun SatelliteStatusContent(state: LocationState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "🛰️ 卫星状态",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentPurple
        )
        Text(
            text = if (state.satelliteCount > 0) "点击查看全屏 →" else state.qualityText,
            fontSize = 12.sp,
            color = if (state.satelliteCount > 0) PrimaryBlue else Color(0xFF94A3B8)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("总数", fontSize = 11.sp, color = Color(0xFF475569))
            Text("${state.satelliteCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🟢 GPS", fontSize = 11.sp, color = Color(0xFF475569))
            Text("${state.gpsCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔵 GLONASS", fontSize = 11.sp, color = Color(0xFF475569))
            Text("${state.glonassCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔴 北斗", fontSize = 11.sp, color = Color(0xFF475569))
            Text("${state.beidouCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🟡 Galileo", fontSize = 11.sp, color = Color(0xFF475569))
            Text("${state.galileoCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "信噪比: ${String.format("%.1f", state.averageSnr)} dBHz | 可用: ${state.usedSatelliteCount}",
        fontSize = 11.sp,
        color = Color(0xFF94A3B8)
    )
}
