package com.geosurvey.android.presentation.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.data.model.WatermarkPhoto
import com.geosurvey.android.data.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CameraState(
    val currentLocation: Location? = null,
    val dipDirection: Float? = null,
    val dipAngle: Float? = null,
    val strike: Float? = null,
    val note: String = "",
    val locationName: String = "",
    val photos: List<WatermarkPhoto> = emptyList(),
    val photoCount: Int = 0,
    val availableDates: List<String> = emptyList()
)

class CameraViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        @Volatile
        private var INSTANCE: CameraViewModel? = null

        fun getInstance(application: Application): CameraViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CameraViewModel(application).also { INSTANCE = it }
            }
        }
    }

    private val photoRepository: PhotoRepository =
        (application as GeoSurveyApplication).photoRepository

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    init {
        loadPhotos()
    }

    fun updateLocation(location: Location) {
        _state.value = _state.value.copy(currentLocation = location)
    }

    fun updateAttitude(dipDirection: Float?, dipAngle: Float?, strike: Float?) {
        _state.value = _state.value.copy(
            dipDirection = dipDirection,
            dipAngle = dipAngle,
            strike = strike
        )
    }

    fun updateNote(note: String) {
        _state.value = _state.value.copy(note = note)
    }

    fun updateLocationName(name: String) {
        _state.value = _state.value.copy(locationName = name)
    }

    suspend fun savePhoto(imagePath: String): Boolean {
        return try {
            val state = _state.value
            val location = state.currentLocation

            val photo = WatermarkPhoto(
                imagePath = imagePath,
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0,
                altitude = location?.altitude,
                locationName = state.locationName,
                dipDirection = state.dipDirection,
                dipAngle = state.dipAngle,
                strike = state.strike,
                note = state.note,
                timestamp = System.currentTimeMillis()
            )

            photoRepository.insertPhoto(photo)
            loadPhotos()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadPhotos() {
        viewModelScope.launch {
            photoRepository.getAllPhotos().collect { photos ->
                _state.value = _state.value.copy(
                    photos = photos,
                    photoCount = photos.size
                )
            }
        }
        viewModelScope.launch {
            photoRepository.getAvailableDates().collect { dates ->
                _state.value = _state.value.copy(availableDates = dates)
            }
        }
    }

    suspend fun deleteAllPhotos() {
        photoRepository.deleteAllPhotos()
        loadPhotos()
    }

    fun getPhotoCount(): Int {
        return _state.value.photoCount
    }
}
