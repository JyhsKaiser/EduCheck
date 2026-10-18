package com.jyhs.educheck.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jyhs.educheck.feature_home.HomePlaceholderScreen
import com.jyhs.educheck.feature_login.LoginScreen

/**
 * Raíz de la navegación. Por ahora decide entre carga, login y un inicio provisional.
 * En las siguientes etapas cada rol tendrá su propio grafo (Alumno, Tutor, Profesor, Prefecto, Admin).
 * El rol solo decide qué pantallas se muestran; la seguridad real vive en Firestore y el backend.
 */
@Composable
fun AppNavHost(vm: SesionViewModel = viewModel()) {
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    when (val s = sesion) {
        SesionUi.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        SesionUi.SinSesion -> LoginScreen()
        is SesionUi.Activa -> HomePlaceholderScreen(
            rol = s.sesion.rol,
            email = s.sesion.email,
            onCerrarSesion = vm::cerrarSesion,
        )
    }
}
