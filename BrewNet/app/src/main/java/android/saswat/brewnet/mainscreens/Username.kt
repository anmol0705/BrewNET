package android.saswat.brewnet.mainscreens

import android.saswat.viewModel.AuthViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.saswat.brewnet.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun UserNameScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    onNavigateNext: () -> Unit
) {
    val backgroundColor = Color(0xFFF5F9FF)
    val buttonColor = Color(0xFF1E6AE1)
    
    var username by remember { mutableStateOf("") }
    val userData by authViewModel.userData.collectAsState()
    var bio by remember { mutableStateOf("") }
    
    // Pre-fill the username field if available
    val initialUsername = userData?.username ?: ""
    if (username.isEmpty() && initialUsername.isNotEmpty()) {
        username = initialUsername
    }
    val initialBio=userData?.bio?:""
    if (bio.isEmpty() && initialBio.isNotEmpty()) {
        bio = initialBio
    }

    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        // Content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            // Title text
            Text(
                text = "What's Your Name?",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle text
            Text(
                text = "Let's Get to Know Each Other",
                fontSize = 16.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Username input field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                placeholder = { Text("Enter your name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = buttonColor,
                    focusedTextColor = Color.Black
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                placeholder = { Text("Enter your bio") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = buttonColor,
                    focusedTextColor = Color.Black
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            // Continue button
            Button(
                onClick = {
                    if (username.isNotBlank()) {
                        // Update username in Firestore
                        authViewModel.updateUserData(
                            newUsername = username,
                            newDateOfBirth = userData?.dateOfBirth ?: "",
                            newGender = userData?.gender ?: "",
                            newGenderSubcategory = "",
                            newBio = userData?.bio ?: ""
                        )
                        onNavigateNext()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                enabled = username.isNotBlank()
            ) {
                Text(
                    text = "Continue", 
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Bottom vector image
        Image(
            painter = painterResource(id = R.drawable.vector),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomEnd,
        )
    }
}
@Preview
@Composable
fun UsernamePreview() {
    UserNameScreen(navController = rememberNavController(), authViewModel = AuthViewModel(), onNavigateNext = {})
}