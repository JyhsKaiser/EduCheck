package com.jyhs.educheck.feature_guardian

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jyhs.educheck.R
import com.jyhs.educheck.core.navigation.ACCOUNT_ROUTE
import com.jyhs.educheck.core.navigation.RoleTab
import com.jyhs.educheck.core.ui.ComingSoonScreen

private object GuardianRoutes {
    const val BOARD = "guardian/board"
    const val HISTORY = "guardian/history"
    const val JUSTIFICATIONS = "guardian/justifications"
}

val GuardianTabs = listOf(
    RoleTab(GuardianRoutes.BOARD, R.string.tab_board, Icons.Filled.Home),
    RoleTab(GuardianRoutes.HISTORY, R.string.tab_history, Icons.Filled.DateRange),
    RoleTab(GuardianRoutes.JUSTIFICATIONS, R.string.tab_justifications, Icons.Filled.Email),
    RoleTab(ACCOUNT_ROUTE, R.string.tab_account, Icons.Filled.Person),
)

fun NavGraphBuilder.guardianGraph() {
    composable(GuardianRoutes.BOARD) { ComingSoonScreen(R.string.tab_board) }
    composable(GuardianRoutes.HISTORY) { ComingSoonScreen(R.string.tab_history) }
    composable(GuardianRoutes.JUSTIFICATIONS) { ComingSoonScreen(R.string.tab_justifications) }
}
