package com.geosurvey.android.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geosurvey.android.presentation.ui.components.GlassCard

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📋 关于",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            IconButton(onClick = onBack) {
                Text("✕", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 应用信息
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🏔️ 地质勘查工具箱",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "GeoSurvey Toolbox v1.0.0",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "专业野外地质调查软件",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
                Text(
                    text = "基于 Android + Jetpack Compose",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 版权信息
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📄 版权信息",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "© 2026 GeoSurvey Toolbox\n" +
                           "保留所有权利 | All Rights Reserved",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "本软件为开源项目，仅供学习研究使用",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 免责条款
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚠️ 免责条款",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. 🔒 国家安全\n" +
                           "   本软件仅用于地质调查和科学研究目的。\n" +
                           "   请勿在军事禁区、国家机密区域使用。\n" +
                           "   使用前请遵守当地法律法规。\n\n" +
                           "2. ⚖️ 法律合规\n" +
                           "   用户须自行承担使用本软件的法律责任。\n" +
                           "   请确保获取必要的地质调查许可。\n\n" +
                           "3. 🛡️ 个人安全\n" +
                           "   野外作业存在风险，请注意人身安全。\n" +
                           "   建议结伴同行，携带必要安全装备。\n" +
                           "   本软件不能替代专业安全判断。\n\n" +
                           "4. 📊 数据精度\n" +
                           "   定位精度受GPS信号、设备、环境影响。\n" +
                           "   数据仅供参考，不作为正式测量依据。\n" +
                           "   建议进行多次测量取平均值。\n\n" +
                           "5. 📱 设备依赖\n" +
                           "   本软件功能依赖手机硬件和GPS信号。\n" +
                           "   不同设备可能存在性能差异。",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 联系方式
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📧 联系方式",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "如发现问题或有改进建议，请联系开发者",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "🔧 开发中... 功能持续更新",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
