package com.l3on1kl.mviewer.domain.repository

import java.io.File
import java.net.URL

sealed interface LoadRequest {
    data class Local(val file: File): LoadRequest
    data class Remote(val url: URL): LoadRequest
}
