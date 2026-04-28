package com.applab.termuxbridge.ui

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.applab.termuxbridge.actions.BackendActionRegistryState
import com.applab.termuxbridge.bridge.BridgeAction
import com.applab.termuxbridge.bridge.BridgeResult

@Composable
fun BridgeHomeScreen(
    treeUri: Uri?,
    latestResult: BridgeResult,
    onPickFolder: () -> Unit,
    onRefresh: () -> Unit,
    onRunAction: (BridgeAction) -> Unit,
    onOpenReport: () -> Unit,
    onOpenLog: () -> Unit,
    onGoTo: (BridgeAppScreen) -> Unit
) {
    BridgeReadinessCard(treeUri, latestResult, onPickFolder, onRunAction)
    BridgeActiveRepoCard(latestResult, onRunAction, onGoTo)
    BridgeNextActionCard(treeUri, latestResult, onPickFolder, onRunAction, onOpenReport, onGoTo)
    BridgeLatestResultCard(latestResult, onRefresh, onOpenReport, onOpenLog, onGoTo)
}

@Composable
fun BridgeReadinessCard(
    treeUri: Uri?,
    latestResult: BridgeResult,
    onPickFolder: () -> Unit,
    onRunAction: (BridgeAction) -> Unit
) {
    BridgeSectionCard("Setup Status") {
        BridgeStatusLine("Shared folder access", if (treeUri == null) "not selected" else "selected", treeUri != null)
        BridgeStatusLine("Saved result file", if (latestResult.isLoaded) "loaded" else "not found", latestResult.isLoaded)
        BridgeStatusLine("Last Termux action", latestResult.action.ifBlank { "none" }, latestResult.status.equals("success", true))
        if (treeUri == null) {
            BridgePrimaryButton("Choose Shared Bridge Folder", onPickFolder)
        } else {
            BridgeActionButton(BridgeAction.CHECK_SETUP, onRunAction)
        }
    }
}

@Composable
fun BridgeActiveRepoCard(
    latestResult: BridgeResult,
    onRunAction: (BridgeAction) -> Unit,
    onGoTo: (BridgeAppScreen) -> Unit
) {
    BridgeSectionCard("Selected Repo") {
        BridgeStatusLine("Repo", latestResult.repoName ?: "unknown", latestResult.repoName != null)
        BridgeStatusLine("Branch", latestResult.branch ?: "unknown", latestResult.branch != null)
        BridgeStatusLine("Git state", latestResult.stateLabel, latestResult.dirty == false)
        BridgeStatusLine("Changes", latestResult.changeBreakdownLabel, latestResult.dirty == false)
        BridgeStatusLine("Patch file", latestResult.patchLabel, latestResult.hasPatchFile == true)
        latestResult.currentCommit?.let { commit -> BridgeStatusLine("Commit", commit, true) }
        latestResult.upstream?.takeIf { it.isNotBlank() }?.let { upstream -> BridgeStatusLine("Upstream", upstream, true) }
        BridgeActionButton(BridgeAction.SHOW_ACTIVE_REPO, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_STATUS, onRunAction)
        BridgeSecondaryButton("Go to Repo Workbench") { onGoTo(BridgeAppScreen.REPO) }
    }
}

@Composable
fun BridgeNextActionCard(
    treeUri: Uri?,
    latestResult: BridgeResult,
    onPickFolder: () -> Unit,
    onRunAction: (BridgeAction) -> Unit,
    onOpenReport: () -> Unit,
    onGoTo: (BridgeAppScreen) -> Unit
) {
    val action = bridgeRecommendedAction(treeUri, latestResult)
    BridgeSectionCard("Suggested Next Step") {
        Text(action.title, color = Color.White, fontWeight = FontWeight.Bold)
        BridgeHintText(action.detail)
        when {
            action.pickFolder -> BridgePrimaryButton(action.buttonLabel, onPickFolder)
            action.openReport -> BridgePrimaryButton(action.buttonLabel, onOpenReport)
            action.screen != null -> BridgePrimaryButton(action.buttonLabel) { onGoTo(action.screen) }
            action.bridgeAction != null -> BridgeActionButton(action.bridgeAction, onRunAction, tone = action.tone)
        }
    }
}

