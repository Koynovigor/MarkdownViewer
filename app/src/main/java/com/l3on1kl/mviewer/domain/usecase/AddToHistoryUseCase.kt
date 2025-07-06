package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.model.HistoryEntry
import com.l3on1kl.mviewer.domain.repository.HistoryRepository
import javax.inject.Inject

class AddToHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    suspend operator fun invoke(
        entry: HistoryEntry
    ) = repository.add(entry)
}
