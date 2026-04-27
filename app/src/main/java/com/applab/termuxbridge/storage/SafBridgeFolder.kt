package com.applab.termuxbridge.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class SafBridgeFolder(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun savedUri(): Uri? = prefs.getString(KEY_TREE_URI, null)?.let(Uri::parse)

    fun saveUri(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply()
    }

    fun root(uri: Uri): DocumentFile? = DocumentFile.fromTreeUri(context, uri)

    fun bridgeRoot(uri: Uri, create: Boolean = false): DocumentFile? {
        val selected = root(uri) ?: return null
        if (selected.name == BRIDGE_DIR_NAME) return selected

        val existing = selected.findFile(BRIDGE_DIR_NAME)
        if (existing != null && existing.isDirectory) return existing

        return if (create) selected.createDirectory(BRIDGE_DIR_NAME) else selected
    }

    fun prepareLayout(uri: Uri): FolderPreparationResult {
        val bridge = bridgeRoot(uri, create = true) ?: return FolderPreparationResult(false, "Could not open or create AppLabBridge folder.")
        val created = mutableListOf<String>()
        REQUIRED_DIRS.forEach { name ->
            if (bridge.findFile(name) == null) {
                bridge.createDirectory(name) ?: return FolderPreparationResult(false, "Could not create $name folder.")
                created += name
            }
        }
        val config = bridge.findFile("config") ?: return FolderPreparationResult(false, "Config folder is missing.")
        config.findFile("android_selected_folder.txt")?.delete()
        val marker = config.createFile("text/plain", "android_selected_folder.txt")
            ?: return FolderPreparationResult(false, "Could not write folder marker.")
        context.contentResolver.openOutputStream(marker.uri)?.bufferedWriter()?.use { writer ->
            writer.write("Selected by AppLab Termux Bridge Android app.\n")
            writer.write("Resolved bridge folder: ${bridge.name}\n")
            writer.write("Recommended Termux path: ~/storage/shared/Documents/AppLabBridge\n")
        }
        return if (created.isEmpty()) {
            FolderPreparationResult(true, "Bridge folder ready.")
        } else {
            FolderPreparationResult(true, "Bridge folder ready. Created: ${created.joinToString()}.")
        }
    }

    fun readText(uri: Uri, path: List<String>): String? {
        val file = find(uri, path) ?: return null
        return context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }
    }

    fun writeText(uri: Uri, folderName: String, fileName: String, text: String): Boolean {
        val bridge = bridgeRoot(uri, create = true) ?: return false
        val folder = findOrCreateDir(bridge, folderName) ?: return false
        folder.findFile(fileName)?.delete()
        val file = folder.createFile("text/plain", fileName) ?: return false
        context.contentResolver.openOutputStream(file.uri)?.bufferedWriter()?.use { writer -> writer.write(text) } ?: return false
        return true
    }

    fun find(uri: Uri, path: List<String>): DocumentFile? {
        var current = bridgeRoot(uri) ?: return null
        path.forEach { part -> current = current.findFile(part) ?: return null }
        return current
    }

    fun newestFile(uri: Uri, folderName: String, extension: String): DocumentFile? {
        val folder = find(uri, listOf(folderName)) ?: return null
        return folder.listFiles()
            .filter { it.isFile && (it.name?.endsWith(extension, ignoreCase = true) == true) }
            .maxByOrNull { it.lastModified() }
    }

    private fun findOrCreateDir(root: DocumentFile, name: String): DocumentFile? {
        return root.findFile(name) ?: root.createDirectory(name)
    }

    companion object {
        private const val PREFS = "applab_bridge_prefs"
        private const val KEY_TREE_URI = "tree_uri"
        private const val BRIDGE_DIR_NAME = "AppLabBridge"
        val REQUIRED_DIRS = listOf("config", "inbox", "logs", "results", "apks", "save_codes", "debug_zips", "patches", "reports")
    }
}

data class FolderPreparationResult(val success: Boolean, val message: String)
