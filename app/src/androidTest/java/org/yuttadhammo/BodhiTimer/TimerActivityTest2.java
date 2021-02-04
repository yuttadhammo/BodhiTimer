package org.yuttadhammo.BodhiTimer;


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import tools.fastlane.screengrab.Screengrab;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class TimerActivityTest2 {

    @Rule
    public ActivityTestRule<TimerActivity> mActivityTestRule = new ActivityTestRule<>(TimerActivity.class);

    @Test
    public void timerActivityTest2() {
        Screengrab.screenshot("main");
        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.setButton), withContentDescription("Set"),
                        childAtPosition(
                                allOf(withId(R.id.mainLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                4),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        ViewInteraction button = onView(
                allOf(withId(R.id.btnOk), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_cont),
                                        childAtPosition(
                                                withId(R.id.container),
                                                6)),
                                2)));
        button.perform(scrollTo(), click());

        ViewInteraction textView = onView(
                allOf(withId(R.id.text_top),
                        withParent(allOf(withId(R.id.mainLayout),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        textView.check(matches(withText("0")));
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
