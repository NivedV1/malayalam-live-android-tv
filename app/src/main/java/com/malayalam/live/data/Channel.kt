package com.malayalam.live.data

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val category: String,
    val tvgId: String? = null,
    val userAgent: String? = null,
)
