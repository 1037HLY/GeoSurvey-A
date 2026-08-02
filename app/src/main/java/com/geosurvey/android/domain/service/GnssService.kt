package com.geosurvey.android.domain.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
 * 使用FusedLocationProvider + GnssStatus
 */
class GnssService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationChannel = Channel<Location>(Channel.BUFFERED)
    val locationFlow: Flow<Location> = locationChannel.receiveAsFlow()

    private val satelliteChannel = Channel<SatelliteInfo>(Channel.BUFFERED)
    val satelliteFlow: Flow<SatelliteInfo> = satelliteChannel.receiveAsFlow()

    private var isRunning = false

    // 卫星状态监听
    private val gnssStatusCallback = object : android.location.GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: android.location.GnssStatus) {
            val info = parseSatelliteStatus(status)
            satelliteChannel.trySend(info)
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

        // 注册卫星状态监听
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.registerGnssStatusCallback(gnssStatusCallback, null)
        }

        // 请求位置更新
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        ).apply {
            setMinUpdateIntervalMillis(500)
            setMaxUpdateDelayMillis(2000)
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
    }

    fun stopUpdates() {
        isRunning = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun parseSatelliteStatus(status: android.location.GnssStatus): SatelliteInfo {
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
                android.location.GnssStatus.CONSTELLATION_GPS -> gpsCount++
                android.location.GnssStatus.CONSTELLATION_GLONASS -> glonassCount++
                android.location.GnssStatus.CONSTELLATION_BEIDOU -> beidouCount++
                android.location.GnssStatus.CONSTELLATION_GALILEO -> galileoCount++
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
