package com.jyhs.educheck.feature_home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jyhs.educheck.R
import com.jyhs.educheck.domain.model.Rol

/** Pantalla provisional para comprobar el login y el enrutado por rol. Se sustituirá por un grafo por rol. */
@Composable
fun HomePlaceholderScreen(
    rol: Rol,
    email: String,
    onCerrarSesion: () -> Unit,
) {
    val etiquetaRol = when (rol) {
        Rol.ALUMNO -> R.string.rol_alumno
        Rol.TUTOR -> R.string.rol_tutor
        Rol.PROFESOR -> R.string.rol_profesor
        Rol.ADMIN -> R.string.rol_admin
    }
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = stringResource(R.string.home_bienvenida), style = MaterialTheme.typography.headlineSmall)
            Text(text = stringResource(etiquetaRol), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Text(text = email, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onCerrarSesion) {
                Text(stringResource(R.string.home_cerrar_sesion))
            }
        }
    }
}
