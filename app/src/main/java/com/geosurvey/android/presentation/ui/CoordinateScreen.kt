package com.geosurvey.android.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.presentation.theme.*
import com.geosurvey.android.presentation.ui.components.GlassCard
import com.geosurvey.android.presentation.viewmodel.CoordinateState
import com.geosurvey.android.presentation.viewmodel.CoordinateViewModel
import com.geosurvey.android.utils.CoordinateConverter
import com.geosurvey.android.utils.GaussProjection
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CoordinateScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication
    val viewModel = remember { CoordinateViewModel.getInstance(application) }
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var location by remember { mutableStateOf<Location?>(null) }
    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }

    // 投影参数对话框
    var showProjectionDialog by remember { mutableStateOf(false) }
    var tempZone by remember { mutableStateOf("") }
    var tempCentralMeridian by remember { mutableStateOf("") }

    // 转换方向选择
    enum class ConvertDirection {
        LATLON_TO_GAUSS,  // 经纬度→高斯投影
        GAUSS_TO_LATLON   // 高斯投影→经纬度
    }
    var convertDirection by remember { mutableStateOf(ConvertDirection.LATLON_TO_GAUSS) }

    // 高斯输入
    var gaussInputX by remember { mutableStateOf("") }
    var gaussInputY by remember { mutableStateOf("") }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    location = it
                    viewModel.updateLocation(it)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (fine == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2000
            ).apply {
                setMinUpdateIntervalMillis(1000)
                setMaxUpdateDelayMillis(5000)
            }.build()
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            } catch (e: Exception) { }
        }
    }

    // 应用投影参数
    fun applyProjectionParams() {
        val zone = tempZone.toIntOrNull()
        val cm = tempCentralMeridian.toDoubleOrNull()
        if (zone != null && cm != null) {
            viewModel.updateCustomZone(tempZone)
            viewModel.updateCustomCentralMeridian(tempCentralMeridian)
            viewModel.toggleUseCustomProjection()
            showProjectionDialog = false
            toastMessage = "✅ 已应用自定义参数"
            showToast = true
        } else {
            toastMessage = "⚠️ 请输入有效参数"
            showToast = true
        }
    }

    // 执行高斯转换
    fun performGaussConversion() {
        if (convertDirection == ConvertDirection.LATLON_TO_GAUSS) {
            // 经纬度→高斯投影
            val lat = state.inputLat.toDoubleOrNull()
            val lon = state.inputLon.toDoubleOrNull()
            if (lat == null || lon == null) {
                toastMessage = "⚠️ 请输入有效经纬度"
                showToast = true
                return
            }
            val gaussCoord = GaussProjection.blhToGauss(lat, lon)
            viewModel.updateGaussResult(gaussCoord)
            toastMessage = "✅ 转换完成"
            showToast = true
        } else {
            // 高斯投影→经纬度
            val x = gaussInputX.toDoubleOrNull()
            val y = gaussInputY.toDoubleOrNull()
            if (x == null || y == null) {
                toastMessage = "⚠️ 请输入有效高斯坐标"
                showToast = true
                return
            }
            try {
                val zone = state.customZone.toIntOrNull() ?: 18
                val coord = GaussProjection.gaussToBlh(x, y, zone)
                viewModel.updateWgs84Result(coord)
                toastMessage = "✅ 转换完成"
                showToast = true
            } catch (e: Exception) {
                toastMessage = "⚠️ 转换失败: ${e.message}"
                showToast = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "🌐 坐标转换",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ========== 当前坐标卡片 ==========
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📍 当前坐标",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (location != null) {
                Text(
                    text = "WGS84: ${CoordinateConverter.formatCoordinate(location!!.latitude, location!!.longitude)}",
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "CGCS2000: ${state.cgcs2000?.let { CoordinateConverter.formatCoordinate(it.latitude, it.longitude) } ?: "计算中..."}",
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "GCJ02: ${state.gcj02?.let { CoordinateConverter.formatCoordinate(it.latitude, it.longitude) } ?: "计算中..."}",
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A)
                )
                if (state.gaussCoord != null) {
                    Text(
                        text = "高斯投影: X=${String.format("%.2f", state.gaussCoord!!.x)}, Y=${String.format("%.2f", state.gaussCoord!!.y)}",
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "带号: ${state.gaussCoord!!.zone}, 中央子午线: ${String.format("%.1f", state.gaussCoord!!.centralMeridian)}°",
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A)
                    )
                }

                // ========== 投影参数设置（按钮方式） ==========
                Spacer(modifier = Modifier.height(12.dp))

                Divider(
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ 投影参数",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = if (state.useCustomProjection) "🔧 自定义" else "⚡ 自动",
                        fontSize = 12.sp,
                        color = if (state.useCustomProjection) PrimaryBlue else Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            tempZone = state.customZone
                            tempCentralMeridian = state.customCentralMeridian
                            showProjectionDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📐 设置参数")
                    }
                    if (state.useCustomProjection) {
                        Button(
                            onClick = {
                                viewModel.toggleUseCustomProjection()
                                toastMessage = "✅ 已切换为自动计算"
                                showToast = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentOrange
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("重置自动")
                        }
                    }
                }

                if (state.useCustomProjection) {
                    Text(
                        text = "当前: 带号=${state.customZone}, 中央子午线=${state.customCentralMeridian}°",
                        fontSize = 11.sp,
                        color = PrimaryBlue,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                Text(
                    text = "等待定位...",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ========== 手动输入转换 ==========
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "✏️ 手动输入转换",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentPurple
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 转换方向选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = convertDirection == ConvertDirection.LATLON_TO_GAUSS,
                    onClick = { convertDirection = ConvertDirection.LATLON_TO_GAUSS },
                    label = { Text("经纬→高斯", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = convertDirection == ConvertDirection.GAUSS_TO_LATLON,
                    onClick = { convertDirection = ConvertDirection.GAUSS_TO_LATLON },
                    label = { Text("高斯→经纬", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (convertDirection == ConvertDirection.LATLON_TO_GAUSS) {
                // 经纬度→高斯投影
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.inputLat,
                        onValueChange = { viewModel.updateInputLat(it) },
                        label = { Text("纬度") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("如: 27.0434", fontSize = 12.sp) }
                    )
                    OutlinedTextField(
                        value = state.inputLon,
                        onValueChange = { viewModel.updateInputLon(it) },
                        label = { Text("经度") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("如: 102.6660", fontSize = 12.sp) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.inputAlt,
                        onValueChange = { viewModel.updateInputAlt(it) },
                        label = { Text("海拔 (m)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("可选", fontSize = 12.sp) }
                    )
                    // 系统选择
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(CoordinateConverter.getSystemName(state.selectedSystem))
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            CoordinateConverter.CoordinateSystem.values().forEach { system ->
                                DropdownMenuItem(
                                    text = { Text(CoordinateConverter.getSystemName(system)) },
                                    onClick = {
                                        viewModel.selectSystem(system)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.convertInput(
                            state.inputLat,
                            state.inputLon,
                            state.inputAlt,
                            state.selectedSystem
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🔄 转换")
                }

                if (state.wgs84 != null && state.wgs84!!.latitude != 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "WGS84: ${CoordinateConverter.formatCoordinate(state.wgs84!!.latitude, state.wgs84!!.longitude)}",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                }
            } else {
                // 高斯投影→经纬度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = gaussInputX,
                        onValueChange = { gaussInputX = it },
                        label = { Text("X (北坐标)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("如: 2999122.39", fontSize = 12.sp) }
                    )
                    OutlinedTextField(
                        value = gaussInputY,
                        onValueChange = { gaussInputY = it },
                        label = { Text("Y (东坐标)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("如: 908592.03", fontSize = 12.sp) }
                    )
                }

                // 带号输入
                OutlinedTextField(
                    value = state.customZone,
                    onValueChange = { viewModel.updateCustomZone(it) },
                    label = { Text("带号") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    placeholder = { Text("如: 18", fontSize = 12.sp) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { performGaussConversion() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPurple
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🔄 转换")
                }

                // 显示转换结果
                if (state.wgs84 != null && state.wgs84!!.latitude != 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "经纬度: ${CoordinateConverter.formatCoordinate(state.wgs84!!.latitude, state.wgs84!!.longitude)}",
                        fontSize = 12.sp,
                        color = AccentPurple
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ========== 记录信息 ==========
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📝 记录信息",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.locationName,
                onValueChange = { viewModel.updateLocationName(it) },
                label = { Text("📍 地点名称") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                placeholder = { Text("如：红岩村、测试点A", fontSize = 12.sp) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.note,
                onValueChange = { viewModel.updateNote(it) },
                label = { Text("📝 备注") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(8.dp),
                maxLines = 3,
                placeholder = { Text("记录岩性、产状、地质描述等", fontSize = 12.sp) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "💡 地点名称和备注会随坐标一起保存",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.saveRecord() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = state.wgs84 != null
                ) {
                    Text("💾 保存")
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.deleteAllRecords()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = state.recordCount > 0
                ) {
                    Text("🗑️ 删除")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ========== 历史记录 ==========
        Text(
            text = "📋 历史记录 (${state.recordCount})",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无坐标记录",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.records) { record ->
                    CoordinateRecordItem(record)
                }
            }
        }
    }

    // ========== 投影参数设置对话框 ==========
    if (showProjectionDialog) {
        AlertDialog(
            onDismissRequest = { showProjectionDialog = false },
            title = { Text("📐 设置投影参数") },
            text = {
                Column {
                    Text(
                        text = "输入自定义带号和中央子午线",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempZone,
                        onValueChange = { tempZone = it },
                        label = { Text("带号") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("如: 18", fontSize = 12.sp) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempCentralMeridian,
                        onValueChange = { tempCentralMeridian = it },
                        label = { Text("中央子午线 (°)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("如: 105", fontSize = 12.sp) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 6度带: 带号×6-3 = 中央子午线",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "💡 3度带: 带号×3 = 中央子午线",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { applyProjectionParams() }) {
                    Text("应用", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProjectionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showToast) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2500)
            showToast = false
        }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = when {
                toastMessage.contains("✅") -> SecondaryGreen
                toastMessage.contains("⚠️") -> AccentOrange
                else -> SecondaryGreen
            }
        ) {
            Text(toastMessage, color = Color.White)
        }
    }
}

@Composable
fun CoordinateRecordItem(record: com.geosurvey.android.data.model.CoordinateRecord) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "📍 ${String.format("%.6f", record.latitude)}, ${String.format("%.6f", record.longitude)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "系统: ${record.system} | 海拔: ${record.altitude?.let { String.format("%.1f", it) } ?: "--"}m",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
                if (record.gaussX != null && record.gaussY != null) {
                    Text(
                        text = "高斯: X=${String.format("%.2f", record.gaussX)}, Y=${String.format("%.2f", record.gaussY)}",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                }
                if (record.locationName.isNotEmpty()) {
                    Text(
                        text = "📍 ${record.locationName}",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                if (record.note.isNotEmpty()) {
                    Text(
                        text = "📝 ${record.note}",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
            Text(
                text = formatDateTime(record.timestamp),
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
