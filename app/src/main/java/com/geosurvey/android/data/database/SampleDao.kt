package com.geosurvey.android.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.geosurvey.android.data.model.NormalSample
import com.geosurvey.android.data.model.DrillSample
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleDao {
    // 普通样本
    @Insert
    suspend fun insertNormalSample(sample: NormalSample)
    
    @Query("SELECT * FROM normal_samples ORDER BY timestamp DESC")
    fun getAllNormalSamples(): Flow<List<NormalSample>>
    
    @Query("SELECT COUNT(*) FROM normal_samples")
    fun getNormalSampleCount(): Flow<Int>
    
    @Query("DELETE FROM normal_samples")
    suspend fun deleteAllNormalSamples()
    
    // 钻孔样本
    @Insert
    suspend fun insertDrillSample(sample: DrillSample)
    
    @Query("SELECT * FROM drill_samples ORDER BY timestamp DESC")
    fun getAllDrillSamples(): Flow<List<DrillSample>>
    
    @Query("SELECT COUNT(*) FROM drill_samples")
    fun getDrillSampleCount(): Flow<Int>
    
    @Query("DELETE FROM drill_samples")
    suspend fun deleteAllDrillSamples()
}
