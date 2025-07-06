package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.repository.HistoryRepository

class InitDefaultHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke() {
        historyRepository.initDefaultHistoryEntryIfNeeded()
    }
}
