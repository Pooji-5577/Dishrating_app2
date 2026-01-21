package com.example.smackcheck2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smackcheck2.ui.theme.appColors
import com.example.smackcheck2.model.LoginUiState
import com.example.smackcheck2.viewmodel.LoginViewModel

@Composable
fun DarkLoginScreen(
    viewModel: LoginViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Handle successful login
    if (uiState.isSuccess) {
        onNavigateToHome()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors().Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Logo
            Box(
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = appColors().Primary.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                )
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = "Logo",
                    modifier = Modifier.size(60.dp),
                    tint = appColors().Primary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Welcome Back",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = appColors().TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Sign in to continue your food journey",
                fontSize = 14.sp,
                color = appColors().TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Email field
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("Email") },
                placeholder = { Text("Enter your email") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = appColors().TextSecondary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = appColors().Primary,
                    unfocusedBorderColor = appColors().Surface,
                    focusedLabelColor = appColors().Primary,
                    unfocusedLabelColor = appColors().TextSecondary,
                    cursorColor = appColors().Primary,
                    focusedTextColor = appColors().TextPrimary,
                    unfocusedTextColor = appColors().TextPrimary,
                    focusedContainerColor = appColors().CardBackground,
                    unfocusedContainerColor = appColors().CardBackground,
                    focusedPlaceholderColor = appColors().TextTertiary,
                    unfocusedPlaceholderColor = appColors().TextTertiary
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Password field
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = appColors().TextSecondary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = appColors().TextSecondary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = appColors().Primary,
                    unfocusedBorderColor = appColors().Surface,
                    focusedLabelColor = appColors().Primary,
                    unfocusedLabelColor = appColors().TextSecondary,
                    cursorColor = appColors().Primary,
                    focusedTextColor = appColors().TextPrimary,
                    unfocusedTextColor = appColors().TextPrimary,
                    focusedContainerColor = appColors().CardBackground,
                    unfocusedContainerColor = appColors().CardBackground,
                    focusedPlaceholderColor = appColors().TextTertiary,
                    unfocusedPlaceholderColor = appColors().TextTertiary
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Forgot Password
            Text(
                text = "Forgot Password?",
                color = appColors().Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { /* Handle forgot password */ }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Error message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = appColors().Error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Login button
            Button(
                onClick = { viewModel.login(onNavigateToHome) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = appColors().Primary,
                    disabledContainerColor = appColors().Primary.copy(alpha = 0.5f)
                ),
                enabled = !uiState.isLoading && uiState.email.isNotBlank() && uiState.password.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Divider with text
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(appColors().Surface)
                )
                Text(
                    text = "  or continue with  ",
                    color = appColors().TextSecondary,
                    fontSize = 12.sp
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(appColors().Surface)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Social login buttons (placeholder)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SocialLoginButton("G") { onNavigateToHome() }
                Spacer(modifier = Modifier.size(16.dp))
                SocialLoginButton("f") { onNavigateToHome() }
                Spacer(modifier = Modifier.size(16.dp))
                SocialLoginButton("A") { onNavigateToHome() }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Skip login button for testing
            Text(
                text = "Skip Login (Demo Mode)",
                color = appColors().Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onNavigateToHome() }
                    .padding(8.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Register link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = appColors().TextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Sign Up",
                    color = appColors().Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SocialLoginButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(
                color = appColors().CardBackground,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = appColors().TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
