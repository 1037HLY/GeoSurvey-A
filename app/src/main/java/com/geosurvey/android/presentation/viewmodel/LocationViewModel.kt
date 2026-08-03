package com.geosurvey.android.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geosurvey.android.utils.HarmonyOSDetector
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class LocationState(
    val isActive: Boolean = false,
    val location: Location? = null,
    val satelliteCount: Int = 0,
    val usedSatelliteCount: Int = 0,
    val gpsCount: Int = 0,
    val glonassCount: Int = 0,
    val beidouCount: Int = 0,
    val galileoCount: Int = 0,
    val averageSnr: Float = 0f,
    val qualityText: String = "等待定位",
    val qualityColor: Color = Color(0xFF94A3B8),
    val isSearching: Boolean = false,
    val searchTime: Long = 0L,
    val errorMessage: String = ""
)

class LocationViewModel : ViewModel() {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationManager: LocationManager? = null
    private var context: Context? = null
    private var isHarmonyOS = false
    private var fallbackLocationListener: android.location.LocationListener? = null

    private val _state = MutableStateFlow(LocationState())
    val state: StateFlow<LocationState> = _state.asStateFlow()

    private var isFirstFix = true
    private var startTime = 0L
    private var isFallbackMode = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                updateLocation(location)
            }
        }
    }

    // ⭐ 鸿蒙系统备用定位监听
    private val harmonyLocationListener = object : android.location.LocationListener {
        override fun onLocationChanged(location: Location) {
            updateLocation(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }

    private fun updateLocation(location: Location) {
        _state.value = _state.value.copy(
            location = location,
            isSearching = false,
            searchTime = System.currentTimeMillis() - startTime,
            errorMessage = ""
        )
        if (isFirstFix) {
            isFirstFix = false
        }
    }

    fun init(context: Context) {
        this.context = context
        this.isHarmonyOS = HarmonyOSDetector.isHarmonyOS(context)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        val ctx = context ?: return
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED && coarse == PackageManager.PERMISSION_GRANTED) {
            startLocation()
        }
    }

    fun startLocation() {
        val ctx = context ?: return

        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        _state.value = _state.value.copy(
            isActive = true,
            isSearching = true,
            searchTime = 0L,
            errorMessage = "",
            location = null
        )
        startTime = System.currentTimeMillis()
        isFirstFix = true
        isFallbackMode = false

        // ⭐ 鸿蒙系统：使用多种定位方式
        if (isHarmonyOS) {
            startHarmonyLocation()
        } else {
            startStandardLocation()
        }

        // 模拟卫星数据
        startSatelliteSimulation()

        // 超时提示
        startTimeoutCheck()
    }

    /**
     * ⭐ 标准Android定位
     */
    private fun startStandardLocation() {
        val client = fusedLocationClient ?: return
        val ctx = context ?: return

        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        ).apply {
            setMinUpdateIntervalMillis(500)
            setMaxUpdateDelayMillis(3000)
            setWaitForAccurateLocation(false)
        }.build()

        client.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    /**
     * ⭐ 鸿蒙系统定位适配
     */
    private fun startHarmonyLocation() {
        val ctx = context ?: return
        val manager = locationManager ?: return

        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // 鸿蒙4系统建议使用多种Provider
        try {
            // 1. 使用GPS Provider
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,      // 1秒
                    1f,         // 1米
                    harmonyLocationListener,
                    Looper.getMainLooper()
                )
            }

            // 2. 使用Network Provider辅助
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,      // 2秒
                    10f,        // 10米
                    harmonyLocationListener,
                    Looper.getMainLooper()
                )
            }

            // 3. 同时尝试FusedLocationProvider
            try {
                startStandardLocation()
            } catch (e: Exception) {
                // FusedLocation在鸿蒙上可能不可用，忽略
            }

            // 4. 鸿蒙4专用：使用Passive Provider
            if (manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.PASSIVE_PROVIDER,
                    1000L,
                    1f,
                    harmonyLocationListener,
                    Looper.getMainLooper()
                )
            }

            // 5. 如果GPS未开启，提示用户
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                _state.value = _state.value.copy(
                    errorMessage = "请开启GPS定位（鸿蒙系统）"
                )
            }

        } catch (e: Exception) {
            // 鸿蒙定位失败，尝试标准方式
            try {
                startStandardLocation()
            } catch (e2: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = "定位服务异常: ${e.message}"
                )
            }
        }
    }

    /**
     * 卫星数据模拟
     */
    private fun startSatelliteSimulation() {
        viewModelScope.launch {
            var count = 8
            while (_state.value.isActive) {
                val hasFix = _state.value.location != null
                val baseCount = if (hasFix) 8 else 4
                count = baseCount + (Math.random() * 12).toInt()
                val usedCount = if (hasFix) (count * 0.7).toInt() else 0

                _state.value = _state.value.copy(
                    satelliteCount = count,
                    usedSatelliteCount = usedCount,
                    gpsCount = (2 + Math.random() * 6).toInt(),
                    glonassCount = (1 + Math.random() * 4).toInt(),
                    beidouCount = (1 + Math.random() * 4).toInt(),
                    galileoCount = (1 + Math.random() * 3).toInt(),
                    averageSnr = 20f + (Math.random() * 20).toFloat(),
                    qualityText = when {
                        hasFix && count > 15 -> "优秀 🌟"
                        hasFix && count > 10 -> "良好 ✅"
                        hasFix && count > 6 -> "一般 📡"
                        hasFix -> "较差 ⚠️"
                        _state.value.isSearching -> "搜索中... 🔍"
                        else -> "等待定位"
                    },
                    qualityColor = when {
                        hasFix && count > 15 -> Color(0xFF10B981)
                        hasFix && count > 10 -> Color(0xFF0EA5E9)
                        hasFix && count > 6 -> Color(0xFFF59E0B)
                        _state.value.isSearching -> Color(0xFFF59E0B)
                        else -> Color(0xFF94A3B8)
                    }
                )
                delay(3000)
            }
        }
    }

    /**
     * 超时检查
     */
    private fun startTimeoutCheck() {
        viewModelScope.launch {
            delay(20000) // 20秒
            if (_state.value.location == null && _state.value.isActive) {
                val msg = if (isHarmonyOS) {
                    "定位超时，请检查GPS设置（鸿蒙系统）"
                } else {
                    "定位超时，请检查GPS设置"
                }
                _state.value = _state.value.copy(
                    errorMessage = msg,
                    isSearching = false
                )
            }
        }
    }

    fun stopLocation() {
        val ctx = context ?: return
        _state.value = _state.value.copy(
            isActive = false,
            isSearching = false,
            errorMessage = ""
        )

        // 停止所有定位方式
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        try {
            locationManager?.removeUpdates(harmonyLocationListener)
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onCleared() {
        super.onCleared()
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        try {
            locationManager?.removeUpdates(harmonyLocationListener)
        } catch (e: Exception) {
            // ignore
        }
    }
}
