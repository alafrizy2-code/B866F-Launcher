package com.b866f.launcher

import android.content.SharedPreferences

object FavoriteManager {

    private const val KEY_FAVORITES = "favorite_packages"

    fun getFavorites(prefs: SharedPreferences): MutableList<String> {
        val raw = prefs.getString(KEY_FAVORITES, "") ?: ""
        return if (raw.isBlank()) {
            mutableListOf()
        } else {
            raw.split(",").filter { it.isNotBlank() }.toMutableList()
        }
    }

    fun saveFavorites(prefs: SharedPreferences, packages: List<String>) {
        val value = packages.filter { it.isNotBlank() }.distinct().joinToString(",")
        prefs.edit().putString(KEY_FAVORITES, value).apply()
    }

    fun ensureDefaultFavorites(prefs: SharedPreferences, allPackageNames: List<String>) {
        val current = getFavorites(prefs)
        if (current.isNotEmpty()) return
        val defaults = allPackageNames.take(6)
        saveFavorites(prefs, defaults)
    }
}
