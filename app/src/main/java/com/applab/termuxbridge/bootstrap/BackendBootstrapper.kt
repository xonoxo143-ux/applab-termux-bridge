package com.applab.termuxbridge.bootstrap

import android.content.Context
import android.net.Uri
import com.applab.termuxbridge.storage.SafBridgeFolder

class BackendBootstrapper(
    private val context: Context,
    private val bridgeFolder: SafBridgeFolder
) {
    fun writeBootstrapFiles(treeUri: Uri?): BootstrapWriteResult {
        if (treeUri == null) {
            return BootstrapWriteResult(false, "Choose Documents/AppLabBridge before writing bootstrap files.")
        }
        val scriptText = runCatching {
            context.assets.open(ASSET_INSTALLER).bufferedReader().use { it.readText() }
        }.getOrElse { error ->
            return BootstrapWriteResult(false, "Bundled bootstrap installer missing: ${error.message ?: error::class.java.simpleName}")
        }
        val ok = bridgeFolder.writeText(treeUri, "bootstrap", INSTALLER_NAME, scriptText)
        if (!ok) {
            return BootstrapWriteResult(false, "Could not write backend bootstrap installer to the selected bridge folder.")
        }
        val readBack = bridgeFolder.readText(treeUri, listOf("bootstrap", INSTALLER_NAME))
        if (readBack.isNullOrBlank()) {
            return BootstrapWriteResult(false, "Bootstrap installer write was reported successful, but Android could not read it back from bootstrap/$INSTALLER_NAME.")
        }
        if (!readBack.contains("AppLab Bridge backend bootstrap")) {
            return BootstrapWriteResult(false, "Bootstrap installer was written, but read-back verification did not match the bundled installer.")
        }
        return BootstrapWriteResult(
            true,
            "Bootstrap installer verified at Documents/AppLabBridge/bootstrap/$INSTALLER_NAME. Termux will run: $TERMUX_INSTALLER_PATH"
        )
    }

    companion object {
        const val INSTALLER_NAME = "install_backend.sh"
        private const val ASSET_INSTALLER = "applab/install_backend.sh"
        const val TERMUX_INSTALLER_PATH = "/data/data/com.termux/files/home/storage/shared/Documents/AppLabBridge/bootstrap/install_backend.sh"
    }
}

data class BootstrapWriteResult(val success: Boolean, val message: String)
