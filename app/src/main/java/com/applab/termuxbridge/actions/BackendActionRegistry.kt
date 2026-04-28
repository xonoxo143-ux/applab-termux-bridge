package com.applab.termuxbridge.actions

import org.json.JSONArray
import org.json.JSONObject

data class BackendActionRegistry(
    val schemaVersion: Int,
    val generatedAt: String,
    val backendRepo: String,
    val backendCommit: String,
    val groups: List<String>,
    val riskLevels: List<String>,
    val actions: List<BackendActionDescriptor>
) {
    companion object {
        fun fromJson(text: String): BackendActionRegistry {
            val json = JSONObject(text)
            return BackendActionRegistry(
                schemaVersion = json.optInt("schema_version", 0),
                generatedAt = json.optString("generated_at", ""),
                backendRepo = json.optString("backend_repo", ""),
                backendCommit = json.optString("backend_commit", ""),
                groups = json.optJSONArray("groups").toStringList(),
                riskLevels = json.optJSONArray("risk_levels").toStringList(),
                actions = json.optJSONArray("actions").toActionList()
            )
        }
    }
}

data class BackendActionDescriptor(
    val id: String,
    val label: String,
    val group: String,
    val description: String,
    val risk: String,
    val visibleByDefault: Boolean,
    val confirm: Boolean,
    val sort: Int,
    val requiresRepo: Boolean,
    val requiresCleanTree: Boolean,
    val requiresPatchFile: Boolean,
    val requiresNetwork: Boolean,
    val advanced: Boolean,
    val parked: Boolean
) {
    val isRisky: Boolean
        get() = confirm || risk in setOf("mutating", "publishing", "install")

    val flags: List<String>
        get() = buildList {
            if (confirm) add("confirm")
            if (requiresRepo) add("repo")
            if (requiresCleanTree) add("clean-tree")
            if (requiresPatchFile) add("patch-file")
            if (requiresNetwork) add("network")
            if (advanced) add("advanced")
            if (parked) add("parked")
        }
}

sealed class BackendActionRegistryState {
    data object MissingFolder : BackendActionRegistryState()
    data object MissingRegistry : BackendActionRegistryState()
    data class ParseError(val message: String) : BackendActionRegistryState()
    data class Loaded(val registry: BackendActionRegistry) : BackendActionRegistryState()
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
}

private fun JSONArray?.toActionList(): List<BackendActionDescriptor> {
    if (this == null) return emptyList()
    return List(length()) { index -> optJSONObject(index) }
        .filterNotNull()
        .map { item ->
            BackendActionDescriptor(
                id = item.optString("id", ""),
                label = item.optString("label", item.optString("id", "")),
                group = item.optString("group", "Advanced Tools"),
                description = item.optString("description", ""),
                risk = item.optString("risk", "safe"),
                visibleByDefault = item.optBoolean("visible_by_default", true),
                confirm = item.optBoolean("confirm", false),
                sort = item.optInt("sort", 100),
                requiresRepo = item.optBoolean("requires_repo", false),
                requiresCleanTree = item.optBoolean("requires_clean_tree", false),
                requiresPatchFile = item.optBoolean("requires_patch_file", false),
                requiresNetwork = item.optBoolean("requires_network", false),
                advanced = item.optBoolean("advanced", false),
                parked = item.optBoolean("parked", false)
            )
        }
        .filter { it.id.isNotBlank() }
}
