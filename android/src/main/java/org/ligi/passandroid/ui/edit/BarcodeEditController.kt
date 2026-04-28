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
import org.ligi.passandroid.model.pass.PassBarCodeFormat.*
import org.ligi.passandroid.ui.BarcodeUIController
import org.ligi.passandroid.ui.PassViewHelper
import java.util.*

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

        rootView.findViewById<AppCompatImageButton>(R.id.randomButton).setOnClickListener {
            messageInput.setText(when (barcodeFormat) {
                EAN_8 -> getRandomEAN8()
                EAN_13 -> getRandomEAN13()
                ITF -> getRandomITF()
                else -> UUID.randomUUID().toString().uppercase(Locale.ROOT)
            })
            refresh()
        }

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
        messageInput.setText(newMessage)
        rootView.findViewById<RadioGroup>(R.id.barcodeRadioGroup).check(passFormatRadioButtons[PassBarCodeFormat.valueOf(newFormat)]!!.id)
        refresh()
    }

    fun refresh() {
        val barcodeUIController = BarcodeUIController(rootView, getBarCode(), context, PassViewHelper(context))
        val isBarcodeShown = barcodeUIController.getBarcodeView().visibility == View.VISIBLE

        if (!isBarcodeShown) {
            messageInput.error = "Invalid message"
        } else {
            messageInput.error = null
        }
    }

    fun getBarCode() = BarCode(barcodeFormat, messageInput.text.toString()).apply {
        val newAlternativeText = alternativeMessageInput.text.toString()
        if (newAlternativeText.isNotEmpty()) {
            alternativeText = newAlternativeText
        }
    }
}
