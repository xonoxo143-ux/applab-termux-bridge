package com.applab.termuxbridge.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.applab.termuxbridge.apk.ApkInstaller
import com.applab.termuxbridge.bridge.BridgeAction
import com.applab.termuxbridge.bridge.BridgeResult
import com.applab.termuxbridge.bridge.BridgeResultReader
import com.applab.termuxbridge.bridge.TermuxRunner
import com.applab.termuxbridge.clipboard.ClipboardBridge
import com.applab.termuxbridge.storage.SafBridgeFolder
import com.applab.termuxbridge.storage.SharedFileOpener
import kotlinx.coroutines.delay

private enum class AppScreen(val title: String) {
    HOME("Home"),
    REPO("Repo Workbench"),
    PATCH("Patch Runner"),
    APK("Build / APK"),
    RESULTS("Results"),
    SETUP("Setup")
}

private enum class ActionTone {
    PRIMARY,
    NEUTRAL,
    WARNING
}

@Composable
fun AppLabBridgeApp() {
    val context = LocalContext.current
    val bridgeFolder = remember(context) { SafBridgeFolder(context) }
    val resultReader = remember(context) { BridgeResultReader(bridgeFolder) }
    val termuxRunner = remember(context) { TermuxRunner(context) }
    val clipboardBridge = remember(context) { ClipboardBridge(context, bridgeFolder) }
    val apkInstaller = remember(context) { ApkInstaller(context, bridgeFolder) }
    val fileOpener = remember(context) { SharedFileOpener(context, bridgeFolder) }

    var treeUri by remember { mutableStateOf(bridgeFolder.savedUri()) }
    var latestResult by remember { mutableStateOf(resultReader.readLatest(treeUri)) }
    var statusText by remember { mutableStateOf("Ready") }
    var pollingToken by remember { mutableStateOf(0) }
    var previousRunId by remember { mutableStateOf(latestResult.runId) }
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

    fun runAction(action: BridgeAction) {
        statusText = termuxRunner.run(action).message
        pollingToken += 1
    }

    fun refreshResult() {
        latestResult = resultReader.readLatest(treeUri)
        previousRunId = latestResult.runId
        statusText = "Result refreshed."
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            bridgeFolder.saveUri(uri)
            val prep = bridgeFolder.prepareLayout(uri)
            treeUri = uri
            latestResult = resultReader.readLatest(uri)
            previousRunId = latestResult.runId
            statusText = prep.message
        }
    }

    LaunchedEffect(treeUri) {
        latestResult = resultReader.readLatest(treeUri)
        previousRunId = latestResult.runId
    }

    LaunchedEffect(pollingToken) {
        if (pollingToken == 0) return@LaunchedEffect
        val startRunId = previousRunId
        repeat(15) {
            delay(2_000)
            val result = resultReader.readLatest(treeUri)
            latestResult = result
            if (result.runId.isNotBlank() && result.runId != startRunId) {
                previousRunId = result.runId
                statusText = "Result updated: ${result.title}"
                return@LaunchedEffect
            }
        }
        statusText = "No new result detected. Open Termux or refresh the result."
    }

    val openReport = {
        statusText = if (fileOpener.openReport(treeUri, latestResult.reportFileName())) "Report opened." else "Report not found."
    }
    val openLog = {
        statusText = if (fileOpener.openNewestLog(treeUri)) "Log opened." else "Log not found."
    }
    val openDebugZip = {
        statusText = if (fileOpener.openDebugZip(treeUri)) "Debug zip opened." else "Debug zip not found."
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0F14)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppHeader(
                    currentScreen = currentScreen,
                    latestResult = latestResult,
                    onScreenSelected = { currentScreen = it }
                )
                StatusPanel(statusText, treeUri, latestResult)
                when (currentScreen) {
                    AppScreen.HOME -> HomeScreen(
                        treeUri = treeUri,
                        latestResult = latestResult,
                        onPickFolder = { folderPicker.launch(null) },
                        onRefresh = ::refreshResult,
                        onRunAction = ::runAction,
                        onOpenReport = openReport,
                        onOpenLog = openLog,
                        onGoTo = { currentScreen = it }
                    )
                    AppScreen.REPO -> RepoWorkbenchScreen(onRunAction = ::runAction)
                    AppScreen.PATCH -> PatchRunnerScreen(onRunAction = ::runAction)
                    AppScreen.APK -> ApkScreen(
                        latestApkName = apkInstaller.latestApkName(treeUri),
                        onInstall = { statusText = apkInstaller.installLatest(treeUri).message },
                        onInstallSettings = { apkInstaller.openInstallSettings() }
                    )
                    AppScreen.RESULTS -> ResultsScreen(
                        result = latestResult,
                        onRefresh = ::refreshResult,
                        onOpenReport = openReport,
                        onOpenLog = openLog,
                        onOpenDebugZip = openDebugZip
                    )
                    AppScreen.SETUP -> SetupScreen(
                        onPickFolder = { folderPicker.launch(null) },
                        onRefresh = ::refreshResult,
                        onRunAction = ::runAction,
                        onOpenSettings = { openAppSettings(context) },
                        onClipboard = { statusText = clipboardBridge.writeClipboardSave(treeUri).message }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppHeader(
    currentScreen: AppScreen,
    latestResult: BridgeResult,
    onScreenSelected: (AppScreen) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "AppLab Bridge",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentScreen.title,
                color = Color(0xFF9AA4B2),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = statusChipText(latestResult),
                color = statusColor(latestResult.status),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
        Box {
            OutlinedButton(onClick = { menuOpen = true }) {
                Text("☰ Menu", color = Color.White)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                AppScreen.entries.forEach { screen ->
                    DropdownMenuItem(
                        text = { Text(screen.title) },
                        onClick = {
                            menuOpen = false
                            onScreenSelected(screen)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPanel(status: String, treeUri: Uri?, result: BridgeResult) {
    val folderText = if (treeUri == null) "Folder: not selected" else "Folder: Documents/AppLabBridge selected"
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101821)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(status, color = Color.White, fontWeight = FontWeight.Bold)
            Text(folderText, color = Color(0xFF9AA4B2), style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Last: ${result.status.uppercase()} — ${result.title}",
                color = statusColor(result.status),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HomeScreen(
    treeUri: Uri?,
    latestResult: BridgeResult,
    onPickFolder: () -> Unit,
    onRefresh: () -> Unit,
    onRunAction: (BridgeAction) -> Unit,
    onOpenReport: () -> Unit,
    onOpenLog: () -> Unit,
    onGoTo: (AppScreen) -> Unit
) {
    ReadinessCard(treeUri, latestResult, onPickFolder, onRunAction)
    ActiveRepoCard(onRunAction, onGoTo)
    NextActionCard(treeUri, latestResult, onPickFolder, onRunAction, onOpenReport, onGoTo)
    LatestResultCard(latestResult, onRefresh, onOpenReport, onOpenLog, onGoTo)
}

@Composable
private fun ReadinessCard(
    treeUri: Uri?,
    latestResult: BridgeResult,
    onPickFolder: () -> Unit,
    onRunAction: (BridgeAction) -> Unit
) {
    SectionCard("System Readiness") {
        StatusLine("Bridge folder", if (treeUri == null) "missing" else "selected", treeUri != null)
        StatusLine("Latest result", if (latestResult.isLoaded) "loaded" else "missing", latestResult.isLoaded)
        StatusLine("Last action", latestResult.action.ifBlank { "none" }, latestResult.status.equals("success", true))
        if (treeUri == null) {
            PrimaryButton("Pick Bridge Folder", onPickFolder)
        } else {
            ActionButton(BridgeAction.CHECK_SETUP, onRunAction)
        }
    }
}

@Composable
private fun ActiveRepoCard(
    onRunAction: (BridgeAction) -> Unit,
    onGoTo: (AppScreen) -> Unit
) {
    SectionCard("Active Repo") {
        HintText("Choose the repo, then refresh status before patching or publishing.")
        ActionButton(BridgeAction.SHOW_ACTIVE_REPO, onRunAction)
        ActionButton(BridgeAction.SHOW_STATUS, onRunAction)
        SecondaryButton("Open Repo Workbench") { onGoTo(AppScreen.REPO) }
    }
}

@Composable
private fun NextActionCard(
    treeUri: Uri?,
    latestResult: BridgeResult,
    onPickFolder: () -> Unit,
    onRunAction: (BridgeAction) -> Unit,
    onOpenReport: () -> Unit,
    onGoTo: (AppScreen) -> Unit
) {
    val action = recommendedAction(treeUri, latestResult)
    SectionCard("Recommended Next") {
        Text(action.title, color = Color.White, fontWeight = FontWeight.Bold)
        HintText(action.detail)
        when {
            action.pickFolder -> PrimaryButton(action.buttonLabel, onPickFolder)
            action.openReport -> PrimaryButton(action.buttonLabel, onOpenReport)
            action.screen != null -> PrimaryButton(action.buttonLabel) { onGoTo(action.screen) }
            action.bridgeAction != null -> ActionButton(action.bridgeAction, onRunAction, tone = action.tone)
        }
    }
}

@Composable
private fun LatestResultCard(
    result: BridgeResult,
    onRefresh: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenLog: () -> Unit,
    onGoTo: (AppScreen) -> Unit
) {
    SectionCard("Latest Result") {
        ResultBlock(result)
        PrimaryButton("Open Report", onOpenReport)
        SecondaryButton("Open Latest Log", onOpenLog)
        SecondaryButton("Refresh Result", onRefresh)
        SecondaryButton("All Results Tools") { onGoTo(AppScreen.RESULTS) }
    }
}

@Composable
private fun RepoWorkbenchScreen(onRunAction: (BridgeAction) -> Unit) {
    SectionCard("Repo Selection") {
        ActionGroup(
            actions = listOf(
                BridgeAction.LIST_PROJECTS,
                BridgeAction.SHOW_ACTIVE_REPO,
                BridgeAction.SET_ACTIVE_BRIDGE,
                BridgeAction.SET_ACTIVE_LIBRESEED
            ),
            onRunAction = onRunAction
        )
    }
    SectionCard("Inspect") {
        ActionGroup(
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
    SectionCard("Update") {
        HintText("Pull/checkout actions should be used on a clean repo.")
        ActionGroup(
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
private fun PatchRunnerScreen(onRunAction: (BridgeAction) -> Unit) {
    SectionCard("Patch Runner") {
        HintText("Runs Documents/AppLabBridge/patches/patch.sh against the active repo. Review status and branch first.")
        ActionButton(BridgeAction.SHOW_ACTIVE_REPO, onRunAction)
        ActionButton(BridgeAction.SHOW_STATUS, onRunAction)
        ActionButton(BridgeAction.RUN_PATCH_SCRIPT, onRunAction, tone = ActionTone.WARNING)
        ActionButton(BridgeAction.LIST_CHANGED_FILES, onRunAction)
        ActionButton(BridgeAction.SHOW_DIFF_SUMMARY, onRunAction)
    }
    SectionCard("Publish") {
        HintText("Use only after reviewing the diff. Commit action appends [no apk] if needed.")
        ActionButton(BridgeAction.STAGE_ALL, onRunAction, tone = ActionTone.WARNING)
        ActionButton(BridgeAction.COMMIT_NO_APK, onRunAction, tone = ActionTone.WARNING)
        ActionButton(BridgeAction.PUSH_CURRENT, onRunAction, tone = ActionTone.WARNING)
    }
}

@Composable
private fun ApkScreen(
    latestApkName: String?,
    onInstall: () -> Unit,
    onInstallSettings: () -> Unit
) {
    SectionCard("Local APK") {
        Text(
            text = "Latest local APK: ${latestApkName ?: "none found"}",
            color = Color(0xFF9AA4B2),
            style = MaterialTheme.typography.bodySmall
        )
        PrimaryButton("Install Latest Local APK", onInstall)
        SecondaryButton("Open Install Settings", onInstallSettings)
        HintText("Artifact download is parked. Download APKs from GitHub or place one in the bridge folder first.")
    }
}

@Composable
private fun ResultsScreen(
    result: BridgeResult,
    onRefresh: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenDebugZip: () -> Unit
) {
    SectionCard("Result Detail") {
        ResultBlock(result)
        PrimaryButton("Open Report", onOpenReport)
        SecondaryButton("Open Latest Log", onOpenLog)
        SecondaryButton("Open Latest Debug Zip", onOpenDebugZip)
        SecondaryButton("Refresh Result", onRefresh)
    }
}

@Composable
private fun SetupScreen(
    onPickFolder: () -> Unit,
    onRefresh: () -> Unit,
    onRunAction: (BridgeAction) -> Unit,
    onOpenSettings: () -> Unit,
    onClipboard: () -> Unit
) {
    SectionCard("Bridge Folder") {
        PrimaryButton("Pick Documents/AppLabBridge", onPickFolder)
        SecondaryButton("Refresh Latest Result", onRefresh)
        HintText("Termux should write to ~/storage/shared/Documents/AppLabBridge.")
    }
    SectionCard("Termux Setup") {
        ActionButton(BridgeAction.CHECK_SETUP, onRunAction)
        SecondaryButton("Open App Permission Settings", onOpenSettings)
        HintText("Required: Termux storage access, allow-external-apps=true, and Android permission for this app to run Termux commands.")
    }
    SectionCard("Parked Tools") {
        SecondaryButton("Write Clipboard to Inbox", onClipboard)
        HintText("Save-code, source audit, APK download, and debug bundle buttons remain parked until their backend paths are clean.")
    }
}

@Composable
private fun ActionGroup(
    actions: List<BridgeAction>,
    onRunAction: (BridgeAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { action ->
            ActionButton(action, onRunAction)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121A24)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String, ok: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF9AA4B2), style = MaterialTheme.typography.bodySmall)
        Text(value, color = if (ok) Color(0xFF5CE38A) else Color(0xFFFFD166), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ActionButton(
    action: BridgeAction,
    onRunAction: (BridgeAction) -> Unit,
    modifier: Modifier = Modifier,
    tone: ActionTone = ActionTone.PRIMARY
) {
    Button(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor(tone)),
        onClick = { onRunAction(action) }
    ) {
        Text(action.label)
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
        onClick = onClick
    ) {
        Text(label)
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        onClick = onClick
    ) {
        Text(label, color = Color.White)
    }
}

@Composable
private fun HintText(text: String) {
    Text(text = text, color = Color(0xFF9AA4B2), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ResultBlock(result: BridgeResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080D12), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(result.title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(result.summary, color = Color(0xFFC7D0DA))
        Text("Action: ${result.action.ifBlank { "none" }}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        Text("Run: ${result.runId.ifBlank { "none" }}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        Text("Exit: ${result.exitCode?.toString() ?: "n/a"}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        result.nextAction?.let { Text("Next: $it", color = Color(0xFFFFD166)) }
        if (result.artifacts.isNotEmpty()) {
            Text("Artifacts: ${result.artifacts.joinToString()}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        }
    }
}

private data class RecommendedAction(
    val title: String,
    val detail: String,
    val buttonLabel: String,
    val bridgeAction: BridgeAction? = null,
    val screen: AppScreen? = null,
    val pickFolder: Boolean = false,
    val openReport: Boolean = false,
    val tone: ActionTone = ActionTone.PRIMARY
)

private fun recommendedAction(treeUri: Uri?, result: BridgeResult): RecommendedAction {
    if (treeUri == null) {
        return RecommendedAction(
            title = "Pick the bridge folder",
            detail = "The app needs Documents/AppLabBridge before it can read Termux results.",
            buttonLabel = "Pick Bridge Folder",
            pickFolder = true
        )
    }
    if (!result.isLoaded) {
        return RecommendedAction(
            title = "Check setup",
            detail = "No result has been loaded from the bridge folder yet.",
            buttonLabel = BridgeAction.CHECK_SETUP.label,
            bridgeAction = BridgeAction.CHECK_SETUP
        )
    }
    if (result.status.equals("failed", true)) {
        return RecommendedAction(
            title = "Inspect the failure",
            detail = "The last action failed. Open the report before running more actions.",
            buttonLabel = "Open Failure Report",
            openReport = true,
            tone = ActionTone.WARNING
        )
    }
    return when (result.action) {
        "check_setup" -> RecommendedAction("Choose a repo", "Setup passed. Select an active repo or check the current one.", BridgeAction.SHOW_ACTIVE_REPO.label, BridgeAction.SHOW_ACTIVE_REPO)
        "set_active_bridge", "set_active_libreseed", "show_active_repo" -> RecommendedAction("Refresh repo status", "The active repo changed or was checked. Pull a fresh status next.", BridgeAction.SHOW_STATUS.label, BridgeAction.SHOW_STATUS)
        "show_status", "pull_current", "pull_staging", "checkout_staging" -> RecommendedAction("Review changed files", "Status is current. Check whether anything changed before patching or publishing.", BridgeAction.LIST_CHANGED_FILES.label, BridgeAction.LIST_CHANGED_FILES)
        "run_patch_script" -> RecommendedAction("Review patch result", "A patch ran. Review changed files and diff before staging.", BridgeAction.SHOW_DIFF_SUMMARY.label, BridgeAction.SHOW_DIFF_SUMMARY)
        "list_changed_files", "show_diff_summary", "show_full_diff" -> RecommendedAction("Open Patch / Publish", "If the diff is correct, stage and commit from the guarded workflow screen.", "Open Patch / Publish", screen = AppScreen.PATCH, tone = ActionTone.WARNING)
        "stage_all" -> RecommendedAction("Commit staged changes", "Files were staged. Commit with [no apk] unless this change should build an APK.", BridgeAction.COMMIT_NO_APK.label, BridgeAction.COMMIT_NO_APK, tone = ActionTone.WARNING)
        "commit_no_apk" -> RecommendedAction("Push current branch", "A commit was created. Push it when ready.", BridgeAction.PUSH_CURRENT.label, BridgeAction.PUSH_CURRENT, tone = ActionTone.WARNING)
        "push_current" -> RecommendedAction("Refresh status", "Push completed. Refresh the active repo status.", BridgeAction.SHOW_STATUS.label, BridgeAction.SHOW_STATUS)
        else -> RecommendedAction("Refresh status", "Start by checking the active repo state.", BridgeAction.SHOW_STATUS.label, BridgeAction.SHOW_STATUS)
    }
}

private fun statusChipText(result: BridgeResult): String {
    val action = result.action.ifBlank { "no action" }
    val exit = result.exitCode?.toString() ?: "n/a"
    return "${result.status.uppercase()} · $action · exit $exit"
}

private fun statusColor(status: String): Color {
    return when (status.lowercase()) {
        "success" -> Color(0xFF5CE38A)
        "failed" -> Color(0xFFFF6B6B)
        "running" -> Color(0xFFFFD166)
        "missing" -> Color(0xFFFFD166)
        else -> Color(0xFF9AA4B2)
    }
}

private fun buttonColor(tone: ActionTone): Color {
    return when (tone) {
        ActionTone.PRIMARY -> Color(0xFF1B6BFF)
        ActionTone.NEUTRAL -> Color(0xFF344055)
        ActionTone.WARNING -> Color(0xFFB86814)
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}
