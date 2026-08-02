package com.geosurvey.android.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.presentation.viewmodel.NavigationState
import com.geosurvey.android.presentation.viewmodel.NavigationViewModel
import com.geosurvey.android.presentation.viewmodel.TrackViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay

@Composable
fun NavigationScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication

    val navViewModel = remember { NavigationViewModel.getInstance(application) }
    val trackViewModel = remember { TrackViewModel.getInstance(application) }
    val state by navViewModel.state.collectAsState()
    val trackPoints by trackViewModel.trackPoints.collectAsState()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var location by remember { mutableStateOf<Location?>(null) }

    // 动画旋转角度
    val infiniteTransition = rememberInfiniteTransition()
    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // 加载轨迹点作为导航目标
    LaunchedEffect(trackPoints) {
        if (trackPoints.isNotEmpty()) {
            navViewModel.setTrackPoints(trackPoints)
        }
    }

    // 定位回调
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    location = it
                    navViewModel.updateLocation(it)
                }
            }
        }
    }

    // 启动定位
    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (fine == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000
            ).apply {
                setMinUpdateIntervalMillis(500)
                setMaxUpdateDelayMillis(2000)
            }.build()
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    // 清理
    DisposableEffect(Unit) {
        onDispose {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "🧭 轨迹导航",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 导航状态卡片
        NavigationStatusCard(state)

        Spacer(modifier = Modifier.height(12.dp))

        // 罗盘指示器
        CompassIndicator(state)

        Spacer(modifier = Modifier.height(12.dp))

        // 目标列表
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Text(
                    text = "🎯 选择导航目标",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (trackPoints.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无轨迹数据\n请先在「轨迹」页面记录轨迹",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(trackPoints.take(20)) { point ->
                            val index = trackPoints.indexOf(point) + 1
                            TargetItem(
                                name = "轨迹点 $index",
                                point = point,
                                isSelected = state.targetName == "轨迹点 $index",
                                onSelect = {
                                    navViewModel.selectTarget(index - 1)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.isNavigating) {
                Button(
                    onClick = { navViewModel.stopNavigation() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("⏹️ 停止导航")
                }
            }

            if (state.isNavigating) {
                Button(
                    onClick = {
                        if (navViewModel.getNextTarget()) {
                            // 切换到下一个目标
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0EA5E9)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = state.targetIndex < trackPoints.size - 1
                ) {
                    Text("下一个 →")
                }
            }
        }

        // 提示信息
        if (state.isNavigating && state.isOffTrack) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "⚠️ 偏离轨迹 ${state.offTrackDistance.toInt()} 米，请调整方向！",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NavigationStatusCard(state: NavigationState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isNavigating)
                Color(0xFF0EA5E9).copy(alpha = 0.1f)
            else
                Color(0xFFF1F5F9)
        ),
        shape = RoundedCornerShape(16.dp),
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
                    text = if (state.isNavigating) "🧭 导航中" else "⏸️ 未导航",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.isNavigating) Color(0xFF0EA5E9) else Color(0xFF94A3B8)
                )
                if (state.isNavigating) {
                    Text(
                        text = state.targetName,
                        fontSize = 14.sp,
                        color = Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isNavigating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("距离", fontSize = 11.sp, color = Color(0xFF475569))
                        Text(
                            "${state.distance.toInt()} m",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.distance < 50) Color(0xFF10B981) else Color(0xFF0F172A)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("方向", fontSize = 11.sp, color = Color(0xFF475569))
                        Text(
                            state.directionDescription,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0EA5E9)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("进度", fontSize = 11.sp, color = Color(0xFF475569))
                        Text(
                            "${state.progress.toInt()}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5CF6)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 进度条
                LinearProgressIndicator(
                    progress = state.progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF0EA5E9),
                    trackColor = Color(0xFFE2E8F0)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.guidanceText,
                    fontSize = 13.sp,
                    color = if (state.isOffTrack) Color(0xFFEF4444) else Color(0xFF475569)
                )
            } else {
                Text(
                    text = "选择下方的轨迹点作为导航目标",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun CompassIndicator(state: NavigationState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (state.isNavigating && state.targetLocation != null) {
                // 方位指示器
                val targetDirection = state.bearingToTarget
                val isOffTrack = state.isOffTrack

                // 外圈
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    // 方向箭头
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(targetDirection)
                    ) {
                        // 绘制三角形箭头
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val arrowSize = size.width * 0.3f

                        // 绘制箭头
                        drawLine(
                            color = if (isOffTrack) Color(0xFFEF4444) else Color(0xFF0EA5E9),
                            start = androidx.compose.ui.geometry.Offset(
                                centerX,
                                centerY - arrowSize
                            ),
                            end = androidx.compose.ui.geometry.Offset(
                                centerX,
                                centerY + arrowSize
                            ),
                            strokeWidth = 4f
                        )
                        // 箭头头部
                        drawLine(
                            color = if (isOffTrack) Color(0xFFEF4444) else Color(0xFF0EA5E9),
                            start = androidx.compose.ui.geometry.Offset(
                                centerX - arrowSize * 0.5f,
                                centerY - arrowSize * 0.3f
                            ),
                            end = androidx.compose.ui.geometry.Offset(
                                centerX,
                                centerY - arrowSize
                            ),
                            strokeWidth = 4f
                        )
                        drawLine(
                            color = if (isOffTrack) Color(0xFFEF4444) else Color(0xFF0EA5E9),
                            start = androidx.compose.ui.geometry.Offset(
                                centerX + arrowSize * 0.5f,
                                centerY - arrowSize * 0.3f
                            ),
                            end = androidx.compose.ui.geometry.Offset(
                                centerX,
                                centerY - arrowSize
                            ),
                            strokeWidth = 4f
                        )
                    }

                    // 中心点
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(if (isOffTrack) Color(0xFFEF4444) else Color(0xFF0EA5E9))
                    )
                }

                // 距离显示
                Text(
                    text = "${state.distance.toInt()}m",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.distance < 50) Color(0xFF10B981) else Color(0xFF0F172A),
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                Text(
                    text = "🧭",
                    fontSize = 48.sp
                )
            }
        }
    }
}

@Composable
fun TargetItem(
    name: String,
    point: com.geosurvey.android.data.model.TrackPoint,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                Color(0xFF0EA5E9).copy(alpha = 0.15f)
            else
                Color.White.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📍 $name",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color(0xFF0EA5E9) else Color(0xFF0F172A)
                )
                Text(
                    text = "${String.format("%.6f", point.latitude)}, ${String.format("%.6f", point.longitude)}",
                    fontSize = 10.sp,
                    color = Color(0xFF475569)
                )
            }
            if (isSelected) {
                Text(
                    text = "✅",
                    fontSize = 16.sp
                )
            }
        }
    }
}

// 扩展函数
fun Modifier.clickable(onClick: () -> Unit): Modifier {
    return androidx.compose.foundation.clickable(onClick = onClick)
}
