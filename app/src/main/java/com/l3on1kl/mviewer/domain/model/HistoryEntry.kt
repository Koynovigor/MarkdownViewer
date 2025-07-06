package com.l3on1kl.mviewer.domain.model

data class HistoryEntry(
    val path: String,
    val name: String,
    val openedAt: Long
)
