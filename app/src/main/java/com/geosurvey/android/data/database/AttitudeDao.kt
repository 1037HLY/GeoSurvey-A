package com.geosurvey.android.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.geosurvey.android.data.model.AttitudeRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttitudeDao {
    @Insert
    suspend fun insertRecord(record: AttitudeRecord)

    @Query("SELECT * FROM attitude_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AttitudeRecord>>

    @Query("SELECT * FROM attitude_records WHERE date(timestamp / 1000, 'unixepoch') = :date ORDER BY timestamp DESC")
    fun getRecordsByDate(date: String): Flow<List<AttitudeRecord>>

    @Query("SELECT DISTINCT date(timestamp / 1000, 'unixepoch') as date FROM attitude_records ORDER BY date DESC")
    fun getAvailableDates(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM attitude_records")
    fun getRecordCount(): Flow<Int>

    @Query("DELETE FROM attitude_records")
    suspend fun deleteAllRecords()
}
