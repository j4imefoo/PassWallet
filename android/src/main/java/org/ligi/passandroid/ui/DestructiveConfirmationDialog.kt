package org.ligi.passandroid.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.ligi.passandroid.R

fun Activity.showDestructiveConfirmationDialog(
    @StringRes titleRes: Int,
    message: CharSequence,
    @StringRes confirmTextRes: Int,
    @DrawableRes iconRes: Int = R.drawable.ic_action_delete,
    checkboxText: CharSequence? = null,
    onConfirm: (checkboxChecked: Boolean) -> Unit
) {
    val view = LayoutInflater.from(this).inflate(R.layout.dialog_destructive_confirmation, null)
    view.findViewById<ImageView>(R.id.confirmationIcon).setImageResource(iconRes)
    view.findViewById<TextView>(R.id.confirmationTitle).setText(titleRes)
    view.findViewById<TextView>(R.id.confirmationMessage).text = message

    val checkBox = view.findViewById<CheckBox>(R.id.sourceDeleteCheckbox)
    if (checkboxText == null) {
        checkBox.visibility = View.GONE
    } else {
        checkBox.visibility = View.VISIBLE
        checkBox.text = checkboxText
    }

    val dialog = MaterialAlertDialogBuilder(this)
        .setView(view)
        .create()

    view.findViewById<MaterialButton>(R.id.confirmationCancel).setOnClickListener {
        dialog.dismiss()
    }
    view.findViewById<MaterialButton>(R.id.confirmationConfirm).apply {
        setText(confirmTextRes)
        setOnClickListener {
            onConfirm(checkBox.isChecked)
            dialog.dismiss()
        }
    }

    dialog.setOnShowListener {
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
    dialog.show()
}
