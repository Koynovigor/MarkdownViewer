package com.l3on1kl.mviewer.presentation.model

import com.l3on1kl.mviewer.domain.model.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UiHistoryEntry(
    val index: Int,
    val entry: HistoryEntry,
    val date: String
)

fun HistoryEntry.toUi(index: Int): UiHistoryEntry {
    val formatter = SimpleDateFormat(
        "d MMM yyyy, HH:mm",
        Locale.getDefault()
    )

    val formattedDate = formatter.format(
        Date(openedAt)
    )

    return UiHistoryEntry(
        index = index,
        entry = this,
        date = formattedDate
    )
}
