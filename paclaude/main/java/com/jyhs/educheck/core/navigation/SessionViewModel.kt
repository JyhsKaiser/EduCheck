package com.jyhs.educheck.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.domain.model.Session
import com.jyhs.educheck.domain.usecase.ObserveSessionUseCase
import com.jyhs.educheck.domain.usecase.RestoreSessionUseCase
import com.jyhs.educheck.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SessionUi {
    data object Loading : SessionUi
    data object SignedOut : SessionUi
    data class Active(val session: Session) : SessionUi
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {

    val session: StateFlow<SessionUi> = observeSession()
        .map { s -> if (s == null) SessionUi.SignedOut else SessionUi.Active(s) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionUi.Loading)

    init {
        viewModelScope.launch { restoreSessionUseCase() }
    }

    fun signOut() {
        viewModelScope.launch { signOutUseCase() }
    }
}
