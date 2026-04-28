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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
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

private enum class CatalogProfile(val label: String) {
    QUICK("Quick"),
    SETUP("Setup"),
    REPO("Repo"),
    PATCH("Patch"),
    APK("APK"),
    RESULTS("Results"),
    ADVANCED("Advanced"),
    ALL("All")
}

@Composable
fun BridgeActionCatalogScreen(
    registryState: BackendActionRegistryState,
    onReloadRegistry: () -> Unit,
    onRunBuiltInAction: (BridgeAction) -> Unit
) {
    var selectedProfile by remember { mutableStateOf(CatalogProfile.QUICK) }

    BridgeSectionCard("Action Catalog") {
        BridgeHintText("Backend-published actions are grouped and collapsed here. Fixed recovery screens remain available if the registry is missing.")
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
            BridgeCatalogStatusCard(
                schema = registry.schemaVersion,
                commit = registry.backendCommit,
                generatedAt = registry.generatedAt,
                actionCount = registry.actions.size
            )
            BridgeCatalogProfilePicker(selectedProfile) { selectedProfile = it }

            val normalActions = registry.actions.filter { it.visibleByDefault && !it.advanced && !it.parked }
            val advancedActions = registry.actions.filter { !it.visibleByDefault || it.advanced || it.parked }
            val quickActions = quickActionIds.mapNotNull { id -> registry.actions.firstOrNull { it.id == id } }

            when (selectedProfile) {
                CatalogProfile.QUICK -> BridgeCompactActionGroup("Quick Actions", quickActions, expandedByDefault = true, onRunBuiltInAction)
                CatalogProfile.SETUP -> BridgeCatalogGroups(listOf("Setup"), normalActions, onRunBuiltInAction)
                CatalogProfile.REPO -> BridgeCatalogGroups(listOf("Repo Workbench"), normalActions, onRunBuiltInAction)
                CatalogProfile.PATCH -> BridgeCatalogGroups(listOf("Patch Runner"), normalActions, onRunBuiltInAction)
                CatalogProfile.APK -> BridgeCatalogGroups(listOf("Build / APK"), normalActions, onRunBuiltInAction)
                CatalogProfile.RESULTS -> BridgeCatalogGroups(listOf("Results"), normalActions, onRunBuiltInAction)
                CatalogProfile.ADVANCED -> BridgeCompactActionGroup("Advanced / Parked Actions", advancedActions.sortedWith(compareBy<BackendActionDescriptor> { it.group }.thenBy { it.sort }), expandedByDefault = true, onRunBuiltInAction)
                CatalogProfile.ALL -> {
                    registry.groups.forEach { group ->
                        val groupActions = normalActions.filter { it.group == group }.sortedBy { it.sort }
                        if (groupActions.isNotEmpty()) {
                            BridgeCompactActionGroup(group, groupActions, expandedByDefault = group == "Setup", onRunBuiltInAction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BridgeCatalogStatusCard(schema: Int, commit: String, generatedAt: String, actionCount: Int) {
    BridgeSectionCard("Registry Status") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BridgeMiniStatus("Schema", schema.toString(), schema == 1)
            BridgeMiniStatus("Actions", actionCount.toString(), actionCount > 0)
            BridgeMiniStatus("Commit", commit.ifBlank { "unknown" }, commit.isNotBlank())
        }
        Text("Generated: ${generatedAt.ifBlank { "unknown" }}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun BridgeMiniStatus(label: String, value: String, ok: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF9AA4B2))
        Text(value, color = if (ok) Color(0xFF5CE38A) else Color(0xFFFFD166), fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun BridgeCatalogProfilePicker(selected: CatalogProfile, onSelected: (CatalogProfile) -> Unit) {
    BridgeSectionCard("View") {
        CatalogProfile.entries.chunked(2).forEach { rowProfiles ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowProfiles.forEach { profile ->
                    val isSelected = profile == selected
                    Button(
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFF1B6BFF) else Color(0xFF344055)),
                        onClick = { onSelected(profile) }
                    ) {
                        Text(profile.label)
                    }
                }
                if (rowProfiles.size == 1) {
                    Column(modifier = Modifier.weight(1f)) { }
                }
            }
        }
    }
}

@Composable
private fun BridgeCatalogGroups(
    groupNames: List<String>,
    actions: List<BackendActionDescriptor>,
    onRunBuiltInAction: (BridgeAction) -> Unit
) {
    groupNames.forEach { group ->
        val groupActions = actions.filter { it.group == group }.sortedBy { it.sort }
        if (groupActions.isNotEmpty()) {
            BridgeCompactActionGroup(group, groupActions, expandedByDefault = true, onRunBuiltInAction)
        } else {
            BridgeSectionCard(group) { BridgeHintText("No actions found for this group in the current registry.") }
        }
    }
}

@Composable
private fun BridgeCompactActionGroup(
    group: String,
    actions: List<BackendActionDescriptor>,
    expandedByDefault: Boolean,
    onRunBuiltInAction: (BridgeAction) -> Unit
) {
    var expanded by remember(group) { mutableStateOf(expandedByDefault) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121A24)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${actions.size} action(s)", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
                }
                OutlinedButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show", color = Color.White)
                }
            }
            if (expanded) {
                actions.forEach { action -> BridgeCompactActionRow(action, onRunBuiltInAction) }
            }
        }
    }
}

@Composable
private fun BridgeCompactActionRow(
    action: BackendActionDescriptor,
    onRunBuiltInAction: (BridgeAction) -> Unit
) {
    val builtIn = BridgeAction.fromId(action.id)
    var detailsExpanded by remember(action.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080D12), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(action.label, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${action.id} · ${action.risk}", color = BridgeRiskColor(action), fontFamily = FontFamily.Monospace)
            }
            if (builtIn != null) {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = bridgeButtonColor(if (action.isRisky) BridgeActionTone.WARNING else BridgeActionTone.PRIMARY)),
                    onClick = { onRunBuiltInAction(builtIn) }
                ) { Text("Run") }
            } else {
                Text("unsupported", color = Color(0xFFFFD166), fontFamily = FontFamily.Monospace)
            }
        }
        if (action.flags.isNotEmpty()) {
            Text(action.flags.joinToString(" · "), color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        }
        OutlinedButton(modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp), onClick = { detailsExpanded = !detailsExpanded }) {
            Text(if (detailsExpanded) "Hide details" else "Details", color = Color.White)
        }
        if (detailsExpanded) {
            Text(action.description.ifBlank { "No description." }, color = Color(0xFFC7D0DA))
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

private val quickActionIds = listOf(
    "check_setup",
    "list_actions",
    "list_projects",
    "show_active_repo",
    "show_status",
    "create_debug_zip"
)
