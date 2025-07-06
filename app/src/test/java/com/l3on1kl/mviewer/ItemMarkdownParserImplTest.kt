package com.l3on1kl.mviewer

import com.l3on1kl.mviewer.domain.model.MarkdownElementType
import com.l3on1kl.mviewer.domain.parser.MarkdownParserImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemMarkdownParserImplTest {

    private val parser = MarkdownParserImpl()

    @Test
    fun parse_backslashEscaped_keepsLiteralCharacters() {
        val element = parser.parse(
            "Escaped \\*asterisk\\* stays literal"

        )
        assertEquals(
            1,
            element.size
        )

        assertEquals(
            MarkdownElementType.Paragraph,
            element[0].type
        )

        assertEquals(
            "Escaped *asterisk* stays literal",
            element[0].text
        )
    }

    @Test
    fun parse_imageWithoutAlt_usesUrlAsText() {
        val url = "http://example.com/x.png"
        val element = parser.parse(
            "![]($url)"
        ).single()

        assertEquals(
            MarkdownElementType.Image,
            element.type
        )

        assertEquals(
            url,
            element.text
        )

        assertEquals(
            url,
            element.params["src"]
        )
    }

    @Test
    fun parse_listWithoutSpaceAfterDash_supported() {
        val element = parser.parse(
            "-text"
        ).single()

        assertEquals(
            MarkdownElementType.ListItem,
            element.type
        )

        assertEquals(
            "text",
            element.text
        )

        assertEquals(
            "false",
            element.params["ordered"]
        )

        assertEquals(
            "-",
            element.params["marker"]
        )
    }

    @Test
    fun parse_deepNestedMixedList_levelsCorrect() {
        val content = """
            - first
                1. inner-one
                    - inner-inner
        """.trimIndent()
        val element = parser.parse(content)

        assertEquals(
            3,
            element.size
        )

        assertEquals(
            "0",
            element[0].params["level"]
        )

        assertEquals(
            "1",
            element[1].params["level"]
        )

        assertEquals(
            "2",
            element[2].params["level"]
        )

        assertEquals(
            "false",
            element[2].params["ordered"]
        )
    }

    @Test
    fun parse_blankLine_createsEmptyParagraphElement() {
        val content = "Line1\n\nLine2"
        val element = parser.parse(content)

        assertEquals(
            3,
            element.size
        )

        assertTrue(element[1].text.isEmpty())
    }

    @Test
    fun parse_boldAndItalicNested_areSeparated() {
        val content = "**bold _and italic_ inside**"
        val element = parser.parse(content)
        val types = element.map {
            it.type
        }

        assertEquals(
            listOf(
                MarkdownElementType.Bold,
                MarkdownElementType.Italic,
                MarkdownElementType.Bold
            ),
            types
        )
    }

    @Test
    fun parse_table_dividerLineIgnored() {
        val content = """
            | A | B |
            |---|---|
            | 1 | 2 |
            | 3 | 4 |
        """.trimIndent()
        val element = parser.parse(content)

        assertEquals(
            3,
            element.size
        )

        assertEquals(
            "A;B",
            element[0].params["cells"]
        )

        assertEquals(
            "1;2",
            element[1].params["cells"]
        )

        assertEquals(
            "3;4",
            element[2].params["cells"]
        )
    }

    @Test
    fun parse_strikeThenBold_sequencePreserved() {
        val content = "~~gone~~ **stay**"
        val element = parser.parse(content)
        val type = element.map {
            it.type
        }

        assertEquals(
            listOf(
                MarkdownElementType.Strikethrough,
                MarkdownElementType.Paragraph,
                MarkdownElementType.Bold
            ),
            type
        )
    }
}
