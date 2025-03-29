package android.saswat.brewnet.screens




import android.saswat.brewnet.R
import android.saswat.state.AuthState
import android.saswat.viewModel.AuthViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun FirstScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),

    onSignInClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {}
) {
    // UI states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val authState by authViewModel.authState.collectAsState()
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF2F6FF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top spacing


            // Logo and text exactly as in reference
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "BrewNet Logo",
                    modifier = Modifier.size(190.dp),

                    )

            }

            Spacer(modifier = Modifier.height(24.dp))

            // Background image - exactly as shown in reference
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = "Crossroad Background",
                modifier = Modifier
                    .fillMaxWidth() // Maintain square aspect ratio
                    .clip(RoundedCornerShape(0.dp)), // No rounding
                contentScale = ContentScale.FillWidth // Fill width without cropping height
            )

            Spacer(modifier = Modifier.height(45.dp))

            // Text exactly as in reference
            Text(
                text = "Connect With People Who\nGet You.",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Button exactly as in reference
            Button(
                onClick = onSignInClick ,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text(
                    "Sign In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Account text exactly as in reference
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account?",
                    color = Color.Gray,
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

            // Bottom spacing
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FirstPreview() {
    FirstScreen(navController = rememberNavController())
}