package com.l3on1kl.mviewer.presentation.history

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.l3on1kl.mviewer.R
import com.l3on1kl.mviewer.domain.model.HistoryEntry
import com.l3on1kl.mviewer.presentation.model.UiHistoryEntry

class HistoryAdapter(
    private val onClick: (HistoryEntry) -> Unit,
    private val onDelete: (HistoryEntry) -> Unit
) : ListAdapter<UiHistoryEntry, HistoryAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {
        private val name = view.findViewById<TextView>(R.id.name)
        private val btnDelete = view.findViewById<ImageButton>(R.id.deleteButton)
        private val date = view.findViewById<TextView>(R.id.date)

        fun bind(item: UiHistoryEntry) {
            name.text = item.entry.name

            date.text = item.date
            date.setTextColor(
                ColorUtils.setAlphaComponent(
                    itemView.context.attrColor(
                        com.google.android.material.R.attr.colorOnBackground
                    ),
                    160
                )
            )

            val backgroundColor = if (item.index % 2 == 0) {
                itemView.context.attrColor(
                    com.google.android.material.R.attr.backgroundColor
                )
            } else {
                ColorUtils.setAlphaComponent(
                    itemView.context.attrColor(
                        com.google.android.material.R.attr.colorOnBackground
                    ),
                    13
                )
            }
            (itemView as MaterialCardView).setCardBackgroundColor(
                backgroundColor
            )

            btnDelete.setOnClickListener {
                onDelete(item.entry)
            }
            itemView.setOnClickListener {
                onClick(item.entry)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = ViewHolder(
        LayoutInflater.from(
            parent.context
        ).inflate(
            R.layout.item_history,
            parent,
            false
        )
    )

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) = holder.bind(
        getItem(position)
    )

    private fun Context.attrColor(
        attr: Int
    ): Int = TypedValue()
        .also {
            theme.resolveAttribute(
                attr,
                it,
                true
            )
        }.data

    companion object DIFF : DiffUtil.ItemCallback<UiHistoryEntry>() {
        override fun areItemsTheSame(
            oldItem: UiHistoryEntry,
            newItem: UiHistoryEntry
        ): Boolean =
            oldItem.entry.path == newItem.entry.path

        override fun areContentsTheSame(
            oldItem: UiHistoryEntry,
            newItem: UiHistoryEntry
        ): Boolean =
            oldItem == newItem
    }
}
