package com.geosurvey.android.presentation.viewmodel

import android.content.Context
import android.location.Location
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geosurvey.android.domain.service.GnssService
import com.geosurvey.android.domain.service.SatelliteInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

class LocationViewModel(
    private val context: Context
) : ViewModel() {

    private val gnssService = GnssService(context)

    private val _state = MutableStateFlow(LocationState())
    val state: StateFlow<LocationState> = _state.asStateFlow()

    init {
        // 监听位置更新
        gnssService.locationFlow
            .onEach { location ->
                updateLocation(location)
            }
            .launchIn(viewModelScope)

        // 监听卫星更新
        gnssService.satelliteFlow
            .onEach { satelliteInfo ->
                updateSatellite(satelliteInfo)
            }
            .launchIn(viewModelScope)
    }

    fun startLocation() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isActive = true)
            gnssService.startUpdates()
        }
    }

    fun stopLocation() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isActive = false)
            gnssService.stopUpdates()
        }
    }

    private fun updateLocation(location: Location) {
        val currentState = _state.value
        _state.value = currentState.copy(
            location = location
        )
    }

    private fun updateSatellite(info: SatelliteInfo) {
        val currentState = _state.value
        val total = info.totalCount

        // 评估定位质量
        val qualityText = when {
            total > 15 -> "优秀 🌟"
            total > 10 -> "良好 ✅"
            total > 6 -> "一般 📡"
            else -> "较差 ⚠️"
        }

        val qualityColor = when {
            total > 15 -> Color(0xFF10B981)
            total > 10 -> Color(0xFF0EA5E9)
            total > 6 -> Color(0xFFF59E0B)
            else -> Color(0xFFEF4444)
        }

        _state.value = currentState.copy(
            satelliteCount = info.totalCount,
            usedSatelliteCount = info.usedCount,
            gpsCount = info.gpsCount,
            glonassCount = info.glonassCount,
            beidouCount = info.beidouCount,
            galileoCount = info.galileoCount,
            averageSnr = info.averageSnr,
            qualityText = qualityText,
            qualityColor = qualityColor
        )
    }

    override fun onCleared() {
        super.onCleared()
        gnssService.stopUpdates()
    }
}
