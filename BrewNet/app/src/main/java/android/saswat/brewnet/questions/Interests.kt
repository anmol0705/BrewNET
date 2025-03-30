package android.saswat.brewnet.questions

import android.saswat.brewnet.R
import android.saswat.brewnet.screens.Screens
import android.saswat.state.UpdateState
import android.saswat.viewModel.AuthViewModel
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@Composable
fun InterestsScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val interestsMap = remember { mutableStateMapOf<String, Boolean>() }
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current

    // Available interests with their icons

    val interests = listOf(
        InterestItem("Reading", R.drawable.round_menu_book_24),
        InterestItem("Photography", R.drawable.outline_camera_24),
        InterestItem("Gaming", R.drawable.baseline_sports_esports_24),
        InterestItem("Music", R.drawable.baseline_headphones_24),
        InterestItem("Travel", R.drawable.baseline_travel_explore_24),
        InterestItem("Painting", R.drawable.outline_color_lens_24),
        InterestItem("Politics", R.drawable.baseline_emoji_people_24),
        InterestItem("Charity", R.drawable.baseline_money_24),
        InterestItem("Cooking", R.drawable.baseline_dining_24),
        InterestItem("Pets", R.drawable.baseline_pets_24),
        InterestItem("Sports", R.drawable.baseline_sports_score_24),
        InterestItem("Fashion", R.drawable.baseline_dry_cleaning_24),
        InterestItem("Movies & TV Shows", R.drawable.baseline_movie_24),
        InterestItem("Personal Development", R.drawable.baseline_psychology_24),
        InterestItem("Sustainability", R.drawable.baseline_grass_24),
        InterestItem("Volunteering", R.drawable.baseline_volunteer_activism_24),
        InterestItem("Finance & Investing", R.drawable.baseline_savings_24),
        InterestItem("Science & Technology", R.drawable.baseline_science_24),
        InterestItem("Fitness & Yoga", R.drawable.baseline_fitness_center_24),
        InterestItem("Dance & Performing Arts", R.drawable.baseline_theater_comedy_24),
        InterestItem("History & Culture", R.drawable.baseline_history_edu_24),
        InterestItem("Languages & Linguistics", R.drawable.baseline_translate_24),
        InterestItem("Podcasts & Audiobooks", R.drawable.baseline_podcasts_24),
        InterestItem("Cars & Motorcycles", R.drawable.baseline_directions_car_24),
        InterestItem("DIY & Crafts", R.drawable.baseline_construction_24),
        InterestItem("Health & Nutrition", R.drawable.baseline_health_and_safety_24)
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
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Specific Interests You\nAre Looking For...",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
                color = Color(0xFF1A1C1E)
            )

            Spacer(modifier = Modifier.height(48.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    var currentRow = mutableListOf<InterestItem>()
                    var currentWidth = 0f
                    val maxWidth = 280f

                    interests.forEach { interest ->
                        val itemWidth = (interest.text.length * 10 + 64).toFloat()
                        if (currentWidth + itemWidth > maxWidth) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                currentRow.forEach { rowItem ->
                                    InterestBubble(
                                        text = rowItem.text,
                                        iconResId = rowItem.iconResId,
                                        isSelected = interestsMap[rowItem.text] == true,
                                        onClick = {
                                            interestsMap[rowItem.text] = !(interestsMap[rowItem.text] ?: false)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            currentRow.clear()
                            currentWidth = itemWidth
                            currentRow.add(interest)
                        } else {
                            currentRow.add(interest)
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
                                InterestBubble(
                                    text = rowItem.text,
                                    iconResId = rowItem.iconResId,
                                    isSelected = interestsMap[rowItem.text] == true,
                                    onClick = {
                                        interestsMap[rowItem.text] = !(interestsMap[rowItem.text] ?: false)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.updateUserInterests(interestsMap.toMap()) { success ->
                        if (success) {
                            navController.navigate(Screens.MainScreen.route) {
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
                    containerColor = if (interestsMap.any { it.value }) Color(0xFF246BFD) else Color(0xFFE0E0E0),
                    contentColor = if (interestsMap.any { it.value }) Color.White else Color(0xFF999999)
                ),
                enabled = interestsMap.any { it.value } && updateState !is UpdateState.Loading
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

private data class InterestItem(
    val text: String,
    val iconResId: Int
)

@Composable
private fun InterestBubble(
    text: String,
    iconResId: Int,
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
        shadowElevation = if (isPressed) 4.dp else 0.dp,
        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFEEEEEE)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) Color.White else Color(0xFF246BFD)
            )
            Text(
                text = text,
                color = if (isSelected) Color.White else Color(0xFF1A1C1E),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}
@Preview
@Composable
fun InterestPreview() {
    InterestsScreen(navController = rememberNavController(), viewModel = viewModel())
}