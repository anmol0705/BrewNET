package android.saswat.viewModel

import android.app.Activity
import android.net.Uri
import android.saswat.state.PhoneAuthState
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class PhoneAuthViewModel : ViewModel() {
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
    
    // Callbacks for phone auth
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential) { success ->
                if (success) {
                    _phoneAuthState.value = PhoneAuthState.AutoVerified
                }
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.e("PhoneAuthViewModel", "Verification failed", e)
            _phoneAuthState.value = PhoneAuthState.Error(e.message ?: "Verification failed")
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            storedVerificationId = verificationId
            resendToken = token
            _phoneAuthState.value = PhoneAuthState.CodeSent
        }
    }
    
    fun startPhoneNumberVerification(phoneNumber: String, activity: Activity) {
        // Basic validation for phone number (consider using libphonenumber library for better validation)
        if (!phoneNumber.startsWith("+")) {
            _phoneAuthState.value = PhoneAuthState.Error("Phone number must start with country code (e.g., +1)")
            return
        }
        
        _phoneAuthState.value = PhoneAuthState.Loading
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout duration
            .setActivity(activity) // Activity for callback binding
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
            
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    
    fun verifyPhoneNumberWithCode(code: String, onComplete: (Boolean) -> Unit) {
        if (storedVerificationId.isEmpty()) {
            _phoneAuthState.value = PhoneAuthState.Error("Verification ID not found")
            onComplete(false)
            return
        }
        
        _phoneAuthState.value = PhoneAuthState.Loading
        val credential = PhoneAuthProvider.getCredential(storedVerificationId, code)
        signInWithPhoneAuthCredential(credential, onComplete)
    }
    
    fun resendVerificationCode(phoneNumber: String, activity: Activity) {
        _phoneAuthState.value = PhoneAuthState.Loading
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            
        // Only set resendToken if it's not null
        resendToken?.let { token ->
            options.setForceResendingToken(token)
        }
            
        PhoneAuthProvider.verifyPhoneNumber(options.build())
    }
    
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user ?: throw Exception("Failed to sign in: No user returned")
                val isNewUser = authResult.additionalUserInfo?.isNewUser == true
                
                if (isNewUser) {
                    // This is a new user, we'll need to collect profile info later
                    val userData = UserData(
                        userId = user.uid,
                        phoneNumber = user.phoneNumber ?: "",
                        authProvider = "phone"
                    )
                    
                    firestore.collection("users").document(user.uid).set(userData).await()
                    _userData.value = userData
                }
                else {
                    // Existing user, fetch their data
                    val document = firestore.collection("users").document(user.uid).get().await()
                    if (document.exists()) {
                        _userData.value = document.toObject(UserData::class.java)
                    }
                }
                
                _phoneAuthState.value = PhoneAuthState.Authenticated(isNewUser)
                onComplete(true)
            } catch (e: Exception) {
                Log.e("PhoneAuthViewModel", "Sign in failed", e)
                _phoneAuthState.value = PhoneAuthState.Error(e.message ?: "Sign in failed")
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
                    genderSubcategory = genderSubcategory,
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
    
    
}