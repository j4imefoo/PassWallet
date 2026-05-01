package org.ligi.passandroid.ui.edit.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.ligi.passandroid.databinding.ColorPickSheetBinding
import org.ligi.passandroid.model.pass.Pass

private val colorPresets = intArrayOf(
    0xFF3D73E9.toInt(),
    0xFF9F3DD0.toInt(),
    0xFFEA3C48.toInt(),
    0xFFF29B21.toInt(),
    0xFF9CCB05.toInt(),
    0xFF00897B.toInt(),
    0xFF2A2727.toInt(),
    0xFFFFFFFF.toInt(),
)

fun showColorPickDialog(context: Context, pass: Pass, refreshCallback: () -> Unit) {
    val dialog = BottomSheetDialog(context)
    val binding = ColorPickSheetBinding.inflate(LayoutInflater.from(context))
    dialog.setContentView(binding.root)

    binding.colorPicker.color = pass.accentColor
    binding.colorPicker.oldCenterColor = pass.accentColor

    colorPresets.forEach { color ->
        binding.colorPresetContainer.addView(createColorSwatch(context, color) {
            binding.colorPicker.color = color
        })
    }

    binding.cancelButton.setOnClickListener { dialog.dismiss() }
    binding.applyButton.setOnClickListener {
        pass.accentColor = binding.colorPicker.color
        refreshCallback.invoke()
        dialog.dismiss()
    }

    dialog.show()
}

private fun createColorSwatch(context: Context, color: Int, onClick: () -> Unit): View {
    val size = 44.dp(context)
    return View(context).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(2.dp(context), if (isVeryLight(color)) 0xFFCBD5E1.toInt() else Color.WHITE)
        }
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(size, size).apply {
            marginEnd = 10.dp(context)
        }
    }
}

private fun isVeryLight(color: Int): Boolean {
    val red = Color.red(color)
    val green = Color.green(color)
    val blue = Color.blue(color)
    return (red + green + blue) / 3 > 220
}

private fun Int.dp(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
