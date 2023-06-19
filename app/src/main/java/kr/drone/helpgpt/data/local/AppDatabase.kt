package kr.drone.helpgpt.data.local

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.drone.helpgpt.data.model.GptProfile
import kr.drone.helpgpt.data.model.GptProfileEntity
import kr.drone.helpgpt.data.model.SummaryEntity
import kr.drone.helpgpt.data.model.UserProfileEntity
import kr.drone.helpgpt.util.DATABASE_NAME


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

    companion object{
        @Volatile private var instance:AppDatabase?=null
        fun getInstance(context:Context):AppDatabase{
            return instance ?: synchronized(this){
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase{
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch(Dispatchers.Main){
                                getInstance(context).userProfileDao().saveUserProfile(
                                    UserProfileEntity("",null)
                                )
                            }
                        }
                    }
                )
                .build()
        }
    }
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