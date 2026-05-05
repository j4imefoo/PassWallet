package org.ligi.passandroid.ui.edit.dialogs

import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.ligi.kaxt.inflate
import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.ui.edit.BarcodeEditController

fun showBarcodeEditDialog(
    context: AppCompatActivity,
    refreshCallback: () -> Unit,
    pass: Pass,
    barCode: BarCode,
    launchScan: (onScanResult: (String, String) -> Unit) -> Unit,
) {
    val view = context.inflate(R.layout.barcode_edit)
    val barcodeEditController = BarcodeEditController(view, context, barCode, launchScan)

    val dialog = MaterialAlertDialogBuilder(context)
        .setView(view)
        .create()

    view.findViewById<View>(R.id.cancelBarcodeButton).setOnClickListener {
        dialog.dismiss()
    }
    view.findViewById<View>(R.id.saveBarcodeButton).setOnClickListener {
        pass.barCode = barcodeEditController.getBarCode()
        refreshCallback.invoke()
        dialog.dismiss()
    }
    dialog.setOnShowListener {
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    }
    dialog.show()
}
