package com.example.smackcheck2.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginViewModelTest {

    @Test
    fun login_validates_before_starting_auth() {
        val viewModel = LoginViewModel()
        var authStarted = false

        viewModel.login { _, _ -> authStarted = true }

        assertFalse(authStarted)
        assertEquals("Email is required", viewModel.uiState.value.emailError)
        assertEquals("Password is required", viewModel.uiState.value.passwordError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun login_starts_auth_with_valid_credentials() {
        val viewModel = LoginViewModel()
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("Secret1")

        var credentials: Pair<String, String>? = null
        viewModel.login { email, password -> credentials = email to password }

        assertEquals("user@example.com" to "Secret1", credentials)
        assertTrue(viewModel.uiState.value.isLoading)
    }
}
