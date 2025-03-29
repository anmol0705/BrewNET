package android.saswat.brewnet.ui.signInandSignUp

import android.saswat.brewnet.screens.Screens
import android.saswat.state.PhoneAuthState
import android.saswat.viewModel.PhoneAuthViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneVerificationScreen(
    navController: NavController,
    phoneAuthViewModel: PhoneAuthViewModel,
    phoneNumber: String,
    verificationId: String
) {
    var otpValue by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isResendEnabled by remember { mutableStateOf(true) }
    var secondsLeft by remember { mutableStateOf(60) }
    
    val phoneAuthState by phoneAuthViewModel.phoneAuthState.collectAsState()

    LaunchedEffect(verificationId) {
        phoneAuthViewModel.setVerificationId(verificationId)
    }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        isResendEnabled = true
    }

    LaunchedEffect(phoneAuthState) {
        when (phoneAuthState) {
            is PhoneAuthState.CodeSent -> {
                errorMessage = null
                secondsLeft = 60
                isResendEnabled = false
            }
            is PhoneAuthState.Authenticated -> {
                val isNewUser = (phoneAuthState as PhoneAuthState.Authenticated).isNewUser
                if (isNewUser) {
                    navController.navigate(Screens.CompleteProfile.route)
                } else {
                    navController.navigate(Screens.VerificationSuccess.route)
                }
            }
            is PhoneAuthState.Error -> {
                errorMessage = (phoneAuthState as PhoneAuthState.Error).message
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F6FF))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Verification Code",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Please enter code we just send to\n$phoneNumber",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 16.sp,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // OTP Input Field
        OutlinedTextField(
            value = otpValue,
            onValueChange = { 
                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                    otpValue = it
                    errorMessage = null  // Clear error when user types
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("Enter 6-digit code") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color.LightGray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                cursorColor = Color.Black,
                errorBorderColor = Color.Red,
                errorTextColor = Color.Red
            ),
            isError = errorMessage != null
        )

        errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Verify Button
        Button(
            onClick = {
                if (otpValue.length == 6) {
                    phoneAuthViewModel.verifyPhoneNumberWithCode(otpValue) { success ->
                        if (!success) {
                            errorMessage = "Invalid verification code"
                        }
                    }
                } else {
                    errorMessage = "Please enter a 6-digit code"
                }

            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            ),
            enabled = otpValue.length == 6 && phoneAuthState !is PhoneAuthState.Loading
        ) {
            if (phoneAuthState is PhoneAuthState.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    "Verify",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isResendEnabled) {
                Text(
                    "Resend code in $secondsLeft seconds",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            TextButton(
                onClick = {
                    phoneAuthViewModel.resendVerificationCode(phoneNumber, navController.context as android.app.Activity)
                    isResendEnabled = false
                    secondsLeft = 60
                },
                enabled = isResendEnabled && phoneAuthState !is PhoneAuthState.Loading
            ) {
                Text(
                    "Didn't receive code?\nResend",
                    color = if (isResendEnabled) Color(0xFF2196F3) else Color.Gray,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        }
    }
}