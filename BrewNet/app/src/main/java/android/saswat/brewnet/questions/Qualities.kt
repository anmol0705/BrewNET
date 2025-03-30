package android.saswat.brewnet.questions

import android.saswat.brewnet.screens.Screens
import android.saswat.state.UpdateState
import android.saswat.viewModel.AuthViewModel
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun QualitiesScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val qualititesMap = remember { mutableStateMapOf<String, Boolean>() }
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current

    // Available interests
    val qualities = listOf(
        "Loyalty", "Open Minded", "Passionate", "Supportive",
        "Compassion", "Empowering", "Independent", "Creative",
        "Balanced", "Confident", "Practical", "Humorous",
        "Dependable", "Curious", "Encouraging", "Playful",
        "Driven", "Kind", "Trustworthy", "Self-Sufficient",
        "Inspiring", "Down-to-Earth", "Energetic", "Enthusiastic",
        "Thoughtful", "Considerate", "Assertive", "Innovative",
        "Spontaneous", "Carefree", "Calm"
    )


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
            // Fixed header section
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Select The Qualities\nYou Value In A\nConnection ...",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                color = Color(0xFF1A1C1E)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Scrollable bubbles section
            Box(
                modifier = Modifier
                    .weight(1f) // Takes remaining space between header and button
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var currentRow = mutableListOf<String>()
                    var currentWidth = 0f
                    val maxWidth = 320f

                    qualities.forEach { qualities ->
                        val itemWidth = (qualities.length * 11 + 44).toFloat()
                        if (currentWidth + itemWidth > maxWidth) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                currentRow.forEach { rowItem ->
                                    QualitiesBubble(
                                        text = rowItem,
                                        isSelected = qualititesMap[rowItem] == true,
                                        onClick = {
                                            qualititesMap[rowItem] = qualititesMap[rowItem] != true
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                            currentRow.clear()
                            currentWidth = itemWidth
                            currentRow.add(qualities)
                        } else {
                            currentRow.add(qualities)
                            currentWidth += itemWidth
                        }
                    }

                    if (currentRow.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            currentRow.forEach { rowItem ->
                                QualitiesBubble(
                                    text = rowItem,
                                    isSelected = qualititesMap[rowItem] == true,
                                    onClick = {
                                        qualititesMap[rowItem] = qualititesMap[rowItem] != true
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.updateUserQualities(qualititesMap.toMap()) { success ->
                        if (success) {
                            navController.navigate(Screens.Interests.route) {
                                popUpTo(0)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (qualititesMap.any { it.value }) Color(0xFF246BFD) else Color(0xFFE0E0E0),
                    contentColor = if (qualititesMap.any { it.value }) Color.White else Color(0xFF999999)
                ),
                enabled = qualititesMap.any { it.value } && updateState !is UpdateState.Loading
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
private fun QualitiesBubble(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Surface(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(100.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(100.dp),
        color = if (isSelected) Color(0xFF246BFD) else Color.White,
        tonalElevation = if (isPressed) 2.dp else 0.dp,
        shadowElevation = if (isPressed) 4.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = if (isSelected) Color.White else Color(0xFF1A1C1E),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Preview
@Composable
fun PreviewInterestBubble() {
    QualitiesScreen(
        navController = rememberNavController(),
    )
}