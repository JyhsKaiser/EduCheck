package com.jyhs.educheck.core.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jyhs.educheck.domain.model.Session
import com.jyhs.educheck.feature_home.AccountScreen

/** Route of the account tab, shared by every role. */
const val ACCOUNT_ROUTE = "account"

data class RoleTab(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
)

/**
 * Shell of one role: bottom bar + NavHost. Each role provides its tabs and its graph
 * (see StudentNavGraph, GuardianNavGraph, TeacherNavGraph and AdminNavGraph).
 * The first tab is the start destination; the account tab is added here for everyone.
 */
@Composable
fun RoleScaffold(
    session: Session,
    tabs: List<RoleTab>,
    onSignOut: () -> Unit,
    graph: NavGraphBuilder.(NavController) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = tabs.first().route,
            modifier = Modifier.padding(padding),
        ) {
            this.graph(navController)
            composable(ACCOUNT_ROUTE) {
                AccountScreen(role = session.role, email = session.email, onSignOut = onSignOut)
            }
        }
    }
}
