package android.saswat.brewnet.mainscreens

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.saswat.brewnet.screens.Screens
import android.saswat.viewModel.AuthViewModel
import android.saswat.viewModel.LocationState
import android.saswat.viewModel.LocationViewModel
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch



@Composable
fun ManualLocationScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel()
) {
    val context = LocalContext.current
    val locationState by locationViewModel.locationState.collectAsState()
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var cameraPosition by remember { mutableStateOf(LatLng(20.5937, 78.9629)) }
    var dragPosition by remember { mutableStateOf<LatLng?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F9FF))
    ) {
        TextField(
            value = locationViewModel.searchQuery,
            onValueChange = { locationViewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search location or drag the pin", color = Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color(0xFF246BFD),
                unfocusedIndicatorColor = Color(0xFFE0E0E0)
            ),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    if (locationState is LocationState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF246BFD),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (locationViewModel.searchQuery.isNotEmpty()) {
                                    locationViewModel.geocodeAddress(context, locationViewModel.searchQuery)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF246BFD)
                            )
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Search
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = {
                    if (locationViewModel.searchQuery.isNotEmpty()) {
                        locationViewModel.geocodeAddress(context, locationViewModel.searchQuery)
                    }
                }
            )
        )

        if (locationState is LocationState.Error) {
            Text(
                text = (locationState as LocationState.Error).message,
                color = Color.Red,
                modifier = Modifier.padding(horizontal = 16.dp),
                fontSize = 14.sp
            )
        }

        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = "Release to select this location",
                color = Color(0xFF246BFD),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(16.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        GoogleMap(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(
                    when {
                        isDragging && dragPosition != null -> dragPosition!!
                        locationState is LocationState.Success -> (locationState as LocationState.Success).location
                        selectedLocation != null -> selectedLocation!!
                        else -> cameraPosition
                    },
                    15f
                )
            },
            properties = MapProperties(
                isMyLocationEnabled = true,
                mapType = MapType.NORMAL
            ),
            onMapClick = { latLng ->
                selectedLocation = latLng
                locationViewModel.reverseGeocode(context, latLng)
            }
        ) {
            val markerPosition = when {
                isDragging && dragPosition != null -> dragPosition
                selectedLocation != null -> selectedLocation
                locationState is LocationState.Success -> (locationState as LocationState.Success).location
                else -> null
            }

            markerPosition?.let { position ->
                Marker(
                    state = MarkerState(position = position),
                    title = "Selected Location",
                    draggable = true,
                    onClick = {
                        false // Allow the default behavior
                    },
                    onInfoWindowClick = {},
                    onInfoWindowClose = {},
                    onInfoWindowLongClick = {},
                    tag = "draggable-marker"
                )

                // Handle marker drag events using MarkerState
                LaunchedEffect(position) {
                    val markerState = MarkerState(position)
                    snapshotFlow { markerState.dragState }.collect { dragState ->
                        when (dragState) {
                            DragState.START -> isDragging = true
                            DragState.DRAG -> dragPosition = markerState.position
                            DragState.END -> {
                                isDragging = false
                                selectedLocation = markerState.position
                                dragPosition = null
                                locationViewModel.reverseGeocode(context, markerState.position)
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = locationState is LocationState.Success,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            val successState = locationState as? LocationState.Success
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                successState?.let { state ->
                    Text(
                        text = state.address,
                        fontSize = 16.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = Color(0xFF333333)
                    )

                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            state.location.let { location ->
                                authViewModel.updateUserLocation(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    locationName = state.address
                                ) { success ->
                                    if (success) {
                                        navController.navigate(Screens.BrewNetPurpose.route) {
                                            popUpTo(Screens.LocationScreen.route) { inclusive = true }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF246BFD)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Confirm Location",
                            fontSize = 16.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}