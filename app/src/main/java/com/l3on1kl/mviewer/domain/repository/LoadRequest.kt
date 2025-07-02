package com.l3on1kl.mviewer.domain.repository

import java.net.URL

sealed interface LoadRequest {
    data class Local(val path: String) : LoadRequest
    data class Remote(val url: URL) : LoadRequest
}
