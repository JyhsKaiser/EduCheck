package com.jyhs.educheck.feature_admin.assignments

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.model.AppUser
import com.jyhs.educheck.domain.model.NewAssignment
import com.jyhs.educheck.domain.model.Role
import com.jyhs.educheck.domain.model.SchoolGroup
import com.jyhs.educheck.domain.model.Subject
import com.jyhs.educheck.domain.usecase.CreateAssignmentUseCase
import com.jyhs.educheck.domain.usecase.ObserveGroupsUseCase
import com.jyhs.educheck.domain.usecase.ObserveSubjectsUseCase
import com.jyhs.educheck.domain.usecase.ObserveUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssignmentNewUiState(
    val group: SchoolGroup? = null,
    val subjects: List<Subject> = emptyList(),
    val teachers: List<AppUser> = emptyList(),
    val subjectId: String? = null,
    val teacherUid: String? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val created: Boolean = false,
)

@HiltViewModel
class AssignmentNewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeGroups: ObserveGroupsUseCase,
    private val observeSubjects: ObserveSubjectsUseCase,
    private val observeUsers: ObserveUsersUseCase,
    private val createAssignment: CreateAssignmentUseCase,
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle.get<String>("id"))

    private val _ui = MutableStateFlow(AssignmentNewUiState())
    val ui: StateFlow<AssignmentNewUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val group = observeGroups().first().firstOrNull { it.id == groupId }
                val subjects = observeSubjects().first()
                val teachers = observeUsers().first().filter { it.role == Role.TEACHER && it.active }
                _ui.update { it.copy(group = group, subjects = subjects, teachers = teachers, loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, errorRes = R.string.error_unknown) }
            }
        }
    }

    fun onSubjectChange(id: String) = _ui.update { it.copy(subjectId = id, errorRes = null) }
    fun onTeacherChange(uid: String) = _ui.update { it.copy(teacherUid = uid, errorRes = null) }
    fun onErrorShown() = _ui.update { it.copy(errorRes = null) }

    fun create() {
        val current = _ui.value
        if (current.saving || current.loading) return
        val group = current.group
        val subject = current.subjects.firstOrNull { it.id == current.subjectId }
        val teacher = current.teachers.firstOrNull { it.uid == current.teacherUid }
        if (group == null || subject == null || teacher == null) {
            _ui.update { it.copy(errorRes = ErrorReason.INVALID_INPUT.messageRes()) }
            return
        }
        _ui.update { it.copy(saving = true, errorRes = null) }
        viewModelScope.launch {
            when (val result = createAssignment(NewAssignment(group, subject, teacher))) {
                is AppResult.Ok -> _ui.update { it.copy(saving = false, created = true) }
                is AppResult.Error -> _ui.update { it.copy(saving = false, errorRes = result.reason.messageRes()) }
            }
        }
    }
}
