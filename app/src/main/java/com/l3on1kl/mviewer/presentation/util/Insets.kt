package com.l3on1kl.mviewer.presentation.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.applySystemBarsPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { contentView, inst ->
        val mask = WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout()
        val insets = inst.getInsets(mask)

        contentView.setPadding(
            insets.left,
            insets.top,
            insets.right,
            insets.bottom
        )
        inst
    }
}