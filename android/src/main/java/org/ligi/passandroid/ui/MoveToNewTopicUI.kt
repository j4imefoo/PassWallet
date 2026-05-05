package org.ligi.passandroid.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.setPadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import org.ligi.passandroid.R
import org.ligi.passandroid.functions.moveWithUndoSnackbar
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.Pass

internal class MoveToNewTopicUI(private val context: Activity, private val passStore: PassStore, private val pass: Pass) {

    fun show() {
        val dialog = BottomSheetDialog(context, R.style.PassWallet_TransparentBottomSheetDialog)
        val view = LayoutInflater.from(context).inflate(
            R.layout.dialog_move_to_new_topic,
            FrameLayout(context),
            false,
        )
        dialog.setContentView(view)
        dialog.setOnCancelListener { passStore.notifyChange() }
        dialog.show()
        makeBottomSheetChromeTransparent(dialog)

        val move: (topic: String) -> Unit = { topic ->
            moveWithUndoSnackbar(passStore.classifier, pass, topic, context)
            dialog.dismiss()
        }

        val newTopicEditText = view.findViewById<EditText>(R.id.new_topic_edit)
        tintTextCursor(newTopicEditText)
        val subtitle = view.findViewById<TextView>(R.id.move_topic_subtitle)
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

        val acceptNewTopic = {
            val topic = newTopicEditText.text.toString().trim()
            if (topic.isEmpty()) {
                newTopicEditText.error = context.getString(R.string.cannot_be_empty)
                newTopicEditText.requestFocus()
            } else {
                move(topic)
            }
        }

        createNewTopicButton.setOnClickListener {
            subtitle.setText(R.string.move_topic_new_subtitle)
            suggestionContainer.visibility = View.GONE
            createNewTopicButton.visibility = View.GONE
            inputContainer.visibility = View.VISIBLE
            inputActions.visibility = View.VISIBLE
            newTopicEditText.requestFocus()
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(newTopicEditText, InputMethodManager.SHOW_IMPLICIT)
        }

        newTopicEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                acceptNewTopic()
                true
            } else {
                false
            }
        }

        cancelButton.setOnClickListener {
            passStore.notifyChange()
            dialog.dismiss()
        }

        okButton.setOnClickListener {
            acceptNewTopic()
        }
    }

    private fun createTopicRow(topic: String, onClick: () -> Unit): View {
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
            radius = dp(18).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = ContextCompat.getColor(context, R.color.edit_action_stroke)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.edit_action_background))
            foreground = ContextCompat.getDrawable(context, selectableItemBackground())
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = context.resources.getDimensionPixelSize(R.dimen.move_topic_row_height)
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }

        val icon = ImageView(context).apply {
            setImageResource(if (topic == TopicNames.TRASH) R.drawable.ic_action_delete else R.drawable.ic_category_24)
            setColorFilter(
                ContextCompat.getColor(
                    context,
                    if (topic == TopicNames.TRASH) R.color.danger else R.color.accent
                )
            )
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                marginEnd = dp(14)
            }
        }

        val title = TextView(context).apply {
            text = TopicNames.displayName(context, topic)
            textSize = 16f
            setTypeface(typeface, Typeface.NORMAL)
            setTextColor(ContextCompat.getColor(context, R.color.edit_action_text))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        row.addView(icon)
        row.addView(title)
        card.addView(row)
        return card
    }

    private fun selectableItemBackground(): Int {
        val outValue = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return outValue.resourceId
    }

    private fun tintTextCursor(editText: EditText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            editText.textCursorDrawable = GradientDrawable().apply {
                setColor(ContextCompat.getColor(context, R.color.accent))
                setSize(dp(2), dp(24))
            }
        }
    }

    private fun makeBottomSheetChromeTransparent(dialog: BottomSheetDialog) {
        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0f)
        }
        var current = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        repeat(5) {
            current?.background = Color.TRANSPARENT.toDrawable()
            current = current?.parent as? View
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
