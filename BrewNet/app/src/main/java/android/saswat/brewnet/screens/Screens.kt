package android.saswat.brewnet.screens

sealed class Screens(val route: String) {
    object PhotosScreen : Screens("photos")
    object LocationScreen : Screens("location")
    object ManualLocation : Screens("manual_location")
    object SignInScreen : Screens("signin")
    object SignUpScreen : Screens("signup")
    object FirstScreen : Screens("first")
    object MainScreen : Screens("main")
    object PhoneSignIn : Screens("phoneSignIn")
    object VerifyPhone : Screens("verifyPhone/{phoneNumber}/{verificationId}")
    object VerificationSuccess : Screens("verificationSuccess")
    object CompleteProfile : Screens("completeProfile")
    object AgeSelection : Screens("ageSelection")
    object GenderSelection : Screens("genderSelection")
    object BrewNetPurpose : Screens("brewNetPurpose")
    object ConnectionType : Screens("connectionType")
    object Interests : Screens("interests")
    object Qualities: Screens("qualities")

    // Helper functions
    companion object {
        fun getVerifyPhoneRoute(phoneNumber: String, verificationId: String) = "verifyPhone/$phoneNumber/$verificationId"
    }
}