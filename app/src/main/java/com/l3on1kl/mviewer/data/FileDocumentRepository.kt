package com.l3on1kl.mviewer.data

import com.l3on1kl.mviewer.domain.DocumentRepository
import com.l3on1kl.mviewer.domain.model.MarkdownDocument
import java.io.File
import java.util.UUID

/**
 * Simple implementation of [com.l3on1kl.mviewer.domain.DocumentRepository] that works with local files.
 */
class FileDocumentRepository : DocumentRepository {

    override fun load(path: String): MarkdownDocument {
        val file = File(path)
        val content = if (file.exists()) file.readText() else ""
        val id = file.nameWithoutExtension.ifEmpty { UUID.randomUUID().toString() }
        return MarkdownDocument(id = id, content = content, path = file.absolutePath)
    }

    override fun save(document: MarkdownDocument) {
        val destPath = document.path ?: document.id
        File(destPath).writeText(document.content)
    }
}
