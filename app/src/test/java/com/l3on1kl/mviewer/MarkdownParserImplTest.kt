package com.l3on1kl.mviewer

import com.l3on1kl.mviewer.domain.model.MarkdownElementType
import com.l3on1kl.mviewer.domain.parser.MarkdownParserImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserImplTest {

    private val parser = MarkdownParserImpl()

    @Test
    fun parse_emptyContent_returnsEmptyList() {
        val elements = parser.parse("")
        assertTrue(elements.isEmpty())
    }

    @Test
    fun parse_headingLine_returnsHeadingElement() {
        val elements = parser.parse("# My heading")
        assertEquals(
            1,
            elements.size
        )

        val heading = elements.first()
        assertEquals(
            MarkdownElementType.Heading,
            heading.type
        )
        assertEquals(
            "My heading",
            heading.text
        )
        assertEquals(
            "1",
            heading.params["level"]
        )
    }

    @Test
    fun parse_paragraph_returnsParagraphElement() {
        val elements = parser.parse("Just a text line")
        assertEquals(
            1,
            elements.size
        )

        val paragraph = elements.first()
        assertEquals(
            MarkdownElementType.Paragraph,
            paragraph.type
        )
        assertEquals(
            "Just a text line",
            paragraph.text
        )
    }

    @Test
    fun parse_unorderedList_returnsListItems() {
        val markdownText = """ 
            - First
            - Second
        """.trimIndent()
        val elements = parser.parse(markdownText)

        assertEquals(
            2,
            elements.size
        )

        elements.forEachIndexed { itemIndex, listItem ->
            assertEquals(
                MarkdownElementType.ListItem,
                listItem.type
            )

            assertEquals(
                if (itemIndex == 0) "First" else "Second",
                listItem.text

            )
            assertEquals(
                "false",
                listItem.params["ordered"]
            )

            assertEquals(
                "0",
                listItem.params["level"]
            )

            assertTrue(
                listItem.params.containsKey("li")
            )
        }
    }

    @Test
    fun parse_orderedList_returnsOrderedListItems() {
        val markdownText = """ 
            1. One
            2. Two
        """.trimIndent()
        val elements = parser.parse(markdownText)

        assertEquals(
            2,
            elements.size
        )

        elements.forEachIndexed { itemIndex, listItem ->
            assertEquals(
                MarkdownElementType.ListItem,
                listItem.type
            )

            assertEquals(
                if (itemIndex == 0) "One" else "Two",
                listItem.text
            )

            assertEquals(
                "true",
                listItem.params["ordered"]
            )

            assertEquals(
                "0",
                listItem.params["level"]
            )
        }
    }

    @Test
    fun parse_nestedList_levelsAreCorrect() {
        val markdownText = """ 
            - Parent
                - Child
        """.trimIndent()
        val elements = parser.parse(markdownText)

        assertEquals(
            2,
            elements.size
        )

        val topLevelElement = elements[0]
        val subElement = elements[1]
        assertEquals(
            "0",
            topLevelElement.params["level"]
        )
        assertEquals(
            "1",
            subElement.params["level"]
        )
    }

    @Test
    fun parse_table_returnsTableElements() {
        val markdownText = """ 
            | Name | Age |
            | ---- | --- |
            | Bob  | 30  |
        """.trimIndent()
        val elements = parser.parse(markdownText)

        assertEquals(
            2,
            elements.size

        )
        val header = elements[0]
        val data = elements[1]
        assertEquals(
            MarkdownElementType.Table,
            header.type
        )
        assertEquals(
            "Name;Age",
            header.params["cells"]
        )
        assertEquals(
            "Bob;30",
            data.params["cells"]
        )
    }

    @Test
    fun parse_inlineFormatting_returnsSeparateElements() {
        val markdownText = "Start **bold** *italic* ~~strike~~ end"
        val elements = parser.parse(markdownText)
        val types = elements.map { it.type }

        assertEquals(
            listOf(
                MarkdownElementType.Paragraph,
                MarkdownElementType.Bold,
                MarkdownElementType.Paragraph,
                MarkdownElementType.Italic,
                MarkdownElementType.Paragraph,
                MarkdownElementType.Strikethrough,
                MarkdownElementType.Paragraph
            ),
            types
        )

        assertEquals(
            "bold",
            elements[1].text
        )
        assertEquals(
            "italic",
            elements[3].text
        )
        assertEquals(
            "strike",
            elements[5].text
        )
    }

    @Test
    fun parse_image_returnsImageElement() {
        val markdownText = "![Cat](http://example.com/cat.png)"
        val elements = parser.parse(markdownText)

        assertEquals(
            1,
            elements.size
        )

        val imageElement = elements.first()
        assertEquals(
            MarkdownElementType.Image,
            imageElement.type
        )
        assertEquals(
            "Cat",
            imageElement.text
        )
        assertEquals(
            "http://example.com/cat.png",
            imageElement.params["src"]
        )
    }
}
