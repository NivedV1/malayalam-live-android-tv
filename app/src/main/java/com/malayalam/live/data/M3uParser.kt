package com.malayalam.live.data

object M3uParser {
    private val attributePattern = Regex("([\\w-]+)=\"([^\"]*)\"")

    fun parse(contents: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        var pending: PendingChannel? = null

        contents.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val attributes = attributePattern.findAll(line)
                        .associate { it.groupValues[1].lowercase() to it.groupValues[2].trim() }
                    val separator = findTitleSeparator(line)
                    val name = if (separator >= 0) line.substring(separator + 1).trim() else ""
                        .ifBlank { attributes["tvg-name"].orEmpty() }
                    pending = PendingChannel(
                        name = name,
                        id = attributes["tvg-id"].orEmpty(),
                        logoUrl = attributes["tvg-logo"]?.takeIf(String::isNotBlank),
                        category = normalizeCategory(attributes["group-title"].orEmpty()),
                    )
                }
                line.isNotBlank() && !line.startsWith('#') && pending != null -> {
                    val streamUrl = line.substringBefore('|').trim()
                    val pendingChannel = pending
                    if (pendingChannel != null && (streamUrl.startsWith("http://") || streamUrl.startsWith("https://"))) {
                        val name = pendingChannel.name.ifBlank { streamUrl.substringAfterLast('/').substringBefore('?') }
                        val stableId = "${pendingChannel.id.ifBlank { name.lowercase().replace(Regex("[^a-z0-9]+"), "-") }}-${streamUrl.hashCode().toUInt().toString(16)}"
                        channels += Channel(
                            id = stableId,
                            name = name,
                            streamUrl = streamUrl,
                            logoUrl = pendingChannel.logoUrl,
                            category = pendingChannel.category,
                        )
                    }
                    pending = null
                }
            }
        }

        return channels.distinctBy(Channel::id)
    }

    private fun findTitleSeparator(line: String): Int {
        var insideQuotes = false
        line.forEachIndexed { index, character ->
            when (character) {
                '"' -> insideQuotes = !insideQuotes
                ',' -> if (!insideQuotes) return index
            }
        }
        return -1
    }

    private fun normalizeCategory(raw: String): String {
        val value = raw.trim()
        if (value.isBlank()) return "Other"
        val lower = value.lowercase()
        return when {
            "news" in lower -> "News"
            "entertainment" in lower || "general" in lower -> "Entertainment"
            "movie" in lower || "film" in lower -> "Movies"
            "music" in lower -> "Music"
            "kid" in lower || "cartoon" in lower -> "Kids"
            "relig" in lower || "spiritual" in lower -> "Religious"
            "documentary" in lower || "travel" in lower -> "Documentary"
            else -> value
        }
    }

    private data class PendingChannel(
        val name: String,
        val id: String,
        val logoUrl: String?,
        val category: String,
    )
}
