package org.ligi.passandroid

import android.annotation.TargetApi
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.ui.PassListActivity
import org.ligi.passandroid.ui.PreferenceActivity
import org.ligi.trulesk.TruleskIntentRule

@TargetApi(14)
class TheNavigationDrawer {

    @get:Rule
    var rule = TruleskIntentRule(PassListActivity::class.java)

    @Test
    fun testSettingsOpensFromToolbar() {
        onView(withId(R.id.menu_settings)).perform(click())

        intended(hasComponent(PreferenceActivity::class.java.name))
    }
}
