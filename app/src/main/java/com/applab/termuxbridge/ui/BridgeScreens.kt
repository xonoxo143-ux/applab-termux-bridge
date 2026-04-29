package com.applab.termuxbridge.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.applab.termuxbridge.actions.BackendActionRegistryState
import com.applab.termuxbridge.bridge.BridgeAction
import com.applab.termuxbridge.bridge.BridgeResult

@Composable
fun BridgeHomeScreen(
    treeUri: Uri?,
    latestResult: BridgeResult,
    registryState: BackendActionRegistryState,
    hasTermuxPermission: Boolean,
    curationState: ActionCurationState,
    latestApkName: String?,
    onPickFolder: () -> Unit,
    onRefresh: () -> Unit,
    onRunAction: (BridgeAction) -> Unit,
    onOpenReport: () -> Unit,
    onOpenLog: () -> Unit,
    onGoTo: (BridgeAppScreen) -> Unit,
    onTogglePin: (String) -> Unit,
    onHideAction: (String) -> Unit,
    onUnhideAction: (String) -> Unit
) {
    BridgeDashboardReadinessStrip(treeUri, hasTermuxPermission, registryState, latestResult, onPickFolder, onRunAction, onGoTo)
    BridgeDashboardRepoSummary(latestResult, onRunAction, onGoTo)
    BridgeDashboardNextStep(treeUri, latestResult, onPickFolder, onRunAction, onOpenReport, onGoTo)
    BridgeDashboardPinnedActions(registryState, latestResult, hasTermuxPermission, curationState, latestApkName, onRunAction, onGoTo)
    BridgeDashboardLastResult(latestResult, onOpenReport, onOpenLog, onGoTo)
}

@Composable
fun BridgeRepoWorkbenchScreen(
    latestResult: BridgeResult,
    repoChoices: List<BridgeRepoChoice>,
    onRunAction: (BridgeAction) -> Unit,
    onChooseRepo: (BridgeRepoChoice) -> Unit,
    onGoTo: (BridgeAppScreen) -> Unit
) {
    BridgeSectionCard("Current Repo") {
        BridgeStatusLine("Repo", latestResult.repoName ?: "none selected", latestResult.repoName != null)
        BridgeStatusLine("Branch", latestResult.branch ?: "unknown", latestResult.branch != null)
        BridgeStatusLine("State", latestResult.stateLabel, latestResult.dirty == false)
        BridgeStatusLine("Changes", latestResult.changeBreakdownLabel, latestResult.dirty == false)
        BridgeStatusLine("Patch", latestResult.patchLabel, latestResult.hasPatchFile == true)
    }

    BridgeSectionCard("Choose Repo") {
        BridgeHintText("Find Repos scans Termux ~/projects and your GitHub repos. Local repos are selected; online repos are cloned then selected.")
        BridgePrimaryButton("Find Repos", { onRunAction(BridgeAction.LIST_PROJECTS) })
        if (repoChoices.isEmpty()) {
            BridgeHintText("No repos loaded yet. Tap Find Repos first.")
        } else {
            repoChoices.take(6).forEach { choice ->
                val suffix = when (choice.source) {
                    BridgeRepoChoiceSource.LOCAL -> "local"
                    BridgeRepoChoiceSource.GITHUB -> "online"
                }
                Button(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (choice.source == BridgeRepoChoiceSource.LOCAL) Color(0xFF344055) else Color(0xFF1B6BFF)),
                    onClick = { onChooseRepo(choice) }
                ) { Text("${choice.displayName} · $suffix") }
            }
            if (repoChoices.size > 6) {
                BridgeHintText("Showing first 6 repos. More filtering can be added later if this gets crowded.")
            }
        }
    }

    BridgeSectionCard("Repo Actions") {
        val hasRepo = latestResult.repoName != null || latestResult.branch != null
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                enabled = hasRepo,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
                onClick = { onRunAction(BridgeAction.CHECK_REPO) }
            ) { Text("Check") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                enabled = hasRepo,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB86814)),
                onClick = { onRunAction(BridgeAction.PULL_CURRENT) }
            ) { Text("Update") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                enabled = hasRepo,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = { onRunAction(BridgeAction.LIST_CHANGED_FILES) }
            ) { Text("Changes") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = { onGoTo(BridgeAppScreen.ACTION_CATALOG) }
            ) { Text("Actions") }
        }
        if (!hasRepo) {
            BridgeHintText("Choose a repo before checking, updating, or viewing changes.")
        }
    }
}

