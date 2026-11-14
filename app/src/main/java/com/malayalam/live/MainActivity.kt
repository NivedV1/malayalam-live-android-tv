package com.malayalam.live

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.malayalam.live.ui.GuideScreen
import com.malayalam.live.ui.GuideViewModel
import com.malayalam.live.ui.MalayalamLiveTheme
import com.malayalam.live.ui.PlayerScreen

@OptIn(UnstableApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    private lateinit var httpDataSourceFactory: DefaultHttpDataSource.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(DEFAULT_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()

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
                        schedule = state.schedules[channel.id],
                        player = player,
                        retryToken = state.retryToken,
                        onUserAgent = { userAgent ->
                            httpDataSourceFactory.setUserAgent(userAgent ?: DEFAULT_USER_AGENT)
                        },
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

    private companion object {
        const val DEFAULT_USER_AGENT = "MalayalamLiveAndroidTV/1.0"
    }
}
