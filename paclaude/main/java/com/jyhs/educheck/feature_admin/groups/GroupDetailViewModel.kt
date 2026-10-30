package com.jyhs.educheck.feature_admin.groups

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.domain.model.Assignment
import com.jyhs.educheck.domain.model.SchoolGroup
import com.jyhs.educheck.domain.usecase.ObserveAssignmentsUseCase
import com.jyhs.educheck.domain.usecase.ObserveGroupsUseCase
import com.jyhs.educheck.domain.usecase.ObserveUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssignmentRow(val assignment: Assignment, val teacherName: String)

data class GroupDetailUiState(
    val group: SchoolGroup? = null,
    val rows: List<AssignmentRow> = emptyList(),
    val loading: Boolean = true,
    @StringRes val errorRes: Int? = null,
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeGroups: ObserveGroupsUseCase,
    observeAssignments: ObserveAssignmentsUseCase,
    observeUsers: ObserveUsersUseCase,
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle.get<String>("id"))

    private val _ui = MutableStateFlow(GroupDetailUiState())
    val ui: StateFlow<GroupDetailUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            combine(observeGroups(), observeAssignments(), observeUsers()) { groups, assignments, users ->
                val names = users.associate { it.uid to it.name }
                GroupDetailUiState(
                    group = groups.firstOrNull { it.id == groupId },
                    rows = assignments
                        .filter { it.groupId == groupId }
                        .sortedBy { it.subjectName }
                        .map { AssignmentRow(it, names[it.teacherUid] ?: "—") },
                    loading = false,
                )
            }
                .catch { _ui.update { it.copy(loading = false, errorRes = R.string.error_unknown) } }
                .collect { state -> _ui.update { state.copy(errorRes = it.errorRes) } }
        }
    }

    fun onErrorShown() = _ui.update { it.copy(errorRes = null) }
}
