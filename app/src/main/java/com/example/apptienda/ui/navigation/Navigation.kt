package com.example.apptienda.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.apptienda.ui.viewmodel.ProductoViewModel
import com.example.apptienda.ui.screens.products.add.AddProductScreen
import com.example.apptienda.ui.screens.products.edit.EditProductScreen
import com.example.apptienda.ui.screens.products.list.ProductListScreen

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
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            requireNotNull(productId) { "productId parameter wasn't found. Please make sure it's set!" }

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