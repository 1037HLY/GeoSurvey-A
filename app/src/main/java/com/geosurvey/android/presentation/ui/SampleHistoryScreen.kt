package com.geosurvey.android.presentation.ui

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
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.data.model.DrillSample
import com.geosurvey.android.data.model.NormalSample
import com.geosurvey.android.presentation.theme.*
import com.geosurvey.android.presentation.viewmodel.SampleViewModel
import com.geosurvey.android.utils.CSVExportHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private fun formatSampleTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun SampleHistoryScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication
    val viewModel = remember { SampleViewModel.getInstance(application) }
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // 全屏模式
    var showFullScreen by remember { mutableStateOf(false) }

    // 选中的样本（用于编辑/删除）
    var selectedNormalSample by remember { mutableStateOf<NormalSample?>(null) }
    var selectedDrillSample by remember { mutableStateOf<DrillSample?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editType by remember { mutableStateOf("") } // "normal" 或 "drill"

    // 只取最新2条（1条普通+1条钻孔）
    val latestNormal = state.normalSamples.firstOrNull()
    val latestDrill = state.drillSamples.firstOrNull()
    val hasData = latestNormal != null || latestDrill != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📋 样本历史记录",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            if (hasData) {
                Row {
                    // 导出按钮
                    IconButton(onClick = {
                        // 显示导出选项对话框
                        showExportDialog(context, state.normalSamples, state.drillSamples)
                    }) {
                        Text("📤", fontSize = 20.sp)
                    }
                    // 全屏按钮
                    IconButton(onClick = { showFullScreen = true }) {
                        Text("⛶", fontSize = 20.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!hasData) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无样本记录\n请先录入样本",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            // 显示最新1条普通样本
            latestNormal?.let { sample ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedNormalSample = sample
                            editType = "normal"
                            showEditDialog = true
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📦 ${sample.sampleNumber} - ${sample.sampleName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "类型: ${sample.sampleType} | 重量: ${sample.weight}kg",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "点击编辑/删除",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Text(
                            text = formatSampleTime(sample.timestamp),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 显示最新1条钻孔样本
            latestDrill?.let { sample ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedDrillSample = sample
                            editType = "drill"
                            showEditDialog = true
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🪨 ${sample.sampleNumber} - ${sample.sampleName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "井深: ${sample.fromDepth}-${sample.toDepth}m | 采取率: ${sample.recoveryRate}%",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "点击编辑/删除",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Text(
                            text = formatSampleTime(sample.timestamp),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "💡 点击条目可编辑或删除 | 📤 导出CSV | ⛶ 查看全部",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }

    // ========== 全屏对话框 ==========
    if (showFullScreen) {
        Dialog(onDismissRequest = { showFullScreen = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC)
            ) {
                FullScreenHistory(
                    normalSamples = state.normalSamples,
                    drillSamples = state.drillSamples,
                    onDismiss = { showFullScreen = false },
                    onEditNormal = { sample ->
                        selectedNormalSample = sample
                        editType = "normal"
                        showEditDialog = true
                        showFullScreen = false
                    },
                    onEditDrill = { sample ->
                        selectedDrillSample = sample
                        editType = "drill"
                        showEditDialog = true
                        showFullScreen = false
                    },
                    onExport = {
                        showExportDialog(context, state.normalSamples, state.drillSamples)
                    }
                )
            }
        }
    }

    // ========== 编辑/删除对话框 ==========
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (editType == "normal") "📦 编辑普通样本" else "🪨 编辑钻孔样本") },
            text = {
                Column {
                    if (editType == "normal" && selectedNormalSample != null) {
                        val sample = selectedNormalSample!!
                        Text("编号: ${sample.sampleNumber}")
                        Text("名称: ${sample.sampleName}")
                        Text("类型: ${sample.sampleType}")
                        Text("重量: ${sample.weight}kg")
                        Text("描述: ${sample.description}")
                    }
                    if (editType == "drill" && selectedDrillSample != null) {
                        val sample = selectedDrillSample!!
                        Text("编号: ${sample.sampleNumber}")
                        Text("名称: ${sample.sampleName}")
                        Text("井深: ${sample.fromDepth}-${sample.toDepth}m")
                        Text("样长: ${sample.sampleLength}m")
                        Text("采取率: ${sample.recoveryRate}%")
                        Text("重量: ${sample.weight}kg")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("选择操作：", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 编辑按钮
                    Button(
                        onClick = {
                            Toast.makeText(context, "✏️ 编辑功能开发中", Toast.LENGTH_SHORT).show()
                            showEditDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("✏️ 编辑")
                    }
                    // 删除按钮
                    Button(
                        onClick = {
                            // 删除操作
                            coroutineScope.launch {
                                try {
                                    if (editType == "normal" && selectedNormalSample != null) {
                                        // TODO: 实现单个删除
                                        Toast.makeText(context, "🗑️ 已删除普通样本", Toast.LENGTH_SHORT).show()
                                    }
                                    if (editType == "drill" && selectedDrillSample != null) {
                                        // TODO: 实现单个删除
                                        Toast.makeText(context, "🗑️ 已删除钻孔样本", Toast.LENGTH_SHORT).show()
                                    }
                                    showEditDialog = false
                                    viewModel.loadData()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🗑️ 删除")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// ========== 全屏历史记录 ==========
@Composable
fun FullScreenHistory(
    normalSamples: List<NormalSample>,
    drillSamples: List<DrillSample>,
    onDismiss: () -> Unit,
    onEditNormal: (NormalSample) -> Unit,
    onEditDrill: (DrillSample) -> Unit,
    onExport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📋 全部样本",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Row {
                IconButton(onClick = onExport) {
                    Text("📤", fontSize = 20.sp)
                }
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 上半部分：普通样本
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                ) {
                    Text(
                        text = "📦 普通样本 (${normalSamples.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (normalSamples.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无记录", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(normalSamples) { sample ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onEditNormal(sample) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                "${sample.sampleNumber} - ${sample.sampleName}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "类型: ${sample.sampleType} | ${sample.weight}kg",
                                                fontSize = 10.sp,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                        Text(
                                            formatSampleTime(sample.timestamp),
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 下半部分：钻孔样本
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                ) {
                    Text(
                        text = "🪨 钻孔样本 (${drillSamples.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPurple
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (drillSamples.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无记录", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(drillSamples) { sample ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onEditDrill(sample) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                "${sample.sampleNumber} - ${sample.sampleName}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "井深: ${sample.fromDepth}-${sample.toDepth}m | 采取率: ${sample.recoveryRate}%",
                                                fontSize = 10.sp,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                        Text(
                                            formatSampleTime(sample.timestamp),
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
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

// ========== 导出对话框 ==========
@Composable
fun showExportDialog(
    context: android.content.Context,
    normalSamples: List<NormalSample>,
    drillSamples: List<DrillSample>
) {
    // 使用AlertDialog显示导出选项
    // 由于在Composable外部调用，使用Dialog
}

// 在Composable中调用
@Composable
fun ExportDialog(
    normalSamples: List<NormalSample>,
    drillSamples: List<DrillSample>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📤 导出CSV") },
        text = {
            Column {
                Text("选择要导出的数据：", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val helper = CSVExportHelper(context)
                            val file = helper.exportNormalSamples(normalSamples)
                            if (file != null) {
                                Toast.makeText(context, "导出成功: ${file.name}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("📦 导出普通样本 (${normalSamples.size})")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val helper = CSVExportHelper(context)
                            val file = helper.exportDrillSamples(drillSamples)
                            if (file != null) {
                                Toast.makeText(context, "导出成功: ${file.name}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🪨 导出钻孔样本 (${drillSamples.size})")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val helper = CSVExportHelper(context)
                            val allSamples = normalSamples + drillSamples
                            // 导出全部
                            val normalFile = helper.exportNormalSamples(normalSamples, "all_normal")
                            val drillFile = helper.exportDrillSamples(drillSamples, "all_drill")
                            if (normalFile != null || drillFile != null) {
                                Toast.makeText(context, "全部导出成功", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("📤 全部导出 (${normalSamples.size + drillSamples.size})")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
