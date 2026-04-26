package com.applab.termuxbridge.bridge

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class TermuxRunner(private val context: Context) {
    fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun run(action: BridgeAction): RunResult {
        if (!isTermuxInstalled()) {
            return RunResult(false, "Termux is not installed or is not visible to this app.")
        }
        return try {
            val intent = Intent(TERMUX_RUN_COMMAND_ACTION).apply {
                setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
                putExtra(EXTRA_PATH, BRIDGE_PATH)
                putExtra(EXTRA_ARGUMENTS, arrayOf(action.id))
                putExtra(EXTRA_WORKDIR, TERMUX_HOME)
                putExtra(EXTRA_BACKGROUND, true)
                putExtra(EXTRA_SESSION_ACTION, "0")
                putExtra(EXTRA_COMMAND_LABEL, "AppLab Bridge: ${action.label}")
                putExtra(EXTRA_COMMAND_DESCRIPTION, "Run approved AppLab bridge action ${action.id}")
            }
            context.startService(intent)
            RunResult(true, "Started ${action.label}. Waiting for result file.")
        } catch (_: SecurityException) {
            RunResult(false, "Permission denied. Grant this app permission to run commands in Termux.")
        } catch (_: ActivityNotFoundException) {
            RunResult(false, "Termux RunCommandService was not found.")
        } catch (error: Exception) {
            RunResult(false, "Failed to start Termux action: ${error.message ?: error::class.java.simpleName}")
        }
    }

    companion object {
        const val TERMUX_PACKAGE = "com.termux"
        private const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
        private const val TERMUX_RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
        private const val BRIDGE_PATH = "/data/data/com.termux/files/home/.termux/applab/bridge.sh"
        private const val TERMUX_HOME = "/data/data/com.termux/files/home"

        private const val EXTRA_PATH = "com.termux.RUN_COMMAND_PATH"
        private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        private const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
        private const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_COMMAND_LABEL"
        private const val EXTRA_COMMAND_DESCRIPTION = "com.termux.RUN_COMMAND_COMMAND_DESCRIPTION"
    }
}

data class RunResult(val started: Boolean, val message: String)
