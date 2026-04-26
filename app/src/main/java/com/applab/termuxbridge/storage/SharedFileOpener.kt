package com.applab.termuxbridge.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile

class SharedFileOpener(
    private val context: Context,
    private val folder: SafBridgeFolder
) {
    fun openReport(treeUri: Uri?, reportFileName: String): Boolean {
        if (treeUri == null) return false
        return openDocument(folder.find(treeUri, listOf("results", reportFileName)), "text/plain", "Open report")
    }

    fun openNewestLog(treeUri: Uri?): Boolean {
        if (treeUri == null) return false
        return openDocument(folder.newestFile(treeUri, "logs", ".log"), "text/plain", "Open log")
    }

    fun openDebugZip(treeUri: Uri?): Boolean {
        if (treeUri == null) return false
        return openDocument(folder.newestFile(treeUri, "debug_zips", ".zip"), "application/zip", "Open debug zip")
    }

    private fun openDocument(file: DocumentFile?, mimeType: String, chooserTitle: String): Boolean {
        if (file == null) return false
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return tryStart(Intent.createChooser(intent, chooserTitle))
    }

    private fun tryStart(intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (error: Exception) {
            Toast.makeText(context, error.message ?: "No app can open this file.", Toast.LENGTH_LONG).show()
            false
        }
    }
}
