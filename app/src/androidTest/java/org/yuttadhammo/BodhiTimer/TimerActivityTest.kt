package org.yuttadhammo.BodhiTimer


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab

@LargeTest
@RunWith(AndroidJUnit4::class)
class TimerActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(TimerActivity::class.java)

    @Test
    fun timerActivityTest() {
        Screengrab.screenshot("main")
        val appCompatImageButton = onView(
            allOf(
                withId(R.id.setButton), withContentDescription("Set"),
                childAtPosition(
                    allOf(
                        withId(R.id.mainLayout),
                        childAtPosition(
                            withId(android.R.id.content),
                            0
                        )
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        appCompatImageButton.perform(click())

        //val gallery = onView()

        val button = onView(
            allOf(
                withId(R.id.btnOk), withText("OK"),
                childAtPosition(
                    allOf(
                        withId(R.id.button_cont),
                        childAtPosition(
                            withId(R.id.container),
                            6
                        )
                    ),
                    2
                )
            )
        )
        button.perform(scrollTo(), click())

        val textView = onView(
            allOf(
                withId(R.id.text_top),
                withParent(
                    allOf(
                        withId(R.id.mainLayout),
                        withParent(withId(android.R.id.content))
                    )
                ),
                isDisplayed()
            )
        )
        textView.check(matches(withText("0")))

    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
