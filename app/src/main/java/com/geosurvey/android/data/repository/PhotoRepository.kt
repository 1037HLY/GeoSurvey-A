package com.geosurvey.android.data.repository

import com.geosurvey.android.data.database.PhotoDao
import com.geosurvey.android.data.model.WatermarkPhoto
import kotlinx.coroutines.flow.Flow

class PhotoRepository(
    private val photoDao: PhotoDao
) {
    suspend fun insertPhoto(photo: WatermarkPhoto) {
        photoDao.insertPhoto(photo)
    }

    fun getAllPhotos(): Flow<List<WatermarkPhoto>> {
        return photoDao.getAllPhotos()
    }

    fun getPhotosByDate(date: String): Flow<List<WatermarkPhoto>> {
        return photoDao.getPhotosByDate(date)
    }

    fun getAvailableDates(): Flow<List<String>> {
        return photoDao.getAvailableDates()
    }

    fun getPhotoCount(): Flow<Int> {
        return photoDao.getPhotoCount()
    }

    suspend fun deleteAllPhotos() {
        photoDao.deleteAllPhotos()
    }
}
