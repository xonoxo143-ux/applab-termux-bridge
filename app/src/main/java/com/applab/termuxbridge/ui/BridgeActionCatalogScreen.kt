package com.applab.termuxbridge.ui

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
import com.applab.termuxbridge.bridge.BridgeResult

private enum class CatalogProfile(val label: String) {
    QUICK("Quick"),
    PINNED("Pinned"),
    SETUP("Setup"),
    REPO("Repo"),
    PATCH("Patch"),
    APK("APK"),
    RESULTS("Results"),
    ADVANCED("Advanced"),
    HIDDEN("Hidden"),
    ALL("All")
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
    var selectedProfile by remember { mutableStateOf(CatalogProfile.QUICK) }
    var availabilityFilter by remember { mutableStateOf(ActionAvailabilityFilter.ACTIVE) }

    BridgeSectionCard("Action Catalog") {
        BridgeHintText("Backend-published actions are grouped here. Use Customize Actions to pin or hide actions.")
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
            BridgeCurationControlCard(curationState, onSetCustomizeMode, onResetLayout)
            BridgeCatalogProfilePicker(selectedProfile) { selectedProfile = it }
            BridgeSectionCard("Availability") {
                BridgeAvailabilityFilterPicker(availabilityFilter) { availabilityFilter = it }
            }

            val visibleNormalActions = registry.actions.filter { it.visibleByDefault && !it.advanced && !it.parked && !curationState.isHidden(it.id) }
            val hiddenActions = registry.actions.filter { curationState.isHidden(it.id) }
            val advancedActions = registry.actions.filter { !it.visibleByDefault || it.advanced || it.parked }
            val quickActions = quickActionIds.mapNotNull { id -> registry.actions.firstOrNull { it.id == id && !curationState.isHidden(it.id) } }
            val pinnedActions = curationState.pinnedIds.mapNotNull { id -> registry.actions.firstOrNull { it.id == id } }

            when (selectedProfile) {
                CatalogProfile.QUICK -> BridgeCompactActionGroup("Quick Actions", quickActions, true, latestResult, hasTermuxPermission, latestApkName, availabilityFilter, curationState, onRunBuiltInAction, onTogglePin, onHideAction, onUnhideAction)
                CatalogProfile.PINNED -> BridgeCompactActionGroup("Pinned Actions", pinnedActions, true, latestResult, hasTermuxPermission, latestApkName, availabilityFilter, curationState, onRunBuiltInAction, onTogglePin, onHideAction, onUnhideAction, hideAdvancedParked = false)
                CatalogProfile.SETUP -> BridgeCatalogGroups(listOf("Setup"), visibleNormalActions, latestResult, hasTermuxPermission, latestApkName, availabilityFilter, curationState, onRunBuiltInAction, onTogglePin, onHideAction, onUnhideAction)
                CatalogProfile.REPO -> BridgeCatalogGroups(listOf("Repo Workbench"), visibleNormalActions, latestResult, hasTermuxPermission, latestApkName, availabilityFilter, curationState, onRunBuiltInAction, onTogglePin, onHideAction, onUnhideAction)
                CatalogProfile.PATCH -> BridgeCatalogGroups(listOf("Patch Runner"), visibleNormalActions, latestResult, hasTermuxPermission, latestApkName, availabilityFilter, curationState, onRunBuiltInAction, onTogglePin, onHideAction, onUnhideAction)
                CatalogProfile.APK -> BridgeCatalogGroups(listOf("Build / APK"), visibleNormalActions, latestResult, hasTermuxPermission, latestApkName, availabilityFilter, curationState, onRunBuiltInAction, onTogglePin, onHideAction, onUnhideAction)
                CatalogProfile.RESULTS -> BridgeCatalogGroups(listOf("Results"), visibleNormalActions, latestResult, hasTermuxPermission, latestApkName, availabilityFilter, curationState, onRunBuiltInAction, onTogglePin, onHideAction, onUnhideAction)
                CatalogProfile.ADVANCED -> BridgeCompactActionGroup("Advanced / Parked Actions", advancedActions.sortedWith(compareBy<BackendActionDescriptor> { it.group }.thenBy { it.sort }), true, latestResult, hasTermuxPermission, latestApkName, ActionAvailabilityFilter.ALL, curationState, onRunBuiltInAction, onTogglePin, onHideAction, onUnhideAction, hideAdvancedParked = false)
                CatalogProfile.HIDDEN -> BridgeCompactActionGroup("Hidden Actions", hiddenActions.sortedWith(compareBy<BackendActionDescriptor> { it.group }.thenBy { it.sort }), true, latestResult, hasTermuxPermission, latestApkName, ActionAvailabilityFilter.ALL, curationState, onRunBuiltInAction, onTogglePin, onHideAction, onUnhideAction, hideAdvancedParked = false, showHiddenAction = true)
                CatalogProfile.ALL -> {
                    registry.groups.forEach { group ->
                        val groupActions = visibleNormalActions.filter { it.group == group }.sortedBy { it.sort }
                        if (groupActions.isNotEmpty()) {
                            BridgeCompactActionGroup(group, groupActions, group == "Setup", latestResult, hasTermuxPermission, latestApkName, availabilityFilter, curationState, onRunBuiltInAction, onTogglePin, onHideAction, onUnhideAction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BridgeCurationControlCard(
    curationState: ActionCurationState,
    onSetCustomizeMode: (Boolean) -> Unit,
    onResetLayout: () -> Unit
) {
    BridgeSectionCard("Customize Actions") {
        Text("Pinned ${curationState.pinnedIds.size} · Hidden ${curationState.hiddenIds.size}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        BridgePrimaryButton(if (curationState.customizeMode) "Done Customizing" else "Customize Actions") {
            onSetCustomizeMode(!curationState.customizeMode)
        }
        BridgeSecondaryButton("Reset Action Layout", onResetLayout)
        BridgeHintText("Customize mode adds Pin/Hide controls to action rows. Hidden actions are still available from the Hidden profile.")
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
                    ) { Text(profile.label) }
                }
                if (rowProfiles.size == 1) Column(modifier = Modifier.weight(1f)) { }
            }
        }
    }
}

@Composable
private fun BridgeCatalogGroups(
    groupNames: List<String>,
    actions: List<BackendActionDescriptor>,
    latestResult: BridgeResult,
    hasTermuxPermission: Boolean,
    latestApkName: String?,
    availabilityFilter: ActionAvailabilityFilter,
    curationState: ActionCurationState,
    onRunBuiltInAction: (BridgeAction) -> Unit,
    onTogglePin: (String) -> Unit,
    onHideAction: (String) -> Unit,
    onUnhideAction: (String) -> Unit
) {
    groupNames.forEach { group ->
        val groupActions = actions.filter { it.group == group }.sortedBy { it.sort }
        if (groupActions.isNotEmpty()) {
            BridgeCompactActionGroup(group, groupActions, true, latestResult, hasTermuxPermission, latestApkName, availabilityFilter, curationState, onRunBuiltInAction, onTogglePin, onHideAction, onUnhideAction)
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
    latestResult: BridgeResult,
    hasTermuxPermission: Boolean,
    latestApkName: String?,
    availabilityFilter: ActionAvailabilityFilter,
    curationState: ActionCurationState,
    onRunBuiltInAction: (BridgeAction) -> Unit,
    onTogglePin: (String) -> Unit,
    onHideAction: (String) -> Unit,
    onUnhideAction: (String) -> Unit,
    hideAdvancedParked: Boolean = true,
    showHiddenAction: Boolean = false
) {
    var expanded by remember(group) { mutableStateOf(expandedByDefault) }
    val actionStates = actions.map { descriptor ->
        descriptor to relevanceForAction(descriptor, latestResult, hasTermuxPermission, latestApkName)
    }.filter { (descriptor, relevance) ->
        (relevance.availability != ActionAvailability.HIDDEN || !hideAdvancedParked || descriptor.advanced || descriptor.parked) &&
            (showHiddenAction || !curationState.isHidden(descriptor.id))
    }
    val visibleActions = actionStates.filter { (_, relevance) -> actionVisibleForFilter(relevance, availabilityFilter) }
    val readyCount = actionStates.count { (_, relevance) -> relevance.availability == ActionAvailability.READY }
    val warningCount = actionStates.count { (_, relevance) -> relevance.availability == ActionAvailability.WARNING }
    val blockedCount = actionStates.count { (_, relevance) -> relevance.availability == ActionAvailability.BLOCKED }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121A24)), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("ready $readyCount · warning $warningCount · blocked $blockedCount", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
                }
                OutlinedButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide" else "Show", color = Color.White) }
            }
            if (expanded) {
                if (visibleActions.isEmpty()) BridgeHintText("No actions match the current filter.") else visibleActions.forEach { (action, relevance) ->
                    BridgeRegistryActionRow(action, relevance, curationState, showHiddenAction, onRunBuiltInAction, onTogglePin, onHideAction, onUnhideAction)
                }
            }
        }
    }
}

private val quickActionIds = listOf("check_setup", "list_actions", "list_projects", "show_active_repo", "show_status", "create_debug_zip")
