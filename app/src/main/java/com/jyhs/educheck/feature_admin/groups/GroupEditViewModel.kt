package com.jyhs.educheck.feature_admin.groups

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.model.GroupInput
import com.jyhs.educheck.domain.model.Shift
import com.jyhs.educheck.domain.usecase.DeleteGroupUseCase
import com.jyhs.educheck.domain.usecase.ObserveGroupsUseCase
import com.jyhs.educheck.domain.usecase.SaveGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val NEW_ID = "new"

data class GroupEditUiState(
    val isNew: Boolean = true,
    val name: String = "",
    val semester: String = "",
    val shift: Shift = Shift.MORNING,
    val period: String = "",
    val loading: Boolean = true,
    val working: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

@HiltViewModel
class GroupEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeGroups: ObserveGroupsUseCase,
    private val saveGroup: SaveGroupUseCase,
    private val deleteGroup: DeleteGroupUseCase,
) : ViewModel() {

    private val groupId: String? = savedStateHandle.get<String>("id")?.takeIf { it != NEW_ID }

    private val _ui = MutableStateFlow(GroupEditUiState(isNew = groupId == null))
    val ui: StateFlow<GroupEditUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val existing = groupId?.let { id -> observeGroups().first().firstOrNull { it.id == id } }
                _ui.update {
                    it.copy(
                        name = existing?.name ?: "",
                        semester = existing?.semester?.toString() ?: "",
                        shift = existing?.shift ?: Shift.MORNING,
                        period = existing?.period ?: "",
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

    fun onNameChange(value: String) = _ui.update { it.copy(name = value, errorRes = null) }
    fun onSemesterChange(value: String) = _ui.update { it.copy(semester = value.filter(Char::isDigit).take(2), errorRes = null) }
    fun onShiftChange(value: Shift) = _ui.update { it.copy(shift = value, errorRes = null) }
    fun onPeriodChange(value: String) = _ui.update { it.copy(period = value, errorRes = null) }
    fun onErrorShown() = _ui.update { it.copy(errorRes = null) }

    fun save() {
        val current = _ui.value
        if (current.working || current.loading) return
        _ui.update { it.copy(working = true, errorRes = null) }
        viewModelScope.launch {
            val input = GroupInput(current.name, current.semester.toIntOrNull() ?: 0, current.shift, current.period)
            when (val result = saveGroup(groupId, input)) {
                is AppResult.Ok -> _ui.update { it.copy(working = false, saved = true) }
                is AppResult.Error -> _ui.update { it.copy(working = false, errorRes = result.reason.messageRes()) }
            }
        }
    }

    fun delete() {
        val id = groupId ?: return
        if (_ui.value.working) return
        _ui.update { it.copy(working = true, errorRes = null) }
        viewModelScope.launch {
            when (val result = deleteGroup(id)) {
                is AppResult.Ok -> _ui.update { it.copy(working = false, deleted = true) }
                is AppResult.Error -> _ui.update { it.copy(working = false, errorRes = result.reason.messageRes()) }
            }
        }
    }
}
