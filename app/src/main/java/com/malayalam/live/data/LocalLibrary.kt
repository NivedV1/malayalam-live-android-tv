package com.malayalam.live.data

import android.content.Context

class LocalLibrary(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("malayalam_live_library", Context.MODE_PRIVATE)

    fun favorites(): Set<String> = preferences.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toSet()

    fun setFavorite(channelId: String, favorite: Boolean): Set<String> {
        val updated = favorites().toMutableSet().apply {
            if (favorite) add(channelId) else remove(channelId)
        }
        preferences.edit().putStringSet(KEY_FAVORITES, updated).apply()
        return updated
    }

    fun recent(): List<String> = preferences.getString(KEY_RECENT, "").orEmpty()
        .split('|').filter(String::isNotBlank)

    fun addRecent(channelId: String): List<String> {
        val updated = (listOf(channelId) + recent().filterNot { it == channelId }).take(12)
        preferences.edit().putString(KEY_RECENT, updated.joinToString("|")).apply()
        return updated
    }

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_RECENT = "recent"
    }
}
