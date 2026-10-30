package com.jyhs.educheck.feature_admin.groups

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.model.GroupMember
import com.jyhs.educheck.domain.model.MAX_GROUP_SIZE
import com.jyhs.educheck.domain.model.Student
import com.jyhs.educheck.domain.usecase.ObserveGroupsUseCase
import com.jyhs.educheck.domain.usecase.ObserveStudentsUseCase
import com.jyhs.educheck.domain.usecase.SetGroupStudentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A student that can be picked, with the name of the other group it is in (if any). */
data class StudentOption(val student: Student, val otherGroupName: String?)

data class GroupStudentsUiState(
    val options: List<StudentOption> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val maxSize: Int = MAX_GROUP_SIZE,
    val loading: Boolean = true,
    val saving: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class GroupStudentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeGroups: ObserveGroupsUseCase,
    private val observeStudents: ObserveStudentsUseCase,
    private val setGroupStudents: SetGroupStudentsUseCase,
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle.get<String>("id"))
    private var students: List<Student> = emptyList()

    private val _ui = MutableStateFlow(GroupStudentsUiState())
    val ui: StateFlow<GroupStudentsUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val groups = observeGroups().first()
                val all = observeStudents().first()
                val group = groups.firstOrNull { it.id == groupId }
                val currentIds = group?.members?.map { it.id }?.toSet().orEmpty()
                val groupNames = groups.associate { it.id to it.name }
                students = all
                _ui.update {
                    it.copy(
                        // Inactive students are hidden unless they are already in this group.
                        options = all
                            .filter { s -> s.active || s.id in currentIds }
                            .map { s -> StudentOption(s, s.groupId?.takeIf { g -> g != groupId }?.let(groupNames::get)) },
                        selectedIds = currentIds,
                        loading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, errorRes = R.string.error_unknown) }
            }
        }
    }

    fun onToggle(studentId: String) = _ui.update {
        val next = if (studentId in it.selectedIds) it.selectedIds - studentId else it.selectedIds + studentId
        it.copy(selectedIds = next)
    }

    fun onErrorShown() = _ui.update { it.copy(errorRes = null) }

    fun save() {
        val current = _ui.value
        if (current.saving || current.loading) return
        _ui.update { it.copy(saving = true, errorRes = null) }
        viewModelScope.launch {
            val members = students
                .filter { it.id in current.selectedIds }
                .map { GroupMember(it.id, it.fullName, it.studentNumber) }
            when (val result = setGroupStudents(groupId, members)) {
                is AppResult.Ok -> _ui.update { it.copy(saving = false, saved = true) }
                is AppResult.Error -> _ui.update { it.copy(saving = false, errorRes = result.reason.messageRes()) }
            }
        }
    }
}
