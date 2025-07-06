package com.l3on1kl.mviewer.domain.parser

import com.l3on1kl.mviewer.domain.model.MarkdownElement
import com.l3on1kl.mviewer.domain.model.MarkdownElementType
import java.util.UUID

class MarkdownParserImpl : MarkdownParser {

    override fun parse(
        content: String
    ): List<MarkdownElement> {
        if (content.isBlank()) return emptyList()

        val result = mutableListOf<MarkdownElement>()
        val contentLines = content.lines()
        val listStack = ArrayDeque<ListContext>()
        var rootLevelIndent = 0

        var lineIndex = 0
        while (lineIndex < contentLines.size) {
            val currentLine = contentLines[lineIndex]

            val currentIndent = currentLine.indexOfFirst {
                !it.isWhitespace()
            }.coerceAtLeast(0)

            val trimmedLine = currentLine.trimEnd()
            if (listStack.isEmpty()) rootLevelIndent = 0

            if (trimmedLine.isBlank()) {
                listStack.clear()
                result += MarkdownElement(
                    MarkdownElementType.Paragraph,
                    ""
                )

                lineIndex++
                continue
            }

            if (trimmedLine.trimStart().startsWith('#')) {
                val trimmedContent = trimmedLine.trimStart()

                val level = trimmedContent.takeWhile {
                    it == '#'
                }.length.coerceIn(1, 6)

                val text = trimmedContent
                    .drop(level)
                    .trim()
                    .trimEnd('#')
                    .trim()

                result += MarkdownElement(
                    MarkdownElementType.Heading,
                    text,
                    mapOf("level" to level.toString())
                )

                lineIndex++
                continue
            }

            if (isTableRow(trimmedLine)) {
                val currentLineIndex = lineIndex
                var tableEndIndex = lineIndex

                while (tableEndIndex < contentLines.size &&
                    isTableRow(contentLines[tableEndIndex])
                ) {
                    tableEndIndex++
                }

                contentLines.subList(
                    currentLineIndex,
                    tableEndIndex
                ).forEach { tableRow ->
                    val rowData = tableRow
                        .trim()
                        .trim('|')
                        .split('|')
                        .map { it.trim() }

                    val isDivider = rowData.all { identifier ->
                        identifier.isNotEmpty() && identifier.all { char ->
                            char == '-' || char == ':'
                        }
                    }
                    if (isDivider) return@forEach

                    result += MarkdownElement(
                        MarkdownElementType.Table,
                        tableRow,
                        mapOf("cells" to rowData.joinToString(";"))
                    )
                }

                lineIndex = tableEndIndex
                continue
            }

            detectListItem(currentLine)?.let { (ordered, body) ->
                if (listStack.isEmpty()) rootLevelIndent = currentIndent

                val indentLevel = ((currentIndent - rootLevelIndent)
                    .coerceAtLeast(0)) / INDENT_SIZE

                val listLevel = updateListStack(
                    listStack,
                    indentLevel, ordered
                )

                val listPrefix = if (ordered) {
                    listStack.joinToString(".") {
                        it.counter.toString()
                    } + "."
                } else {
                    "-".repeat(listLevel + 1)
                }

                val listItemId = UUID.randomUUID().toString()
                val listItemMetadata = mapOf(
                    "ordered" to ordered.toString(),
                    "marker" to listPrefix,
                    "level" to listLevel.toString(),
                    "li" to listItemId
                )

                val parsedFragments = parseInline(body)

                if (parsedFragments.isNotEmpty()) {
                    val first = parsedFragments.first()
                    val origType = first.type
                    val meta = listItemMetadata + ("origStyle" to origType.name)

                    result += first.copy(
                        type = MarkdownElementType.ListItem,
                        params = first.params + meta
                    )
                    parsedFragments.drop(1).forEach { frag ->
                        result += frag.copy(params = frag.params + meta)
                    }
                } else {
                    result += MarkdownElement(
                        MarkdownElementType.ListItem,
                        body,
                        listItemMetadata
                    )
                }

                lineIndex++
                continue
            }

            listStack.clear()
            result += parseInline(trimmedLine)
            if (lineIndex + 1 < contentLines.size &&
                contentLines[lineIndex + 1].isNotBlank()
            ) {
                result += MarkdownElement(
                    MarkdownElementType.Paragraph,
                    "\n"
                )
            }

            lineIndex++
        }
        return result
    }

    private fun isTableRow(
        markdownLine: String
    ): Boolean =
        "|" in markdownLine &&
                (markdownLine.trimStart().startsWith('|') || markdownLine.trimEnd()
                    .endsWith('|')) &&
                markdownLine.count { it == '|' } >= 2

    private fun detectListItem(listItemText: String): Pair<Boolean, String>? {
        val trimmed = listItemText.trimStart()

        if (trimmed.startsWith("**") || trimmed.startsWith("__"))
            return null

        val unordered = Regex("^([*+\\-])\\s?(.*)")
        unordered.matchEntire(trimmed)?.let { m -> return false to m.groupValues[2] }

        val ordered = Regex("^(\\d+)\\.\\s?(.*)")
        ordered.matchEntire(trimmed)?.let { m -> return true to m.groupValues[2] }

        return null
    }