@Composable
fun BridgeLatestResultCard(
    result: BridgeResult,
    onRefresh: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenLog: () -> Unit,
    onGoTo: (BridgeAppScreen) -> Unit
) {
    BridgeSectionCard("Last Saved Termux Result") {
        BridgeResultBlock(result)
        BridgePrimaryButton("Open Last Action Report", onOpenReport)
        BridgeSecondaryButton("Open Latest Log File", onOpenLog)
        BridgeSecondaryButton("Reload Saved Result File", onRefresh)
        BridgeSecondaryButton("Go to Results Tools") { onGoTo(BridgeAppScreen.RESULTS) }
    }
}

@Composable
fun BridgeRepoWorkbenchScreen(
    registryState: BackendActionRegistryState,
    latestResult: BridgeResult,
    hasTermuxPermission: Boolean,
    curationState: ActionCurationState,
    onRunAction: (BridgeAction) -> Unit,
    onTogglePin: (String) -> Unit,
    onHideAction: (String) -> Unit,
    onUnhideAction: (String) -> Unit
) {
    BridgeRegistryGroupOrFallback(
        registryState = registryState,
        groupName = "Repo Workbench",
        latestResult = latestResult,
        hasTermuxPermission = hasTermuxPermission,
        latestApkName = null,
        curationState = curationState,
        title = "Repo Workbench",
        onRunAction = onRunAction,
        onTogglePin = onTogglePin,
        onHideAction = onHideAction,
        onUnhideAction = onUnhideAction
    ) {
        BridgeRepoWorkbenchFallback(onRunAction)
    }
}

@Composable
private fun BridgeRepoWorkbenchFallback(onRunAction: (BridgeAction) -> Unit) {
    BridgeSectionCard("Repo Setup") {
        BridgeHintText("Use Clone only when a repo is missing or needs to be repaired. Select actions only switch the active repo pointer.")
        BridgeActionButton(BridgeAction.CLONE_BRIDGE, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.CLONE_LIBRESEED, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.LIST_PROJECTS, onRunAction)
    }
    BridgeSectionCard("Choose Active Repo") {
        BridgeActionButton(BridgeAction.SHOW_ACTIVE_REPO, onRunAction)
        BridgeActionButton(BridgeAction.SET_ACTIVE_BRIDGE, onRunAction)
        BridgeActionButton(BridgeAction.SET_ACTIVE_LIBRESEED, onRunAction)
    }
    BridgeSectionCard("Inspect Repo") {
        BridgeActionGroup(
            actions = listOf(
                BridgeAction.SHOW_STATUS,
                BridgeAction.SHOW_CURRENT_COMMIT,
                BridgeAction.SHOW_BRANCHES,
                BridgeAction.LIST_CHANGED_FILES,
                BridgeAction.SHOW_DIFF_SUMMARY,
                BridgeAction.SHOW_FULL_DIFF
            ),
            onRunAction = onRunAction
        )
    }
    BridgeSectionCard("Update Repo From GitHub") {
        BridgeHintText("Pull and checkout actions should be used on a clean repo.")
        BridgeActionGroup(
            actions = listOf(
                BridgeAction.FETCH_REPO,
                BridgeAction.PULL_CURRENT,
                BridgeAction.CHECKOUT_STAGING,
                BridgeAction.PULL_STAGING
            ),
            onRunAction = onRunAction
        )
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
        BridgeActionButton(BridgeAction.SHOW_ACTIVE_REPO, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_STATUS, onRunAction)
    }
    BridgeRegistryGroupOrFallback(
        registryState = registryState,
        groupName = "Patch Runner",
        latestResult = latestResult,
        hasTermuxPermission = hasTermuxPermission,
        latestApkName = null,
        curationState = curationState,
        title = "Patch Runner Actions",
        onRunAction = onRunAction,
        onTogglePin = onTogglePin,
        onHideAction = onHideAction,
        onUnhideAction = onUnhideAction
    ) {
        BridgePatchRunnerFallback(latestResult, onRunAction)
    }
}

