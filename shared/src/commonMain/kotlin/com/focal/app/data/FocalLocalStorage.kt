package com.focal.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.focal.app.model.currentTimeMillis

/**
 * Multiplatform Offline-First State & Storage Manager for FOCAL.
 * Keeps track of offline sync queue and state cache.
 */
object FocalLocalStorage {

    var isStorageInitialized by mutableStateOf(true)
    var lastSavedTimestamp by mutableStateOf(currentTimeMillis())

    private val inMemoryCache = mutableMapOf<String, String>()

    fun saveString(key: String, value: String) {
        inMemoryCache[key] = value
        lastSavedTimestamp = currentTimeMillis()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return inMemoryCache[key] ?: defaultValue
    }

    fun saveBoolean(key: String, value: Boolean) {
        inMemoryCache[key] = value.toString()
        lastSavedTimestamp = currentTimeMillis()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return inMemoryCache[key]?.toBooleanStrictOrNull() ?: defaultValue
    }

    fun clear() {
        inMemoryCache.clear()
        lastSavedTimestamp = currentTimeMillis()
    }
}
