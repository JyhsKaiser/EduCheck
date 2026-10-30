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
import com.jyhs.educheck.feature_admin.assignments.AssignmentDetailScreen
import com.jyhs.educheck.feature_admin.assignments.AssignmentNewScreen
import com.jyhs.educheck.feature_admin.devices.DevicesScreen
import com.jyhs.educheck.feature_admin.groups.GroupDetailScreen
import com.jyhs.educheck.feature_admin.groups.GroupEditScreen
import com.jyhs.educheck.feature_admin.groups.GroupStudentsScreen
import com.jyhs.educheck.feature_admin.groups.GroupsScreen
import com.jyhs.educheck.feature_admin.groups.NEW_ID
import com.jyhs.educheck.feature_admin.students.NEW_STUDENT_ID
import com.jyhs.educheck.feature_admin.students.StudentEditScreen
import com.jyhs.educheck.feature_admin.students.StudentsScreen
import com.jyhs.educheck.feature_admin.subjects.SubjectsScreen
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
    const val SUBJECTS = "admin/subjects"
    const val GROUPS = "admin/groups"
    const val GROUP_EDIT = "admin/groups/edit/{id}"
    const val GROUP_DETAIL = "admin/groups/{id}"
    const val GROUP_STUDENTS = "admin/groups/{id}/students"
    const val ASSIGNMENT_NEW = "admin/groups/{id}/assignments/new"
    const val ASSIGNMENT_DETAIL = "admin/assignments/{id}"

    fun studentEdit(id: String) = "admin/students/edit/$id"
    fun groupEdit(id: String) = "admin/groups/edit/$id"
    fun groupDetail(id: String) = "admin/groups/$id"
    fun groupStudents(id: String) = "admin/groups/$id/students"
    fun assignmentNew(groupId: String) = "admin/groups/$groupId/assignments/new"
    fun assignmentDetail(id: String) = "admin/assignments/$id"
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
            onOpenSubjects = { nav.navigate(AdminRoutes.SUBJECTS) },
            onOpenGroups = { nav.navigate(AdminRoutes.GROUPS) },
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

    composable(AdminRoutes.SUBJECTS) {
        SubjectsScreen(onBack = { nav.popBackStack() })
    }

    composable(AdminRoutes.GROUPS) {
        GroupsScreen(
            onBack = { nav.popBackStack() },
            onCreate = { nav.navigate(AdminRoutes.groupEdit(NEW_ID)) },
            onOpen = { id -> nav.navigate(AdminRoutes.groupDetail(id)) },
        )
    }
    composable(
        route = AdminRoutes.GROUP_EDIT,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) {
        GroupEditScreen(
            onBack = { nav.popBackStack() },
            // The group no longer exists: go back to the list, not to its detail.
            onDeleted = { nav.popBackStack(AdminRoutes.GROUPS, inclusive = false) },
        )
    }
    composable(
        route = AdminRoutes.GROUP_DETAIL,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { entry ->
        val groupId = entry.arguments?.getString("id").orEmpty()
        GroupDetailScreen(
            onBack = { nav.popBackStack() },
            onEdit = { nav.navigate(AdminRoutes.groupEdit(groupId)) },
            onStudents = { nav.navigate(AdminRoutes.groupStudents(groupId)) },
            onAddSubject = { nav.navigate(AdminRoutes.assignmentNew(groupId)) },
            onOpenAssignment = { id -> nav.navigate(AdminRoutes.assignmentDetail(id)) },
        )
    }
    composable(
        route = AdminRoutes.GROUP_STUDENTS,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) {
        GroupStudentsScreen(onBack = { nav.popBackStack() })
    }
    composable(
        route = AdminRoutes.ASSIGNMENT_NEW,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) {
        AssignmentNewScreen(onBack = { nav.popBackStack() })
    }
    composable(
        route = AdminRoutes.ASSIGNMENT_DETAIL,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) {
        AssignmentDetailScreen(onBack = { nav.popBackStack() })
    }
}
