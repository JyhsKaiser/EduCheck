package com.jyhs.educheck.feature_admin.groups

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.domain.model.SchoolGroup
import com.jyhs.educheck.domain.usecase.ObserveGroupsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupsUiState(
    val groups: List<SchoolGroup> = emptyList(),
    val loading: Boolean = true,
    @StringRes val errorRes: Int? = null,
)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    observeGroups: ObserveGroupsUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(GroupsUiState())
    val ui: StateFlow<GroupsUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            observeGroups()
                .catch { _ui.update { it.copy(loading = false, errorRes = R.string.error_unknown) } }
                .collect { list -> _ui.update { it.copy(groups = list, loading = false) } }
        }
    }

    fun onErrorShown() = _ui.update { it.copy(errorRes = null) }
}
