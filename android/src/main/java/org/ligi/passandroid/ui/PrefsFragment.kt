package org.ligi.passandroid.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.TextViewCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.ligi.passandroid.R
import org.ligi.passandroid.databinding.ItemSortOrderBinding
import org.ligi.passandroid.databinding.SortOrderSheetBinding

class PrefsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        findPreference<Preference>(getString(R.string.preference_key_backup))?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), BackupActivity::class.java))
            true
        }
        findPreference<ListPreference>(getString(R.string.preference_key_sort))?.let { sortPreference ->
            updateSortSummary(sortPreference)
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference.key == getString(R.string.preference_key_sort) && preference is ListPreference) {
            showSortOrderSheet(preference)
            return
        }
        super.onDisplayPreferenceDialog(preference)
    }

    private fun showSortOrderSheet(sortPreference: ListPreference) {
        val context = requireContext()
        val dialog = BottomSheetDialog(context)
        val binding = SortOrderSheetBinding.inflate(LayoutInflater.from(context))
        val entries = resources.getStringArray(R.array.sort_orders)
        val values = resources.getStringArray(R.array.sort_order_keys)
        val selectedValue = sortPreference.value ?: values.firstOrNull().orEmpty()

        entries.zip(values).forEach { (entry, value) ->
            val row = ItemSortOrderBinding.inflate(LayoutInflater.from(context), binding.sortOrderOptions, false)
            val isSelected = value == selectedValue
            row.sortOrderLabel.text = entry
            row.sortOrderLabel.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                if (isSelected) R.drawable.ic_check_24 else 0,
                0,
            )
            TextViewCompat.setCompoundDrawableTintList(
                row.sortOrderLabel,
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.edit_action_text)),
            )
            row.sortOrderLabel.contentDescription = if (isSelected) {
                "$entry, ${getString(R.string.selected)}"
            } else {
                entry
            }
            row.root.setOnClickListener {
                sortPreference.value = value
                updateSortSummary(sortPreference)
                dialog.dismiss()
            }
            binding.sortOrderOptions.addView(
                row.root,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }

        dialog.setContentView(binding.root)
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundColor(Color.TRANSPARENT)
        }
        dialog.show()
    }

    private fun updateSortSummary(sortPreference: ListPreference) {
        sortPreference.summary = sortPreference.entry ?: getString(R.string.preference_sort_summary)
    }
}
