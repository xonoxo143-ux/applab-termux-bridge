package com.applab.termuxbridge.ui

import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.applab.termuxbridge.bridge.BridgeAction
import com.applab.termuxbridge.bridge.BridgeResult

enum class BridgeAppScreen(val title: String) {
    HOME("Home"),
    REPO("Repo Workbench"),
    PATCH("Patch Runner"),
    APK("Build / APK"),
    RESULTS("Results"),
    SETUP("Setup")
}

enum class BridgeActionTone {
    PRIMARY,
    NEUTRAL,
    WARNING
}

data class BridgeRecommendedAction(
    val title: String,
    val detail: String,
    val buttonLabel: String,
    val bridgeAction: BridgeAction? = null,
    val screen: BridgeAppScreen? = null,
    val pickFolder: Boolean = false,
    val openReport: Boolean = false,
    val tone: BridgeActionTone = BridgeActionTone.PRIMARY
)

fun bridgeRecommendedAction(treeUri: Uri?, result: BridgeResult): BridgeRecommendedAction {
    if (treeUri == null) {
        return BridgeRecommendedAction(
            title = "Pick the bridge folder",
            detail = "The app needs Documents/AppLabBridge before it can read Termux results.",
            buttonLabel = "Pick Bridge Folder",
            pickFolder = true
        )
    }
    if (!result.isLoaded) {
        return BridgeRecommendedAction(
            title = "Check setup",
            detail = "No result has been loaded from the bridge folder yet.",
            buttonLabel = BridgeAction.CHECK_SETUP.label,
            bridgeAction = BridgeAction.CHECK_SETUP
        )
    }
    if (result.status.equals("failed", true)) {
        return BridgeRecommendedAction(
            title = "Inspect the failure",
            detail = "The last action failed. Open the report before running more actions.",
            buttonLabel = "Open Failure Report",
            openReport = true,
            tone = BridgeActionTone.WARNING
        )
    }
    if (result.action == "download_latest_apk") {
        return BridgeRecommendedAction("Install the downloaded APK", "The latest APK should now be in the bridge folder. Install it from Build / APK.", "Open Build / APK", screen = BridgeAppScreen.APK)
    }
    if (result.action == "update_dispatcher") {
        return BridgeRecommendedAction("Check setup again", "The dispatcher was updated. Verify the backend before running more actions.", BridgeAction.CHECK_SETUP.label, BridgeAction.CHECK_SETUP)
    }
    if ((result.stagedFiles ?: 0) > 0) {
        return BridgeRecommendedAction("Commit staged changes", "There are staged files ready to commit.", BridgeAction.COMMIT_NO_APK.label, BridgeAction.COMMIT_NO_APK, tone = BridgeActionTone.WARNING)
    }
    if (result.ahead != null && result.ahead > 0 && result.dirty == false) {
        return BridgeRecommendedAction("Push current branch", "The active repo has local commits ahead of upstream.", BridgeAction.PUSH_CURRENT.label, BridgeAction.PUSH_CURRENT, tone = BridgeActionTone.WARNING)
    }
    if (result.dirty == true && result.action == "show_status") {
        return BridgeRecommendedAction("Review the diff", "The active repo has changed files. Review before staging.", BridgeAction.SHOW_DIFF_SUMMARY.label, BridgeAction.SHOW_DIFF_SUMMARY)
    }
    if (result.dirty == false && result.action == "show_status") {
        return if (result.hasPatchFile == true) {
            BridgeRecommendedAction("Ready to run patch", "The repo is clean and patches/patch.sh is available.", "Open Patch Runner", screen = BridgeAppScreen.PATCH)
        } else {
            BridgeRecommendedAction("Ready for work", "The repo is clean. Pull current branch, place a patch script, or switch workflows.", BridgeAction.PULL_CURRENT.label, BridgeAction.PULL_CURRENT)
        }
    }
    return when (result.action) {
        "check_setup" -> BridgeRecommendedAction("Choose a repo", "Setup passed. Select an active repo or check the current one.", BridgeAction.SHOW_ACTIVE_REPO.label, BridgeAction.SHOW_ACTIVE_REPO)
        "set_active_bridge", "set_active_libreseed", "show_active_repo" -> BridgeRecommendedAction("Refresh repo status", "The active repo changed or was checked. Pull a fresh status next.", BridgeAction.SHOW_STATUS.label, BridgeAction.SHOW_STATUS)
        "check_latest_apk" -> BridgeRecommendedAction("Download GitHub APK", "A successful app build is available if the report found a workflow run.", BridgeAction.DOWNLOAD_LATEST_APK.label, BridgeAction.DOWNLOAD_LATEST_APK, tone = BridgeActionTone.WARNING)
        "pull_current", "pull_staging", "checkout_staging" -> BridgeRecommendedAction("Review changed files", "Status is current. Check whether anything changed before patching or publishing.", BridgeAction.LIST_CHANGED_FILES.label, BridgeAction.LIST_CHANGED_FILES)
        "run_patch_script" -> BridgeRecommendedAction("Review patch result", "A patch ran. Review changed files and diff before staging.", BridgeAction.SHOW_DIFF_SUMMARY.label, BridgeAction.SHOW_DIFF_SUMMARY)
        "list_changed_files", "show_diff_summary", "show_full_diff" -> BridgeRecommendedAction("Open Patch / Publish", "If the diff is correct, stage and commit from the guarded workflow screen.", "Open Patch / Publish", screen = BridgeAppScreen.PATCH, tone = BridgeActionTone.WARNING)
        "stage_all" -> BridgeRecommendedAction("Commit staged changes", "Files were staged. Commit with [no apk] unless this change should build an APK.", BridgeAction.COMMIT_NO_APK.label, BridgeAction.COMMIT_NO_APK, tone = BridgeActionTone.WARNING)
        "commit_no_apk" -> BridgeRecommendedAction("Push current branch", "A commit was created. Push it when ready.", BridgeAction.PUSH_CURRENT.label, BridgeAction.PUSH_CURRENT, tone = BridgeActionTone.WARNING)
        "push_current" -> BridgeRecommendedAction("Refresh status", "Push completed. Refresh the active repo status.", BridgeAction.SHOW_STATUS.label, BridgeAction.SHOW_STATUS)
        else -> BridgeRecommendedAction("Refresh status", "Start by checking the active repo state.", BridgeAction.SHOW_STATUS.label, BridgeAction.SHOW_STATUS)
    }
}

