package com.jyhs.educheck.domain.usecase

import com.jyhs.educheck.domain.model.Sesion
import com.jyhs.educheck.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObservarSesionUseCase @Inject constructor(private val repositorio: AuthRepository) {
    operator fun invoke(): Flow<Sesion?> = repositorio.sesion
}

class RestaurarSesionUseCase @Inject constructor(private val repositorio: AuthRepository) {
    suspend operator fun invoke() = repositorio.restaurarSesion()
}

class CerrarSesionUseCase @Inject constructor(private val repositorio: AuthRepository) {
    suspend operator fun invoke() = repositorio.cerrarSesion()
}
