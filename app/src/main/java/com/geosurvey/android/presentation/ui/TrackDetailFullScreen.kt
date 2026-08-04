// 修正统计计算函数
fun calculateTrackStats(points: List<TrackPoint>): TrackStats {
    if (points.isEmpty()) {
        return TrackStats(
            totalDistance = 0.0,
            totalTime = "00:00:00",
            pointCount = 0,
            avgSpeed = 0.0,
            maxSpeed = 0.0,
            maxAltitude = 0.0,
            minAltitude = 0.0,
            totalAscent = 0.0,
            totalDescent = 0.0
        )
    }

    // ⭐ 修正总距离计算 - 使用更精确的Haversine公式
    var totalDistance = 0.0
    var lastValidPoint: TrackPoint? = null
    for (point in points) {
        if (lastValidPoint != null) {
            val p1 = lastValidPoint!!
            val p2 = point
            val lat1 = Math.toRadians(p1.latitude)
            val lon1 = Math.toRadians(p1.longitude)
            val lat2 = Math.toRadians(p2.latitude)
            val lon2 = Math.toRadians(p2.longitude)
            val dLat = lat2 - lat1
            val dLon = lon2 - lon1
            val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                    kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
                    kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
            val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
            totalDistance += 6378137.0 * c
        }
        lastValidPoint = point
    }

    // ⭐ 修正用时计算
    val startTime = points.first().timestamp
    val endTime = points.last().timestamp
    val diff = endTime - startTime
    val hours = diff / 3600000
    val minutes = (diff % 3600000) / 60000
    val seconds = (diff % 60000) / 1000
    val totalTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    // ⭐ 修正速度计算 - 基于总距离和总时间
    val timeHours = diff / 3600000.0
    val avgSpeed = if (timeHours > 0) (totalDistance / 1000) / timeHours else 0.0

    // 最大速度
    var maxSpeed = 0.0
    for (point in points) {
        point.speed?.let {
            val speedKmh = it * 3.6
            if (speedKmh > maxSpeed) maxSpeed = speedKmh
        }
    }

    // ⭐ 修正海拔计算
    var maxAltitude = -Double.MAX_VALUE
    var minAltitude = Double.MAX_VALUE
    var hasAltitude = false
    for (point in points) {
        point.altitude?.let {
            hasAltitude = true
            if (it > maxAltitude) maxAltitude = it
            if (it < minAltitude) minAltitude = it
        }
    }
    if (!hasAltitude) {
        maxAltitude = 0.0
        minAltitude = 0.0
    }

    // ⭐ 修正累计爬升/下降 - 只累计有效变化
    var totalAscent = 0.0
    var totalDescent = 0.0
    for (i in 1 until points.size) {
        val p1 = points[i - 1]
        val p2 = points[i]
        if (p1.altitude != null && p2.altitude != null) {
            val diffAlt = p2.altitude - p1.altitude
            if (diffAlt > 1.0) {  // 只累计超过1米的变化
                totalAscent += diffAlt
            } else if (diffAlt < -1.0) {
                totalDescent += kotlin.math.abs(diffAlt)
            }
        }
    }

    return TrackStats(
        totalDistance = totalDistance,
        totalTime = totalTime,
        pointCount = points.size,
        avgSpeed = avgSpeed,
        maxSpeed = maxSpeed,
        maxAltitude = maxAltitude,
        minAltitude = minAltitude,
        totalAscent = totalAscent,
        totalDescent = totalDescent
    )
}
