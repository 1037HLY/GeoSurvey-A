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
import com.geosurvey.android.presentation.viewmodel.AttitudeViewModel
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
fun AttitudeScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication
    val viewModel = remember { AttitudeViewModel.getInstance(application) }
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var location by remember { mutableStateOf<Location?>(null) }

    var noteText by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }

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
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (fine == PackageManager.PERMISSION_GRANTED &&
            coarse == PackageManager.PERMISSION_GRANTED
        ) {
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

    fun handleSaveRecord() {
        if (!state.isMeasuring) {
            toastMessage = "请先点击「开始测量」"
            showToast = true
            return
        }
        if (location == null) {
            toastMessage = "正在获取定位，请稍后..."
            showToast = true
            return
        }
        viewModel.saveRecord(noteText)
        noteText = ""
        toastMessage = "✅ 产状记录已保存！"
        showToast = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🔬 地质产状测量",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 实时测量卡片
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📐 实时产状",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentPurple
                )
                Text(
                    text = if (state.isMeasuring) "● 测量中" else "○ 已停止",
                    fontSize = 14.sp,
                    color = if (state.isMeasuring) SecondaryGreen else Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("倾向", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        String.format("%.1f", state.dipDirection) + "°",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("倾角", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        String.format("%.1f", state.dipAngle) + "°",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryGreen
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("走向", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        String.format("%.1f", state.strike) + "°",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (location != null) {
                Text(
                    text = "📍 ${String.format("%.6f", location!!.latitude)}, ${String.format("%.6f", location!!.longitude)}",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
                Text(
                    text = "海拔: ${location!!.altitude?.let { String.format("%.1f", it) } ?: "--"}m | 精度: ±${location!!.accuracy?.let { String.format("%.1f", it) } ?: "--"}m",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
            } else {
                Text(
                    text = "⏳ 正在获取定位...",
                    fontSize = 12.sp,
                    color = AccentOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (state.isMeasuring) {
                        viewModel.stopMeasuring()
                    } else {
                        viewModel.startMeasuring()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isMeasuring) ErrorRed else PrimaryBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (state.isMeasuring) "停止测量" else "开始测量")
            }

            Button(
                onClick = { handleSaveRecord() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryGreen
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = state.isMeasuring
            ) {
                Text("📝 记录")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("备注 (可选)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            maxLines = 2
        )

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
                    Text("记录数", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("${state.recordCount}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("日期", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("${state.availableDates.size}天", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "📋 历史记录",
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
                    text = "暂无产状记录\n点击「开始测量」并「记录」",
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
                    AttitudeRecordItem(record)
                }
            }
        }

        if (state.recordCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.deleteAllRecords()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🗑️ 删除所有记录")
            }
        }
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
                else -> AccentOrange
            }
        ) {
            Text(toastMessage, color = Color.White)
        }
    }
}

@Composable
fun AttitudeRecordItem(record: com.geosurvey.android.data.model.AttitudeRecord) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "倾向 ${String.format("%.1f", record.dipDirection)}° | 倾角 ${String.format("%.1f", record.dipAngle)}° | 走向 ${String.format("%.1f", record.strike)}°",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F172A)
            )
            Text(
                text = formatDate(record.timestamp),
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
        Text(
            text = "📍 ${String.format("%.6f", record.latitude)}, ${String.format("%.6f", record.longitude)}",
            fontSize = 11.sp,
            color = Color(0xFF475569)
        )
        if (record.note.isNotEmpty()) {
            Text(
                text = "📝 ${record.note}",
                fontSize = 11.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
