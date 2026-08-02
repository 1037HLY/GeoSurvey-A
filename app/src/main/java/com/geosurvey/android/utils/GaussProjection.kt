package com.geosurvey.android.utils

import kotlin.math.*

/**
 * 高斯-克吕格投影
 * 用于经纬度 ↔ 平面坐标转换
 */
object GaussProjection {

    /**
     * 高斯投影坐标
     */
    data class GaussCoord(
        val x: Double,  // 北坐标 (米)
        val y: Double,  // 东坐标 (米，已加带号)
        val zone: Int,  // 带号
        val centralMeridian: Double // 中央子午线 (度)
    )

    // WGS84 椭球参数
    private const val A = 6378137.0
    private const val E2 = 0.00669437999014

    /**
     * 经纬度 → 高斯投影坐标
     * @param lat 纬度 (度)
     * @param lon 经度 (度)
     * @param zone 带号 (3度带或6度带)
     * @param isSixDegree 是否6度带
     */
    fun blhToGauss(
        lat: Double,
        lon: Double,
        zone: Int? = null,
        isSixDegree: Boolean = true
    ): GaussCoord {
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)

        // 计算带号和中央子午线
        val actualZone = zone ?: if (isSixDegree) {
            ((lon + 3) / 6).toInt()
        } else {
            ((lon + 1.5) / 3).toInt()
        }

        val centralMeridian = if (isSixDegree) {
            actualZone * 6 - 3
        } else {
            actualZone * 3
        }

        val cmRad = Math.toRadians(centralMeridian.toDouble())
        val dLon = lonRad - cmRad

        // 计算辅助量
        val a0 = 1 - E2 / 4 - 3 * E2 * E2 / 64 - 5 * E2 * E2 * E2 / 256
        val a2 = 3 * E2 / 8 + 3 * E2 * E2 / 32 + 45 * E2 * E2 * E2 / 1024
        val a4 = 15 * E2 * E2 / 256 + 45 * E2 * E2 * E2 / 1024
        val a6 = 35 * E2 * E2 * E2 / 3072

        // 计算子午线弧长
        val B = A * (a0 * latRad - a2 * sin(2 * latRad) + a4 * sin(4 * latRad) - a6 * sin(6 * latRad))

        // 计算投影坐标
        val t = tan(latRad)
        val eta2 = E2 / (1 - E2) * cos(latRad) * cos(latRad)
        val N = A / sqrt(1 - E2 * sin(latRad) * sin(latRad))

        val x = B + N / 2 * t * dLon * dLon +
                N / 24 * t * (5 - t * t + 9 * eta2 + 4 * eta2 * eta2) * dLon * dLon * dLon * dLon +
                N / 720 * t * (61 - 58 * t * t + t * t * t * t) * dLon * dLon * dLon * dLon * dLon * dLon

        val y = N * dLon +
                N / 6 * (1 - t * t + eta2) * dLon * dLon * dLon +
                N / 120 * (5 - 18 * t * t + t * t * t * t + 14 * eta2 - 58 * eta2 * t * t) * dLon * dLon * dLon * dLon * dLon

        // 东坐标加带号 (500公里偏移)
        val yFinal = y + 500000

