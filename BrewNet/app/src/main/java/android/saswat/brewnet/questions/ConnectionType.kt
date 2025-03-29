package android.saswat.brewnet.questions

import android.saswat.brewnet.screens.Screens
import android.saswat.state.UpdateState
import android.saswat.viewModel.AuthViewModel
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun ConnectionTypeScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current

    // Show error if exists
    LaunchedEffect(updateState) {
        if (updateState is UpdateState.Error) {
            Toast.makeText(
                context,
                (updateState as UpdateState.Error).message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F9FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "What Type Of\nConnections Are You\nSeeking?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
                color = Color(0xFF1A1C1E)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Option buttons
            ConnectionOption(
                text = "Professional",
                isSelected = selectedOption == "professional",
                onClick = { selectedOption = "professional" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConnectionOption(
                text = "Social",
                isSelected = selectedOption == "social",
                onClick = { selectedOption = "social" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConnectionOption(
                text = "Both equally",
                isSelected = selectedOption == "both",
                onClick = { selectedOption = "both" }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (selectedOption != null) {
                        viewModel.updateUserSeek(selectedOption!!) { success ->
                            if (success) {
                                navController.navigate(Screens.Qualities.route) {
                                    popUpTo(0)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOption != null) Color(0xFF246BFD) else Color(0xFFE0E0E0),
                    contentColor = if (selectedOption != null) Color.White else Color(0xFF999999)
                ),
                enabled = selectedOption != null && updateState !is UpdateState.Loading
            ) {
                if (updateState is UpdateState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ConnectionOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color(0xFF246BFD) else Color(0xFFE0E0E0)
        ),
        color = if (isSelected) Color(0xFFEEF4FF) else Color.White,
        tonalElevation = if (isPressed) 2.dp else 0.dp,
        shadowElevation = if (isPressed) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                color = if (isSelected) Color(0xFF246BFD) else Color(0xFF1A1C1E),
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(if (isSelected) Color(0xFF246BFD) else Color.White, CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) Color(0xFF246BFD) else Color(0xFFE0E0E0),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .scale(
                                animateFloatAsState(
                                    targetValue = if (isSelected) 1f else 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ).value
                            )
                    )
                }
            }
        }
    }
}
@Preview
@Composable
fun PreviewConnectionType() {
    ConnectionTypeScreen(
        navController = rememberNavController(),
    )
}