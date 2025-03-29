package android.saswat.state

sealed class UpdateState {
    object Idle : UpdateState()
    object Loading : UpdateState()
    object Success : UpdateState()
    data class Error(val message: String) : UpdateState()
}

sealed class ImageLoadState {
    object Idle : ImageLoadState()
    object Loading : ImageLoadState()
    object Success : ImageLoadState()
    data class Error(val message: String) : ImageLoadState()
}

sealed class PhoneAuthState {
    object Initial : PhoneAuthState()
    object Loading : PhoneAuthState()
    data class CodeSent(val verificationId: String) : PhoneAuthState()
    object AutoVerified : PhoneAuthState()
    data class Authenticated(val isNewUser: Boolean) : PhoneAuthState()
    object ProfileCompleted : PhoneAuthState()
    data class Error(val message: String) : PhoneAuthState()
}
sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object PasswordResetEmailSent : AuthState()
    object NeedsProfileCompletion : AuthState()
    data class Error(val message: String) : AuthState()
}
