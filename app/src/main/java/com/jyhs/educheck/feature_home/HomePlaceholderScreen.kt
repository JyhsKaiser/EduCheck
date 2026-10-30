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
import com.jyhs.educheck.domain.model.Role

/** Provisional screen to verify login and role routing. It will be replaced by one graph per role. */
@Composable
fun HomePlaceholderScreen(
    role: Role,
    email: String,
    onSignOut: () -> Unit,
) {
    val roleLabel = when (role) {
        Role.STUDENT -> R.string.role_student
        Role.GUARDIAN -> R.string.role_guardian
        Role.TEACHER -> R.string.role_teacher
        Role.ADMIN -> R.string.role_admin
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
//            Text(text = stringResource(R.string.home_welcome), style = MaterialTheme.typography.headlineSmall)
            Text(text = stringResource(roleLabel), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Text(text = email, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onSignOut) {
//                Text(stringResource(R.string.home_sign_out))
            }
        }
    }
}
