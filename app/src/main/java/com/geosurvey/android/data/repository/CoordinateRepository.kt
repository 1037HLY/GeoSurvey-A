package com.geosurvey.android.data.repository

import com.geosurvey.android.data.database.CoordinateDao
import com.geosurvey.android.data.model.CoordinateRecord
import kotlinx.coroutines.flow.Flow

class CoordinateRepository(
    private val coordinateDao: CoordinateDao
) {
    suspend fun insertRecord(record: CoordinateRecord) {
        coordinateDao.insertRecord(record)
    }

    fun getAllRecords(): Flow<List<CoordinateRecord>> {
        return coordinateDao.getAllRecords()
    }

    fun getRecordCount(): Flow<Int> {
        return coordinateDao.getRecordCount()
    }

    suspend fun deleteAllRecords() {
        coordinateDao.deleteAllRecords()
    }
}
