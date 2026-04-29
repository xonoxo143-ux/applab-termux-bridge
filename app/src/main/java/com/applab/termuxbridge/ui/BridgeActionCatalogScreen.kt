package com.applab.termuxbridge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.applab.termuxbridge.actions.BackendActionDescriptor
import com.applab.termuxbridge.actions.BackendActionRegistryState
import com.applab.termuxbridge.bridge.BridgeAction
import com.applab.termuxbridge.bridge.BridgeResult

private enum class ActionRunnerView(val label: String, val detail: String) {
    QUICK("Quick", "Common actions for setup, repo, and results."),
    PINNED("Pinned", "Your pinned actions."),
    REPO("Repo", "Repo inspection and maintenance actions."),
    PATCH("Patch", "Patch, stage, commit, and push actions."),
    UPDATES("Updates", "App update checks and downloads."),
    RESULTS("Results", "Reports, logs, and debug bundle actions."),
    SETUP("Setup", "Bridge setup and backend repair actions."),
    HIDDEN("Hidden", "Actions hidden from normal views.")
}

@Composable
fun BridgeActionCatalogScreen(
    registryState: BackendActionRegistryState,
    latestResult: BridgeResult,
    hasTermuxPermission: Boolean,
    latestApkName: String?,
    curationState: ActionCurationState,
    onReloadRegistry: () -> Unit,
    onRunBuiltInAction: (BridgeAction) -> Unit,
    onSetCustomizeMode: (Boolean) -> Unit,
    onTogglePin: (String) -> Unit,
    onHideAction: (String) -> Unit,
    onUnhideAction: (String) -> Unit,
    onResetLayout: () -> Unit
) {
    var selectedView by remember { mutableStateOf(ActionRunnerView.QUICK) }

    when (registryState) {
        BackendActionRegistryState.MissingFolder -> BridgeActionsUnavailable(
            title = "Actions unavailable",
            detail = "Choose the shared bridge folder before loading backend actions.",
            onReloadRegistry = onReloadRegistry,
            onRunBuiltInAction = onRunBuiltInAction
        )
        BackendActionRegistryState.MissingRegistry -> BridgeActionsUnavailable(
            title = "Actions unavailable",
            detail = "No backend action list is loaded yet. Refresh backend actions after setup or backend repair.",
            onReloadRegistry = onReloadRegistry,
            onRunBuiltInAction = onRunBuiltInAction
        )
        is BackendActionRegistryState.ParseError -> BridgeActionsUnavailable(
            title = "Actions unavailable",
            detail = "Backend action list could not be read: ${registryState.message}",
            onReloadRegistry = onReloadRegistry,
            onRunBuiltInAction = onRunBuiltInAction
        )
        is BackendActionRegistryState.Loaded -> {
            val registry = registryState.registry
            val actionsForView = actionsForRunnerView(registry.actions, selectedView, curationState)
            val visibleRows = actionsForView.take(6)
            val hiddenCount = actionsForView.size - visibleRows.size

            BridgeActionViewCard(
                selectedView = selectedView,
                onPrevious = { selectedView = previousActionRunnerView(selectedView) },
                onNext = { selectedView = nextActionRunnerView(selectedView) }
            )

            BridgeSectionCard("Run Actions") {
                if (visibleRows.isEmpty()) {
                    BridgeHintText(emptyActionsText(selectedView))
                } else {
                    visibleRows.forEach { descriptor ->
                        val relevance = relevanceForAction(descriptor, latestResult, hasTermuxPermission, latestApkName)
                        BridgeActionRunnerRow(
                            descriptor = descriptor,
                            relevance = relevance,
                            curationState = curationState,
                            showManagement = curationState.customizeMode || selectedView == ActionRunnerView.HIDDEN,
                            onRunBuiltInAction = onRunBuiltInAction,
                            onTogglePin = onTogglePin,
                            onHideAction = onHideAction,
                            onUnhideAction = onUnhideAction
                        )
                    }
                    if (hiddenCount > 0) {
                        BridgeHintText("Showing first ${visibleRows.size}. Use a narrower view if this list gets crowded.")
                    }
                }
            }

            BridgeActionManageCard(
                registryActionCount = registry.actions.size,
                pinnedCount = curationState.pinnedIds.size,
                hiddenCount = curationState.hiddenIds.size,
                customizeMode = curationState.customizeMode,
                onReloadRegistry = onReloadRegistry,
                onRefreshBackendActions = { onRunBuiltInAction(BridgeAction.LIST_ACTIONS) },
                onSetCustomizeMode = onSetCustomizeMode,
                onResetLayout = onResetLayout
            )
        }
    }
}

