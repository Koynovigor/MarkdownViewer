package com.l3on1kl.mviewer.presentation.viewer

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import com.l3on1kl.mviewer.domain.model.MarkdownElement
import com.l3on1kl.mviewer.domain.model.MarkdownElementType
import com.l3on1kl.mviewer.domain.usecase.ParseMarkdownUseCase
import com.l3on1kl.mviewer.presentation.model.MarkdownRenderItem
import javax.inject.Inject

class MarkdownRenderer @Inject constructor(
    private val parseMarkdown: ParseMarkdownUseCase
) {
    fun render(doc: MarkdownDocument): List<MarkdownRenderItem> =
        toRenderItems(
            parseMarkdown(doc.content)
        )

    private fun toRenderItems(
        elements: List<MarkdownElement>
    ): List<MarkdownRenderItem> {
        val parsedMarkdownItems = mutableListOf<MarkdownRenderItem>()
        val paragraph = SpannableStringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                parsedMarkdownItems += MarkdownRenderItem
                    .Paragraph(
                        SpannableStringBuilder(paragraph)
                    )

                paragraph.clear()
            }
        }

        var elementIndex = 0
        while (elementIndex < elements.size) {
            val currentElement = elements[elementIndex]

            when (currentElement.type) {

                MarkdownElementType.Image -> {
                    flushParagraph()
                    parsedMarkdownItems += MarkdownRenderItem.Image(
                        url = currentElement.params["src"].orEmpty(),
                        alt = currentElement.text.takeIf(
                            String::isNotBlank
                        )
                    )
                }

                MarkdownElementType.Heading -> {
                    flushParagraph()

                    val styled = SpannableStringBuilder().apply {
                        parseMarkdown(currentElement.text).forEach { inline ->
                            when (inline.type) {
                                MarkdownElementType.Image -> {
                                    parsedMarkdownItems += MarkdownRenderItem.Image(
                                        url = inline.params["src"].orEmpty(),
                                        alt = inline.text.takeIf(
                                            String::isNotBlank
                                        )
                                    )
                                }

                                else -> appendStyled(
                                    inline.text,
                                    inline.type
                                )
                            }
                        }
                    }

                    parsedMarkdownItems += MarkdownRenderItem.Header(
                        text = styled,
                        level = currentElement.params["level"]?.toIntOrNull() ?: 1
                    )
                }

                MarkdownElementType.Table -> {
                    flushParagraph()

                    val tableRows = mutableListOf<List<MarkdownRenderItem.Table.Cell>>()

                    while (elementIndex < elements.size &&
                        elements[elementIndex].type == MarkdownElementType.Table
                    ) {
                        val rawCells = elements[elementIndex].params["cells"]
                            ?.split(';')
                            ?.map { it.trim() }
                            ?: emptyList()

                        val parsedCells = rawCells.map { source ->
                            val inline = parseMarkdown(source)

                            if (inline.size == 1 && inline[0].type == MarkdownElementType.Image) {
                                val inlineImage = inline[0]
                                MarkdownRenderItem.Table.Cell.Image(
                                    url = inlineImage.params["src"].orEmpty(),
                                    alt = inlineImage.text.takeIf(
                                        String::isNotBlank
                                    )
                                )
                            } else {
                                val styledText = SpannableStringBuilder().apply {
                                    inline.forEach {
                                        appendStyled(
                                            it.text,
                                            it.type
                                        )
                                    }
                                }
                                MarkdownRenderItem.Table.Cell.Text(styledText)
                            }
                        }
                        tableRows += parsedCells
                        elementIndex++
                    }
                    parsedMarkdownItems += MarkdownRenderItem.Table(tableRows)
                    continue
                }

                MarkdownElementType.ListItem -> {
                    flushParagraph()
                    val listItemId = currentElement.params["li"]
                        ?: run {
                            elementIndex++
                            continue
                        }

                    val listItemText = SpannableStringBuilder()
                    var imageForListItem: MarkdownRenderItem.Image? = null

                    while (elementIndex < elements.size &&
                        elements[elementIndex].params["li"] == listItemId
                    ) {
                        val currentElement = elements[elementIndex]
                        val startIndex = listItemText.length
                        listItemText.append(currentElement.text)
                        val endIndex = listItemText.length

                        when (currentElement.type) {
                            MarkdownElementType.Bold -> listItemText.setSpan(
                                StyleSpan(Typeface.BOLD),
                                startIndex,
                                endIndex,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            MarkdownElementType.Italic -> listItemText.setSpan(
                                StyleSpan(Typeface.ITALIC),
                                startIndex,
                                endIndex,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            MarkdownElementType.Strikethrough -> listItemText.setSpan(
                                StrikethroughSpan(),
                                startIndex,
                                endIndex,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            MarkdownElementType.Paragraph ->
                                listItemText.appendStyled(
                                    currentElement.text,
                                    MarkdownElementType.Paragraph
                                )

                            MarkdownElementType.Image -> imageForListItem =
                                MarkdownRenderItem.Image(
                                    url = currentElement.params["src"].orEmpty(),
                                    alt = currentElement.text.takeIf(
                                        String::isNotBlank
                                    )
                                )

                            else -> {}
                        }
                        elementIndex++
                    }

                    parsedMarkdownItems += MarkdownRenderItem.ListItem(
                        text = listItemText,
                        ordered = currentElement.params["ordered"] == "true",
                        level = currentElement.params["level"]?.toIntOrNull() ?: 0,
                        marker = currentElement.params["marker"].orEmpty()
                    )
                    imageForListItem?.let(parsedMarkdownItems::add)
                    continue
                }

                MarkdownElementType.Bold,
                MarkdownElementType.Italic,
                MarkdownElementType.Strikethrough,
                MarkdownElementType.Paragraph -> {
                    when {
                        currentElement.text.isEmpty() -> {
                            flushParagraph()
                            parsedMarkdownItems += MarkdownRenderItem.EmptyLine
                        }

                        currentElement.text == "\n" -> {
                            paragraph.append('\n')
                        }

                        else ->
                            paragraph.appendStyled(
                                currentElement.text,
                                currentElement.type
                            )
                    }
                }
            }
            elementIndex++
        }
        flushParagraph()

        return parsedMarkdownItems
    }

    private fun SpannableStringBuilder.appendStyled(
        textContent: String,
        textStyle: MarkdownElementType
    ) {
        val startIndex = length
        append(textContent)
        val endIndex = length

        when (textStyle) {
            MarkdownElementType.Bold ->
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    startIndex,
                    endIndex,
                    0
                )

            MarkdownElementType.Italic ->
                setSpan(
                    StyleSpan(Typeface.ITALIC),
                    startIndex,
                    endIndex,
                    0
                )

            MarkdownElementType.Strikethrough ->
                setSpan(
                    StrikethroughSpan(),
                    startIndex,
                    endIndex,
                    0
                )

            else -> {}
        }
    }
}
