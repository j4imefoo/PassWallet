package org.ligi.passandroid.ui.edit.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.ligi.passandroid.R
import org.ligi.passandroid.databinding.CategoryPickSheetBinding
import org.ligi.passandroid.databinding.ItemCategoryPickBinding
import org.ligi.passandroid.functions.getHumanCategoryString
import org.ligi.passandroid.model.PassBitmapDefinitions
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassType

private val passTypes = arrayOf(PassType.BOARDING, PassType.EVENT, PassType.GENERIC, PassType.LOYALTY, PassType.VOUCHER, PassType.COUPON)

fun showCategoryPickDialog(context: Context, pass: Pass, passStore: PassStore, refreshCallback: () -> Unit) {
    val dialog = BottomSheetDialog(context)
    val binding = CategoryPickSheetBinding.inflate(LayoutInflater.from(context))
    dialog.setContentView(binding.root)

    val iconBitmap = pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_ICON)
        ?: pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_LOGO)

    passTypes.forEach { type ->
        val row = ItemCategoryPickBinding.inflate(LayoutInflater.from(context), binding.categoryOptions, false)
        row.categoryView.setAccentColor(pass.accentColor)
        row.categoryView.setImageByCategory(type)
        iconBitmap?.let { row.categoryView.setIcon(it) }
        row.navCategoryLabel.setText(getHumanCategoryString(type))
        row.categorySelectedCheck.visibility = if (pass.type == type) View.VISIBLE else View.GONE
        row.root.setOnClickListener {
            pass.type = type
            refreshCallback.invoke()
            dialog.dismiss()
        }
        binding.categoryOptions.addView(
            row.root,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
    }

    dialog.show()
}
