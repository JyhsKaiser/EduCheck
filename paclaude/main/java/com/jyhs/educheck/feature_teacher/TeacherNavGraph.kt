package com.jyhs.educheck.feature_teacher

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jyhs.educheck.R
import com.jyhs.educheck.core.navigation.ACCOUNT_ROUTE
import com.jyhs.educheck.core.navigation.RoleTab
import com.jyhs.educheck.core.ui.ComingSoonScreen

private object TeacherRoutes {
    const val MY_DAY = "teacher/my_day"
    const val GROUPS = "teacher/groups"
    const val GRADES = "teacher/grades"
}

val TeacherTabs = listOf(
    RoleTab(TeacherRoutes.MY_DAY, R.string.tab_my_day, Icons.Filled.Home),
    RoleTab(TeacherRoutes.GROUPS, R.string.tab_groups, Icons.Filled.AccountBox),
    RoleTab(TeacherRoutes.GRADES, R.string.tab_grades, Icons.Filled.Check),
    RoleTab(ACCOUNT_ROUTE, R.string.tab_account, Icons.Filled.Person),
)

fun NavGraphBuilder.teacherGraph() {
    composable(TeacherRoutes.MY_DAY) { ComingSoonScreen(R.string.tab_my_day) }
    composable(TeacherRoutes.GROUPS) { ComingSoonScreen(R.string.tab_groups) }
    composable(TeacherRoutes.GRADES) { ComingSoonScreen(R.string.tab_grades) }
}
