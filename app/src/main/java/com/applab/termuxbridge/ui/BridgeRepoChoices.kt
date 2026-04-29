package com.applab.termuxbridge.ui

import android.net.Uri
import com.applab.termuxbridge.storage.SafBridgeFolder

enum class BridgeRepoChoiceSource(val label: String) {
    LOCAL("local"),
    GITHUB("online")
}

data class BridgeRepoChoice(
    val name: String,
    val relativePath: String,
    val termuxPath: String,
    val source: BridgeRepoChoiceSource = BridgeRepoChoiceSource.LOCAL,
    val remoteFullName: String = "",
    val cloneUrl: String = ""
) {
    val displayName: String
        get() = if (remoteFullName.isNotBlank()) remoteFullName else name
}

class BridgeRepoChoiceReader(private val bridgeFolder: SafBridgeFolder) {
    fun readChoices(treeUri: Uri?): List<BridgeRepoChoice> {
        if (treeUri == null) return emptyList()
        val report = bridgeFolder.readText(treeUri, listOf("reports", "list_projects.txt")).orEmpty()
        return report.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { isNoiseLine(it) }
            .mapNotNull { parseChoiceLine(it) }
            .distinctBy { "${it.source}:${it.displayName}:${it.termuxPath}" }
            .toList()
    }

    fun writeSelectedRepo(treeUri: Uri?, choice: BridgeRepoChoice): Boolean {
        if (treeUri == null) return false
        val pathWritten = bridgeFolder.writeText(treeUri, "config", "selected_repo.txt", choice.termuxPath + "\n")
        val cloneWritten = bridgeFolder.writeText(treeUri, "config", "selected_repo_clone_url.txt", choice.cloneUrl + "\n")
        val sourceWritten = bridgeFolder.writeText(treeUri, "config", "selected_repo_source.txt", choice.source.label + "\n")
        val nameWritten = bridgeFolder.writeText(treeUri, "config", "selected_repo_name.txt", choice.displayName + "\n")
        return pathWritten && cloneWritten && sourceWritten && nameWritten
    }

    private fun parseChoiceLine(line: String): BridgeRepoChoice? {
        val parts = line.split('\t')
        if (parts.size >= 4 && parts[0] == "local") {
            val relative = parts[1].trim('/').removeSuffix("/.git")
            val path = parts[2].ifBlank { "$TERMUX_PROJECTS_DIR/$relative" }
            return BridgeRepoChoice(
                name = parts[3].ifBlank { relative.substringAfterLast('/') },
                relativePath = relative,
                termuxPath = path,
                source = BridgeRepoChoiceSource.LOCAL
            )
        }
        if (parts.size >= 4 && parts[0] == "github") {
            val fullName = parts[1]
            val repoName = fullName.substringAfterLast('/')
            val path = "$TERMUX_PROJECTS_DIR/$repoName"
            return BridgeRepoChoice(
                name = repoName,
                relativePath = repoName,
                termuxPath = path,
                source = BridgeRepoChoiceSource.GITHUB,
                remoteFullName = fullName,
                cloneUrl = parts[2]
            )
        }
        val clean = line.trim('/').removeSuffix("/.git")
        if (clean.isBlank()) return null
        return BridgeRepoChoice(
            name = clean.substringAfterLast('/'),
            relativePath = clean,
            termuxPath = "$TERMUX_PROJECTS_DIR/$clean",
            source = BridgeRepoChoiceSource.LOCAL
        )
    }

    private fun isNoiseLine(line: String): Boolean {
        return line.startsWith("Projects under ") ||
            line.startsWith("Local repos") ||
            line.startsWith("GitHub repos") ||
            line.startsWith("Action:") ||
            line.startsWith("Run:") ||
            line.startsWith("Started:") ||
            line.startsWith("Time:") ||
            line.startsWith(".") ||
            line.contains("No such file", ignoreCase = true) ||
            line.contains("gh missing", ignoreCase = true) ||
            line.contains("gh repo list unavailable", ignoreCase = true)
    }

    companion object {
        private const val TERMUX_PROJECTS_DIR = "/data/data/com.termux/files/home/projects"
    }
}
