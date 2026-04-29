package org.ligi.passandroid

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.ligi.passandroid.ui.PassListActivity
import org.ligi.passandroid.ui.TopicNames
import org.ligi.trulesk.TruleskIntentRule

const val CUSTOM_PROBE = "FOO_PROBE"

class ThePassListLongPress {

    @get:Rule
    val rule = TruleskIntentRule(PassListActivity::class.java) {
        TestApp.populatePassStoreWithSinglePass()
    }

    @Test
    fun testLongPressCanMoveToTrash() {
        openMoveDialog()

        onView(withText(R.string.topic_trash)).perform(click())

        assertThat(TestApp.passStore.classifier.getTopics()).containsExactly(TopicNames.TRASH)
    }

    @Test
    fun testLongPressCanMoveToCustomTopic() {
        openMoveDialog()

        onView(withText(R.string.create_new_topic)).perform(click())
        onView(withId(R.id.new_topic_edit)).perform(replaceText(CUSTOM_PROBE))
        onView(withId(R.id.ok_button)).perform(click())

        assertThat(TestApp.passStore.classifier.getTopics()).containsExactly(CUSTOM_PROBE)
    }

    @Test
    fun testLongPressOpensMoveDialog() {
        openMoveDialog()

        rule.screenShot("move_to_new_topic_dialog")
        onView(withText(R.string.move_to_new_topic)).check(matches(isDisplayed()))
    }

    private fun openMoveDialog() {
        onView(withId(R.id.pass_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick())
        )
    }
}
