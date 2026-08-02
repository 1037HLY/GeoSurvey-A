package com.geosurvey.android.data.repository

import com.geosurvey.android.data.database.TrackPointDao
import com.geosurvey.android.data.model.TrackPoint
import kotlinx.coroutines.flow.Flow

class TrackRepository(
    private val trackPointDao: TrackPointDao
) {
    suspend fun insertTrackPoint(point: TrackPoint) {
        trackPointDao.insertTrackPoint(point)
    }

    suspend fun insertTrackPoints(points: List<TrackPoint>) {
        trackPointDao.insertTrackPoints(points)
    }

    fun getAllTrackPoints(): Flow<List<TrackPoint>> {
        return trackPointDao.getAllTrackPoints()
    }

    fun getRecentTrackPoints(): Flow<List<TrackPoint>> {
        return trackPointDao.getRecentTrackPoints()
    }

    fun getTrackPointsByDate(date: String): Flow<List<TrackPoint>> {
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
