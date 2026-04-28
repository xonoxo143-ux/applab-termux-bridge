package com.applab.termuxbridge.actions

import android.net.Uri
import com.applab.termuxbridge.storage.SafBridgeFolder

class BackendActionRegistryReader(private val bridgeFolder: SafBridgeFolder) {
    fun read(treeUri: Uri?): BackendActionRegistryState {
        if (treeUri == null) return BackendActionRegistryState.MissingFolder
        val text = bridgeFolder.readText(treeUri, listOf("config", "actions.json"))
            ?: return BackendActionRegistryState.MissingRegistry
        return runCatching { BackendActionRegistry.fromJson(text) }
            .fold(
                onSuccess = { BackendActionRegistryState.Loaded(it) },
                onFailure = { error -> BackendActionRegistryState.ParseError(error.message ?: error::class.java.simpleName) }
            )
    }
}
