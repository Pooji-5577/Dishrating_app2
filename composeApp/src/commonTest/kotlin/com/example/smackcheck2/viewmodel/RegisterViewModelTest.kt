package com.example.smackcheck2.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegisterViewModelTest {

    @Test
    fun register_validates_before_starting_auth() {
        val viewModel = RegisterViewModel()
        var authStarted = false

        viewModel.register { _, _, _ -> authStarted = true }

        assertFalse(authStarted)
        assertEquals("Name is required", viewModel.uiState.value.nameError)
        assertEquals("Email is required", viewModel.uiState.value.emailError)
        assertEquals("Password is required", viewModel.uiState.value.passwordError)
        assertEquals("Please confirm your password", viewModel.uiState.value.confirmPasswordError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun register_starts_auth_with_valid_registration() {
        val viewModel = RegisterViewModel()
        viewModel.onNameChange("Simha Teja")
        viewModel.onEmailChange("simhateja@example.com")
        viewModel.onPasswordChange("Secret1")
        viewModel.onConfirmPasswordChange("Secret1")

        var registration: Triple<String, String, String>? = null
        viewModel.register { name, email, password -> registration = Triple(name, email, password) }

        assertEquals(Triple("Simha Teja", "simhateja@example.com", "Secret1"), registration)
        assertTrue(viewModel.uiState.value.isLoading)
    }
}
