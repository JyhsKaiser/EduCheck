package com.jyhs.educheck.feature_admin.students

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.model.AppUser
import com.jyhs.educheck.domain.model.Role
import com.jyhs.educheck.domain.model.StudentInput
import com.jyhs.educheck.domain.usecase.ObserveStudentsUseCase
import com.jyhs.educheck.domain.usecase.ObserveUsersUseCase
import com.jyhs.educheck.domain.usecase.SaveStudentUseCase
import com.jyhs.educheck.domain.usecase.SetGuardiansUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val NEW_STUDENT_ID = "new"

data class StudentEditUiState(
    val isNew: Boolean = true,
    val studentNumber: String = "",
    val firstNames: String = "",
    val lastNames: String = "",
    /** Active GUARDIAN accounts that can be linked. */
    val guardianOptions: List<AppUser> = emptyList(),
    val selectedGuardianUids: Set<String> = emptySet(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class StudentEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeStudents: ObserveStudentsUseCase,
    private val observeUsers: ObserveUsersUseCase,
    private val saveStudent: SaveStudentUseCase,
    private val setGuardians: SetGuardiansUseCase,
) : ViewModel() {

    private val studentId: String? =
        savedStateHandle.get<String>("id")?.takeIf { it != NEW_STUDENT_ID }

    private val _ui = MutableStateFlow(StudentEditUiState(isNew = studentId == null))
    val ui: StateFlow<StudentEditUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val guardians = observeUsers().first().filter { it.role == Role.GUARDIAN && it.active }
                val existing = studentId?.let { id -> observeStudents().first().firstOrNull { it.id == id } }
                _ui.update {
                    it.copy(
                        guardianOptions = guardians,
                        studentNumber = existing?.studentNumber ?: "",
                        firstNames = existing?.firstNames ?: "",
                        lastNames = existing?.lastNames ?: "",
                        selectedGuardianUids = existing?.guardianUids?.toSet() ?: emptySet(),
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

    fun onNumberChange(value: String) = _ui.update { it.copy(studentNumber = value, errorRes = null) }
    fun onFirstNamesChange(value: String) = _ui.update { it.copy(firstNames = value, errorRes = null) }
    fun onLastNamesChange(value: String) = _ui.update { it.copy(lastNames = value, errorRes = null) }

    fun onToggleGuardian(uid: String) = _ui.update {
        val next = if (uid in it.selectedGuardianUids) it.selectedGuardianUids - uid else it.selectedGuardianUids + uid
        it.copy(selectedGuardianUids = next)
    }

    fun onErrorShown() = _ui.update { it.copy(errorRes = null) }

    fun save() {
        val current = _ui.value
        if (current.saving || current.loading) return
        _ui.update { it.copy(saving = true, errorRes = null) }
        viewModelScope.launch {
            val input = StudentInput(current.studentNumber, current.firstNames, current.lastNames)
            when (val saved = saveStudent(studentId, input)) {
                is AppResult.Error -> _ui.update { it.copy(saving = false, errorRes = saved.reason.messageRes()) }
                is AppResult.Ok -> when (val linked = setGuardians(saved.value, current.selectedGuardianUids.toList())) {
                    is AppResult.Error -> _ui.update { it.copy(saving = false, errorRes = linked.reason.messageRes()) }
                    is AppResult.Ok -> _ui.update { it.copy(saving = false, saved = true) }
                }
            }
        }
    }
}
