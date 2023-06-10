package com.example.screenshare.navigation

sealed class Screens(val route: String){
    object HomeScreen: Screens("home")
}
