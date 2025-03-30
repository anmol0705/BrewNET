package android.saswat.brewnet.chat

import android.saswat.brewnet.R
import android.saswat.brewnet.screens.Screens
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * A simplified chat screen that directly uses Firebase without complex architecture
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDirectChatScreen(
    navController: NavController,
    otherUserId: String?,
    currentUserId: String
) {
    Log.d("SimpleChat", "Starting with otherUserId=$otherUserId, currentUserId=$currentUserId")
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Keep values that need to be accessed in DisposableEffect
    val otherUserIdState = rememberUpdatedState(otherUserId)
    val currentUserIdState = rememberUpdatedState(currentUserId)
    
    // Local state
    var messageText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var otherUsername by remember { mutableStateOf("Chat") }
    val messages = remember { mutableStateListOf<SimpleMessage>() }
    
    // Firebase instance
    val firestore = remember { FirebaseFirestore.getInstance() }
    var chatId by remember { mutableStateOf<String?>(null) }
    var messageListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    
    // Setup and cleanup
    DisposableEffect(currentUserIdState.value, otherUserIdState.value) {
        Log.d("SimpleChat", "Setting up Firebase listeners")
        isLoading = true
        
        // Function to create or get chat ID
        fun getOrCreateChat(currentUserId: String, otherUserId: String, onChatCreated: (String) -> Unit) {
            Log.d("SimpleChat", "Trying to get or create chat")
            // Try to find existing chat
            firestore.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    Log.d("SimpleChat", "Got ${querySnapshot.size()} chats")
                    // Find a chat that contains both users
                    val existingChat = querySnapshot.documents.find { doc ->
                        val participants = doc["participants"] as? List<String>
                        participants?.containsAll(listOf(currentUserId, otherUserId)) == true
                    }
                    
                    if (existingChat != null) {
                        Log.d("SimpleChat", "Found existing chat: ${existingChat.id}")
                        onChatCreated(existingChat.id)
                    } else {
                        // Create new chat
                        val newChatId = UUID.randomUUID().toString()
                        val chatData = hashMapOf(
                            "id" to newChatId,
                            "participants" to listOf(currentUserId, otherUserId),
                            "lastMessage" to "",
                            "lastMessageTimestamp" to Timestamp.now(),
                            "unreadCount" to mapOf(
                                currentUserId to 0,
                                otherUserId to 0
                            )
                        )
                        
                        firestore.collection("chats").document(newChatId)
                            .set(chatData)
                            .addOnSuccessListener {
                                Log.d("SimpleChat", "Created new chat: $newChatId")
                                onChatCreated(newChatId)
                            }
                            .addOnFailureListener { e ->
                                Log.e("SimpleChat", "Error creating chat", e)
                                isLoading = false
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("SimpleChat", "Error finding chats", e)
                    isLoading = false
                }
        }
        
        // Function to listen for messages
        fun setupMessageListener(chatId: String) {
            Log.d("SimpleChat", "Setting up message listener for $chatId")
            val listener = firestore.collection("messages")
                .whereEqualTo("chatId", chatId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("SimpleChat", "Error listening for messages", error)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val newMessages = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.id
                                val senderId = doc.getString("senderId") ?: return@mapNotNull null
                                val content = doc.getString("content") ?: return@mapNotNull null
                                val timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                                
                                SimpleMessage(id, senderId, content, timestamp)
                            } catch (e: Exception) {
                                Log.e("SimpleChat", "Error parsing message", e)
                                null
                            }
                        }
                        
                        Log.d("SimpleChat", "Got ${newMessages.size} messages")
                        messages.clear()
                        messages.addAll(newMessages)
                        
                        // Scroll to bottom when new messages arrive
                        coroutineScope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }
                    
                    isLoading = false
                }
            
            messageListener = listener
        }
        
        // Function to get other user info
        fun getOtherUserInfo(otherUserId: String) {
            Log.d("SimpleChat", "Getting user info for $otherUserId")
            firestore.collection("users").document(otherUserId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        otherUsername = doc.getString("username") ?: "User"
                        Log.d("SimpleChat", "Got username: $otherUsername")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("SimpleChat", "Error getting user info", e)
                }
        }
        
        // Initialize
        val actualOtherId = otherUserIdState.value
        val actualCurrentId = currentUserIdState.value
        if (actualOtherId != null && actualCurrentId.isNotEmpty()) {
            // Try to get other user info in parallel
            getOtherUserInfo(actualOtherId)
            
            // Get or create chat and listen for messages
            getOrCreateChat(actualCurrentId, actualOtherId) { createdChatId ->
                chatId = createdChatId
                setupMessageListener(createdChatId)
            }
        } else {
            isLoading = false
        }
        
        onDispose {
            Log.d("SimpleChat", "Cleaning up Firebase listeners")
            messageListener?.remove()
        }
    }
    
    // Function to send a message
    fun sendMessage() {
        val text = messageText.trim()
        if (text.isBlank() || chatId == null) return
        
        val messageId = UUID.randomUUID().toString()
        val message = hashMapOf(
            "id" to messageId,
            "chatId" to chatId!!,
            "senderId" to currentUserId,
            "receiverId" to (otherUserId ?: ""),
            "content" to text,
            "timestamp" to Timestamp.now(),
            "isRead" to false
        )
        
        firestore.collection("messages").document(messageId)
            .set(message)
            .addOnSuccessListener {
                Log.d("SimpleChat", "Message sent")
                messageText = ""
                
                // Update the chat with latest message info
                val chatUpdate = hashMapOf<String, Any>(
                    "lastMessage" to text,
                    "lastMessageTimestamp" to Timestamp.now(),
                    "lastMessageSenderId" to currentUserId
                )
                
                firestore.collection("chats").document(chatId!!)
                    .update(chatUpdate)
                    .addOnFailureListener { e ->
                        Log.e("SimpleChat", "Error updating chat", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("SimpleChat", "Error sending message", e)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.default_profile),
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = otherUsername,
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
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    
                    IconButton(
                        onClick = { sendMessage() },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (messageText.isNotBlank()) Color(0xFFFF4081) else Color.LightGray)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF5F9FF)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFFFF4081)
                )
            } else if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No messages yet",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start the conversation",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        val isCurrentUser = message.senderId == currentUserId
                        
                        MessageBubble(
                            message = message,
                            isCurrentUser = isCurrentUser
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

data class SimpleMessage(
    val id: String,
    val senderId: String,
    val content: String,
    val timestamp: Timestamp
)

@Composable
fun MessageBubble(
    message: SimpleMessage,
    isCurrentUser: Boolean
) {
    val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val messageTime = message.timestamp.seconds.let { seconds ->
        dateFormat.format(Date(seconds * 1000))
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
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
            modifier = Modifier.fillMaxWidth(0.75f)
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
    }
}