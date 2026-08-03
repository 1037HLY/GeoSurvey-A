package com.geosurvey.android.utils

import android.location.Location
import kotlin.math.*

/**
 * GPS精度优化器
 * 包含：卡尔曼滤波 + 移动平均滤波 + HDOP/PDOP过滤 + 动态采样
 */
class PrecisionOptimizer {

    data class Config(
        var minAccuracy: Float = 15f,
        var minDistance: Float = 2.0f,
        var minSatellites: Int = 4,
        var maxHdop: Float = 5.0f,
        var maxPdop: Float = 8.0f,
        var stationarySpeed: Float = 0.5f,
        var useKalmanFilter: Boolean = true,
        var useMovingAverage: Boolean = true,
        var movingAverageWindow: Int = 5,
        var dynamicSampling: Boolean = true,
        var kalmanProcessNoise: Float = 0.005f,
        var kalmanMeasurementNoise: Float = 0.05f
    )

    // 卡尔曼滤波器
    class KalmanFilter(
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

    // 移动平均滤波器
    class MovingAverageFilter(private val windowSize: Int = 5) {
        private val buffer = mutableListOf<Float>()

        fun update(value: Float): Float {
            buffer.add(value)
            if (buffer.size > windowSize) {
                buffer.removeAt(0)
            }
            return buffer.average().toFloat()
        }

        fun reset() {
            buffer.clear()
        }
    }

    // 2D卡尔曼滤波器
    class KalmanFilter2D(processNoise: Float = 0.005f, measurementNoise: Float = 0.05f) {
        private val filterLat = KalmanFilter(processNoise, measurementNoise)
        private val filterLon = KalmanFilter(processNoise, measurementNoise)
        private val filterAlt = KalmanFilter(processNoise, measurementNoise * 2)

        fun update(lat: Float, lon: Float, alt: Float?): Triple<Float, Float, Float?> {
            val filteredLat = filterLat.update(lat)
            val filteredLon = filterLon.update(lon)
            val filteredAlt = alt?.let { filterAlt.update(it) }
            return Triple(filteredLat, filteredLon, filteredAlt)
        }

        fun reset() {
            filterLat.reset()
            filterLon.reset()
            filterAlt.reset()
        }
    }

    // 2D移动平均滤波器
    class MovingAverage2D(windowSize: Int = 5) {
        private val filterLat = MovingAverageFilter(windowSize)
        private val filterLon = MovingAverageFilter(windowSize)
        private val filterAlt = MovingAverageFilter(windowSize)

        fun update(lat: Float, lon: Float, alt: Float?): Triple<Float, Float, Float?> {
            val filteredLat = filterLat.update(lat)
            val filteredLon = filterLon.update(lon)
            val filteredAlt = alt?.let { filterAlt.update(it) }
            return Triple(filteredLat, filteredLon, filteredAlt)
        }

        fun reset() {
            filterLat.reset()
            filterLon.reset()
            filterAlt.reset()
        }
    }

    private val config = Config()
    private val kalmanFilter = KalmanFilter2D(
        config.kalmanProcessNoise,
        config.kalmanMeasurementNoise
    )
    private val movingAverage = MovingAverage2D(config.movingAverageWindow)

    private var lastLocation: Location? = null
    private var stationaryCount = 0
    private val stationaryThreshold = 3
    private var isFirstFix = true

    fun optimize(
        location: Location,
        satelliteCount: Int = 0,
        hdop: Float? = null,
        pdop: Float? = null,
        isRecording: Boolean = false
    ): Location? {
        // 1. 精度过滤
        if (location.accuracy != null && location.accuracy > config.minAccuracy) {
            return null
        }

        // 2. 卫星数量过滤
        if (satelliteCount > 0 && satelliteCount < config.minSatellites) {
            return null
        }

        // 3. HDOP过滤
        if (hdop != null && hdop > config.maxHdop) {
            return null
        }

        // 4. PDOP过滤
        if (pdop != null && pdop > config.maxPdop) {
            return null
        }

        // 5. 静止检测
        val speedKmh = location.speed?.let { it * 3.6 } ?: 0f
        if (speedKmh < config.stationarySpeed) {
            if (lastLocation != null) {
                val distance = lastLocation!!.distanceTo(location)
                if (distance < config.minDistance) {
                    stationaryCount++
                    if (stationaryCount < stationaryThreshold) {
                        return null
                    }
                } else {
                    stationaryCount = 0
                }
            }
        } else {
            stationaryCount = 0
        }

        // 6. 距离过滤
        if (lastLocation != null) {
            val distance = lastLocation!!.distanceTo(location)
            if (distance < config.minDistance) {
                return null
            }
        }

        // 7. 应用滤波
        var lat = location.latitude.toFloat()
        var lon = location.longitude.toFloat()
        var alt = location.altitude?.toFloat()

        if (config.useKalmanFilter) {
            val (kalmanLat, kalmanLon, kalmanAlt) = kalmanFilter.update(lat, lon, alt)
            lat = kalmanLat
            lon = kalmanLon
            alt = kalmanAlt
        }

        if (config.useMovingAverage) {
            val (avgLat, avgLon, avgAlt) = movingAverage.update(lat, lon, alt)
            lat = avgLat
            lon = avgLon
            alt = avgAlt
        }

        // 8. 创建优化后的Location
        val result = Location(location).apply {
            this.latitude = lat.toDouble()
            this.longitude = lon.toDouble()
            this.altitude = alt?.toDouble()
            this.accuracy = location.accuracy?.let { it * 0.7f }
        }

        lastLocation = result
        return result
    }

    fun reset() {
        kalmanFilter.reset()
        movingAverage.reset()
        lastLocation = null
        isFirstFix = true
        stationaryCount = 0
    }

    fun getConfig(): Config = config
}
