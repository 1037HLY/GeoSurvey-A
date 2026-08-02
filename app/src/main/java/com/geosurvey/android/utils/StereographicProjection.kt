package com.geosurvey.android.utils

import com.geosurvey.android.data.model.AttitudeRecord
import kotlin.math.*

/**
 * 赤平投影计算工具
 * 用于生成极射赤平投影图数据
 */
object StereographicProjection {

    /**
     * 产状数据点
     */
    data class ProjectionPoint(
        val x: Float,           // 投影X坐标 (-1 到 1)
        val y: Float,           // 投影Y坐标 (-1 到 1)
        val dipDirection: Float, // 倾向
        val dipAngle: Float,    // 倾角
        val label: String = ""  // 标签
    )

    /**
     * 将产状数据转换为赤平投影点
     * @param records 产状记录列表
     * @return 投影点列表
     */
    fun projectRecords(records: List<AttitudeRecord>): List<ProjectionPoint> {
        return records.map { record ->
            projectSingle(record.dipDirection, record.dipAngle)
        }
    }

    /**
     * 单个产状投影
     * @param dipDirection 倾向 (0-360)
     * @param dipAngle 倾角 (0-90)
     * @return 投影点
     */
    fun projectSingle(dipDirection: Float, dipAngle: Float): ProjectionPoint {
        // 将倾向转为弧度
        val dipRad = Math.toRadians(dipDirection.toDouble())
        
        // 计算投影半径 (倾角越大，投影点越靠近中心)
        val radius = 1.0 - (dipAngle / 90.0)
        
        // 计算投影坐标
        val x = radius * sin(dipRad)
        val y = radius * cos(dipRad)
        
        return ProjectionPoint(
            x = x.toFloat(),
            y = y.toFloat(),
            dipDirection = dipDirection,
            dipAngle = dipAngle
        )
    }

    /**
     * 生成赤平投影网格数据（大圆）
     */
    fun getGreatCirclePoints(radius: Float = 1f): List<Pair<Float, Float>> {
        val points = mutableListOf<Pair<Float, Float>>()
        val steps = 360
        for (i in 0..steps) {
            val angle = 2 * PI * i / steps
            val x = radius * sin(angle).toFloat()
            val y = radius * cos(angle).toFloat()
            points.add(x to y)
        }
        return points
    }

    /**
     * 生成倾向方向标记点
     */
    fun getDirectionMarkers(): List<Pair<Float, Float>> {
        return listOf(
            0f to 1.1f,   // N (北)
            1.1f to 0f,   // E (东)
            0f to -1.1f,  // S (南)
            -1.1f to 0f   // W (西)
        )
    }

    /**
     * 获取倾向方向标签
     */
    fun getDirectionLabels(): List<String> {
        return listOf("N", "E", "S", "W")
    }

    /**
     * 统计产状数据分布
     */
    fun getStatistics(records: List<AttitudeRecord>): Statistics {
        if (records.isEmpty()) return Statistics()

        val dipDirections = records.map { it.dipDirection }
        val dipAngles = records.map { it.dipAngle }

        val avgDirection = dipDirections.average().toFloat()
        val avgAngle = dipAngles.average().toFloat()

        // 计算最大最小
        val maxDirection = dipDirections.maxOrNull() ?: 0f
        val minDirection = dipDirections.minOrNull() ?: 0f
        val maxAngle = dipAngles.maxOrNull() ?: 0f
        val minAngle = dipAngles.minOrNull() ?: 0f

        // 计算优势方向（频数最高的方向区间）
        val directionHistogram = IntArray(36) // 每10度一个区间
        dipDirections.forEach { dir ->
            val index = (dir / 10).toInt() % 36
            directionHistogram[index]++
        }
        val dominantIndex = directionHistogram.indices.maxByOrNull { directionHistogram[it] } ?: 0
        val dominantDirection = dominantIndex * 10f + 5f

        return Statistics(
            count = records.size,
            avgDirection = avgDirection,
            avgAngle = avgAngle,
            maxDirection = maxDirection,
            minDirection = minDirection,
            maxAngle = maxAngle,
            minAngle = minAngle,
            dominantDirection = dominantDirection
        )
    }

    data class Statistics(
        val count: Int = 0,
        val avgDirection: Float = 0f,
        val avgAngle: Float = 0f,
        val maxDirection: Float = 0f,
        val minDirection: Float = 0f,
        val maxAngle: Float = 0f,
        val minAngle: Float = 0f,
        val dominantDirection: Float = 0f
    )
}
