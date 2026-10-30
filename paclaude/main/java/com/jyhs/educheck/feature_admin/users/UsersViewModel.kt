package com.jyhs.educheck.feature_admin.users

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.model.AppUser
import com.jyhs.educheck.domain.usecase.ObserveUsersUseCase
import com.jyhs.educheck.domain.usecase.SetUserActiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UsersUiState(
    val users: List<AppUser> = emptyList(),
    val loading: Boolean = true,
    @StringRes val errorRes: Int? = null,
)

@HiltViewModel
class UsersViewModel @Inject constructor(
    observeUsers: ObserveUsersUseCase,
    private val setUserActive: SetUserActiveUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(UsersUiState())
    val ui: StateFlow<UsersUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            observeUsers()
                .catch { _ui.update { it.copy(loading = false, errorRes = R.string.error_unknown) } }
                .collect { list -> _ui.update { it.copy(users = list, loading = false) } }
        }
    }

    fun onToggleActive(user: AppUser) {
        viewModelScope.launch {
            val result = setUserActive(user.uid, !user.active)
            if (result is AppResult.Error) _ui.update { it.copy(errorRes = result.reason.messageRes()) }
        }
    }

    fun onErrorShown() = _ui.update { it.copy(errorRes = null) }
}
