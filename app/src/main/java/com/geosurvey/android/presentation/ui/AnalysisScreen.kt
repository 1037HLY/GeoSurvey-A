package com.geosurvey.android.presentation.ui

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
import androidx.navigation.NavController
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.presentation.viewmodel.AttitudeViewModel

@Composable
fun AnalysisScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val application = context.applicationContext as GeoSurveyApplication
    val viewModel = remember { AttitudeViewModel.getInstance(application) }
    val state by viewModel.state.collectAsState()

    val records = state.records

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "📊 地质分析",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 数据状态
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (records.isNotEmpty())
                    Color(0xFF10B981).copy(alpha = 0.1f)
                else
                    Color(0xFFF59E0B).copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📋 可用数据：${records.size} 条产状记录",
                    fontSize = 14.sp,
                    color = if (records.isNotEmpty()) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
                if (records.isEmpty()) {
                    Text(
                        text = "请先记录产状数据",
                        fontSize = 12.sp,
                        color = Color(0xFFF59E0B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ========== 第一行：赤平投影 + 玫瑰花图 ==========
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 赤平投影卡片
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📐",
                        fontSize = 36.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "赤平投影",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${records.size} 条数据",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (records.isNotEmpty()) {
                                navController.navigate("stereographic")
                            }
                        },
                        enabled = records.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0EA5E9)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (records.isNotEmpty()) "查看" else "无数据", fontSize = 12.sp)
                    }
                }
            }

            // 玫瑰花图卡片
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🌹",
                        fontSize = 36.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "玫瑰花图",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${records.size} 条数据",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (records.isNotEmpty()) {
                                navController.navigate("rose")
                            }
                        },
                        enabled = records.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B5CF6)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (records.isNotEmpty()) "查看" else "无数据", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ========== 第二行：坐标转换 ==========
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌐",
                    fontSize = 40.sp,
                    modifier = Modifier.width(60.dp)
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "坐标转换",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "WGS84 / CGCS2000 / 高斯投影",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "经纬度 ↔ 平面坐标互转",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                Button(
                    onClick = { navController.navigate("coordinate") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0EA5E9)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("进入")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ========== 第三行：使用说明 ==========
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF1F5F9)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💡 使用说明",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1. 在「产状」页面测量并记录产状数据\n2. 选择「赤平投影」或「玫瑰花图」查看分析\n3. 选择「坐标转换」进行坐标系转换",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
