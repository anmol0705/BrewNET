package android.saswat.viewModel

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.saswat.brewnet.screens.Screens
import android.saswat.viewModel.AuthViewModel
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LocationState {
    object Initial : LocationState()
    object Loading : LocationState()
    data class Success(
        val location: LatLng,
        val address: String
    ) : LocationState()
    data class Error(val message: String) : LocationState()
}

class LocationViewModel : ViewModel() {
    private val _locationState = MutableStateFlow<LocationState>(LocationState.Initial)
    val locationState: StateFlow<LocationState> = _locationState

    var searchQuery by mutableStateOf("")
        private set

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun geocodeAddress(context: Context, address: String) {
        viewModelScope.launch {
            try {
                _locationState.value = LocationState.Loading

                val geocoder = Geocoder(context, java.util.Locale.getDefault())
                val addresses: List<Address>? = geocoder.getFromLocationName(address, 1)

                if (addresses.isNullOrEmpty()) {
                    _locationState.value = LocationState.Error("Location not found")
                    return@launch
                }

                val location = addresses[0]
                val latLng = LatLng(location.latitude, location.longitude)
                val formattedAddress = getFormattedAddress(location)

                _locationState.value = LocationState.Success(latLng, formattedAddress)
            } catch (e: java.io.IOException) {
                _locationState.value = LocationState.Error("Error finding location")
            }
        }
    }

    fun reverseGeocode(context: Context, latLng: LatLng) {
        viewModelScope.launch {
            try {
                _locationState.value = LocationState.Loading

                val geocoder = Geocoder(context, java.util.Locale.getDefault())
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

                if (addresses.isNullOrEmpty()) {
                    _locationState.value = LocationState.Error("Address not found")
                    return@launch
                }

                val address = addresses[0]
                val formattedAddress = getFormattedAddress(address)

                _locationState.value = LocationState.Success(latLng, formattedAddress)
            } catch (e: java.io.IOException) {
                _locationState.value = LocationState.Error("Error finding address")
            }
        }
    }

    private fun getFormattedAddress(address: Address): String {
        val parts = mutableListOf<String>()

        if (!address.subLocality.isNullOrBlank()) {
            parts.add(address.subLocality)
        }
        if (!address.locality.isNullOrBlank()) {
            parts.add(address.locality)
        }
        if (!address.adminArea.isNullOrBlank()) {
            parts.add(address.adminArea)
        }
        if (!address.countryName.isNullOrBlank()) {
            parts.add(address.countryName)
        }

        return parts.joinToString(", ")
    }
}

