package com.geosurvey.android.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.geosurvey.android.data.model.WatermarkPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert
    suspend fun insertPhoto(photo: WatermarkPhoto)

    @Query("SELECT * FROM watermark_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<WatermarkPhoto>>

    @Query("SELECT * FROM watermark_photos WHERE date(timestamp / 1000, 'unixepoch') = :date ORDER BY timestamp DESC")
    fun getPhotosByDate(date: String): Flow<List<WatermarkPhoto>>

    @Query("SELECT DISTINCT date(timestamp / 1000, 'unixepoch') as date FROM watermark_photos ORDER BY date DESC")
    fun getAvailableDates(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM watermark_photos")
    fun getPhotoCount(): Flow<Int>

    @Query("DELETE FROM watermark_photos")
    suspend fun deleteAllPhotos()
}
