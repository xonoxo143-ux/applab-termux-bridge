package com.applab.termuxbridge.ui

import android.content.Context

data class ActionCurationState(
    val pinnedIds: Set<String> = emptySet(),
    val hiddenIds: Set<String> = emptySet(),
    val customizeMode: Boolean = false
) {
    fun isPinned(actionId: String): Boolean = pinnedIds.contains(actionId)
    fun isHidden(actionId: String): Boolean = hiddenIds.contains(actionId)
}

class BridgeActionCurationStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): ActionCurationState {
        return ActionCurationState(
            pinnedIds = prefs.getStringSet(KEY_PINNED_IDS, emptySet()).orEmpty(),
            hiddenIds = prefs.getStringSet(KEY_HIDDEN_IDS, emptySet()).orEmpty(),
            customizeMode = prefs.getBoolean(KEY_CUSTOMIZE_MODE, false)
        )
    }

    fun save(state: ActionCurationState) {
        prefs.edit()
            .putStringSet(KEY_PINNED_IDS, state.pinnedIds)
            .putStringSet(KEY_HIDDEN_IDS, state.hiddenIds)
            .putBoolean(KEY_CUSTOMIZE_MODE, state.customizeMode)
            .apply()
    }

    fun setCustomizeMode(enabled: Boolean): ActionCurationState {
        val next = load().copy(customizeMode = enabled)
        save(next)
        return next
    }

    fun togglePin(actionId: String): ActionCurationState {
        val current = load()
        val nextPinned = if (current.pinnedIds.contains(actionId)) {
            current.pinnedIds - actionId
        } else {
            current.pinnedIds + actionId
        }
        val next = current.copy(pinnedIds = nextPinned)
        save(next)
        return next
    }

    fun hide(actionId: String): ActionCurationState {
        val current = load()
        val next = current.copy(hiddenIds = current.hiddenIds + actionId)
        save(next)
        return next
    }

    fun unhide(actionId: String): ActionCurationState {
        val current = load()
        val next = current.copy(hiddenIds = current.hiddenIds - actionId)
        save(next)
        return next
    }

    fun reset(): ActionCurationState {
        val next = ActionCurationState()
        save(next)
        return next
    }

    companion object {
        private const val PREFS = "applab_bridge_action_curation"
        private const val KEY_PINNED_IDS = "pinned_action_ids"
        private const val KEY_HIDDEN_IDS = "hidden_action_ids"
        private const val KEY_CUSTOMIZE_MODE = "customize_mode"
    }
}
