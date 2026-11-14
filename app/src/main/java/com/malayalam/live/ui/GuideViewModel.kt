package com.malayalam.live.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.malayalam.live.data.Channel
import com.malayalam.live.data.ChannelRepository
import com.malayalam.live.data.ChannelSchedule
import com.malayalam.live.data.EpgRepository
import com.malayalam.live.data.LocalLibrary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuideUiState(
    val channels: List<Channel> = emptyList(),
    val schedules: Map<String, ChannelSchedule> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val recentIds: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val playingChannel: Channel? = null,
    val retryToken: Int = 0,
)

class GuideViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChannelRepository(application)
    private val epgRepository = EpgRepository(application)
    private val library = LocalLibrary(application)
    private val _uiState = MutableStateFlow(
        GuideUiState(favorites = library.favorites(), recentIds = library.recent()),
    )
    val uiState: StateFlow<GuideUiState> = _uiState.asStateFlow()

    init { refreshChannels() }

    fun refreshChannels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.loadChannels() }
                .onSuccess { channels ->
                    _uiState.update { it.copy(channels = channels, isLoading = false) }
                    viewModelScope.launch {
                        val schedules = epgRepository.loadSchedules(channels)
                        _uiState.update { it.copy(schedules = schedules) }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Could not load the channel guide.")
                    }
                }
        }
    }

    fun toggleFavorite(channel: Channel) {
        val isFavorite = channel.id !in _uiState.value.favorites
        val favorites = library.setFavorite(channel.id, isFavorite)
        _uiState.update { it.copy(favorites = favorites) }
    }

    fun play(channel: Channel) {
        val recentIds = library.addRecent(channel.id)
        _uiState.update { it.copy(playingChannel = channel, recentIds = recentIds) }
    }

    fun retryPlayback() {
        _uiState.update { it.copy(retryToken = it.retryToken + 1) }
    }

    fun closePlayer() {
        _uiState.update { it.copy(playingChannel = null) }
    }
}
