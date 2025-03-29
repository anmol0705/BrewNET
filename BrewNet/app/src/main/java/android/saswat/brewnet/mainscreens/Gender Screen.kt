package android.saswat.brewnet.mainscreens

import android.saswat.brewnet.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun GenderSelectionScreen(
    navController: NavController,
    onGenderSelected: (String) -> Unit = {}
) {
    var selectedGender by remember { mutableStateOf<String?>(null) }

    val maleColor = Color(0xFF246BFD)
    val femaleColor = Color(0xFFFFB7C5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFF2F6FF))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "What's Your Gender?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tell us about your gender",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(80.dp))

        // Male Selection
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(if (selectedGender == "Male") maleColor else Color(0xFFF5F9FF))
                .clickable { selectedGender = "Male" },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.male),
                    contentDescription = "Male Icon",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Male",
                    color = if (selectedGender == "Male") Color.White else maleColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Female Selection
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(if (selectedGender == "Female") femaleColor else Color(0xFFFFF5F6))
                .clickable { selectedGender = "Female" },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.female),
                    contentDescription = "Female Icon",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Female",
                    color = if (selectedGender == "Female") Color.White else femaleColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Continue Button
        Button(
            onClick = {
                selectedGender?.let { gender ->
                    onGenderSelected(gender)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = maleColor,
                disabledContainerColor = Color(0xFFEEEEEE)
            ),
            enabled = selectedGender != null,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Continue",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = if (selectedGender != null) Color.White else Color.Gray
            )
        }
    }
}

@Preview
@Composable
fun PreviewGenderScreen() {
    GenderSelectionScreen(
        navController = rememberNavController()
    )
}