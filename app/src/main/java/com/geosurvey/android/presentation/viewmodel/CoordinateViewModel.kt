package com.geosurvey.android.presentation.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.data.model.CoordinateRecord
import com.geosurvey.android.data.repository.CoordinateRepository
import com.geosurvey.android.utils.CoordinateConverter
import com.geosurvey.android.utils.GaussProjection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CoordinateState(
    val currentLocation: Location? = null,
    val wgs84: CoordinateConverter.Coordinate? = null,
    val cgcs2000: CoordinateConverter.Coordinate? = null,
    val gcj02: CoordinateConverter.Coordinate? = null,
    val gaussCoord: GaussProjection.GaussCoord? = null,
    val records: List<CoordinateRecord> = emptyList(),
    val recordCount: Int = 0,
    val selectedSystem: CoordinateConverter.CoordinateSystem = CoordinateConverter.CoordinateSystem.WGS84,
    val inputLat: String = "",
    val inputLon: String = "",
    val inputAlt: String = "",
    val note: String = "",
    val locationName: String = "",
    // 自定义带号和中央子午线
    val customZone: String = "",
    val customCentralMeridian: String = "",
    val useCustomProjection: Boolean = false
)

class CoordinateViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        @Volatile
        private var INSTANCE: CoordinateViewModel? = null

        fun getInstance(application: Application): CoordinateViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CoordinateViewModel(application).also { INSTANCE = it }
            }
        }
    }

    private val coordinateRepository: CoordinateRepository =
        (application as GeoSurveyApplication).coordinateRepository

    private val _state = MutableStateFlow(CoordinateState())
    val state: StateFlow<CoordinateState> = _state.asStateFlow()

    init {
        loadRecords()
    }

    fun updateLocation(location: Location) {
        val wgs84 = CoordinateConverter.Coordinate(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            system = CoordinateConverter.CoordinateSystem.WGS84
        )

        val cgcs2000 = CoordinateConverter.wgs84ToCgcs2000(
            location.latitude,
            location.longitude,
            location.altitude ?: 0.0
        )

        val gcj02 = CoordinateConverter.wgs84ToGcj02(
            location.latitude,
            location.longitude
        )

        val gaussCoord = calculateGaussProjection(
            location.latitude,
            location.longitude
        )

        _state.value = _state.value.copy(
            currentLocation = location,
            wgs84 = wgs84,
            cgcs2000 = cgcs2000,
            gcj02 = gcj02,
            gaussCoord = gaussCoord
        )
    }

    private fun calculateGaussProjection(
        lat: Double,
        lon: Double,
        customZone: Int? = null,
        customCentralMeridian: Double? = null
    ): GaussProjection.GaussCoord? {
        return try {
            if (_state.value.useCustomProjection) {
                val zone = customZone ?: _state.value.customZone.toIntOrNull()
                val cm = customCentralMeridian ?: _state.value.customCentralMeridian.toDoubleOrNull()

                if (zone != null && cm != null) {
                    GaussProjection.blhToGaussWithCustom(
                        lat,
                        lon,
                        zone,
                        cm
                    )
                } else {
                    GaussProjection.blhToGauss(lat, lon)
                }
            } else {
                GaussProjection.blhToGauss(lat, lon)
            }
        } catch (e: Exception) {
            GaussProjection.blhToGauss(lat, lon)
        }
    }

    fun convertInput(
        lat: String,
        lon: String,
        alt: String = "0",
        system: CoordinateConverter.CoordinateSystem
    ) {
        try {
            val latVal = lat.toDoubleOrNull() ?: 0.0
            val lonVal = lon.toDoubleOrNull() ?: 0.0
            val altVal = alt.toDoubleOrNull() ?: 0.0

            if (latVal == 0.0 && lonVal == 0.0) return

            val wgs84 = when (system) {
                CoordinateConverter.CoordinateSystem.WGS84 -> CoordinateConverter.Coordinate(
                    latVal, lonVal, altVal, system
                )
                CoordinateConverter.CoordinateSystem.CGCS2000 -> {
                    val coord = CoordinateConverter.cgcs2000ToWgs84(latVal, lonVal, altVal)
                    coord
                }
                CoordinateConverter.CoordinateSystem.GCJ02 -> {
                    val coord = CoordinateConverter.gcj02ToWgs84(latVal, lonVal)
                    coord
                }
                else -> CoordinateConverter.Coordinate(latVal, lonVal, altVal, system)
            }

            val cgcs2000 = CoordinateConverter.wgs84ToCgcs2000(
                wgs84.latitude,
                wgs84.longitude,
                wgs84.altitude ?: 0.0
            )

            val gcj02 = CoordinateConverter.wgs84ToGcj02(
                wgs84.latitude,
                wgs84.longitude
            )

            val gaussCoord = calculateGaussProjection(
                wgs84.latitude,
                wgs84.longitude
            )

            _state.value = _state.value.copy(
                wgs84 = wgs84,
                cgcs2000 = cgcs2000,
                gcj02 = gcj02,
                gaussCoord = gaussCoord
            )
        } catch (e: Exception) {
            // 转换失败
        }
    }

    fun calculateWithCustomParams() {
        val state = _state.value
        val wgs84 = state.wgs84 ?: return

        try {
            val zone = state.customZone.toIntOrNull()
            val cm = state.customCentralMeridian.toDoubleOrNull()

            if (zone != null && cm != null) {
                val gaussCoord = GaussProjection.blhToGaussWithCustom(
                    wgs84.latitude,
                    wgs84.longitude,
                    zone,
                    cm
                )
                _state.value = state.copy(
                    gaussCoord = gaussCoord,
                    useCustomProjection = true
                )
            } else {
                val gaussCoord = GaussProjection.blhToGauss(
                    wgs84.latitude,
                    wgs84.longitude
                )
                _state.value = state.copy(
                    gaussCoord = gaussCoord,
                    useCustomProjection = false
                )
            }
        } catch (e: Exception) {
            val gaussCoord = GaussProjection.blhToGauss(
                wgs84.latitude,
                wgs84.longitude
            )
            _state.value = state.copy(
                gaussCoord = gaussCoord,
                useCustomProjection = false
            )
        }
    }

    fun saveRecord() {
        val state = _state.value
        val wgs84 = state.wgs84 ?: return

        viewModelScope.launch {
            val record = CoordinateRecord(
                latitude = wgs84.latitude,
                longitude = wgs84.longitude,
                altitude = wgs84.altitude,
                system = "WGS84",
                gaussX = state.gaussCoord?.x,
                gaussY = state.gaussCoord?.y,
                zone = state.gaussCoord?.zone,
                locationName = state.locationName,
                note = state.note,
                timestamp = System.currentTimeMillis()
            )
            coordinateRepository.insertRecord(record)
            loadRecords()
            _state.value = state.copy(
                note = "",
                locationName = "",
                inputLat = "",
                inputLon = "",
                inputAlt = ""
            )
        }
    }

    fun loadRecords() {
        viewModelScope.launch {
            coordinateRepository.getAllRecords().collect { records ->
                _state.value = _state.value.copy(
                    records = records,
                    recordCount = records.size
                )
            }
        }
    }

    suspend fun deleteAllRecords() {
        coordinateRepository.deleteAllRecords()
        loadRecords()
    }

    fun selectSystem(system: CoordinateConverter.CoordinateSystem) {
        _state.value = _state.value.copy(selectedSystem = system)
    }

    fun updateInputLat(value: String) {
        _state.value = _state.value.copy(inputLat = value)
    }

    fun updateInputLon(value: String) {
        _state.value = _state.value.copy(inputLon = value)
    }

    fun updateInputAlt(value: String) {
        _state.value = _state.value.copy(inputAlt = value)
    }

    fun updateNote(value: String) {
        _state.value = _state.value.copy(note = value)
    }

    fun updateLocationName(value: String) {
        _state.value = _state.value.copy(locationName = value)
    }

    // 自定义参数更新
    fun updateCustomZone(value: String) {
        _state.value = _state.value.copy(customZone = value)
    }

    fun updateCustomCentralMeridian(value: String) {
        _state.value = _state.value.copy(customCentralMeridian = value)
    }

    fun toggleUseCustomProjection() {
        _state.value = _state.value.copy(
            useCustomProjection = !_state.value.useCustomProjection
        )
        if (_state.value.useCustomProjection) {
            calculateWithCustomParams()
        } else {
            val wgs84 = _state.value.wgs84
            if (wgs84 != null) {
                val gaussCoord = GaussProjection.blhToGauss(
                    wgs84.latitude,
                    wgs84.longitude
                )
                _state.value = _state.value.copy(gaussCoord = gaussCoord)
            }
        }
    }
}
