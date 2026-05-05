package org.ligi.passandroid.functions

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import timber.log.Timber

private const val MAX_WIDTH_1D = 1500
private const val MAX_WIDTH_2D = 500
private const val DEFAULT_1D_HEIGHT = 300

fun generateBitmapDrawable(
    resources: Resources,
    data: String,
    type: PassBarCodeFormat,
    widthPx: Int? = null,
    heightPx: Int? = null
): BitmapDrawable? {
    val bitmap = generateBarCodeBitmap(data, type, widthPx, heightPx) ?: return null

    return bitmap.toDrawable(resources).apply {
        isFilterBitmap = false
        setAntiAlias(false)
    }
}

fun generateBarCodeBitmap(data: String, type: PassBarCodeFormat, widthPx: Int? = null, heightPx: Int? = null): Bitmap? {

    if (data.isEmpty()) {
        return null
    }

    try {
        val matrix = getBitMatrix(data, type, widthPx, heightPx)

        val width = matrix.width
        val height = matrix.height
        val barcodeImage = createBitmap(width, height)
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        barcodeImage.setPixels(pixels, 0, width, 0, 0, width, height)

        return barcodeImage
    } catch (e: com.google.zxing.WriterException) {
        Timber.w(e, "could not write image")
        return null
    } catch (e: IllegalArgumentException) {
        Timber.w("could not write image: $e")
        return null
    } catch (e: ArrayIndexOutOfBoundsException) {
        // happens for ITF barcode on certain inputs
        Timber.w("could not write image: $e")
        return null
    }

}

fun getBitMatrix(data: String, type: PassBarCodeFormat, widthPx: Int? = null, heightPx: Int? = null): BitMatrix {
    val (defaultWidth, defaultHeight) = when (type) {
        PassBarCodeFormat.AZTEC,
        PassBarCodeFormat.PDF_417,
        PassBarCodeFormat.QR_CODE -> MAX_WIDTH_2D to MAX_WIDTH_2D
        PassBarCodeFormat.DATA_MATRIX -> MAX_WIDTH_2D to MAX_WIDTH_2D
        else -> MAX_WIDTH_1D to DEFAULT_1D_HEIGHT
    }
    val width = widthPx?.coerceAtLeast(1) ?: defaultWidth
    val height = heightPx?.coerceAtLeast(1) ?: defaultHeight

    return MultiFormatWriter().encode(data, type.zxingBarCodeFormat(), width, height, emptyMap<EncodeHintType, Any>())!!
}
