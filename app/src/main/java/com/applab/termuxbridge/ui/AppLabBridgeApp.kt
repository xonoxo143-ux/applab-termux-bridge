package com.applab.termuxbridge.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
    var showAdvanced by remember { mutableStateOf(false) }

    fun runAction(action: BridgeAction) {
        statusText = termuxRunner.run(action).message
        pollingToken += 1
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

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0F14)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Header()
                StatusPanel(statusText, treeUri, latestResult)
                BridgeFolderCard(
                    onPickFolder = { folderPicker.launch(null) },
                    onRefresh = {
                        latestResult = resultReader.readLatest(treeUri)
                        previousRunId = latestResult.runId
                        statusText = "Result refreshed."
                    }
                )
                SetupCard(
                    onRunAction = ::runAction,
                    onOpenSettings = { openAppSettings(context) }
                )
                GitWorkbenchCard(onRunAction = ::runAction)
                LocalApkCard(
                    latestApkName = apkInstaller.latestApkName(treeUri),
                    onInstall = { statusText = apkInstaller.installLatest(treeUri).message },
                    onInstallSettings = { apkInstaller.openInstallSettings() }
                )
                LatestResultCard(
                    result = latestResult,
                    onOpenReport = {
                        statusText = if (fileOpener.openReport(treeUri, latestResult.reportFileName())) "Report opened." else "Report not found."
                    },
                    onOpenLog = {
                        statusText = if (fileOpener.openNewestLog(treeUri)) "Log opened." else "Log not found."
                    },
                    onOpenDebugZip = {
                        statusText = if (fileOpener.openDebugZip(treeUri)) "Debug zip opened." else "Debug zip not found."
                    }
                )
                AdvancedCard(
                    expanded = showAdvanced,
                    onToggle = { showAdvanced = !showAdvanced },
                    onClipboard = { statusText = clipboardBridge.writeClipboardSave(treeUri).message }
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "AppLab Bridge",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Phone Git cockpit for approved Termux workflows.",
            color = Color(0xFF9AA4B2),
            style = MaterialTheme.typography.bodyMedium
        )
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
private fun BridgeFolderCard(onPickFolder: () -> Unit, onRefresh: () -> Unit) {
    SectionCard("Bridge Folder") {
        PrimaryButton("Pick Documents/AppLabBridge", onPickFolder)
        SecondaryButton("Refresh Latest Result", onRefresh)
        HintText("Use the same folder Termux writes to: ~/storage/shared/Documents/AppLabBridge.")
    }
}

@Composable
private fun SetupCard(onRunAction: (BridgeAction) -> Unit, onOpenSettings: () -> Unit) {
    SectionCard("Setup") {
        ActionButton(BridgeAction.CHECK_SETUP, onRunAction)
        SecondaryButton("Open App Permission Settings", onOpenSettings)
        HintText("Needed once: Termux storage, allow-external-apps, and Android permission for this app to run Termux commands.")
    }
}

@Composable
private fun GitWorkbenchCard(onRunAction: (BridgeAction) -> Unit) {
    SectionCard("Git Workbench") {
        ActionGroup(
            title = "Repo",
            actions = listOf(
                BridgeAction.LIST_PROJECTS,
                BridgeAction.SHOW_ACTIVE_REPO,
                BridgeAction.SET_ACTIVE_BRIDGE,
                BridgeAction.SET_ACTIVE_LIBRESEED
            ),
            onRunAction = onRunAction
        )
        ActionGroup(
            title = "Inspect",
            actions = listOf(
                BridgeAction.SHOW_STATUS,
                BridgeAction.SHOW_CURRENT_COMMIT,
                BridgeAction.SHOW_BRANCHES,
                BridgeAction.LIST_CHANGED_FILES,
                BridgeAction.SHOW_DIFF_SUMMARY
            ),
            onRunAction = onRunAction
        )
        ActionGroup(
            title = "Update",
            actions = listOf(
                BridgeAction.PULL_CURRENT,
                BridgeAction.PULL_STAGING,
                BridgeAction.CHECKOUT_STAGING,
                BridgeAction.FETCH_REPO
            ),
            onRunAction = onRunAction
        )
        ActionGroup(
            title = "Patch / Publish",
            actions = listOf(
                BridgeAction.RUN_PATCH_SCRIPT,
                BridgeAction.STAGE_ALL,
                BridgeAction.COMMIT_NO_APK,
                BridgeAction.PUSH_CURRENT
            ),
            onRunAction = onRunAction
        )
    }
}

@Composable
private fun LocalApkCard(
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
        SecondaryButton("Install Latest Local APK", onInstall)
        SecondaryButton("Open Install Settings", onInstallSettings)
        HintText("APK download is parked. Put an APK in the bridge folder or download it from GitHub first.")
    }
}

@Composable
private fun LatestResultCard(
    result: BridgeResult,
    onOpenReport: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenDebugZip: () -> Unit
) {
    SectionCard("Latest Result") {
        ResultBlock(result)
        SecondaryButton("Open Report", onOpenReport)
        SecondaryButton("Open Latest Log", onOpenLog)
        SecondaryButton("Open Latest Debug Zip", onOpenDebugZip)
    }
}

@Composable
private fun AdvancedCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    onClipboard: () -> Unit
) {
    SectionCard("Advanced / Parked") {
        SecondaryButton(if (expanded) "Hide Parked Tools" else "Show Parked Tools", onToggle)
        if (expanded) {
            SecondaryButton("Write Clipboard to Inbox", onClipboard)
            HintText("Save-code, source-audit, APK download, and debug bundle buttons are parked until the backend supports them cleanly.")
        } else {
            HintText("Only working Git cockpit actions are shown by default.")
        }
    }
}

@Composable
private fun ActionGroup(
    title: String,
    actions: List<BridgeAction>,
    onRunAction: (BridgeAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Color(0xFFC7D0DA), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
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
private fun ActionButton(action: BridgeAction, onRunAction: (BridgeAction) -> Unit, modifier: Modifier = Modifier) {
    Button(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
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

private fun statusColor(status: String): Color {
    return when (status.lowercase()) {
        "success" -> Color(0xFF5CE38A)
        "failed" -> Color(0xFFFF6B6B)
        "running" -> Color(0xFFFFD166)
        "missing" -> Color(0xFFFFD166)
        else -> Color(0xFF9AA4B2)
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}
