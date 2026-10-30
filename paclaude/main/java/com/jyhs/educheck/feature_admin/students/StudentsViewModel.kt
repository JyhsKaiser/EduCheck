package com.jyhs.educheck.feature_admin.students

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.model.Student
import com.jyhs.educheck.domain.usecase.ObserveStudentsUseCase
import com.jyhs.educheck.domain.usecase.SetStudentActiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentsUiState(
    val students: List<Student> = emptyList(),
    val loading: Boolean = true,
    @StringRes val errorRes: Int? = null,
)

@HiltViewModel
class StudentsViewModel @Inject constructor(
    observeStudents: ObserveStudentsUseCase,
    private val setStudentActive: SetStudentActiveUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(StudentsUiState())
    val ui: StateFlow<StudentsUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            observeStudents()
                .catch { _ui.update { it.copy(loading = false, errorRes = R.string.error_unknown) } }
                .collect { list -> _ui.update { it.copy(students = list, loading = false) } }
        }
    }

    fun onToggleActive(student: Student) {
        viewModelScope.launch {
            val result = setStudentActive(student.id, !student.active)
            if (result is AppResult.Error) _ui.update { it.copy(errorRes = result.reason.messageRes()) }
        }
    }

    fun onErrorShown() = _ui.update { it.copy(errorRes = null) }
}
