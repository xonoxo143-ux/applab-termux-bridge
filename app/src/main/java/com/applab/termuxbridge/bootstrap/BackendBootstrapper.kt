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
            return BootstrapWriteResult(false, "Choose the shared bridge folder before writing bootstrap files.")
        }
        val script = bootstrapScriptText()
        if (!script.success) return BootstrapWriteResult(false, script.message)

        val wroteInspectionCopy = bridgeFolder.writeText(treeUri, "bootstrap", INSTALLER_NAME, script.text)
        if (!wroteInspectionCopy) {
            return BootstrapWriteResult(
                true,
                "Could not write optional bootstrap inspection copy, but critical bootstrap will still run through Termux stdin."
            )
        }

        val readBack = bridgeFolder.readText(treeUri, listOf("bootstrap", INSTALLER_NAME))
        if (readBack.isNullOrBlank()) {
            return BootstrapWriteResult(
                true,
                "Bootstrap inspection copy was written, but Android could not read it back by name. Continuing because critical bootstrap runs through Termux stdin."
            )
        }
        if (!readBack.contains("AppLab Bridge backend bootstrap")) {
            return BootstrapWriteResult(
                true,
                "Bootstrap inspection copy readback did not match exactly. Continuing because critical bootstrap runs through Termux stdin."
            )
        }
        return BootstrapWriteResult(
            true,
            "Bootstrap installer also written for inspection at bootstrap/$INSTALLER_NAME. Critical bootstrap runs through Termux stdin."
        )
    }

    companion object {
        const val INSTALLER_NAME = "install_backend.sh"
        private const val ASSET_INSTALLER = "applab/install_backend.sh"
    }
}

data class BootstrapWriteResult(val success: Boolean, val message: String)
data class BootstrapScriptResult(val success: Boolean, val text: String, val message: String)
