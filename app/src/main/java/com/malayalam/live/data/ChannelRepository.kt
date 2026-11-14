package com.malayalam.live.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class ChannelRepository(context: Context) {
    private val cacheFile = context.applicationContext.getFileStreamPath("malayalam_channels.m3u")

    suspend fun loadChannels(): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(PLAYLIST_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "MalayalamLiveAndroidTV/1.0")
            }
            val contents = try {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                connection.disconnect()
            }
            val parsed = M3uParser.parse(contents)
            if (parsed.isEmpty()) error("The channel playlist did not contain any playable streams.")
            cacheFile.writeText(contents, Charsets.UTF_8)
            parsed
        } catch (networkError: Exception) {
            val cached = runCatching { cacheFile.readText(Charsets.UTF_8) }.getOrNull()
            val parsed = cached?.let(M3uParser::parse).orEmpty()
            if (parsed.isNotEmpty()) parsed else throw networkError
        }
    }

    private companion object {
        const val PLAYLIST_URL = "https://iptv-org.github.io/iptv/languages/mal.m3u"
    }
}
