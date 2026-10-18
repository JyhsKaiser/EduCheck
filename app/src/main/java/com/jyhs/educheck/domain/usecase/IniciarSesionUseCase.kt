package com.jyhs.educheck.domain.usecase

import com.jyhs.educheck.core.common.MotivoError
import com.jyhs.educheck.core.common.Resultado
import com.jyhs.educheck.domain.model.Sesion
import com.jyhs.educheck.domain.repository.AuthRepository
import javax.inject.Inject

class IniciarSesionUseCase @Inject constructor(
    private val repositorio: AuthRepository,
) {
    suspend operator fun invoke(email: String, clave: String): Resultado<Sesion> {
        if (email.isBlank() || clave.isBlank()) {
            return Resultado.Error(MotivoError.CREDENCIALES)
        }
        return repositorio.iniciarSesion(email.trim(), clave)
    }
}
