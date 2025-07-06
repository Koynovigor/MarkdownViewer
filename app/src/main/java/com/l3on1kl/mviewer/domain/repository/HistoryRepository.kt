package com.l3on1kl.mviewer.domain.repository

import com.l3on1kl.mviewer.domain.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    suspend fun add(entry: HistoryEntry)

    fun flow(): Flow<List<HistoryEntry>>

    suspend fun remove(path: String)

    suspend fun initDefaultHistoryEntryIfNeeded()
}
