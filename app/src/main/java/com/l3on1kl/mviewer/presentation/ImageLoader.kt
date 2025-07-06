package com.l3on1kl.mviewer.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

object ImageLoader {

    interface Listener {
        fun onError(ex: Throwable)
    }

    var listener: Listener? = null
    private const val IO_TIMEOUT_MS = 5_000
    private val cache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(
            key: String,
            value: Bitmap
        ) = value.byteCount
    }
    private var errorReported = false

    suspend fun load(
        url: String,
        requestedWidth: Int
    ): Bitmap? {
        cache.get(url)?.let { image ->
            return image
        }

        return withContext(
            Dispatchers.IO
        ) {
            cache.get(url) ?: runCatching {
                val bytes = openBytes(url)
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.size,
                    boundsOptions
                )

                val sampleSize = calculateInSampleSize(
                    boundsOptions.outWidth,
                    requestedWidth
                )
                val decodingOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }

                BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.size,
                    decodingOptions
                )
            }.onFailure { exception ->
                if (!errorReported &&
                    (exception is UnknownHostException || exception is ConnectException)
                ) {
                    errorReported = true
                    withContext(
                        Dispatchers.Main
                    ) {
                        listener?.onError(exception)
                    }
                }
            }.getOrNull()?.also {
                cache.put(
                    url,
                    it
                )
            }
        }
    }

    fun clear() {
        cache.evictAll()
        errorReported = false
    }

    private fun calculateInSampleSize(
        imageWidth: Int,
        targetWidth: Int
    ): Int {
        var scaleFactor = 1
        var scaledWidth = imageWidth

        while (scaledWidth / 2 >= targetWidth) {
            scaleFactor *= 2
            scaledWidth /= 2
        }

        return scaleFactor
    }

    private fun openBytes(
        url: String
    ): ByteArray =
        (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = IO_TIMEOUT_MS
            readTimeout = IO_TIMEOUT_MS
            BufferedInputStream(inputStream).use {
                it.readBytes()
            }
        }

    suspend fun loadOriginal(
        url: String
    ): Bitmap? {
        cache.get(url)?.let {
            return it
        }

        return withContext(
            Dispatchers.IO
        ) {
            runCatching {
                val bytes = openBytes(url)
                BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.size
                )
            }.onFailure { exception ->
                if (!errorReported &&
                    (exception is UnknownHostException || exception is ConnectException)
                ) {
                    errorReported = true
                    withContext(
                        Dispatchers.Main
                    ) {
                        listener?.onError(exception)
                    }
                }
            }.getOrNull()?.also {
                cache.put(
                    url,
                    it
                )
            }
        }
    }
}
