package com.applab.termuxbridge.ui

import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.applab.termuxbridge.bridge.BridgeAction
import com.applab.termuxbridge.bridge.BridgeResult

enum class BridgeAppScreen(val title: String) {
    HOME("Home"),
    REPO("Repo"),
    PATCH("Patch"),
    APK("Updates"),
    RESULTS("Results"),
    ACTION_CATALOG("Actions"),
    ADVANCED("Advanced"),
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
            title = "Choose the shared folder",
            detail = "The app needs folder access before it can read reports, logs, updates, or patch files.",
            buttonLabel = "Choose Folder",
            pickFolder = true
        )
    }
    if (!result.isLoaded) {
        return BridgeRecommendedAction(
            title = "Check setup",
            detail = "No saved result is loaded yet. Ask Termux to write a setup report.",
            buttonLabel = "Check Setup",
            bridgeAction = BridgeAction.CHECK_SETUP
        )
    }
    if (result.status.equals("failed", true)) {
        return BridgeRecommendedAction(
            title = "Read the failure report",
            detail = result.diagnosticHint ?: "The last action failed. Read the report before running more actions.",
            buttonLabel = "Report",
            openReport = true,
            tone = BridgeActionTone.WARNING
        )
    }
    if (result.action == "download_latest_apk") {
        return BridgeRecommendedAction("Install update", "The latest APK should be in the shared bridge folder.", "Updates", screen = BridgeAppScreen.APK)
    }
    if (result.action == "list_actions") {
        return BridgeRecommendedAction("Open Actions", "The backend action list was refreshed.", "Actions", screen = BridgeAppScreen.ACTION_CATALOG)
    }
    if (result.action == "update_dispatcher") {
        return BridgeRecommendedAction("Check setup", "The backend was updated. Check setup before other actions.", "Check", BridgeAction.CHECK_SETUP)
    }
    if ((result.stagedFiles ?: 0) > 0) {
        return BridgeRecommendedAction("Commit staged changes", "There are staged files ready to commit.", BridgeAction.COMMIT_NO_APK.label, BridgeAction.COMMIT_NO_APK, tone = BridgeActionTone.WARNING)
    }
    if (result.ahead != null && result.ahead > 0 && result.dirty == false) {
        return BridgeRecommendedAction("Push branch", "The selected repo has local commits ahead of upstream.", BridgeAction.PUSH_CURRENT.label, BridgeAction.PUSH_CURRENT, tone = BridgeActionTone.WARNING)
    }
    if (result.dirty == true && (result.action == "show_status" || result.action == "check_repo")) {
        return BridgeRecommendedAction("View changes", "The selected repo has changed files. Review them before staging.", "Changes", BridgeAction.LIST_CHANGED_FILES)
    }
    if (result.dirty == false && (result.action == "show_status" || result.action == "check_repo")) {
        return if (result.hasPatchFile == true) {
            BridgeRecommendedAction("Ready for patch", "The repo is clean and patches/patch.sh is available.", "Patch", screen = BridgeAppScreen.PATCH)
        } else {
            BridgeRecommendedAction("Repo is clean", "Update the repo, place a patch script, or choose another workflow.", "Update", BridgeAction.PULL_CURRENT)
        }
    }
    return when (result.action) {
        "check_setup" -> BridgeRecommendedAction("Choose a repo", "Setup passed. Choose a repo or scan Termux projects first.", "Repo", screen = BridgeAppScreen.REPO)
        "list_projects" -> BridgeRecommendedAction("Choose a repo", "Repos were scanned. Open Repo and tap the repo you want active.", "Repo", screen = BridgeAppScreen.REPO)
        "select_configured_repo", "set_active_bridge", "set_active_libreseed", "show_active_repo", "clone_bridge", "clone_libreseed" -> BridgeRecommendedAction("Check repo", "The selected repo changed. Check its current state next.", "Check", BridgeAction.CHECK_REPO)
        "check_latest_apk" -> BridgeRecommendedAction("Download update", "A successful app build is available if the report found a workflow run.", "Download", BridgeAction.DOWNLOAD_LATEST_APK, tone = BridgeActionTone.WARNING)
        "pull_current", "pull_staging", "checkout_staging", "fetch_repo" -> BridgeRecommendedAction("View changes", "Repo state changed. Review files before patching or publishing.", "Changes", BridgeAction.LIST_CHANGED_FILES)
        "run_patch_script" -> BridgeRecommendedAction("Review patch", "A patch ran. Review changed files before staging.", "Changes", BridgeAction.LIST_CHANGED_FILES)
        "list_changed_files", "show_diff_summary", "show_full_diff" -> BridgeRecommendedAction("Open Patch", "If the changes are correct, stage and commit from Patch.", "Patch", screen = BridgeAppScreen.PATCH, tone = BridgeActionTone.WARNING)
        "stage_all" -> BridgeRecommendedAction("Commit staged changes", "Files were staged. Commit with [no apk] unless this change should build an APK.", BridgeAction.COMMIT_NO_APK.label, BridgeAction.COMMIT_NO_APK, tone = BridgeActionTone.WARNING)
        "commit_no_apk" -> BridgeRecommendedAction("Push branch", "A commit was created. Push it when ready.", BridgeAction.PUSH_CURRENT.label, BridgeAction.PUSH_CURRENT, tone = BridgeActionTone.WARNING)
        "push_current" -> BridgeRecommendedAction("Check repo", "Push completed. Check the selected repo again.", "Check", BridgeAction.CHECK_REPO)
        "create_debug_zip" -> BridgeRecommendedAction("Open debug bundle", "A debug zip should now be available from Results.", "Results", screen = BridgeAppScreen.RESULTS)
        else -> BridgeRecommendedAction("Check repo", "Start by checking the selected repo state.", "Check", BridgeAction.CHECK_REPO)
    }
}

fun bridgeRequiresConfirmation(action: BridgeAction): Boolean {
    return action in setOf(
        BridgeAction.UPDATE_DISPATCHER,
        BridgeAction.CLONE_BRIDGE,
        BridgeAction.CLONE_LIBRESEED,
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
        BridgeAction.UPDATE_DISPATCHER -> "Updates the live Termux backend from the bridge repo. This changes backend behavior without installing a new APK."
        BridgeAction.CLONE_BRIDGE -> "Clones or updates the bridge repo under ~/projects and selects it as the active repo."
        BridgeAction.CLONE_LIBRESEED -> "Clones or updates the LibreSeed repo under ~/projects and selects it as the active repo."
        BridgeAction.PULL_STAGING, BridgeAction.CHECKOUT_STAGING -> "Changes the selected repo branch. Use on a clean working tree."
        BridgeAction.RUN_PATCH_SCRIPT -> "Runs patches/patch.sh against the selected repo. Review repo and branch before confirming."
        BridgeAction.STAGE_ALL -> "Stages every changed file in the selected repo. Review the diff first."
        BridgeAction.COMMIT_NO_APK -> "Creates a local commit. The backend appends [no apk] when needed."
        BridgeAction.PUSH_CURRENT -> "Pushes the current branch to GitHub. Confirm the branch and commit first."
        BridgeAction.DOWNLOAD_LATEST_APK -> "Downloads the latest GitHub APK artifact into the shared bridge folder. Install remains a separate Android confirmation."
        BridgeAction.LIST_ACTIONS -> "Writes the backend action list to config/actions.json and a readable report to reports/list_actions.txt."
        else -> "This action changes repo or backend state. Confirm before running."
    }
}

fun bridgeRepoHeaderText(result: BridgeResult): String {
    if (result.repoName == null && result.branch == null) return "No repo selected"
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
