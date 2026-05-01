package org.ligi.passandroid.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.ligi.passandroid.R

fun Activity.showImportErrorDialog(
    title: CharSequence,
    message: CharSequence,
    finishOnOk: Boolean = false,
) {
    if (isFinishing) {
        return
    }

    val content = layoutInflater.inflate(R.layout.dialog_import_error, null).apply {
        findViewById<TextView>(R.id.error_title).text = title
        findViewById<TextView>(R.id.error_message).text = message
    }

    val dialog = MaterialAlertDialogBuilder(this)
        .setView(content)
        .setCancelable(!finishOnOk)
        .create()

    content.findViewById<MaterialButton>(R.id.error_ok).setOnClickListener {
        dialog.dismissSafely()
        if (finishOnOk && !isFinishing) {
            finish()
        }
    }

    dialog.setOnShowListener {
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
    dialog.show()
}