@Composable
private fun BridgePatchRunnerFallback(latestResult: BridgeResult, onRunAction: (BridgeAction) -> Unit) {
    BridgeSectionCard("Run Patch File") {
        BridgeHintText("Runs Documents/AppLabBridge/patches/patch.sh against the selected repo.")
        BridgeActionButton(BridgeAction.RUN_PATCH_SCRIPT, onRunAction, tone = BridgeActionTone.WARNING)
    }
    BridgeSectionCard("Review Patch Output") {
        BridgeActionButton(BridgeAction.LIST_CHANGED_FILES, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_DIFF_SUMMARY, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_FULL_DIFF, onRunAction)
    }
    BridgeSectionCard("Commit and Push") {
        BridgeHintText("Use only after reviewing the diff. The commit action appends [no apk] when needed.")
        BridgeStatusLine("Staged", "${latestResult.stagedFiles ?: 0}", (latestResult.stagedFiles ?: 0) > 0)
        BridgeStatusLine("Unstaged", "${latestResult.unstagedFiles ?: 0}", (latestResult.unstagedFiles ?: 0) == 0)
        BridgeStatusLine("Untracked", "${latestResult.untrackedFiles ?: 0}", (latestResult.untrackedFiles ?: 0) == 0)
        BridgeActionButton(BridgeAction.STAGE_ALL, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.COMMIT_NO_APK, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.PUSH_CURRENT, onRunAction, tone = BridgeActionTone.WARNING)
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
    BridgeRegistryGroupOrFallback(
        registryState = registryState,
        groupName = "Build / APK",
        latestResult = latestResult,
        hasTermuxPermission = hasTermuxPermission,
        latestApkName = latestApkName,
        curationState = curationState,
        title = "App Update Actions",
        onRunAction = onRunAction,
        onTogglePin = onTogglePin,
        onHideAction = onHideAction,
        onUnhideAction = onUnhideAction
    ) {
        BridgeApkFallback(onRunAction)
    }
    BridgeSectionCard("Install Downloaded APK") {
        Text(text = "Newest APK in shared folder: ${latestApkName ?: "none found"}", color = Color(0xFF9AA4B2))
        BridgePrimaryButton("Open Android Installer for Newest APK", onInstall)
        BridgeSecondaryButton("Open This App's Install Permission", onInstallSettings)
        BridgeHintText("If Android says conflicting package, this APK was signed with a different key than the installed app. Uninstall AppLab Bridge once, install the APK from the kept Debug APK workflow, then future APKs from that same workflow should update normally.")
    }
}

@Composable
private fun BridgeApkFallback(onRunAction: (BridgeAction) -> Unit) {
    BridgeSectionCard("Check for App Update") {
        BridgeHintText("Uses Termux and GitHub CLI to inspect the latest successful Debug APK workflow artifact. This does not build an APK on the phone.")
        BridgeActionButton(BridgeAction.CHECK_LATEST_APK, onRunAction)
    }
    BridgeSectionCard("Download App Update") {
        BridgeHintText("Downloads the newest GitHub APK artifact to Documents/AppLabBridge/apks. Use APKs from the single kept Debug APK workflow to avoid signing conflicts.")
        BridgeActionButton(BridgeAction.DOWNLOAD_LATEST_APK, onRunAction, tone = BridgeActionTone.WARNING)
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
        BridgeHintText("The app now asks for Termux RUN_COMMAND permission instead of silently waiting for a timeout. Some Android builds may still require opening app settings manually.")
    }
    BridgeSectionCard("Backend Bootstrap / Repair") {
        BridgeHintText("Use this when the app times out because bridge_v2.sh or helper files are missing or stale. The app asks Termux bash to install the live backend through stdin; the inspection copy is optional.")
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
