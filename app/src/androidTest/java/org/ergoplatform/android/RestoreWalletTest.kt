package org.ergoplatform.android


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RestoreWalletTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun restoreWalletTest() {
        val actionMenuItemView = onView(withId(R.id.menu_add_wallet))
        actionMenuItemView.perform(click())

        val cardView = onView(withId(R.id.card_restore_wallet))
        cardView.perform(scrollTo(), click())

        val materialButton = onView(withId(R.id.button_restore))
        materialButton.perform(click())

        val textInputEditText = onView(
            allOf(
                childAtPosition(
                    childAtPosition(
                        withId(R.id.tv_mnemonic),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textInputEditText.perform(click())

        textInputEditText.perform(
            replaceText("this is a test mnemonic that is not built from "),
            pressImeActionButton()
        )
        textInputEditText.check(matches(isDisplayed()))

        textInputEditText.perform(replaceText("this is a test mnemonic that is not built from the usual wordlist for testing"))

        textInputEditText.perform(closeSoftKeyboard())

        val materialButton2 = onView(withId(R.id.button_restore))

        materialButton2.perform(click())

        val textView2 = onView(withId(R.id.public_address))

        textView2.check(matches(withText(anyOf((startsWith("3")), startsWith("9")))))
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
