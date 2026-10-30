package com.jyhs.educheck.feature_login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.usecase.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signIn: SignInUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    fun onEmailChange(value: String) = _ui.update { it.copy(email = value, errorRes = null) }

    fun onPasswordChange(value: String) = _ui.update { it.copy(password = value, errorRes = null) }

    fun submit() {
        val current = _ui.value
        if (current.loading) return
        _ui.update { it.copy(loading = true, errorRes = null) }
        viewModelScope.launch {
            when (val result = signIn(current.email, current.password)) {
                // Once there is a session, AppNavHost switches screens by itself.
                is AppResult.Ok -> _ui.update { it.copy(loading = false, password = "") }
                is AppResult.Error -> _ui.update { it.copy(loading = false, errorRes = result.reason.messageRes()) }
            }
        }
    }
}
