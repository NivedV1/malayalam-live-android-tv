package com.malayalam.live.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {
    @Test
    fun parsesChannelMetadataAndNormalizesCategory() {
        val channels = M3uParser.parse(
            """#EXTM3U
                #EXTINF:-1 tvg-id="news.one" tvg-name="News One" tvg-logo="https://example.com/logo.png" group-title="Kerala News",News One HD
                https://example.com/news.m3u8
            """.trimIndent(),
        )

        assertEquals(1, channels.size)
        assertEquals("News One HD", channels.single().name)
        assertEquals("news.one-" + "https://example.com/news.m3u8".hashCode().toUInt().toString(16), channels.single().id)
        assertEquals("https://example.com/logo.png", channels.single().logoUrl)
        assertEquals("News", channels.single().category)
    }

    @Test
    fun preservesMultipleStreamVariantsForTheSameChannelId() {
        val channels = M3uParser.parse(
            """#EXTM3U
                #EXTINF:-1 tvg-id="asianet" group-title="Entertainment",Asianet HD
                https://example.com/hd.m3u8
                #EXTINF:-1 tvg-id="asianet" group-title="Entertainment",Asianet SD
                https://example.com/sd.m3u8
            """.trimIndent(),
        )

        assertEquals(2, channels.size)
        assertTrue(channels[0].id != channels[1].id)
        assertEquals(listOf("Asianet HD", "Asianet SD"), channels.map(Channel::name))
    }

    @Test
    fun ignoresCommentsAndNonHttpEntries() {
        val channels = M3uParser.parse(
            """#EXTM3U
                #EXTINF:-1,Invalid local channel
                file:///tmp/video.ts
                #EXTINF:-1 tvg-name="Valid",Valid Channel
                https://example.com/live.m3u8
            """.trimIndent(),
        )

        assertEquals(1, channels.size)
        assertEquals("Valid Channel", channels.single().name)
    }
}
