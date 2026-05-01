package org.ligi.passandroid.functions

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.ligi.passandroid.R

fun expand(): ViewAction = ExpandFabAction()

class ExpandFabAction : ViewAction {

    override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)

    override fun getDescription() = "expands the floating action menu"

    override fun perform(uiController: UiController?, view: View?) {
        view?.findViewById<View>(R.id.fab_menu_toggle)?.performClick()
        uiController?.loopMainThreadUntilIdle()
    }
}

class CollapsedCheck : TypeSafeMatcher<View>(View::class.java) {

    override fun describeTo(description: Description?) {
        description?.appendText("is in collapsed state")
    }

    override fun matchesSafely(view: View?): Boolean {
        return view?.findViewById<View>(R.id.fab_action_import)?.visibility == View.GONE &&
            view.findViewById<View>(R.id.fab_action_create_pass)?.visibility == View.GONE
    }
}
