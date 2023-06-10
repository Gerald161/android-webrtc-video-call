package com.example.screenshare.navigation
import HomePage
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Navigation(){
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screens.HomeScreen.route,){
        composable(Screens.HomeScreen.route){
            HomePage()
        }
    }
}