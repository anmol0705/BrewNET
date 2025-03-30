package android.saswat.brewnet.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null,
    val isRead: Boolean = false,
    val chatId: String = ""
)

data class Chat(
    @DocumentId val id: String = "",
    val participants: List<String> = listOf(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    val lastMessageSenderId: String = "",
    val unreadCount: Map<String, Int> = mapOf()
)

data class ChatUser(
    val id: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null,
    val unreadCount: Int = 0
)