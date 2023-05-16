package kr.drone.helpgpt.data.local

import androidx.room.*
import kr.drone.helpgpt.data.model.Summary
import kr.drone.helpgpt.data.model.SummaryEntity

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaryTable")
    suspend fun getAllSummary(): List<Summary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSummary(summary: SummaryEntity)

    @Delete
    suspend fun deleteSummary(summary: SummaryEntity)

    @Query("SELECT * FROM summaryTable WHERE videoId = :videoId")
    suspend fun getSummaryByVideoId(videoId: String): SummaryEntity?
}