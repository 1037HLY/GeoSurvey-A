package com.geosurvey.android.data.repository

import com.geosurvey.android.data.database.AttitudeDao
import com.geosurvey.android.data.model.AttitudeRecord
import kotlinx.coroutines.flow.Flow

class AttitudeRepository(
    private val attitudeDao: AttitudeDao
) {
    suspend fun insertRecord(record: AttitudeRecord) {
        attitudeDao.insertRecord(record)
    }

    fun getAllRecords(): Flow<List<AttitudeRecord>> {
        return attitudeDao.getAllRecords()
    }

    fun getRecordsByDate(date: String): Flow<List<AttitudeRecord>> {
        return attitudeDao.getRecordsByDate(date)
    }

    fun getAvailableDates(): Flow<List<String>> {
        return attitudeDao.getAvailableDates()
    }

    fun getRecordCount(): Flow<Int> {
        return attitudeDao.getRecordCount()
    }

    suspend fun deleteAllRecords() {
        attitudeDao.deleteAllRecords()
    }
}
