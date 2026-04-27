package com.applab.termuxbridge.bridge

import org.json.JSONObject
import java.io.File

data class BridgeResult(
    val schemaVersion: Int = 0,
    val runId: String = "",
    val action: String = "",
    val status: String = "unknown",
    val title: String = "No result loaded",
    val summary: String = "Pick the bridge folder, run an action, then reload the saved result file.",
    val exitCode: Int? = null,
    val reportFile: String? = null,
    val logFile: String? = null,
    val nextAction: String? = null,
    val diagnosticHint: String? = null,
    val startedAt: String? = null,
    val finishedAt: String? = null,
    val artifacts: List<String> = emptyList(),
    val repoPath: String? = null,
    val repoName: String? = null,
    val branch: String? = null,
    val dirty: Boolean? = null,
    val changedFiles: Int? = null,
    val stagedFiles: Int? = null,
    val unstagedFiles: Int? = null,
    val untrackedFiles: Int? = null,
    val ahead: Int? = null,
    val behind: Int? = null,
    val currentCommit: String? = null,
    val currentCommitMessage: String? = null,
    val upstream: String? = null,
    val remoteUrl: String? = null,
    val hasPatchFile: Boolean? = null
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

    val changeBreakdownLabel: String
        get() {
            val parts = listOfNotNull(
                stagedFiles?.let { "staged $it" },
                unstagedFiles?.let { "unstaged $it" },
                untrackedFiles?.let { "untracked $it" }
            )
            return parts.joinToString(" · ").ifBlank { "change details unknown" }
        }

    val patchLabel: String
        get() = when (hasPatchFile) {
            true -> "patch.sh found"
            false -> "patch.sh missing"
            null -> "patch state unknown"
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
            summary = "Pick Documents/AppLabBridge before running or reading actions.",
            diagnosticHint = "Choose the shared bridge folder from Android storage, not a Termux-private path."
        )

        fun missingResult(): BridgeResult = BridgeResult(
            status = "missing",
            title = "No result file found",
            summary = "The selected folder does not contain results/latest_result.json.",
            diagnosticHint = "Run a Termux action, then reload the saved result file. If Termux already wrote a result, re-pick Documents/AppLabBridge."
        )

        fun invalidJson(message: String): BridgeResult = BridgeResult(
            status = "failed",
            title = "Invalid result JSON",
            summary = message,
            diagnosticHint = "The result file exists but could not be parsed. Open the report/log or delete results/latest_result.json and rerun an action."
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
                    diagnosticHint = json.optNullableString("diagnostic_hint"),
                    startedAt = json.optNullableString("started_at"),
                    finishedAt = json.optNullableString("finished_at"),
                    artifacts = artifacts,
                    repoPath = json.optNullableString("repo_path"),
                    repoName = json.optNullableString("repo_name"),
                    branch = json.optNullableString("branch"),
                    dirty = json.optNullableBoolean("dirty"),
                    changedFiles = json.optNullableInt("changed_files"),
                    stagedFiles = json.optNullableInt("staged_files"),
                    unstagedFiles = json.optNullableInt("unstaged_files"),
                    untrackedFiles = json.optNullableInt("untracked_files"),
                    ahead = json.optNullableInt("ahead"),
                    behind = json.optNullableInt("behind"),
                    currentCommit = json.optNullableString("current_commit"),
                    currentCommitMessage = json.optNullableString("current_commit_message"),
                    upstream = json.optNullableString("upstream"),
                    remoteUrl = json.optNullableString("remote_url"),
                    hasPatchFile = json.optNullableBoolean("has_patch_file")
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
