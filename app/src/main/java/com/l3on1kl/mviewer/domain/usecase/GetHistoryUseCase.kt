package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.model.HistoryEntry
import com.l3on1kl.mviewer.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    operator fun invoke(): Flow<List<HistoryEntry>> = repository.flow()
}