@Composable
private fun BridgeActionViewCard(
    selectedView: ActionRunnerView,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    BridgeSectionCard("Action View") {
        Text(selectedView.label, color = Color.White, fontWeight = FontWeight.Bold)
        BridgeHintText(selectedView.detail)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = onPrevious
            ) { Text("Previous") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
                onClick = onNext
            ) { Text("Next") }
        }
        Text("Order: Quick → Pinned → Repo → Patch → Updates → Results → Setup → Hidden", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun BridgeActionRunnerRow(
    descriptor: BackendActionDescriptor,
    relevance: ActionRelevance,
    curationState: ActionCurationState,
    showManagement: Boolean,
    onRunBuiltInAction: (BridgeAction) -> Unit,
    onTogglePin: (String) -> Unit,
    onHideAction: (String) -> Unit,
    onUnhideAction: (String) -> Unit
) {
    val builtIn = BridgeAction.fromId(descriptor.id)
    val pinned = curationState.isPinned(descriptor.id)
    val hidden = curationState.isHidden(descriptor.id)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080D12), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(descriptor.label, color = Color.White, fontWeight = FontWeight.Bold)
                Text(actionAvailabilityLabel(relevance), color = actionRunnerStateColor(relevance), fontFamily = FontFamily.Monospace)
                if (pinned || hidden) {
                    Text(
                        listOfNotNull(if (pinned) "pinned" else null, if (hidden) "hidden" else null).joinToString(" · "),
                        color = Color(0xFFFFD166),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Button(
                modifier = Modifier.heightIn(min = 40.dp),
                enabled = relevance.canRun && builtIn != null && !hidden,
                colors = ButtonDefaults.buttonColors(containerColor = bridgeButtonColor(relevance.tone)),
                onClick = { builtIn?.let(onRunBuiltInAction) }
            ) { Text(if (relevance.canRun && !hidden) "Run" else "Blocked") }
        }
        if (relevance.availability == ActionAvailability.BLOCKED || relevance.availability == ActionAvailability.WARNING) {
            Text(relevance.reason, color = actionRunnerStateColor(relevance), fontFamily = FontFamily.Monospace)
        }
        if (showManagement) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f).heightIn(min = 38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (pinned) Color(0xFF344055) else Color(0xFF1B6BFF)),
                    onClick = { onTogglePin(descriptor.id) }
                ) { Text(if (pinned) "Unpin" else "Pin") }
                Button(
                    modifier = Modifier.weight(1f).heightIn(min = 38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (hidden) Color(0xFF1B6BFF) else Color(0xFF344055)),
                    onClick = { if (hidden) onUnhideAction(descriptor.id) else onHideAction(descriptor.id) }
                ) { Text(if (hidden) "Unhide" else "Hide") }
            }
        }
    }
}

