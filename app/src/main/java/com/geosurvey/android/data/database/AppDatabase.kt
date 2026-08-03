package com.geosurvey.android.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.geosurvey.android.data.model.*

@Database(
    entities = [
        TrackPoint::class,
        AttitudeRecord::class,
        WatermarkPhoto::class,
        CoordinateRecord::class,
        NormalSample::class,
        DrillSample::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackPointDao(): TrackPointDao
    abstract fun attitudeDao(): AttitudeDao
    abstract fun photoDao(): PhotoDao
    abstract fun coordinateDao(): CoordinateDao
    abstract fun sampleDao(): SampleDao
}
