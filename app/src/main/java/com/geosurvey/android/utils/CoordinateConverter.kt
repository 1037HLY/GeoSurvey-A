package com.geosurvey.android.utils

import kotlin.math.*

/**
 * 坐标转换工具
 * 支持 WGS84 ↔ CGCS2000 转换
 */
object CoordinateConverter {

    // WGS84 椭球参数
    private const val WGS84_A = 6378137.0
    private const val WGS84_F = 1.0 / 298.257223563
    private val WGS84_B = WGS84_A * (1 - WGS84_F)
    private val WGS84_E2 = 2 * WGS84_F - WGS84_F * WGS84_F

    // CGCS2000 椭球参数
    private const val CGCS2000_A = 6378137.0
    private const val CGCS2000_F = 1.0 / 298.257222101
    private val CGCS2000_B = CGCS2000_A * (1 - CGCS2000_F)
    private val CGCS2000_E2 = 2 * CGCS2000_F - CGCS2000_F * CGCS2000_F

    // 七参数转换 (WGS84 → CGCS2000)
    // 实际应用中需要根据地区获取准确的七参数
    private val DX = 0.0
    private val DY = 0.0
    private val DZ = 0.0
    private val RX = 0.0
    private val RY = 0.0
    private val RZ = 0.0
    private val SCALE = 0.0

    /**
     * 坐标数据类
     */
    data class Coordinate(
        val latitude: Double,   // 纬度 (度)
        val longitude: Double,  // 经度 (度)
        val altitude: Double? = null, // 海拔 (米)
        val system: CoordinateSystem = CoordinateSystem.WGS84
    )

    enum class CoordinateSystem {
        WGS84, CGCS2000, GCJ02, BD09
    }

    /**
     * 高斯投影坐标
     */
    data class GaussCoordinate(
        val x: Double,  // 北坐标 (米)
        val y: Double,  // 东坐标 (米)
        val zone: Int,  // 带号
        val centralMeridian: Double // 中央子午线
    )

    /**
     * WGS84 → CGCS2000 转换 (七参数法)
     */
    fun wgs84ToCgcs2000(lat: Double, lon: Double, alt: Double = 0.0): Coordinate {
        // 1. WGS84 经纬度 → 空间直角坐标
        val (x, y, z) = blhToXyz(lat, lon, alt, WGS84_A, WGS84_E2)

        // 2. 七参数转换
        val dx = DX + (1 + SCALE) * (x + RZ * y - RY * z)
        val dy = DY + (1 + SCALE) * (-RZ * x + y + RX * z)
        val dz = DZ + (1 + SCALE) * (RY * x - RX * y + z)

        // 3. 空间直角坐标 → CGCS2000 经纬度
        return xyzToBlh(dx, dy, dz, CGCS2000_A, CGCS2000_E2)
    }

    /**
     * CGCS2000 → WGS84 转换 (七参数法)
     */
    fun cgcs2000ToWgs84(lat: Double, lon: Double, alt: Double = 0.0): Coordinate {
        // 1. CGCS2000 经纬度 → 空间直角坐标
        val (x, y, z) = blhToXyz(lat, lon, alt, CGCS2000_A, CGCS2000_E2)

        // 2. 七参数逆转换
        val dx = DX + (1 + SCALE) * (x + RZ * y - RY * z)
        val dy = DY + (1 + SCALE) * (-RZ * x + y + RX * z)
        val dz = DZ + (1 + SCALE) * (RY * x - RX * y + z)

        // 3. 空间直角坐标 → WGS84 经纬度
        return xyzToBlh(dx, dy, dz, WGS84_A, WGS84_E2)
    }

    /**
     * 经纬度 → 空间直角坐标
     */
    private fun blhToXyz(lat: Double, lon: Double, alt: Double, a: Double, e2: Double): Triple<Double, Double, Double> {
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)

        val N = a / sqrt(1 - e2 * sin(latRad) * sin(latRad))
        val x = (N + alt) * cos(latRad) * cos(lonRad)
        val y = (N + alt) * cos(latRad) * sin(lonRad)
        val z = (N * (1 - e2) + alt) * sin(latRad)