@Composable
private fun BridgeActionManageCard(
    registryActionCount: Int,
    pinnedCount: Int,
    hiddenCount: Int,
    customizeMode: Boolean,
    onReloadRegistry: () -> Unit,
    onRefreshBackendActions: () -> Unit,
    onSetCustomizeMode: (Boolean) -> Unit,
    onResetLayout: () -> Unit
) {
    BridgeSectionCard("Manage") {
        Text("Actions $registryActionCount · Pinned $pinnedCount · Hidden $hiddenCount", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (customizeMode) Color(0xFF344055) else Color(0xFF1B6BFF)),
                onClick = { onSetCustomizeMode(!customizeMode) }
            ) { Text(if (customizeMode) "Done" else "Customize") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = onReloadRegistry
            ) { Text("Reload") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = onRefreshBackendActions
            ) { Text("Refresh") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = onResetLayout
            ) { Text("Reset") }
        }
        BridgeHintText("Customize shows Pin/Hide controls. Refresh asks Termux to rewrite the backend action list; Reload only rereads the saved list.")
    }
}

@Composable
private fun BridgeActionsUnavailable(
    title: String,
    detail: String,
    onReloadRegistry: () -> Unit,
    onRunBuiltInAction: (BridgeAction) -> Unit
) {
    BridgeSectionCard(title) {
        BridgeHintText(detail)
        BridgePrimaryButton("Refresh Backend Actions") { onRunBuiltInAction(BridgeAction.LIST_ACTIONS) }
        BridgeSecondaryButton("Reload Saved Actions", onReloadRegistry)
    }
}

private fun actionsForRunnerView(
    actions: List<BackendActionDescriptor>,
    selectedView: ActionRunnerView,
    curationState: ActionCurationState
): List<BackendActionDescriptor> {
    val normal = actions.filter { it.visibleByDefault && !it.advanced && !it.parked && !curationState.isHidden(it.id) }
    return when (selectedView) {
        ActionRunnerView.QUICK -> quickActionIds.mapNotNull { id -> actions.firstOrNull { it.id == id && !curationState.isHidden(it.id) } }
        ActionRunnerView.PINNED -> curationState.pinnedIds.mapNotNull { id -> actions.firstOrNull { it.id == id && !curationState.isHidden(it.id) } }
        ActionRunnerView.REPO -> normal.filter { it.group == "Repo Workbench" }
        ActionRunnerView.PATCH -> normal.filter { it.group == "Patch Runner" }
        ActionRunnerView.UPDATES -> normal.filter { it.group == "Build / APK" }
        ActionRunnerView.RESULTS -> normal.filter { it.group == "Results" }
        ActionRunnerView.SETUP -> normal.filter { it.group == "Setup" }
        ActionRunnerView.HIDDEN -> actions.filter { curationState.isHidden(it.id) }.sortedWith(compareBy<BackendActionDescriptor> { it.group }.thenBy { it.sort })
    }.sortedBy { it.sort }
}

private fun emptyActionsText(view: ActionRunnerView): String {
    return when (view) {
        ActionRunnerView.PINNED -> "No pinned actions yet. Use Customize to pin actions you want on Home and Actions."
        ActionRunnerView.HIDDEN -> "No hidden actions."
        else -> "No actions available for this view. Refresh backend actions or check setup."
    }
}

private fun previousActionRunnerView(current: ActionRunnerView): ActionRunnerView {
    val entries = ActionRunnerView.entries
    val index = entries.indexOf(current)
    return entries[(index - 1 + entries.size) % entries.size]
}

private fun nextActionRunnerView(current: ActionRunnerView): ActionRunnerView {
    val entries = ActionRunnerView.entries
    val index = entries.indexOf(current)
    return entries[(index + 1) % entries.size]
}

private fun actionRunnerStateColor(relevance: ActionRelevance): Color {
    return when (relevance.availability) {
        ActionAvailability.READY -> Color(0xFF5CE38A)
        ActionAvailability.WARNING -> Color(0xFFFFD166)
        ActionAvailability.BLOCKED -> Color(0xFFFF6B6B)
        ActionAvailability.HIDDEN -> Color(0xFF9AA4B2)
    }
}

private val quickActionIds = listOf("check_setup", "list_projects", "check_repo", "list_changed_files", "create_debug_zip")
