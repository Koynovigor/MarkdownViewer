package com.l3on1kl.mviewer.presentation.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

fun getFileName(
    contentResolver: ContentResolver,
    uri: Uri
): String? {
    return contentResolver.query(
        uri,
        null,
        null,
        null,
        null
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(
            OpenableColumns.DISPLAY_NAME
        )

        if (cursor.moveToFirst() && nameIndex != -1) {
            cursor.getString(nameIndex)
        } else null
    }
}
