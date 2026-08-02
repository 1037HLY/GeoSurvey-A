package com.geosurvey.android.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.geosurvey.android.data.model.AttitudeRecord
import com.geosurvey.android.data.model.TrackPoint

@Database(
    entities = [
        TrackPoint::class,
        AttitudeRecord::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackPointDao(): TrackPointDao
    abstract fun attitudeDao(): AttitudeDao
}
