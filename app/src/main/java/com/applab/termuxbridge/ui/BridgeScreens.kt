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
        BridgeHintText("Find repos scans Termux ~/projects. Tap a repo below to make it the active repo.")
        BridgePrimaryButton("Find Repos", { onRunAction(BridgeAction.LIST_PROJECTS) })
        if (repoChoices.isEmpty()) {
            BridgeHintText("No scanned repos loaded yet. Tap Find Repos first.")
        } else {
            repoChoices.take(5).forEach { choice ->
                Button(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                    onClick = { onChooseRepo(choice) }
                ) { Text(choice.name) }
            }
            if (repoChoices.size > 5) {
                BridgeHintText("Showing first 5 repos. Refine repo folders later if this gets crowded.")
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
    BridgeRegistryGroupOrFallback(registryState, "Build / APK", latestResult, hasTermuxPermission, latestApkName, curationState, title = "Update Actions", onRunAction = onRunAction, onTogglePin = onTogglePin, onHideAction = onHideAction, onUnhideAction = onUnhideAction) {
        BridgeSectionCard("Update Actions") {
            BridgeActionButton(BridgeAction.CHECK_LATEST_APK, onRunAction)
            BridgeActionButton(BridgeAction.DOWNLOAD_LATEST_APK, onRunAction, tone = BridgeActionTone.WARNING)
        }
    }
    BridgeSectionCard("Install Downloaded Update") {
        Text(text = "Newest APK in shared folder: ${latestApkName ?: "none found"}", color = Color(0xFF9AA4B2))
        BridgePrimaryButton("Open Android Installer", onInstall)
        BridgeSecondaryButton("Open Install Permission", onInstallSettings)
        BridgeHintText("If Android says conflicting package, uninstall AppLab Bridge once, then install APKs from the kept Debug APK workflow.")
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
    BridgeSectionCard("Saved Result File") {
        BridgeResultBlock(result)
        BridgePrimaryButton("Open Last Action Report", onOpenReport)
        BridgeSecondaryButton("Open Latest Log File", onOpenLog)
        BridgeSecondaryButton("Reload Saved Result File", onRefresh)
    }
    BridgeSectionCard("Debug Bundle") {
        BridgeHintText("Create a zip of reports, logs, config, and result files. APK files are excluded.")
        BridgeActionButton(BridgeAction.CREATE_DEBUG_ZIP, onRunAction)
        BridgeSecondaryButton("Open Latest Debug Zip", onOpenDebugZip)
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
    BridgeSectionCard("Shared Bridge Folder") {
        BridgePrimaryButton("Choose / Switch Shared Bridge Folder", onPickFolder)
        BridgeSecondaryButton("Reload Saved Result File", onRefresh)
        BridgeHintText("On first launch the app asks for this folder automatically. This button stays here only for manually switching folders later. Pick Documents or AppLabBridge.")
    }
    BridgeSectionCard("Termux Permission") {
        BridgePrimaryButton("Request Termux Command Permission", onRequestTermuxPermission)
        BridgeSecondaryButton("Open Android Permissions for This App", onOpenSettings)
        BridgeHintText("The app asks for Termux RUN_COMMAND permission instead of silently waiting for a timeout.")
    }
    BridgeSectionCard("Backend Bootstrap / Repair") {
        BridgeHintText("Use this when the app times out because bridge_v2.sh or helper files are missing or stale.")
        BridgeActionButton(BridgeAction.CHECK_SETUP, onRunAction)
        BridgePrimaryButton("Bootstrap / Repair Termux Backend", onBootstrapBackend)
    }
    BridgeSectionCard("Termux Backend From GitHub") {
        BridgeActionButton(BridgeAction.UPDATE_DISPATCHER, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeHintText("Termux needs storage access, allow-external-apps=true, GitHub auth, and Android permission for this app to run Termux commands.")
    }
    BridgeSectionCard("Inbox") {
        BridgeSecondaryButton("Save Clipboard Text to Inbox", onClipboard)
        BridgeHintText("This writes clipboard text into the shared bridge folder for later backend workflows. It does not run Termux.")
    }
}
