package com.applab.termuxbridge.ui

import android.net.Uri
import com.applab.termuxbridge.storage.SafBridgeFolder

data class BridgeRepoChoice(
    val name: String,
    val relativePath: String,
    val termuxPath: String
)

class BridgeRepoChoiceReader(private val bridgeFolder: SafBridgeFolder) {
    fun readChoices(treeUri: Uri?): List<BridgeRepoChoice> {
        if (treeUri == null) return emptyList()
        val report = bridgeFolder.readText(treeUri, listOf("reports", "list_projects.txt")).orEmpty()
        return report.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.startsWith("Projects under ") }
            .filterNot { it.startsWith("Action:") || it.startsWith("Run:") || it.startsWith("Started:") || it.startsWith("Time:") }
            .filterNot { it.startsWith(".") || it.contains("No such file", ignoreCase = true) }
            .distinct()
            .map { relative ->
                val clean = relative.trim('/').removeSuffix("/.git")
                BridgeRepoChoice(
                    name = clean.substringAfterLast('/'),
                    relativePath = clean,
                    termuxPath = "$TERMUX_PROJECTS_DIR/$clean"
                )
            }
            .toList()
    }

    fun writeSelectedRepo(treeUri: Uri?, choice: BridgeRepoChoice): Boolean {
        if (treeUri == null) return false
        return bridgeFolder.writeText(treeUri, "config", "selected_repo.txt", choice.termuxPath + "\n")
    }

    companion object {
        private const val TERMUX_PROJECTS_DIR = "/data/data/com.termux/files/home/projects"
    }
}
