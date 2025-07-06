package com.l3on1kl.mviewer.presentation.util

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

fun ContentResolver.persistReadPermissionIfPossible(uri: Uri) {
    if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        try {
            takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
    }
}
