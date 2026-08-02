package com.geosurvey.android.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.geosurvey.android.data.model.CoordinateRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface CoordinateDao {
    @Insert
    suspend fun insertRecord(record: CoordinateRecord)

    @Query("SELECT * FROM coordinate_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<CoordinateRecord>>

    @Query("SELECT COUNT(*) FROM coordinate_records")
    fun getRecordCount(): Flow<Int>

    @Query("DELETE FROM coordinate_records")
    suspend fun deleteAllRecords()
}
