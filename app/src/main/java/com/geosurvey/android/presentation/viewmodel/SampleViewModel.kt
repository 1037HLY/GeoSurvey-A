package com.geosurvey.android.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geosurvey.android.GeoSurveyApplication
import com.geosurvey.android.data.model.DrillSample
import com.geosurvey.android.data.model.NormalSample
import com.geosurvey.android.data.repository.SampleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SampleState(
    val normalSamples: List<NormalSample> = emptyList(),
    val drillSamples: List<DrillSample> = emptyList(),
    val normalCount: Int = 0,
    val drillCount: Int = 0
)

class SampleViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        @Volatile
        private var INSTANCE: SampleViewModel? = null

        fun getInstance(application: Application): SampleViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SampleViewModel(application).also { INSTANCE = it }
            }
        }
    }

    private val sampleRepository: SampleRepository =
        (application as GeoSurveyApplication).sampleRepository

    private val _state = MutableStateFlow(SampleState())
    val state: StateFlow<SampleState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            sampleRepository.getAllNormalSamples().collect { samples ->
                _state.value = _state.value.copy(
                    normalSamples = samples,
                    normalCount = samples.size
                )
            }
        }
        viewModelScope.launch {
            sampleRepository.getAllDrillSamples().collect { samples ->
                _state.value = _state.value.copy(
                    drillSamples = samples,
                    drillCount = samples.size
                )
            }
        }
    }

    suspend fun insertNormalSample(sample: NormalSample) {
        sampleRepository.insertNormalSample(sample)
        loadData()
    }

    suspend fun insertDrillSample(sample: DrillSample) {
        sampleRepository.insertDrillSample(sample)
        loadData()
    }

    suspend fun deleteAllNormalSamples() {
        sampleRepository.deleteAllNormalSamples()
        loadData()
    }

    suspend fun deleteAllDrillSamples() {
        sampleRepository.deleteAllDrillSamples()
        loadData()
    }
}
