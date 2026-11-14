package com.malayalam.live.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.malayalam.live.data.Channel
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

@Composable
fun PlayerScreen(
    channel: Channel,
    player: ExoPlayer,
    retryToken: Int,
    onUserAgent: (String?) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    var playbackError by remember(channel.id) { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)

    DisposableEffect(player, channel.id) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.localizedMessage ?: "This stream is temporarily unavailable."
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(channel.id, retryToken) {
        playbackError = null
        onUserAgent(channel.userAgent)
        player.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        player.prepare()
        player.playWhenReady = true
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = true
                    controllerAutoShow = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(start = 48.dp, top = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(channel.name, color = Color.White, fontSize = 26.sp)
            Spacer(Modifier.width(14.dp))
            Text("LIVE", color = Color(0xFFA8C7FA), fontSize = 14.sp)
        }

        if (playbackError != null) {
            Column(
                modifier = Modifier.align(Alignment.Center).background(Color(0xEE141B23)).padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Unable to play this channel", color = Color.White, fontSize = 25.sp)
                Spacer(Modifier.height(10.dp))
                Text(playbackError.orEmpty(), color = Color(0xFFBFC8D5), fontSize = 16.sp)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(onClick = onRetry, modifier = Modifier.height(52.dp)) {
                        Box(Modifier.background(Color(0xFFA8C7FA)).padding(horizontal = 23.dp), contentAlignment = Alignment.Center) {
                            Text("Retry", color = Color(0xFF10233F), fontSize = 16.sp)
                        }
                    }
                    Surface(onClick = onBack, modifier = Modifier.height(52.dp)) {
                        Box(Modifier.background(Color(0xFF263442)).padding(horizontal = 23.dp), contentAlignment = Alignment.Center) {
                            Text("Back to guide", color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
