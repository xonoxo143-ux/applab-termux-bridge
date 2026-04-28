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

@Composable
fun BridgeRegistryGroupOrFallback(
    registryState: BackendActionRegistryState,
    groupName: String,
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

    BridgeSectionCard(title) {
        BridgeHintText("Registry-driven actions from config/actions.json. Fixed recovery screens remain available if the registry is unavailable.")
        actions.forEach { descriptor ->
            BridgeRegistryActionRow(descriptor, onRunAction)
        }
    }
}

@Composable
fun BridgeRegistryActionRow(
    descriptor: BackendActionDescriptor,
    onRunAction: (BridgeAction) -> Unit
) {
    val builtIn = BridgeAction.fromId(descriptor.id)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080D12), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(descriptor.label, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${descriptor.id} · ${descriptor.risk}", color = registryRiskColor(descriptor), fontFamily = FontFamily.Monospace)
            }
            if (builtIn != null) {
                Button(
                    modifier = Modifier.heightIn(min = 40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = bridgeButtonColor(if (descriptor.isRisky) BridgeActionTone.WARNING else BridgeActionTone.PRIMARY)),
                    onClick = { onRunAction(builtIn) }
                ) { Text("Run") }
            } else {
                Text("unsupported", color = Color(0xFFFFD166), fontFamily = FontFamily.Monospace)
            }
        }
        if (descriptor.flags.isNotEmpty()) {
            Text(descriptor.flags.joinToString(" · "), color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        }
        if (descriptor.description.isNotBlank()) {
            Text(descriptor.description, color = Color(0xFF9AA4B2))
        }
    }
}

private fun registryRiskColor(action: BackendActionDescriptor): Color {
    return when (action.risk) {
        "safe" -> Color(0xFF5CE38A)
        "network" -> Color(0xFF9AA4B2)
        "mutating", "publishing", "install" -> Color(0xFFFFD166)
        "experimental" -> Color(0xFFFF6B6B)
        else -> Color(0xFF9AA4B2)
    }
}
