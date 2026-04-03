package de.mm20.launcher2.preferences.search

import de.mm20.launcher2.preferences.LauncherDataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ClaudeSearchSettings internal constructor(
    private val dataStore: LauncherDataStore,
) {
    val enabled
        get() = dataStore.data.map { it.claudeSearchEnabled }.distinctUntilChanged()

    fun setEnabled(enabled: Boolean) {
        dataStore.update {
            it.copy(claudeSearchEnabled = enabled)
        }
    }

    val model
        get() = dataStore.data.map { it.claudeSearchModel }.distinctUntilChanged()

    fun setModel(model: String) {
        dataStore.update {
            it.copy(claudeSearchModel = model)
        }
    }

    val delayMs
        get() = dataStore.data.map { it.claudeSearchDelayMs }.distinctUntilChanged()

    fun setDelayMs(delayMs: Long) {
        dataStore.update {
            it.copy(claudeSearchDelayMs = delayMs)
        }
    }

    val minQueryLength
        get() = dataStore.data.map { it.claudeSearchMinQueryLength }.distinctUntilChanged()

    fun setMinQueryLength(length: Int) {
        dataStore.update {
            it.copy(claudeSearchMinQueryLength = length)
        }
    }
}
