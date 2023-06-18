package kr.drone.helpgpt.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kr.drone.helpgpt.data.model.GptProfile
import kr.drone.helpgpt.data.model.GptProfileEntity
import kr.drone.helpgpt.data.model.SummaryEntity
import kr.drone.helpgpt.data.model.UserProfileEntity


@Database(
    entities = [
    UserProfileEntity::class,
    GptProfileEntity::class,
    SummaryEntity::class
               ],
    version = 1,
    exportSchema = false)
@TypeConverters(GptProfileConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun summaryDao(): SummaryDao
    abstract fun userProfileDao(): UserProfileDao
}


class GptProfileConverter {
    @TypeConverter
    fun fromGptProfile(gptProfile: GptProfile): String {
        return Gson().toJson(gptProfile)
    }

    @TypeConverter
    fun toGptProfile(data: String): GptProfile {
        val type = object: TypeToken<GptProfile>() {}.type
        return Gson().fromJson(data, type)
    }
}