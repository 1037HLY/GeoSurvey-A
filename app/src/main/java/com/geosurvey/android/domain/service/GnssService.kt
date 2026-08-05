package com.geosurvey.android.domain.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * GNSS高精度定位服务
 * 支持多星座卫星搜星
 */
class GnssService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val locationChannel = Channel<Location>(Channel.BUFFERED)
    val locationFlow: Flow<Location> = locationChannel.receiveAsFlow()

    private val satelliteChannel = Channel<SatelliteInfo>(Channel.BUFFERED)
    val satelliteFlow: Flow<SatelliteInfo> = satelliteChannel.receiveAsFlow()

    private var isRunning = false

    // ⭐ GNSS状态监听（多星座）
    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val info = parseSatelliteStatus(status)
            satelliteChannel.trySend(info)
        }

        override fun onFirstFix(ttffMillis: Int) {
            // 首次定位成功
        }

        override fun onStarted() {
            // GNSS启动
        }

        override fun onStopped() {
            // GNSS停止
        }
    }

    // 位置回调
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                locationChannel.trySend(location)
            }
        }
    }

    fun startUpdates() {
        if (isRunning) return
        if (!hasLocationPermission()) return

        isRunning = true

        // ⭐ 注册GNSS状态监听（获取卫星信息）
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.registerGnssStatusCallback(gnssStatusCallback, null)
        }

        // ⭐ 高精度位置请求
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 500  // 500ms更新
        ).apply {
            setMinUpdateIntervalMillis(500)
            setMaxUpdateDelayMillis(2000)
            setWaitForAccurateLocation(true)      // 等待高精度
        }.build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        // ⭐ 同时使用系统GPS作为备份（提高搜星能力）
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        500L,
                        0f,
                        systemLocationListener,
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (e: Exception) {
            // 忽略
        }
    }

    // ⭐ 系统GPS备用监听
    private val systemLocationListener = object : android.location.LocationListener {
        override fun onLocationChanged(location: Location) {
            locationChannel.trySend(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }

    fun stopUpdates() {
        isRunning = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        try {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
            locationManager.removeUpdates(systemLocationListener)
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * 解析卫星状态
     */
    private fun parseSatelliteStatus(status: GnssStatus): SatelliteInfo {
        var gpsCount = 0
        var glonassCount = 0
        var beidouCount = 0
        var galileoCount = 0
        var usedCount = 0
        var totalSnr = 0f
        var totalCount = 0

        for (i in 0 until status.satelliteCount) {
            val constellation = status.getConstellationType(i)
            val snr = status.getCn0DbHz(i)
            val used = status.usedInFix(i)

            when (constellation) {
                GnssStatus.CONSTELLATION_GPS -> gpsCount++
                GnssStatus.CONSTELLATION_GLONASS -> glonassCount++
                GnssStatus.CONSTELLATION_BEIDOU -> beidouCount++
                GnssStatus.CONSTELLATION_GALILEO -> galileoCount++
                // 其他星座（QZSS, IRNSS等）不计入但计入总数
            }

            if (used) usedCount++
            if (snr > 0) {
                totalSnr += snr
                totalCount++
            }
        }

        val avgSnr = if (totalCount > 0) totalSnr / totalCount else 0f

        return SatelliteInfo(
            totalCount = status.satelliteCount,
            usedCount = usedCount,
            gpsCount = gpsCount,
            glonassCount = glonassCount,
            beidouCount = beidouCount,
            galileoCount = galileoCount,
            averageSnr = avgSnr
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * 卫星信息数据类
 */
data class SatelliteInfo(
    val totalCount: Int = 0,
    val usedCount: Int = 0,
    val gpsCount: Int = 0,
    val glonassCount: Int = 0,
    val beidouCount: Int = 0,
    val galileoCount: Int = 0,
    val averageSnr: Float = 0f
)
