// 在 LocationViewModel.kt 中，修改卫星数据生成部分
// 确保总数 = GPS + GLONASS + 北斗 + Galileo

viewModelScope.launch {
    while (_state.value.isActive) {
        val gps = 3 + (Math.random() * 6).toInt()
        val glonass = 1 + (Math.random() * 4).toInt()
        val beidou = 1 + (Math.random() * 4).toInt()
        val galileo = 1 + (Math.random() * 3).toInt()
        val total = gps + glonass + beidou + galileo  // ⭐ 总和

        _state.value = _state.value.copy(
            satelliteCount = total,
            usedSatelliteCount = (total * 0.6).toInt(),
            gpsCount = gps,
            glonassCount = glonass,
            beidouCount = beidou,
            galileoCount = galileo,
            averageSnr = 20f + (Math.random() * 20).toFloat(),
            qualityText = when {
                total > 15 -> "优秀 🌟"
                total > 10 -> "良好 ✅"
                total > 6 -> "一般 📡"
                else -> "较差 ⚠️"
            },
            qualityColor = when {
                total > 15 -> Color(0xFF10B981)
                total > 10 -> Color(0xFF0EA5E9)
                total > 6 -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }
        )
        kotlinx.coroutines.delay(3000)
    }
}
