package com.applab.termuxbridge.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
    BridgeSectionCard("System Readiness") {
        BridgeStatusLine("Bridge folder", if (treeUri == null) "missing" else "selected", treeUri != null)
        BridgeStatusLine("Latest result", if (latestResult.isLoaded) "loaded" else "missing", latestResult.isLoaded)
        BridgeStatusLine("Last action", latestResult.action.ifBlank { "none" }, latestResult.status.equals("success", true))
        if (treeUri == null) {
            BridgePrimaryButton("Pick Bridge Folder", onPickFolder)
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
    BridgeSectionCard("Active Repo") {
        BridgeStatusLine("Repo", latestResult.repoName ?: "unknown", latestResult.repoName != null)
        BridgeStatusLine("Branch", latestResult.branch ?: "unknown", latestResult.branch != null)
        BridgeStatusLine("State", latestResult.stateLabel, latestResult.dirty == false)
        BridgeStatusLine("Changes", latestResult.changeBreakdownLabel, latestResult.dirty == false)
        BridgeStatusLine("Patch file", latestResult.patchLabel, latestResult.hasPatchFile == true)
        latestResult.currentCommit?.let { commit -> BridgeStatusLine("Commit", commit, true) }
        latestResult.upstream?.takeIf { it.isNotBlank() }?.let { upstream -> BridgeStatusLine("Upstream", upstream, true) }
        BridgeActionButton(BridgeAction.SHOW_ACTIVE_REPO, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_STATUS, onRunAction)
        BridgeSecondaryButton("Open Repo Workbench") { onGoTo(BridgeAppScreen.REPO) }
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
    BridgeSectionCard("Recommended Next") {
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
    BridgeSectionCard("Latest Result") {
        BridgeResultBlock(result)
        BridgePrimaryButton("Open Report", onOpenReport)
        BridgeSecondaryButton("Open Latest Log", onOpenLog)
        BridgeSecondaryButton("Refresh Result", onRefresh)
        BridgeSecondaryButton("All Results Tools") { onGoTo(BridgeAppScreen.RESULTS) }
    }
}

@Composable
fun BridgeRepoWorkbenchScreen(onRunAction: (BridgeAction) -> Unit) {
    BridgeSectionCard("Repo Selection") {
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
    BridgeSectionCard("Inspect") {
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
    BridgeSectionCard("Update") {
        BridgeHintText("Pull/checkout actions should be used on a clean repo.")
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
    BridgeSectionCard("Patch Runner") {
        BridgeHintText("Runs Documents/AppLabBridge/patches/patch.sh against the active repo. Review status and branch first.")
        BridgeStatusLine("Patch file", latestResult.patchLabel, latestResult.hasPatchFile == true)
        BridgeActionButton(BridgeAction.SHOW_ACTIVE_REPO, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_STATUS, onRunAction)
        BridgeActionButton(BridgeAction.RUN_PATCH_SCRIPT, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.LIST_CHANGED_FILES, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_DIFF_SUMMARY, onRunAction)
    }
    BridgeSectionCard("Publish") {
        BridgeHintText("Use only after reviewing the diff. Commit action appends [no apk] if needed.")
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
    BridgeSectionCard("Update App from GitHub") {
        BridgeHintText("Checks the latest successful Debug APK workflow, downloads the APK to the bridge folder, then you install it with the local installer.")
        BridgeActionButton(BridgeAction.CHECK_LATEST_APK, onRunAction)
        BridgeActionButton(BridgeAction.DOWNLOAD_LATEST_APK, onRunAction, tone = BridgeActionTone.WARNING)
    }
    BridgeSectionCard("Install Downloaded APK") {
        Text(text = "Latest local APK: ${latestApkName ?: "none found"}", color = Color(0xFF9AA4B2))
        BridgePrimaryButton("Install Latest Local APK", onInstall)
        BridgeSecondaryButton("Open Install Settings", onInstallSettings)
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
    BridgeSectionCard("Result Detail") {
        BridgeResultBlock(result)
        BridgePrimaryButton("Open Report", onOpenReport)
        BridgeSecondaryButton("Open Latest Log", onOpenLog)
        BridgeSecondaryButton("Open Latest Debug Zip", onOpenDebugZip)
        BridgeSecondaryButton("Refresh Result", onRefresh)
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
    BridgeSectionCard("Bridge Folder") {
        BridgePrimaryButton("Pick Documents/AppLabBridge", onPickFolder)
        BridgeSecondaryButton("Refresh Latest Result", onRefresh)
        BridgeHintText("Termux should write to ~/storage/shared/Documents/AppLabBridge.")
    }
    BridgeSectionCard("Termux Setup") {
        BridgeActionButton(BridgeAction.CHECK_SETUP, onRunAction)
        BridgeActionButton(BridgeAction.UPDATE_DISPATCHER, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeSecondaryButton("Open App Permission Settings", onOpenSettings)
        BridgeHintText("Required: Termux storage access, allow-external-apps=true, GitHub auth, and Android permission for this app to run Termux commands.")
    }
    BridgeSectionCard("Parked Tools") {
        BridgeSecondaryButton("Write Clipboard to Inbox", onClipboard)
        BridgeHintText("Save-code, source audit, APK download, and debug bundle buttons remain parked until their backend paths are clean.")
    }
}
