package com.applab.termuxbridge.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import com.applab.termuxbridge.bridge.BridgeAction
import com.applab.termuxbridge.bridge.BridgeResult

@Composable
fun BridgeAppHeader(
    currentScreen: BridgeAppScreen,
    latestResult: BridgeResult,
    onScreenSelected: (BridgeAppScreen) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "AppLab Bridge",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentScreen.title,
                color = Color(0xFF9AA4B2),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = bridgeRepoHeaderText(latestResult),
                color = bridgeRepoStateColor(latestResult),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = bridgeStatusChipText(latestResult),
                color = bridgeStatusColor(latestResult.status),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
        Box {
            OutlinedButton(onClick = { menuOpen = true }) {
                Text("☰ Menu", color = Color.White)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                BridgeAppScreen.entries.forEach { screen ->
                    DropdownMenuItem(
                        text = { Text(screen.title) },
                        onClick = {
                            menuOpen = false
                            onScreenSelected(screen)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BridgeStatusPanel(status: String, treeUri: Uri?, result: BridgeResult) {
    val folderText = if (treeUri == null) "Folder: not selected" else "Folder: Documents/AppLabBridge selected"
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101821)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(status, color = Color.White, fontWeight = FontWeight.Bold)
            Text(folderText, color = Color(0xFF9AA4B2), style = MaterialTheme.typography.bodySmall)
            Text(result.repoLabel, color = bridgeRepoStateColor(result), style = MaterialTheme.typography.bodyMedium)
            Text(result.stateLabel, color = bridgeRepoStateColor(result), style = MaterialTheme.typography.bodySmall)
            Text(result.changeBreakdownLabel, color = Color(0xFF9AA4B2), style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Last: ${result.status.uppercase()} — ${result.title}",
                color = bridgeStatusColor(result.status),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun BridgeConfirmActionCard(
    action: BridgeAction,
    latestResult: BridgeResult,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1A08)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Confirm Action", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(action.label, color = Color(0xFFFFD166), fontWeight = FontWeight.Bold)
            BridgeHintText(bridgeActionRiskText(action))
            BridgeStatusLine("Repo", latestResult.repoName ?: "unknown", latestResult.repoName != null)
            BridgeStatusLine("Branch", latestResult.branch ?: "unknown", latestResult.branch != null)
            BridgeStatusLine("State", latestResult.stateLabel, latestResult.dirty == false)
            BridgeStatusLine("Changes", latestResult.changeBreakdownLabel, latestResult.dirty == false)
            BridgeStatusLine("Patch file", latestResult.patchLabel, latestResult.hasPatchFile == true)
            Button(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB86814)),
                onClick = onConfirm
            ) {
                Text("Confirm ${action.label}")
            }
            BridgeSecondaryButton("Cancel", onCancel)
        }
    }
}

@Composable
fun BridgeSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121A24)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun BridgeStatusLine(label: String, value: String, ok: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF9AA4B2), style = MaterialTheme.typography.bodySmall)
        Text(value, color = if (ok) Color(0xFF5CE38A) else Color(0xFFFFD166), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun BridgeActionButton(
    action: BridgeAction,
    onRunAction: (BridgeAction) -> Unit,
    modifier: Modifier = Modifier,
    tone: BridgeActionTone = BridgeActionTone.PRIMARY
) {
    Button(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bridgeButtonColor(tone)),
        onClick = { onRunAction(action) }
    ) {
        Text(action.label)
    }
}

@Composable
fun BridgePrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
        onClick = onClick
    ) {
        Text(label)
    }
}

@Composable
fun BridgeSecondaryButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        onClick = onClick
    ) {
        Text(label, color = Color.White)
    }
}

@Composable
fun BridgeHintText(text: String) {
    Text(text = text, color = Color(0xFF9AA4B2), style = MaterialTheme.typography.bodySmall)
}

@Composable
fun BridgeResultBlock(result: BridgeResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080D12), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(result.title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(result.summary, color = Color(0xFFC7D0DA))
        Text("Repo: ${result.repoLabel}", color = bridgeRepoStateColor(result), fontFamily = FontFamily.Monospace)
        Text("State: ${result.stateLabel}", color = bridgeRepoStateColor(result), fontFamily = FontFamily.Monospace)
        Text("Changes: ${result.changeBreakdownLabel}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        Text("Patch: ${result.patchLabel}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        result.currentCommit?.let { commit ->
            Text("Commit: $commit", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        }
        result.currentCommitMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text("Message: $message", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        }
        Text("Action: ${result.action.ifBlank { "none" }}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        Text("Run: ${result.runId.ifBlank { "none" }}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        Text("Exit: ${result.exitCode?.toString() ?: "n/a"}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        result.nextAction?.let { Text("Next: $it", color = Color(0xFFFFD166)) }
        if (result.artifacts.isNotEmpty()) {
            Text("Artifacts: ${result.artifacts.joinToString()}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun BridgeActionGroup(
    actions: List<BridgeAction>,
    onRunAction: (BridgeAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { action ->
            BridgeActionButton(action, onRunAction)
        }
    }
}