        return GaussCoord(
            x = x,
            y = yFinal,
            zone = actualZone,
            centralMeridian = centralMeridian.toDouble()
        )
    }

    /**
     * 高斯投影坐标 → 经纬度
     */
    fun gaussToBlh(
        x: Double,
        y: Double,
        zone: Int,
        isSixDegree: Boolean = true
    ): CoordinateConverter.Coordinate {
        val yWithoutZone = y - 500000
        val centralMeridian = if (isSixDegree) {
            zone * 6 - 3
        } else {
            zone * 3
        }
        val cmRad = Math.toRadians(centralMeridian.toDouble())

        // 计算底点纬度
        val a0 = 1 - E2 / 4 - 3 * E2 * E2 / 64 - 5 * E2 * E2 * E2 / 256
        val Bf = x / A / a0

        var latRad = Bf
        var iteration = 0
        var latPrev = 0.0

        do {
            latPrev = latRad
            val a2 = 3 * E2 / 8 + 3 * E2 * E2 / 32 + 45 * E2 * E2 * E2 / 1024
            val a4 = 15 * E2 * E2 / 256 + 45 * E2 * E2 * E2 / 1024
            val a6 = 35 * E2 * E2 * E2 / 3072

            val B = A * (a0 * latRad - a2 * sin(2 * latRad) + a4 * sin(4 * latRad) - a6 * sin(6 * latRad))
            latRad = Bf + (x - B) / A / a0
            iteration++
        } while (abs(latRad - latPrev) > 1e-10 && iteration < 100)

        val t = tan(latRad)
        val eta2 = E2 / (1 - E2) * cos(latRad) * cos(latRad)
        val N = A / sqrt(1 - E2 * sin(latRad) * sin(latRad))

        val lat = Math.toDegrees(
            latRad -
                    t / (2 * N * N) * yWithoutZone * yWithoutZone +
                    t / (24 * N * N * N * N) * (5 + 3 * t * t + eta2 - 9 * eta2 * t * t) * yWithoutZone * yWithoutZone * yWithoutZone * yWithoutZone -
                    t / (720 * N * N * N * N * N * N) * (61 + 90 * t * t + 45 * t * t * t * t) * yWithoutZone * yWithoutZone * yWithoutZone * yWithoutZone * yWithoutZone * yWithoutZone
        )

        val lon = Math.toDegrees(
            cmRad +
                    1 / (N * cos(latRad)) * yWithoutZone -
                    1 / (6 * N * N * N * cos(latRad)) * (1 + 2 * t * t + eta2) * yWithoutZone * yWithoutZone * yWithoutZone +
                    1 / (120 * N * N * N * N * N * cos(latRad)) * (5 + 28 * t * t + 24 * t * t * t * t) * yWithoutZone * yWithoutZone * yWithoutZone * yWithoutZone * yWithoutZone
        )

        return CoordinateConverter.Coordinate(
            latitude = lat,
            longitude = lon,
            system = CoordinateConverter.CoordinateSystem.WGS84
        )
    }

    /**
     * 计算带号
     */
    fun getZone(lon: Double, isSixDegree: Boolean = true): Int {
        return if (isSixDegree) {
            ((lon + 3) / 6).toInt()
        } else {
            ((lon + 1.5) / 3).toInt()
        }
    }

    /**
     * 计算中央子午线
     */
    fun getCentralMeridian(zone: Int, isSixDegree: Boolean = true): Double {
        return if (isSixDegree) {
            zone * 6 - 3.0
        } else {
            zone * 3.0
        }
    }

    /**
     * ⭐ 使用自定义带号和中央子午线进行高斯投影
     * @param lat 纬度 (度)
     * @param lon 经度 (度)
     * @param zone 自定义带号
     * @param centralMeridian 自定义中央子午线 (度)
     */
    fun blhToGaussWithCustom(
        lat: Double,
        lon: Double,
        zone: Int,
        centralMeridian: Double
    ): GaussCoord {
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        val cmRad = Math.toRadians(centralMeridian)
        val dLon = lonRad - cmRad

        // 计算辅助量
        val a0 = 1 - E2 / 4 - 3 * E2 * E2 / 64 - 5 * E2 * E2 * E2 / 256
        val a2 = 3 * E2 / 8 + 3 * E2 * E2 / 32 + 45 * E2 * E2 * E2 / 1024
        val a4 = 15 * E2 * E2 / 256 + 45 * E2 * E2 * E2 / 1024
        val a6 = 35 * E2 * E2 * E2 / 3072

        // 子午线弧长
        val B = A * (a0 * latRad - a2 * sin(2 * latRad) + a4 * sin(4 * latRad) - a6 * sin(6 * latRad))

        val t = tan(latRad)
        val eta2 = E2 / (1 - E2) * cos(latRad) * cos(latRad)
        val N = A / sqrt(1 - E2 * sin(latRad) * sin(latRad))

        val x = B + N / 2 * t * dLon * dLon +
                N / 24 * t * (5 - t * t + 9 * eta2 + 4 * eta2 * eta2) * dLon * dLon * dLon * dLon +
                N / 720 * t * (61 - 58 * t * t + t * t * t * t) * dLon * dLon * dLon * dLon * dLon * dLon

        val y = N * dLon +
                N / 6 * (1 - t * t + eta2) * dLon * dLon * dLon +
                N / 120 * (5 - 18 * t * t + t * t * t * t + 14 * eta2 - 58 * eta2 * t * t) * dLon * dLon * dLon * dLon * dLon

        val yFinal = y + 500000

        return GaussCoord(
            x = x,
            y = yFinal,
            zone = zone,
            centralMeridian = centralMeridian
        )
    }
}
