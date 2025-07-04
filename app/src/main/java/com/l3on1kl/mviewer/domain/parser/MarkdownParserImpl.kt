package com.l3on1kl.mviewer.domain.parser

import com.l3on1kl.mviewer.domain.model.MarkdownElement
import com.l3on1kl.mviewer.domain.model.MarkdownElementType
import java.util.UUID

class MarkdownParserImpl : MarkdownParser {

    override fun parse(content: String): List<MarkdownElement> {
        if (content.isBlank()) return emptyList()

        val out = mutableListOf<MarkdownElement>()
        val lines = content.lines()
        val listStack = ArrayDeque<ListCtx>()
        var rootIndent = 0

        var i = 0
        while (i < lines.size) {
            val raw = lines[i]
            val indent = raw.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
            val line = raw.trimEnd()
            if (listStack.isEmpty()) rootIndent = 0

            // ─── пустая строка ────────────────────────────────────────────────
            if (line.isBlank()) {
                listStack.clear()
                out += MarkdownElement(MarkdownElementType.Paragraph, "")
                i++; continue
            }

            // ─── заголовок H1–H6 ─────────────────────────────────────────────
            if (line.trimStart().startsWith('#')) {
                val trimmed = line.trimStart()
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                val text = trimmed.drop(level).trim().trimEnd('#').trim()
                out += MarkdownElement(
                    MarkdownElementType.Heading,
                    text,
                    mapOf("level" to level.toString())
                )
                i++; continue
            }

            /* ─── таблица ──────────────────────────────────────────────── */
            if (isTableRow(line)) {
                val start = i
                var end = i
                while (end < lines.size && isTableRow(lines[end])) end++

                lines.subList(start, end).forEach { row ->
                    val cells = row.trim().trim('|').split('|').map { it.trim() }

                    val isDivider = cells.all { cell ->
                        cell.isNotEmpty() && cell.all { ch -> ch == '-' || ch == ':' }
                    }
                    if (isDivider) return@forEach

                    out += MarkdownElement(
                        MarkdownElementType.Table,
                        row,
                        mapOf("cells" to cells.joinToString(";"))
                    )
                }
                i = end; continue
            }

            // ─── списки ──────────────────────────────────────────────────────
            detectListItem(raw)?.let { (ordered, body) ->
                if (listStack.isEmpty()) rootIndent = indent

                val relIndent = ((indent - rootIndent).coerceAtLeast(0)) / INDENT_SIZE
                val level = updateListStack(listStack, relIndent, ordered)

                val marker = if (ordered) {
                    listStack.joinToString(".") { it.counter.toString() } + "."
                } else {
                    "-".repeat(level + 1)
                }

                val liId = UUID.randomUUID().toString()
                val common = mapOf(
                    "ordered" to ordered.toString(),
                    "marker" to marker,
                    "level" to level.toString(),
                    "li" to liId
                )

                val fragments = parseInline(body)
                if (fragments.isEmpty()) {
                    out += MarkdownElement(MarkdownElementType.ListItem, body, common)
                } else {
                    out += fragments.first().copy(
                        type = MarkdownElementType.ListItem,
                        params = fragments.first().params + common
                    )
                    fragments.drop(1).forEach { out += it.copy(params = it.params + common) }
                }
                i++; continue
            }

            // ─── обычный параграф + inline ───────────────────────────────────
            listStack.clear()
            out += parseInline(line)
            i++
        }
        return out
    }

    /* ───────────────────────── helpers ───────────────────────── */

    private fun isTableRow(line: String): Boolean =
        "|" in line && (line.trimStart().startsWith('|') || line.trimEnd().endsWith('|')) &&
                line.count { it == '|' } >= 2

    private fun detectListItem(line: String): Pair<Boolean, String>? {
        val trimmed = line.trimStart()

        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ "))
            return false to trimmed.drop(2)

        val dot = trimmed.indexOf('.')
        if (dot > 0 && dot + 1 < trimmed.length && trimmed[dot + 1] == ' ') {
            val digits = trimmed.take(dot)
            if (digits.all(Char::isDigit))
                return true to trimmed.substring(dot + 2)
        }
        return null
    }

    private fun updateListStack(stack: ArrayDeque<ListCtx>, indent: Int, ordered: Boolean): Int {
        while (stack.isNotEmpty() && indent < stack.last().indent) stack.removeLast()

        if (stack.isEmpty() || indent > stack.last().indent ||
            (indent == stack.last().indent && stack.last().ordered != ordered)
        ) {
            stack.addLast(ListCtx(indent, ordered))
        }

        val ctx = stack.last()

        if (ordered) ctx.counter++
        return stack.size - 1
    }

    private data class ListCtx(
        val indent: Int,
        val ordered: Boolean,
        var counter: Int = 0
    )

    /* ───────────────────────── inline parser ───────────────────────── */
    private fun parseInline(text: String): List<MarkdownElement> {
        if (text.isBlank()) return emptyList()

        val out = mutableListOf<MarkdownElement>()
        val plain = StringBuilder()
        var i = 0

        fun flushPlain() {
            if (plain.isNotEmpty()) {
                out += MarkdownElement(MarkdownElementType.Paragraph, plain.toString())
                plain.clear()
            }
        }

        while (i < text.length) {
            // image ![alt](url)
            if (text.startsWith("![", i)) {
                val endAlt = text.indexOf(']', i + 2)
                val startUrl =
                    if (endAlt != -1 && endAlt + 1 < text.length && text[endAlt + 1] == '(') endAlt + 2 else -1
                val endUrl = if (startUrl != -1) text.indexOf(')', startUrl) else -1
                if (endAlt != -1 && endUrl != -1) {
                    flushPlain()
                    val alt = text.substring(i + 2, endAlt)
                    val url = text.substring(startUrl, endUrl)
                    out += MarkdownElement(
                        MarkdownElementType.Image,
                        alt.ifBlank { url },
                        mapOf("src" to url)
                    )
                    i = endUrl + 1; continue
                }
            }

            // bold **text** or __text__
            if (text.startsWith("**", i) || text.startsWith("__", i)) {
                val delim = text.substring(i, i + 2)
                val end = text.indexOf(delim, i + 2)
                if (end != -1) {
                    flushPlain()
                    val inner = text.substring(i + 2, end)
                    out += MarkdownElement(MarkdownElementType.Bold, inner)
                    i = end + 2; continue
                }
            }

            // strike ~~text~~
            if (text.startsWith("~~", i)) {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    flushPlain()
                    val inner = text.substring(i + 2, end)
                    out += MarkdownElement(MarkdownElementType.Strikethrough, inner)
                    i = end + 2; continue
                }
            }

            // italic *text* or _text_
            if ((text[i] == '*' || text[i] == '_') && (i == 0 || text[i - 1] != text[i])) {
                val delim = text[i]
                val end = text.indexOf(delim, i + 1)
                if (end != -1 && text.getOrNull(end + 1) != delim) {
                    flushPlain()
                    val inner = text.substring(i + 1, end)
                    out += MarkdownElement(MarkdownElementType.Italic, inner)
                    i = end + 1; continue
                }
            }

            /* обычный символ */
            if (text[i] == '\\' && i + 1 < text.length) {
                plain.append(text[i + 1]); i += 2; continue
            }
            plain.append(text[i]); i++
        }
        flushPlain()
        return out
    }

    private companion object {
        const val INDENT_SIZE: Int = 4
    }
}
