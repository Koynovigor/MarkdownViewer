package com.l3on1kl.mviewer.presentation.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.l3on1kl.mviewer.R
import com.l3on1kl.mviewer.domain.model.MarkdownRenderItem
import com.l3on1kl.mviewer.presentation.ImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MarkdownAdapter : ListAdapter<MarkdownRenderItem, RecyclerView.ViewHolder>(DIFF) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = currentList[position].key().hashCode().toLong()

    override fun getItemViewType(pos: Int) = when (getItem(pos)) {
        is MarkdownRenderItem.Header -> TYPE_HEADER
        is MarkdownRenderItem.Paragraph -> TYPE_PARAGRAPH
        is MarkdownRenderItem.ListItem -> TYPE_LIST
        is MarkdownRenderItem.Image -> TYPE_IMAGE
        is MarkdownRenderItem.Table -> TYPE_TABLE
        is MarkdownRenderItem.EmptyLine -> TYPE_EMPTY_LINE
    }

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (vt) {
            TYPE_HEADER -> HeaderViewHolder(
                inf.inflate(
                    R.layout.item_md_header,
                    parent,
                    false
                )
            )

            TYPE_PARAGRAPH -> ParagraphViewHolder(
                inf.inflate(
                    R.layout.item_md_para,
                    parent,
                    false
                )
            )

            TYPE_LIST -> ListItemViewHolder(
                inf.inflate(
                    R.layout.item_md_list,
                    parent,
                    false
                )
            )

            TYPE_IMAGE -> ImageViewHolder(
                inf.inflate(
                    R.layout.item_md_image,
                    parent,
                    false
                )
            )

            TYPE_TABLE -> TableViewHolder(
                inf.inflate(
                    R.layout.item_md_table,
                    parent, false
                ),
                parent.context
            )

            TYPE_EMPTY_LINE -> EmptyLineViewHolder(
                inf.inflate(
                    R.layout.item_md_empty,
                    parent,
                    false
                )
            )

            else -> error("Unknown view type: $vt")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        when (val item = getItem(position)) {
            is MarkdownRenderItem.Header -> (holder as HeaderViewHolder).bind(item)
            is MarkdownRenderItem.Paragraph -> (holder as ParagraphViewHolder).bind(item)
            is MarkdownRenderItem.ListItem -> (holder as ListItemViewHolder).bind(item)
            is MarkdownRenderItem.Image -> (holder as ImageViewHolder).bind(item)
            is MarkdownRenderItem.Table -> (holder as TableViewHolder).bind(item)
            is MarkdownRenderItem.EmptyLine -> (holder as EmptyLineViewHolder).bind()
        }

    /* ---------------- view-holders ---------------- */

    class HeaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val tv: TextView = v.findViewById(R.id.textHeader)
        fun bind(i: MarkdownRenderItem.Header) {
            tv.text = i.text
            tv.textSize = when (i.level) {
                1 -> 24f; 2 -> 22f; 3 -> 20f; 4 -> 18f; 5 -> 16f; else -> 14f
            }
            tv.setPadding(0, 8, 0, 8)
        }
    }

    class ParagraphViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val tv: TextView = v.findViewById(R.id.textParagraph)
        fun bind(i: MarkdownRenderItem.Paragraph) {
            tv.text = i.text
        }
    }

    class ListItemViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val tv: TextView = v.findViewById(R.id.textListItem)

        @SuppressLint("SetTextI18n")
        fun bind(i: MarkdownRenderItem.ListItem) {
            val bullet = "${i.marker} "
            tv.text = SpannableStringBuilder(bullet).append(i.text)
            tv.setPadding(24 * i.level, 8, 0, 8)
        }
    }

    class ImageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val iv: ImageView = v.findViewById(R.id.imageView)
        private var job: Job? = null
        fun bind(i: MarkdownRenderItem.Image) {
            iv.contentDescription = i.alt
            job?.cancel()
            job = CoroutineScope(Dispatchers.Main).launch {
                val bmp = ImageLoader.load(i.url, iv.width.takeIf { it > 0 } ?: 400)
                bmp?.let { iv.setImageBitmap(it) }
            }
        }
    }

    class TableViewHolder(v: View, private val ctx: Context) : RecyclerView.ViewHolder(v) {

        private val table: TableLayout = v.findViewById(R.id.tableLayout)

        fun bind(t: MarkdownRenderItem.Table) {
            table.removeAllViews()

            t.rows.forEachIndexed { rIdx, cells ->
                val tr = TableRow(ctx)

                cells.forEachIndexed { cIdx, cell ->
                    val view = when (cell) {
                        is MarkdownRenderItem.Table.Cell.Text -> createTextCell(
                            cell.text,
                            rIdx == 0
                        )

                        is MarkdownRenderItem.Table.Cell.Image -> createImageCell(cell)
                    }
                    tr.addView(
                        view, TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT, 1f
                        ).apply { leftMargin = if (cIdx == 0) 0 else dp(1) })
                }
                table.addView(tr)
            }
        }

        /* ---------------- helpers ---------------- */

        private fun createTextCell(text: CharSequence, header: Boolean): View =
            TextView(ctx).apply {
                this.text = text
                setPadding(dp(8), dp(4), dp(8), dp(4))
                if (header) {
                    setTypeface(typeface, Typeface.BOLD)
                    setBackgroundColor(0xFFEFEFEF.toInt())
                }
                background = border()
            }

        private fun createImageCell(cell: MarkdownRenderItem.Table.Cell.Image): View =
            ImageView(ctx).apply {
                contentDescription = cell.alt
                adjustViewBounds = true
                setPadding(dp(4), dp(4), dp(4), dp(4))
                background = border()
                CoroutineScope(Dispatchers.Main).launch {
                    val bmp = ImageLoader.load(cell.url, dp(80))
                    bmp?.let { setImageBitmap(it) }
                }
            }

        private fun border() = GradientDrawable().apply {
            setStroke(dp(1), 0xFFCCCCCC.toInt())
            setColor(0x00FFFFFF)
        }

        private fun dp(v: Int) = (v * ctx.resources.displayMetrics.density).toInt()
    }

    class EmptyLineViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        fun bind() {}
    }

    private companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PARAGRAPH = 1
        private const val TYPE_LIST = 2
        private const val TYPE_IMAGE = 3
        private const val TYPE_TABLE = 4
        private const val TYPE_EMPTY_LINE = 5


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
                old: MarkdownRenderItem,
                new: MarkdownRenderItem
            ): Boolean =
                old.key() == new.key()

            override fun areContentsTheSame(
                old: MarkdownRenderItem,
                new: MarkdownRenderItem
            ): Boolean =
                old == new
        }

    }
}
