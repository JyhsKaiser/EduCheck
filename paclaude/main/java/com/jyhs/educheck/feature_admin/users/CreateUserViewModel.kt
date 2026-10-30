package com.jyhs.educheck.feature_admin.users

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.model.NewUser
import com.jyhs.educheck.domain.model.Role
import com.jyhs.educheck.domain.model.Student
import com.jyhs.educheck.domain.usecase.CreateUserUseCase
import com.jyhs.educheck.domain.usecase.ObserveStudentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateUserUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val role: Role = Role.TEACHER,
    val studentId: String? = null,
    /** Active students that do not have an account yet (only used when role == STUDENT). */
    val availableStudents: List<Student> = emptyList(),
    val loading: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val created: Boolean = false,
)

@HiltViewModel
class CreateUserViewModel @Inject constructor(
    observeStudents: ObserveStudentsUseCase,
    private val createUser: CreateUserUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(CreateUserUiState())
    val ui: StateFlow<CreateUserUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            observeStudents()
                .catch { /* the list is optional; the form still works without it */ }
                .collect { list ->
                    _ui.update { state ->
                        state.copy(availableStudents = list.filter { it.studentUid == null && it.active })
                    }
                }
        }
    }

    fun onNameChange(value: String) = _ui.update { it.copy(name = value, errorRes = null) }
    fun onEmailChange(value: String) = _ui.update { it.copy(email = value, errorRes = null) }
    fun onPasswordChange(value: String) = _ui.update { it.copy(password = value, errorRes = null) }

    fun onRoleChange(role: Role) = _ui.update {
        it.copy(role = role, studentId = if (role == Role.STUDENT) it.studentId else null, errorRes = null)
    }

    fun onStudentChange(studentId: String?) = _ui.update { it.copy(studentId = studentId, errorRes = null) }

    fun onErrorShown() = _ui.update { it.copy(errorRes = null) }

    fun submit() {
        val current = _ui.value
        if (current.loading) return
        _ui.update { it.copy(loading = true, errorRes = null) }
        viewModelScope.launch {
            val result = createUser(
                NewUser(
                    email = current.email,
                    password = current.password,
                    name = current.name,
                    role = current.role,
                    studentId = current.studentId,
                ),
            )
            when (result) {
                is AppResult.Ok -> _ui.update { it.copy(loading = false, created = true) }
                is AppResult.Error -> _ui.update { it.copy(loading = false, errorRes = result.reason.messageRes()) }
            }
        }
    }
}
