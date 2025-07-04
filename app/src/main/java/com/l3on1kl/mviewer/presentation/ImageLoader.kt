package com.l3on1kl.mviewer.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageLoader {

    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /**
     * @param url абсолютный http/https URL картинки
     * @param reqWidth желаемая ширина (для расчёта inSampleSize)
     */
    suspend fun load(url: String, reqWidth: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            cache.get(url) ?: runCatching {
                val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                openStream(url).use { BitmapFactory.decodeStream(it, null, optsBounds) }

                val sample = calculateInSampleSize(optsBounds.outWidth, reqWidth)
                val optsDecode = BitmapFactory.Options().apply { inSampleSize = sample }

                openStream(url).use { BitmapFactory.decodeStream(it, null, optsDecode) }
            }.getOrNull()?.also { cache.put(url, it) }
        }

    private fun calculateInSampleSize(srcWidth: Int, reqWidth: Int): Int {
        var sample = 1
        var w = srcWidth
        while (w / 2 >= reqWidth) {
            sample *= 2
            w /= 2
        }
        return sample
    }

    private fun openStream(url: String): BufferedInputStream {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        return BufferedInputStream(conn.inputStream)
    }
}