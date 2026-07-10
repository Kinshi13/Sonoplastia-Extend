package com.escalachurch.app.ui.stellacore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** What a Stella Core action asks the currently-visible screen to do. Screens that own the
 *  relevant local state (e.g. GeneralScaleScreen's `editingTarget`) collect the ones they care
 *  about via a `LaunchedEffect` and react by driving their own existing state - Stella Core never
 *  reaches into another screen's state directly. */
sealed interface StellaCoreCommand {
    data object NewPersonalEvent : StellaCoreCommand
    data object NewOfficialScale : StellaCoreCommand
    data object NewDoxology : StellaCoreCommand
    data object NewAnnouncement : StellaCoreCommand
    data object GoToTodayCalendar : StellaCoreCommand
    data object GoToTodayGeneralScale : StellaCoreCommand
    data object ToggleOnlyMyClassesGeneralScale : StellaCoreCommand
}

/** Same shape/spirit as [com.escalachurch.app.data.remote.LocalRefreshTrigger] - a fire-and-forget
 *  event stream, just for cross-screen commands instead of data refreshes. */
object StellaCoreBus {
    private val flow = MutableSharedFlow<StellaCoreCommand>(extraBufferCapacity = 1)
    fun send(command: StellaCoreCommand) { flow.tryEmit(command) }
    fun events(): Flow<StellaCoreCommand> = flow
}