        return Triple(x, y, z)
    }

    /**
     * 空间直角坐标 → 经纬度
     */
    private fun xyzToBlh(x: Double, y: Double, z: Double, a: Double, e2: Double): Coordinate {
        val lon = Math.toDegrees(atan2(y, x))

        val p = sqrt(x * x + y * y)
        var lat = Math.toDegrees(atan2(z, p * (1 - e2)))
        var iteration = 0
        var latPrev = 0.0

        do {
            latPrev = lat
            val latRad = Math.toRadians(lat)
            val N = a / sqrt(1 - e2 * sin(latRad) * sin(latRad))
            val h = p / cos(latRad) - N
            lat = Math.toDegrees(atan2(z, p * (1 - e2 * N / (N + h))))
            iteration++
        } while (abs(lat - latPrev) > 1e-10 && iteration < 100)

        val latRad = Math.toRadians(lat)
        val N = a / sqrt(1 - e2 * sin(latRad) * sin(latRad))
        val alt = p / cos(latRad) - N

        return Coordinate(
            latitude = lat,
            longitude = lon,
            altitude = alt
        )
    }

    /**
     * WGS84 → GCJ02 (火星坐标系)
     */
    fun wgs84ToGcj02(lat: Double, lon: Double): Coordinate {
        // 简化转换，实际需要更复杂的算法
        val dLat = transformLat(lon - 105.0, lat - 35.0)
        val dLon = transformLon(lon - 105.0, lat - 35.0)
        val radLat = Math.toRadians(lat)
        var magic = sin(radLat)
        magic = 1 - 0.006693421622965943 * magic * magic
        val sqrtMagic = sqrt(magic)
        val dLat2 = dLat * 180.0 / (6378245.0 * (1 - 0.006693421622965943) / (magic * sqrtMagic) * PI)
        val dLon2 = dLon * 180.0 / (6378245.0 / sqrtMagic * cos(radLat) * PI)
        return Coordinate(
            latitude = lat + dLat2,
            longitude = lon + dLon2,
            system = CoordinateSystem.GCJ02
        )
    }

    /**
     * GCJ02 → WGS84
     */
    fun gcj02ToWgs84(lat: Double, lon: Double): Coordinate {
        val coord = wgs84ToGcj02(lat, lon)
        return Coordinate(
            latitude = 2 * lat - coord.latitude,
            longitude = 2 * lon - coord.longitude,
            system = CoordinateSystem.WGS84
        )
    }

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLon(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }

    /**
     * 角度转弧度
     */
    fun degToRad(deg: Double): Double = deg * PI / 180.0

    /**
     * 弧度转角度
     */
    fun radToDeg(rad: Double): Double = rad * 180.0 / PI

    /**
     * 计算两点距离 (米)
     */
    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val latRad1 = degToRad(lat1)
        val latRad2 = degToRad(lat2)
        val dLat = degToRad(lat2 - lat1)
        val dLon = degToRad(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(latRad1) * cos(latRad2) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return 6378137.0 * c
    }

    /**
     * 获取坐标系统名称
     */
    fun getSystemName(system: CoordinateSystem): String {
        return when (system) {
            CoordinateSystem.WGS84 -> "WGS84"
            CoordinateSystem.CGCS2000 -> "CGCS2000"
            CoordinateSystem.GCJ02 -> "GCJ02 (火星)"
            CoordinateSystem.BD09 -> "BD09 (百度)"
        }
    }

    /**
     * 格式化坐标显示
     */
    fun formatCoordinate(lat: Double, lon: Double): String {
        return String.format("%.6f°, %.6f°", lat, lon)
    }

    /**
     * 转换为度分秒格式
     */
    fun toDMS(degrees: Double): String {
        val d = degrees.toInt()
        val m = ((degrees - d) * 60).toInt()
        val s = ((degrees - d - m / 60.0) * 3600)
        return String.format("%d°%d′%.2f″", d, m, s)
    }
}
