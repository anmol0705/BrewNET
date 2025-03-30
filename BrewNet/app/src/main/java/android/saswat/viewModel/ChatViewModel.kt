package android.saswat.viewModel

import android.saswat.brewnet.chat.Chat
import android.saswat.brewnet.chat.ChatMessage
import android.saswat.brewnet.chat.ChatRepository
import android.saswat.brewnet.chat.ChatUser
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ChatState {
    object Loading : ChatState()
    data class Success(val chatUsers: List<ChatUser>) : ChatState()
    data class Error(val message: String) : ChatState()
}

sealed class ChatDetailState {
    object Loading : ChatDetailState()
    data class Success(val messages: List<ChatMessage>, val otherUser: ChatUser) : ChatDetailState()
    data class Error(val message: String) : ChatDetailState()
}

class ChatViewModel : ViewModel() {
    private val TAG = "ChatViewModel"
    private val repository = ChatRepository()
    private val _chatState = MutableStateFlow<ChatState>(ChatState.Loading)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()
    
    private val _selectedChatId = MutableStateFlow<String?>(null)
    private val _currentUserId = MutableStateFlow<String?>(null)
    private val _chatDetailState = MutableStateFlow<ChatDetailState>(ChatDetailState.Loading)
    val chatDetailState: StateFlow<ChatDetailState> = _chatDetailState.asStateFlow()
    
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val chatMessages: StateFlow<List<ChatMessage>> = _selectedChatId
        .flatMapLatest { chatId ->
            if (chatId == null) emptyFlow() else repository.getChatMessages(chatId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    fun loadChatList(userId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Loading chat list for user: $userId")
            _currentUserId.value = userId
            _chatState.value = ChatState.Loading
            
            try {
                val result = repository.getAllChatUsers(userId)
                if (result.isSuccess) {
                    val chatUsers = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "Chat users loaded: ${chatUsers.size}")
                    _chatState.value = ChatState.Success(chatUsers)
                } else {
                    Log.e(TAG, "Failed to load chat users", result.exceptionOrNull())
                    _chatState.value = ChatState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chat list", e)
                _chatState.value = ChatState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun selectChat(chatId: String, otherUserId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Selecting chat: $chatId with user: $otherUserId")
            _selectedChatId.value = chatId
            _chatDetailState.value = ChatDetailState.Loading
            
            // Mark messages as read when opening chat
            val currentUserId = _currentUserId.value
            if (currentUserId == null) {
                Log.e(TAG, "Current user ID is null")
                _chatDetailState.value = ChatDetailState.Error("User not authenticated")
                return@launch
            }
            
            try {
                Log.d(TAG, "Marking messages as read")
                repository.markMessagesAsRead(chatId, currentUserId)
                
                // Get other user info for the chat header
                Log.d(TAG, "Getting chat user info")
                val userResult = repository.getChatUserInfo(chatId, currentUserId)
                if (userResult.isSuccess) {
                    val chatUser = userResult.getOrNull()
                    if (chatUser == null) {
                        Log.e(TAG, "Chat user is null")
                        _chatDetailState.value = ChatDetailState.Error("Failed to get user info")
                        return@launch
                    }
                    
                    Log.d(TAG, "Chat user loaded: ${chatUser.username}")
                    _chatDetailState.value = ChatDetailState.Success(emptyList(), chatUser)
                } else {
                    Log.e(TAG, "Failed to get chat user info", userResult.exceptionOrNull())
                    _chatDetailState.value = ChatDetailState.Error("Failed to load chat: ${userResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in selectChat", e)
                _chatDetailState.value = ChatDetailState.Error("Error: ${e.message}")
            }
        }
    }
    
    fun setMessageText(text: String) {
        _messageText.value = text
    }
    
    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isBlank()) return
        
        viewModelScope.launch {
            val chatId = _selectedChatId.value
            if (chatId == null) {
                Log.e(TAG, "Cannot send message: chat ID is null")
                return@launch
            }
            
            val currentUserId = _currentUserId.value
            if (currentUserId == null) {
                Log.e(TAG, "Cannot send message: current user ID is null")
                return@launch
            }
            
            try {
                // Create a simple message directly since we know the chat ID and participants
                val otherUserId = try {
                    val chatDoc = repository.getChatDocument(chatId)
                    val participants = chatDoc.get("participants") as? List<String>
                    participants?.find { it != currentUserId } ?: throw Exception("Failed to find other user")
                } catch (e: Exception) {
                    // If we can't determine the other user, fall back to what we know
                    Log.e(TAG, "Error determining receiver, using stored state", e)
                    
                    // Use the ChatDetailState's user if available
                    val state = _chatDetailState.value
                    if (state is ChatDetailState.Success) {
                        state.otherUser.id
                    } else {
                        Log.e(TAG, "No chat detail state available")
                        return@launch
                    }
                }
                
                Log.d(TAG, "Sending message to user: $otherUserId")
                val message = ChatMessage(
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    content = text,
                    timestamp = Timestamp.now(),
                    isRead = false,
                    chatId = chatId
                )
                
                repository.sendMessage(message)
                _messageText.value = ""
                Log.d(TAG, "Message sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
            }
        }
    }
    
    fun createChat(currentUserId: String, otherUserId: String, onChatCreated: (String) -> Unit) {
        viewModelScope.launch {
            Log.d(TAG, "Creating chat between $currentUserId and $otherUserId")
            try {
                val result = repository.getOrCreateChat(currentUserId, otherUserId)
                if (result.isSuccess) {
                    val chatId = result.getOrNull()
                    if (chatId != null) {
                        Log.d(TAG, "Chat created with ID: $chatId")
                        onChatCreated(chatId)
                    } else {
                        Log.e(TAG, "Created chat ID is null")
                    }
                } else {
                    Log.e(TAG, "Failed to create chat", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating chat", e)
            }
        }
    }
    
    fun initChatWithUser(currentUserId: String, otherUserId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Initializing chat with user: currentUserId=$currentUserId, otherUserId=$otherUserId")
            _currentUserId.value = currentUserId
            _chatDetailState.value = ChatDetailState.Loading
            
            // Start with default user
            val defaultUser = ChatUser(
                id = otherUserId,
                username = "User",
                profileImageUrl = "",
                lastMessage = "",
                unreadCount = 0
            )
            
            // First just create the chat in the database
            try {
                Log.d(TAG, "Creating or getting chat")
                repository.getOrCreateChat(currentUserId, otherUserId)
                    .onSuccess { chatId ->
                        Log.d(TAG, "Chat created/found with ID: $chatId")
                        _selectedChatId.value = chatId
                        
                        // Immediately show default user while loading
                        _chatDetailState.value = ChatDetailState.Success(emptyList(), defaultUser)
                        
                        // Now try to get the real user info in the background
                        viewModelScope.launch {
                            try {
                                Log.d(TAG, "Fetching user info for chat")
                                repository.getChatUserInfo(chatId, currentUserId)
                                    .onSuccess { chatUser ->
                                        if (chatUser != null) {
                                            Log.d(TAG, "User info fetched: ${chatUser.username}")
                                            _chatDetailState.value = ChatDetailState.Success(emptyList(), chatUser)
                                        }
                                    }
                                    .onFailure { error ->
                                        Log.e(TAG, "Failed to get user info, keeping default user", error)
                                    }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting user info, keeping default user", e)
                            }
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to create chat", error)
                        _chatDetailState.value = ChatDetailState.Success(emptyList(), defaultUser)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in initChatWithUser, using default user", e)
                _chatDetailState.value = ChatDetailState.Success(emptyList(), defaultUser)
            }
        }
    }
}