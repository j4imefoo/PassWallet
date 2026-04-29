package org.ligi.passandroid.ui

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.ligi.passandroid.R
import org.ligi.passandroid.functions.moveWithUndoSnackbar
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass

internal class MoveToNewTopicUI(private val context: Activity, private val passStore: PassStore, private val pass: Pass) {

    fun show() {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_move_to_new_topic, null)
        dialog.setContentView(view)
        dialog.setOnCancelListener { passStore.notifyChange() }
        dialog.show()
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.background =
            ContextCompat.getDrawable(context, R.drawable.bottom_sheet_background)

        val move: (topic: String) -> Unit = { topic ->
            moveWithUndoSnackbar(passStore.classifier, pass, topic, context)
            dialog.dismiss()
        }

        val newTopicEditText = view.findViewById<EditText>(R.id.new_topic_edit)
        val inputContainer = view.findViewById<View>(R.id.new_topic_input_container)
        val inputActions = view.findViewById<View>(R.id.new_topic_actions)
        val suggestionContainer = view.findViewById<LinearLayout>(R.id.topic_suggestions_button_container)
        val createNewTopicButton = view.findViewById<View>(R.id.create_new_topic_button)
        val cancelButton = view.findViewById<View>(R.id.cancel_button)
        val okButton = view.findViewById<View>(R.id.ok_button)

        val oldTopic = passStore.classifier.getTopic(pass, "")
        val suggestedTopics = TopicNames.sortedTopics(
            (passStore.classifier.getTopics() + TopicNames.builtInTopics)
                .filterNot { it == oldTopic }
                .toSet()
        )

        suggestedTopics.forEach { topic ->
            suggestionContainer.addView(createTopicRow(topic) { move(topic) })
        }

        createNewTopicButton.setOnClickListener {
            inputContainer.visibility = View.VISIBLE
            inputActions.visibility = View.VISIBLE
            newTopicEditText.requestFocus()
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(newTopicEditText, InputMethodManager.SHOW_IMPLICIT)
        }

        cancelButton.setOnClickListener {
            passStore.notifyChange()
            dialog.dismiss()
        }

        okButton.setOnClickListener {
            val topic = newTopicEditText.text.toString().trim()
            if (topic.isEmpty()) {
                newTopicEditText.error = context.getString(R.string.cannot_be_empty)
                newTopicEditText.requestFocus()
            } else {
                move(topic)
            }
        }
    }

    private fun createTopicRow(topic: String, onClick: () -> Unit): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            minimumHeight = context.resources.getDimensionPixelSize(R.dimen.move_topic_row_height)
            setPadding(0, 0, 0, 0)
            background = ContextCompat.getDrawable(context, selectableItemBackground())
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        val title = TextView(context).apply {
            text = TopicNames.displayName(context, topic)
            textSize = 18f
            setTypeface(typeface, Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        row.addView(title)
        return row
    }

    private fun selectableItemBackground(): Int {
        val outValue = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return outValue.resourceId
    }
}
