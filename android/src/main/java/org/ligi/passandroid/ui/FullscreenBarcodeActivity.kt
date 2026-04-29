package org.ligi.passandroid.ui

import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.WindowManager
import org.ligi.passandroid.databinding.FullscreenImageBinding
import timber.log.Timber
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class FullscreenBarcodeActivity : PassViewActivityBase() {

    private lateinit var binding: FullscreenImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        supportActionBar?.hide()

        val barcode = currentPass.barCode
        if (barcode == null) {
            Timber.w("FullscreenBarcodeActivity in bad state")
            finish() // this should never happen, but better safe than sorry
            return
        }

        val barcodeDrawable = barcode.getBitmap(resources) ?: run {
            Timber.w("FullscreenBarcodeActivity could not render barcode")
            finish()
            return
        }
        binding.fullscreenBarcode.setImageDrawable(barcodeDrawable)
        sizeBarcodeForScreen(barcodeDrawable)

        if (barcode.alternativeText != null) {
            binding.alternativeBarcodeText.visibility = View.VISIBLE
            binding.alternativeBarcodeText.text = barcode.alternativeText
        } else {
            binding.alternativeBarcodeText.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean = false

    override fun onPrepareOptionsMenu(menu: Menu): Boolean = false

    private fun sizeBarcodeForScreen(barcodeDrawable: BitmapDrawable) {
        val metrics = resources.displayMetrics
        val outerPadding = (40 * metrics.density).toInt()
        val panelPadding = (36 * metrics.density).toInt()
        val maxWidth = metrics.widthPixels - outerPadding - panelPadding
        val maxHeight = metrics.heightPixels - outerPadding - panelPadding
        val bitmap = barcodeDrawable.bitmap

        // Upscale only by whole pixels; downscale when needed so the right end is never clipped.
        val fitScale = min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
        val scale = if (fitScale >= 1f) floor(fitScale) else fitScale
        val width = max(1, (bitmap.width * scale).toInt())
        val height = max(1, (bitmap.height * scale).toInt())

        binding.fullscreenBarcode.layoutParams = binding.fullscreenBarcode.layoutParams.apply {
            this.width = width
            this.height = height
        }
    }
}
