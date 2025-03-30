package android.saswat.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserCardData(
    val userId: String = "",
    val username: String = "",
    val dateOfBirth: String = "",
    val profileImageUrl: String = "",
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val interests: Map<String, Boolean> = mapOf(),
    val qualities: Map<String, Boolean> = mapOf(),
    val bio: String? = null
)

sealed class CardState {
    object Loading : CardState()
    data class Success(val users: List<UserCardData>) : CardState()
    data class Error(val message: String) : CardState()
}

class UserCardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val _cardState = MutableStateFlow<CardState>(CardState.Loading)
    val cardState: StateFlow<CardState> = _cardState
    
    private var lastFetchedUser: UserCardData? = null
    private val batchSize = 10

    fun removeTopCard() {
        if (_cardState.value is CardState.Success) {
            val currentUsers = (_cardState.value as CardState.Success).users
            if (currentUsers.isNotEmpty()) {
                val remainingUsers = currentUsers.drop(1)
                _cardState.value = CardState.Success(remainingUsers)
            }
        }
    }
    
    fun fetchNextBatchOfUsers(currentUserId: String) {
        viewModelScope.launch {
            try {
                if (_cardState.value !is CardState.Success || (_cardState.value as? CardState.Success)?.users?.isEmpty() == true) {
                    _cardState.value = CardState.Loading
                }
                
                var query = firestore.collection("users")
                    .whereNotEqualTo("userId", currentUserId)
                    .orderBy("userId", Query.Direction.ASCENDING)
                    .limit(batchSize.toLong())

                lastFetchedUser?.let { lastUser ->
                    query = query.startAfter(lastUser.userId)
                }

                val documents = query.get().await()
                val users = documents.mapNotNull { doc ->
                    try {
                        val userData = doc.toObject(UserData::class.java)
                        userData?.let {
                            Log.d("UserCardViewModel", "Fetched user with image URL: ${it.profileImageUrl}")
                            UserCardData(
                                userId = it.userId,
                                username = it.username,
                                dateOfBirth = it.dateOfBirth,
                                profileImageUrl = it.profileImageUrl ?: "",
                                locationName = it.locationName,
                                latitude = it.latitude,
                                longitude = it.longitude,
                                interests = it.interests,
                                qualities = it.qualities,
                                bio = it.bio
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("UserCardViewModel", "Error mapping user data", e)
                        null
                    }
                }

                if (users.isNotEmpty()) {
                    lastFetchedUser = users.last()
                }

                val currentUsers = if (_cardState.value is CardState.Success) {
                    (_cardState.value as CardState.Success).users
                } else {
                    emptyList()
                }

                _cardState.value = CardState.Success(currentUsers + users)

            } catch (e: Exception) {
                Log.e("UserCardViewModel", "Error fetching users", e)
                _cardState.value = CardState.Error(e.message ?: "Failed to fetch users")
            }
        }
    }

    fun clearCards() {
        lastFetchedUser = null
        _cardState.value = CardState.Loading
    }
}