@Composable
fun BridgePatchRunnerScreen(
    registryState: BackendActionRegistryState,
    latestResult: BridgeResult,
    hasTermuxPermission: Boolean,
    curationState: ActionCurationState,
    onRunAction: (BridgeAction) -> Unit,
    onTogglePin: (String) -> Unit,
    onHideAction: (String) -> Unit,
    onUnhideAction: (String) -> Unit
) {
    BridgeSectionCard("Patch Readiness") {
        BridgeHintText("Use this card before running any patch. It confirms the selected repo, branch, git state, and patch file visibility.")
        BridgeStatusLine("Repo", latestResult.repoName ?: "unknown", latestResult.repoName != null)
        BridgeStatusLine("Branch", latestResult.branch ?: "unknown", latestResult.branch != null)
        BridgeStatusLine("Git state", latestResult.stateLabel, latestResult.dirty == false)
        BridgeStatusLine("Patch file", latestResult.patchLabel, latestResult.hasPatchFile == true)
        BridgeActionButton(BridgeAction.CHECK_REPO, onRunAction)
    }
    BridgeRegistryGroupOrFallback(registryState, "Patch Runner", latestResult, hasTermuxPermission, null, curationState, title = "Patch Actions", onRunAction = onRunAction, onTogglePin = onTogglePin, onHideAction = onHideAction, onUnhideAction = onUnhideAction) {
        BridgeSectionCard("Patch Unavailable") { BridgeHintText("Backend action registry is unavailable. Open Setup and refresh backend actions.") }
    }
}

@Composable
fun BridgeApkScreen(
    registryState: BackendActionRegistryState,
    latestResult: BridgeResult,
    hasTermuxPermission: Boolean,
    curationState: ActionCurationState,
    latestApkName: String?,
    onRunAction: (BridgeAction) -> Unit,
    onTogglePin: (String) -> Unit,
    onHideAction: (String) -> Unit,
    onUnhideAction: (String) -> Unit,
    onInstall: () -> Unit,
    onInstallSettings: () -> Unit
) {
    BridgeSectionCard("Latest Build") {
        BridgeHintText("Check GitHub for the latest signed debug APK, then download it to the shared bridge folder.")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
                onClick = { onRunAction(BridgeAction.CHECK_LATEST_APK) }
            ) { Text("Check") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB86814)),
                onClick = { onRunAction(BridgeAction.DOWNLOAD_LATEST_APK) }
            ) { Text("Download") }
        }
    }

    BridgeSectionCard("Downloaded Update") {
        Text(text = latestApkName ?: "No downloaded APK found", color = if (latestApkName == null) Color(0xFFFFD166) else Color(0xFF5CE38A), fontFamily = FontFamily.Monospace)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                enabled = latestApkName != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
                onClick = onInstall
            ) { Text("Install") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = onInstallSettings
            ) { Text("Permission") }
        }
    }

    BridgeSectionCard("Install Help") {
        BridgeHintText("Use APKs from the kept Debug APK workflow. If Android says conflicting package, uninstall AppLab Bridge once, then install the downloaded update.")
    }
}

@Composable
fun BridgeResultsScreen(
    result: BridgeResult,
    onRefresh: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenDebugZip: () -> Unit,
    onRunAction: (BridgeAction) -> Unit
) {
    BridgeSectionCard("Last Result") {
        Text("${result.status.uppercase()} · ${result.action.ifBlank { "none" }}", color = bridgeStatusColor(result.status), fontFamily = FontFamily.Monospace)
        Text(result.title, color = Color.White)
        BridgeHintText(result.summary)
        BridgeSecondaryButton("Reload", onRefresh)
    }

    BridgeSectionCard("Reports / Logs") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
                onClick = onOpenReport
            ) { Text("Report") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = onOpenLog
            ) { Text("Log") }
        }
    }

    BridgeSectionCard("Debug Bundle") {
        BridgeHintText("Create a zip of reports, logs, config, and result files. APK files are excluded.")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
                onClick = { onRunAction(BridgeAction.CREATE_DEBUG_ZIP) }
            ) { Text("Create") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = onOpenDebugZip
            ) { Text("Open") }
        }
    }
}

@Composable
fun BridgeSetupScreen(
    onPickFolder: () -> Unit,
    onRefresh: () -> Unit,
    onRunAction: (BridgeAction) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestTermuxPermission: () -> Unit,
    onClipboard: () -> Unit,
    onBootstrapBackend: () -> Unit
) {
    BridgeSectionCard("Access") {
        BridgeHintText("Use these when Android cannot read the shared folder or Termux cannot run commands.")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
                onClick = onPickFolder
            ) { Text("Folder") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
                onClick = onRequestTermuxPermission
            ) { Text("Termux") }
        }
        BridgeSecondaryButton("App Settings", onOpenSettings)
    }

    BridgeSectionCard("Backend") {
        BridgeHintText("Check or repair the Termux backend when actions timeout or reports stop updating.")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
                onClick = { onRunAction(BridgeAction.CHECK_SETUP) }
            ) { Text("Check") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB86814)),
                onClick = onBootstrapBackend
            ) { Text("Repair") }
        }
        BridgeSecondaryButton("Update Backend", { onRunAction(BridgeAction.UPDATE_DISPATCHER) })
    }

    BridgeSectionCard("Recovery") {
        BridgeHintText("Use these when you need to reload the latest saved state or pass clipboard text into the shared bridge folder.")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = onRefresh
            ) { Text("Reload") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = onClipboard
            ) { Text("Clipboard") }
        }
    }
}
