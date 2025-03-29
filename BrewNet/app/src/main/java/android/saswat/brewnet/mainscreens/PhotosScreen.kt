package android.saswat.brewnet.mainscreens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.saswat.brewnet.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch

@Composable
fun PhotosScreen(
    navController: NavController,
    onPhotosUploaded: () -> Unit = {}
) {
    val context = LocalContext.current
    var mainPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var additionalPhotos by remember { mutableStateOf<List<Uri?>>(List(4) { null }) }
    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val mainPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { mainPhotoUri = it }
    }
    
    val additionalPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { newUri ->
            val firstEmptyIndex = additionalPhotos.indexOfFirst { it == null }
            if (firstEmptyIndex != -1) {
                additionalPhotos = additionalPhotos.toMutableList().apply {
                    set(firstEmptyIndex, newUri)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F6FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Upload Your Photo",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "We'd love to see you. Upload a photo for\nyour dating journey.",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(
                modifier = Modifier
                    .size(280.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(
                        color = Color(0xFF246BFD),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        ),
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { mainPhotoLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (mainPhotoUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(mainPhotoUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Main Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Photo",
                            tint = Color(0xFF246BFD),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                additionalPhotos.forEachIndexed { index, uri ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(
                                color = Color(0xFF246BFD),
                                style = Stroke(
                                    width = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                ),
                                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { additionalPhotoLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(uri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Additional Photo $index",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Photo",
                                    tint = Color(0xFF246BFD),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (!isUploading && mainPhotoUri != null) {
                        scope.launch {
                            isUploading = true
                            uploadPhotosToFirebase(
                                mainPhotoUri!!,
                                additionalPhotos.filterNotNull()
                            ) { success ->
                                isUploading = false
                                if (success) {
                                    onPhotosUploaded()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF246BFD),
                    disabledContainerColor = Color(0xFF2A2A2A)
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = mainPhotoUri != null && !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Continue",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (mainPhotoUri != null) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}

private fun uploadPhotosToFirebase(
    mainPhoto: Uri,
    additionalPhotos: List<Uri>,
    onComplete: (Boolean) -> Unit
) {
    val storage = FirebaseStorage.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val mainPhotoRef = storage.reference.child("users/$userId/photos/main.jpg")
    
    mainPhotoRef.putFile(mainPhoto)
        .addOnSuccessListener {
            var uploadedCount = 0
            val totalAdditionalPhotos = additionalPhotos.size
            
            if (totalAdditionalPhotos == 0) {
                onComplete(true)
                return@addOnSuccessListener
            }

            additionalPhotos.forEachIndexed { index, uri ->
                val photoRef = storage.reference.child("users/$userId/photos/additional_$index.jpg")
                photoRef.putFile(uri)
                    .addOnSuccessListener {
                        uploadedCount++
                        if (uploadedCount == totalAdditionalPhotos) {
                            onComplete(true)
                        }
                    }
                    .addOnFailureListener {
                        onComplete(false)
                    }
            }
        }
        .addOnFailureListener {
            onComplete(false)
        }
}