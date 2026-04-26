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
    val artifacts: List<String> = emptyList()
) {
    val isLoaded: Boolean get() = runId.isNotBlank()

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
                    artifacts = artifacts
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
