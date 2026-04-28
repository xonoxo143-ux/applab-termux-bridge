package com.applab.termuxbridge.ui

import android.net.Uri
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.applab.termuxbridge.actions.BackendActionRegistryState
import com.applab.termuxbridge.bridge.BridgeAction
import com.applab.termuxbridge.bridge.BridgeResult

@Composable
fun BridgeDashboardReadinessStrip(
    treeUri: Uri?,
    hasTermuxPermission: Boolean,
    registryState: BackendActionRegistryState,
    latestResult: BridgeResult,
    onPickFolder: () -> Unit,
    onRunAction: (BridgeAction) -> Unit,
    onGoTo: (BridgeAppScreen) -> Unit
) {
    val backendReady = registryState is BackendActionRegistryState.Loaded ||
        (latestResult.action == BridgeAction.CHECK_SETUP.id && latestResult.status.equals("success", true))
    BridgeSectionCard("Readiness") {
        BridgeDashboardStatusChipRow(
            items = listOf(
                DashboardChip("Folder", if (treeUri == null) "Missing" else "Ready", treeUri != null),
                DashboardChip("Termux", if (hasTermuxPermission) "Ready" else "Needs permission", hasTermuxPermission),
                DashboardChip("Backend", if (backendReady) "Ready" else "Unknown", backendReady)
            )
        )
        when {
            treeUri == null -> BridgePrimaryButton("Choose Folder", onPickFolder)
            !hasTermuxPermission -> BridgePrimaryButton("Open Setup", { onGoTo(BridgeAppScreen.SETUP) })
            !backendReady -> BridgeActionButton(BridgeAction.CHECK_SETUP, onRunAction)
        }
    }
}

@Composable
fun BridgeDashboardRepoSummary(
    latestResult: BridgeResult,
    onRunAction: (BridgeAction) -> Unit,
    onGoTo: (BridgeAppScreen) -> Unit
) {
    val hasRepo = latestResult.repoName != null || latestResult.branch != null
    BridgeSectionCard("Repo") {
        BridgeStatusLine("Repo", latestResult.repoName ?: "unknown", latestResult.repoName != null)
        BridgeStatusLine("Branch", latestResult.branch ?: "unknown", latestResult.branch != null)
        BridgeStatusLine("State", latestResult.stateLabel, latestResult.dirty == false)
        BridgeStatusLine("Patch", latestResult.patchLabel, latestResult.hasPatchFile == true)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = { onGoTo(BridgeAppScreen.REPO) }
            ) { Text(if (hasRepo) "Open Repo" else "Select Repo") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
                onClick = { onRunAction(if (hasRepo) BridgeAction.SHOW_STATUS else BridgeAction.LIST_PROJECTS) }
            ) { Text(if (hasRepo) "Status" else "Scan") }
        }
        if (!hasRepo) {
            BridgeHintText("No active repo is selected yet. Scan projects or open Repo to choose one before checking status.")
        }
    }
}

