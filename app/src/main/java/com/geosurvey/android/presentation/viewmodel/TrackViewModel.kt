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
import com.geosurvey.android.data.model.TrackPoint
import com.geosurvey.android.data.repository.TrackRepository
import com.geosurvey.android.utils.PrecisionOptimizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        @Volatile
        private var INSTANCE: TrackViewModel? = null

        fun getInstance(application: Application): TrackViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TrackViewModel(application).also { INSTANCE = it }
            }
        }

        // 轨迹记录参数
        private const val MIN_RECORD_DISTANCE = 6.0
        private const val MAX_JUMP_DISTANCE = 20.0
        private const val MIN_ACCURACY = 15f
        private const val STATIC_DISTANCE_THRESHOLD = 1.0
        private const val MAX_SPEED_THRESHOLD = 0.5
    }

    private val trackRepository: TrackRepository =
        (application as GeoSurveyApplication).trackRepository

    private val precisionOptimizer = PrecisionOptimizer()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _trackPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()

    private val _pointCount = MutableStateFlow(0)
    val pointCount: StateFlow<Int> = _pointCount.asStateFlow()

    private val _availableDates = MutableStateFlow<List<String>>(emptyList())
    val availableDates: StateFlow<List<String>> = _availableDates.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    private val _filteredPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val filteredPoints: StateFlow<List<TrackPoint>> = _filteredPoints.asStateFlow()

    private var lastLocation: Location? = null
    private var isReceiverRegistered = false

    // ⭐ 广播接收器 - 在后台持续接收位置更新
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

    init {
        loadTrackPoints()
        loadAvailableDates()
        registerReceiver()
    }

    // ⭐ 注册广播接收器 - 确保页面切换不中断
    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter("LOCATION_UPDATE")
                getApplication<GeoSurveyApplication>().registerReceiver(locationReceiver, filter)
                isReceiverRegistered = true
            } catch (e: Exception) { }
        }
    }

    fun startRecording() {
        _isRecording.value = true
        lastLocation = null
        precisionOptimizer.reset()
    }

    fun stopRecording() {
        _isRecording.value = false
    }

    /**
     * 优化的轨迹点添加方法
     */
    fun addTrackPoint(location: Location) {
        if (!_isRecording.value) return

        // 1. 精度过滤
        if (location.accuracy != null && location.accuracy > MIN_ACCURACY) {
            return
        }

        // 2. 跳点检测
        if (lastLocation != null) {
            val timeDiff = location.time - lastLocation!!.time
            val distance = lastLocation!!.distanceTo(location)
            
            if (timeDiff < 3000 && distance > MAX_JUMP_DISTANCE) {
                lastLocation = location
                return
            }
            
            // 3. 每6米记录一个点
            if (distance < MIN_RECORD_DISTANCE) {
                return
            }
        }

        // 4. 静止检测
        if (location.speed != null && location.speed < MAX_SPEED_THRESHOLD) {
            if (lastLocation != null) {
                val distance = lastLocation!!.distanceTo(location)
                if (distance < STATIC_DISTANCE_THRESHOLD) {
                    return
                }
            }
        }

        // 5. 保存轨迹点
        viewModelScope.launch {
            val point = TrackPoint(
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
            loadAvailableDates()
            _selectedDate.value?.let { filterByDate(it) }
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

    fun loadAvailableDates() {
        viewModelScope.launch {
            trackRepository.getAvailableDates().collect { dates ->
                _availableDates.value = dates
            }
        }
    }

    fun filterByDate(date: String) {
        _selectedDate.value = date
        viewModelScope.launch {
            trackRepository.getTrackPointsByDate(date).collect { points ->
                _filteredPoints.value = points
            }
        }
    }

    fun clearDateFilter() {
        _selectedDate.value = null
        _filteredPoints.value = emptyList()
    }

    suspend fun deleteTrackPointsByDate(date: String): Boolean {
        return try {
            trackRepository.deleteTrackPointsByDate(date)
            loadTrackPoints()
            loadAvailableDates()
            if (_selectedDate.value == date) {
                clearDateFilter()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteAllTrackPoints(): Boolean {
        return try {
            trackRepository.deleteAllTrackPoints()
            loadTrackPoints()
            loadAvailableDates()
            clearDateFilter()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getDisplayPoints(): List<TrackPoint> {
        return if (_selectedDate.value != null) _filteredPoints.value else _trackPoints.value
    }

    override fun onCleared() {
        super.onCleared()
        try {
            if (isReceiverRegistered) {
                getApplication<GeoSurveyApplication>().unregisterReceiver(locationReceiver)
                isReceiverRegistered = false
            }
        } catch (e: Exception) { }
    }
}
