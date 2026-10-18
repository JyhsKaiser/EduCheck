package com.jyhs.educheck.feature_login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.MotivoError
import com.jyhs.educheck.core.common.Resultado
import com.jyhs.educheck.domain.usecase.IniciarSesionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val clave: String = "",
    val cargando: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val iniciarSesion: IniciarSesionUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    fun onEmailChange(valor: String) = _ui.update { it.copy(email = valor, errorRes = null) }

    fun onClaveChange(valor: String) = _ui.update { it.copy(clave = valor, errorRes = null) }

    fun enviar() {
        val actual = _ui.value
        if (actual.cargando) return
        _ui.update { it.copy(cargando = true, errorRes = null) }
        viewModelScope.launch {
            when (val resultado = iniciarSesion(actual.email, actual.clave)) {
                // Al tener sesión, AppNavHost cambia de pantalla por sí solo.
                is Resultado.Ok -> _ui.update { it.copy(cargando = false, clave = "") }
                is Resultado.Error -> _ui.update { it.copy(cargando = false, errorRes = mensaje(resultado.motivo)) }
            }
        }
    }

    @StringRes
    private fun mensaje(motivo: MotivoError): Int = when (motivo) {
        MotivoError.SIN_RED -> R.string.error_sin_red
        MotivoError.CREDENCIALES -> R.string.error_credenciales
        MotivoError.SIN_PERFIL -> R.string.error_sin_perfil
        MotivoError.DESCONOCIDO -> R.string.error_desconocido
    }
}
