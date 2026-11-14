package com.malayalam.live.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class ChannelRepository(context: Context) {
    private val applicationContext = context.applicationContext
    private val cacheFile = applicationContext.getFileStreamPath(CACHE_FILE)

    suspend fun loadChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val preferredChannels = applicationContext.assets.open(LOCAL_PLAYLIST_ASSET).bufferedReader(Charsets.UTF_8).use {
            M3uParser.parse(it.readText())
        }
        check(preferredChannels.isNotEmpty()) { "The bundled Malayalam channel list is empty." }

        val fallbackChannels = runCatching { loadOnlineChannels() }
            .getOrElse {
                runCatching { cacheFile.readText(Charsets.UTF_8) }
                    .getOrNull()
                    ?.let(M3uParser::parse)
                    .orEmpty()
            }

        mergePreferredChannels(preferredChannels, fallbackChannels)
    }

    private fun loadOnlineChannels(): List<Channel> {
        val connection = (URL(PLAYLIST_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "MalayalamLiveAndroidTV/1.0")
        }
        val contents = try {
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
        val channels = M3uParser.parse(contents)
        check(channels.isNotEmpty()) { "The online channel list was empty." }
        cacheFile.writeText(contents, Charsets.UTF_8)
        return channels
    }

    private fun mergePreferredChannels(preferred: List<Channel>, fallback: List<Channel>): List<Channel> {
        val preferredTvgIds = preferred.mapNotNull(Channel::tvgId).toSet()
        val preferredIds = preferred.map(Channel::id).toSet()
        val supplemental = fallback.filter { channel ->
            val tvgId = channel.tvgId
            if (tvgId != null) tvgId !in preferredTvgIds else channel.id !in preferredIds
        }
        return (preferred + supplemental).distinctBy(Channel::id)
    }

    private companion object {
        const val CACHE_FILE = "malayalam_channels.m3u"
        const val LOCAL_PLAYLIST_ASSET = "malayalam_verified_1080p.m3u"
        const val PLAYLIST_URL = "https://iptv-org.github.io/iptv/languages/mal.m3u"
    }
}
