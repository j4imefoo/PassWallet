package org.ligi.passandroid.ui

import android.app.Activity
import android.app.Dialog
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import kotlin.math.roundToInt

fun Activity.createProgressDialog(
    message: CharSequence,
    @StringRes titleRes: Int? = null,
): AlertDialog {
    val padding = (24 * resources.displayMetrics.density).roundToInt()
    val spacing = (16 * resources.displayMetrics.density).roundToInt()

    val content = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(padding, padding, padding, padding)
        addView(ProgressBar(context))
        addView(TextView(context).apply {
            text = message
            setPadding(spacing, 0, 0, 0)
        })
    }

    return AlertDialog.Builder(this).apply {
        if (titleRes != null) {
            setTitle(titleRes)
        }
        setView(content)
        setCancelable(false)
    }.create()
}

fun Dialog.dismissSafely() {
    if (!isShowing) {
        return
    }

    try {
        dismiss()
    } catch (_: IllegalArgumentException) {
        // The hosting activity is already gone.
    }
}
