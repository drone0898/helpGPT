package kr.drone.helpgpt.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Summary(
    val summaryId:String ="",
    val videoId:String ="",
    val origin:String ="",
    val summary:String ="",
    val language:String = "",
    val date:String =""
)

@Entity(tableName = "summaryTable")
data class SummaryEntity(
    @PrimaryKey
    val summaryId:String,
    val videoId:String ="",
    val origin:String ="",
    val summary:String ="",
    val language:String = "",
    val date:String =""
)

data class SummaryList(
    val summaryList: List<Summary> = listOf()
)

fun List<SummaryEntity>.asSummaryList(): List<Summary> = this.map{
    Summary(it.summaryId,it.videoId,it.origin,it.summary,it.language,it.date)
}
fun Summary.asSummaryEntity(): SummaryEntity =
    SummaryEntity(this.summaryId,this.videoId,this.origin,this.summary,this.language,this.date)