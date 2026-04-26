package com.applab.termuxbridge.bridge

import android.net.Uri
import com.applab.termuxbridge.storage.SafBridgeFolder

class BridgeResultReader(private val folder: SafBridgeFolder) {
    fun readLatest(treeUri: Uri?): BridgeResult {
        if (treeUri == null) return BridgeResult.missingFolder()
        val text = folder.readText(treeUri, listOf("results", "latest_result.json"))
            ?: return BridgeResult.missingResult()
        return BridgeResult.fromJson(text)
    }
}
