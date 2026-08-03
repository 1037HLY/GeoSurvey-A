package com.geosurvey.android.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.data.model.DrillSample
import com.geosurvey.android.data.model.NormalSample
import com.geosurvey.android.presentation.theme.*
import com.geosurvey.android.presentation.ui.components.GlassCard
import com.geosurvey.android.presentation.viewmodel.SampleViewModel
import com.geosurvey.android.utils.CoordinateConverter
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
fun SampleInputScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication
    val viewModel = remember { SampleViewModel.getInstance(application) }
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var location by remember { mutableStateOf<Location?>(null) }
    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }

    // 样本类型选择: 0-普通样本, 1-钻孔样本
    var sampleType by remember { mutableStateOf(0) }

    // 普通样本字段
    var normalType by remember { mutableStateOf("") }
    var normalNumber by remember { mutableStateOf("") }
    var normalName by remember { mutableStateOf("") }
    var normalWeight by remember { mutableStateOf("") }
    var normalDesc by remember { mutableStateOf("") }

    // 钻孔样本字段
    var drillNumber by remember { mutableStateOf("") }
    var drillFromDepth by remember { mutableStateOf("") }
    var drillToDepth by remember { mutableStateOf("") }
    var drillSampleLength by remember { mutableStateOf("") }
    var drillCoreLength by remember { mutableStateOf("") }
    var drillRecoveryRate by remember { mutableStateOf("") }
    var drillWeight by remember { mutableStateOf("") }
    var drillName by remember { mutableStateOf("") }
    var drillCoreDiameter by remember { mutableStateOf("") }
    var drillDesc by remember { mutableStateOf("") }

    // 位置回调
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    location = it
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

    fun saveNormalSample() {
        if (normalNumber.isEmpty() || normalName.isEmpty()) {
            toastMessage = "请填写编号和名称"
            showToast = true
            return
        }
        coroutineScope.launch {
            val sample = NormalSample(
                sampleType = normalType,
                sampleNumber = normalNumber,
                sampleName = normalName,
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0,
                altitude = location?.altitude,
                weight = normalWeight,
                description = normalDesc,
                timestamp = System.currentTimeMillis()
            )
            viewModel.insertNormalSample(sample)
            toastMessage = "✅ 普通样本已保存"
            showToast = true
            normalNumber = ""
            normalName = ""
            normalType = ""
            normalWeight = ""
            normalDesc = ""
        }
    }

    fun saveDrillSample() {
        if (drillNumber.isEmpty() || drillName.isEmpty()) {
            toastMessage = "请填写编号和名称"
            showToast = true
            return
        }
        coroutineScope.launch {
            val sample = DrillSample(
                sampleNumber = drillNumber,
                fromDepth = drillFromDepth,
                toDepth = drillToDepth,
                sampleLength = drillSampleLength,
                coreLength = drillCoreLength,
                recoveryRate = drillRecoveryRate,
                weight = drillWeight,
                sampleName = drillName,
                coreDiameter = drillCoreDiameter,
                description = drillDesc,
                timestamp = System.currentTimeMillis()
            )
            viewModel.insertDrillSample(sample)
            toastMessage = "✅ 钻孔样本已保存"
            showToast = true
            drillNumber = ""
            drillFromDepth = ""
            drillToDepth = ""
            drillSampleLength = ""
            drillCoreLength = ""
            drillRecoveryRate = ""
            drillWeight = ""
            drillName = ""
            drillCoreDiameter = ""
            drillDesc = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "📋 样本信息录入",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 位置信息
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📍 当前位置",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue
            )
            if (location != null) {
                Text(
                    text = "纬度: ${String.format("%.6f", location!!.latitude)}",
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "经度: ${String.format("%.6f", location!!.longitude)}",
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "海拔: ${location!!.altitude?.let { String.format("%.1f", it) } ?: "--"}m",
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A)
                )
            } else {
                Text(
                    text = "等待定位...",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 样本类型切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { sampleType = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sampleType == 0) PrimaryBlue else Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "普通样本",
                    color = if (sampleType == 0) Color.White else Color(0xFF475569)
                )
            }
            Button(
                onClick = { sampleType = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sampleType == 1) PrimaryBlue else Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "钻孔样本",
                    color = if (sampleType == 1) Color.White else Color(0xFF475569)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (sampleType == 0) {
            // 普通样本录入
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📝 普通样本",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = normalType,
                    onValueChange = { normalType = it },
                    label = { Text("样本类型") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    placeholder = { Text("如: 岩石、土壤、矿物", fontSize = 12.sp) }
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = normalNumber,
                    onValueChange = { normalNumber = it },
                    label = { Text("样本编号 *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    placeholder = { Text("如: S001", fontSize = 12.sp) }
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = normalName,
                    onValueChange = { normalName = it },
                    label = { Text("样本名称 *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    placeholder = { Text("如: 花岗岩样品", fontSize = 12.sp) }
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = normalWeight,
                    onValueChange = { normalWeight = it },
                    label = { Text("重量 (kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    placeholder = { Text("如: 2.5", fontSize = 12.sp) }
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = normalDesc,
                    onValueChange = { normalDesc = it },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 3,
                    placeholder = { Text("样本描述、岩性特征等", fontSize = 12.sp) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { saveNormalSample() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("💾 保存普通样本")
                }
            }
        } else {
            // 钻孔样本录入
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🪨 钻孔样本",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = drillNumber,
                    onValueChange = { drillNumber = it },
                    label = { Text("样本编号 *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    placeholder = { Text("如: ZK001-1", fontSize = 12.sp) }
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = drillFromDepth,
                        onValueChange = { drillFromDepth = it },
                        label = { Text("井深(自)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("m", fontSize = 10.sp) }
                    )
                    OutlinedTextField(
                        value = drillToDepth,
                        onValueChange = { drillToDepth = it },
                        label = { Text("井深(至)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("m", fontSize = 10.sp) }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = drillSampleLength,
                        onValueChange = { drillSampleLength = it },
                        label = { Text("样长(m)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = drillCoreLength,
                        onValueChange = { drillCoreLength = it },
                        label = { Text("岩心长(m)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = drillRecoveryRate,
                        onValueChange = { drillRecoveryRate = it },
                        label = { Text("采取率(%)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("如: 95", fontSize = 10.sp) }
                    )
                    OutlinedTextField(
                        value = drillWeight,
                        onValueChange = { drillWeight = it },
                        label = { Text("重量(kg)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = drillName,
                        onValueChange = { drillName = it },
                        label = { Text("样本名称 *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("如: 岩心样品", fontSize = 10.sp) }
                    )
                    OutlinedTextField(
                        value = drillCoreDiameter,
                        onValueChange = { drillCoreDiameter = it },
                        label = { Text("岩心直径(mm)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("如: 75", fontSize = 10.sp) }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = drillDesc,
                    onValueChange = { drillDesc = it },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 3,
                    placeholder = { Text("岩性、颜色、结构等", fontSize = 12.sp) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { saveDrillSample() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("💾 保存钻孔样本")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 历史记录
        Text(
            text = "📋 样本历史记录",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.normalSamples.isEmpty() && state.drillSamples.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无样本记录",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.normalSamples) { sample ->
                    NormalSampleItem(sample)
                }
                items(state.drillSamples) { sample ->
                    DrillSampleItem(sample)
                }
            }
        }

        // 删除按钮
        if (state.normalSamples.isNotEmpty() || state.drillSamples.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.normalSamples.isNotEmpty()) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.deleteAllNormalSamples()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🗑️ 删除普通")
                    }
                }
                if (state.drillSamples.isNotEmpty()) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.deleteAllDrillSamples()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🗑️ 删除钻孔")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
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
fun NormalSampleItem(sample: NormalSample) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📦 ${sample.sampleNumber} - ${sample.sampleName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = formatTime(sample.timestamp),
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Text(
                text = "类型: ${sample.sampleType} | 重量: ${sample.weight}kg",
                fontSize = 11.sp,
                color = Color(0xFF475569)
            )
            Text(
                text = "📍 ${String.format("%.6f", sample.latitude)}, ${String.format("%.6f", sample.longitude)}",
                fontSize = 11.sp,
                color = Color(0xFF475569)
            )
            if (sample.description.isNotEmpty()) {
                Text(
                    text = "📝 ${sample.description}",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
fun DrillSampleItem(sample: DrillSample) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🪨 ${sample.sampleNumber} - ${sample.sampleName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = formatTime(sample.timestamp),
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Text(
                text = "井深: ${sample.fromDepth}-${sample.toDepth}m | 样长: ${sample.sampleLength}m",
                fontSize = 11.sp,
                color = Color(0xFF475569)
            )
            Text(
                text = "岩心长: ${sample.coreLength}m | 采取率: ${sample.recoveryRate}% | 重量: ${sample.weight}kg",
                fontSize = 11.sp,
                color = Color(0xFF475569)
            )
            if (sample.description.isNotEmpty()) {
                Text(
                    text = "📝 ${sample.description}",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
