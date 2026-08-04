package com.geosurvey.android.presentation.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.data.model.TrackPoint
import com.geosurvey.android.domain.service.TrackingService
import com.geosurvey.android.presentation.theme.*
import com.geosurvey.android.presentation.ui.components.GlassCard
import com.geosurvey.android.presentation.viewmodel.TrackViewModel
import com.geosurvey.android.utils.TrackExportHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrackScreen(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication
    val coroutineScope = rememberCoroutineScope()

    val viewModel = remember { TrackViewModel.getInstance(application) }

    val isRecording by viewModel.isRecording.collectAsState()
    val trackPoints by viewModel.trackPoints.collectAsState()
    val pointCount by viewModel.pointCount.collectAsState()
    val availableDates by viewModel.availableDates.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val filteredPoints by viewModel.filteredPoints.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    var showFullTrackDetail by remember { mutableStateOf(false) }

    val displayPoints = if (selectedDate != null) filteredPoints else trackPoints

    fun exportTracks(format: String) {
        val helper = TrackExportHelper(context)
        val points = if (selectedDate != null) filteredPoints else trackPoints
        if (points.isEmpty()) {
            Toast.makeText(context, "没有轨迹点可导出", Toast.LENGTH_SHORT).show()
            return
        }

        val filename = if (selectedDate != null) "track_$selectedDate" else "all_tracks"
        val file = if (format == "GPX") {
            helper.exportToGPX(points, filename)
        } else {
            helper.exportToKML(points, filename)
        }

        if (file != null) {
            Toast.makeText(context, "导出成功: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
        }
        showExportDialog = false
    }

    fun deleteSelectedDate() {
        selectedDate?.let { date ->
            coroutineScope.launch {
                viewModel.deleteTrackPointsByDate(date)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛣️ 轨迹记录",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (trackPoints.isNotEmpty()) {
                    IconButton(
                        onClick = { navController?.navigate("navigation") }
                    ) {
                        Text("🧭", fontSize = 20.sp)
                    }
                    IconButton(
                        onClick = { showExportDialog = true }
                    ) {
                        Text("📤", fontSize = 20.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("轨迹点", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("$pointCount", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("状态", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        if (isRecording) "● 记录中" else "○ 已停止",
                        color = if (isRecording) SecondaryGreen else Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("日期", fontSize = 12.sp, color = Color(0xFF475569))
                    Text("${availableDates.size}天", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 轨迹预览卡片
        if (trackPoints.isNotEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFullTrackDetail = true }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🗺️ 轨迹预览",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isRecording) "● 记录中" else "○ 已停止",
                                fontSize = 11.sp,
                                color = if (isRecording) SecondaryGreen else Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${trackPoints.size}点",
                                fontSize = 11.sp,
                                color = Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "点击放大 →",
                                fontSize = 10.sp,
                                color = PrimaryBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TrackMapProjection(
                        points = trackPoints,
                        height = 120
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (availableDates.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedDate ?: "📅 选择日期")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("📋 全部") },
                            onClick = {
                                viewModel.clearDateFilter()
                                expanded = false
                            }
                        )
                        availableDates.forEach { date ->
                            DropdownMenuItem(
                                text = { Text("📅 $date") },
                                onClick = {
                                    viewModel.filterByDate(date)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (!isRecording) {
                        val intent = Intent(context, TrackingService::class.java)
                        context.startForegroundService(intent)
                        viewModel.startRecording()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) SecondaryGreen else PrimaryBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isRecording) "● 记录中" else "开始记录")
            }

            Button(
                onClick = {
                    if (isRecording) {
                        val intent = Intent(context, TrackingService::class.java)
                        context.stopService(intent)
                        viewModel.stopRecording()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) ErrorRed else Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = isRecording
            ) {
                Text("停止")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (pointCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedDate != null) {
                    Button(
                        onClick = { deleteSelectedDate() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🗑️ 删除当天")
                    }
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.deleteAllTrackPoints()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🗑️ 删除全部")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (selectedDate != null) "📋 $selectedDate 轨迹" else "📋 全部轨迹",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            if (selectedDate != null) {
                TextButton(onClick = { viewModel.clearDateFilter() }) {
                    Text("显示全部", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (displayPoints.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedDate != null) "该日期没有轨迹数据" else "暂无轨迹数据\n点击「开始记录」开始采集",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(displayPoints) { point ->
                    TrackPointItem(point)
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("📤 导出轨迹") },
            text = {
                Column {
                    Text("选择导出格式：")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("共 ${trackPoints.size} 个轨迹点", fontSize = 12.sp, color = Color(0xFF475569))
                    if (selectedDate != null) {
                        Text("日期: $selectedDate", fontSize = 12.sp, color = PrimaryBlue)
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { exportTracks("GPX") }) {
                        Text("📄 GPX")
                    }
                    TextButton(onClick = { exportTracks("KML") }) {
                        Text("🗺️ KML")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showFullTrackDetail) {
        Dialog(
            onDismissRequest = { showFullTrackDetail = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A)
            ) {
                TrackDetailFullScreen(
                    points = trackPoints,
                    isRecording = isRecording,
                    onDismiss = { showFullTrackDetail = false }
                )
            }
        }
    }
}

@Composable
fun TrackPointItem(point: TrackPoint) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "📍 ${String.format("%.6f", point.latitude)}, ${String.format("%.6f", point.longitude)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "海拔: ${point.altitude?.let { String.format("%.1f", it) } ?: "--"}m | 精度: ±${point.accuracy?.let { String.format("%.1f", it) } ?: "--"}m",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
            }
            Text(
                text = formatTimestamp(point.timestamp),
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
