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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AdvPicker {

    @Rule
    public ActivityTestRule<TimerActivity> mActivityTestRule = new ActivityTestRule<>(TimerActivity.class);

    @Test
    public void advPicker() {
        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.setButton)));
        appCompatImageButton.perform(click());

        ViewInteraction button = onView(
                allOf(withId(R.id.btnadv), withText("adv")));
        button.perform(scrollTo(), click());

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.mins),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.controls),
                                        1),
                                1),
                        isDisplayed()));
        appCompatEditText.perform(replaceText("2"), closeSoftKeyboard());

        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.secs),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.controls),
                                        1),
                                2),
                        isDisplayed()));
        appCompatEditText2.perform(replaceText("01"), closeSoftKeyboard());

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.add), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.controls),
                                        1),
                                3),
                        isDisplayed()));
        appCompatButton.perform(click());

        ViewInteraction textView = onView(
                allOf(withId(R.id.time),
                        withParent(withParent(withId(R.id.timesList))),
                        isDisplayed()));
        textView.check(matches(withText("2 minutes, 1 second")));

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.sound), withText("System Default"),
                        withParent(withParent(withId(R.id.timesList))),
                        isDisplayed()));
        textView2.check(matches(withText("System Default")));

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.save), withText("Save"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.controls),
                                        3),
                                2),
                        isDisplayed()));
        appCompatButton2.perform(click());

        ViewInteraction button2 = onView(
                allOf(withId(R.id.btnadv), withText("adv"),
                        childAtPosition(
                                allOf(withId(R.id.pre_button_cont),
                                        childAtPosition(
                                                withId(R.id.container),
                                                4)),
                                8)));
        button2.perform(scrollTo(), longClick());

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.time),
                        withParent(withParent(withId(R.id.timesList))),
                        isDisplayed()));
        textView3.check(matches(withText("2 minutes, 1 second")));

        ViewInteraction appCompatButton3 = onView(
                allOf(withId(R.id.save), withText("Save"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.controls),
                                        3),
                                2),
                        isDisplayed()));
        appCompatButton3.perform(click());

        ViewInteraction button3 = onView(
                allOf(withId(R.id.btnadv), withText("adv"),
                        childAtPosition(
                                allOf(withId(R.id.pre_button_cont),
                                        childAtPosition(
                                                withId(R.id.container),
                                                4)),
                                8)));
        button3.perform(scrollTo(), click());

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.text_top),
                        withParent(allOf(withId(R.id.mainLayout),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        textView4.check(matches(withText("2:01")));
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
