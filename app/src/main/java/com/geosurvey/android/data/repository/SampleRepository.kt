package com.geosurvey.android.data.repository

import com.geosurvey.android.data.database.SampleDao
import com.geosurvey.android.data.model.NormalSample
import com.geosurvey.android.data.model.DrillSample
import kotlinx.coroutines.flow.Flow

class SampleRepository(
    private val sampleDao: SampleDao
) {
    // 普通样本
    suspend fun insertNormalSample(sample: NormalSample) {
        sampleDao.insertNormalSample(sample)
    }
    
    fun getAllNormalSamples(): Flow<List<NormalSample>> {
        return sampleDao.getAllNormalSamples()
    }
    
    fun getNormalSampleCount(): Flow<Int> {
        return sampleDao.getNormalSampleCount()
    }
    
    suspend fun deleteAllNormalSamples() {
        sampleDao.deleteAllNormalSamples()
    }
    
    // 钻孔样本
    suspend fun insertDrillSample(sample: DrillSample) {
        sampleDao.insertDrillSample(sample)
    }
    
    fun getAllDrillSamples(): Flow<List<DrillSample>> {
        return sampleDao.getAllDrillSamples()
    }
    
    fun getDrillSampleCount(): Flow<Int> {
        return sampleDao.getDrillSampleCount()
    }
    
    suspend fun deleteAllDrillSamples() {
        sampleDao.deleteAllDrillSamples()
    }
}
