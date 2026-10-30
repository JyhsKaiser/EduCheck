package com.jyhs.educheck.feature_admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jyhs.educheck.R
import com.jyhs.educheck.core.navigation.ACCOUNT_ROUTE
import com.jyhs.educheck.core.navigation.RoleTab
import com.jyhs.educheck.core.ui.ComingSoonScreen
import com.jyhs.educheck.feature_admin.devices.DevicesScreen
import com.jyhs.educheck.feature_admin.students.NEW_STUDENT_ID
import com.jyhs.educheck.feature_admin.students.StudentEditScreen
import com.jyhs.educheck.feature_admin.students.StudentsScreen
import com.jyhs.educheck.feature_admin.users.CreateUserScreen
import com.jyhs.educheck.feature_admin.users.UsersScreen

private object AdminRoutes {
    const val DASHBOARD = "admin/dashboard"
    const val JUSTIFICATIONS = "admin/justifications"
    const val MANAGEMENT = "admin/management"
    const val USERS = "admin/users"
    const val USER_NEW = "admin/users/new"
    const val STUDENTS = "admin/students"
    const val STUDENT_EDIT = "admin/students/edit/{id}"
    const val DEVICES = "admin/devices"

    fun studentEdit(id: String) = "admin/students/edit/$id"
}

val AdminTabs = listOf(
    RoleTab(AdminRoutes.DASHBOARD, R.string.tab_dashboard, Icons.Filled.Home),
    RoleTab(AdminRoutes.JUSTIFICATIONS, R.string.tab_justifications, Icons.Filled.Email),
    RoleTab(AdminRoutes.MANAGEMENT, R.string.tab_management, Icons.Filled.Settings),
    RoleTab(ACCOUNT_ROUTE, R.string.tab_account, Icons.Filled.Person),
)

fun NavGraphBuilder.adminGraph(nav: NavController) {
    composable(AdminRoutes.DASHBOARD) { ComingSoonScreen(R.string.tab_dashboard) }
    composable(AdminRoutes.JUSTIFICATIONS) { ComingSoonScreen(R.string.tab_justifications) }

    composable(AdminRoutes.MANAGEMENT) {
        AdminManagementScreen(
            onOpenUsers = { nav.navigate(AdminRoutes.USERS) },
            onOpenStudents = { nav.navigate(AdminRoutes.STUDENTS) },
            onOpenDevices = { nav.navigate(AdminRoutes.DEVICES) },
        )
    }

    composable(AdminRoutes.USERS) {
        UsersScreen(onBack = { nav.popBackStack() }, onCreate = { nav.navigate(AdminRoutes.USER_NEW) })
    }
    composable(AdminRoutes.USER_NEW) {
        CreateUserScreen(onBack = { nav.popBackStack() })
    }

    composable(AdminRoutes.STUDENTS) {
        StudentsScreen(
            onBack = { nav.popBackStack() },
            onCreate = { nav.navigate(AdminRoutes.studentEdit(NEW_STUDENT_ID)) },
            onEdit = { id -> nav.navigate(AdminRoutes.studentEdit(id)) },
        )
    }
    composable(
        route = AdminRoutes.STUDENT_EDIT,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) {
        StudentEditScreen(onBack = { nav.popBackStack() })
    }

    composable(AdminRoutes.DEVICES) {
        DevicesScreen(onBack = { nav.popBackStack() })
    }
}
