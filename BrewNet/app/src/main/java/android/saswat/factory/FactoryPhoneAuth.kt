package android.saswat.factory

import android.content.Context
import android.saswat.viewModel.PhoneAuthViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PhoneAuthViewModelFactory(private val applicationContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhoneAuthViewModel::class.java)) {
            return PhoneAuthViewModel(applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}