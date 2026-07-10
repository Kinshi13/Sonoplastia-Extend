package com.escalachurch.app.ui.screens.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.PlanRepository
import com.escalachurch.app.entitlements.EntitlementService
import com.escalachurch.app.entitlements.Entitlements
import com.escalachurch.app.entitlements.Plan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PlansViewModel(
    planRepository: PlanRepository,
    entitlementService: EntitlementService
) : ViewModel() {

    val plans: StateFlow<List<Plan>> = planRepository.observePlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val current: StateFlow<Entitlements> = entitlementService.entitlements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), entitlementService.entitlements.value)
}
