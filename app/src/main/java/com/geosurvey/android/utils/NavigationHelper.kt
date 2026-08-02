package com.geosurvey.android.utils

import android.location.Location
import kotlin.math.*

/**
 * 轨迹导航计算工具
 */
object NavigationHelper {

    /**
     * 导航数据
     */
    data class NavigationData(
        val currentLocation: Location? = null,
        val targetLocation: Location? = null,
        val distance: Float = 0f,           // 到目标距离（米）
        val bearing: Float = 0f,             // 到目标方位角（度）
        val bearingToTarget: Float = 0f,     // 到目标的方位角
        val heading: Float = 0f,             // 当前航向
        val isOffTrack: Boolean = false,     // 是否偏离轨迹
        val offTrackDistance: Float = 0f,    // 偏离距离（米）
        val targetName: String = "",         // 目标名称
        val progress: Float = 0f             // 进度百分比
    )

    /**
     * 计算两点之间距离（米）
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * 计算方位角（度）
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)

        val x = sin(dLon) * cos(lat2Rad)
        val y = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)

        val bearing = Math.toDegrees(atan2(x, y)).toFloat()
        return (bearing + 360) % 360
    }

    /**
     * 计算目标方向指示（用于UI箭头）
     */
    fun calculateTargetDirection(
        currentLocation: Location,
        targetLocation: Location,
        currentHeading: Float
    ): Float {
        val bearing = calculateBearing(
            currentLocation.latitude, currentLocation.longitude,
            targetLocation.latitude, targetLocation.longitude
        )
        // 计算相对于当前航向的角度差
        var diff = bearing - currentHeading
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360
        return diff
    }

    /**
     * 检查是否偏离轨迹
     * @param current 当前位置
     * @param target 目标位置
     * @param threshold 偏离阈值（米）
     */
    fun checkOffTrack(
        current: Location,
        target: Location,
        threshold: Float = 10f
    ): Boolean {
        val distance = calculateDistance(
            current.latitude, current.longitude,
            target.latitude, target.longitude
        )
        return distance > threshold
    }

    /**
     * 计算导航进度
     * @param start 起点
     * @param current 当前位置
     * @param target 目标点
     */
    fun calculateProgress(
        start: Location,
        current: Location,
        target: Location
    ): Float {
        val totalDistance = calculateDistance(
            start.latitude, start.longitude,
            target.latitude, target.longitude
        )
        if (totalDistance == 0f) return 0f

        val traveledDistance = calculateDistance(
            start.latitude, start.longitude,
            current.latitude, current.longitude
        )
        return (traveledDistance / totalDistance * 100).coerceIn(0f, 100f)
    }

    /**
     * 生成导航指引文字
     */
    fun getGuidanceText(distance: Float, bearing: Float, isOffTrack: Boolean): String {
        return when {
            isOffTrack -> "⚠️ 偏离轨迹！请调整方向"
            distance < 5 -> "🎯 已到达目标点！"
            distance < 50 -> "📍 目标就在前方 ${distance.toInt()} 米"
            distance < 200 -> "📍 目标 ${distance.toInt()} 米，方向 ${bearing.toInt()}°"
            else -> "📍 目标 ${distance.toInt()} 米，方向 ${bearing.toInt()}°"
        }
    }

    /**
     * 生成方向描述
     */
    fun getDirectionDescription(bearing: Float): String {
        return when {
            bearing in 337.5f..360f || bearing in 0f..22.5f -> "北 ↑"
            bearing in 22.5f..67.5f -> "东北 ↗"
            bearing in 67.5f..112.5f -> "东 →"
            bearing in 112.5f..157.5f -> "东南 ↘"
            bearing in 157.5f..202.5f -> "南 ↓"
            bearing in 202.5f..247.5f -> "西南 ↙"
            bearing in 247.5f..292.5f -> "西 ←"
            bearing in 292.5f..337.5f -> "西北 ↖"
            else -> "北 ↑"
        }
    }
}
