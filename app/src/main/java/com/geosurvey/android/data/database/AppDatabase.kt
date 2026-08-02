package com.geosurvey.android.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.geosurvey.android.data.model.AttitudeRecord
import com.geosurvey.android.data.model.TrackPoint
import com.geosurvey.android.data.model.WatermarkPhoto

@Database(
    entities = [
        TrackPoint::class,
        AttitudeRecord::class,
        WatermarkPhoto::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackPointDao(): TrackPointDao
    abstract fun attitudeDao(): AttitudeDao
    abstract fun photoDao(): PhotoDao
}
