package org.ligi.passandroid.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.google.android.material.button.MaterialButton
import org.ligi.passandroid.R
import java.util.EnumMap
import java.util.concurrent.Executors

class ScanBarcodeActivity : AppCompatActivity() {

    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var torchButton: MaterialButton? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var torchEnabled = false
    private var deliveredResult = false
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val zxingReader = MultiFormatReader().apply {
        setHints(
            EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.TRY_HARDER, true)
                put(DecodeHintType.POSSIBLE_FORMATS, BarcodeFormat.entries)
            },
        )
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startCamera()
        } else {
            showPermissionDenied()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            decodePickedImage(uri)
        } else if (hasCameraPermission()) {
            startCamera()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(createContentView())

        if (hasCameraPermission()) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun createContentView(): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        root.addView(previewView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        root.addView(ScannerOverlayView(this), FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        val topPanel = createTopPanel()
        root.addView(
            topPanel,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP
                setMargins(18.dp, 18.dp, 18.dp, 0)
            },
        )

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = roundedDrawable(0xCC111820.toInt(), 34.dp.toFloat(), 0x33FFFFFF, 1.dp)
            setPadding(10.dp, 10.dp, 10.dp, 10.dp)
        }

        val flashAction = scannerButton(R.drawable.ic_flash_24, getString(R.string.scan_flash)) {
            toggleTorch()
        }
        torchButton = flashAction
        buttons.addView(flashAction)
        buttons.addView(
            scannerButton(R.drawable.ic_image_24, getString(R.string.scan_read_image), 72) {
                cameraProvider?.unbindAll()
                imagePickerLauncher.launch("image/*")
            },
        )
        buttons.addView(
            scannerButton(R.drawable.ic_camera_rotate_24, getString(R.string.scan_rotate_camera)) {
                switchCamera()
            },
        )

        root.addView(
            buttons,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setMargins(18.dp, 0, 18.dp, 28.dp)
            },
        )

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topPanel.updateLayoutParams<FrameLayout.LayoutParams> {
                topMargin = 18.dp + systemBars.top
            }
            buttons.updateLayoutParams<FrameLayout.LayoutParams> {
                bottomMargin = 28.dp + systemBars.bottom
            }
            insets
        }
        ViewCompat.requestApplyInsets(root)

        return root
    }

    private fun createTopPanel(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedDrawable(0xAA111820.toInt(), 24.dp.toFloat(), 0x22FFFFFF, 1.dp)
            setPadding(10.dp, 10.dp, 16.dp, 10.dp)
        }

        panel.addView(
            ImageButton(this).apply {
                contentDescription = getString(android.R.string.cancel)
                setImageResource(R.drawable.ic_close_24)
                setColorFilter(Color.WHITE)
                background = roundedDrawable(0x33FFFFFF, 20.dp.toFloat())
                scaleType = android.widget.ImageView.ScaleType.CENTER
                setOnClickListener { finish() }
            },
            LinearLayout.LayoutParams(40.dp, 40.dp),
        )

        panel.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14.dp, 0, 0, 0)
                addView(TextView(this@ScanBarcodeActivity).apply {
                    text = getString(R.string.scan_title)
                    setTextColor(Color.WHITE)
                    textSize = 18f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                })
                addView(TextView(this@ScanBarcodeActivity).apply {
                    text = getString(R.string.scan_hint)
                    setTextColor(0xCCFFFFFF.toInt())
                    textSize = 13f
                })
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )

        return panel
    }

    private fun scannerButton(@DrawableRes iconRes: Int, text: String, sizeDp: Int = 62, onClick: () -> Unit): MaterialButton {
        return MaterialButton(this).apply {
            contentDescription = text
            this.text = ""
            minWidth = 0
            minHeight = 0
            minimumWidth = 0
            minimumHeight = 0
            gravity = Gravity.CENTER
            icon = ContextCompat.getDrawable(this@ScanBarcodeActivity, iconRes)
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            iconPadding = 0
            iconTint = ColorStateList.valueOf(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(0x22FFFFFF)
            strokeColor = ColorStateList.valueOf(0x33FFFFFF)
            strokeWidth = 1.dp
            cornerRadius = sizeDp.dp / 2
            insetTop = 0
            insetBottom = 0
            setPadding(0, 0, 0, 0)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(sizeDp.dp, sizeDp.dp).apply {
                marginStart = 6.dp
                marginEnd = 6.dp
            }
        }
    }

    private fun roundedDrawable(color: Int, radius: Float, strokeColor: Int? = null, strokeWidth: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(color)
            if (strokeColor != null && strokeWidth > 0) {
                setStroke(strokeWidth, strokeColor)
            }
        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        if (deliveredResult) return
        val previewTarget = previewView ?: return
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider
            bindCamera(provider, previewTarget)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera(provider: ProcessCameraProvider, previewTarget: PreviewView) {
        provider.unbindAll()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewTarget.surfaceProvider
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(cameraExecutor) { image ->
            try {
                if (deliveredResult) return@setAnalyzer
                val result = decode(image) ?: return@setAnalyzer
                deliveredResult = true
                runOnUiThread { deliverResult(result.barcodeFormat.name, result.text) }
            } catch (_: RuntimeException) {
                // Keep scanning. Camera analyzers should be boring, not crashy.
            } finally {
                image.close()
            }
        }

        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        try {
            camera = provider.bindToLifecycle(this, selector, preview, analysis)
            camera?.cameraControl?.enableTorch(torchEnabled)
            updateTorchButton()
        } catch (_: IllegalArgumentException) {
            lensFacing = CameraSelector.LENS_FACING_BACK
            torchEnabled = false
            bindCamera(provider, previewTarget)
        }
    }

    private fun decode(image: ImageProxy): Result? {
        val source = image.toLuminanceSource()
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val invertedBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
        return listOf(bitmap, invertedBitmap).firstNotNullOfOrNull { candidate -> decodeBinaryBitmap(candidate) }
    }

    private fun decodePickedImage(uri: Uri) {
        cameraProvider?.unbindAll()
        cameraExecutor.execute {
            val result = runCatching { loadBitmap(uri)?.let(::decodeBitmap) }.getOrNull()
            runOnUiThread {
                if (result != null) {
                    deliveredResult = true
                    deliverResult(result.barcodeFormat.name, result.text)
                } else {
                    Toast.makeText(this, R.string.scan_image_failed, Toast.LENGTH_SHORT).show()
                    if (hasCameraPermission()) startCamera()
                }
            }
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val maxSize = 1800
        var sampleSize = 1
        while ((bounds.outWidth / sampleSize) > maxSize || (bounds.outHeight / sampleSize) > maxSize) {
            sampleSize *= 2
        }

        return contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(
                it,
                null,
                BitmapFactory.Options().apply { inSampleSize = sampleSize },
            )
        }
    }

    private fun decodeBitmap(bitmap: Bitmap): Result? {
        return listOf(0f, 90f, 180f, 270f).firstNotNullOfOrNull { degrees ->
            val candidate = if (degrees == 0f) bitmap else rotateBitmap(bitmap, degrees)
            try {
                candidate?.let(::decodeBitmapCandidate)
            } finally {
                if (candidate != null && candidate !== bitmap) {
                    candidate.recycle()
                }
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap? {
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
        }.getOrNull()
    }

    private fun decodeBitmapCandidate(bitmap: Bitmap): Result? {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val normal = BinaryBitmap(HybridBinarizer(source))
        val inverted = BinaryBitmap(HybridBinarizer(source.invert()))
        return listOf(normal, inverted).firstNotNullOfOrNull { candidate -> decodeBinaryBitmap(candidate) }
    }

    private fun decodeBinaryBitmap(candidate: BinaryBitmap): Result? {
        return synchronized(zxingReader) {
            try {
                zxingReader.decodeWithState(candidate).takeIf { it.text.isNotBlank() }
            } catch (_: NotFoundException) {
                null
            } finally {
                zxingReader.reset()
            }
        }
    }

    private fun ImageProxy.toLuminanceSource(): PlanarYUVLuminanceSource {
        val plane = planes.first()
        val width = width
        val height = height
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val data = ByteArray(width * height)
        var outputOffset = 0
        for (row in 0 until height) {
            buffer.position(row * rowStride)
            buffer.get(data, outputOffset, width)
            outputOffset += width
        }
        return PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
    }

    private fun deliverResult(format: String, text: String) {
        if (isFinishing || isDestroyed) return
        setResult(
            Activity.RESULT_OK,
            Intent().apply {
                putExtra(EXTRA_SCAN_RESULT_FORMAT, format)
                putExtra(EXTRA_SCAN_RESULT, text)
            },
        )
        finish()
    }

    private fun toggleTorch() {
        val currentCamera = camera ?: return
        if (!currentCamera.cameraInfo.hasFlashUnit()) return
        torchEnabled = !torchEnabled
        currentCamera.cameraControl.enableTorch(torchEnabled)
        updateTorchButton()
    }

    private fun updateTorchButton() {
        val activeColor = if (torchEnabled) 0xFFD8A657.toInt() else Color.WHITE
        val backgroundColor = if (torchEnabled) 0x33D8A657 else 0x22FFFFFF
        torchButton?.apply {
            setTextColor(activeColor)
            iconTint = ColorStateList.valueOf(activeColor)
            backgroundTintList = ColorStateList.valueOf(backgroundColor)
        }
    }

    private fun switchCamera() {
        val provider = cameraProvider ?: return
        val nextLens = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        val selector = CameraSelector.Builder().requireLensFacing(nextLens).build()
        if (!provider.hasCamera(selector)) return
        lensFacing = nextLens
        torchEnabled = false
        updateTorchButton()
        previewView?.let { bindCamera(provider, it) }
    }

    private fun showPermissionDenied() {
        (findViewById<View>(android.R.id.content) as? FrameLayout)?.addView(
            TextView(this).apply {
                text = getString(R.string.camera_permission_required)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(32.dp, 32.dp, 32.dp, 32.dp)
            },
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission()) startCamera()
    }

    override fun onPause() {
        cameraProvider?.unbindAll()
        super.onPause()
    }

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdownNow()
        super.onDestroy()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_SCAN_RESULT = "SCAN_RESULT"
        const val EXTRA_SCAN_RESULT_FORMAT = "SCAN_RESULT_FORMAT"
    }
}

private class ScannerOverlayView(context: android.content.Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val shadePaint = Paint().apply { color = 0xA6000000.toInt() }
    private val clearPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD8A657.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 5f * density
    }
    private val scanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCD8A657.toInt()
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2f * density
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val frameSize = width * 0.72f
        val left = (width - frameSize) / 2f
        val top = (height - frameSize) / 2f
        val right = left + frameSize
        val bottom = top + frameSize
        val radius = 28f * density
        val corner = 42f * density
        val scanY = top + frameSize * 0.58f

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shadePaint)
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, clearPaint)
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, borderPaint)

        canvas.drawLine(left, top + corner, left, top + radius, cornerPaint)
        canvas.drawLine(left + radius, top, left + corner, top, cornerPaint)
        canvas.drawLine(right - corner, top, right - radius, top, cornerPaint)
        canvas.drawLine(right, top + radius, right, top + corner, cornerPaint)
        canvas.drawLine(left, bottom - corner, left, bottom - radius, cornerPaint)
        canvas.drawLine(left + radius, bottom, left + corner, bottom, cornerPaint)
        canvas.drawLine(right - corner, bottom, right - radius, bottom, cornerPaint)
        canvas.drawLine(right, bottom - corner, right, bottom - radius, cornerPaint)

        canvas.drawLine(left + 24f * density, scanY, right - 24f * density, scanY, scanPaint)
    }
}
