package com.geosurvey.android.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val qualityColor: Color = Color(0xFF94A3B8)
)

class LocationViewModel : ViewModel() {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var context: Context? = null

    private val _state = MutableStateFlow(LocationState())
    val state: StateFlow<LocationState> = _state.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                _state.value = _state.value.copy(
                    location = location
                )
            }
        }
    }

    fun init(context: Context) {
        this.context = context
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
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
        val client = fusedLocationClient ?: return

        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        _state.value = _state.value.copy(isActive = true)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000
        ).apply {
            setMinUpdateIntervalMillis(1000)
            setMaxUpdateDelayMillis(5000)
        }.build()

        client.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // 模拟卫星数据
        viewModelScope.launch {
            while (_state.value.isActive) {
                val gps = 3 + (Math.random() * 6).toInt()
                val glonass = 1 + (Math.random() * 4).toInt()
                val beidou = 1 + (Math.random() * 4).toInt()
                val galileo = 1 + (Math.random() * 3).toInt()
                val total = gps + glonass + beidou + galileo

                _state.value = _state.value.copy(
                    satelliteCount = total,
                    usedSatelliteCount = (total * 0.6).toInt(),
                    gpsCount = gps,
                    glonassCount = glonass,
                    beidouCount = beidou,
                    galileoCount = galileo,
                    averageSnr = 20f + (Math.random() * 20).toFloat(),
                    qualityText = when {
                        total > 15 -> "优秀 🌟"
                        total > 10 -> "良好 ✅"
                        total > 6 -> "一般 📡"
                        else -> "较差 ⚠️"
                    },
                    qualityColor = when {
                        total > 15 -> Color(0xFF10B981)
                        total > 10 -> Color(0xFF0EA5E9)
                        total > 6 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                )
                kotlinx.coroutines.delay(3000)
            }
        }
    }

    fun stopLocation() {
        _state.value = _state.value.copy(isActive = false)
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }

    override fun onCleared() {
        super.onCleared()
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }
}
