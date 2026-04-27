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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.applab.termuxbridge.bridge.BridgeSection
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
        statusText = "No new result detected yet. Refresh manually or check Termux."
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
                    onRunAction = { action ->
                        statusText = termuxRunner.run(action).message
                        pollingToken += 1
                    },
                    onOpenSettings = { openAppSettings(context) }
                )
                ActionSection(BridgeSection.REPO) { action ->
                    statusText = termuxRunner.run(action).message
                    pollingToken += 1
                }
                ApkSection(
                    latestApkName = apkInstaller.latestApkName(treeUri),
                    onRunAction = { action ->
                        statusText = termuxRunner.run(action).message
                        pollingToken += 1
                    },
                    onInstall = { statusText = apkInstaller.installLatest(treeUri).message },
                    onInstallSettings = { apkInstaller.openInstallSettings() }
                )
                SaveSection(
                    onClipboard = { statusText = clipboardBridge.writeClipboardSave(treeUri).message },
                    onRunAction = { action ->
                        statusText = termuxRunner.run(action).message
                        pollingToken += 1
                    }
                )
                ActionSection(BridgeSection.AUDIT) { action ->
                    statusText = termuxRunner.run(action).message
                    pollingToken += 1
                }
                ActionSection(BridgeSection.DEBUG) { action ->
                    statusText = termuxRunner.run(action).message
                    pollingToken += 1
                }
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
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "AppLab Termux Bridge",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Button cockpit for approved Termux-backed workflows.",
            color = Color(0xFF9AA4B2),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatusPanel(status: String, treeUri: Uri?, result: BridgeResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101821)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(status, color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                text = "Folder: ${treeUri?.toString() ?: "not selected"}",
                color = Color(0xFF9AA4B2),
                style = MaterialTheme.typography.bodySmall
            )
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Pick Folder", onPickFolder)
            SecondaryButton("Refresh Result", onRefresh)
        }
        Text(
            text = "Recommended: Documents/AppLabBridge. Termux should use ~/storage/shared/Documents/AppLabBridge.",
            color = Color(0xFF9AA4B2),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SetupCard(onRunAction: (BridgeAction) -> Unit, onOpenSettings: () -> Unit) {
    SectionCard("Setup") {
        ActionButton(BridgeAction.CHECK_SETUP, onRunAction)
        SecondaryButton("Open App Permission Settings", onOpenSettings)
        Text(
            text = "Setup checklist: install script pack, run termux-setup-storage, set allow-external-apps=true, grant this app Termux command permission, then pick Documents/AppLabBridge.",
            color = Color(0xFF9AA4B2),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ActionSection(section: BridgeSection, onRunAction: (BridgeAction) -> Unit) {
    SectionCard(section.title) {
        ActionGrid(BridgeAction.forSection(section), onRunAction)
    }
}

@Composable
private fun ApkSection(
    latestApkName: String?,
    onRunAction: (BridgeAction) -> Unit,
    onInstall: () -> Unit,
    onInstallSettings: () -> Unit
) {
    SectionCard("APK") {
        ActionGrid(listOf(BridgeAction.CHECK_LATEST_APK, BridgeAction.DOWNLOAD_LATEST_APK), onRunAction)
        Text(
            text = "Latest local APK: ${latestApkName ?: "none found"}",
            color = Color(0xFF9AA4B2),
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Install Latest APK", onInstall)
            SecondaryButton("Open Install Settings", onInstallSettings)
        }
    }
}

@Composable
private fun SaveSection(onClipboard: () -> Unit, onRunAction: (BridgeAction) -> Unit) {
    SectionCard("Save Codes") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SecondaryButton("Write Clipboard to Inbox", onClipboard)
            ActionButton(BridgeAction.DECODE_SAVE, onRunAction)
        }
        ActionButton(BridgeAction.VALIDATE_SAVE, onRunAction)
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Open Report", onOpenReport)
            SecondaryButton("Open Latest Log", onOpenLog)
        }
        SecondaryButton("Open Latest Debug Zip", onOpenDebugZip)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable Column.() -> Unit) {
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
private fun ActionGrid(actions: List<BridgeAction>, onRunAction: (BridgeAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                row.forEach { action ->
                    ActionButton(action, onRunAction, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActionButton(action: BridgeAction, onRunAction: (BridgeAction) -> Unit, modifier: Modifier = Modifier) {
    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
        onClick = { onRunAction(action) }
    ) {
        Text(action.label)
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)), onClick = onClick) {
        Text(label)
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Text(label, color = Color.White)
    }
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
