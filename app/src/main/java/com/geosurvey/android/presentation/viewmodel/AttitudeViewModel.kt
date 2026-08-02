package com.geosurvey.android.presentation.viewmodel

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.data.model.AttitudeRecord
import com.geosurvey.android.data.repository.AttitudeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AttitudeState(
    val dipDirection: Float = 0f,   // 倾向
    val dipAngle: Float = 0f,       // 倾角
    val strike: Float = 0f,         // 走向
    val isMeasuring: Boolean = false,
    val currentLocation: Location? = null,
    val records: List<AttitudeRecord> = emptyList(),
    val recordCount: Int = 0,
    val availableDates: List<String> = emptyList()
)

class AttitudeViewModel(
    application: Application
) : AndroidViewModel(application), SensorEventListener {

    companion object {
        @Volatile
        private var INSTANCE: AttitudeViewModel? = null

        fun getInstance(application: Application): AttitudeViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AttitudeViewModel(application).also { INSTANCE = it }
            }
        }
    }

    private val attitudeRepository: AttitudeRepository =
        (application as GeoSurveyApplication).attitudeRepository

    private val sensorManager: SensorManager =
        application.getSystemService(Application.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private val _state = MutableStateFlow(AttitudeState())
    val state: StateFlow<AttitudeState> = _state.asStateFlow()

    init {
        loadRecords()
    }

    fun startMeasuring() {
        _state.value = _state.value.copy(isMeasuring = true)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopMeasuring() {
        _state.value = _state.value.copy(isMeasuring = false)
        sensorManager.unregisterListener(this)
    }

    fun updateLocation(location: Location) {
        _state.value = _state.value.copy(currentLocation = location)
    }

    fun saveRecord(note: String = "") {
        val currentState = _state.value
        val location = currentState.currentLocation

        viewModelScope.launch {
            val record = AttitudeRecord(
                dipDirection = currentState.dipDirection,
                dipAngle = currentState.dipAngle,
                strike = currentState.strike,
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0,
                altitude = location?.altitude,
                accuracy = location?.accuracy,
                note = note,
                timestamp = System.currentTimeMillis()
            )
            attitudeRepository.insertRecord(record)
            loadRecords()
        }
    }

    fun loadRecords() {
        viewModelScope.launch {
            attitudeRepository.getAllRecords().collect { records ->
                _state.value = _state.value.copy(
                    records = records,
                    recordCount = records.size
                )
            }
        }
        viewModelScope.launch {
            attitudeRepository.getAvailableDates().collect { dates ->
                _state.value = _state.value.copy(availableDates = dates)
            }
        }
    }

    suspend fun deleteAllRecords() {
        attitudeRepository.deleteAllRecords()
        loadRecords()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravity, 0, 3)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagnetic, 0, 3)
            }
        }

        SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
        SensorManager.getOrientation(rotationMatrix, orientation)

        // 计算产状
        val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

        // 倾向：azimuth 归一化到 0-360
        val dipDirection = (azimuth + 360) % 360

        // 倾角：pitch 取绝对值，范围 0-90
        val dipAngle = Math.abs(pitch)

        // 走向：倾向 ± 90 度
        val strike = (dipDirection + 90) % 360

        _state.value = _state.value.copy(
            dipDirection = dipDirection,
            dipAngle = dipAngle,
            strike = strike
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 不处理
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }
}
