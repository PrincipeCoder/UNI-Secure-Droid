package com.example.unisecuredroid.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class AuthViewModel : ViewModel() {

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val token: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // Lógica de Login: Implementación temporal de acceso
    fun login(user: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            delay(500)

            try {
                if (user.isBlank() || pass.isBlank()) {
                    throw Exception("El usuario y la contraseña no pueden estar vacíos.")
                }

                val token = "FakeJWTToken_AuthZService"
                _authState.value = AuthState.Success(token)

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}