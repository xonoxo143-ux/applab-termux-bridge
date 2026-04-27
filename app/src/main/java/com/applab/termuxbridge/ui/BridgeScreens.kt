package com.applab.termuxbridge.ui

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
fun BridgeRepoWorkbenchScreen(onRunAction: (BridgeAction) -> Unit) {
    BridgeSectionCard("Choose Repo") {
        BridgeActionGroup(
            actions = listOf(
                BridgeAction.LIST_PROJECTS,
                BridgeAction.SHOW_ACTIVE_REPO,
                BridgeAction.SET_ACTIVE_BRIDGE,
                BridgeAction.SET_ACTIVE_LIBRESEED
            ),
            onRunAction = onRunAction
        )
    }
    BridgeSectionCard("Read Repo State") {
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
fun BridgePatchRunnerScreen(latestResult: BridgeResult, onRunAction: (BridgeAction) -> Unit) {
    BridgeSectionCard("Run Patch File") {
        BridgeHintText("Runs Documents/AppLabBridge/patches/patch.sh against the selected repo. Check repo and branch first.")
        BridgeStatusLine("Patch file", latestResult.patchLabel, latestResult.hasPatchFile == true)
        BridgeActionButton(BridgeAction.SHOW_ACTIVE_REPO, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_STATUS, onRunAction)
        BridgeActionButton(BridgeAction.RUN_PATCH_SCRIPT, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.LIST_CHANGED_FILES, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_DIFF_SUMMARY, onRunAction)
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
    latestApkName: String?,
    onRunAction: (BridgeAction) -> Unit,
    onInstall: () -> Unit,
    onInstallSettings: () -> Unit
) {
    BridgeSectionCard("Download App Update") {
        BridgeHintText("Uses Termux and GitHub CLI to find the latest successful Debug APK workflow artifact, then saves the APK to the shared bridge folder.")
        BridgeActionButton(BridgeAction.CHECK_LATEST_APK, onRunAction)
        BridgeActionButton(BridgeAction.DOWNLOAD_LATEST_APK, onRunAction, tone = BridgeActionTone.WARNING)
    }
    BridgeSectionCard("Install Downloaded APK") {
        Text(text = "Newest APK in shared folder: ${latestApkName ?: "none found"}", color = Color(0xFF9AA4B2))
        BridgePrimaryButton("Open Android Installer for Newest APK", onInstall)
        BridgeSecondaryButton("Open This App's Install Permission", onInstallSettings)
        BridgeHintText("Android may ask for permission to install unknown apps from AppLab Bridge.")
    }
}

@Composable
fun BridgeResultsScreen(
    result: BridgeResult,
    onRefresh: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenDebugZip: () -> Unit
) {
    BridgeSectionCard("Saved Result File") {
        BridgeResultBlock(result)
        BridgePrimaryButton("Open Last Action Report", onOpenReport)
        BridgeSecondaryButton("Open Latest Log File", onOpenLog)
        BridgeSecondaryButton("Open Latest Debug Zip", onOpenDebugZip)
        BridgeSecondaryButton("Reload Saved Result File", onRefresh)
    }
}

@Composable
fun BridgeSetupScreen(
    onPickFolder: () -> Unit,
    onRefresh: () -> Unit,
    onRunAction: (BridgeAction) -> Unit,
    onOpenSettings: () -> Unit,
    onClipboard: () -> Unit
) {
    BridgeSectionCard("Shared Bridge Folder") {
        BridgePrimaryButton("Choose Documents/AppLabBridge Folder", onPickFolder)
        BridgeSecondaryButton("Reload Saved Result File", onRefresh)
        BridgeHintText("This only rereads Documents/AppLabBridge/results/latest_result.json. It does not run a Termux command.")
    }
    BridgeSectionCard("Termux Backend") {
        BridgeActionButton(BridgeAction.CHECK_SETUP, onRunAction)
        BridgeActionButton(BridgeAction.UPDATE_DISPATCHER, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeSecondaryButton("Open Android Permissions for This App", onOpenSettings)
        BridgeHintText("Termux needs storage access, allow-external-apps=true, GitHub auth, and Android permission for this app to run Termux commands.")
    }
    BridgeSectionCard("Parked Tools") {
        BridgeSecondaryButton("Save Clipboard Text to Inbox", onClipboard)
        BridgeHintText("These tools are parked until their backend workflows are clean and testable.")
    }
}
