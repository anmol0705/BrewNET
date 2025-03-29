package android.saswat.brewnet

import android.os.Bundle
import android.saswat.brewnet.Navigation.Navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import android.saswat.brewnet.ui.theme.BrewNetTheme
import android.saswat.factory.PhoneAuthViewModelFactory
import android.saswat.viewModel.PhoneAuthViewModel
import androidx.compose.foundation.layout.Box
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController


class MainActivity : ComponentActivity() {
    private lateinit var phoneAuthViewModel: PhoneAuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val phoneAuthViewModelFactory = PhoneAuthViewModelFactory(applicationContext)
        phoneAuthViewModel = ViewModelProvider(
            this,
            phoneAuthViewModelFactory
        )[PhoneAuthViewModel::class.java]

        setContent {
            BrewNetTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                            Navigation(navController = navController)
                    }
                }
            }
        }
    }
}

