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

    private val _state = MutableStateFlow(LocationState())
    val state: StateFlow<LocationState> = _state.asStateFlow()

    private var isFirstFix = true
    private var startTime = 0L
    private var isLocationStarted = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                updateLocation(location)
            }
        }
    }

    private val systemLocationListener = object : android.location.LocationListener {
        override fun onLocationChanged(location: Location) {
            updateLocation(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {
            _state.value = _state.value.copy(
                errorMessage = "GPS已禁用，请开启GPS定位"
            )
        }
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
        this.isHarmonyOS = HarmonyOSDetector.isHarmonyOS()
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
        val manager = locationManager ?: return

        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        if (isLocationStarted) return
        isLocationStarted = true

        _state.value = _state.value.copy(
            isActive = true,
            isSearching = true,
            searchTime = 0L,
            errorMessage = "",
            location = null
        )
        startTime = System.currentTimeMillis()
        isFirstFix = true

        if (isHarmonyOS) {
            startHarmonyOSLocation()
        } else {
            startAndroidLocation()
        }

        startSatelliteSimulation()
        startTimeoutCheck()
        
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            _state.value = _state.value.copy(
                errorMessage = if (isHarmonyOS) {
                    "请开启GPS定位（鸿蒙系统设置）"
                } else {
                    "请开启GPS定位"
                }
            )
        }
    }

    private fun startHarmonyOSLocation() {
        val ctx = context ?: return
        val manager = locationManager ?: return

        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    systemLocationListener,
                    Looper.getMainLooper()
                )
            }

            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    0f,
                    systemLocationListener,
                    Looper.getMainLooper()
                )
            }

            try {
                startAndroidLocation()
            } catch (e: Exception) { }

            val lastKnownGps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastKnownNetwork = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            
            val bestLocation = if (lastKnownGps != null) lastKnownGps else lastKnownNetwork
            bestLocation?.let {
                updateLocation(it)
            }

        } catch (e: Exception) {
            try {
                startAndroidLocation()
            } catch (e2: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = "定位服务异常，请重启应用"
                )
            }
        }
    }

    private fun startAndroidLocation() {
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

    private fun startSatelliteSimulation() {
        viewModelScope.launch {
            while (_state.value.isActive) {
                val hasFix = _state.value.location != null
                
                val gps = (2 + Math.random() * 6).toInt()
                val glonass = (1 + Math.random() * 4).toInt()
                val beidou = (1 + Math.random() * 4).toInt()
                val galileo = (1 + Math.random() * 3).toInt()
                val total = gps + glonass + beidou + galileo
                
                val usedCount = if (hasFix) (total * 0.7).toInt() else 0

                _state.value = _state.value.copy(
                    satelliteCount = total,
                    usedSatelliteCount = usedCount,
                    gpsCount = gps,
                    glonassCount = glonass,
                    beidouCount = beidou,
                    galileoCount = galileo,
                    averageSnr = 20f + (Math.random() * 20).toFloat(),
                    qualityText = when {
                        hasFix && total > 15 -> "优秀 🌟"
                        hasFix && total > 10 -> "良好 ✅"
                        hasFix && total > 6 -> "一般 📡"
                        hasFix -> "较差 ⚠️"
                        _state.value.isSearching -> "搜索中... 🔍"
                        else -> "等待定位"
                    },
                    qualityColor = when {
                        hasFix && total > 15 -> Color(0xFF10B981)
                        hasFix && total > 10 -> Color(0xFF0EA5E9)
                        hasFix && total > 6 -> Color(0xFFF59E0B)
                        _state.value.isSearching -> Color(0xFFF59E0B)
                        else -> Color(0xFF94A3B8)
                    }
                )
                delay(3000)
            }
        }
    }

    private fun startTimeoutCheck() {
        viewModelScope.launch {
            delay(60000)
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
        isLocationStarted = false
        _state.value = _state.value.copy(
            isActive = false,
            isSearching = false,
            errorMessage = ""
        )

        fusedLocationClient?.removeLocationUpdates(locationCallback)
        try {
            locationManager?.removeUpdates(systemLocationListener)
        } catch (e: Exception) { }
    }

    override fun onCleared() {
        super.onCleared()
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        try {
            locationManager?.removeUpdates(systemLocationListener)
        } catch (e: Exception) { }
    }
}
