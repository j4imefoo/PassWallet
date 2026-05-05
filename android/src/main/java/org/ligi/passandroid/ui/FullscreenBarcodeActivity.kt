package org.ligi.passandroid.ui

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.WindowManager
import org.ligi.passandroid.databinding.FullscreenImageBinding
import org.ligi.passandroid.model.pass.BarCode
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

class FullscreenBarcodeActivity : PassViewActivityBase() {

    private lateinit var binding: FullscreenImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasCurrentPass()) {
            return
        }
        supportActionBar?.hide()
        binding = FullscreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fullscreenBarcodeRoot.setOnClickListener { finish() }

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            this.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasCurrentPass()) {
            return
        }
        supportActionBar?.hide()

        val barcode = currentPass.barCode
        if (barcode == null) {
            Timber.w("FullscreenBarcodeActivity in bad state")
            finish() // this should never happen, but better safe than sorry
            return
        }

        val barcodeSize = getBarcodeSizeForScreen(barcode)
        val barcodeDrawable = barcode.getBitmap(resources, barcodeSize.first, barcodeSize.second) ?: run {
            Timber.w("FullscreenBarcodeActivity could not render barcode")
            finish()
            return
        }
        binding.fullscreenBarcode.setImageDrawable(barcodeDrawable)
        applyBarcodeSize(barcodeSize)

        if (barcode.alternativeText != null) {
            binding.alternativeBarcodeText.visibility = View.VISIBLE
            binding.alternativeBarcodeText.text = barcode.alternativeText
        } else {
            binding.alternativeBarcodeText.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean = false

    override fun onPrepareOptionsMenu(menu: Menu): Boolean = false

    private fun getBarcodeSizeForScreen(barcode: BarCode): Pair<Int, Int> {
        val metrics = resources.displayMetrics
        val outerPadding = (40 * metrics.density).toInt()
        val panelPadding = (36 * metrics.density).toInt()
        val maxWidth = max(1, metrics.widthPixels - outerPadding - panelPadding)
        val maxHeight = max(1, metrics.heightPixels - outerPadding - panelPadding)
        val quadratic = barcode.format?.isQuadratic() ?: true

        return if (quadratic) {
            val side = min(maxWidth, maxHeight)
            side to side
        } else {
            val height = min(maxHeight, max(1, (maxWidth * 0.2f).toInt()))
            maxWidth to height
        }
    }

    private fun applyBarcodeSize(size: Pair<Int, Int>) {
        binding.fullscreenBarcode.layoutParams = binding.fullscreenBarcode.layoutParams.apply {
            width = size.first
            height = size.second
        }
    }
}
