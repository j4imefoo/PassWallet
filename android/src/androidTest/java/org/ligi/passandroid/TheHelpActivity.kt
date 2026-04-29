package org.ligi.passandroid

import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.ui.PreferenceActivity
import org.ligi.trulesk.TruleskActivityRule

class TheHelpActivity {

    @get:Rule
    val rule = TruleskActivityRule(PreferenceActivity::class.java)

    @Test
    fun testSettingsActivityStarts() {
        rule.screenShot("settings")
    }
}
