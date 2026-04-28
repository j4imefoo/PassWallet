package org.ligi.passandroid.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.widget.TextView
import com.google.android.material.navigation.NavigationView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassStore

class PassNavigationView(context: Context, attrs: AttributeSet) : NavigationView(context, attrs), KoinComponent {

    val passStore: PassStore by inject()

    private fun getIntent(id: Int) = when (id) {
        R.id.menu_settings -> Intent(context, PreferenceActivity::class.java)
        else -> null
    }

    @SuppressLint("RestrictedApi") // FIXME: temporary workaround for false-positive
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        setNavigationItemSelectedListener { item ->
            getIntent(item.itemId)?.let {
                context.startActivity(it)
                true
            } ?: false
        }

        passStoreUpdate()
    }

    fun passStoreUpdate() {

        val passCount = passStore.passMap.size
        val passCountHeader = getHeaderView(0).findViewById<TextView>(R.id.pass_count_header)
        passCountHeader.text = context.getString(R.string.passes_nav, passCount)

        val topicCount = passStore.classifier.getTopics().size
        val topicCountHeader = getHeaderView(0).findViewById<TextView>(R.id.topic_count_header)
        topicCountHeader.text = context.getString(R.string.categories_nav, topicCount)

    }
}
