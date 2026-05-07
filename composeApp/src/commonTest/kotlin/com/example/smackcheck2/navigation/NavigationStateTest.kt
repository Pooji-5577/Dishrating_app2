package com.example.smackcheck2.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NavigationStateTest {

    @Test
    fun navigate_back_returns_to_previous_screen() {
        val navigationState = NavigationState()

        navigationState.navigateTo(Screen.Login)
        navigationState.navigateTo(Screen.Register)

        assertEquals(true, navigationState.navigateBack())
        assertEquals(Screen.Login, navigationState.currentScreen)
    }

    @Test
    fun replace_with_clears_back_stack() {
        val navigationState = NavigationState()

        navigationState.navigateTo(Screen.Login)
        navigationState.navigateTo(Screen.Register)
        navigationState.replaceWith(Screen.DarkHome)

        assertEquals(Screen.DarkHome, navigationState.currentScreen)
        assertFalse(navigationState.canGoBack)
    }
}
