package com.applab.termuxbridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
fun BridgeActionCatalogScreen(
    registryState: BackendActionRegistryState,
    onReloadRegistry: () -> Unit,
    onRunBuiltInAction: (BridgeAction) -> Unit
) {
    BridgeSectionCard("Action Catalog") {
        BridgeHintText("This screen reads backend-published actions from config/actions.json. Existing fixed screens remain unchanged for now.")
        BridgeSecondaryButton("Reload Action Registry", onReloadRegistry)
        BridgeActionButton(BridgeAction.LIST_ACTIONS, onRunBuiltInAction)
    }

    when (registryState) {
        BackendActionRegistryState.MissingFolder -> BridgeSectionCard("Registry Missing") {
            BridgeHintText("Choose the shared bridge folder before loading backend actions.")
        }
        BackendActionRegistryState.MissingRegistry -> BridgeSectionCard("Registry Missing") {
            BridgeHintText("No config/actions.json found yet. Run List Backend Actions after bootstrapping the backend.")
        }
        is BackendActionRegistryState.ParseError -> BridgeSectionCard("Registry Parse Error") {
            BridgeHintText(registryState.message)
        }
        is BackendActionRegistryState.Loaded -> {
            val registry = registryState.registry
            BridgeSectionCard("Registry Status") {
                BridgeStatusLine("Schema", registry.schemaVersion.toString(), registry.schemaVersion == 1)
                BridgeStatusLine("Backend", registry.backendCommit.ifBlank { "unknown" }, registry.backendCommit.isNotBlank())
                BridgeStatusLine("Actions", registry.actions.size.toString(), registry.actions.isNotEmpty())
                Text("Generated: ${registry.generatedAt.ifBlank { "unknown" }}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
            }

            val normalActions = registry.actions.filter { it.visibleByDefault && !it.advanced && !it.parked }
            val advancedActions = registry.actions.filter { !it.visibleByDefault || it.advanced || it.parked }

            registry.groups.forEach { group ->
                val groupActions = normalActions.filter { it.group == group }.sortedBy { it.sort }
                if (groupActions.isNotEmpty()) {
                    BridgeDynamicActionGroup(group, groupActions, onRunBuiltInAction)
                }
            }

            if (advancedActions.isNotEmpty()) {
                BridgeSectionCard("Advanced / Parked Actions") {
                    BridgeHintText("Shown for visibility only in Phase 2. Existing fixed Advanced Tools remains the safer place for these until curation is added.")
                    advancedActions.sortedWith(compareBy<BackendActionDescriptor> { it.group }.thenBy { it.sort }).forEach { action ->
                        BridgeDynamicActionRow(action, onRunBuiltInAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun BridgeDynamicActionGroup(
    group: String,
    actions: List<BackendActionDescriptor>,
    onRunBuiltInAction: (BridgeAction) -> Unit
) {
    BridgeSectionCard(group) {
        actions.forEach { action -> BridgeDynamicActionRow(action, onRunBuiltInAction) }
    }
}

@Composable
private fun BridgeDynamicActionRow(
    action: BackendActionDescriptor,
    onRunBuiltInAction: (BridgeAction) -> Unit
) {
    val builtIn = BridgeAction.fromId(action.id)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(action.label, color = Color.White, fontWeight = FontWeight.Bold)
                Text(action.description.ifBlank { action.id }, color = Color(0xFF9AA4B2))
                Text("${action.id} · ${action.risk}${action.flags.takeIf { it.isNotEmpty() }?.joinToString(prefix = " · ") ?: ""}", color = BridgeRiskColor(action), fontFamily = FontFamily.Monospace)
            }
        }
        if (builtIn != null) {
            BridgeActionButton(builtIn, onRunBuiltInAction, tone = if (action.isRisky) BridgeActionTone.WARNING else BridgeActionTone.PRIMARY)
        } else {
            BridgeSecondaryButton("Unsupported by this APK: ${action.id}") { }
        }
    }
}

private fun BridgeRiskColor(action: BackendActionDescriptor): Color {
    return when (action.risk) {
        "safe" -> Color(0xFF5CE38A)
        "network" -> Color(0xFF9AA4B2)
        "mutating", "publishing", "install" -> Color(0xFFFFD166)
        "experimental" -> Color(0xFFFF6B6B)
        else -> Color(0xFF9AA4B2)
    }
}
