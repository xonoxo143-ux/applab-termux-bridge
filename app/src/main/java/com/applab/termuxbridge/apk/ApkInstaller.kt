package com.applab.termuxbridge.apk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.applab.termuxbridge.storage.SafBridgeFolder
import java.io.File
import java.io.FileOutputStream

class ApkInstaller(
    private val context: Context,
    private val folder: SafBridgeFolder
) {
    fun latestApkName(treeUri: Uri?): String? {
        if (treeUri == null) return null
        return folder.newestFile(treeUri, "apks", ".apk")?.name
    }

    fun installLatest(treeUri: Uri?): InstallResult {
        if (treeUri == null) return InstallResult(false, "Pick bridge folder first.")
        val apk = folder.newestFile(treeUri, "apks", ".apk")
            ?: return InstallResult(false, "No APK found in apks/.")
        clearOldApkCache()
        val cacheFile = File(context.cacheDir, apk.name ?: "latest.apk")
        context.contentResolver.openInputStream(apk.uri)?.use { input ->
            FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
        } ?: return InstallResult(false, "Could not copy APK to app cache.")

        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            InstallResult(true, "Installer opened for ${cacheFile.name}.")
        } catch (error: Exception) {
            Toast.makeText(context, error.message ?: "Could not open installer.", Toast.LENGTH_LONG).show()
            InstallResult(false, "Installer failed: ${error.message ?: error::class.java.simpleName}")
        }
    }

    fun openInstallSettings(): Boolean {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        return try {
            context.startActivity(intent)
            true
        } catch (error: Exception) {
            Toast.makeText(context, error.message ?: "Could not open install settings.", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun clearOldApkCache() {
        context.cacheDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".apk", ignoreCase = true) }
            ?.forEach { it.delete() }
    }
}

data class InstallResult(val success: Boolean, val message: String)
