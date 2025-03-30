package android.saswat.brewnet.ui.signInandSignUp

import android.app.Activity
import android.saswat.brewnet.R
import android.saswat.brewnet.screens.Screens
import android.saswat.factory.PhoneAuthViewModelFactory
import android.saswat.state.AuthState
import android.saswat.state.PhoneAuthState
import android.saswat.viewModel.AuthViewModel
import android.saswat.viewModel.PhoneAuthViewModel
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.tasks.await

private fun Int.toPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

@Composable
fun SignInScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    onSignUpClick: () -> Unit = {},
    onEmailSignInClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // Set up phone auth view model with context
    val phoneAuthViewModel: PhoneAuthViewModel = viewModel(
        factory = PhoneAuthViewModelFactory(context.applicationContext)
    )
    
    // States for the UI
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedCountryCode by remember { mutableStateOf("+91") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showEmailSignIn by remember { mutableStateOf(false) }
    
    // Collect states from view models
    val authState by authViewModel.authState.collectAsState()
    val phoneAuthState by phoneAuthViewModel.phoneAuthState.collectAsState()
    
    // Store CCP reference in Compose state
    var ccp by remember { mutableStateOf<CountryCodePicker?>(null) }

    // Monitor auth states for navigation
    LaunchedEffect(authState) {
        Log.d("SignInScreen", "Auth state changed: $authState")
        when (authState) {
            is AuthState.Success -> {
                Log.d("SignInScreen", "Auth success, navigating to success screen")
                isLoading = false
                navController.navigate(Screens.MainScreen.route) {
                    popUpTo(Screens.SignInScreen.route) { inclusive = true }
                }
            }
            is AuthState.NeedsProfileCompletion -> {
                Log.d("SignInScreen", "Profile completion needed")
                isLoading = false
                navController.navigate(Screens.MainScreen.route) {
                    popUpTo(Screens.SignInScreen.route) { inclusive = true }
                }
            }
            is AuthState.Error -> {
                isLoading = false
                val error = (authState as AuthState.Error).message
                Log.e("SignInScreen", "Auth error: $error")
                errorMessage = error
            }
            else -> {
                Log.d("SignInScreen", "Auth state: $authState")
            }
        }
    }
    
    // Monitor phone auth states for navigation
    LaunchedEffect(phoneAuthState) {
        when (phoneAuthState) {
            is PhoneAuthState.CodeSent -> {
                ccp?.let { picker ->
                    val verificationId = (phoneAuthState as PhoneAuthState.CodeSent).verificationId
                    phoneAuthViewModel.setVerificationId(verificationId)
                    navController.navigate("verifyPhone/${picker.fullNumberWithPlus}/$verificationId")
                }
            }
            is PhoneAuthState.Authenticated -> {
                val isNewUser = (phoneAuthState as PhoneAuthState.Authenticated).isNewUser
                if (isNewUser) {
                    navController.navigate("completeProfile")
                } else {
                    navController.navigate("main") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
            is PhoneAuthState.Error -> {
                errorMessage = (phoneAuthState as PhoneAuthState.Error).message
            }
            else -> {}
        }
    }

    // Google Sign In setup
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // Clear existing Google sign-in on launch
    LaunchedEffect(Unit) {
        try {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut()
            // Sign out from Google
            googleSignInClient.signOut().await()
            Log.d("SignInScreen", "Successfully signed out from previous sessions")
        } catch (e: Exception) {
            Log.e("SignInScreen", "Error signing out", e)
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                isLoading = true
                errorMessage = null
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                Log.d("SignInScreen", "Got Google account: ${account?.email}")
                
                account?.idToken?.let { token ->
                    Log.d("SignInScreen", "Got Google Sign In token, starting authentication...")
                    authViewModel.handleGoogleSignInResult(token) { success ->
                        isLoading = false
                        if (!success) {
                            Log.e("SignInScreen", "Failed to authenticate with Google")
                            errorMessage = "Failed to authenticate with Google"
                        } else {
                            Log.d("SignInScreen", "Google authentication callback success")
                        }
                    }
                } ?: run {
                    isLoading = false
                    Log.e("SignInScreen", "No ID token received from Google")
                    errorMessage = "Failed to get authentication token"
                }
            } catch (e: ApiException) {
                isLoading = false
                Log.e("SignInScreen", "Google sign in failed with status code: ${e.statusCode}", e)
                errorMessage = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Sign in cancelled"
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Sign in failed"
                    else -> "Google sign-in failed: ${e.message}"
                }
            }
        } else {
            Log.d("SignInScreen", "Google Sign In result not OK: ${result.resultCode}")
        }
    }

    // Monitor auth states for navigation

    


    // Google Sign In Button Click Handler
    val handleGoogleSignIn = {
        errorMessage = null
        Log.d("SignInScreen", "Starting Google Sign In flow")
        // Force a fresh sign-in by signing out first
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d("SignInScreen", "Fresh sign out complete, launching sign in intent")
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    // UI Implementation
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF2F6FF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BrewNet Logo and Text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "BrewNet Logo",
                    modifier = Modifier.size(150.dp)
                )
            }

            // Toggle between Email and Phone Sign In
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { showEmailSignIn = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (!showEmailSignIn) Color(0xFF2196F3) else Color.Gray
                        )
                    ) {
                        Text(
                            "Phone Number",
                            fontWeight = if (!showEmailSignIn) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    
                    TextButton(
                        onClick = { showEmailSignIn = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (showEmailSignIn) Color(0xFF2196F3) else Color.Gray
                        )
                    ) {
                        Text(
                            "Email",
                            fontWeight = if (showEmailSignIn) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (showEmailSignIn) {
                // Email Sign In Form
                EmailSignInContent(
                    email = email,
                    password = password,
                    isLoading = isLoading,
                    onEmailChange = { email = it; errorMessage = null },
                    onPasswordChange = { password = it; errorMessage = null },
                    onSignInClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            isLoading = true
                            errorMessage = null
                            authViewModel.signInWithEmailPassword(email, password) { success ->
                                isLoading = false
                                if (!success) {
                                    errorMessage = "Invalid email or password"
                                }
                            }
                        } else {
                            errorMessage = "Please fill in all fields"
                        }
                    }
                )
            } else {
                // Phone Number Sign In Content
                PhoneSignInContent(
                    phoneNumber = phoneNumber,
                    ccp = ccp,
                    onCcpInit = { ccp = it },
                    onPhoneNumberChange = { phoneNumber = it },
                    isLoading = phoneAuthState is PhoneAuthState.Loading,
                    onVerifyClick = {
                        ccp?.let { picker ->
                            if (picker.isValidFullNumber) {
                                val fullPhoneNumber = picker.fullNumberWithPlus
                                phoneAuthViewModel.startPhoneNumberVerification(
                                    phoneNumber = fullPhoneNumber,
                                    activity = context as Activity
                                )
                            } else {
                                errorMessage = "Please enter a valid phone number"
                            }
                        }
                    }
                )
            }

            // Error message
            errorMessage?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // OR divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp,
                    color = Color.LightGray
                )
                
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp,
                    color = Color.LightGray
                )
            }
            
            // Google Sign In Button
            OutlinedButton(
                onClick = { handleGoogleSignIn() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.LightGray
                ),
                enabled = !isLoading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF2196F3)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.google),
                            contentDescription = "Google Icon",
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Sign in with Google",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            // Sign Up prompt at the bottom
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account?",
                    color = Color.DarkGray,
                    fontSize = 14.sp
                )
                
                TextButton(onClick = onSignUpClick) {
                    Text(
                        text = "Sign Up",
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailSignInContent(
    email: String,
    password: String,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign in with Email",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color.LightGray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                cursorColor = Color.Black,
                errorBorderColor = Color.Red,
                errorTextColor = Color.Red
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color.LightGray,
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSignInClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    "Sign In",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PhoneSignInContent(
    phoneNumber: String,
    ccp: CountryCodePicker?,
    onCcpInit: (CountryCodePicker) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    isLoading: Boolean,
    onVerifyClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Let's start with your\nnumber",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp,
            color = Color(0xFF333333),
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 1.dp,
                    color = Color.LightGray,
                    shape = RoundedCornerShape(28.dp)
                )
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            AndroidView(
                factory = { ctx ->
                    LinearLayout(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        
                        val cp = CountryCodePicker(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            showNameCode(false)
                            setShowPhoneCode(true)
                            setContentColor("#333333".toColorInt())
                            setTextSize(16)
                            setPadding(0, 0, 0, 0)
                            setDefaultCountryUsingNameCode("IN")
                            resetToDefaultCountry()
                        }
                        
                        val divider = android.view.View(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                1.toInt().toPx(ctx),
                                24.toInt().toPx(ctx)
                            ).apply {
                                marginStart = 8.toInt().toPx(ctx)
                                marginEnd = 8.toInt().toPx(ctx)
                            }
                            setBackgroundColor("#DDDDDD".toColorInt())
                        }
                        
                        val phoneEditText = android.widget.EditText(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            hint = "Enter phone number"
                            setHintTextColor("#9E9E9E".toColorInt())
                            setTextColor("#333333".toColorInt())
                            background = null
                            inputType = android.text.InputType.TYPE_CLASS_PHONE
                            textSize = 16f
                            
                            addTextChangedListener(object : android.text.TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                override fun afterTextChanged(s: android.text.Editable?) {
                                    onPhoneNumberChange(s?.toString() ?: "")
                                }
                            })
                        }
                        
                        cp.registerCarrierNumberEditText(phoneEditText)
                        onCcpInit(cp)
                        
                        addView(cp)
                        addView(divider)
                        addView(phoneEditText)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onVerifyClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF5678)
            ),
            enabled = phoneNumber.isNotEmpty() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    "Continue",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignInScreenPreview() {
    SignInScreen(navController = rememberNavController())
}