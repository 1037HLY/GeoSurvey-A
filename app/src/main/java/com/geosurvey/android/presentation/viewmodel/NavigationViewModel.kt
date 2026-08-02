package com.geosurvey.android.presentation.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geosurvey.android.data.model.TrackPoint
import com.geosurvey.android.data.repository.TrackRepository
import com.geosurvey.android.utils.NavigationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NavigationState(
    val isNavigating: Boolean = false,
    val targetLocation: Location? = null,
    val targetName: String = "",
    val currentLocation: Location? = null,
    val distance: Float = 0f,
    val bearing: Float = 0f,
    val bearingToTarget: Float = 0f,
    val heading: Float = 0f,
    val isOffTrack: Boolean = false,
    val offTrackDistance: Float = 0f,
    val progress: Float = 0f,
    val guidanceText: String = "请选择导航目标",
    val directionDescription: String = "",
    val availableTargets: List<TrackPoint> = emptyList(),
    val targetIndex: Int = 0
)

class NavigationViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        @Volatile
        private var INSTANCE: NavigationViewModel? = null

        fun getInstance(application: Application): NavigationViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NavigationViewModel(application).also { INSTANCE = it }
            }
        }
    }

    private val _state = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    private var trackPoints: List<TrackPoint> = emptyList()

    fun setTrackPoints(points: List<TrackPoint>) {
        trackPoints = points
        _state.value = _state.value.copy(
            availableTargets = points
        )
    }

    fun selectTarget(index: Int) {
        if (index < 0 || index >= trackPoints.size) return

        val point = trackPoints[index]
        val location = Location("target").apply {
            latitude = point.latitude
            longitude = point.longitude
            altitude = point.altitude ?: 0.0
        }

        _state.value = _state.value.copy(
            isNavigating = true,
            targetLocation = location,
            targetName = "轨迹点 ${index + 1}",
            targetIndex = index,
            guidanceText = "导航到目标点..."
        )
    }

    fun selectTargetByName(name: String) {
        val index = trackPoints.indexOfFirst { "轨迹点 ${trackPoints.indexOf(it) + 1}" == name }
        if (index >= 0) {
            selectTarget(index)
        }
    }

    fun stopNavigation() {
        _state.value = _state.value.copy(
            isNavigating = false,
            targetLocation = null,
            targetName = "",
            guidanceText = "导航已停止"
        )
    }

    fun updateLocation(location: Location) {
        val currentState = _state.value

        // 更新当前位置
        _state.value = currentState.copy(
            currentLocation = location,
            heading = location.bearing
        )

        // 如果正在导航，更新导航数据
        val target = currentState.targetLocation
        if (currentState.isNavigating && target != null) {
            updateNavigationData(location, target)
        }
    }

    private fun updateNavigationData(current: Location, target: Location) {
        val distance = NavigationHelper.calculateDistance(
            current.latitude, current.longitude,
            target.latitude, target.longitude
        )

        val bearing = NavigationHelper.calculateBearing(
            current.latitude, current.longitude,
            target.latitude, target.longitude
        )

        val isOffTrack = NavigationHelper.checkOffTrack(current, target)
        val offTrackDistance = if (isOffTrack) {
            NavigationHelper.calculateDistance(
                current.latitude, current.longitude,
                target.latitude, target.longitude
            )
        } else 0f

        val targetDirection = NavigationHelper.calculateTargetDirection(
            current, target, current.bearing
        )

        val guidanceText = NavigationHelper.getGuidanceText(distance, bearing, isOffTrack)
        val directionDescription = NavigationHelper.getDirectionDescription(bearing)

        _state.value = _state.value.copy(
            distance = distance,
            bearing = bearing,
            bearingToTarget = targetDirection,
            isOffTrack = isOffTrack,
            offTrackDistance = offTrackDistance,
            guidanceText = guidanceText,
            directionDescription = directionDescription
        )
    }

    fun getNextTarget(): Boolean {
        val currentIndex = _state.value.targetIndex
        if (currentIndex >= 0 && currentIndex < trackPoints.size - 1) {
            selectTarget(currentIndex + 1)
            return true
        }
        return false
    }

    fun getPreviousTarget(): Boolean {
        val currentIndex = _state.value.targetIndex
        if (currentIndex > 0) {
            selectTarget(currentIndex - 1)
            return true
        }
        return false
    }

    fun clearTargets() {
        trackPoints = emptyList()
        _state.value = _state.value.copy(
            availableTargets = emptyList(),
            isNavigating = false,
            targetLocation = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        INSTANCE = null
    }
}
