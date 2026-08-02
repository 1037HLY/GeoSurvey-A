package com.geosurvey.android.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.geosurvey.android.data.model.TrackPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
    @Insert
    suspend fun insertTrackPoint(point: TrackPoint)

    @Insert
    suspend fun insertTrackPoints(points: List<TrackPoint>)

    @Query("SELECT * FROM track_points ORDER BY timestamp DESC")
    fun getAllTrackPoints(): Flow<List<TrackPoint>>

    @Query("SELECT * FROM track_points ORDER BY timestamp DESC LIMIT 50")
    fun getRecentTrackPoints(): Flow<List<TrackPoint>>

    @Query("SELECT COUNT(*) FROM track_points")
    fun getTrackPointCount(): Flow<Int>

    @Query("DELETE FROM track_points")
    suspend fun deleteAllTrackPoints()
}
