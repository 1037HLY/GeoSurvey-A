package com.geosurvey.android.presentation.ui

import android.widget.Toast
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
import com.geosurvey.android.presentation.ui.components.GlassCard
import com.geosurvey.android.presentation.viewmodel.SampleViewModel
import com.geosurvey.android.utils.CSVExportHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ⭐ 使用唯一的函数名
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

    var selectedNormalSample by remember { mutableStateOf<NormalSample?>(null) }
    var showNormalDetail by remember { mutableStateOf(false) }
    var isEditingNormal by remember { mutableStateOf(false) }
    var editNormalSample by remember { mutableStateOf<NormalSample?>(null) }

    var selectedDrillSample by remember { mutableStateOf<DrillSample?>(null) }
    var showDrillDetail by remember { mutableStateOf(false) }
    var isEditingDrill by remember { mutableStateOf(false) }
    var editDrillSample by remember { mutableStateOf<DrillSample?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "📋 样本历史记录",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "左侧: 普通样本 | 右侧: 钻孔样本  |  点击查看详情 | 长按编辑",
            fontSize = 12.sp,
            color = Color(0xFF475569)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 左侧普通样本面板
            NormalSamplePanel(
                samples = state.normalSamples,
                onSampleClick = { sample ->
                    selectedNormalSample = sample
                    showNormalDetail = true
                    isEditingNormal = false
                },
                onSampleLongClick = { sample ->
                    selectedNormalSample = sample
                    editNormalSample = sample.copy()
                    isEditingNormal = true
                    showNormalDetail = true
                },
                onExportClick = {
                    coroutineScope.launch {
                        val helper = CSVExportHelper(context)
                        val file = helper.exportNormalSamples(state.normalSamples)
                        if (file != null) {
                            Toast.makeText(context, "导出成功: ${file.name}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            // 右侧钻孔样本面板
            DrillSamplePanel(
                samples = state.drillSamples,
                onSampleClick = { sample ->
                    selectedDrillSample = sample
                    showDrillDetail = true
                    isEditingDrill = false
                },
                onSampleLongClick = { sample ->
                    selectedDrillSample = sample
                    editDrillSample = sample.copy()
                    isEditingDrill = true
                    showDrillDetail = true
                },
                onExportClick = {
                    coroutineScope.launch {
                        val helper = CSVExportHelper(context)
                        val file = helper.exportDrillSamples(state.drillSamples)
                        if (file != null) {
                            Toast.makeText(context, "导出成功: ${file.name}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    // 普通样本详情对话框
    if (showNormalDetail && selectedNormalSample != null) {
        Dialog(onDismissRequest = { showNormalDetail = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC)
            ) {
                if (isEditingNormal && editNormalSample != null) {
                    NormalSampleEditDialog(
                        sample = editNormalSample!!,
                        onSave = {
                            Toast.makeText(context, "已更新", Toast.LENGTH_SHORT).show()
                            showNormalDetail = false
                            isEditingNormal = false
                            viewModel.loadData()
                        },
                        onCancel = {
                            showNormalDetail = false
                            isEditingNormal = false
                        }
                    )
                } else {
                    NormalSampleDetailDialog(
                        sample = selectedNormalSample!!,
                        onEdit = {
                            isEditingNormal = true
                            editNormalSample = selectedNormalSample!!.copy()
                        },
                        onClose = { showNormalDetail = false }
                    )
                }
            }
        }
    }

    // 钻孔样本详情对话框
    if (showDrillDetail && selectedDrillSample != null) {
        Dialog(onDismissRequest = { showDrillDetail = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC)
            ) {
                if (isEditingDrill && editDrillSample != null) {
                    DrillSampleEditDialog(
                        sample = editDrillSample!!,
                        onSave = {
                            Toast.makeText(context, "已更新", Toast.LENGTH_SHORT).show()
                            showDrillDetail = false
                            isEditingDrill = false
                            viewModel.loadData()
                        },
                        onCancel = {
                            showDrillDetail = false
                            isEditingDrill = false
                        }
                    )
                } else {
                    DrillSampleDetailDialog(
                        sample = selectedDrillSample!!,
                        onEdit = {
                            isEditingDrill = true
                            editDrillSample = selectedDrillSample!!.copy()
                        },
                        onClose = { showDrillDetail = false }
                    )
                }
            }
        }
    }
}

// ========== 普通样本面板 ==========
@Composable
fun NormalSamplePanel(
    samples: List<NormalSample>,
    onSampleClick: (NormalSample) -> Unit,
    onSampleLongClick: (NormalSample) -> Unit,
    onExportClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📦 普通样本 (${samples.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
                if (samples.isNotEmpty()) {
                    IconButton(
                        onClick = onExportClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("📤", fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (samples.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无普通样本",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(samples) { sample ->
                        NormalSampleCard(
                            sample = sample,
                            onClick = { onSampleClick(sample) },
                            onLongClick = { onSampleLongClick(sample) }
                        )
                    }
                }
            }
        }
    }
}

// ========== 钻孔样本面板 ==========
@Composable
fun DrillSamplePanel(
    samples: List<DrillSample>,
    onSampleClick: (DrillSample) -> Unit,
    onSampleLongClick: (DrillSample) -> Unit,
    onExportClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🪨 钻孔样本 (${samples.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentPurple
                )
                if (samples.isNotEmpty()) {
                    IconButton(
                        onClick = onExportClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("📤", fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (samples.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无钻孔样本",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(samples) { sample ->
                        DrillSampleCard(
                            sample = sample,
                            onClick = { onSampleClick(sample) },
                            onLongClick = { onSampleLongClick(sample) }
                        )
                    }
                }
            }
        }
    }
}

// ========== 普通样本卡片 ==========
@Composable
fun NormalSampleCard(
    sample: NormalSample,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📦 ${sample.sampleNumber}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = formatSampleTime(sample.timestamp),
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Text(
                text = sample.sampleName,
                fontSize = 12.sp,
                color = Color(0xFF475569)
            )
            Text(
                text = "类型: ${sample.sampleType} | 重量: ${sample.weight}kg",
                fontSize = 11.sp,
                color = Color(0xFF475569)
            )
            Text(
                text = "长按可编辑",
                fontSize = 9.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

// ========== 钻孔样本卡片 ==========
@Composable
fun DrillSampleCard(
    sample: DrillSample,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🪨 ${sample.sampleNumber}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = formatSampleTime(sample.timestamp),
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Text(
                text = sample.sampleName,
                fontSize = 12.sp,
                color = Color(0xFF475569)
            )
            Text(
                text = "井深: ${sample.fromDepth}-${sample.toDepth}m | 样长: ${sample.sampleLength}m",
                fontSize = 11.sp,
                color = Color(0xFF475569)
            )
            Text(
                text = "采取率: ${sample.recoveryRate}% | 重量: ${sample.weight}kg",
                fontSize = 11.sp,
                color = Color(0xFF475569)
            )
            Text(
                text = "长按可编辑",
                fontSize = 9.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

// ========== 普通样本详情对话框 ==========
@Composable
fun NormalSampleDetailDialog(
    sample: NormalSample,
    onEdit: () -> Unit,
    onClose: () -> Unit
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
                text = "📦 样本详情",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Row {
                IconButton(onClick = onEdit) {
                    Text("✏️", fontSize = 18.sp)
                }
                IconButton(onClick = onClose) {
                    Text("✕", fontSize = 18.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("编号: ${sample.sampleNumber}", fontSize = 14.sp)
                Text("名称: ${sample.sampleName}", fontSize = 14.sp)
                Text("类型: ${sample.sampleType}", fontSize = 14.sp)
                Text("重量: ${sample.weight}kg", fontSize = 14.sp)
                Text("纬度: ${String.format("%.6f", sample.latitude)}", fontSize = 14.sp)
                Text("经度: ${String.format("%.6f", sample.longitude)}", fontSize = 14.sp)
                sample.altitude?.let {
                    Text("海拔: ${String.format("%.1f", it)}m", fontSize = 14.sp)
                }
                Text("描述: ${sample.description}", fontSize = 14.sp)
                Text("时间: ${formatSampleTime(sample.timestamp)}", fontSize = 14.sp)
            }
        }
    }
}

// ========== 钻孔样本详情对话框 ==========
@Composable
fun DrillSampleDetailDialog(
    sample: DrillSample,
    onEdit: () -> Unit,
    onClose: () -> Unit
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
                text = "🪨 钻孔样本详情",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Row {
                IconButton(onClick = onEdit) {
                    Text("✏️", fontSize = 18.sp)
                }
                IconButton(onClick = onClose) {
                    Text("✕", fontSize = 18.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("编号: ${sample.sampleNumber}", fontSize = 14.sp)
                Text("名称: ${sample.sampleName}", fontSize = 14.sp)
                Text("井深: ${sample.fromDepth} - ${sample.toDepth}m", fontSize = 14.sp)
                Text("样长: ${sample.sampleLength}m", fontSize = 14.sp)
                Text("岩心长: ${sample.coreLength}m", fontSize = 14.sp)
                Text("采取率: ${sample.recoveryRate}%", fontSize = 14.sp)
                Text("重量: ${sample.weight}kg", fontSize = 14.sp)
                Text("岩心直径: ${sample.coreDiameter}mm", fontSize = 14.sp)
                Text("描述: ${sample.description}", fontSize = 14.sp)
                Text("时间: ${formatSampleTime(sample.timestamp)}", fontSize = 14.sp)
            }
        }
    }
}

// ========== 普通样本编辑对话框 ==========
@Composable
fun NormalSampleEditDialog(
    sample: NormalSample,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var type by remember { mutableStateOf(sample.sampleType) }
    var number by remember { mutableStateOf(sample.sampleNumber) }
    var name by remember { mutableStateOf(sample.sampleName) }
    var weight by remember { mutableStateOf(sample.weight) }
    var desc by remember { mutableStateOf(sample.description) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("✏️ 编辑普通样本", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("编号 *") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称 *") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("类型") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("重量 (kg)") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("描述") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), maxLines = 3)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                shape = RoundedCornerShape(12.dp)) { Text("💾 保存") }
            Button(onClick = onCancel, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF94A3B8)),
                shape = RoundedCornerShape(12.dp)) { Text("取消") }
        }
    }
}

// ========== 钻孔样本编辑对话框 ==========
@Composable
fun DrillSampleEditDialog(
    sample: DrillSample,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var number by remember { mutableStateOf(sample.sampleNumber) }
    var name by remember { mutableStateOf(sample.sampleName) }
    var fromDepth by remember { mutableStateOf(sample.fromDepth) }
    var toDepth by remember { mutableStateOf(sample.toDepth) }
    var sampleLength by remember { mutableStateOf(sample.sampleLength) }
    var coreLength by remember { mutableStateOf(sample.coreLength) }
    var recoveryRate by remember { mutableStateOf(sample.recoveryRate) }
    var weight by remember { mutableStateOf(sample.weight) }
    var coreDiameter by remember { mutableStateOf(sample.coreDiameter) }
    var desc by remember { mutableStateOf(sample.description) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("✏️ 编辑钻孔样本", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("编号 *") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称 *") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = fromDepth, onValueChange = { fromDepth = it }, label = { Text("井深(自)") },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), singleLine = true)
            OutlinedTextField(value = toDepth, onValueChange = { toDepth = it }, label = { Text("井深(至)") },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), singleLine = true)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = sampleLength, onValueChange = { sampleLength = it }, label = { Text("样长(m)") },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), singleLine = true)
            OutlinedTextField(value = coreLength, onValueChange = { coreLength = it }, label = { Text("岩心长(m)") },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), singleLine = true)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = recoveryRate, onValueChange = { recoveryRate = it }, label = { Text("采取率(%)") },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), singleLine = true)
            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("重量(kg)") },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), singleLine = true)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = coreDiameter, onValueChange = { coreDiameter = it }, label = { Text("岩心直径(mm)") },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), singleLine = true)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("描述") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), maxLines = 3)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                shape = RoundedCornerShape(12.dp)) { Text("💾 保存") }
            Button(onClick = onCancel, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF94A3B8)),
                shape = RoundedCornerShape(12.dp)) { Text("取消") }
        }
    }
}
