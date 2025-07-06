package com.l3on1kl.mviewer.presentation.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.l3on1kl.mviewer.R
import com.l3on1kl.mviewer.presentation.ImageLoader
import com.l3on1kl.mviewer.presentation.model.MarkdownRenderItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MarkdownAdapter :
    ListAdapter<MarkdownRenderItem, RecyclerView.ViewHolder>(DIFF) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long =
        currentList[position]
            .key()
            .hashCode()
            .toLong()

    override fun getItemViewType(pos: Int) =
        when (getItem(pos)) {
            is MarkdownRenderItem.Header -> TYPE_HEADER

            is MarkdownRenderItem.Paragraph -> TYPE_PARAGRAPH

            is MarkdownRenderItem.ListItem -> TYPE_LIST

            is MarkdownRenderItem.Image -> TYPE_IMAGE

            is MarkdownRenderItem.Table -> TYPE_TABLE

            is MarkdownRenderItem.EmptyLine -> TYPE_EMPTY_LINE
        }

    override fun onCreateViewHolder(
        container: ViewGroup,
        itemType: Int
    ): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(container.context)

        return when (itemType) {
            TYPE_HEADER -> HeaderViewHolder(
                layoutInflater.inflate(
                    R.layout.item_md_header,
                    container,
                    false
                )
            )

            TYPE_PARAGRAPH -> ParagraphViewHolder(
                layoutInflater.inflate(
                    R.layout.item_md_para,
                    container,
                    false
                )
            )

            TYPE_LIST -> ListItemViewHolder(
                layoutInflater.inflate(
                    R.layout.item_md_list,
                    container,
                    false
                )
            )

            TYPE_IMAGE -> ImageViewHolder(
                layoutInflater.inflate(
                    R.layout.item_md_image,
                    container,
                    false
                )
            )

            TYPE_TABLE -> TableViewHolder(
                layoutInflater.inflate(
                    R.layout.item_md_table,
                    container, false
                ),
                container.context
            )

            TYPE_EMPTY_LINE -> EmptyLineViewHolder(
                layoutInflater.inflate(
                    R.layout.item_md_empty,
                    container,
                    false
                )
            )

            else -> error("Unknown view type: $itemType")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) = when (val item = getItem(position)) {
        is MarkdownRenderItem.Header ->
            (holder as HeaderViewHolder).bind(item)

        is MarkdownRenderItem.Paragraph ->
            (holder as ParagraphViewHolder).bind(item)

        is MarkdownRenderItem.ListItem ->
            (holder as ListItemViewHolder).bind(item)

        is MarkdownRenderItem.Image ->
            (holder as ImageViewHolder).bind(item)

        is MarkdownRenderItem.Table ->
            (holder as TableViewHolder).bind(item)

        is MarkdownRenderItem.EmptyLine ->
            (holder as EmptyLineViewHolder).bind()
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_RELOAD_IMAGE) &&
            holder is ImageViewHolder
        ) {
            holder.rebindImageOnly(
                currentList[position] as MarkdownRenderItem.Image
            )
            return
        }

        super.onBindViewHolder(holder, position, payloads)
    }


    class HeaderViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val headerTextView: TextView =
            itemView.findViewById(R.id.textHeader)

        fun bind(headerItem: MarkdownRenderItem.Header) {
            headerTextView.text = headerItem.text
            headerTextView.textSize = when (headerItem.level) {
                1 -> 24f

                2 -> 22f

                3 -> 20f

                4 -> 18f

                5 -> 16f

                else -> 14f
            }
            headerTextView.setPadding(
                0,
                8,
                0,
                8
            )
        }
    }

    class ParagraphViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val paragraphTextView: TextView =
            itemView.findViewById(R.id.textParagraph)

        fun bind(paragraphItem: MarkdownRenderItem.Paragraph) {
            paragraphTextView.text = paragraphItem.text
        }
    }

    class ListItemViewHolder(
        listItemView: View
    ) : RecyclerView.ViewHolder(listItemView) {
        private val listItemTextView: TextView =
            listItemView.findViewById(R.id.textListItem)

        @SuppressLint("SetTextI18n")
        fun bind(
            itemData: MarkdownRenderItem.ListItem
        ) {
            val bullet = "${itemData.marker} "
            listItemTextView.text = SpannableStringBuilder(
                bullet
            ).append(itemData.text)

            listItemTextView.setPadding(
                24 * itemData.level,
                8,
                0,
                8
            )
        }
    }

    class ImageViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val progressBar: View = itemView.findViewById(R.id.progressBarImage)
        private var job: Job? = null

        fun bind(item: MarkdownRenderItem.Image) {
            startLoading(item)
        }

        fun rebindImageOnly(item: MarkdownRenderItem.Image) {
            startLoading(item)
        }

        private fun startLoading(imageItem: MarkdownRenderItem.Image) {
            job?.cancel()
            progressBar.isVisible = true

            imageView.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        imageView.viewTreeObserver.removeOnPreDrawListener(this)

                        val width = imageView.width.takeIf {
                            it > 0
                        } ?: return true

                        job = CoroutineScope(
                            Dispatchers.Main
                        ).launch {
                            val cached = ImageLoader.load(
                                imageItem.url,
                                width
                            )

                            if (cached != null) {
                                progressBar.isVisible = false
                                imageView.setImageBitmap(cached)
                            } else {
                                progressBar.isVisible = true
                                imageView.setImageResource(R.drawable.ic_placeholder)

                                val bmp = ImageLoader.load(
                                    imageItem.url,
                                    width
                                )

                                bmp?.let {
                                    imageView.setImageBitmap(it)
                                }
                                progressBar.isVisible = false
                            }
                        }
                        return true
                    }
                }
            )
        }
    }

    class TableViewHolder(
        itemView: View,
        private val itemViewContext: Context
    ) : RecyclerView.ViewHolder(itemView) {
        private val table: TableLayout = itemView.findViewById(R.id.tableLayout)

        fun bind(
            tableItem: MarkdownRenderItem.Table
        ) {
            table.removeAllViews()

            tableItem.rows.forEachIndexed { rowIndex, rowCells ->
                val tableRowView = TableRow(itemViewContext)

                rowCells.forEachIndexed { columnIndex, content ->
                    val view = when (content) {
                        is MarkdownRenderItem.Table.Cell.Text -> createTextCell(
                            content.text,
                            rowIndex == 0
                        )

                        is MarkdownRenderItem.Table.Cell.Image -> createImageCell(
                            content
                        )
                    }

                    tableRowView.addView(
                        view,
                        TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT,
                            1f
                        ).apply {
                            leftMargin = if (columnIndex == 0) 0
                            else dp(1)
                        }
                    )
                }

                table.addView(tableRowView)
            }
        }

        private fun createTextCell(
            content: CharSequence,
            header: Boolean
        ): View =
            TextView(itemViewContext).apply {
                this.text = content
                setPadding(
                    dp(8),
                    dp(4),
                    dp(8),
                    dp(4)
                )

                if (header) {
                    setTypeface(
                        typeface,
                        Typeface.BOLD
                    )
                    setBackgroundColor(0xFFEFEFEF.toInt())
                }
                background = border()
            }

        private fun createImageCell(
            imageCell: MarkdownRenderItem.Table.Cell.Image
        ): View =
            ImageView(itemViewContext).apply {
                contentDescription = imageCell.alt
                adjustViewBounds = true
                setPadding(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(4)
                )
                background = border()

                CoroutineScope(
                    Dispatchers.Main
                ).launch {
                    val loadedImage = ImageLoader.load(
                        imageCell.url,
                        dp(80)
                    )
                    loadedImage?.let { setImageBitmap(it) }
                }
            }

        private fun border() = GradientDrawable().apply {
            setStroke(
                dp(1),
                0xFFCCCCCC.toInt()
            )
            setColor(0x00FFFFFF)
        }

        private fun dp(
            rawValue: Int
        ) = (rawValue * itemViewContext.resources.displayMetrics.density).toInt()
    }

    class EmptyLineViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {
        fun bind() {}
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PARAGRAPH = 1
        private const val TYPE_LIST = 2
        private const val TYPE_IMAGE = 3
        private const val TYPE_TABLE = 4
        private const val TYPE_EMPTY_LINE = 5

        const val PAYLOAD_RELOAD_IMAGE = 1

        private fun MarkdownRenderItem.key(): Any = when (this) {
            is MarkdownRenderItem.Header -> "H$level:$text"

            is MarkdownRenderItem.Paragraph -> "P:${text.hashCode()}"

            is MarkdownRenderItem.ListItem -> "L$level:$ordered:$marker:${text.hashCode()}"

            is MarkdownRenderItem.Image -> "I:$url"

            is MarkdownRenderItem.Table -> "T:${rows.hashCode()}"

            is MarkdownRenderItem.EmptyLine -> "E"
        }

        private val DIFF = object : DiffUtil.ItemCallback<MarkdownRenderItem>() {

            override fun areItemsTheSame(
                oldItem: MarkdownRenderItem,
                newItem: MarkdownRenderItem
            ) = oldItem.key() == newItem.key()

            override fun areContentsTheSame(
                oldItem: MarkdownRenderItem,
                newItem: MarkdownRenderItem
            ): Boolean =
                when {
                    oldItem is MarkdownRenderItem.Paragraph &&
                            newItem is MarkdownRenderItem.Paragraph ->
                        oldItem.text.contentEquals(newItem.text)

                    oldItem is MarkdownRenderItem.ListItem &&
                            newItem is MarkdownRenderItem.ListItem -> {
                        oldItem.marker == newItem.marker &&
                                oldItem.level == newItem.level &&
                                oldItem.ordered == newItem.ordered &&
                                oldItem.text.contentEquals(newItem.text)
                    }

                    oldItem is MarkdownRenderItem.Table &&
                            newItem is MarkdownRenderItem.Table ->
                        oldItem.rows.size == newItem.rows.size &&
                                oldItem.rows.indices.all { rowIndex ->
                                    oldItem.rows[rowIndex].size == newItem.rows[rowIndex].size &&
                                            oldItem.rows[rowIndex].indices.all { columnIndex ->
                                                when {
                                                    oldItem.rows[rowIndex][columnIndex] is MarkdownRenderItem.Table.Cell.Text &&
                                                            newItem.rows[rowIndex][columnIndex] is MarkdownRenderItem.Table.Cell.Text -> {
                                                        val oldText =
                                                            (oldItem.rows[rowIndex][columnIndex] as MarkdownRenderItem.Table.Cell.Text).text
                                                        val newText =
                                                            (newItem.rows[rowIndex][columnIndex] as MarkdownRenderItem.Table.Cell.Text).text
                                                        oldText.contentEquals(newText)
                                                    }

                                                    oldItem.rows[rowIndex][columnIndex] == newItem.rows[rowIndex][columnIndex] -> true

                                                    else -> false
                                                }
                                            }
                                }

                    else -> oldItem == newItem
                }

            private fun MarkdownRenderItem.key(): Any = when (this) {
                is MarkdownRenderItem.Header -> "H$level:$text"

                is MarkdownRenderItem.Paragraph -> "P:${text.toString().hashCode()}"

                is MarkdownRenderItem.ListItem -> "L$level:$ordered:$marker:${
                    text.toString().hashCode()
                }"

                is MarkdownRenderItem.Image -> "I:$url"

                is MarkdownRenderItem.Table -> hashRows(this.rows)

                is MarkdownRenderItem.EmptyLine -> "E"
            }

            private fun hashRows(
                tableRows: List<List<MarkdownRenderItem.Table.Cell>>
            ): Int =
                tableRows.joinToString { tableRow ->
                    tableRow.joinToString { renderItem ->
                        when (renderItem) {
                            is MarkdownRenderItem.Table.Cell.Text -> renderItem.text.toString()

                            is MarkdownRenderItem.Table.Cell.Image -> renderItem.url
                        }
                    }
                }.hashCode()
        }
    }
}
