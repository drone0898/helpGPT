package kr.drone.helpgpt.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("userId")
    val userId: String ="",
    val gptProfile: GptProfile
)

data class GptProfile(
    @SerializedName("gptApiKey")
    val gptApiKey: String ="",
    @SerializedName("gptModelVersion")
    val gptModelVersion: String =""
)

@Entity(tableName = "userProfileTable")
data class UserProfileEntity(
    @PrimaryKey
    val userId: String,
    @ColumnInfo(name = "gpt_profile")
    val gptProfile: GptProfile
)

@Entity(tableName = "gptProfileTable")
data class GptProfileEntity(
    @PrimaryKey
    val gptApiKey: String,
    val gptModelVersion: String = ""
)