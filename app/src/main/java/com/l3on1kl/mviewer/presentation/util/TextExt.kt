package com.l3on1kl.mviewer.presentation.util

fun CharSequence?.sameAs(other: CharSequence?): Boolean =
    when {
        this === other ->
            true

        this == null || other == null ->
            false

        this is String && other is String ->
            this == other

        this.length != other.length ->
            false

        else -> indices.all {
            this[it] == other[it]
        }
    }
