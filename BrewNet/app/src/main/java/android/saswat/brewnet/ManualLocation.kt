package android.saswat.brewnet.mainscreens
//
//import android.content.Context
//import android.location.Address
//import android.location.Geocoder
//import android.util.Log
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.weight
//import androidx.compose.material.Button
//import androidx.compose.material.ButtonDefaults
//import androidx.compose.material.Icon
//import androidx.compose.material.IconButton
//import androidx.compose.material.OutlinedTextField
//import androidx.compose.material.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import androidx.navigation.NavController
//import com.google.android.gms.maps.CameraPosition
//import com.google.android.gms.maps.GoogleMap
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.maps.model.MapProperties
//import com.google.android.gms.maps.model.Marker
//import com.google.android.gms.maps.model.MarkerState
//import com.google.maps.android.compose.CameraPositionState
//import com.google.maps.android.compose.rememberCameraPositionState
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//
//sealed class LocationState {
//    object Initial : LocationState()
//    object Loading : LocationState()
//    data class Success(
//        val location: LatLng,
//        val address: String
//    ) : LocationState()
//    data class Error(val message: String) : LocationState()
//}
//
//class LocationViewModel : ViewModel() {
//    private val _locationState = MutableStateFlow<LocationState>(LocationState.Initial)
//    val locationState: StateFlow<LocationState> = _locationState
//
//    var searchQuery by mutableStateOf("")
//        private set
//
//    fun updateSearchQuery(query: String) {
//        searchQuery = query
//    }
//
//    fun geocodeAddress(context: Context, address: String) {
//        viewModelScope.launch {
//            try {
//                _locationState.value = LocationState.Loading
//
//                val geocoder = Geocoder(context, java.util.Locale.getDefault())
//                val addresses: List<Address>? = geocoder.getFromLocationName(address, 1)
//
//                if (addresses.isNullOrEmpty()) {
//                    _locationState.value = LocationState.Error("Location not found")
//                    return@launch
//                }
//
//                val location = addresses[0]
//                val latLng = LatLng(location.latitude, location.longitude)
//                val formattedAddress = getFormattedAddress(location)
//
//                _locationState.value = LocationState.Success(latLng, formattedAddress)
//            } catch (e: java.io.IOException) {
//                _locationState.value = LocationState.Error("Error finding location")
//            }
//        }
//    }
//
//    fun reverseGeocode(context: Context, latLng: LatLng) {
//        viewModelScope.launch {
//            try {
//                _locationState.value = LocationState.Loading
//
//                val geocoder = Geocoder(context, java.util.Locale.getDefault())
//                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
//
//                if (addresses.isNullOrEmpty()) {
//                    _locationState.value = LocationState.Error("Address not found")
//                    return@launch
//                }
//
//                val address = addresses[0]
//                val formattedAddress = getFormattedAddress(address)
//
//                _locationState.value = LocationState.Success(latLng, formattedAddress)
//            } catch (e: java.io.IOException) {
//                _locationState.value = LocationState.Error("Error finding address")
//            }
//        }
//    }
//
//    private fun getFormattedAddress(address: Address): String {
//        val parts = mutableListOf<String>()
//
//        if (!address.subLocality.isNullOrBlank()) {
//            parts.add(address.subLocality)
//        }
//        if (!address.locality.isNullOrBlank()) {
//            parts.add(address.locality)
//        }
//        if (!address.adminArea.isNullOrBlank()) {
//            parts.add(address.adminArea)
//        }
//        if (!address.countryName.isNullOrBlank()) {
//            parts.add(address.countryName)
//        }
//
//        return parts.joinToString(", ")
//    }
//}
//
//@Composable
//fun ManualLocationScreen(
//    navController: NavController,
//    authViewModel: AuthViewModel = viewModel(),
//    locationViewModel: LocationViewModel = viewModel()
//) {
//    val context = LocalContext.current
//    val locationState by locationViewModel.locationState.collectAsState()
//    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
//    var cameraPosition by remember { mutableStateOf(LatLng(20.5937, 78.9629)) } // India center
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color(0xFFF5F9FF))
//    ) {
//        // Search Bar
//        OutlinedTextField(
//            value = locationViewModel.searchQuery,
//            onValueChange = { locationViewModel.updateSearchQuery(it) },
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            placeholder = { Text("Search location") },
//            trailingIcon = {
//                IconButton(onClick = {
//                    if (locationViewModel.searchQuery.isNotEmpty()) {
//                        locationViewModel.geocodeAddress(context, locationViewModel.searchQuery)
//                    }
//                }) {
//                    Icon(
//                        imageVector = androidx.compose.material.icons.filled.Search,
//                        contentDescription = "Search"
//                    )
//                }
//            }
//        )
//
//        // Google Map
//        GoogleMap(
//            modifier = Modifier
//                .weight(1f)
//                .fillMaxWidth(),
//            cameraPositionState = rememberCameraPositionState {
//                position = CameraPosition.fromLatLngZoom(cameraPosition, 10f)
//            },
//            properties = MapProperties(
//                isMyLocationEnabled = true,
//                mapType = com.google.maps.android.compose.MapType.NORMAL
//            ),
//            onMapClick = { latLng ->
//                selectedLocation = latLng
//                locationViewModel.reverseGeocode(context, latLng)
//            }
//        ) {
//            selectedLocation?.let { location ->
//                Marker(
//                    state = MarkerState(position = location),
//                    title = "Selected Location"
//                )
//            }
//        }
//
//        // Location info and confirmation
//        androidx.compose.animation.AnimatedVisibility(
//            visible = locationState is LocationState.Success,
//            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
//            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
//        ) {
//            val successState = locationState as? LocationState.Success
//
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .background(Color.White)
//                    .padding(16.dp)
//            ) {
//                successState?.let { state ->
//                    Text(
//                        text = state.address,
//                        fontSize = 16.sp,
//                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
//                    )
//
//                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
//
//                    Button(
//                        onClick = {
//                            state.location.let { location ->
//                                authViewModel.updateUserLocation(
//                                    latitude = location.latitude,
//                                    longitude = location.longitude,
//                                    locationName = state.address
//                                ) { success ->
//                                    if (success) {
//                                        navController.navigate("photos")
//                                    }
//                                }
//                            }
//                        },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(56.dp),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = Color(0xFF246BFD)
//                        )
//                    ) {
//                        Text("Confirm Location")
//                    }
//                }
//            }
//        }
//    }
//}