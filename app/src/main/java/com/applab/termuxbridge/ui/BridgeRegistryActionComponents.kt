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

enum class ActionAvailabilityFilter(val label: String) {
    ACTIVE("Active"),
    READY("Ready"),
    WARNINGS("Warnings"),
    BLOCKED("Blocked"),
    ALL("All")
}

fun actionVisibleForFilter(relevance: ActionRelevance, filter: ActionAvailabilityFilter): Boolean {
    return when (filter) {
        ActionAvailabilityFilter.ACTIVE -> relevance.availability == ActionAvailability.READY || relevance.availability == ActionAvailability.WARNING
        ActionAvailabilityFilter.READY -> relevance.availability == ActionAvailability.READY
        ActionAvailabilityFilter.WARNINGS -> relevance.availability == ActionAvailability.WARNING
        ActionAvailabilityFilter.BLOCKED -> relevance.availability == ActionAvailability.BLOCKED
        ActionAvailabilityFilter.ALL -> relevance.availability != ActionAvailability.HIDDEN
    }
}

@Composable
fun BridgeAvailabilityFilterPicker(
    selected: ActionAvailabilityFilter,
    onSelected: (ActionAvailabilityFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionAvailabilityFilter.entries.chunked(2).forEach { rowFilters ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowFilters.forEach { filter ->
                    val isSelected = filter == selected
                    Button(
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFF1B6BFF) else Color(0xFF344055)),
                        onClick = { onSelected(filter) }
                    ) {
                        Text(filter.label)
                    }
                }
                if (rowFilters.size == 1) {
                    Column(modifier = Modifier.weight(1f)) { }
                }
            }
        }
    }
}

@Composable
fun BridgeRegistryGroupOrFallback(
    registryState: BackendActionRegistryState,
    groupName: String,
    latestResult: BridgeResult,
    hasTermuxPermission: Boolean,
    latestApkName: String?,
    title: String = groupName,
    fallbackReason: String = "Backend action registry is missing, incomplete, or has no actions for this group. Showing fixed fallback controls.",
    onRunAction: (BridgeAction) -> Unit,
    fallbackContent: @Composable () -> Unit
) {
    var filter by remember(groupName) { mutableStateOf(ActionAvailabilityFilter.ACTIVE) }
    val actions = (registryState as? BackendActionRegistryState.Loaded)
        ?.registry
        ?.actions
        ?.filter { action -> action.group == groupName && action.visibleByDefault && !action.advanced && !action.parked }
        ?.sortedBy { it.sort }
        .orEmpty()

    if (actions.isEmpty()) {
        BridgeSectionCard("$title Fallback") {
            BridgeHintText(fallbackReason)
        }
        fallbackContent()
        return
    }

    val relevantActions = actions.map { descriptor ->
        descriptor to relevanceForAction(
            descriptor = descriptor,
            result = latestResult,
            hasTermuxPermission = hasTermuxPermission,
            latestApkName = latestApkName
        )
    }.filter { (_, relevance) -> relevance.availability != ActionAvailability.HIDDEN }

    val visibleActions = relevantActions.filter { (_, relevance) -> actionVisibleForFilter(relevance, filter) }
    val readyCount = relevantActions.count { (_, relevance) -> relevance.availability == ActionAvailability.READY }
    val warningCount = relevantActions.count { (_, relevance) -> relevance.availability == ActionAvailability.WARNING }
    val blockedCount = relevantActions.count { (_, relevance) -> relevance.availability == ActionAvailability.BLOCKED }

    BridgeSectionCard(title) {
        BridgeHintText("Registry-driven actions from config/actions.json. Default view shows ready and warning actions; blocked actions stay available for troubleshooting.")
        Text("ready $readyCount · warning $warningCount · blocked $blockedCount", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        BridgeAvailabilityFilterPicker(filter) { filter = it }
        if (visibleActions.isEmpty()) {
            BridgeHintText("No actions match the current filter.")
        } else {
            visibleActions.forEach { (descriptor, relevance) ->
                BridgeRegistryActionRow(descriptor, relevance, onRunAction)
            }
        }
    }
}

@Composable
fun BridgeRegistryActionRow(
    descriptor: BackendActionDescriptor,
    relevance: ActionRelevance,
    onRunAction: (BridgeAction) -> Unit
) {
    val builtIn = BridgeAction.fromId(descriptor.id)
    val rowColor = when (relevance.availability) {
        ActionAvailability.READY -> Color(0xFF080D12)
        ActionAvailability.WARNING -> Color(0xFF1F1707)
        ActionAvailability.BLOCKED -> Color(0xFF161A20)
        ActionAvailability.HIDDEN -> Color(0xFF080D12)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(descriptor.label, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${descriptor.id} · ${descriptor.risk} · ${actionAvailabilityLabel(relevance)}", color = registryRiskColor(descriptor, relevance), fontFamily = FontFamily.Monospace)
            }
            if (builtIn != null) {
                Button(
                    modifier = Modifier.heightIn(min = 40.dp),
                    enabled = relevance.canRun,
                    colors = ButtonDefaults.buttonColors(containerColor = bridgeButtonColor(relevance.tone)),
                    onClick = { onRunAction(builtIn) }
                ) { Text(if (relevance.canRun) "Run" else "Blocked") }
            } else {
                Text("unsupported", color = Color(0xFFFFD166), fontFamily = FontFamily.Monospace)
            }
        }
        Text(relevance.reason, color = availabilityColor(relevance), fontFamily = FontFamily.Monospace)
        if (descriptor.flags.isNotEmpty()) {
            Text(descriptor.flags.joinToString(" · "), color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        }
        if (descriptor.description.isNotBlank()) {
            Text(descriptor.description, color = Color(0xFF9AA4B2))
        }
    }
}

private fun registryRiskColor(action: BackendActionDescriptor, relevance: ActionRelevance): Color {
    if (relevance.availability == ActionAvailability.BLOCKED) return Color(0xFFFF6B6B)
    return when (action.risk) {
        "safe" -> Color(0xFF5CE38A)
        "network" -> Color(0xFF9AA4B2)
        "mutating", "publishing", "install" -> Color(0xFFFFD166)
        "experimental" -> Color(0xFFFF6B6B)
        else -> Color(0xFF9AA4B2)
    }
}

private fun availabilityColor(relevance: ActionRelevance): Color {
    return when (relevance.availability) {
        ActionAvailability.READY -> Color(0xFF5CE38A)
        ActionAvailability.WARNING -> Color(0xFFFFD166)
        ActionAvailability.BLOCKED -> Color(0xFFFF6B6B)
        ActionAvailability.HIDDEN -> Color(0xFF9AA4B2)
    }
}
