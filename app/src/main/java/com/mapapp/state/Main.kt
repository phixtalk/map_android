package com.mapapp.state

sealed class MainState {
    data class Error(val errorMessage: String) : MainState()
    object Loading : MainState()
    object Idle : MainState()
}