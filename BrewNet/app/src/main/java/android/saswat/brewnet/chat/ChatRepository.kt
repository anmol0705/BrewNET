package android.saswat.brewnet.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val chatsCollection = db.collection("chats")
    private val messagesCollection = db.collection("messages")
    private val usersCollection = db.collection("users")

    // Create or get existing chat between two users
    suspend fun getOrCreateChat(currentUserId: String, otherUserId: String): Result<String> {
        return try {
            // Try to find existing chat with these participants
            val query = chatsCollection
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()
            
            val existingChat = query.documents.find { doc ->
                val participants = doc["participants"] as? List<String>
                participants?.containsAll(listOf(currentUserId, otherUserId)) == true
            }
            
            if (existingChat != null) {
                Result.success(existingChat.id)
            } else {
                // Create new chat if none exists
                val newChatId = UUID.randomUUID().toString()
                val chat = Chat(
                    id = newChatId,
                    participants = listOf(currentUserId, otherUserId),
                    lastMessage = "",
                    unreadCount = mapOf(currentUserId to 0, otherUserId to 0)
                )
                
                chatsCollection.document(newChatId).set(chat).await()
                Result.success(newChatId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Send a message
    suspend fun sendMessage(message: ChatMessage): Result<Unit> {
        return try {
            // Add message to the messages collection
            val messageId = messagesCollection.document().id
            val messageWithId = message.copy(id = messageId)
            messagesCollection.document(messageId).set(messageWithId).await()
            
            // Update the chat with latest message info
            val chatUpdate = hashMapOf<String, Any>(
                "lastMessage" to message.content,
                "lastMessageTimestamp" to (message.timestamp ?: Timestamp.now()),
                "lastMessageSenderId" to message.senderId
            )
            
            // Update unread count for the receiver
            val unreadCountPath = "unreadCount.${message.receiverId}"
            val chatRef = chatsCollection.document(message.chatId)
            val chat = chatRef.get().await()
            val currentUnreadCount = (chat.get("unreadCount") as? Map<*, *>)?.get(message.receiverId) as? Long ?: 0
            
            chatUpdate[unreadCountPath] = currentUnreadCount + 1
            
            chatRef.set(chatUpdate, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mark all messages as read
    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        return try {
            // Reset unread count for this user
            val update = hashMapOf<String, Any>(
                "unreadCount.$userId" to 0
            )
            chatsCollection.document(chatId).set(update, SetOptions.merge()).await()
            
            // Mark all messages as read that are addressed to this user
            val unreadMessages = messagesCollection
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            for (doc in unreadMessages.documents) {
                messagesCollection.document(doc.id).update("isRead", true).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Listen for new messages in a chat
    fun getChatMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = messagesCollection
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                    trySend(messages).isSuccess
                }
            }
        
        awaitClose { listener.remove() }
    }

    // Get all user chats
    fun getUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = chatsCollection
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { it.toObject(Chat::class.java) }
                    trySend(chats).isSuccess
                }
            }
        
        awaitClose { listener.remove() }
    }
    
    // Get chat user info for UI
    suspend fun getChatUserInfo(chatId: String, currentUserId: String): Result<ChatUser> {
        return try {
            // Get the chat
            val chatDocument = chatsCollection.document(chatId).get().await()
            if (!chatDocument.exists()) {
                return Result.failure(Exception("Chat not found"))
            }
            
            val chat = chatDocument.toObject(Chat::class.java)
                ?: return Result.failure(Exception("Chat not found"))
                
            // Find the other user's ID
            val participants = chatDocument.get("participants") as? List<String> 
                ?: return Result.failure(Exception("Invalid participants data"))
                
            val otherUserId = participants.find { it != currentUserId }
                ?: return Result.failure(Exception("Other user not found in chat"))
                
            // Get other user's profile
            val userDoc = usersCollection.document(otherUserId).get().await()
            
            if (!userDoc.exists()) {
                // If user document doesn't exist, create a basic user with just the ID
                return Result.success(
                    ChatUser(
                        id = otherUserId,
                        username = "User",
                        profileImageUrl = "",
                        lastMessage = chat.lastMessage,
                        lastMessageTime = chat.lastMessageTimestamp,
                        unreadCount = (chat.unreadCount[currentUserId] ?: 0).toInt()
                    )
                )
            }
            
            // Create the ChatUser object with relevant info
            val chatUser = ChatUser(
                id = otherUserId,
                username = userDoc.getString("username") ?: "User",
                profileImageUrl = userDoc.getString("profileImageUrl") ?: "",
                lastMessage = chat.lastMessage,
                lastMessageTime = chat.lastMessageTimestamp,
                unreadCount = (chat.unreadCount[currentUserId] ?: 0).toInt()
            )
            
            Result.success(chatUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get all chat users for the chat list
    suspend fun getAllChatUsers(userId: String): Result<List<ChatUser>> {
        return try {
            val chatDocs = chatsCollection
                .whereArrayContains("participants", userId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val chatUsers = mutableListOf<ChatUser>()
            
            for (chatDoc in chatDocs.documents) {
                val chat = chatDoc.toObject(Chat::class.java) ?: continue
                val otherUserId = chat.participants.find { it != userId } ?: continue
                
                val userDoc = usersCollection.document(otherUserId).get().await()
                if (!userDoc.exists()) continue
                
                val chatUser = ChatUser(
                    id = otherUserId,
                    username = userDoc.getString("username") ?: "User",
                    profileImageUrl = userDoc.getString("profileImageUrl") ?: "",
                    lastMessage = chat.lastMessage,
                    lastMessageTime = chat.lastMessageTimestamp,
                    unreadCount = (chat.unreadCount[userId] ?: 0).toInt()
                )
                
                chatUsers.add(chatUser)
            }
            
            Result.success(chatUsers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get a chat document directly
    suspend fun getChatDocument(chatId: String) = chatsCollection.document(chatId).get().await()
}