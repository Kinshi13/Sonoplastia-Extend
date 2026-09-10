package com.escalachurch.app.ui.screens.worship

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escalachurch.app.data.repository.WorshipSongRepository
import com.escalachurch.app.domain.model.WorshipSong
import com.escalachurch.app.domain.util.YoutubeLinkParser
import com.escalachurch.app.security.AdminSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class WorshipUiState(
    val isAdmin: Boolean = false,
    val recommendation: WorshipSong? = null,
    val currentProgram: List<WorshipSong> = emptyList(),
    val nextProgram: List<WorshipSong> = emptyList(),
    val isLoading: Boolean = true,
    val hasLoadError: Boolean = false
)

class WorshipViewModel(
    private val repository: WorshipSongRepository,
    private val adminSession: AdminSession
) : ViewModel() {

    // Same shape as AnnouncementViewModel.uiState: keyed on isAdmin via flatMapLatest, so
    // switching admin mode swaps between the published-only feed and the full admin feed.
    val uiState: StateFlow<WorshipUiState> = adminSession.isUnlocked
        .flatMapLatest { isAdmin ->
            val source = if (isAdmin) repository.observeAllForAdmin() else repository.observePublished()
            source.map { songs -> buildUiState(songs, isAdmin) }
        }
        .catch { emit(WorshipUiState(isLoading = false, hasLoadError = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorshipUiState())

    private fun buildUiState(songs: List<WorshipSong>, isAdmin: Boolean): WorshipUiState {
        val today = LocalDate.now()
        val recommendation = songs.firstOrNull { it.isTodaysRecommendation(today) }
        val nonRecommendation = songs.filter { it != recommendation }
        val current = nonRecommendation.filter { it.programDate == today }
        val next = nonRecommendation
            .filter { it.programDate != null && it.programDate!!.isAfter(today) }
            .sortedBy { it.programDate }
        return WorshipUiState(
            isAdmin = isAdmin,
            recommendation = recommendation,
            currentProgram = current,
            nextProgram = next,
            isLoading = false
        )
    }

    fun save(song: WorshipSong, onDone: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = runCatching { repository.save(song) }
            onDone(result.map {})
        }
    }

    fun setPublished(song: WorshipSong, isPublished: Boolean) {
        viewModelScope.launch { runCatching { repository.setPublished(song, isPublished) } }
    }

    fun delete(song: WorshipSong) {
        viewModelScope.launch { runCatching { repository.delete(song) } }
    }

    /** Bloco A2 - resolves whatever the admin pasted into a playable video id + thumbnail, or
     *  null if it isn't a recognized YouTube link. */
    fun resolveYoutubeLink(url: String): Pair<String, String>? {
        val videoId = YoutubeLinkParser.extractVideoId(url) ?: return null
        return videoId to YoutubeLinkParser.thumbnailUrl(videoId)
    }
}
