package com.jyhs.educheck.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.domain.model.Sesion
import com.jyhs.educheck.domain.usecase.CerrarSesionUseCase
import com.jyhs.educheck.domain.usecase.ObservarSesionUseCase
import com.jyhs.educheck.domain.usecase.RestaurarSesionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SesionUi {
    data object Cargando : SesionUi
    data object SinSesion : SesionUi
    data class Activa(val sesion: Sesion) : SesionUi
}

@HiltViewModel
class SesionViewModel @Inject constructor(
    observarSesion: ObservarSesionUseCase,
    private val restaurarSesionUseCase: RestaurarSesionUseCase,
    private val cerrarSesionUseCase: CerrarSesionUseCase,
) : ViewModel() {

    val sesion: StateFlow<SesionUi> = observarSesion()
        .map { s -> if (s == null) SesionUi.SinSesion else SesionUi.Activa(s) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SesionUi.Cargando)

    init {
        viewModelScope.launch { restaurarSesionUseCase() }
    }

    fun cerrarSesion() {
        viewModelScope.launch { cerrarSesionUseCase() }
    }
}
