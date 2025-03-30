package android.saswat.brewnet.chat

import android.saswat.brewnet.R
import android.saswat.viewModel.ChatDetailState
import android.saswat.viewModel.ChatViewModel
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    navController: NavController,
    chatViewModel: ChatViewModel,
    otherUserId: String?,
    currentUserId: String
) {
    Log.d("ChatDetailScreen", "Starting with otherUserId=$otherUserId, currentUserId=$currentUserId")
    
    val chatDetailState by chatViewModel.chatDetailState.collectAsState()
    val chatMessages by chatViewModel.chatMessages.collectAsState()
    val messageText by chatViewModel.messageText.collectAsState()
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var placeholderUser by remember { mutableStateOf(
        ChatUser(
            id = otherUserId ?: "",
            username = "User",
            profileImageUrl = "",
            lastMessage = "",
            unreadCount = 0
        )
    )}
    
    var uiState by remember { mutableStateOf<UiChatState>(UiChatState.Loading) }
    
    DisposableEffect(Unit) {
        onDispose {
            Log.d("ChatDetailScreen", "Cleaning up chat screen")
        }
    }
    
    LaunchedEffect(otherUserId, currentUserId) {
        try {
            Log.d("ChatDetailScreen", "Initializing chat")
            if (otherUserId != null) {
                uiState = UiChatState.Loading
                
                chatViewModel.initChatWithUser(currentUserId, otherUserId)
            }
        } catch (e: Exception) {
            Log.e("ChatDetailScreen", "Error initializing chat", e)
            errorMessage = "Failed to load chat: ${e.message}"
            uiState = UiChatState.Error("Failed to load chat: ${e.message}")
        }
    }
    
    LaunchedEffect(chatDetailState) {
        uiState = when(val state = chatDetailState) {
            is ChatDetailState.Loading -> UiChatState.Loading
            is ChatDetailState.Error -> UiChatState.Error(state.message)
            is ChatDetailState.Success -> {
                placeholderUser = state.otherUser
                UiChatState.Success
            }
        }
    }
    
    LaunchedEffect(chatMessages.size) {
        try {
            if (chatMessages.isNotEmpty()) {
                coroutineScope.launch {
                    listState.animateScrollToItem(chatMessages.size - 1)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatDetailScreen", "Error scrolling to bottom", e)
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                navController = navController,
                chatUser = placeholderUser
            )
        },
        bottomBar = {
            ChatMessageInput(
                value = messageText,
                onValueChange = { 
                    try {
                        chatViewModel.setMessageText(it) 
                    } catch (e: Exception) {
                        Log.e("ChatDetailScreen", "Error setting message text", e)
                    }
                },
                onSendClick = {
                    try { 
                        chatViewModel.sendMessage()
                    } catch (e: Exception) {
                        Log.e("ChatDetailScreen", "Error sending message", e)
                        errorMessage = "Failed to send message: ${e.message}"
                    }
                }
            )
        },
        containerColor = Color(0xFFF5F9FF)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
                return@Box
            }
            
            when (uiState) {
                is UiChatState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFFFF4081)
                    )
                }
                is UiChatState.Success -> {
                    if (chatMessages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No messages yet",
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Start the conversation with ${placeholderUser.username.ifEmpty { "User" }}",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatMessages) { message ->
                                val isCurrentUser = message.senderId == currentUserId
                                
                                MessageBubble(
                                    message = message,
                                    isCurrentUser = isCurrentUser,
                                    otherUserImage = placeholderUser.profileImageUrl
                                )
                            }
                        }
                    }
                }
                is UiChatState.Error -> {
                    Text(
                        text = "Error: ${(uiState as UiChatState.Error).message}",
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    navController: NavController,
    chatUser: ChatUser
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(chatUser.profileImageUrl)
                            .crossfade(true)
                            .error(R.drawable.default_profile)
                            .build(),
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = chatUser.username.ifEmpty { "User" },
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFF5F9FF)
        )
    )
}

sealed class UiChatState {
    object Loading : UiChatState()
    object Success : UiChatState()
    data class Error(val message: String) : UiChatState()
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean,
    otherUserImage: String
) {
    val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val messageTime = message.timestamp?.seconds?.let { seconds ->
        dateFormat.format(Date(seconds * 1000))
    } ?: ""
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Show profile image for received messages
            if (!isCurrentUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(otherUserImage)
                            .crossfade(true)
                            .error(R.drawable.default_profile)
                            .build(),
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Message bubble
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentUser) Color(0xFFFF4081) else Color.White
                ),
                modifier = Modifier.weight(0.8f, fill = false)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = message.content,
                        color = if (isCurrentUser) Color.White else Color.Black,
                        fontSize = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = messageTime,
                        fontSize = 10.sp,
                        color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                        modifier = Modifier.align(if (isCurrentUser) Alignment.End else Alignment.Start)
                    )
                }
            }
            
            if (isCurrentUser) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Type a message") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                shape = RoundedCornerShape(24.dp)
            )
            
            IconButton(
                onClick = onSendClick,
                enabled = value.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (value.isNotBlank()) Color(0xFFFF4081) else Color.LightGray)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}