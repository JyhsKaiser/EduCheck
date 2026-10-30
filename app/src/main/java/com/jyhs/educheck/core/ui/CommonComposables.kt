package com.jyhs.educheck.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.unit.dp
import com.jyhs.educheck.R

/**
 * Scaffold for the screens that live inside a role tab. The role shell (RoleScaffold) already
 * applies the system insets, so this one uses none to avoid a double gap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreenScaffold(
    @StringRes title: Int,
    onBack: () -> Unit,
    snackbarHost: SnackbarHostState? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            )
        },
        snackbarHost = { if (snackbarHost != null) SnackbarHost(snackbarHost) },
        floatingActionButton = floatingActionButton,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        content = content,
    )
}

@Composable
fun CenteredProgress(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun CenteredMessage(@StringRes text: Int, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text(stringResource(text)) }
}

/** Shows [errorRes] in a snackbar once and then calls [onShown]. */
@Composable
fun rememberErrorHost(@StringRes errorRes: Int?, onShown: () -> Unit): SnackbarHostState {
    val host = remember { SnackbarHostState() }
    val message = errorRes?.let { stringResource(it) }
    LaunchedEffect(message) {
        if (message != null) {
            host.showSnackbar(message)
            onShown()
        }
    }
    return host
}

@Composable
fun RadioRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = SemanticsRole.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
