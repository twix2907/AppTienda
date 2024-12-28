package com.example.apptienda

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String) {
    object ProductList : Screen("productList")
    object AddProduct : Screen("addProduct")
}

@Composable
fun AppNavigation(viewModel: ProductoViewModel) {  // Agregamos el parámetro viewModel
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.ProductList.route
    ) {
        composable(Screen.ProductList.route) {
            ProductListScreen(
                viewModel = viewModel,  // Pasamos el viewModel
                onNavigateToAddProduct = {
                    navController.navigate(Screen.AddProduct.route)
                }
            )
        }
        composable(Screen.AddProduct.route) {
            AddProductScreen(
                viewModel = viewModel,  // Pasamos el viewModel
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}