package org.ligi.passandroid.functions

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.VisibleForTesting
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.ligi.passandroid.model.pass.PassBarCodeFormat
import timber.log.Timber

private const val MAX_WIDTH_1D = 1500
private const val MAX_WIDTH_2D = 500
private const val DEFAULT_1D_HEIGHT = 300

fun generateBitmapDrawable(resources: Resources, data: String, type: PassBarCodeFormat): BitmapDrawable? {
    val bitmap = generateBarCodeBitmap(data, type) ?: return null

    return BitmapDrawable(resources, bitmap).apply {
        isFilterBitmap = false
        setAntiAlias(false)
    }
}

@VisibleForTesting
fun generateBarCodeBitmap(data: String, type: PassBarCodeFormat): Bitmap? {

    if (data.isEmpty()) {
        return null
    }

    try {
        val matrix = getBitMatrix(data, type)

        val width = matrix.width
        val height = matrix.height
        val barcodeImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                barcodeImage.setPixel(x, y, if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }

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

@VisibleForTesting
fun getBitMatrix(data: String, type: PassBarCodeFormat): BitMatrix {
    val (width, height) = when (type) {
        PassBarCodeFormat.AZTEC,
        PassBarCodeFormat.PDF_417,
        PassBarCodeFormat.QR_CODE -> MAX_WIDTH_2D to MAX_WIDTH_2D
        PassBarCodeFormat.DATA_MATRIX -> MAX_WIDTH_1D to DEFAULT_1D_HEIGHT
        else -> MAX_WIDTH_1D to DEFAULT_1D_HEIGHT
    }

    return MultiFormatWriter().encode(data, type.zxingBarCodeFormat(), width, height, emptyMap<EncodeHintType, Any>())!!
}