    private fun updateListStack(
        listStack: ArrayDeque<ListContext>,
        indentLevel: Int,
        orderedList: Boolean
    ): Int {
        while (listStack.isNotEmpty() &&
            indentLevel < listStack.last().indent
        ) {
            listStack.removeLast()
        }

        if (listStack.isEmpty() || indentLevel > listStack.last().indent ||
            (indentLevel == listStack.last().indent && listStack.last().ordered != orderedList)
        ) {
            listStack.addLast(
                ListContext(
                    indentLevel,
                    orderedList
                )
            )
        }

        val currentListContext = listStack.last()
        if (orderedList) currentListContext.counter++
        return listStack.size - 1
    }

    private data class ListContext(
        val indent: Int,
        val ordered: Boolean,
        var counter: Int = 0
    )

    private fun parseInline(
        content: String
    ): List<MarkdownElement> {
        if (content.isBlank()) return emptyList()

        val result = mutableListOf<MarkdownElement>()
        val textBuilder = StringBuilder()
        var currentIndex = 0

        fun flushPlain() {
            if (textBuilder.isNotEmpty()) {
                result += MarkdownElement(
                    MarkdownElementType.Paragraph,
                    textBuilder.toString()
                )
                textBuilder.clear()
            }
        }

        while (currentIndex < content.length) {
            // image ![alt](url)
            if (content.startsWith("![", currentIndex)) {
                val altTextEndIndex = content.indexOf(
                    ']',
                    currentIndex + 2
                )

                val urlStartIndex =
                    if (
                        altTextEndIndex != -1 &&
                        altTextEndIndex + 1 < content.length &&
                        content[altTextEndIndex + 1] == '('
                    ) {
                        altTextEndIndex + 2
                    } else -1

                val urlEndIndex = if (urlStartIndex != -1) {
                    content.indexOf(
                        ')',
                        urlStartIndex
                    )
                } else -1

                if (altTextEndIndex != -1 && urlEndIndex != -1) {
                    flushPlain()

                    val altText = content.substring(
                        currentIndex + 2,
                        altTextEndIndex
                    )

                    val imageUrl = content.substring(
                        urlStartIndex,
                        urlEndIndex
                    )

                    result += MarkdownElement(
                        MarkdownElementType.Image,
                        altText.ifBlank { imageUrl },
                        mapOf("src" to imageUrl)
                    )

                    currentIndex = urlEndIndex + 1
                    continue
                }
            }

            if (content.startsWith(
                    "**",
                    currentIndex
                ) || content.startsWith(
                    "__",
                    currentIndex
                )
            ) {
                val string = content.substring(
                    currentIndex,
                    currentIndex + 2
                )

                val end = content.indexOf(
                    string,
                    currentIndex + 2
                )

                if (end != -1) {
                    flushPlain()

                    val inner = content.substring(
                        currentIndex + 2,
                        end
                    )
                    val markdownElements = parseInline(inner)

                    markdownElements.forEach { elem ->
                        result += if (elem.type == MarkdownElementType.Paragraph) {
                            elem.copy(type = MarkdownElementType.Bold)
                        } else {
                            elem
                        }
                    }

                    currentIndex = end + 2
                    continue
                }
            }

            if (
                content.startsWith(
                    "~~",
                    currentIndex
                )
            ) {
                val strikethroughEnd = content.indexOf(
                    "~~",
                    currentIndex + 2
                )

                if (strikethroughEnd != -1) {
                    flushPlain()
                    val strikethroughText = content.substring(
                        currentIndex + 2,
                        strikethroughEnd
                    )

                    result += MarkdownElement(
                        MarkdownElementType.Strikethrough,
                        strikethroughText
                    )

                    currentIndex = strikethroughEnd + 2
                    continue
                }
            }

            if (content[currentIndex] == '*' || content[currentIndex] == '_') {
                val delimiter = content[currentIndex]
                val endIndex = content.indexOf(
                    delimiter,
                    currentIndex + 1
                )

                if (endIndex != -1 &&
                    content.getOrNull(endIndex + 1) != delimiter
                ) {
                    flushPlain()

                    val text = content.substring(
                        currentIndex + 1,
                        endIndex
                    )
                    result += MarkdownElement(
                        MarkdownElementType.Italic,
                        text
                    )

                    currentIndex = endIndex + 1
                    continue
                }
            }

            if (
                content[currentIndex] == '\\' &&
                currentIndex + 1 < content.length
            ) {
                textBuilder.append(content[currentIndex + 1])
                currentIndex += 2
                continue
            }

            textBuilder.append(content[currentIndex])
            currentIndex++
        }

        flushPlain()
        return result
    }

    private companion object {
        const val INDENT_SIZE: Int = 4
    }
}
