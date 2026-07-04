package com.escalachurch.app.ui.screens.bulletins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.BulletinRepository
import com.escalachurch.app.domain.model.Bulletin
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BulletinsViewModel(repository: BulletinRepository) : ViewModel() {

    val bulletins: StateFlow<List<Bulletin>> = repository.observeActive()
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
