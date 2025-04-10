package android.saswat.viewModel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.saswat.MyApplication
import android.saswat.state.AuthState
import android.saswat.state.ImageLoadState
import android.saswat.state.UpdateState
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class UserData(
    val username: String = "",
    val email: String = "",
    val userId: String = "",
    val profileImageUrl: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val phoneNumber: String = "",
    val authProvider: String = "email", // Possible values: "email", "phone", "google"
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String = "",
    val purpose: String = "",
    val want: String = "",
    val qualities: Map<String, Boolean> = mapOf(), 
    val interests: Map<String, Boolean> = mapOf(), 
    val isOnline: Boolean = false,
    val lastActive: Long = System.currentTimeMillis(),
    val locationUpdatedAt: Long = System.currentTimeMillis(),
    val bio: String? = null
)

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    private val _imageLoadState = MutableStateFlow<ImageLoadState>(ImageLoadState.Idle)
    val imageLoadState: StateFlow<ImageLoadState> = _imageLoadState

    init {
        // Check if user is already signed in and fetch their data
        auth.currentUser?.let { user ->
            fetchUserData()
        }
    }

    suspend fun uploadProfileImage(userId: String, imageUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            _imageLoadState.value = ImageLoadState.Loading

            // Compress the image before uploading
            val compressedImageFile = compressImage(imageUri) ?: throw Exception("Failed to compress image")

            val storageRef = storage.reference.child("profile_images/$userId")

            // Set metadata to enable caching
            val metadata = storageMetadata {
                cacheControl = "public, max-age=31536000" // 1 year cache
            }

            // Upload the compressed image file
            val uploadTask = storageRef.putFile(Uri.fromFile(compressedImageFile))
            uploadTask.await()

            // Delete the temporary compressed file
            compressedImageFile.delete()

            val downloadUrl = storageRef.downloadUrl.await()
            _imageLoadState.value = ImageLoadState.Success
            return@withContext downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error uploading profile image", e)
            _imageLoadState.value = ImageLoadState.Error(e.message ?: "Image upload failed")
            throw e
        }
    }

    private suspend fun compressImage(imageUri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = MyApplication.instance.contentResolver
            val inputStream = contentResolver.openInputStream(imageUri)

            // Decode the image dimensions without loading the full bitmap
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Calculate sample size to reduce to about 500px on the longest side
            val maxDimension = 500
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxDimension)

            // Load a smaller version of the bitmap
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val newInputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(newInputStream, null, loadOptions)
            newInputStream?.close()

            // Save the compressed bitmap to a temp file
            val tempFile = File.createTempFile("profile_pic", ".jpg", MyApplication.instance.cacheDir)
            val outputStream = FileOutputStream(tempFile)

            bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.close()
            bitmap?.recycle()

            return@withContext tempFile
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error compressing image", e)
            return@withContext null
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, targetSize: Int): Int {
        var sampleSize = 1
        while (width / (sampleSize * 2) >= targetSize && height / (sampleSize * 2) >= targetSize) {
            sampleSize *= 2
        }
        return sampleSize
    }

    fun updateUserData(newUsername: String, newDateOfBirth: String, newGender: String, newGenderSubcategory: String, newBio: String) {
        viewModelScope.launch {
            try {
                _updateState.value = UpdateState.Loading

                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                val userRef = firestore.collection("users").document(currentUser.uid)

                val updates = hashMapOf<String, Any>(
                    "username" to newUsername,
                    "dateOfBirth" to newDateOfBirth,
                    "gender" to newGender,
                    "bio" to newBio
                )

                // Update Firestore
                userRef.update(updates).await()

                // Fetch the complete user data to ensure we have all fields
                val updatedDoc = userRef.get().await()
                val updatedUserData = updatedDoc.toObject(UserData::class.java)?.copy(
                    username = newUsername,
                    dateOfBirth = newDateOfBirth,
                    gender = newGender,
                    bio = newBio
                )

                // Update local state
                _userData.value = updatedUserData

                _updateState.value = UpdateState.Success

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating user data", e)
                _updateState.value = UpdateState.Error(e.message ?: "Failed to update user data")
            }
        }
    }

    fun updateProfileImage(imageUri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _updateState.value = UpdateState.Loading

                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                
                // Add log statements to track what's happening
                Log.d("AuthViewModel", "Starting profile image update for user: ${currentUser.uid}")
                
                val profileImageUrl = uploadProfileImage(currentUser.uid, imageUri)
                Log.d("AuthViewModel", "Image uploaded successfully, URL: $profileImageUrl")

                // Update Firestore with new image URL
                val userRef = firestore.collection("users").document(currentUser.uid)
                
                // Use set with merge option instead of update to ensure the field is created if it doesn't exist
                val updates = mapOf("profileImageUrl" to profileImageUrl)
                userRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
                
                Log.d("AuthViewModel", "Firestore document updated with new image URL")

                // Update local state
                _userData.value = _userData.value?.copy(profileImageUrl = profileImageUrl)
                Log.d("AuthViewModel", "Local user data updated with URL: ${_userData.value?.profileImageUrl}")

                _updateState.value = UpdateState.Success
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating profile image", e)
                _updateState.value = UpdateState.Error(e.message ?: "Failed to update profile image")
                onComplete(false)
            }
        }
    }

    fun fetchUserData() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val document = firestore.collection("users").document(currentUser.uid).get().await()

                if (document.exists()) {
                    val userData = document.toObject(UserData::class.java)
                    _userData.value = userData?.copy(userId = currentUser.uid)
                } else {
                    Log.e("AuthViewModel", "No user document found for ID: ${currentUser.uid}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching user data: ${e.message}", e)
            }
        }
    }

    fun isProfileComplete(): Boolean {
        val user = _userData.value ?: return false
        return user.username.isNotBlank() &&
                user.dateOfBirth.isNotBlank() &&
                user.gender.isNotBlank() &&
                user.bio != null
    }

    fun signInWithEmailPassword(email: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let {
                    fetchUserData()
                    _authState.value = AuthState.Success
                    onComplete(true)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Authentication failed")
                onComplete(false)
            }
        }
    }

    fun signUpWithEmailPassword(
        email: String,
        password: String,
        phoneNumber: String,
        confirmPassword: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val (isValid, errorMessage) = validateSignUpFields(email, phoneNumber, password, confirmPassword)
                if (!isValid) {
                    _authState.value = AuthState.Error(errorMessage)
                    onComplete(false)
                    return@launch
                }

                _authState.value = AuthState.Loading
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid ?: throw Exception("Failed to create user: No UID returned")

                val userData = UserData(
                    username = email.substringBefore('@'), // Default username from email
                    email = email,
                    userId = uid,
                    phoneNumber = phoneNumber,
                    authProvider = "email"
                )

                firestore.collection("users").document(uid).set(userData).await()

                // Update local state
                _userData.value = userData

                _authState.value = AuthState.Success
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign up failed: ${e.localizedMessage}", e)
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
                onComplete(false)
            }
        }
    }

    fun handleGoogleSignInResult(idToken: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                // Create credential
                val credential = GoogleAuthProvider.getCredential(idToken, null)

                // Sign in with credential
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user ?: throw Exception("Failed to sign in: No user returned")
                val isNewUser = authResult.additionalUserInfo?.isNewUser == true

                if (isNewUser) {
                    // Create a new user record in Firestore
                    val userData = UserData(
                        username = user.displayName ?: "",
                        email = user.email ?: "",
                        userId = user.uid,
                        profileImageUrl = user.photoUrl?.toString() ?: "",
                        authProvider = "google"
                    )

                    firestore.collection("users").document(user.uid).set(userData).await()
                    _userData.value = userData
                    _authState.value = AuthState.NeedsProfileCompletion
                    onComplete(true)
                } else {
                    // Fetch existing user data
                    fetchUserData()
                    _authState.value = AuthState.Success
                    onComplete(true)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign in failed: ${e.localizedMessage}", e)
                _authState.value = AuthState.Error(e.message ?: "Google sign in failed")
                onComplete(false)
            }
        }
    }

    fun completeUserProfile(
        username: String,
        dateOfBirth: String,
        gender: String,
        genderSubcategory: String,
        bio: String,
        profileImageUri: Uri? = null,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _updateState.value = UpdateState.Loading

                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                Log.d("AuthViewModel", "Completing profile for user: ${currentUser.uid}")

                // Upload profile image if provided
                var profileImageUrl = _userData.value?.profileImageUrl ?: ""
                if (profileImageUri != null) {
                    profileImageUrl = uploadProfileImage(currentUser.uid, profileImageUri)
                    Log.d("AuthViewModel", "Profile image uploaded with URL: $profileImageUrl")
                }

                val userRef = firestore.collection("users").document(currentUser.uid)

                // Get current data and update with new values
                val currentData = _userData.value
                val updatedData = currentData?.copy(
                    username = username,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    bio = bio,
                    profileImageUrl = profileImageUrl
                ) ?: UserData(
                    username = username,
                    email = currentUser.email ?: "",
                    userId = currentUser.uid,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    bio = bio,
                    profileImageUrl = profileImageUrl,
                    authProvider = currentData?.authProvider ?: "email"
                )

                // Use set instead of update to ensure all fields are properly updated
                userRef.set(updatedData).await()
                Log.d("AuthViewModel", "User profile completed and saved with imageUrl: $profileImageUrl")

                // Update local state
                _userData.value = updatedData
                Log.d("AuthViewModel", "Local state updated with imageUrl: ${_userData.value?.profileImageUrl}")

                _updateState.value = UpdateState.Success
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Profile update failed: ${e.localizedMessage}", e)
                _updateState.value = UpdateState.Error(e.message ?: "Profile update failed")
                onComplete(false)
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.PasswordResetEmailSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Initial
        _userData.value = null
        _updateState.value = UpdateState.Idle
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    fun resetImageLoadState() {
        _imageLoadState.value = ImageLoadState.Idle
    }

    fun updateUserLocation(
        latitude: Double,
        longitude: Double,
        locationName: String = "",
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _updateState.value = UpdateState.Loading

                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                val userRef = firestore.collection("users").document(currentUser.uid)

                val updates = mapOf(
                    "latitude" to latitude as Any,
                    "longitude" to longitude as Any,
                    "locationUpdatedAt" to System.currentTimeMillis()
                )
                if (locationName.isNotEmpty()) {
                    (updates as MutableMap<String, Any>)["locationName"] = locationName
                }

                userRef.update(updates).await()

                _userData.value = _userData.value?.copy(
                    latitude = latitude,
                    longitude = longitude,
                    locationName = if (locationName.isNotEmpty()) locationName else _userData.value?.locationName ?: "",
                    locationUpdatedAt = System.currentTimeMillis()
                )

                _updateState.value = UpdateState.Success
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating location", e)
                _updateState.value = UpdateState.Error(e.message ?: "Failed to update location")
                onComplete(false)
            }
        }
    }


    fun updateUserPurpose(purpose: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val userRef = firestore.collection("users").document(currentUser.uid)
                    val updates = mapOf(
                        "purpose" to purpose
                    )
                    userRef.update(updates).await()

                    // Update local state
                    _userData.value = _userData.value?.copy(purpose = purpose)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating user purpose", e)
                onComplete(false)
            }
        }
    }

    fun updateUserSeek(want: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _updateState.value = UpdateState.Loading
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val userRef = firestore.collection("users").document(currentUser.uid)
                    val updates = mapOf(
                        "want" to want
                    )
                    userRef.update(updates).await()

                    // Update local state
                    _userData.value = _userData.value?.copy(want = want)
                    _updateState.value = UpdateState.Success
                    onComplete(true)
                } else {
                    _updateState.value = UpdateState.Error("User not authenticated")
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating user seek preference", e)
                _updateState.value = UpdateState.Error(e.message ?: "Failed to update preference")
                onComplete(false)
            }
        }
    }

    fun updateUserQualities(qualities: Map<String, Boolean>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _updateState.value = UpdateState.Loading
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val userRef = firestore.collection("users").document(currentUser.uid)
                    val updates = mapOf(
                        "qualities" to qualities
                    )
                    userRef.update(updates).await()

                    // Update local state
                    _userData.value = _userData.value?.copy(qualities = qualities)
                    _updateState.value = UpdateState.Success
                    onComplete(true)
                } else {
                    _updateState.value = UpdateState.Error("User not authenticated")
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating user qualities", e)
                _updateState.value = UpdateState.Error(e.message ?: "Failed to update qualities")
                onComplete(false)
            }
        }
    }

    fun updateUserInterests(interests: Map<String, Boolean>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _updateState.value = UpdateState.Loading
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val userRef = firestore.collection("users").document(currentUser.uid)
                    val updates = mapOf(
                        "interests" to interests
                    )
                    userRef.update(updates).await()

                    // Update local state
                    _userData.value = _userData.value?.copy(interests = interests)
                    _updateState.value = UpdateState.Success
                    onComplete(true)
                } else {
                    _updateState.value = UpdateState.Error("User not authenticated")
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating user interests", e)
                _updateState.value = UpdateState.Error(e.message ?: "Failed to update interests")
                onComplete(false)
            }
        }
    }

    fun validateSignUpFields(
        email: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String
    ): Pair<Boolean, String> {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank() || phoneNumber.isBlank()) {
            return Pair(false, "All fields are required")
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Pair(false, "Please enter a valid email address")
        }

        if (phoneNumber.length < 10) {
            return Pair(false, "Please enter a valid phone number")
        }

        if (password != confirmPassword) {
            return Pair(false, "Passwords don't match")
        }

        if (password.length < 8) {
            return Pair(false, "Password must be at least 8 characters long")
        }

        return Pair(true, "")
    }

    fun updateOnlineStatus(isOnline: Boolean) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val userRef = firestore.collection("users").document(currentUser.uid)
                
                val updates = mapOf(
                    "isOnline" to isOnline,
                    "lastActive" to System.currentTimeMillis()
                )
                
                userRef.update(updates).await()
                
                // Update local state
                _userData.value = _userData.value?.copy(
                    isOnline = isOnline,
                    lastActive = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating online status: ${e.message}", e)
            }
        }
    }
}