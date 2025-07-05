package com.l3on1kl.mviewer.domain.repository

import java.net.URL

sealed interface LoadRequest {
    val identifier: String

    data class Local(val path: String) : LoadRequest {
        override val identifier: String get() = path
    }

    data class Remote(val url: URL) : LoadRequest {
        override val identifier: String get() = url.toString()
    }
}
