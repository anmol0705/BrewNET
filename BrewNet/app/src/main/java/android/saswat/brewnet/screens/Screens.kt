package android.saswat.brewnet.screens

sealed class Screens(val route: String) {
    data object FirstScreen: Screens("first")
    data object SignIn: Screens("signIn")
    data object SignUp: Screens("signUp")
    data object MainScreen: Screens("main")
    data object PhoneSignIn: Screens("phoneSignIn")
    data object VerifyPhone: Screens("verifyPhone/{phoneNumber}")
    data object VerificationSuccess: Screens("verificationSuccess")
    data object CompleteProfile: Screens("completeProfile")
    data object AgeSelection: Screens("ageSelection")
    data object GenderSelection: Screens("genderSelection")
    data object PhotosScreen : Screens("photos")

    // Helper functions
    companion object {
        fun getVerifyPhoneRoute(phoneNumber: String) = "verifyPhone/$phoneNumber"
    }
}