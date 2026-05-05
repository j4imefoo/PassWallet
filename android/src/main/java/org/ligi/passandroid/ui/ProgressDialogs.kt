package org.ligi.passandroid.ui

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.ligi.passandroid.R

fun Activity.createProgressDialog(
    message: CharSequence,
    @StringRes titleRes: Int? = null,
): AlertDialog {
    val content = layoutInflater.inflate(R.layout.dialog_progress, null).apply {
        findViewById<TextView>(R.id.progress_title).text = titleRes?.let { getString(it) } ?: getString(R.string.progress_dialog_title)
        findViewById<TextView>(R.id.progress_message).text = message
    }

    return MaterialAlertDialogBuilder(this)
        .setView(content)
        .setCancelable(false)
        .create()
        .apply {
            setOnShowListener {
                window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            }
        }
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
