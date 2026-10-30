package com.jyhs.educheck.feature_student

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jyhs.educheck.R
import com.jyhs.educheck.core.navigation.ACCOUNT_ROUTE
import com.jyhs.educheck.core.navigation.RoleTab
import com.jyhs.educheck.core.ui.ComingSoonScreen

private object StudentRoutes {
    const val TODAY = "student/today"
}

val StudentTabs = listOf(
    RoleTab(StudentRoutes.TODAY, R.string.tab_today, Icons.Filled.Home),
    RoleTab(ACCOUNT_ROUTE, R.string.tab_account, Icons.Filled.Person),
)

fun NavGraphBuilder.studentGraph() {
    composable(StudentRoutes.TODAY) { ComingSoonScreen(R.string.tab_today) }
}
