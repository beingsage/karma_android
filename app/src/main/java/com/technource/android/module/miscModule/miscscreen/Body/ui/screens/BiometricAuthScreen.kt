//package com.technource.android.module.miscModule.miscscreen.Body.ui.screens
//
//import android.os.Build
//import androidx.biometric.BiometricManager
//import androidx.biometric.BiometricPrompt
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.FragmentActivity
//import androidx.navigation.NavController
//import com.technource.android.module.miscModule.miscscreen.Body.ui.viewmodels.MainViewModel
//
//@Composable
//fun BiometricAuthScreen(
//    navController: NavController,
//    viewModel: MainViewModel,
//    onAuthSuccess: () -> Unit
//) {
//    val context = LocalContext.current
//    var authState by remember { mutableStateOf(AuthState.IDLE) }
//    var errorMessage by remember { mutableStateOf("") }
//
//    val biometricManager = BiometricManager.from(context)
//    val canAuthenticate = biometricManager.canAuthenticate(
//        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
//    )
//
//    LaunchedEffect(Unit) {
//        when (canAuthenticate) {
//            BiometricManager.BIOMETRIC_SUCCESS -> {
//                // Biometric authentication is available
//            }
//            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
//                errorMessage = "No biometric hardware available"
//                authState = AuthState.ERROR
//            }
//            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
//                errorMessage = "Biometric hardware unavailable"
//                authState = AuthState.ERROR
//            }
//            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
//                errorMessage = "No biometric credentials enrolled"
//                authState = AuthState.ERROR
//            }
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(24.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Icon(
//            imageVector = Icons.Default.Fingerprint,
//            contentDescription = "Biometric Authentication",
//            modifier = Modifier.size(80.dp),
//            tint = MaterialTheme.colorScheme.primary
//        )
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        Text(
//            text = "Secure Access",
//            style = MaterialTheme.typography.headlineMedium,
//            fontWeight = FontWeight.Bold
//        )
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Text(
//            text = "Use your fingerprint or face to access your health data securely",
//            style = MaterialTheme.typography.bodyLarge,
//            textAlign = TextAlign.Center,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//
//        Spacer(modifier = Modifier.height(32.dp))
//
//        when (authState) {
//            AuthState.IDLE -> {
//                Button(
//                    onClick = {
//                        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
//                            authenticateWithBiometric(
//                                context as FragmentActivity,
//                                onSuccess = {
//                                    authState = AuthState.SUCCESS
//                                    onAuthSuccess()
//                                },
//                                onError = { error ->
//                                    authState = AuthState.ERROR
//                                    errorMessage = error
//                                }
//                            )
//                        }
//                    },
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Fingerprint,
//                        contentDescription = null,
//                        modifier = Modifier.size(20.dp)
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text("Authenticate")
//                }
//            }
//
//            AuthState.AUTHENTICATING -> {
//                CircularProgressIndicator()
//                Spacer(modifier = Modifier.height(16.dp))
//                Text("Authenticating...")
//            }
//
//            AuthState.SUCCESS -> {
//                Icon(
//                    imageVector = Icons.Default.CheckCircle,
//                    contentDescription = "Success",
//                    modifier = Modifier.size(48.dp),
//                    tint = MaterialTheme.colorScheme.primary
//                )
//                Spacer(modifier = Modifier.height(16.dp))
//                Text(
//                    text = "Authentication Successful!",
//                    style = MaterialTheme.typography.titleMedium,
//                    color = MaterialTheme.colorScheme.primary
//                )
//            }
//
//            AuthState.ERROR -> {
//                Icon(
//                    imageVector = Icons.Default.Error,
//                    contentDescription = "Error",
//                    modifier = Modifier.size(48.dp),
//                    tint = MaterialTheme.colorScheme.error
//                )
//                Spacer(modifier = Modifier.height(16.dp))
//                Text(
//                    text = errorMessage,
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.error,
//                    textAlign = TextAlign.Center
//                )
//                Spacer(modifier = Modifier.height(16.dp))
//                TextButton(
//                    onClick = {
//                        authState = AuthState.IDLE
//                        errorMessage = ""
//                    }
//                ) {
//                    Text("Try Again")
//                }
//            }
//        }
//
//        Spacer(modifier = Modifier.height(32.dp))
//
//        OutlinedButton(
//            onClick = { navController.popBackStack() },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("Skip for Now")
//        }
//    }
//}
//
//private fun authenticateWithBiometric(
//    activity: FragmentActivity,
//    onSuccess: () -> Unit,
//    onError: (String) -> Unit
//) {
//    val executor = ContextCompat.getMainExecutor(activity)
//    val biometricPrompt = BiometricPrompt(
//        activity,
//        executor,
//        object : BiometricPrompt.AuthenticationCallback() {
//            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
//                super.onAuthenticationError(errorCode, errString)
//                onError(errString.toString())
//            }
//            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
//                super.onAuthenticationSucceeded(result)
//                onSuccess()
//            }
//            override fun onAuthenticationFailed() {
//                super.onAuthenticationFailed()
//                onError("Authentication failed. Please try again.")
//            }
//        }
//    )
//
//    val promptInfo = BiometricPrompt.PromptInfo.Builder()
//        .setTitle("Biometric Authentication")
//        .setSubtitle("Use your biometric credential to access BodyTrack")
//        .setAllowedAuthenticators(
//            BiometricManager.Authenticators.BIOMETRIC_STRONG or
//            BiometricManager.Authenticators.DEVICE_CREDENTIAL
//        )
//        .build()
//
//    biometricPrompt.authenticate(promptInfo)
//}
//
//enum class AuthState {
//    IDLE, AUTHENTICATING, SUCCESS, ERROR
//}
