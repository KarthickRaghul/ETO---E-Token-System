package com.eto.manager.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eto.manager.presentation.EtoViewModel
import com.eto.manager.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRegisterScreen(
    viewModel: EtoViewModel,
    isDark: Boolean
) {
    var isLoginMode by remember { mutableStateOf(true) }
    
    // Form inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("PATIENT") }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    
    var passwordVisible by remember { mutableStateOf(false) }

    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val authSuccessMessage by viewModel.authSuccessMessage.collectAsState()

    val backgroundBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(DarkBgStart, DarkBgEnd)
        } else {
            listOf(LightBgStart, LightBgEnd)
        }
    )

    // Clear messages when toggling mode
    LaunchedEffect(isLoginMode) {
        viewModel.clearAuthError()
        viewModel.clearAuthSuccessMessage()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ETO App Logo & Title
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "ETO Token System",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) DarkTextPrimary else LightTextPrimary
            )
            Text(
                text = "Healthcare Management, Simplified",
                fontSize = 14.sp,
                color = if (isDark) DarkTextTertiary else LightTextTertiary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Card (Glassmorphic)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphicCard(isDark, cornerRadius = 24.dp)
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Mode Selector Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isDark) Color(0x1F94A3B8) else Color(0x1F64748B))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isLoginMode) {
                                        Color(0xFF2563EB)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable { isLoginMode = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Login",
                                color = if (isLoginMode) Color.White else (if (isDark) DarkTextSecondary else LightTextSecondary),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (!isLoginMode) {
                                        Color(0xFF2563EB)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable { isLoginMode = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Register",
                                color = if (!isLoginMode) Color.White else (if (isDark) DarkTextSecondary else LightTextSecondary),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error Message
                    if (authError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) DarkErrorBg else LightErrorBg)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = if (isDark) DarkErrorText else LightErrorText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = authError ?: "",
                                    color = if (isDark) DarkErrorText else LightErrorText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Success Message
                    if (authSuccessMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) DarkSuccessBg else LightSuccessBg)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = if (isDark) DarkSuccessText else LightSuccessText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = authSuccessMessage ?: "",
                                    color = if (isDark) DarkSuccessText else LightSuccessText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Form Fields
                    if (isLoginMode) {
                        // Login Fields
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = if (isDark) DarkCardBorderAlt else LightCardBorderAlt
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = if (isDark) DarkCardBorderAlt else LightCardBorderAlt
                            )
                        )
                    } else {
                        // Registration Fields
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = { Text("First Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = if (isDark) DarkCardBorderAlt else LightCardBorderAlt
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = { Text("Last Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = if (isDark) DarkCardBorderAlt else LightCardBorderAlt
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = if (isDark) DarkCardBorderAlt else LightCardBorderAlt
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = if (isDark) DarkCardBorderAlt else LightCardBorderAlt
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = if (isDark) DarkCardBorderAlt else LightCardBorderAlt
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Role Selection Box
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedRole,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Role") },
                                trailingIcon = {
                                    IconButton(onClick = { roleDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { roleDropdownExpanded = true },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = if (isDark) DarkCardBorderAlt else LightCardBorderAlt
                                )
                            )
                            DropdownMenu(
                                expanded = roleDropdownExpanded,
                                onDismissRequest = { roleDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("PATIENT", "DOCTOR", "RECEPTIONIST", "ADMIN").forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role) },
                                        onClick = {
                                            selectedRole = role
                                            roleDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (isLoginMode) {
                                viewModel.login(email, password)
                            } else {
                                viewModel.register(
                                    firstName = firstName,
                                    lastName = lastName,
                                    email = email,
                                    phone = phone,
                                    password = password,
                                    role = selectedRole
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        ),
                        enabled = !isAuthLoading
                    ) {
                        if (isAuthLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isLoginMode) "Log In" else "Sign Up",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
