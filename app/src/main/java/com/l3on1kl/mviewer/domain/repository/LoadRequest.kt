package com.l3on1kl.mviewer.domain.repository

import android.net.Uri
import java.net.URL

sealed interface LoadRequest {
    data class Local(val uri: Uri) : LoadRequest
    data class Remote(val url: URL) : LoadRequest
}
