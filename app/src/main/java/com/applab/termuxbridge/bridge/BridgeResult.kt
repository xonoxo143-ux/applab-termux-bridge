package com.applab.termuxbridge.bridge

import org.json.JSONObject
import java.io.File

data class BridgeResult(
    val schemaVersion: Int = 0,
    val runId: String = "",
    val action: String = "",
    val status: String = "unknown",
    val title: String = "No result loaded",
    val summary: String = "Pick the bridge folder, run an action, then refresh the result.",
    val exitCode: Int? = null,
    val reportFile: String? = null,
    val logFile: String? = null,
    val nextAction: String? = null,
    val artifacts: List<String> = emptyList(),
    val repoPath: String? = null,
    val repoName: String? = null,
    val branch: String? = null,
    val dirty: Boolean? = null,
    val changedFiles: Int? = null,
    val ahead: Int? = null,
    val behind: Int? = null
) {
    val isLoaded: Boolean get() = runId.isNotBlank()

    val repoLabel: String
        get() = listOfNotNull(repoName, branch).joinToString(" · ").ifBlank { "No repo loaded" }

    val stateLabel: String
        get() = when {
            dirty == true -> "changed${changedFiles?.let { " · $it file(s)" } ?: ""}"
            ahead != null && ahead > 0 && behind != null && behind > 0 -> "diverged"
            ahead != null && ahead > 0 -> "ahead $ahead"
            behind != null && behind > 0 -> "behind $behind"
            dirty == false -> "clean"
            else -> "unknown"
        }

    fun reportFileName(): String {
        val explicit = reportFile?.substringAfterLast('/')?.substringAfterLast(File.separatorChar)?.takeIf { it.isNotBlank() }
        if (explicit != null) return explicit
        return BridgeAction.fromId(action)?.expectedReportName ?: "latest_result.json"
    }

    companion object {
        fun missingFolder(): BridgeResult = BridgeResult(
            status = "missing",
            title = "No bridge folder selected",
            summary = "Pick Documents/AppLabBridge before running or reading actions."
        )

        fun missingResult(): BridgeResult = BridgeResult(
            status = "missing",
            title = "No result file found",
            summary = "The selected folder does not contain results/latest_result.json. Termux may be writing to a different AppLabBridge folder, or no action has run yet."
        )

        fun invalidJson(message: String): BridgeResult = BridgeResult(
            status = "failed",
            title = "Invalid result JSON",
            summary = message
        )

        fun fromJson(text: String): BridgeResult {
            return try {
                val json = JSONObject(text)
                val artifacts = mutableListOf<String>()
                val artifactArray = json.optJSONArray("artifacts")
                if (artifactArray != null) {
                    for (index in 0 until artifactArray.length()) {
                        artifacts += artifactArray.optString(index)
                    }
                }
                BridgeResult(
                    schemaVersion = json.optInt("schema_version", 0),
                    runId = json.optString("run_id", ""),
                    action = json.optString("action", ""),
                    status = json.optString("status", "unknown"),
                    title = json.optString("title", "Result loaded"),
                    summary = json.optString("summary", ""),
                    exitCode = if (json.has("exit_code")) json.optInt("exit_code") else null,
                    reportFile = json.optNullableString("report_file"),
                    logFile = json.optNullableString("log_file"),
                    nextAction = json.optNullableString("next_action"),
                    artifacts = artifacts,
                    repoPath = json.optNullableString("repo_path"),
                    repoName = json.optNullableString("repo_name"),
                    branch = json.optNullableString("branch"),
                    dirty = json.optNullableBoolean("dirty"),
                    changedFiles = json.optNullableInt("changed_files"),
                    ahead = json.optNullableInt("ahead"),
                    behind = json.optNullableInt("behind")
                )
            } catch (error: Exception) {
                invalidJson(error.message ?: "Could not parse latest_result.json")
            }
        }
    }
}

private fun JSONObject.optNullableString(name: String): String? {
    if (!has(name) || isNull(name)) return null
    return optString(name).takeIf { it.isNotBlank() && it != "null" }
}

private fun JSONObject.optNullableBoolean(name: String): Boolean? {
    if (!has(name) || isNull(name)) return null
    return optBoolean(name)
}

private fun JSONObject.optNullableInt(name: String): Int? {
    if (!has(name) || isNull(name)) return null
    return optInt(name)
}