@Composable
fun BridgeDashboardNextStep(
    treeUri: Uri?,
    latestResult: BridgeResult,
    onPickFolder: () -> Unit,
    onRunAction: (BridgeAction) -> Unit,
    onOpenReport: () -> Unit,
    onGoTo: (BridgeAppScreen) -> Unit
) {
    val action = bridgeRecommendedAction(treeUri, latestResult)
    BridgeSectionCard("Next") {
        Text(action.title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(action.detail, color = Color(0xFF9AA4B2), style = MaterialTheme.typography.bodySmall)
        when {
            action.pickFolder -> BridgePrimaryButton(action.buttonLabel, onPickFolder)
            action.openReport -> BridgePrimaryButton(action.buttonLabel, onOpenReport)
            action.screen != null -> BridgePrimaryButton(action.buttonLabel) { onGoTo(action.screen) }
            action.bridgeAction != null -> BridgeActionButton(action.bridgeAction, onRunAction, tone = action.tone)
        }
    }
}

@Composable
fun BridgeDashboardPinnedActions(
    registryState: BackendActionRegistryState,
    latestResult: BridgeResult,
    hasTermuxPermission: Boolean,
    curationState: ActionCurationState,
    latestApkName: String?,
    onRunAction: (BridgeAction) -> Unit,
    onGoTo: (BridgeAppScreen) -> Unit
) {
    val registry = (registryState as? BackendActionRegistryState.Loaded)?.registry
    val pinnedActions = curationState.pinnedIds.mapNotNull { id ->
        registry?.actions?.firstOrNull { it.id == id && !curationState.isHidden(it.id) }
    }.take(5)
    BridgeSectionCard("Pinned") {
        when {
            registry == null -> {
                BridgeHintText("No backend action registry loaded yet.")
                BridgeSecondaryButton("Open Actions") { onGoTo(BridgeAppScreen.ACTION_CATALOG) }
            }
            curationState.pinnedIds.isEmpty() -> {
                BridgeHintText("No pinned actions yet.")
                BridgeSecondaryButton("Manage Actions") { onGoTo(BridgeAppScreen.ACTION_CATALOG) }
            }
            pinnedActions.isEmpty() -> {
                BridgeHintText("Pinned actions are hidden or missing from the latest registry.")
                BridgeSecondaryButton("Manage Actions") { onGoTo(BridgeAppScreen.ACTION_CATALOG) }
            }
            else -> {
                pinnedActions.forEach { action ->
                    val relevance = relevanceForAction(action, latestResult, hasTermuxPermission, latestApkName)
                    BridgeCompactDashboardActionRow(action.label, action.id, relevance) {
                        BridgeAction.fromId(action.id)?.let(onRunAction)
                    }
                }
                if (curationState.pinnedIds.size > pinnedActions.size) {
                    BridgeHintText("Showing first ${pinnedActions.size} pinned actions.")
                }
                BridgeSecondaryButton("Manage Actions") { onGoTo(BridgeAppScreen.ACTION_CATALOG) }
            }
        }
    }
}

@Composable
fun BridgeDashboardLastResult(
    latestResult: BridgeResult,
    onOpenReport: () -> Unit,
    onOpenLog: () -> Unit,
    onGoTo: (BridgeAppScreen) -> Unit
) {
    BridgeSectionCard("Last Result") {
        Text(
            "${latestResult.status.uppercase()} · ${latestResult.action.ifBlank { "none" }}",
            color = bridgeStatusColor(latestResult.status),
            fontFamily = FontFamily.Monospace
        )
        Text(latestResult.title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(latestResult.summary, color = Color(0xFF9AA4B2), style = MaterialTheme.typography.bodySmall)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = onOpenReport
            ) { Text("Report") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF344055)),
                onClick = onOpenLog
            ) { Text("Log") }
            Button(
                modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
                onClick = { onGoTo(BridgeAppScreen.RESULTS) }
            ) { Text("Results") }
        }
    }
}

@Composable
private fun BridgeDashboardStatusChipRow(items: List<DashboardChip>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF080D12), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.label, color = Color(0xFF9AA4B2), style = MaterialTheme.typography.bodySmall)
                Text(item.value, color = if (item.ok) Color(0xFF5CE38A) else Color(0xFFFFD166), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BridgeCompactDashboardActionRow(
    label: String,
    id: String,
    relevance: ActionRelevance,
    onRun: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080D12), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold)
            Text("$id · ${actionAvailabilityLabel(relevance)}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            modifier = Modifier.heightIn(min = 38.dp),
            enabled = relevance.canRun,
            colors = ButtonDefaults.buttonColors(containerColor = bridgeButtonColor(relevance.tone)),
            onClick = onRun
        ) { Text(if (relevance.canRun) "Run" else "Blocked") }
    }
}

private data class DashboardChip(val label: String, val value: String, val ok: Boolean)
