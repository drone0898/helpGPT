package kr.drone.helpgpt.domain

import kr.drone.helpgpt.data.model.Summary

interface SummaryRepository {
    suspend fun getAllSummary(): List<Summary>
    suspend fun saveSummary(summary: Summary)
}