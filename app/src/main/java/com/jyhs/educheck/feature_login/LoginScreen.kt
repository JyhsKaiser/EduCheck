package com.jyhs.educheck.feature_login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jyhs.educheck.R
import com.jyhs.educheck.core.designsystem.EduCheckTheme

@Composable
fun LoginScreen(vm: LoginViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LoginContent(
        ui = ui,
        onEmailChange = vm::onEmailChange,
        onClaveChange = vm::onClaveChange,
        onEnviar = vm::enviar,
    )
}

/** Composable sin estado: recibe el estado y emite eventos (facilita pruebas y previews). */
@Composable
fun LoginContent(
    ui: LoginUiState,
    onEmailChange: (String) -> Unit,
    onClaveChange: (String) -> Unit,
    onEnviar: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.login_titulo),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = ui.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.login_correo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.clave,
                onValueChange = onClaveChange,
                label = { Text(stringResource(R.string.login_clave)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            if (ui.errorRes != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(ui.errorRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onEnviar,
                enabled = !ui.cargando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (ui.cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.login_entrar))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    EduCheckTheme {
        LoginContent(
            ui = LoginUiState(email = "profesor@escuela.mx"),
            onEmailChange = {},
            onClaveChange = {},
            onEnviar = {},
        )
    }
}
