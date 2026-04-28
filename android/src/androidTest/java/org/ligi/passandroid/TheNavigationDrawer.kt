package org.ligi.passandroid

import android.annotation.TargetApi
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions.open
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.R.string.*
import org.ligi.passandroid.ui.PassListActivity
import org.ligi.passandroid.ui.PreferenceActivity
import org.ligi.trulesk.TruleskIntentRule

@TargetApi(14)
class TheNavigationDrawer {

    @get:Rule
    var rule = TruleskIntentRule(PassListActivity::class.java)

    @Test
    fun testNavigationDrawerIsUsuallyNotShown() {
        onView(withId(R.id.navigationView)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testThatNavigationDrawerOpens() {
        onView(withId(R.id.drawer_layout)).perform(open())
        onView(withId(R.id.navigationView)).check(matches(isDisplayed()))
    }

    @Test
    fun testThatNavigationDrawerClosesOnBackPress() {
        testThatNavigationDrawerOpens()

        pressBack()

        onView(withId(R.id.navigationView)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testSettings() {
        testThatNavigationDrawerOpens()
        rule.screenShot("open_drawer")

        onView(withText(nav_settings)).perform(click())

        intended(hasComponent(PreferenceActivity::class.java.name))
    }
}
