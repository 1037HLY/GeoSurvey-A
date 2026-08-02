package com.geosurvey.android.presentation.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
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

    private var lastLocation: Location? = null

    // ⭐ 广播接收器 - 接收服务发送的位置更新
    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val lat = intent.getDoubleExtra("latitude", 0.0)
            val lng = intent.getDoubleExtra("longitude", 0.0)
            val alt = intent.getDoubleExtra("altitude", 0.0)
            val speed = intent.getFloatExtra("speed", 0f)
            val accuracy = intent.getFloatExtra("accuracy", 0f)
            val bearing = intent.getFloatExtra("bearing", 0f)

            if (lat != 0.0 || lng != 0.0) {
                val location = Location("gps").apply {
                    this.latitude = lat
                    this.longitude = lng
                    this.altitude = alt
                    this.speed = speed
                    this.accuracy = accuracy
                    this.bearing = bearing
                }
                addTrackPoint(location)
            }
        }
    }

    companion object {
        private const val MIN_ACCURACY = 15f
        private const val MIN_DISTANCE = 2.0
    }

    init {
        loadTrackPoints()
        // 注册广播接收器
        val filter = IntentFilter("LOCATION_UPDATE")
        getApplication<GeoSurveyApplication>().registerReceiver(locationReceiver, filter)
    }

    fun startRecording() {
        _isRecording.value = true
        lastLocation = null
    }

    fun stopRecording() {
        _isRecording.value = false
    }

    fun addTrackPoint(location: Location) {
        if (!_isRecording.value) return

        // 精度过滤
        if (location.accuracy != null && location.accuracy > MIN_ACCURACY) {
            return
        }

        // 距离过滤
        if (lastLocation != null) {
            val distance = lastLocation!!.distanceTo(location)
            if (distance < MIN_DISTANCE) {
                return
            }
        }

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

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<GeoSurveyApplication>().unregisterReceiver(locationReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }
}
