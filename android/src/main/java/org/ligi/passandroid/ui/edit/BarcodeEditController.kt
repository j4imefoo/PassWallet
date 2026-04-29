package org.ligi.passandroid.ui.edit

import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import org.ligi.kaxt.doAfterEdit
import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.BarCode
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import org.ligi.passandroid.ui.BarcodeUIController
import org.ligi.passandroid.ui.PassViewHelper
import java.util.EnumMap

class BarcodeEditController(
    private val rootView: View,
    internal val context: AppCompatActivity,
    barCode: BarCode,
    private val launchScan: (onScanResult: (String, String) -> Unit) -> Unit,
) {
    private var alternativeMessageInput: AppCompatEditText
    private var messageInput: AppCompatEditText
    private var barcodeFormat: PassBarCodeFormat?
    private val passFormatRadioButtons: MutableMap<PassBarCodeFormat, RadioButton> = EnumMap(PassBarCodeFormat::class.java)

    private fun bindRadio(formats: Array<PassBarCodeFormat>) {
        formats.forEach {
            val radioButton = RadioButton(context)
            rootView.findViewById<RadioGroup>(R.id.barcodeRadioGroup).addView(radioButton)
            passFormatRadioButtons[it] = radioButton

            radioButton.text = it.name
            radioButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    barcodeFormat = it
                    refresh()
                }
            }

            radioButton.isChecked = barcodeFormat == it
        }

    }

    init {
        barcodeFormat = barCode.format


        messageInput =rootView.findViewById(R.id.messageInput)
        alternativeMessageInput =rootView.findViewById(R.id.alternativeMessageInput)

        rootView.findViewById<View>(R.id.scanButton).setOnClickListener {
            launchScan(::applyScanResult)
        }

        bindRadio(PassBarCodeFormat.values())

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
        passFormatRadioButtons[format]?.let {
            rootView.findViewById<RadioGroup>(R.id.barcodeRadioGroup).check(it.id)
        }
        refresh()
    }

    fun refresh() {
        val barcodeUIController = BarcodeUIController(rootView, getBarCode(), context, PassViewHelper(context))
        val isBarcodeShown = barcodeUIController.getBarcodeView().visibility == View.VISIBLE

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
}
