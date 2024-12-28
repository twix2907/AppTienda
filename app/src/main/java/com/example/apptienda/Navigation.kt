package com.example.apptienda

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object ProductList : Screen("productList")
    object AddProduct : Screen("addProduct")
    object EditProduct : Screen("editProduct/{productId}") {
        fun createRoute(productId: String) = "editProduct/$productId"
    }
}

@Composable
fun AppNavigation(viewModel: ProductoViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.ProductList.route
    ) {
        composable(Screen.ProductList.route) {
            ProductListScreen(
                viewModel = viewModel,
                onNavigateToAddProduct = {
                    navController.navigate(Screen.AddProduct.route)
                },
                onNavigateToEditProduct = { producto ->
                    navController.navigate(Screen.EditProduct.createRoute(producto.id))
                }
            )
        }

        composable(Screen.AddProduct.route) {
            AddProductScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditProduct.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            EditProductScreen(
                productId = productId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}