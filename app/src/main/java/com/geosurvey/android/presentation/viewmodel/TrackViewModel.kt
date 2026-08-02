package com.geosurvey.android.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.data.model.TrackPointEntity
import com.geosurvey.android.data.repository.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val trackRepository: TrackRepository =
        (application as GeoSurveyApplication).trackRepository

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _trackPoints = MutableStateFlow<List<TrackPointEntity>>(emptyList())
    val trackPoints: StateFlow<List<TrackPointEntity>> = _trackPoints.asStateFlow()

    private val _pointCount = MutableStateFlow(0)
    val pointCount: StateFlow<Int> = _pointCount.asStateFlow()

    // 轨迹优化参数
    private var lastLocation: android.location.Location? = null

    companion object {
        private const val MIN_ACCURACY = 15f      // 最小精度要求（米）
        private const val MIN_DISTANCE = 2.0      // 最小移动距离（米）
    }

    init {
        loadTrackPoints()
    }

    fun startRecording() {
        _isRecording.value = true
        lastLocation = null
    }

    fun stopRecording() {
        _isRecording.value = false
    }

    fun addTrackPoint(location: android.location.Location) {
        if (!_isRecording.value) return

        // 1. 精度过滤
        if (location.accuracy != null && location.accuracy > MIN_ACCURACY) {
            return
        }

        // 2. 距离过滤（防止静止时产生多个点）
        if (lastLocation != null) {
            val distance = lastLocation!!.distanceTo(location)
            if (distance < MIN_DISTANCE) {
                return
            }
        }

        // 保存轨迹点
        viewModelScope.launch {
            val point = TrackPointEntity(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                speed = location.speed,
                bearing = location.bearing,
                accuracy = location.accuracy,
                timestamp = System.currentTimeMillis()
            )
            trackRepository.insertTrackPoint(point)
            lastLocation = location
            loadTrackPoints()
        }
    }

    fun loadTrackPoints() {
        viewModelScope.launch {
            trackRepository.getAllTrackPoints().collect { points ->
                _trackPoints.value = points
                _pointCount.value = points.size
            }
        }
    }

    fun deleteAllTrackPoints() {
        viewModelScope.launch {
            trackRepository.deleteAllTrackPoints()
            loadTrackPoints()
        }
    }
}
