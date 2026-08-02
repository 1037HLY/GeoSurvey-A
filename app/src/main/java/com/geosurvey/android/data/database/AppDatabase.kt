package com.geosurvey.android.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.geosurvey.android.data.model.TrackPointEntity

/**
 * 应用数据库
 */
@Database(
    entities = [TrackPointEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackPointDao(): TrackPointDao
}
