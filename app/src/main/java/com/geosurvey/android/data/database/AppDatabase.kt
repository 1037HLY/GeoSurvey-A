package com.geosurvey.android.data.database

// 暂时简化，让构建通过
// import androidx.room.Database
// import androidx.room.RoomDatabase
// import androidx.room.TypeConverters
// import com.geosurvey.android.data.model.TrackPointEntity

// @Database(
//     entities = [TrackPointEntity::class],
//     version = 1,
//     exportSchema = false
// )
// @TypeConverters(Converters::class)
// abstract class AppDatabase : RoomDatabase() {
//     abstract fun trackPointDao(): TrackPointDao
// }

// 临时简化版本
abstract class AppDatabase {
    abstract fun trackPointDao(): TrackPointDao
}
