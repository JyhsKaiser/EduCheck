package com.jyhs.educheck.feature_admin.assignments

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.model.Assignment
import com.jyhs.educheck.domain.model.Schedule
import com.jyhs.educheck.domain.model.ScheduleInput
import com.jyhs.educheck.domain.usecase.CreateSchedulesUseCase
import com.jyhs.educheck.domain.usecase.DeleteAssignmentUseCase
import com.jyhs.educheck.domain.usecase.DeleteScheduleUseCase
import com.jyhs.educheck.domain.usecase.ObserveAssignmentsUseCase
import com.jyhs.educheck.domain.usecase.ObserveSchedulesUseCase
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

/** Form of the "new schedule" dialog: one set of hours for several weekdays. */
data class ScheduleForm(
    val weekdays: Set<Int> = emptySet(),
    val startTime: String = "",
    val endTime: String = "",
    val room: String = "",
    val saving: Boolean = false,
)

data class AssignmentDetailUiState(
    val assignment: Assignment? = null,
    val teacherName: String = "",
    val schedules: List<Schedule> = emptyList(),
    val form: ScheduleForm? = null,
    val loading: Boolean = true,
    val working: Boolean = false,
    @StringRes val errorRes: Int? = null,
    /** Text inserted in the error message (for example the class a schedule clashes with). */
    val errorArg: String? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class AssignmentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeAssignments: ObserveAssignmentsUseCase,
    observeSchedules: ObserveSchedulesUseCase,
    observeUsers: ObserveUsersUseCase,
    private val createSchedules: CreateSchedulesUseCase,
    private val deleteSchedule: DeleteScheduleUseCase,
    private val deleteAssignment: DeleteAssignmentUseCase,
) : ViewModel() {

    private val assignmentId: String = checkNotNull(savedStateHandle.get<String>("id"))

    private val _ui = MutableStateFlow(AssignmentDetailUiState())
    val ui: StateFlow<AssignmentDetailUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            combine(observeAssignments(), observeSchedules(assignmentId), observeUsers()) { assignments, schedules, users ->
                val assignment = assignments.firstOrNull { it.id == assignmentId }
                Triple(assignment, schedules, users.firstOrNull { it.uid == assignment?.teacherUid }?.name.orEmpty())
            }
                .catch { _ui.update { it.copy(loading = false, errorRes = R.string.error_unknown) } }
                .collect { (assignment, schedules, teacher) ->
                    _ui.update {
                        it.copy(assignment = assignment, schedules = schedules, teacherName = teacher, loading = false)
                    }
                }
        }
    }

    // ---- Schedule dialog
    fun onAddSchedule() = _ui.update { it.copy(form = ScheduleForm()) }
    fun onDismissForm() = _ui.update { it.copy(form = null) }

    fun onToggleWeekday(day: Int) = _ui.update { state ->
        state.copy(
            form = state.form?.let { f ->
                f.copy(weekdays = if (day in f.weekdays) f.weekdays - day else f.weekdays + day)
            },
        )
    }

    fun onStartChange(value: String) = _ui.update { state -> state.copy(form = state.form?.copy(startTime = value)) }
    fun onEndChange(value: String) = _ui.update { state -> state.copy(form = state.form?.copy(endTime = value)) }
    fun onRoomChange(value: String) = _ui.update { state -> state.copy(form = state.form?.copy(room = value)) }

    fun onSaveSchedule() {
        val form = _ui.value.form ?: return
        val assignment = _ui.value.assignment ?: return
        if (form.saving) return
        _ui.update { it.copy(form = form.copy(saving = true)) }
        viewModelScope.launch {
            val input = ScheduleInput(form.weekdays, form.startTime, form.endTime, form.room)
            when (val result = createSchedules(assignment, input)) {
                is AppResult.Ok -> _ui.update { it.copy(form = null) }
                is AppResult.Error -> _ui.update {
                    it.copy(
                        form = form.copy(saving = false),
                        errorRes = result.reason.messageRes(),
                        errorArg = result.detail,
                    )
                }
            }
        }
    }

    fun onDeleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            val result = deleteSchedule(schedule.id)
            if (result is AppResult.Error) _ui.update { it.copy(errorRes = result.reason.messageRes(), errorArg = null) }
        }
    }

    // ---- Assignment
    fun onDeleteAssignment() {
        val assignment = _ui.value.assignment ?: return
        if (_ui.value.working) return
        _ui.update { it.copy(working = true) }
        viewModelScope.launch {
            when (val result = deleteAssignment(assignment)) {
                is AppResult.Ok -> _ui.update { it.copy(working = false, deleted = true) }
                is AppResult.Error -> _ui.update {
                    it.copy(working = false, errorRes = result.reason.messageRes(), errorArg = null)
                }
            }
        }
    }

    fun onErrorShown() = _ui.update { it.copy(errorRes = null, errorArg = null) }
}
