// package com.technource.android.utils

// import android.app.Activity
// import android.content.Context
// import android.content.Intent
// import android.util.Log
// import com.google.android.gms.auth.api.identity.Identity
// import com.google.android.gms.auth.api.identity.SignInClient
// import com.google.android.gms.auth.api.signin.GoogleSignInAccount
// import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
// import com.google.api.client.http.javanet.NetHttpTransport
// import com.google.api.client.json.gson.GsonFactory
// import com.google.api.services.drive.Drive
// import com.google.api.services.drive.DriveScopes
// import java.util.Collections
// import com.google.android.gms.auth.api.identity.BeginSignInRequest
// import android.accounts.Account
// import android.content.IntentSender
// import android.widget.Toast
// import androidx.activity.result.IntentSenderRequest
// import com.google.android.gms.auth.api.identity.BeginSignInResult
// import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
// import com.google.android.gms.common.api.ApiException
// import com.google.android.gms.tasks.Tasks
// import com.technource.android.R
// import kotlinx.coroutines.*
// import kotlinx.coroutines.flow.*

// class GoogleDriveHelper(private val context: Context) {
//     private var driveService: Drive? = null
//     private val oneTapClient: SignInClient = Identity.getSignInClient(context)
//     private val serverClientId: String = context.getString(R.string.server_client_id)
//     private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
//     private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
//     val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
//     companion object {
//         private const val TAG = "GoogleDriveHelper"
//         const val REQUEST_SIGN_IN = 400
//         private const val PREF_NAME = "GoogleDrivePrefs"
//         private const val KEY_REFRESH_TOKEN = "refresh_token"
//         private const val KEY_ACCESS_TOKEN = "access_token"
//         private const val KEY_EMAIL = "email"
//         private const val MAX_RETRY_ATTEMPTS = 3
//         private const val RETRY_DELAY_MS = 2000L
//         private const val KEY_TOKEN_EXPIRY = "token_expiry"
//     }

//     private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//     private var retryCount = 0
//     private var signInJob: Job? = null

//     init {
//         // Check cached credentials first
//         tryRestoreSession()
//     }

//     private fun tryRestoreSession() {
//         val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
//         val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        
//         if (refreshToken != null && accessToken != null) {
//             scope.launch {
//                 try {
//                     // Validate token
//                     validateToken(accessToken)
//                     initializeDriveService(refreshToken, accessToken)
//                     _authState.value = AuthState.SignedIn
//                 } catch (e: Exception) {
//                     Log.w(TAG, "Cached session invalid, requiring fresh sign-in")
//                     clearAuthState()
//                 }
//             }
//         }
//     }

//     fun signIn(activity: Activity) {
//         if (signInJob?.isActive == true) {
//             Log.d(TAG, "Sign-in already in progress")
//             return
//         }

//         signInJob = scope.launch {
//             try {
//                 _authState.value = AuthState.SigningIn
//                 Log.d(TAG, "Starting sign-in flow")

//                 // Create sign-in request
//                 val signInRequest = BeginSignInRequest.builder()
//                     .setGoogleIdTokenRequestOptions(
//                         BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
//                             .setSupported(true)
//                             .setServerClientId(serverClientId)
//                             .setFilterByAuthorizedAccounts(true) // Changed to true to try existing accounts first
//                             .build()
//                     )
//                     .setAutoSelectEnabled(true)
//                     .build()

//                 try {
//                     // First try with existing accounts
//                     val result = withContext(Dispatchers.IO) {
//                         Tasks.await(oneTapClient.beginSignIn(signInRequest))
//                     }
//                     launchSignInFlow(activity, result)
//                 } catch (e: Exception) {
//                     Log.d(TAG, "No existing accounts, trying without filter")
                    
