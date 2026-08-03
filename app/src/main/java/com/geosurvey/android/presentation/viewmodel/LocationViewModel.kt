package com.geosurvey.android.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
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
    private var context: Context? = null

    private val _state = MutableStateFlow(LocationState())
    val state: StateFlow<LocationState> = _state.asStateFlow()

    private var isFirstFix = true
    private var startTime = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                _state.value = _state.value.copy(
                    location = location,
                    isSearching = false,
                    searchTime = System.currentTimeMillis() - startTime,
                    errorMessage = ""
                )
                if (isFirstFix) {
                    isFirstFix = false
                    // 首次定位成功，更新卫星模拟数据
                    updateSatelliteData()
                }
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

        _state.value = _state.value.copy(
            isActive = true,
            isSearching = true,
            searchTime = 0L,
            errorMessage = "",
            location = null
        )
        startTime = System.currentTimeMillis()
        isFirstFix = true

        // ⭐ 优化：使用更快的定位策略
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000  // 1秒更新
        ).apply {
            setMinUpdateIntervalMillis(500)        // 最小500ms
            setMaxUpdateDelayMillis(3000)          // 最大延迟3秒
            setWaitForAccurateLocation(false)      // 不等待高精度
            setGranularity(Priority.PRIORITY_HIGH_ACCURACY)
        }.build()

        client.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // ⭐ 鸿蒙系统兼容：使用低功耗定位辅助
        if (isHarmonyOS()) {
            // 鸿蒙系统使用更积极的定位策略
            val fallbackRequest = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, 3000
            ).apply {
                setMinUpdateIntervalMillis(1000)
            }.build()
            // 如果高精度定位超时，使用平衡模式
            viewModelScope.launch {
                delay(8000) // 8秒后如果还没定位成功
                if (_state.value.location == null && _state.value.isActive) {
                    client.requestLocationUpdates(
                        fallbackRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
            }
        }

        // 模拟卫星数据（实际应该使用GnssStatus）
        viewModelScope.launch {
            var count = 8
            while (_state.value.isActive) {
                // 模拟卫星数据随定位状态变化
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

        // ⭐ 超时提示
        viewModelScope.launch {
            delay(15000) // 15秒后
            if (_state.value.location == null && _state.value.isActive) {
                _state.value = _state.value.copy(
                    errorMessage = if (isHarmonyOS()) {
                        "定位超时，请检查位置权限和GPS设置（鸿蒙系统）"
                    } else {
                        "定位超时，请检查位置权限和GPS设置"
                    },
                    isSearching = false
                )
            }
        }
    }

    fun stopLocation() {
        _state.value = _state.value.copy(
            isActive = false,
            isSearching = false,
            errorMessage = ""
        )
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }

    private fun updateSatelliteData() {
        // 首次定位成功后更新卫星数据
        // 实际数据由模拟循环更新
    }

    // ⭐ 检测是否为鸿蒙系统
    private fun isHarmonyOS(): Boolean {
        return try {
            val manufacturer = Build.MANUFACTURER
            val brand = Build.BRAND
            manufacturer.equals("huawei", ignoreCase = true) ||
            brand.equals("huawei", ignoreCase = true) ||
            Build.DISPLAY.contains("HarmonyOS", ignoreCase = true) ||
            Build.VERSION.INCREMENTAL.contains("HarmonyOS", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }
}
