package com.applab.termuxbridge

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val PREFS = "applab_bridge_prefs"
private const val KEY_TREE_URI = "tree_uri"
private const val TERMUX_PACKAGE = "com.termux"
private const val TERMUX_RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
private const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
private const val BRIDGE_PATH = "/data/data/com.termux/files/home/.termux/applab/bridge.sh"

private val approvedActions = listOf(
    BridgeAction("check_setup", "Check Termux Setup"),
    BridgeAction("check_repo", "Check Repo"),
    BridgeAction("pull_staging", "Pull Staging"),
    BridgeAction("show_current_commit", "Show Current Commit"),
    BridgeAction("list_changed_files", "List Changed Files"),
    BridgeAction("check_latest_apk", "Check Latest APK"),
    BridgeAction("download_latest_apk", "Download Latest APK"),
    BridgeAction("decode_save", "Decode Clipboard Save"),
    BridgeAction("validate_save", "Validate Save"),
    BridgeAction("find_server_calls", "Find Server Calls"),
    BridgeAction("find_save_paths", "Find Save Code Paths"),
    BridgeAction("find_root_manager", "Find Root Manager Code"),
    BridgeAction("find_hacking_labels", "Find Hacking Labels"),
    BridgeAction("find_todos", "Find TODO/FIXME"),
    BridgeAction("find_android_permissions", "Find Android Permissions"),
    BridgeAction("create_debug_zip", "Create Debug Zip"),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppLabBridgeApp()
        }
    }
}

data class BridgeAction(val id: String, val label: String)

data class BridgeResult(
    val schemaVersion: Int = 0,
    val runId: String = "",
    val action: String = "",
    val status: String = "unknown",
    val title: String = "No result loaded",
    val summary: String = "Pick the bridge folder, run an action, then refresh the result.",
    val exitCode: Int? = null,
    val reportFile: String? = null,
    val logFile: String? = null,
    val nextAction: String? = null,
)