//                     // If no existing accounts, try without filter
//                     val newRequest = BeginSignInRequest.builder()
//                         .setGoogleIdTokenRequestOptions(
//                             BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
//                                 .setSupported(true)
//                                 .setServerClientId(serverClientId)
//                                 .setFilterByAuthorizedAccounts(false)
//                                 .build()
//                         )
//                         .setAutoSelectEnabled(false) // Disable auto-select for new account flow
//                         .build()

//                     try {
//                         val result = withContext(Dispatchers.IO) {
//                             Tasks.await(oneTapClient.beginSignIn(newRequest))
//                         }
//                         launchSignInFlow(activity, result)
//                     } catch (e: Exception) {
//                         throw AuthException("Failed to start sign-in flow", e, isRetryable = true)
//                     }
//                 }

//             } catch (e: Exception) {
//                 Log.e(TAG, "Sign-in failed", e)
//                 handleSignInError(e)
//             }
//         }
//     }

//     private fun launchSignInFlow(activity: Activity, result: BeginSignInResult) {
//         try {
//             activity.startIntentSenderForResult(
//                 result.pendingIntent.intentSender,
//                 REQUEST_SIGN_IN,
//                 null, 0, 0, 0
//             )
//         } catch (e: IntentSender.SendIntentException) {
//             throw AuthException("Could not launch sign-in UI", e, isRetryable = true)
//         }
//     }

//     fun handleSignInResult(data: Intent?) {
//         scope.launch {
//             try {
//                 requireNotNull(data) { "Sign-in data is null" }
                
//                 val credential = oneTapClient.getSignInCredentialFromIntent(data)
//                 val idToken = requireNotNull(credential.googleIdToken) { "No ID token received" }
                
//                 // Exchange ID token for access & refresh tokens
//                 val tokenResponse = exchangeToken(idToken)
                
//                 // Store tokens securely
//                 storeTokens(tokenResponse)
                
//                 initializeDriveService(tokenResponse.refreshToken, tokenResponse.accessToken)
                
//                 _authState.value = AuthState.SignedIn
                
//             } catch (e: Exception) {
//                 handleSignInError(e)
//             }
//         }
//     }

//     private fun handleSignInError(error: Exception) {
//         Log.e(TAG, "Sign-in error", error)
        
//         val shouldRetry = when {
//             error is ApiException && error.statusCode == GoogleSignInStatusCodes.NETWORK_ERROR
//                 && retryCount < MAX_RETRY_ATTEMPTS -> true
//             error is AuthException && error.isRetryable && retryCount < MAX_RETRY_ATTEMPTS -> true
//             else -> false
//         }

//         if (shouldRetry) {
//             retryCount++
//             scope.launch {
//                 delay(RETRY_DELAY_MS)
//                 _authState.value = AuthState.RetryingSignIn(retryCount)
//                 signIn(context as Activity) 
//             }
//         } else {
//             _authState.value = AuthState.SignInFailed(error)
//             clearAuthState()
//         }
//     }

//     private fun clearAuthState() {
//         prefs.edit().clear().apply()
//         driveService = null
//         retryCount = 0
//         _authState.value = AuthState.SignedOut
//     }

//     sealed class AuthState {
//         object Idle : AuthState()
//         object SigningIn : AuthState()
//         data class RetryingSignIn(val attempt: Int) : AuthState()
//         object SignedIn : AuthState()
//         object SignedOut : AuthState()
//         data class SignInFailed(val error: Exception) : AuthState()
//     }

//     class AuthException(
//         message: String, 
//         cause: Throwable? = null,
//         val isRetryable: Boolean = false
//     ) : Exception(message, cause)

//     private fun initializeDriveService(refreshToken: String, accessToken: String) {
//         try {
//             Log.d(TAG, "Initializing Drive service with cached tokens")
            
//             val account = Account(prefs.getString(KEY_EMAIL, null) ?: "", "com.google")
//             Log.d(TAG, "Created account object")
            
//             val credential = GoogleAccountCredential.usingOAuth2(
//                 context,
//                 listOf(DriveScopes.DRIVE_FILE)
//             ).setSelectedAccount(account)
//             Log.d(TAG, "Created OAuth credential")