fun bridgeRequiresConfirmation(action: BridgeAction): Boolean {
    return action in setOf(
        BridgeAction.UPDATE_DISPATCHER,
        BridgeAction.PULL_STAGING,
        BridgeAction.CHECKOUT_STAGING,
        BridgeAction.RUN_PATCH_SCRIPT,
        BridgeAction.STAGE_ALL,
        BridgeAction.COMMIT_NO_APK,
        BridgeAction.PUSH_CURRENT,
        BridgeAction.DOWNLOAD_LATEST_APK
    )
}

fun bridgeActionRiskText(action: BridgeAction): String {
    return when (action) {
        BridgeAction.UPDATE_DISPATCHER -> "Updates the live Termux dispatcher from the bridge repo. This changes backend behavior without installing a new APK."
        BridgeAction.PULL_STAGING, BridgeAction.CHECKOUT_STAGING -> "Changes the active repo branch. Use on a clean working tree."
        BridgeAction.RUN_PATCH_SCRIPT -> "Runs patches/patch.sh against the active repo. Review repo and branch before confirming."
        BridgeAction.STAGE_ALL -> "Stages every changed file in the active repo. Review the diff first."
        BridgeAction.COMMIT_NO_APK -> "Creates a local commit. The backend appends [no apk] when needed."
        BridgeAction.PUSH_CURRENT -> "Pushes the current branch to GitHub. Confirm the branch and commit first."
        BridgeAction.DOWNLOAD_LATEST_APK -> "Downloads the latest GitHub debug APK artifact into the bridge folder. Install remains a separate Android confirmation."
        else -> "This action changes repo or backend state. Confirm before running."
    }
}

fun bridgeRepoHeaderText(result: BridgeResult): String {
    if (result.repoName == null && result.branch == null) return "No repo state loaded"
    return "${result.repoLabel} · ${result.stateLabel}"
}

fun bridgeStatusChipText(result: BridgeResult): String {
    val action = result.action.ifBlank { "no action" }
    val exit = result.exitCode?.toString() ?: "n/a"
    return "${result.status.uppercase()} · $action · exit $exit"
}

fun bridgeStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "success" -> Color(0xFF5CE38A)
        "failed" -> Color(0xFFFF6B6B)
        "running" -> Color(0xFFFFD166)
        "missing" -> Color(0xFFFFD166)
        else -> Color(0xFF9AA4B2)
    }
}

fun bridgeRepoStateColor(result: BridgeResult): Color {
    return when {
        result.dirty == true -> Color(0xFFFFD166)
        result.ahead != null && result.ahead > 0 -> Color(0xFFFFD166)
        result.behind != null && result.behind > 0 -> Color(0xFFFFD166)
        result.dirty == false -> Color(0xFF5CE38A)
        else -> Color(0xFF9AA4B2)
    }
}

fun bridgeButtonColor(tone: BridgeActionTone): Color {
    return when (tone) {
        BridgeActionTone.PRIMARY -> Color(0xFF1B6BFF)
        BridgeActionTone.NEUTRAL -> Color(0xFF344055)
        BridgeActionTone.WARNING -> Color(0xFFB86814)
    }
}
