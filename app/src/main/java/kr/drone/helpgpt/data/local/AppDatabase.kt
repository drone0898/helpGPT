package kr.drone.helpgpt.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import kr.drone.helpgpt.data.model.SummaryEntity
import kr.drone.helpgpt.data.model.UserProfileEntity

@Database(
    entities = [
    UserProfileEntity::class,
    SummaryEntity::class
                     ],
    version = 1,
    exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

}