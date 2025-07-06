package com.l3on1kl.mviewer.domain.usecase

import com.l3on1kl.mviewer.domain.repository.HistoryRepository
import javax.inject.Inject

class RemoveFromHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    suspend operator fun invoke(path: String) = repository.remove(path)
}