@Composable
fun AppLabBridgeApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var treeUriString by remember { mutableStateOf(prefs.getString(KEY_TREE_URI, null)) }
    var lastResult by remember { mutableStateOf(BridgeResult()) }
    var lastStatus by remember { mutableStateOf("Ready") }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply()
            treeUriString = uri.toString()
            lastStatus = "Bridge folder selected."
            lastResult = readLatestResult(context, uri) ?: lastResult
        }
    }

    LaunchedEffect(treeUriString) {
        treeUriString?.let { raw ->
            lastResult = readLatestResult(context, Uri.parse(raw)) ?: lastResult
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0F14)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "AppLab Termux Bridge",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Button cockpit for approved Termux-backed workflows.",
                    color = Color(0xFF9AA4B2),
                    style = MaterialTheme.typography.bodyMedium
                )

                StatusPanel(lastStatus, treeUriString, lastResult)

                SectionCard("Bridge Folder") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton("Pick Folder") { folderPicker.launch(null) }
                        SecondaryButton("Refresh Result") {
                            val uri = treeUriString?.let(Uri::parse)
                            lastResult = uri?.let { readLatestResult(context, it) } ?: BridgeResult(summary = "No bridge folder selected.")
                            lastStatus = "Result refreshed."
                        }
                    }
                    Text(
                        "Recommended: Documents/AppLabBridge",
                        color = Color(0xFF9AA4B2),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                SectionCard("Setup") {
                    ActionRow(context, "check_setup", treeUriString) { lastStatus = it }
                    SecondaryButton("Open Termux Permission Settings") { openAppSettings(context) }
                }

                SectionCard("GitHub / Repo") {
                    ActionGrid(context, listOf("check_repo", "pull_staging", "show_current_commit", "list_changed_files"), treeUriString) { lastStatus = it }
                }

                SectionCard("APK") {
                    ActionGrid(context, listOf("check_latest_apk", "download_latest_apk"), treeUriString) { lastStatus = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("Install Latest APK") {
                            val uri = treeUriString?.let(Uri::parse)
                            if (uri == null) {
                                lastStatus = "Pick bridge folder first."
                            } else {
                                val installed = installLatestApk(context, uri)
                                lastStatus = if (installed) "Installer opened." else "No APK found or installer failed."
                            }
                        }
                        SecondaryButton("Open Install Settings") { openInstallSettings(context) }
                    }
                }

                SectionCard("Save Codes") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("Write Clipboard to Inbox") {
                            val uri = treeUriString?.let(Uri::parse)
                            lastStatus = if (uri != null && writeClipboardSave(context, uri)) {
                                "Clipboard save written to inbox/save_clipboard.txt."
                            } else {
                                "Clipboard write failed. Pick folder and copy save text first."
                            }
                        }
                        ActionButton(context, "decode_save", treeUriString) { lastStatus = it }
                    }
                    ActionRow(context, "validate_save", treeUriString) { lastStatus = it }
                }

                SectionCard("Source Audits") {
                    ActionGrid(
                        context,
                        listOf(
                            "find_server_calls",
                            "find_save_paths",
                            "find_root_manager",
                            "find_hacking_labels",
                            "find_todos",
                            "find_android_permissions",
                        ),
                        treeUriString
                    ) { lastStatus = it }
                }

                SectionCard("Debug Bundle") {
                    ActionRow(context, "create_debug_zip", treeUriString) { lastStatus = it }
                }

                SectionCard("Latest Result") {
                    ResultBlock(lastResult)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("Open Report") {
                            val uri = treeUriString?.let(Uri::parse)
                            lastStatus = if (uri != null && openRelativeFile(context, uri, "results", reportNameFrom(lastResult))) {
                                "Report opened."
                            } else {
                                "Report not found."
                            }
                        }
                        SecondaryButton("Open Latest Log") {
                            val uri = treeUriString?.let(Uri::parse)
                            lastStatus = if (uri != null && openNewestInFolder(context, uri, "logs", ".log")) {
                                "Log opened."
                            } else {
                                "Log not found."
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPanel(status: String, treeUriString: String?, result: BridgeResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101821)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(status, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Folder: ${treeUriString ?: "not selected"}", color = Color(0xFF9AA4B2), style = MaterialTheme.typography.bodySmall)
            Text("Last: ${result.status.uppercase()} — ${result.title}", color = statusColor(result.status), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable Column.() -> Unit) {
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
private fun ActionRow(context: Context, actionId: String, treeUriString: String?, onStatus: (String) -> Unit) {
    ActionButton(context, actionId, treeUriString, onStatus)
}

@Composable
private fun ActionGrid(context: Context, actionIds: List<String>, treeUriString: String?, onStatus: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actionIds.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                row.forEach { id ->
                    ActionButton(context, id, treeUriString, onStatus, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActionButton(context: Context, actionId: String, treeUriString: String?, onStatus: (String) -> Unit, modifier: Modifier = Modifier) {
    val label = approvedActions.firstOrNull { it.id == actionId }?.label ?: actionId
    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)),
        onClick = {
            val result = runTermuxAction(context, actionId)
            onStatus(result)
        }
    ) {
        Text(label)
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6BFF)), onClick = onClick) { Text(label) }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Text(label, color = Color.White) }
}

@Composable
private fun ResultBlock(result: BridgeResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080D12), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(result.title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(result.summary, color = Color(0xFFC7D0DA))
        Text("Action: ${result.action.ifBlank { "none" }}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        Text("Run: ${result.runId.ifBlank { "none" }}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        Text("Exit: ${result.exitCode?.toString() ?: "n/a"}", color = Color(0xFF9AA4B2), fontFamily = FontFamily.Monospace)
        result.nextAction?.let { Text("Next: $it", color = Color(0xFFFFD166)) }
    }
}

private fun runTermuxAction(context: Context, actionId: String): String {
    if (approvedActions.none { it.id == actionId }) return "Rejected unknown action: $actionId"
    return try {
        val intent = Intent(TERMUX_RUN_COMMAND_ACTION).apply {
            setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
            putExtra("com.termux.RUN_COMMAND_PATH", BRIDGE_PATH)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf(actionId))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
        }
        context.startService(intent)
        "Started Termux action: $actionId. Refresh result after it finishes."
    } catch (e: SecurityException) {
        "Permission denied. Grant RUN_COMMAND permission for AppLab Bridge."
    } catch (e: ActivityNotFoundException) {
        "Termux not found. Install Termux first."
    } catch (e: Exception) {
        "Failed to start Termux action: ${e.message ?: e::class.java.simpleName}"
    }
}

private fun readLatestResult(context: Context, treeUri: Uri): BridgeResult? {
    val text = readTextFile(context, treeUri, listOf("results", "latest_result.json")) ?: return null
    return try {
        val json = JSONObject(text)
        BridgeResult(
            schemaVersion = json.optInt("schema_version", 0),
            runId = json.optString("run_id", ""),
            action = json.optString("action", ""),
            status = json.optString("status", "unknown"),
            title = json.optString("title", "Result loaded"),
            summary = json.optString("summary", ""),
            exitCode = if (json.has("exit_code")) json.optInt("exit_code") else null,
            reportFile = json.optString("report_file", null),
            logFile = json.optString("log_file", null),
            nextAction = json.optString("next_action", null),
        )
    } catch (e: Exception) {
        BridgeResult(status = "failed", title = "Invalid result JSON", summary = e.message ?: "Could not parse latest_result.json")
    }
}

private fun readTextFile(context: Context, treeUri: Uri, path: List<String>): String? {
    val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
    val file = findDocument(root, path) ?: return null
    return context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }
}

