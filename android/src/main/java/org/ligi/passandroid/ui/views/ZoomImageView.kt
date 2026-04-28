package org.ligi.passandroid.ui.views

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.min

class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatImageView(context, attrs) {

    private val imageMatrixValues = Matrix()
    private val lastTouch = PointF()
    private var scaleFactor = 1f
    private var fitScale = 1f
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        post(::resetImage)
    }

    override fun setImageBitmap(bm: android.graphics.Bitmap?) {
        super.setImageBitmap(bm)
        post(::resetImage)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouch.set(event.x, event.y)
                isDragging = true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && scaleFactor > 1f) {
                    val dx = event.x - lastTouch.x
                    val dy = event.y - lastTouch.y
                    imageMatrixValues.postTranslate(dx, dy)
                    fixTranslation()
                    imageMatrix = imageMatrixValues
                    lastTouch.set(event.x, event.y)
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.actionMasked == MotionEvent.ACTION_UP && scaleFactor == 1f) {
                    performClick()
                }
                isDragging = false
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun resetImage() {
        val drawable = drawable ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth == 0f || viewHeight == 0f) {
            return
        }

        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()
        if (drawableWidth == 0f || drawableHeight == 0f) {
            return
        }

        fitScale = min(viewWidth / drawableWidth, viewHeight / drawableHeight)
        scaleFactor = 1f
        imageMatrixValues.reset()
        imageMatrixValues.postScale(fitScale, fitScale)
        val dx = (viewWidth - drawableWidth * fitScale) / 2f
        val dy = (viewHeight - drawableHeight * fitScale) / 2f
        imageMatrixValues.postTranslate(dx, dy)
        imageMatrix = imageMatrixValues
    }

    private fun zoomTo(targetScale: Float, focusX: Float, focusY: Float) {
        val clampedTargetScale = targetScale.coerceIn(1f, 4f)
        val delta = clampedTargetScale / scaleFactor
        scaleFactor = clampedTargetScale
        imageMatrixValues.postScale(delta, delta, focusX, focusY)
        fixTranslation()
        imageMatrix = imageMatrixValues
    }

    private fun fixTranslation() {
        val drawable = drawable ?: return
        val values = FloatArray(9)
        imageMatrixValues.getValues(values)

        val scaledWidth = drawable.intrinsicWidth * fitScale * scaleFactor
        val scaledHeight = drawable.intrinsicHeight * fitScale * scaleFactor
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val minX = min(0f, viewWidth - scaledWidth)
        val minY = min(0f, viewHeight - scaledHeight)
        val maxX = max(0f, viewWidth - scaledWidth)
        val maxY = max(0f, viewHeight - scaledHeight)

        val currentX = values[Matrix.MTRANS_X]
        val currentY = values[Matrix.MTRANS_Y]

        val targetX = if (scaledWidth <= viewWidth) (viewWidth - scaledWidth) / 2f else currentX.coerceIn(minX, 0f)
        val targetY = if (scaledHeight <= viewHeight) (viewHeight - scaledHeight) / 2f else currentY.coerceIn(minY, 0f)

        imageMatrixValues.postTranslate(targetX - currentX, targetY - currentY)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoomTo(scaleFactor * detector.scaleFactor, detector.focusX, detector.focusY)
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val targetScale = if (scaleFactor > 1f) 1f else 2f
            zoomTo(targetScale, e.x, e.y)
            return true
        }
    }
}
