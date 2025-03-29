package android.saswat.brewnet.Navigation

import android.saswat.brewnet.screens.FirstScreen
import android.saswat.brewnet.screens.Screens
import android.saswat.brewnet.mainscreens.AgeSelectionScreen
import android.saswat.brewnet.mainscreens.GenderSelectionScreen
import android.saswat.brewnet.mainscreens.PhotosScreen
import android.saswat.brewnet.ui.signInandSignUp.PhoneVerificationScreen
import android.saswat.brewnet.ui.signInandSignUp.SignInScreen
import android.saswat.brewnet.ui.signInandSignUp.SignUpScreen
import android.saswat.brewnet.ui.signInandSignUp.SuccessScreen
import android.saswat.viewModel.AuthViewModel
import android.saswat.viewModel.PhoneAuthViewModel
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun Navigation(navController: NavHostController) {
    // Initialize view models at the navigation level
    val phoneAuthViewModel: PhoneAuthViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screens.FirstScreen.route
    ) {
        composable(route = Screens.FirstScreen.route,

            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }

        ) {

            FirstScreen(
                navController = navController,
                onSignInClick = { navController.navigate(Screens.SignIn.route) },
                onSignUpClick = { navController.navigate(Screens.SignUp.route) },
            )

        }
        composable(route = Screens.SignIn.route,

            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ){
            SignInScreen(
                navController = navController,
                authViewModel = viewModel(),
                onSignUpClick = { navController.navigate(Screens.SignUp.route) },
                onEmailSignInClick = { /* Handle email sign in click */ }
            )
        }

        composable(route = Screens.SignUp.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            SignUpScreen(navController = navController)
        }

        composable(
            route = Screens.VerifyPhone.route,
            arguments = listOf(
                navArgument("phoneNumber") { type = NavType.StringType }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            PhoneVerificationScreen(
                navController = navController,
                phoneAuthViewModel = phoneAuthViewModel,
                phoneNumber = phoneNumber
            )
        }

        composable(
            route = Screens.AgeSelection.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            AgeSelectionScreen(
                navController = navController,
                onAgeSelected = { age ->
                    authViewModel.updateUserData(
                        newUsername = authViewModel.userData.value?.username ?: "",
                        newDateOfBirth = age.toString(),
                        newGender = authViewModel.userData.value?.gender ?: "",

                    ) { success ->
                        if (success) {
                            navController.navigate(Screens.GenderSelection.route) {
                                popUpTo(Screens.SignUp.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(
            route = Screens.GenderSelection.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            GenderSelectionScreen(
                navController = navController,
                onGenderSelected = { gender ->
                    authViewModel.updateUserData(
                        newUsername = authViewModel.userData.value?.username ?: "",
                        newDateOfBirth = authViewModel.userData.value?.dateOfBirth ?: "",
                        newGender = gender,
                    ) { success ->
                        if (success) {
                            navController.navigate(Screens.PhotosScreen.route) {
                                popUpTo(Screens.SignUp.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(
            route = Screens.PhotosScreen.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            PhotosScreen(
                navController = navController,
                onPhotosUploaded = {
                    navController.navigate(Screens.VerificationSuccess.route) {
                        popUpTo(Screens.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screens.VerificationSuccess.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            SuccessScreen(
                navController = navController
            )
        }

        // Add other routes here...
    }
}
