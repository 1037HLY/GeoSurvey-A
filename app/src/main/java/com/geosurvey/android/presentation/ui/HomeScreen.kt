package com.geosurvey.android.presentation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.presentation.viewmodel.LocationState
import com.geosurvey.android.presentation.viewmodel.LocationViewModel
import com.geosurvey.android.presentation.viewmodel.TrackViewModel

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication

    val locationViewModel: LocationViewModel = viewModel(
        factory = LocationViewModelFactory(context)
    )

    // ⭐ 使用单例获取TrackViewModel
    val trackViewModel = remember { TrackViewModel.getInstance(application) }

    val state by locationViewModel.state.collectAsState()
    val isRecording by trackViewModel.isRecording.collectAsState()

    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocation && coarseLocation) {
            locationViewModel.startLocation()
        }
    }

    // 检查权限
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

    // 监听位置更新，自动保存到轨迹
    LaunchedEffect(state.location) {
        state.location?.let { location ->
            if (isRecording) {
                trackViewModel.addTrackPoint(location)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "🏔️ 地质勘查工具箱",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "GeoSurvey Toolbox v1.0.0",
            fontSize = 14.sp,
            color = Color(0xFF475569)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LocationInfoCard(state)

        Spacer(modifier = Modifier.height(16.dp))

        SatelliteStatusCard(state)

        Spacer(modifier = Modifier.height(16.dp))

        // 轨迹记录状态指示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isRecording)
                    Color(0xFF10B981).copy(alpha = 0.15f)
                else
                    Color(0xFF94A3B8).copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isRecording) "🟢 轨迹记录中" else "⏸️ 轨迹未记录",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isRecording) Color(0xFF10B981) else Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { locationViewModel.startLocation() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0EA5E9)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("开始定位")
            }

            Button(
                onClick = { locationViewModel.stopLocation() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("停止定位")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "🔧 开发中... 请等待后续版本",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun LocationInfoCard(state: LocationState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📍 定位信息",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0EA5E9)
                )
                Text(
                    text = if (state.isActive) "● 定位中" else "○ 已停止",
                    fontSize = 14.sp,
                    color = if (state.isActive) Color(0xFF10B981) else Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    text = if (state.isActive) "🔍 搜索GPS信号中..." else "等待定位",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun SatelliteStatusCard(state: LocationState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🛰️ 卫星状态",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8B5CF6)
                )
                Text(
                    text = state.qualityText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = state.qualityColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "信噪比: ${String.format("%.1f", state.averageSnr)} dBHz | 可用: ${state.usedSatelliteCount}",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

// LocationViewModel Factory
class LocationViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocationViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
