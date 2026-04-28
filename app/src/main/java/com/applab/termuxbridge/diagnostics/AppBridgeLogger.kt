package com.applab.termuxbridge.diagnostics

import android.net.Uri
import com.applab.termuxbridge.storage.SafBridgeFolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppBridgeLogger(private val bridgeFolder: SafBridgeFolder) {
    fun log(treeUri: Uri?, event: String, detail: String = "") {
        if (treeUri == null) return
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val safeDetail = detail.replace('\n', ' ').take(2000)
        bridgeFolder.appendText(treeUri, "logs", APP_LOG_NAME, "[$stamp] $event $safeDetail")
    }

    companion object {
        const val APP_LOG_NAME = "android_app_bridge.log"
    }
}
