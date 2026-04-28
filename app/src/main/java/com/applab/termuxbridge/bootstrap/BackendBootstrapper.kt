package com.applab.termuxbridge.bootstrap

import android.content.Context
import android.net.Uri
import com.applab.termuxbridge.storage.SafBridgeFolder

class BackendBootstrapper(
    private val context: Context,
    private val bridgeFolder: SafBridgeFolder
) {
    fun bootstrapScriptText(): BootstrapScriptResult {
        val scriptText = runCatching {
            context.assets.open(ASSET_INSTALLER).bufferedReader().use { it.readText() }
        }.getOrElse { error ->
            return BootstrapScriptResult(false, "", "Bundled bootstrap installer missing: ${error.message ?: error::class.java.simpleName}")
        }
        if (!scriptText.contains("AppLab Bridge backend bootstrap")) {
            return BootstrapScriptResult(false, "", "Bundled bootstrap installer failed content verification.")
        }
        return BootstrapScriptResult(true, scriptText, "Bundled backend bootstrap installer loaded.")
    }

    fun writeBootstrapFiles(treeUri: Uri?): BootstrapWriteResult {
        if (treeUri == null) {
            return BootstrapWriteResult(false, "Choose Documents/AppLabBridge before writing bootstrap files.")
        }
        val script = bootstrapScriptText()
        if (!script.success) return BootstrapWriteResult(false, script.message)
        val ok = bridgeFolder.writeText(treeUri, "bootstrap", INSTALLER_NAME, script.text)
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
            "Bootstrap installer also written for inspection at Documents/AppLabBridge/bootstrap/$INSTALLER_NAME. Critical bootstrap runs through Termux stdin, not this file path."
        )
    }

    companion object {
        const val INSTALLER_NAME = "install_backend.sh"
        private const val ASSET_INSTALLER = "applab/install_backend.sh"
    }
}

data class BootstrapWriteResult(val success: Boolean, val message: String)
data class BootstrapScriptResult(val success: Boolean, val text: String, val message: String)
