package org.ligi.passandroid.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.ligi.passandroid.R
import java.util.EnumMap
import java.util.concurrent.Executors

class ScanBarcodeActivity : AppCompatActivity() {

    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
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

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(24.dp, 0, 24.dp, 40.dp)
        }

        val flashButton = scannerButton(getString(R.string.scan_flash)) {
            toggleTorch()
        }
        val switchButton = scannerButton(getString(R.string.scan_switch_camera)) {
            switchCamera()
        }
        val cancelButton = scannerButton(getString(android.R.string.cancel)) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        buttons.addView(flashButton)
        buttons.addView(switchButton)
        buttons.addView(cancelButton)

        root.addView(
            buttons,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            },
        )

        return root
    }

    private fun scannerButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.BLACK)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 6.dp
                marginEnd = 6.dp
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
        } catch (_: IllegalArgumentException) {
            lensFacing = CameraSelector.LENS_FACING_BACK
            torchEnabled = false
            bindCamera(provider, previewTarget)
        }
    }

    private fun decode(image: ImageProxy): com.google.zxing.Result? {
        val source = image.toLuminanceSource()
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val invertedBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
        return listOf(bitmap, invertedBitmap).firstNotNullOfOrNull { candidate ->
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
    }

    private fun switchCamera() {
        val provider = cameraProvider ?: return
        val nextLens = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        val selector = CameraSelector.Builder().requireLensFacing(nextLens).build()
        if (!provider.hasCamera(selector)) return
        lensFacing = nextLens
        torchEnabled = false
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
    private val shadePaint = Paint().apply { color = 0x99000000.toInt() }
    private val clearPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
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
        val radius = 24f * resources.displayMetrics.density

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shadePaint)
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, clearPaint)
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, strokePaint)
    }
}
