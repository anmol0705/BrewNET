package android.saswat.viewModel

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.saswat.state.PhoneAuthState
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class PhoneAuthViewModel(private val applicationContext: Context) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    // State flows
    private val _phoneAuthState = MutableStateFlow<PhoneAuthState>(PhoneAuthState.Initial)
    val phoneAuthState: StateFlow<PhoneAuthState> = _phoneAuthState
    
    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData
    
    // Store verification ID for later use
    private var storedVerificationId: String = ""
    
    // Store resend token
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    
    private var phoneNumberUtil: PhoneNumberUtil = PhoneNumberUtil.createInstance(applicationContext)

    // Callbacks for phone auth
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d("PhoneAuthViewModel", "Auto verification completed")
            signInWithPhoneAuthCredential(credential) { success ->
                if (success) {
                    _phoneAuthState.value = PhoneAuthState.AutoVerified
                }
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.e("PhoneAuthViewModel", "Verification failed", e)
            when (e) {
                is FirebaseAuthInvalidCredentialsException -> {
                    _phoneAuthState.value = PhoneAuthState.Error("Invalid phone number format")
                }
                is FirebaseAuthInvalidUserException -> {
                    _phoneAuthState.value = PhoneAuthState.Error("Invalid user")
                }
                is FirebaseAuthUserCollisionException -> {
                    _phoneAuthState.value = PhoneAuthState.Error("User already exists")
                }
                is FirebaseAuthWeakPasswordException -> {
                    _phoneAuthState.value = PhoneAuthState.Error("Weak password")
                }
                is FirebaseAuthException -> {
                    _phoneAuthState.value = PhoneAuthState.Error("Authentication error: ${e.message}")
                }
                else -> {
                    _phoneAuthState.value = PhoneAuthState.Error(e.message ?: "Verification failed")
                }
            }
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            Log.d("PhoneAuthViewModel", "New code sent with verificationId: $verificationId")
            storedVerificationId = verificationId
            resendToken = token
            _phoneAuthState.value = PhoneAuthState.CodeSent(verificationId)
        }
    }
    
    fun startPhoneNumberVerification(phoneNumber: String, activity: Activity) {
        try {
            clearVerificationData() // Use this instead of resetState()
            
            val parsedNumber = phoneNumberUtil.parse(phoneNumber, null)
            
            if (!phoneNumberUtil.isValidNumber(parsedNumber)) {
                _phoneAuthState.value = PhoneAuthState.Error("Please enter a valid phone number")
                return
            }
            
            val formattedNumber = phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
            
            _phoneAuthState.value = PhoneAuthState.Loading
            
            Log.d("PhoneAuthViewModel", "Starting new verification for number: $formattedNumber")
            
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            
            PhoneAuthProvider.verifyPhoneNumber(options)
            
        } catch (e: NumberParseException) {
            Log.e("PhoneAuthViewModel", "Number parse exception", e)
            _phoneAuthState.value = PhoneAuthState.Error("Invalid phone number format. Please include country code (+XX)")
        } catch (e: Exception) {
            Log.e("PhoneAuthViewModel", "General exception", e)
            _phoneAuthState.value = PhoneAuthState.Error("Error processing phone number: ${e.message}")
        }
    }
    
    fun resendVerificationCode(phoneNumber: String, activity: Activity) {
        _phoneAuthState.value = PhoneAuthState.Loading
        
        try {
            val parsedNumber = phoneNumberUtil.parse(phoneNumber, null)
            
            if (!phoneNumberUtil.isValidNumber(parsedNumber)) {
                _phoneAuthState.value = PhoneAuthState.Error("Please enter a valid phone number")
                return
            }
            
            val formattedNumber = phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
            Log.d("PhoneAuthViewModel", "Resending code to: $formattedNumber")
            
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
            
            // Only set resendToken if it's not null
            resendToken?.let { token ->
                Log.d("PhoneAuthViewModel", "Using resend token")
                optionsBuilder.setForceResendingToken(token)
            }
            
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
            
        } catch (e: NumberParseException) {
            Log.e("PhoneAuthViewModel", "Number parse exception during resend", e)
            _phoneAuthState.value = PhoneAuthState.Error("Invalid phone number format. Please include country code (+XX)")
        } catch (e: Exception) {
            Log.e("PhoneAuthViewModel", "Error during resend", e)
            _phoneAuthState.value = PhoneAuthState.Error("Error processing phone number: ${e.message}")
        }
    }
    
    fun setVerificationId(verificationId: String) {
        Log.d("PhoneAuthViewModel", "Setting verification ID: $verificationId")
        storedVerificationId = verificationId
    }

    fun verifyPhoneNumberWithCode(code: String, onComplete: (Boolean) -> Unit) {
        Log.d("PhoneAuthViewModel", "Verifying code: $code with storedVerificationId: $storedVerificationId")
        
        if (storedVerificationId.isEmpty()) {
            Log.e("PhoneAuthViewModel", "Verification ID is empty")
            _phoneAuthState.value = PhoneAuthState.Error("Verification ID not found. Please request a new code.")
            onComplete(false)
            return
        }
        
        _phoneAuthState.value = PhoneAuthState.Loading
        viewModelScope.launch {
            try {
                Log.d("PhoneAuthViewModel", "Creating credential with verification ID: $storedVerificationId and code: $code")
                val credential = PhoneAuthProvider.getCredential(storedVerificationId, code)
                signInWithPhoneAuthCredential(credential) { success ->
                    if (!success) {
                        _phoneAuthState.value = PhoneAuthState.Error("Invalid verification code")
                    }
                    onComplete(success)

                }
            } catch (e: Exception) {
                Log.e("PhoneAuthViewModel", "Error during verification", e)
                _phoneAuthState.value = PhoneAuthState.Error(e.message ?: "Error verifying code")
                onComplete(false)
            }
        }
    }
    
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("PhoneAuthViewModel", "Starting sign in with credential")
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user ?: throw Exception("Failed to sign in: No user returned")
                val isNewUser = authResult.additionalUserInfo?.isNewUser == true
                
                Log.d("PhoneAuthViewModel", "Sign in successful. User ID: ${user.uid}, New user: $isNewUser")
                
                if (isNewUser) {
                    val userData = UserData(
                        userId = user.uid,
                        phoneNumber = user.phoneNumber ?: "",
                        authProvider = "phone"
                    )
                    
                    firestore.collection("users").document(user.uid).set(userData).await()
                    _userData.value = userData
                } else {
                    val document = firestore.collection("users").document(user.uid).get().await()
                    if (document.exists()) {
                        _userData.value = document.toObject(UserData::class.java)
                    }
                }
                
                _phoneAuthState.value = PhoneAuthState.Authenticated(isNewUser)
                onComplete(true)
            } catch (e: Exception) {
                Log.e("PhoneAuthViewModel", "Sign in failed", e)
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        _phoneAuthState.value = PhoneAuthState.Error("Invalid verification code")
                    }
                    is FirebaseAuthInvalidUserException -> {
                        _phoneAuthState.value = PhoneAuthState.Error("Invalid user")
                    }
                    is FirebaseAuthUserCollisionException -> {
                        _phoneAuthState.value = PhoneAuthState.Error("User already exists")
                    }
                    is FirebaseAuthWeakPasswordException -> {
                        _phoneAuthState.value = PhoneAuthState.Error("Weak password")
                    }
                    else -> {
                        _phoneAuthState.value = PhoneAuthState.Error("Sign in failed: ${e.message}")
                    }
                }
                onComplete(false)
            }
        }
    }
    
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): String {
        try {
            val storageRef = storage.reference.child("profile_images/$userId")
            val uploadTask = storageRef.putFile(imageUri)
            uploadTask.await()
            return storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("PhoneAuthViewModel", "Error uploading profile image", e)
            throw e
        }
    }
    
    fun completePhoneUserProfile(
        username: String,
        dateOfBirth: String,
        gender: String,
        genderSubcategory: String = "",
        email: String = "",
        profileImageUri: Uri? = null,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _phoneAuthState.value = PhoneAuthState.Loading
                
                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                
                // Upload profile image if provided
                var profileImageUrl = ""
                if (profileImageUri != null) {
                    profileImageUrl = uploadProfileImage(currentUser.uid, profileImageUri)
                }
                
                val userData = UserData(
                    username = username,
                    email = email,
                    userId = currentUser.uid,
                    profileImageUrl = profileImageUrl,
                    dateOfBirth = dateOfBirth,
                    gender = gender,

                    phoneNumber = currentUser.phoneNumber ?: "",
                    authProvider = "phone"
                )
                
                firestore.collection("users").document(currentUser.uid).set(userData).await()
                
                // Update local state
                _userData.value = userData
                
                _phoneAuthState.value = PhoneAuthState.ProfileCompleted
                onComplete(true)
            } catch (e: Exception) {
                Log.e("PhoneAuthViewModel", "Profile completion failed", e)
                _phoneAuthState.value = PhoneAuthState.Error(e.message ?: "Profile completion failed")
                onComplete(false)
            }
        }
    }
    
    fun resetState() {
        _phoneAuthState.value = PhoneAuthState.Initial
    }
    
    fun clearVerificationData() {
        storedVerificationId = ""
        resendToken = null
        _phoneAuthState.value = PhoneAuthState.Initial
    }
}