package com.geosurvey.android.utils

import com.geosurvey.android.data.model.AttitudeRecord

/**
 * 玫瑰花图计算工具
 * 用于统计走向方向分布
 */
object RoseDiagram {

    /**
     * 玫瑰花图数据
     */
    data class RoseData(
        val bins: List<Bin>,
        val totalCount: Int,
        val maxCount: Int
    )

    data class Bin(
        val angle: Float,      // 角度中心值 (0-360)
        val count: Int,        // 频数
        val normalizedValue: Float // 归一化值 (0-1)
    )

    /**
     * 计算走向玫瑰花图数据
     * @param records 产状记录列表
     * @param binSize 分组大小 (默认10度)
     * @return 玫瑰花图数据
     */
    fun calculate(records: List<AttitudeRecord>, binSize: Int = 10): RoseData {
        if (records.isEmpty()) {
            return RoseData(emptyList(), 0, 0)
        }

        // 提取走向
        val strikes = records.map { it.strike }

        // 计算分组数量
        val numBins = 360 / binSize
        val bins = IntArray(numBins)

        // 统计走向分布
        strikes.forEach { strike ->
            var angle = strike
            // 走向范围 0-180 (因为走向是对称的)
            angle = if (angle > 180) angle - 180 else angle
            // 处理边界情况
            if (angle >= 360) angle -= 360
            if (angle < 0) angle += 360

            val index = (angle / binSize).toInt()
            if (index < numBins) {
                bins[index]++
            }
        }

        // 找到最大频数
        val maxCount = bins.maxOrNull() ?: 0

        // 构建Bin列表
        val binList = bins.mapIndexed { index, count ->
            val angle = index * binSize + binSize / 2f
            val normalizedValue = if (maxCount > 0) count.toFloat() / maxCount else 0f
            Bin(
                angle = angle,
                count = count,
                normalizedValue = normalizedValue
            )
        }

        return RoseData(
            bins = binList,
            totalCount = records.size,
            maxCount = maxCount
        )
    }

    /**
     * 获取优势走向（频数最高的方向）
     */
    fun getDominantStrike(records: List<AttitudeRecord>): Float? {
        if (records.isEmpty()) return null

        val strikes = records.map { it.strike }
        val histogram = IntArray(36) // 每10度一个区间
        strikes.forEach { strike ->
            var angle = strike
            if (angle > 180) angle -= 180
            val index = (angle / 10).toInt() % 36
            histogram[index]++
        }

        val dominantIndex = histogram.indices.maxByOrNull { histogram[it] } ?: 0
        return dominantIndex * 10f + 5f
    }
}
