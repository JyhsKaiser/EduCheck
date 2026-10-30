package com.jyhs.educheck.feature_admin.subjects

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.model.Subject
import com.jyhs.educheck.domain.model.SubjectInput
import com.jyhs.educheck.domain.usecase.DeleteSubjectUseCase
import com.jyhs.educheck.domain.usecase.ObserveSubjectsUseCase
import com.jyhs.educheck.domain.usecase.SaveSubjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Form of the add/edit dialog. [id] == null means a new subject. */
data class SubjectForm(
    val id: String? = null,
    val code: String = "",
    val name: String = "",
    val saving: Boolean = false,
)

data class SubjectsUiState(
    val subjects: List<Subject> = emptyList(),
    val loading: Boolean = true,
    val form: SubjectForm? = null,
    @StringRes val errorRes: Int? = null,
)

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    observeSubjects: ObserveSubjectsUseCase,
    private val saveSubject: SaveSubjectUseCase,
    private val deleteSubject: DeleteSubjectUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(SubjectsUiState())
    val ui: StateFlow<SubjectsUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            observeSubjects()
                .catch { _ui.update { it.copy(loading = false, errorRes = R.string.error_unknown) } }
                .collect { list -> _ui.update { it.copy(subjects = list, loading = false) } }
        }
    }

    fun onAdd() = _ui.update { it.copy(form = SubjectForm()) }

    fun onEdit(subject: Subject) = _ui.update {
        it.copy(form = SubjectForm(id = subject.id, code = subject.code, name = subject.name))
    }

    fun onDismissForm() = _ui.update { it.copy(form = null) }

    fun onCodeChange(value: String) = _ui.update { state -> state.copy(form = state.form?.copy(code = value)) }

    fun onNameChange(value: String) = _ui.update { state -> state.copy(form = state.form?.copy(name = value)) }

    fun onSave() {
        val form = _ui.value.form ?: return
        if (form.saving) return
        _ui.update { it.copy(form = form.copy(saving = true)) }
        viewModelScope.launch {
            when (val result = saveSubject(form.id, SubjectInput(form.code, form.name))) {
                is AppResult.Ok -> _ui.update { it.copy(form = null) }
                is AppResult.Error -> _ui.update {
                    it.copy(form = form.copy(saving = false), errorRes = result.reason.messageRes())
                }
            }
        }
    }

    fun onDelete() {
        val form = _ui.value.form ?: return
        val id = form.id ?: return
        if (form.saving) return
        _ui.update { it.copy(form = form.copy(saving = true)) }
        viewModelScope.launch {
            when (val result = deleteSubject(id)) {
                is AppResult.Ok -> _ui.update { it.copy(form = null) }
                is AppResult.Error -> _ui.update {
                    it.copy(form = form.copy(saving = false), errorRes = result.reason.messageRes())
                }
            }
        }
    }

    fun onErrorShown() = _ui.update { it.copy(errorRes = null) }
}
