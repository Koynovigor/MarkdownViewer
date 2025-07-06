package com.l3on1kl.mviewer.presentation.util

import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan

fun CharSequence?.sameAs(other: CharSequence?): Boolean {
    if (this === other) return true

    if (this == null || other == null) return false

    if (!this.contentEquals(other)) return false

    val spannedThis = this as? Spanned
    val spannedOther = other as? Spanned

    if (spannedThis == null ||
        spannedOther == null
    ) return spannedThis === spannedOther

    val arrayThis = spannedThis.getSpans(
        0,
        spannedThis.length,
        Any::class.java
    )

    val arrayOther = spannedOther.getSpans(
        0,
        spannedOther.length,
        Any::class.java
    )

    if (arrayThis.size != arrayOther.size) return false

    fun Any.signature(
        spanned: Spanned
    ): String = buildString {
        append(javaClass.name)
        append('@').append(
            spanned.getSpanStart(this@signature)
        )
        append(':').append(
            spanned.getSpanEnd(this@signature)
        )
        append(':').append(
            spanned.getSpanFlags(this@signature)
        )
        when (this@signature) {
            is StyleSpan -> append(":").append(style)

            is StrikethroughSpan -> append(":strike")
        }
    }

    val stringsThis = arrayThis.map {
        it.signature(spannedThis)
    }.sorted()

    val stringsOther = arrayOther.map {
        it.signature(spannedOther)
    }.sorted()

    return stringsThis == stringsOther
}
