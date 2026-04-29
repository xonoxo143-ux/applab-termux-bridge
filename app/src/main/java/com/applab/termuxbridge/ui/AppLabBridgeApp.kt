package com.applab.termuxbridge.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.applab.termuxbridge.actions.BackendActionRegistryReader
import com.applab.termuxbridge.actions.BackendActionRegistryState
import com.applab.termuxbridge.apk.ApkInstaller
import com.applab.termuxbridge.bootstrap.BackendBootstrapper
import com.applab.termuxbridge.bridge.BridgeAction
import com.applab.termuxbridge.bridge.BridgeCommandPhase
import com.applab.termuxbridge.bridge.BridgePendingCommand
import com.applab.termuxbridge.bridge.BridgeResultReader
import com.applab.termuxbridge.bridge.RunResult
import com.applab.termuxbridge.bridge.TermuxRunner
import com.applab.termuxbridge.bridge.userLabel
import com.applab.termuxbridge.clipboard.ClipboardBridge
import com.applab.termuxbridge.diagnostics.AppBridgeLogger
import com.applab.termuxbridge.storage.SafBridgeFolder
import com.applab.termuxbridge.storage.SharedFileOpener
import kotlinx.coroutines.delay

@Composable
fun AppLabBridgeApp() {
    val context = LocalContext.current
    val bridgeFolder = remember(context) { SafBridgeFolder(context) }
    val resultReader = remember(context) { BridgeResultReader(bridgeFolder) }
    val registryReader = remember(context) { BackendActionRegistryReader(bridgeFolder) }
    val repoChoiceReader = remember(context) { BridgeRepoChoiceReader(bridgeFolder) }
    val termuxRunner = remember(context) { TermuxRunner(context) }
    val bootstrapper = remember(context) { BackendBootstrapper(context, bridgeFolder) }
    val appLogger = remember(context) { AppBridgeLogger(bridgeFolder) }
    val clipboardBridge = remember(context) { ClipboardBridge(context, bridgeFolder) }
    val apkInstaller = remember(context) { ApkInstaller(context, bridgeFolder) }
    val fileOpener = remember(context) { SharedFileOpener(context, bridgeFolder) }
    val curationStore = remember(context) { BridgeActionCurationStore(context) }

    var treeUri by remember { mutableStateOf(bridgeFolder.savedUri()) }
    var latestResult by remember { mutableStateOf(resultReader.readLatest(treeUri)) }
    var registryState by remember { mutableStateOf<BackendActionRegistryState>(registryReader.read(treeUri)) }
    var repoChoices by remember { mutableStateOf(repoChoiceReader.readChoices(treeUri)) }
    var curationState by remember { mutableStateOf(curationStore.load()) }
    var statusText by remember { mutableStateOf("Ready") }
    var pollingToken by remember { mutableStateOf(0) }
    var previousRunId by remember { mutableStateOf(latestResult.runId) }
    var currentScreen by remember { mutableStateOf(BridgeAppScreen.HOME) }
    var pendingAction by remember { mutableStateOf<BridgeAction?>(null) }
    var pendingCommand by remember { mutableStateOf<BridgePendingCommand?>(null) }
    var folderPrompted by remember { mutableStateOf(false) }
    var permissionPrompted by remember { mutableStateOf(false) }
    var autoScannedAfterSetup by remember { mutableStateOf(false) }

    fun updateCuration(next: ActionCurationState, event: String, actionId: String = "") {
        curationState = next
        appLogger.log(treeUri, event, "actionId=$actionId pinned=${next.pinnedIds.size} hidden=${next.hiddenIds.size} customize=${next.customizeMode}")
        statusText = when (event) {
            "action.pin.toggle" -> if (next.isPinned(actionId)) "Pinned $actionId." else "Unpinned $actionId."
            "action.hidden" -> "Hidden $actionId from normal views."
            "action.unhidden" -> "Unhid $actionId."
            "action.customize" -> if (next.customizeMode) "Customize Actions enabled." else "Customize Actions disabled."
            "action.layout.reset" -> "Action layout reset."
            else -> "Action curation updated."
        }
    }

    val termuxPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        statusText = if (granted) "Termux command permission granted." else "Termux command permission was not granted. Open this app's permissions and allow Run commands in Termux."
        appLogger.log(treeUri, "permission.termux.result", "granted=$granted")
    }

    fun requestTermuxPermission(reason: String = "Termux command permission is required before running backend actions.") {
        val permission = TermuxRunner.RUN_COMMAND_PERMISSION
        appLogger.log(treeUri, "permission.termux.request", "reason=$reason installed=${termuxRunner.isTermuxInstalled()} hasPermission=${termuxRunner.hasRunCommandPermission()}")
        if (!termuxRunner.isTermuxInstalled()) {
            statusText = "Termux is not installed or not visible to this app. Install Termux first."
            return
        }
        if (termuxRunner.hasRunCommandPermission()) {
            statusText = "Termux command permission already granted."
            return
        }
        termuxPermissionLauncher.launch(permission)
    }

    fun logRunResult(prefix: String, result: RunResult) {
        appLogger.log(treeUri, prefix, "started=${result.started} phase=${result.phase} path=${result.commandPath} args=${result.arguments.joinToString(",")} stdinBytes=${result.stdinBytes} termuxInstalled=${result.termuxInstalled} runPermission=${result.runCommandPermission} message=${result.message}")
    }

    fun reloadRegistry() {
        registryState = registryReader.read(treeUri)
        statusText = when (val state = registryState) {
            BackendActionRegistryState.MissingFolder -> "No shared folder selected for registry."
            BackendActionRegistryState.MissingRegistry -> "No action registry found. Run List Backend Actions first."
            is BackendActionRegistryState.ParseError -> "Action registry parse error: ${state.message}"
            is BackendActionRegistryState.Loaded -> "Loaded ${state.registry.actions.size} backend action(s)."
        }
        appLogger.log(treeUri, "registry.reload", "state=${registryState::class.java.simpleName} status=$statusText")
    }

    fun startPolling(action: BridgeAction, phase: BridgeCommandPhase = BridgeCommandPhase.WAITING_FOR_RESULT) {
        appLogger.log(treeUri, "poll.start", "expected=${action.id} previousRunId=${latestResult.runId} phase=$phase")
        pendingCommand = BridgePendingCommand(action = action, previousRunId = latestResult.runId, phase = phase)
        previousRunId = latestResult.runId
        pollingToken += 1
    }

    fun executeAction(action: BridgeAction) {
        pendingAction = null
        appLogger.log(treeUri, "action.tap", "action=${action.id} label=${action.label}")
        if (!termuxRunner.hasRunCommandPermission()) {
            statusText = "Termux command permission is required before running ${action.label}. Requesting permission now."
            requestTermuxPermission("before action ${action.id}")
            pendingCommand = BridgePendingCommand(action = action, previousRunId = latestResult.runId, phase = BridgeCommandPhase.LAUNCH_FAILED)
            return
        }
        val runResult = termuxRunner.run(action)
        logRunResult("termux.launch", runResult)
        statusText = "${runResult.phase.userLabel()}: ${runResult.message}"
        if (runResult.started) startPolling(action, runResult.phase) else pendingCommand = BridgePendingCommand(action = action, previousRunId = latestResult.runId, phase = BridgeCommandPhase.LAUNCH_FAILED)
    }

    fun runBackendBootstrap() {
        pendingAction = null
        appLogger.log(treeUri, "bootstrap.tap", "uriSelected=${treeUri != null}")
        if (!termuxRunner.hasRunCommandPermission()) {
            statusText = "Termux command permission is required before bootstrap. Requesting permission now."
            requestTermuxPermission("before backend bootstrap")
            return
        }
        val writeResult = bootstrapper.writeBootstrapFiles(treeUri)
        appLogger.log(treeUri, "bootstrap.write", "success=${writeResult.success} message=${writeResult.message}")
        statusText = writeResult.message
        if (!writeResult.success) return
        val script = bootstrapper.bootstrapScriptText()
        appLogger.log(treeUri, "bootstrap.script", "success=${script.success} bytes=${script.text.toByteArray().size} message=${script.message}")
        if (!script.success) {
            statusText = script.message
            return
        }
        val runResult = termuxRunner.runBootstrapInstaller(script.text)
        logRunResult("bootstrap.launch", runResult)
        statusText = "${runResult.phase.userLabel()}: ${runResult.message}"
        if (runResult.started) startPolling(BridgeAction.CHECK_SETUP, runResult.phase) else pendingCommand = BridgePendingCommand(action = BridgeAction.CHECK_SETUP, previousRunId = latestResult.runId, phase = BridgeCommandPhase.LAUNCH_FAILED)
    }

    fun requestAction(action: BridgeAction) {
        appLogger.log(treeUri, "action.request", "action=${action.id} requiresConfirmation=${bridgeRequiresConfirmation(action)}")
        if (!termuxRunner.hasRunCommandPermission()) {
            statusText = "Termux command permission is required before running ${action.label}. Requesting permission now."
            requestTermuxPermission("before requested action ${action.id}")
            return
        }
        if (bridgeRequiresConfirmation(action)) {
            pendingAction = action
            statusText = "Confirm ${action.label} before running."
        } else {
            executeAction(action)
        }
    }

    fun refreshResult() {
        appLogger.log(treeUri, "result.reload.before", "oldRunId=${latestResult.runId} oldAction=${latestResult.action}")
        latestResult = resultReader.readLatest(treeUri)
        repoChoices = repoChoiceReader.readChoices(treeUri)
        previousRunId = latestResult.runId
        pendingCommand = null
        statusText = "Reloaded saved result file."
        appLogger.log(treeUri, "result.reload.after", "newRunId=${latestResult.runId} action=${latestResult.action} status=${latestResult.status} title=${latestResult.title}")
    }

    fun chooseRepo(choice: BridgeRepoChoice) {
        if (!repoChoiceReader.writeSelectedRepo(treeUri, choice)) {
            statusText = "Could not write selected repo config. Check shared folder permission."
            appLogger.log(treeUri, "repo.choose.failed", "choice=${choice.termuxPath}")
            return
        }
        val action = when {
            choice.source == BridgeRepoChoiceSource.GITHUB -> BridgeAction.SELECT_CONFIGURED_REPO
            choice.name == "applab-termux-bridge" -> BridgeAction.SET_ACTIVE_BRIDGE
            choice.name == "libreseed-labs-android" -> BridgeAction.SET_ACTIVE_LIBRESEED
            else -> BridgeAction.SELECT_CONFIGURED_REPO
        }
        appLogger.log(treeUri, "repo.choose", "choice=${choice.termuxPath} source=${choice.source} action=${action.id}")
        executeAction(action)
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            bridgeFolder.saveUri(uri)
            val prep = bridgeFolder.prepareLayout(uri)
            treeUri = uri
            latestResult = resultReader.readLatest(uri)
            registryState = registryReader.read(uri)
            repoChoices = repoChoiceReader.readChoices(uri)
            previousRunId = latestResult.runId
            pendingCommand = null
            statusText = prep.message
            appLogger.log(uri, "folder.selected", "prepSuccess=${prep.success} message=${prep.message} runId=${latestResult.runId} action=${latestResult.action}")
        } else {
            statusText = "Shared folder selection cancelled. The app cannot read bridge reports until a folder is selected."
            appLogger.log(treeUri, "folder.cancelled", "noUriReturned=true")
        }
    }

    LaunchedEffect(Unit) {
        if (!folderPrompted && treeUri == null) {
            folderPrompted = true
            currentScreen = BridgeAppScreen.SETUP
            statusText = "Choose Documents or the AppLabBridge folder so the app can read backend reports and APKs."
            appLogger.log(treeUri, "folder.prompt", "initial=true")
            delay(500)
            folderPicker.launch(null)
        }
    }

    LaunchedEffect(treeUri) {
        latestResult = resultReader.readLatest(treeUri)
        registryState = registryReader.read(treeUri)
        repoChoices = repoChoiceReader.readChoices(treeUri)
        previousRunId = latestResult.runId
        pendingCommand = null
        appLogger.log(treeUri, "app.load", "runId=${latestResult.runId} action=${latestResult.action} status=${latestResult.status} title=${latestResult.title}")
        if (treeUri != null && !permissionPrompted && !termuxRunner.hasRunCommandPermission()) {
            permissionPrompted = true
            delay(500)
            requestTermuxPermission("after shared folder selection")
        }
    }

    LaunchedEffect(pollingToken) {
        val expected = pendingCommand ?: return@LaunchedEffect
        if (pollingToken == 0) return@LaunchedEffect
        repeat(15) { index ->
            delay(2_000)
            val result = resultReader.readLatest(treeUri)
            latestResult = result
            if (result.action == BridgeAction.LIST_PROJECTS.id) repoChoices = repoChoiceReader.readChoices(treeUri)
            appLogger.log(treeUri, "poll.tick", "tick=${index + 1} expected=${expected.expectedActionId} previous=${expected.previousRunId} sawRunId=${result.runId} sawAction=${result.action} sawStatus=${result.status} sawTitle=${result.title}")
            if (result.runId.isNotBlank() && result.runId != expected.previousRunId) {
                previousRunId = result.runId
                if (result.action == expected.expectedActionId) {
                    pendingCommand = expected.copy(phase = BridgeCommandPhase.RESULT_RECEIVED)
                    statusText = "Result received: ${result.title}"
                    appLogger.log(treeUri, "poll.result", "matched=true runId=${result.runId} action=${result.action} status=${result.status}")
                    if (result.action == BridgeAction.LIST_ACTIONS.id) registryState = registryReader.read(treeUri)
                    if (result.action == BridgeAction.LIST_PROJECTS.id) repoChoices = repoChoiceReader.readChoices(treeUri)
                    if (!autoScannedAfterSetup && result.action == BridgeAction.CHECK_SETUP.id && result.status.equals("success", true)) {
                        autoScannedAfterSetup = true
                        delay(500)
                        executeAction(BridgeAction.LIST_PROJECTS)
                    }
                } else {
                    pendingCommand = expected.copy(phase = BridgeCommandPhase.RESULT_MISMATCH)
                    statusText = "Different result received. Expected ${expected.expectedActionId}, saw ${result.action.ifBlank { "unknown" }}."
                    appLogger.log(treeUri, "poll.result", "matched=false expected=${expected.expectedActionId} saw=${result.action} runId=${result.runId}")
                }
                return@LaunchedEffect
            }
        }
        pendingCommand = expected.copy(phase = BridgeCommandPhase.TIMED_OUT)
        statusText = "Timed out waiting for ${expected.expectedActionId}. Termux may not have launched, the dispatcher may have failed early, or the selected shared folder may not match Termux storage."
        appLogger.log(treeUri, "poll.timeout", "expected=${expected.expectedActionId} previousRunId=${expected.previousRunId}")
    }

    val openReport = {
        appLogger.log(treeUri, "open.report", "report=${latestResult.reportFileName()}")
        statusText = if (fileOpener.openReport(treeUri, latestResult.reportFileName())) "Opened last action report." else "Report file not found."
    }
    val openLog = {
        appLogger.log(treeUri, "open.log", "newest=true")
        statusText = if (fileOpener.openNewestLog(treeUri)) "Opened latest log file." else "Log file not found."
    }
    val openDebugZip = {
        appLogger.log(treeUri, "open.debugZip", "newest=true")
        statusText = if (fileOpener.openDebugZip(treeUri)) "Opened latest debug zip." else "Debug zip not found."
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0F14)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BridgeAppHeader(currentScreen = currentScreen, latestResult = latestResult, onScreenSelected = { currentScreen = it })
                BridgeStatusPanel(statusText, treeUri, latestResult, pendingCommand)
                pendingAction?.let { action ->
                    BridgeConfirmActionCard(action = action, latestResult = latestResult, onConfirm = { executeAction(action) }, onCancel = {
                        pendingAction = null
                        statusText = "Action cancelled."
                        appLogger.log(treeUri, "action.cancel", "action=${action.id}")
                    })
                }
                when (currentScreen) {
                    BridgeAppScreen.HOME -> BridgeHomeScreen(treeUri, latestResult, registryState, termuxRunner.hasRunCommandPermission(), curationState, apkInstaller.latestApkName(treeUri), { folderPicker.launch(null) }, ::refreshResult, ::requestAction, openReport, openLog, { currentScreen = it }, { id -> updateCuration(curationStore.togglePin(id), "action.pin.toggle", id) }, { id -> updateCuration(curationStore.hide(id), "action.hidden", id) }, { id -> updateCuration(curationStore.unhide(id), "action.unhidden", id) })
                    BridgeAppScreen.REPO -> BridgeRepoWorkbenchScreen(latestResult, repoChoices, ::requestAction, ::chooseRepo) { currentScreen = it }
                    BridgeAppScreen.PATCH -> BridgePatchRunnerScreen(registryState, latestResult, termuxRunner.hasRunCommandPermission(), curationState, ::requestAction, { id -> updateCuration(curationStore.togglePin(id), "action.pin.toggle", id) }, { id -> updateCuration(curationStore.hide(id), "action.hidden", id) }, { id -> updateCuration(curationStore.unhide(id), "action.unhidden", id) })
                    BridgeAppScreen.APK -> BridgeApkScreen(registryState, latestResult, termuxRunner.hasRunCommandPermission(), curationState, apkInstaller.latestApkName(treeUri), ::requestAction, { id -> updateCuration(curationStore.togglePin(id), "action.pin.toggle", id) }, { id -> updateCuration(curationStore.hide(id), "action.hidden", id) }, { id -> updateCuration(curationStore.unhide(id), "action.unhidden", id) }, {
                        appLogger.log(treeUri, "apk.install", "latest=${apkInstaller.latestApkName(treeUri)}")
                        statusText = apkInstaller.installLatest(treeUri).message
                    }, { apkInstaller.openInstallSettings() })
                    BridgeAppScreen.RESULTS -> BridgeResultsScreen(latestResult, ::refreshResult, openReport, openLog, openDebugZip, ::requestAction)
                    BridgeAppScreen.ACTION_CATALOG -> BridgeActionCatalogScreen(registryState, latestResult, termuxRunner.hasRunCommandPermission(), apkInstaller.latestApkName(treeUri), curationState, ::reloadRegistry, ::requestAction, { enabled -> updateCuration(curationStore.setCustomizeMode(enabled), "action.customize") }, { id -> updateCuration(curationStore.togglePin(id), "action.pin.toggle", id) }, { id -> updateCuration(curationStore.hide(id), "action.hidden", id) }, { id -> updateCuration(curationStore.unhide(id), "action.unhidden", id) }, { updateCuration(curationStore.reset(), "action.layout.reset") })
                    BridgeAppScreen.ADVANCED -> BridgeAdvancedScreen(onRunAction = ::requestAction)
                    BridgeAppScreen.SETUP -> BridgeSetupScreen({ folderPicker.launch(null) }, ::refreshResult, ::requestAction, { openAppSettings(context) }, { requestTermuxPermission("manual setup button") }, {
                        appLogger.log(treeUri, "clipboard.save", "requested=true")
                        statusText = clipboardBridge.writeClipboardSave(treeUri).message
                    }, ::runBackendBootstrap)
                }
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }
    context.startActivity(intent)
}
