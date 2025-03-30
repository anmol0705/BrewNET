package android.saswat.brewnet.mainscreens

import android.saswat.brewnet.R
import android.saswat.brewnet.screens.Screens
import android.saswat.viewModel.AuthViewModel
import android.saswat.viewModel.LocationViewModel
import android.saswat.viewModel.UserCardViewModel
import android.saswat.viewModel.CardState
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    locationViewModel: LocationViewModel,
    userCardViewModel: UserCardViewModel
) {
    val userData by authViewModel.userData.collectAsState()
    val locationState by locationViewModel.locationState.collectAsState()
    val cardState by userCardViewModel.cardState.collectAsState()
    val context = LocalContext.current
    
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showActionButtons by remember { mutableStateOf(true) }
    
    val rotation by animateFloatAsState(targetValue = (offsetY / 50f))
    val scale by animateFloatAsState(targetValue = 1f - abs(offsetY) / 1000f)

    // Effect to load initial batch of users
    LaunchedEffect(Unit) {
        userData?.userId?.let { currentUserId ->
            userCardViewModel.fetchNextBatchOfUsers(currentUserId)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F9FF),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = { /* TODO: Implement filter */ },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Menu,
                            "Filter",
                            tint = Color.Blue
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* TODO: Implement notifications */ },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            "Notifications",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F9FF)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                modifier = Modifier.height(100.dp)
            ) {
                val items = listOf(
                    "Card" to painterResource(R.drawable.baseline_view_carousel_24),
                    "Explore" to Icons.Outlined.Search,
                    "Chat" to Icons.Outlined.Email,
                    "Profile" to Icons.Outlined.Person
                )
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "card"

                items.forEach { (label, icon) ->
                    NavigationBarItem(
                        icon = { 
                            when (icon) {
                                is androidx.compose.ui.graphics.painter.Painter -> Icon(
                                    painter = icon,
                                    contentDescription = label,
                                    tint = if (currentRoute == label.lowercase()) Color(0xFFFF4081) else Color.Gray
                                )
                                is androidx.compose.ui.graphics.vector.ImageVector -> Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (currentRoute == label.lowercase()) Color(0xFFFF4081) else Color.Gray
                                )
                            }
                        },
                        label = {
                            Text(
                                label,
                                color = if (currentRoute == label.lowercase()) Color(0xFFFF4081) else Color.Gray
                            )
                        },
                        selected = currentRoute == label.lowercase(),
                        onClick = { 
                            if (currentRoute != label.lowercase()) {
                                if (label == "Chat") {
                                    navController.navigate(Screens.Chat.route) {
                                        popUpTo(Screens.MainScreen.route)
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(label.lowercase()) {
                                        popUpTo(Screens.MainScreen.route)
                                        launchSingleTop = true
                                    }
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF4081),
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F9FF))
        ) {
            when (val state = cardState) {
                is CardState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Black
                    )
                }
                is CardState.Error -> {
                    Text(
                        text = state.message,
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                is CardState.Success -> {
                    if (state.users.isEmpty()) {
                        Text(
                            text = "No more users to display",
                            color = Color.Black,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    } else {
                        // Display the first user in the list
                        val currentUser = state.users.first()
                        
                        // Main card content with improved spacing
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.8f)
                                .align(Alignment.TopCenter)
                                .padding(top = 20.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            // Start showing action buttons if hidden
                                            if (!showActionButtons) {
                                                showActionButtons = true
                                            }
                                        },
                                        onDragEnd = {
                                            if (abs(offsetY) > 300f) {
                                                if (offsetY > 0) {
                                                    // Swipe down - reject and hide action buttons
                                                    showActionButtons = false
                                                    userCardViewModel.removeTopCard()
                                                    if (state.users.size <= 3) {
                                                        // Fetch more users when we're running low
                                                        userData?.userId?.let { currentUserId ->
                                                            userCardViewModel.fetchNextBatchOfUsers(currentUserId)
                                                        }
                                                    }
                                                } else {
                                                    // Swipe up - show actions
                                                    showActionButtons = true
                                                }
                                            }
                                            offsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            offsetY += dragAmount.y
                                            // Hide action buttons when swiping down
                                            if (dragAmount.y > 0 && showActionButtons && offsetY > 100f) {
                                                showActionButtons = false
                                            }
                                        }
                                    )
                                }
                                .graphicsLayer {
                                    translationY = offsetY
                                    rotationZ = rotation
                                    scaleX = scale
                                    scaleY = scale
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Force log to understand what's happening with the image URL
                                Log.d("MainScreen", "Current user profile image URL: ${currentUser.profileImageUrl}")
                                
                                // Profile image with proper loading
                                if (currentUser.profileImageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(currentUser.profileImageUrl)
                                            .crossfade(true)
                                            .placeholder(R.drawable.default_profile)
                                            .error(R.drawable.default_profile)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .build(),
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        onLoading = {
                                            Log.d("MainScreen", "Loading image: ${currentUser.profileImageUrl}")
                                        },
                                        onError = {
                                            Log.e("MainScreen", "Error loading image: ${currentUser.profileImageUrl}")
                                        },
                                        onSuccess = {
                                            Log.d("MainScreen", "Successfully loaded image: ${currentUser.profileImageUrl}")
                                        }
                                    )
                                } else {
                                    // Fallback if no image URL
                                    Image(
                                        painter = painterResource(id = R.drawable.default_profile),
                                        contentDescription = "Default Profile",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                
                                // User info overlay gradient - improved gradient
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Transparent,
                                                    Color.Transparent,
                                                    Color(0x80000000),
                                                    Color(0xCC000000)
                                                ),
                                                startY = 0f,
                                                endY = 1000f
                                            )
                                        )
                                )
                                
                                // Bottom content
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp)
                                ) {
                                    // User info with better formatting
                                    Text(
                                        text = "${currentUser.username}, ${
                                            currentUser.dateOfBirth.substringAfterLast('/') ?: "?"
                                        }",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Show distance from logged-in user
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val distance = calculateDistance(
                                            userData?.latitude,
                                            userData?.longitude,
                                            currentUser.latitude,
                                            currentUser.longitude
                                        )
                                        
                                        Text(
                                            text = "${currentUser.locationName} • ${distance} KMS AWAY",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = currentUser.bio ?: "I am why not ??",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                        
                        // Action buttons that only appear when showActionButtons is true
                        AnimatedVisibility(
                            visible = showActionButtons,
                            enter = fadeIn() + slideInVertically { it },
                            exit = fadeOut() + slideOutVertically { it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 90.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // Chat button (left circle)
                                FloatingActionButton(
                                    onClick = { 
                                        // Create a chat with the current visible user
                                        val currentUser = state.users.firstOrNull() ?: return@FloatingActionButton
                                        userData?.userId?.let { currentUserId ->
                                            try {
                                                android.util.Log.d("MainScreen", "Trying to navigate to simple chat")
                                                
                                                // Try to navigate with minimal approach
                                                val chatRoute = Screens.getSimpleChatRoute(currentUser.userId)
                                                navController.navigate(chatRoute)
                                            } catch (e: Exception) {
                                                android.util.Log.e("MainScreen", "Failed to navigate to chat", e)
                                            }
                                        }
                                    },
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF555555),
                                    shape = CircleShape,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Email, 
                                        contentDescription = "Chat",
                                        tint = Color(0xFF555555)
                                    )
                                }
                                
                                // Like button (right circle - blue)
                                FloatingActionButton(
                                    onClick = { /* TODO: Implement like */ },
                                    containerColor = Color(0xFF2196F3),
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Favorite, 
                                        contentDescription = "Like",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun toRadians(deg: Double): Double = deg * (kotlin.math.PI / 180.0)

private fun calculateDistance(
    userLat1: Double?,
    userLon1: Double?,
    cardLat2: Double?,
    cardLon2: Double?
): String {
    if (userLat1 == null || userLon1 == null || cardLat2 == null || cardLon2 == null) {
        return "?"
    }

    val r = 6371 // Earth's radius in kilometers

    val lat1 = toRadians(userLat1)
    val lon1 = toRadians(userLon1)
    val lat2 = toRadians(cardLat2)
    val lon2 = toRadians(cardLon2)

    val dlon = lon2 - lon1
    val dlat = lat2 - lat1

    val a = kotlin.math.sin(dlat / 2) * kotlin.math.sin(dlat / 2) + 
            kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * 
            kotlin.math.sin(dlon / 2) * kotlin.math.sin(dlon / 2)
    val c = 2 * kotlin.math.asin(kotlin.math.sqrt(a))
    val distance = r * c

    return distance.toInt().toString()
}

@Preview
@Composable
fun PreviewComposable() {
    MainScreen(navController = rememberNavController(), authViewModel = viewModel(), locationViewModel = viewModel(), userCardViewModel = viewModel())
}