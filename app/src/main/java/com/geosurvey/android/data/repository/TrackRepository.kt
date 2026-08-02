package com.geosurvey.android.data.repository

import com.geosurvey.android.data.database.TrackPointDao
import com.geosurvey.android.data.model.TrackPointEntity
import kotlinx.coroutines.flow.Flow

/**
 * 轨迹数据仓库
 */
class TrackRepository(
    private val trackPointDao: TrackPointDao
) {
    suspend fun insertTrackPoint(point: TrackPointEntity) {
        trackPointDao.insertTrackPoint(point)
    }

    suspend fun insertTrackPoints(points: List<TrackPointEntity>) {
        trackPointDao.insertTrackPoints(points)
    }

    fun getAllTrackPoints(): Flow<List<TrackPointEntity>> {
        return trackPointDao.getAllTrackPoints()
    }

    fun getTrackPointsByDate(date: String): Flow<List<TrackPointEntity>> {
        return trackPointDao.getTrackPointsByDate(date)
    }

    fun getAvailableDates(): Flow<List<String>> {
        return trackPointDao.getAvailableDates()
    }

    fun getTrackPointCount(): Flow<Int> {
        return trackPointDao.getTrackPointCount()
    }

    suspend fun deleteTrackPointsByDate(date: String) {
        trackPointDao.deleteTrackPointsByDate(date)
    }

    suspend fun deleteAllTrackPoints() {
        trackPointDao.deleteAllTrackPoints()
    }
}
