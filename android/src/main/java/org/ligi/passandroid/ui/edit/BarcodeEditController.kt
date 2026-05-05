package org.ligi.passandroid.ui.edit

import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.isVisible
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import org.ligi.kaxt.doAfterEdit
import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.ui.BarcodeUIController
import org.ligi.passandroid.ui.PassViewHelper

class BarcodeEditController(
    private val rootView: View,
    internal val context: AppCompatActivity,
    barCode: BarCode,
    private val launchScan: (onScanResult: (String, String) -> Unit) -> Unit,
) {
    private var alternativeMessageInput: AppCompatEditText
    private var messageInput: AppCompatEditText
    private lateinit var barcodeFormatInput: MaterialAutoCompleteTextView
    private var barcodeFormat: PassBarCodeFormat?
    private val supportedFormats = PassBarCodeFormat.values()
    private val supportedFormatLabels = supportedFormats.map { it.displayName() }

    private fun bindFormatDropdown() {
        barcodeFormatInput = rootView.findViewById(R.id.barcodeFormatInput)
        barcodeFormatInput.setAdapter(
            ArrayAdapter(
                context,
                android.R.layout.simple_list_item_1,
                supportedFormatLabels,
            ),
        )
        barcodeFormatInput.setOnItemClickListener { _, _, position, _ ->
            barcodeFormat = supportedFormats[position]
            refresh()
        }
        barcodeFormatInput.setOnClickListener {
            barcodeFormatInput.showDropDown()
        }
        setSelectedFormat(barcodeFormat ?: PassBarCodeFormat.QR_CODE)
    }

    private fun setSelectedFormat(format: PassBarCodeFormat) {
        barcodeFormat = format
        barcodeFormatInput.setText(format.displayName(), false)
    }

    init {
        barcodeFormat = barCode.format

        messageInput = rootView.findViewById(R.id.messageInput)
        alternativeMessageInput = rootView.findViewById(R.id.alternativeMessageInput)

        rootView.findViewById<View>(R.id.scanButton).setOnClickListener {
            launchScan(::applyScanResult)
        }

        bindFormatDropdown()

        messageInput.setText(barCode.message)
        messageInput.doAfterEdit {
            refresh()
        }

        alternativeMessageInput.setText(barCode.alternativeText)

        refresh()
    }

    fun applyScanResult(newFormat: String, newMessage: String) {
        val format = runCatching { PassBarCodeFormat.valueOf(newFormat) }.getOrDefault(PassBarCodeFormat.QR_CODE)
        messageInput.setText(newMessage)
        setSelectedFormat(format)
        refresh()
    }

    fun refresh() {
        val barcodeUIController = BarcodeUIController(rootView, getBarCode(), context, PassViewHelper(context))
        val isBarcodeShown = barcodeUIController.getBarcodeView().isVisible

        val message = messageInput.text?.toString().orEmpty()
        if (message.isBlank() || isBarcodeShown) {
            messageInput.error = null
        } else {
            messageInput.error = context.getString(R.string.invalid_barcode_message)
        }
    }

    fun getBarCode() = BarCode(barcodeFormat, messageInput.text.toString()).apply {
        val newAlternativeText = alternativeMessageInput.text.toString()
        if (newAlternativeText.isNotEmpty()) {
            alternativeText = newAlternativeText
        }
    }

    private fun PassBarCodeFormat.displayName() = name.replace('_', ' ')
}
