package com.malayalam.live.data

import android.content.Context
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream

data class TvProgram(
    val title: String,
    val startsAt: Long,
    val endsAt: Long,
)

data class ChannelSchedule(
    val current: TvProgram? = null,
    val next: TvProgram? = null,
)

/** Loads the India XMLTV guide and keeps only programmes belonging to this app's playlist. */
class EpgRepository(context: Context) {
    private val cacheFile: File = context.applicationContext.getFileStreamPath(CACHE_FILE)

    suspend fun loadSchedules(channels: List<Channel>): Map<String, ChannelSchedule> = withContext(Dispatchers.IO) {
        val cached = readCache()
        if (cached != null && System.currentTimeMillis() - cached.first < CACHE_TTL_MS) return@withContext cached.second

        runCatching { downloadAndParse(channels) }
            .onSuccess(::writeCache)
            .getOrElse { cached?.second.orEmpty() }
    }

    private fun downloadAndParse(channels: List<Channel>): Map<String, ChannelSchedule> {
        val connection = (URL(EPG_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 25_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "MalayalamLiveAndroidTV/1.0")
            setRequestProperty("Accept", "application/gzip")
        }
        try {
            connection.inputStream.use { raw ->
                GZIPInputStream(raw).use { gzip ->
                    return parseXmltv(gzip, channels)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseXmltv(input: java.io.InputStream, channels: List<Channel>): Map<String, ChannelSchedule> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, null)
        }
        val byEpgId = mutableMapOf<String, MutableList<Channel>>()
        val events = mutableMapOf<String, MutableList<TvProgram>>()
        val now = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply { isLenient = false }

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "channel") {
                val epgId = parser.getAttributeValue(null, "id").orEmpty()
                val displayNames = mutableListOf<String>()
                var depth = 1
                while (depth > 0 && parser.next() != XmlPullParser.END_DOCUMENT) {
                    when (parser.eventType) {
                        XmlPullParser.START_TAG -> {
                            depth++
                            if (parser.name == "display-name") displayNames += parser.nextText().orEmpty()
                                .also { depth-- }
                        }
                        XmlPullParser.END_TAG -> depth--
                    }
                }
                if (epgId.isNotBlank()) {
                    channels.filter { channel -> matches(channel, epgId, displayNames) }
                        .forEach { channel -> byEpgId.getOrPut(epgId) { mutableListOf() } += channel }
                }
            } else if (event == XmlPullParser.START_TAG && parser.name == "programme") {
                val epgId = parser.getAttributeValue(null, "channel").orEmpty()
                val matchedChannels = byEpgId[epgId].orEmpty()
                if (matchedChannels.isNotEmpty()) {
                    val start = parseTime(parser.getAttributeValue(null, "start"), timeFormat)
                    val end = parseTime(parser.getAttributeValue(null, "stop"), timeFormat)
                    var title = ""
                    var depth = 1
                    while (depth > 0 && parser.next() != XmlPullParser.END_DOCUMENT) {
                        when (parser.eventType) {
                            XmlPullParser.START_TAG -> {
                                depth++
                                if (parser.name == "title") title = parser.nextText().orEmpty().trim().ifBlank { title }
                                    .also { depth-- }
                            }
                            XmlPullParser.END_TAG -> depth--
                        }
                    }
                    if (start != null && end != null && end > now && title.isNotBlank()) {
                        val program = TvProgram(title, start, end)
                        matchedChannels.forEach { channel -> events.getOrPut(channel.id) { mutableListOf() } += program }
                    }
                }
            }
            event = parser.next()
        }

        return events.mapValues { (_, programs) ->
            val current = programs.filter { it.startsAt <= now && it.endsAt > now }.maxByOrNull(TvProgram::startsAt)
            val next = programs.filter { it.startsAt > now }.minByOrNull(TvProgram::startsAt)
            ChannelSchedule(current, next)
        }
    }

    private fun matches(channel: Channel, epgId: String, displayNames: List<String>): Boolean {
        val tvgId = channel.tvgId.orEmpty()
        if (tvgId.isBlank()) return false
        val streamId = normalizeId(tvgId)
        val guideId = normalizeId(epgId)
        val idMatch = streamId == guideId || guideId == "${streamId}in"
        if (!idMatch) return false

        val wantedName = normalizeName(channel.name)
        val guideNames = displayNames.map(::normalizeName).map { it.removePrefix("in") }
        // Avoid associating similarly named HD/SD variants with the wrong guide channel.
        return guideNames.isEmpty() || guideNames.any { it == wantedName } ||
            (streamId == guideId && guideNames.any { it.contains(wantedName) || wantedName.contains(it) })
    }

    private fun normalizeId(value: String): String = value.substringBefore('@')
        .lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

    private fun normalizeName(value: String): String = value.substringBefore("(")
        .replace(Regex("\\b(HD|SD|UHD|FHD|4K)\\b", RegexOption.IGNORE_CASE), "")
        .replace("+", " plus ")
        .lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

    private fun parseTime(value: String?, format: SimpleDateFormat): Long? = runCatching {
        value?.trim()?.let { format.parse(it)?.time }
    }.getOrNull()

    private fun readCache(): Pair<Long, Map<String, ChannelSchedule>>? = runCatching {
        val lines = cacheFile.readLines(Charsets.UTF_8)
        val updatedAt = lines.first().toLong()
        val schedules = lines.drop(1).mapNotNull { line ->
            val fields = line.split('\t')
            if (fields.size != 7) return@mapNotNull null
            val key = decode(fields[0])
            val current = decodeProgram(fields, 1)
            val next = decodeProgram(fields, 4)
            key to ChannelSchedule(current, next)
        }.toMap()
        updatedAt to schedules
    }.getOrNull()

    private fun writeCache(schedules: Map<String, ChannelSchedule>) {
        runCatching {
            val lines = mutableListOf(System.currentTimeMillis().toString())
            schedules.forEach { (id, schedule) ->
                lines += listOf(encode(id), *encodeProgram(schedule.current), *encodeProgram(schedule.next)).joinToString("\t")
            }
            cacheFile.writeText(lines.joinToString("\n"), Charsets.UTF_8)
        }
    }

    private fun encodeProgram(program: TvProgram?): Array<String> = if (program == null) {
        arrayOf("", "0", "0")
    } else arrayOf(encode(program.title), program.startsAt.toString(), program.endsAt.toString())

    private fun decodeProgram(fields: List<String>, offset: Int): TvProgram? {
        val title = decode(fields[offset])
        if (title.isBlank()) return null
        return TvProgram(title, fields[offset + 1].toLong(), fields[offset + 2].toLong())
    }

    private fun encode(value: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun decode(value: String): String = String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)

    private companion object {
        const val CACHE_FILE = "india_epg_schedule.tsv"
        const val EPG_URL = "https://iptv-epg.org/files/epg-in.xml.gz"
        const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L
    }
}
