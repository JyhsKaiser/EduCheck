package com.jyhs.educheck.feature_admin.grades

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.model.GradeImportPlan
import com.jyhs.educheck.domain.usecase.ImportGradesUseCase
import com.jyhs.educheck.domain.usecase.PrepareGradeImportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GradesImportUiState(
    val plan: GradeImportPlan? = null,
    val reading: Boolean = false,
    val importing: Boolean = false,
    @StringRes val messageRes: Int? = null,
    /** Text inserted in the message (for example how many grades were imported). */
    val messageArg: String? = null,
)

@HiltViewModel
class GradesImportViewModel @Inject constructor(
    private val prepareImport: PrepareGradeImportUseCase,
    private val importGrades: ImportGradesUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(GradesImportUiState())
    val ui: StateFlow<GradesImportUiState> = _ui.asStateFlow()

    fun onFilePicked(uri: String) {
        if (_ui.value.reading || _ui.value.importing) return
        _ui.update { it.copy(reading = true, plan = null) }
        viewModelScope.launch {
            when (val result = prepareImport(uri)) {
                is AppResult.Ok -> _ui.update { it.copy(reading = false, plan = result.value) }
                is AppResult.Error -> _ui.update {
                    it.copy(reading = false, messageRes = result.reason.messageRes(), messageArg = null)
                }
            }
        }
    }

    fun onImport() {
        val plan = _ui.value.plan ?: return
        if (_ui.value.importing || plan.records.isEmpty()) return
        _ui.update { it.copy(importing = true) }
        viewModelScope.launch {
            when (val result = importGrades(plan.records)) {
                is AppResult.Ok -> _ui.update {
                    it.copy(
                        importing = false,
                        plan = null,
                        messageRes = R.string.info_grades_imported,
                        messageArg = plan.records.size.toString(),
                    )
                }
                is AppResult.Error -> _ui.update {
                    it.copy(importing = false, messageRes = result.reason.messageRes(), messageArg = null)
                }
            }
        }
    }

    fun onMessageShown() = _ui.update { it.copy(messageRes = null, messageArg = null) }
}