private fun writeClipboardSave(context: Context, treeUri: Uri): Boolean {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip ?: return false
    if (clip.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN).not() && clip.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML).not()) return false
    val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim().orEmpty()
    if (text.isBlank()) return false
    val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
    val inbox = findOrCreateDir(root, "inbox") ?: return false
    inbox.findFile("save_clipboard.txt")?.delete()
    val file = inbox.createFile("text/plain", "save_clipboard.txt") ?: return false
    context.contentResolver.openOutputStream(file.uri, "wt")?.bufferedWriter()?.use { it.write(text) } ?: return false
    return true
}

private fun openRelativeFile(context: Context, treeUri: Uri, folder: String, fileName: String): Boolean {
    val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
    val file = findDocument(root, listOf(folder, fileName)) ?: return false
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(file.uri, "text/plain")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return tryStart(context, Intent.createChooser(intent, "Open file"))
}

private fun openNewestInFolder(context: Context, treeUri: Uri, folderName: String, extension: String): Boolean {
    val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
    val folder = root.findFile(folderName) ?: return false
    val file = folder.listFiles()
        .filter { it.isFile && (it.name?.endsWith(extension) == true) }
        .maxByOrNull { it.lastModified() } ?: return false
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(file.uri, "text/plain")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return tryStart(context, Intent.createChooser(intent, "Open log"))
}

private fun installLatestApk(context: Context, treeUri: Uri): Boolean {
    val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
    val apks = root.findFile("apks") ?: return false
    val apk = apks.listFiles()
        .filter { it.isFile && (it.name?.endsWith(".apk", ignoreCase = true) == true) }
        .maxByOrNull { it.lastModified() } ?: return false

    val cacheFile = File(context.cacheDir, apk.name ?: "latest.apk")
    context.contentResolver.openInputStream(apk.uri)?.use { input ->
        FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
    } ?: return false

    val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return tryStart(context, intent)
}

private fun findDocument(root: DocumentFile, path: List<String>): DocumentFile? {
    var current: DocumentFile = root
    path.forEach { part ->
        current = current.findFile(part) ?: return null
    }
    return current
}

private fun findOrCreateDir(root: DocumentFile, name: String): DocumentFile? {
    return root.findFile(name) ?: root.createDirectory(name)
}

private fun reportNameFrom(result: BridgeResult): String {
    return when (result.action) {
        "check_setup" -> "check_setup.txt"
        "check_repo" -> "check_repo.txt"
        "pull_staging" -> "pull_repo.txt"
        "show_current_commit" -> "show_current_commit.txt"
        "list_changed_files" -> "list_changed_files.txt"
        "check_latest_apk" -> "check_latest_apk.txt"
        "download_latest_apk" -> "download_latest_apk.txt"
        "decode_save", "validate_save" -> "save_report.txt"
        "find_server_calls" -> "server_calls_report.txt"
        "find_save_paths" -> "save_paths_report.txt"
        "find_root_manager" -> "root_manager_report.txt"
        "find_hacking_labels" -> "hacking_labels_report.txt"
        "find_todos" -> "todos_report.txt"
        "find_android_permissions" -> "android_permissions_report.txt"
        "create_debug_zip" -> "debug_bundle_report.txt"
        else -> "latest_result.json"
    }
}

private fun statusColor(status: String): Color {
    return when (status.lowercase()) {
        "success" -> Color(0xFF5CE38A)
        "failed" -> Color(0xFFFF6B6B)
        "running" -> Color(0xFFFFD166)
        else -> Color(0xFF9AA4B2)
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    tryStart(context, intent)
}

private fun openInstallSettings(context: Context) {
    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    tryStart(context, intent)
}

private fun tryStart(context: Context, intent: Intent): Boolean {
    return try {
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        Toast.makeText(context, e.message ?: "No app can handle this action.", Toast.LENGTH_LONG).show()
        false
    }
}
