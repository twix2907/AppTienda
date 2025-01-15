package com.example.apptienda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.apptienda.data.repository.CloudinaryRepository
import com.example.apptienda.data.repository.FirestoreRepository
import com.example.apptienda.ui.navigation.AppNavigation
import com.example.apptienda.ui.theme.AppTiendaTheme
import com.example.apptienda.ui.viewmodel.ProductoViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ProductoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar los repositorios
        val cloudinaryRepository = CloudinaryRepository()
        val firestoreRepository = FirestoreRepository()

        // Inicializar el ViewModel con los repositorios
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProductoViewModel(firestoreRepository, cloudinaryRepository) as T
            }
        })[ProductoViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            AppTiendaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}