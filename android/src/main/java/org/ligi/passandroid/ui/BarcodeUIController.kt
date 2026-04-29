package org.ligi.passandroid.ui

import android.app.Activity
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import org.ligi.kaxt.getSmallestSide
import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.BarCode
import kotlin.math.min

internal class BarcodeUIController(private val rootView: View, private val barCode: BarCode?, activity: Activity, private val passViewHelper: PassViewHelper) {

    fun getBarcodeView(): ImageView = rootView.findViewById(R.id.barcode_img)

    private val zoomIn = rootView.findViewById<AppCompatImageView>(R.id.zoomIn)
    private val zoomOut = rootView.findViewById<AppCompatImageView>(R.id.zoomOut)
    private val barcodeImage = rootView.findViewById<ImageView>(R.id.barcode_img)
    private val barcodeAltText = rootView.findViewById<TextView>(R.id.barcode_alt_text)
    private val barcodePanel = rootView.findViewById<View>(R.id.barcode_panel)

    init {
        zoomIn.visibility = GONE
        zoomOut.visibility = GONE

        if (barCode != null) {
            val smallestSide = activity.windowManager.getSmallestSide()

            val bitmapDrawable = barCode.getBitmap(activity.resources)
            if (bitmapDrawable != null) {
                barcodeImage.setImageDrawable(bitmapDrawable)
                barcodeImage.visibility = VISIBLE
                barcodePanel.visibility = VISIBLE
                setBarCodeSize(smallestSide)
            } else {
                barcodeImage.visibility = GONE
                barcodePanel.visibility = GONE
            }

            if (barCode.alternativeText != null) {
                barcodeAltText.text = barCode.alternativeText
                barcodeAltText.visibility = VISIBLE
            } else {
                barcodeAltText.visibility = GONE
            }

        } else {
            passViewHelper.setBitmapSafe(barcodeImage, null)
            barcodePanel.visibility = GONE
            barcodeAltText.visibility = GONE
        }
    }

    private fun setBarCodeSize(smallestSide: Int) {
        val quadratic = barCode!!.format!!.isQuadratic()
        val maxBarcodeWidth = passViewHelper.windowWidth - passViewHelper.fingerSize * 2
        val width = if (quadratic) {
            min((smallestSide * 0.72f).toInt(), maxBarcodeWidth)
        } else {
            maxBarcodeWidth
        }
        barcodeImage.layoutParams = FrameLayout.LayoutParams(width, if (quadratic) width else ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
