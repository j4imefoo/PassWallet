package org.ligi.passandroid

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.ui.PassListActivity
import org.ligi.trulesk.TruleskActivityRule

class ThePassViewHolder {

    @get:Rule
    var rule = TruleskActivityRule(PassListActivity::class.java, false)

    @Test
    fun passCardStillRendersWithoutExtraActionButtons() {
        TestApp.populatePassStoreWithSinglePass()

        rule.launchActivity()

        onView(withId(R.id.pass_card)).check(matches(isDisplayed()))
        onView(withId(R.id.passTitle)).check(matches(isDisplayed()))
    }
}
