package org.ligi.passandroid.ui

import android.app.Activity
import android.view.View
import android.view.View.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import org.ligi.kaxt.getSmallestSide
import org.ligi.passandroid.R
import org.ligi.passandroid.model.pass.BarCode
import kotlin.math.max
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

            val barcodeSize = getBarCodeSize(smallestSide)
            val bitmapDrawable = barCode.getBitmap(activity.resources, barcodeSize.first, barcodeSize.second)
            if (bitmapDrawable != null) {
                barcodeImage.setImageDrawable(bitmapDrawable)
                barcodeImage.visibility = VISIBLE
                barcodePanel.visibility = VISIBLE
                setBarCodeSize(barcodeSize)
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

    private fun getBarCodeSize(smallestSide: Int): Pair<Int, Int> {
        val quadratic = barCode!!.format?.isQuadratic() ?: true
        val maxBarcodeWidth = (passViewHelper.windowWidth - passViewHelper.fingerSize * 2).coerceAtLeast(1)
        val width = if (quadratic) {
            min((smallestSide * 0.66f).toInt(), maxBarcodeWidth)
        } else {
            maxBarcodeWidth
        }
        val height = if (quadratic) width else max(passViewHelper.fingerSize, (width * 0.2f).toInt())
        return width to height
    }

    private fun setBarCodeSize(size: Pair<Int, Int>) {
        barcodeImage.layoutParams = FrameLayout.LayoutParams(size.first, size.second, android.view.Gravity.CENTER)
    }
}
