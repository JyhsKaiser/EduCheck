package com.jyhs.educheck.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jyhs.educheck.domain.model.Role
import com.jyhs.educheck.domain.model.Session
import com.jyhs.educheck.feature_admin.AdminTabs
import com.jyhs.educheck.feature_admin.adminGraph
import com.jyhs.educheck.feature_guardian.GuardianTabs
import com.jyhs.educheck.feature_guardian.guardianGraph
import com.jyhs.educheck.feature_login.LoginScreen
import com.jyhs.educheck.feature_student.StudentTabs
import com.jyhs.educheck.feature_student.studentGraph
import com.jyhs.educheck.feature_teacher.TeacherTabs
import com.jyhs.educheck.feature_teacher.teacherGraph

/**
 * Navigation root: loading, login, or the graph of the signed-in user's role.
 * The role only decides which screens are shown; real security lives in the Firestore rules
 * (custom claim) and in the Edge Functions.
 */
@Composable
fun AppNavHost(vm: SessionViewModel = viewModel()) {
    val session by vm.session.collectAsStateWithLifecycle()
    when (val s = session) {
        SessionUi.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        SessionUi.SignedOut -> LoginScreen()
        is SessionUi.Active -> RoleHost(session = s.session, onSignOut = vm::signOut)
    }
}

@Composable
private fun RoleHost(session: Session, onSignOut: () -> Unit) {
    when (session.role) {
        Role.STUDENT -> RoleScaffold(session, StudentTabs, onSignOut) { studentGraph() }
        Role.GUARDIAN -> RoleScaffold(session, GuardianTabs, onSignOut) { guardianGraph() }
        Role.TEACHER -> RoleScaffold(session, TeacherTabs, onSignOut) { teacherGraph() }
        Role.ADMIN -> RoleScaffold(session, AdminTabs, onSignOut) { nav -> adminGraph(nav) }
    }
}
