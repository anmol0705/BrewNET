package android.saswat.viewModel

import android.location.Address
import android.location.Geocoder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

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

    fun geocodeAddress(context: android.content.Context, address: String) {
        viewModelScope.launch {
            try {
                _locationState.value = LocationState.Loading
                
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = geocoder.getFromLocationName(address, 1)
                
                if (addresses.isNullOrEmpty()) {
                    _locationState.value = LocationState.Error("Location not found")
                    return@launch
                }
                
                val location = addresses[0]
                val latLng = LatLng(location.latitude, location.longitude)
                val formattedAddress = getFormattedAddress(location)
                
                _locationState.value = LocationState.Success(latLng, formattedAddress)
            } catch (e: IOException) {
                _locationState.value = LocationState.Error("Error finding location")
            }
        }
    }

    fun reverseGeocode(context: android.content.Context, latLng: LatLng) {
        viewModelScope.launch {
            try {
                _locationState.value = LocationState.Loading
                
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                
                if (addresses.isNullOrEmpty()) {
                    _locationState.value = LocationState.Error("Address not found")
                    return@launch
                }
                
                val address = addresses[0]
                val formattedAddress = getFormattedAddress(address)
                
                _locationState.value = LocationState.Success(latLng, formattedAddress)
            } catch (e: IOException) {
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