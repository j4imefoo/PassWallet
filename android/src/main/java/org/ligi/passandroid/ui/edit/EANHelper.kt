package org.ligi.passandroid.ui.edit

import com.google.zxing.oned.EAN13Writer
import com.google.zxing.oned.EAN8Writer
import com.google.zxing.oned.UPCEANWriter

fun isValidEAN13(payload: String) = isValidEAN(payload, EAN13Writer())

fun isValidEAN8(payload: String) = isValidEAN(payload, EAN8Writer())

fun isValidEAN(payload: String, writer: UPCEANWriter) = try {
    writer.encode(payload)
    true
} catch (e: IllegalArgumentException) {
    false
}
