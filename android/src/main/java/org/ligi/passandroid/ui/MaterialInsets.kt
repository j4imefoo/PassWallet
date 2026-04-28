package org.ligi.passandroid.ui

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import org.ligi.passandroid.R

fun AppCompatActivity.applyMaterialInsets(
    root: View,
    appBar: View? = null,
    drawerContent: View? = null,
    content: View? = null,
    floatingView: View? = null,
) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = ContextCompat.getColor(this, R.color.status_bar)
    window.navigationBarColor = ContextCompat.getColor(this, R.color.surface_container)
    WindowInsetsControllerCompat(window, root).isAppearanceLightStatusBars = false

    val appBarTop = appBar?.paddingTop ?: 0
    val appBarLeft = appBar?.paddingLeft ?: 0
    val appBarRight = appBar?.paddingRight ?: 0

    val drawerTop = drawerContent?.paddingTop ?: 0
    val drawerBottom = drawerContent?.paddingBottom ?: 0

    val contentBottom = content?.paddingBottom ?: 0
    val contentLeft = content?.paddingLeft ?: 0
    val contentRight = content?.paddingRight ?: 0

    val fabBottom = (floatingView?.layoutParams as? MarginLayoutParams)?.bottomMargin ?: 0
    val fabRight = (floatingView?.layoutParams as? MarginLayoutParams)?.rightMargin ?: 0

    ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        appBar?.updatePadding(
            left = appBarLeft + systemBars.left,
            top = appBarTop + systemBars.top,
            right = appBarRight + systemBars.right,
        )

        drawerContent?.updatePadding(
            top = drawerTop + systemBars.top,
            bottom = drawerBottom + systemBars.bottom,
        )

        content?.updatePadding(
            left = contentLeft + systemBars.left,
            right = contentRight + systemBars.right,
            bottom = contentBottom + systemBars.bottom,
        )

        floatingView?.updateLayoutParams<MarginLayoutParams> {
            bottomMargin = fabBottom + systemBars.bottom
            rightMargin = fabRight + systemBars.right
        }

        insets
    }

    ViewCompat.requestApplyInsets(root)
}
