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

    BridgeSectionCard(title) {
        BridgeHintText("Registry-driven actions from config/actions.json. Rows now show ready, warning, or blocked state.")
        relevantActions.forEach { (descriptor, relevance) ->
            BridgeRegistryActionRow(descriptor, relevance, onRunAction)
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
