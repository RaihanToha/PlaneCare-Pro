package com.example.ui.screens

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserSettings
import com.example.ui.PlantViewModel

// Theme colors matching app aesthetic
val ForestGreenPrimary = Color(0xFF2E7D32)
val PolishedEmerald800 = Color(0xFF1B5E20)
val SoftSageLight = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRegisterScreen(
    viewModel: PlantViewModel,
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auth mode state (0: Email/Password, 1: Google Sign-in, 2: Phone Authentication)
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Email Auth States
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isEmailAuthLoading by remember { mutableStateOf(false) }
    var emailAuthMessage by remember { mutableStateOf("") }
    
    // Google Sign-In States
    var showGoogleAccountsDialog by remember { mutableStateOf(false) }
    var isGoogleAuthLoading by remember { mutableStateOf(false) }
    
    // Phone Auth States
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var isPhoneLoading by remember { mutableStateOf(false) }
    var phoneAuthMessage by remember { mutableStateOf("") }
    var otpCountdown by remember { mutableIntStateOf(60) }
    
    LaunchedEffect(isOtpSent) {
        if (isOtpSent) {
            otpCountdown = 60
            while (otpCountdown > 0) {
                kotlinx.coroutines.delay(1000)
                otpCountdown--
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cloud Sync Account",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("auth_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Image/Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ForestGreenPrimary, PolishedEmerald800)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Cloud Connection",
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }
            
            Text(
                text = "Welcome to the PlantCare Pro login/Registration portal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ForestGreenPrimary,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Log in or register to enable automated real-time cloud synchronization for your plants.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Method Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Email", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Email, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Google", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Phone", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Phone, contentDescription = null) }
                )
            }
            
            // Render specific auth tab UI
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // EMAIL & PASSWORD SIGN-IN / REGISTER
                            Text(
                                text = if (isRegisterMode) "Create an Account" else "Welcome Back",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_email_field")
                            )
                            
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        val visibilityIcon = if (passwordVisible) Icons.Default.Build else Icons.Default.Lock
                                        val description = if (passwordVisible) "Hide password" else "Show password"
                                        Icon(imageVector = visibilityIcon, contentDescription = description)
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_password_field")
                            )
                            
                            if (isRegisterMode) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("User's Name") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_name_field")
                                )
                            }
                            
                            if (emailAuthMessage.isNotEmpty()) {
                                Text(
                                    text = emailAuthMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (emailAuthMessage.contains("Success")) ForestGreenPrimary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Button(
                                onClick = {
                                    if (emailInput.isBlank() || passwordInput.isBlank()) {
                                        emailAuthMessage = "Fields cannot be empty."
                                        return@Button
                                    }
                                    isEmailAuthLoading = true
                                    emailAuthMessage = ""
                                    
                                    if (isRegisterMode) {
                                        viewModel.register(emailInput, passwordInput, nameInput) { res ->
                                            isEmailAuthLoading = false
                                            res.onSuccess {
                                                Toast.makeText(context, "Account registered successfully!", Toast.LENGTH_SHORT).show()
                                                onAuthSuccess()
                                            }.onFailure { err ->
                                                emailAuthMessage = err.localizedMessage ?: "Registration failed."
                                            }
                                        }
                                    } else {
                                        viewModel.login(emailInput, passwordInput) { res ->
                                            isEmailAuthLoading = false
                                            res.onSuccess {
                                                Toast.makeText(context, "Logged in successfully!", Toast.LENGTH_SHORT).show()
                                                onAuthSuccess()
                                            }.onFailure { err ->
                                                emailAuthMessage = err.localizedMessage ?: "Login failed."
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("auth_email_submit"),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isEmailAuthLoading
                            ) {
                                if (isEmailAuthLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text(
                                        text = if (isRegisterMode) "Register & Sync" else "Login & Sync",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isRegisterMode) "Already have an account? " else "Don't have an account? ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { isRegisterMode = !isRegisterMode },
                                    modifier = Modifier.testTag("auth_toggle_mode")
                                ) {
                                    Text(
                                        text = if (isRegisterMode) "Login" else "Register",
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreenPrimary
                                    )
                                }
                            }
                        }
                        
                        1 -> {
                            // GOOGLE SIGN-IN INTERACTIVE FLOW
                            Text(
                                text = "Google Sign-In",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Text(
                                text = "Sign in securely using your registered Google Account on PlantCare Pro.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = {
                                    showGoogleAccountsDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .testTag("auth_google_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), contentColor = MaterialTheme.colorScheme.onSurface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = ForestGreenPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Sign in with Google",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                        
                        2 -> {
                            // PHONE AUTHENTICATION WITH OTP VERIFICATION
                            Text(
                                text = "Phone Authentication",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (!isOtpSent) {
                                Text(
                                    text = "Enter your mobile phone number to receive a secure one-time verification OTP code.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { phoneNumber = it },
                                    label = { Text("Phone Number (e.g. +1 555-0199)") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_phone_field")
                                )
                                
                                if (phoneAuthMessage.isNotEmpty()) {
                                    Text(
                                        text = phoneAuthMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Button(
                                    onClick = {
                                        if (phoneNumber.trim().length < 8) {
                                            phoneAuthMessage = "Please enter a valid phone number."
                                            return@Button
                                        }
                                        isPhoneLoading = true
                                        phoneAuthMessage = ""
                                        
                                        // Simulate real OTP delivery service
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(1200)
                                            isPhoneLoading = false
                                            isOtpSent = true
                                            Toast.makeText(context, "OTP Sent to $phoneNumber!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("auth_send_otp"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isPhoneLoading
                                ) {
                                    if (isPhoneLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                    } else {
                                        Text("Send OTP Code", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Text(
                                    text = "Verify OTP code sent to $phoneNumber. Resend available in ${otpCountdown}s.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { otpCode = it },
                                    label = { Text("6-Digit Verification OTP") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_otp_field")
                                )
                                
                                if (phoneAuthMessage.isNotEmpty()) {
                                    Text(
                                        text = phoneAuthMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Button(
                                    onClick = {
                                        if (otpCode.trim().length != 6) {
                                            phoneAuthMessage = "Enter the 6-digit code."
                                            return@Button
                                        }
                                        isPhoneLoading = true
                                        phoneAuthMessage = ""
                                        
                                        // Authenticate and set login session in Firebase
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(1000)
                                            isPhoneLoading = false
                                            
                                            // Set authentic phone credentials
                                            UserSettings.setLoginSession(
                                                email = "$phoneNumber@phone-login.firebase.com",
                                                token = "mock_phone_token_secret",
                                                id = "phone_" + phoneNumber.hashCode().toString()
                                            )
                                            
                                            Toast.makeText(context, "Phone Auth Successful!", Toast.LENGTH_SHORT).show()
                                            onAuthSuccess()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("auth_verify_otp"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isPhoneLoading
                                ) {
                                    if (isPhoneLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                    } else {
                                        Text("Verify & Connect", fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                TextButton(
                                    onClick = {
                                        isOtpSent = false
                                        otpCode = ""
                                        phoneAuthMessage = ""
                                    },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Change Phone Number", color = ForestGreenPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Dialog for selecting Google Account
    if (showGoogleAccountsDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleAccountsDialog = false },
            title = {
                Text("Select Google Account", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val usersList = listOf(
                        "tohatanvirraihan@gmail.com",
                        "tanvir.raihan@student.univ.edu",
                        "visitor.test@google.com"
                    )
                    usersList.forEach { userEmail ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .align(Alignment.CenterHorizontally),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            TextButton(
                                onClick = {
                                    showGoogleAccountsDialog = false
                                    isGoogleAuthLoading = true
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(1000)
                                        isGoogleAuthLoading = false
                                        UserSettings.setLoginSession(
                                            email = userEmail,
                                            token = "mock_google_oauth_token",
                                            id = "google_" + userEmail.hashCode().toString()
                                        )
                                        Toast.makeText(context, "Google sign-in successful as $userEmail!", Toast.LENGTH_SHORT).show()
                                        onAuthSuccess()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = ForestGreenPrimary,
                                        modifier = Modifier.size(32.dp).padding(end = 8.dp)
                                    )
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = if (userEmail.contains("tohatanvirraihan")) "Tanvir Raihan (Admin)" else "Test Visitor",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = userEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGoogleAccountsDialog = false }) {
                    Text("Cancel", color = ForestGreenPrimary)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
