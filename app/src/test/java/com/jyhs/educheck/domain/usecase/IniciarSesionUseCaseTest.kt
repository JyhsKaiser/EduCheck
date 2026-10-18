package com.jyhs.educheck.domain.usecase

import com.jyhs.educheck.core.common.MotivoError
import com.jyhs.educheck.core.common.Resultado
import com.jyhs.educheck.domain.model.Rol
import com.jyhs.educheck.domain.model.Sesion
import com.jyhs.educheck.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IniciarSesionUseCaseTest {

    private class RepositorioFalso : AuthRepository {
        var llamadas = 0
        var ultimoEmail: String? = null

        override val sesion: Flow<Sesion?> = flowOf(null)
        override suspend fun restaurarSesion() = Unit
        override suspend fun cerrarSesion() = Unit

        override suspend fun iniciarSesion(email: String, clave: String): Resultado<Sesion> {
            llamadas++
            ultimoEmail = email
            return Resultado.Ok(Sesion(uid = "uid-1", email = email, rol = Rol.ADMIN, plantelId = null))
        }
    }

    @Test
    fun correo_vacio_devuelve_credenciales_sin_llamar_al_repositorio() = runTest {
        val repo = RepositorioFalso()

        val resultado = IniciarSesionUseCase(repo)("   ", "secreto")

        assertTrue(resultado is Resultado.Error)
        assertEquals(MotivoError.CREDENCIALES, (resultado as Resultado.Error).motivo)
        assertEquals(0, repo.llamadas)
    }

    @Test
    fun clave_vacia_devuelve_credenciales_sin_llamar_al_repositorio() = runTest {
        val repo = RepositorioFalso()

        val resultado = IniciarSesionUseCase(repo)("a@b.com", "")

        assertTrue(resultado is Resultado.Error)
        assertEquals(0, repo.llamadas)
    }

    @Test
    fun credenciales_validas_recortan_el_correo() = runTest {
        val repo = RepositorioFalso()

        val resultado = IniciarSesionUseCase(repo)("  a@b.com ", "secreto")

        assertTrue(resultado is Resultado.Ok)
        assertEquals("a@b.com", repo.ultimoEmail)
        assertEquals(1, repo.llamadas)
    }
}