//             driveService = Drive.Builder(
//                 NetHttpTransport(),
//                 GsonFactory.getDefaultInstance(),
//                 credential
//             )
//                 .setApplicationName("Karma")
//                 .build()
//             Log.d(TAG, "Successfully built Drive service")
//         } catch (e: Exception) {
//             Log.e(TAG, "Failed to initialize Drive service: ${e.message}")
//             Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
//             clearAuthState()
//         }
//     }

//     private fun validateToken(accessToken: String) {
//         // Token validation logic (e.g., check expiration, format, etc.)
//         // For simplicity, we just log the access token here
//         Log.d(TAG, "Validating access token: $accessToken")
//     }

//     private suspend fun exchangeToken(idToken: String): TokenResponse {
//         // Simulate network call to exchange ID token for access & refresh tokens
//         return withContext(Dispatchers.IO) {
//             // TODO: Implement actual token exchange logic
//             TokenResponse("mockAccessToken", "mockRefreshToken")
//         }
//     }

//     private fun storeTokens(tokenResponse: TokenResponse) {
//         prefs.edit()
//             .putString(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
//             .putString(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
//             .apply()
//     }

//     fun getDriveService(): Drive? {
//         if (!isSignedIn()) {
//             Log.w(TAG, "Drive service requested but user is not signed in")
//             return null
//         }
//         return driveService
//     }

//     fun isSignedIn(): Boolean {
//         val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
//         val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        
//         return when {
//             driveService == null -> false
//             accessToken == null || refreshToken == null -> false
//             isTokenExpired() -> {
//                 // Token is expired, try to refresh
//                 scope.launch {
//                     try {
//                         refreshTokens()
//                     } catch (e: Exception) {
//                         Log.e(TAG, "Failed to refresh tokens", e)
//                         clearAuthState()
//                     }
//                 }
//                 false
//             }
//             else -> true
//         }
//     }

//     private fun isTokenExpired(): Boolean {
//         val tokenExpiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0)
//         // Add 5 minute buffer before expiration
//         return System.currentTimeMillis() > (tokenExpiry - 300000)
//     }

//     private suspend fun refreshTokens() {
//         val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
//             ?: throw AuthException("No refresh token available")

//         try {
//             // Implement token refresh logic here using your OAuth endpoints
//             val tokenResponse = withContext(Dispatchers.IO) {
//                 // Make network call to refresh token
//                 // This is a placeholder - implement actual refresh logic
//                 TokenResponse("newAccessToken", refreshToken)
//             }

//             storeTokens(tokenResponse)
//             initializeDriveService(tokenResponse.refreshToken, tokenResponse.accessToken)
//             _authState.value = AuthState.SignedIn
//         } catch (e: Exception) {
//             throw AuthException("Failed to refresh tokens", e, isRetryable = true)
//         }
//     }

//     data class TokenResponse(
//         val accessToken: String,
//         val refreshToken: String
//     )
// }

