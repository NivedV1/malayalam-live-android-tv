package com.malayalam.live

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import com.malayalam.live.ui.GuideScreen
import com.malayalam.live.ui.GuideViewModel
import com.malayalam.live.ui.MalayalamLiveTheme
import com.malayalam.live.ui.PlayerScreen

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()

        setContent {
            val model: GuideViewModel = viewModel()
            val state by model.uiState.collectAsStateWithLifecycle()

            MalayalamLiveTheme {
                val channel = state.playingChannel
                if (channel == null) {
                    GuideScreen(
                        state = state,
                        onPlay = model::play,
                        onToggleFavorite = model::toggleFavorite,
                        onRefresh = model::refreshChannels,
                    )
                } else {
                    PlayerScreen(
                        channel = channel,
                        player = player,
                        retryToken = state.retryToken,
                        onBack = {
                            player.pause()
                            player.clearMediaItems()
                            model.closePlayer()
                        },
                        onRetry = model::retryPlayback,
                    )
                }
            }
        }
    }

    override fun onStop() {
        player.pause()
        super.onStop()
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}
