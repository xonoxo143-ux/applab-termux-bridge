package com.applab.termuxbridge.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.applab.termuxbridge.apk.ApkInstaller
import com.applab.termuxbridge.bridge.BridgeAction
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
    var currentScreen by remember { mutableStateOf(BridgeAppScreen.HOME) }
    var pendingAction by remember { mutableStateOf<BridgeAction?>(null) }

    fun executeAction(action: BridgeAction) {
        pendingAction = null
        statusText = termuxRunner.run(action).message
        pollingToken += 1
    }

    fun requestAction(action: BridgeAction) {
        if (bridgeRequiresConfirmation(action)) {
            pendingAction = action
            statusText = "Confirm ${action.label} before running."
        } else {
            executeAction(action)
        }
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
                BridgeAppHeader(
                    currentScreen = currentScreen,
                    latestResult = latestResult,
                    onScreenSelected = { currentScreen = it }
                )
                BridgeStatusPanel(statusText, treeUri, latestResult)
                pendingAction?.let { action ->
                    BridgeConfirmActionCard(
                        action = action,
                        latestResult = latestResult,
                        onConfirm = { executeAction(action) },
                        onCancel = {
                            pendingAction = null
                            statusText = "Action cancelled."
                        }
                    )
                }
                when (currentScreen) {
                    BridgeAppScreen.HOME -> BridgeHomeScreen(
                        treeUri = treeUri,
                        latestResult = latestResult,
                        onPickFolder = { folderPicker.launch(null) },
                        onRefresh = ::refreshResult,
                        onRunAction = ::requestAction,
                        onOpenReport = openReport,
                        onOpenLog = openLog,
                        onGoTo = { currentScreen = it }
                    )
                    BridgeAppScreen.REPO -> BridgeRepoWorkbenchScreen(onRunAction = ::requestAction)
                    BridgeAppScreen.PATCH -> BridgePatchRunnerScreen(latestResult = latestResult, onRunAction = ::requestAction)
                    BridgeAppScreen.APK -> BridgeApkScreen(
                        latestApkName = apkInstaller.latestApkName(treeUri),
                        onRunAction = ::requestAction,
                        onInstall = { statusText = apkInstaller.installLatest(treeUri).message },
                        onInstallSettings = { apkInstaller.openInstallSettings() }
                    )
                    BridgeAppScreen.RESULTS -> BridgeResultsScreen(
                        result = latestResult,
                        onRefresh = ::refreshResult,
                        onOpenReport = openReport,
                        onOpenLog = openLog,
                        onOpenDebugZip = openDebugZip
                    )
                    BridgeAppScreen.SETUP -> BridgeSetupScreen(
                        onPickFolder = { folderPicker.launch(null) },
                        onRefresh = ::refreshResult,
                        onRunAction = ::requestAction,
                        onOpenSettings = { openAppSettings(context) },
                        onClipboard = { statusText = clipboardBridge.writeClipboardSave(treeUri).message }
                    )
                }
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}