package com.technource.android.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Tasks
import com.technource.android.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GoogleDriveHelper(private val context: Context) {
    private var driveService: Drive? = null
    private val oneTapClient: SignInClient = Identity.getSignInClient(context)
    private val serverClientId: String = context.getString(R.string.server_client_id)
    private val clientSecret: String = context.getString(R.string.client_secret) // NEW: Added client secret
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var retryCount = 0  // Added retryCount
    private var signInJob: Job? = null  // Added signInJob

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    companion object {
        private const val TAG = "GoogleDriveHelper"
        const val REQUEST_SIGN_IN = 400
        private const val PREF_NAME = "GoogleDrivePrefs"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_EMAIL = "email"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    init {
        tryRestoreSession()
    }

    private fun tryRestoreSession() {
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        if (refreshToken != null && accessToken != null) {
            scope.launch {
                try {
                    validateToken(accessToken)
                    initializeDriveService(refreshToken, accessToken)
                    _authState.value = AuthState.SignedIn
                } catch (e: Exception) {
                    Log.w(TAG, "Cached session invalid, requiring fresh sign-in", e)
                    clearAuthState()
                }
            }
        }
    }

    fun signIn(activity: Activity) {
        if (!isNetworkAvailable()) {
            _authState.value = AuthState.SignInFailed(Exception("No internet connection"))
            return
        }
        if (signInJob?.isActive == true) {
            Log.d(TAG, "Sign-in already in progress")
            return
        }
        signInJob = scope.launch {
            try {
                _authState.value = AuthState.SigningIn
                Log.d(TAG, "Starting sign-in flow")
                val signInRequest = BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId(serverClientId)
                            .setFilterByAuthorizedAccounts(true)
                            .build()
                    )
                    .setAutoSelectEnabled(true)
                    .build()
                try {
                    val result = withContext(Dispatchers.IO) {
                        Tasks.await(oneTapClient.beginSignIn(signInRequest))
                    }
                    launchSignInFlow(activity, result)
                } catch (e: ApiException) {
                    Log.d(TAG, "No existing accounts, trying without filter", e)
                    val newRequest = BeginSignInRequest.builder()
                        .setGoogleIdTokenRequestOptions(
                            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(serverClientId)
                                .setFilterByAuthorizedAccounts(false)
                                .build()
                        )
                        .setAutoSelectEnabled(false)
                        .build()
                    val result = withContext(Dispatchers.IO) {
                        Tasks.await(oneTapClient.beginSignIn(newRequest))
                    }
                    launchSignInFlow(activity, result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign-in failed", e)
                handleSignInError(e)
            }
        }
    }

    private fun launchSignInFlow(activity: Activity, result: com.google.android.gms.auth.api.identity.BeginSignInResult) {
        try {
            activity.startIntentSenderForResult(
                result.pendingIntent.intentSender,
                REQUEST_SIGN_IN,
                null, 0, 0, 0
            )
        } catch (e: android.content.IntentSender.SendIntentException) {
            throw AuthException("Could not launch sign-in UI", e, isRetryable = true)
        }
    }

    fun handleSignInResult(data: Intent?) {
        scope.launch {
            try {
                requireNotNull(data) { "Sign-in data is null" }
                val credential = oneTapClient.getSignInCredentialFromIntent(data)
                val idToken = requireNotNull(credential.googleIdToken) { "No ID token received" }
                val email = requireNotNull(credential.id) { "No email received" }
                val tokenResponse = exchangeToken(idToken)
                storeTokens(tokenResponse, email)
                initializeDriveService(tokenResponse.refreshToken, tokenResponse.accessToken)
                _authState.value = AuthState.SignedIn
            } catch (e: Exception) {
                handleSignInError(e)
            }
        }
    }

    private fun handleSignInError(error: Exception) {
        Log.e(TAG, "Sign-in error", error)
        val shouldRetry = error is ApiException && error.statusCode == com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.NETWORK_ERROR && retryCount < MAX_RETRY_ATTEMPTS
        if (shouldRetry) {
            retryCount++
            scope.launch {
                delay(RETRY_DELAY_MS)
                _authState.value = AuthState.RetryingSignIn(retryCount)
                signIn(context as Activity)
            }
        } else {
            _authState.value = AuthState.SignInFailed(error)
            clearAuthState()
        }
    }

    private fun clearAuthState() {
        prefs.edit().clear().apply()
        driveService = null
        retryCount = 0
        _authState.value = AuthState.SignedOut
    }

    sealed class AuthState {
        object Idle : AuthState()
        object SigningIn : AuthState()
        data class RetryingSignIn(val attempt: Int) : AuthState()
        object SignedIn : AuthState()
        object SignedOut : AuthState()
        data class SignInFailed(val error: Exception) : AuthState()
    }

    class AuthException(message: String, cause: Throwable? = null, val isRetryable: Boolean = false) : Exception(message, cause)

    private fun initializeDriveService(refreshToken: String, accessToken: String) {
        try {
            Log.d(TAG, "Initializing Drive service")
            val account = android.accounts.Account(prefs.getString(KEY_EMAIL, null) ?: "", "com.google")
            val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE))
                .setSelectedAccount(account)
            
            // Instead of setAccessToken, use credential directly with Drive.Builder
            driveService = Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Karma")
                .build()
                
            Log.d(TAG, "Drive service initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Drive service: ${e.message}", e)
            clearAuthState()
            throw e
        }
    }

    private fun validateToken(accessToken: String) {
        Log.d(TAG, "Validating access token: $accessToken")
        // TODO: Implement actual token validation (e.g., check with Google's tokeninfo endpoint)
    }

    private suspend fun exchangeToken(idToken: String): TokenResponse {
        if (!isNetworkAvailable()) throw AuthException("No internet connection", isRetryable = true)
        return withContext(Dispatchers.IO) {
            val requestBody = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", serverClientId)
                .add("client_secret", clientSecret)
                .add("code", idToken)
                .add("redirect_uri", "urn:ietf:wg:oauth:2.0:oob")
                .build()
            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(requestBody)
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) throw AuthException("Token exchange failed: ${response.code} ${response.message}", isRetryable = true)
            val json = JSONObject(response.body?.string() ?: "{}")
            TokenResponse(
                accessToken = json.getString("access_token"),
                refreshToken = json.optString("refresh_token", ""),
                expiresIn = json.getLong("expires_in")
            )
        }
    }

    private fun storeTokens(tokenResponse: TokenResponse, email: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
            .putString(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
            .putString(KEY_EMAIL, email)
            .putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + tokenResponse.expiresIn * 1000)
            .apply()
    }

    fun getDriveService(): Drive? {
        if (!isSignedIn()) {
            Log.w(TAG, "Drive service requested but user is not signed in")
            return null
        }
        return driveService
    }

    fun isSignedIn(): Boolean {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        return when {
            driveService == null -> false
            accessToken == null || refreshToken == null -> false
            isTokenExpired() -> {
                scope.launch {
                    try {
                        refreshTokens()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to refresh tokens", e)
                        clearAuthState()
                    }
                }
                false
            }
            else -> true
        }
    }

    fun createPermission(): com.google.api.services.drive.model.Permission {
        return com.google.api.services.drive.model.Permission()
            .setType("anyone")
            .setRole("reader")
    }

    
    private fun isTokenExpired(): Boolean {
        val tokenExpiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0)
        return System.currentTimeMillis() > (tokenExpiry - 300000) // 5-minute buffer
    }

    private suspend fun refreshTokens() {
        if (!isNetworkAvailable()) throw AuthException("No internet connection", isRetryable = true)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
            ?: throw AuthException("No refresh token available")
        try {
            val tokenResponse = withContext(Dispatchers.IO) {
                val requestBody = FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("client_id", serverClientId)
                    .add("client_secret", clientSecret)
                    .add("refresh_token", refreshToken)
                    .build()
                val request = Request.Builder()
                    .url("https://oauth2.googleapis.com/token")
                    .post(requestBody)
                    .build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) throw AuthException("Token refresh failed: ${response.code} ${response.message}", isRetryable = true)
                val json = JSONObject(response.body?.string() ?: "{}")
                TokenResponse(
                    accessToken = json.getString("access_token"),
                    refreshToken = refreshToken,
                    expiresIn = json.getLong("expires_in")
                )
            }
            storeTokens(tokenResponse, prefs.getString(KEY_EMAIL, "")!!)
            initializeDriveService(tokenResponse.refreshToken, tokenResponse.accessToken)
            _authState.value = AuthState.SignedIn
        } catch (e: Exception) {
            throw AuthException("Failed to refresh tokens", e, isRetryable = true)
        }
    }

    // NEW: Check network connectivity
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    data class TokenResponse(
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Long = 3600 // Default to 1 hour
    )
}