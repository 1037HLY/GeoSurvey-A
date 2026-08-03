package com.geosurvey.android.utils

import android.location.Location

/**
 * GPS精度优化器（简化版）
 * 包含：卡尔曼滤波 + 精度过滤 + 距离过滤
 */
class PrecisionOptimizer {

    // 卡尔曼滤波器
    private class KalmanFilter(
        private val processNoise: Float = 0.005f,
        private val measurementNoise: Float = 0.05f
    ) {
        private var x = 0f
        private var p = 1f
        private var initialized = false

        fun update(measurement: Float): Float {
            if (!initialized) {
                x = measurement
                p = 1f
                initialized = true
                return x
            }
            p = p + processNoise
            val k = p / (p + measurementNoise)
            x = x + k * (measurement - x)
            p = (1 - k) * p
            return x
        }

        fun reset() {
            initialized = false
            x = 0f
            p = 1f
        }
    }

    // 2D卡尔曼滤波器
    private class KalmanFilter2D {
        private val filterLat = KalmanFilter(0.005f, 0.05f)
        private val filterLon = KalmanFilter(0.005f, 0.05f)
        private val filterAlt = KalmanFilter(0.01f, 0.1f)

        fun update(lat: Float, lon: Float, alt: Float): Triple<Float, Float, Float> {
            return Triple(
                filterLat.update(lat),
                filterLon.update(lon),
                filterAlt.update(alt)
            )
        }

        fun reset() {
            filterLat.reset()
            filterLon.reset()
            filterAlt.reset()
        }
    }

    private val kalmanFilter = KalmanFilter2D()
    private var lastLocation: Location? = null

    fun optimize(location: Location): Location? {
        // 1. 精度过滤
        if (location.accuracy != null && location.accuracy > 15f) {
            return null
        }

        // 2. 距离过滤
        if (lastLocation != null) {
            val distance = lastLocation!!.distanceTo(location)
            if (distance < 2.0) {
                return null
            }
        }

        // 3. 应用卡尔曼滤波
        val lat = location.latitude.toFloat()
        val lon = location.longitude.toFloat()
        val alt = location.altitude?.toFloat() ?: 0f
        
        val (filteredLat, filteredLon, filteredAlt) = kalmanFilter.update(lat, lon, alt)
        
        val result = Location(location).apply {
            this.latitude = filteredLat.toDouble()
            this.longitude = filteredLon.toDouble()
            this.altitude = filteredAlt.toDouble()
            // 修复：使用 ?: 0f 提供默认值
            this.accuracy = location.accuracy?.let { it * 0.7f } ?: location.accuracy
        }

        lastLocation = result
        return result
    }

    fun reset() {
        kalmanFilter.reset()
        lastLocation = null
    }
}
