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

    fun hasRunCommandPermission(): Boolean {
        return context.checkSelfPermission(RUN_COMMAND_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    fun run(action: BridgeAction): RunResult {
        return runTermuxCommand(
            path = BRIDGE_PATH,
            arguments = arrayOf(action.id),
            stdin = null,
            label = "AppLab Bridge: ${action.label}",
            description = "Run approved AppLab bridge action ${action.id}",
            waitingMessage = "Requested Termux action: ${action.id}. Waiting for results/latest_result.json to update."
        )
    }

    fun runBootstrapInstaller(scriptText: String): RunResult {
        return runTermuxCommand(
            path = TERMUX_BASH,
            arguments = emptyArray(),
            stdin = scriptText,
            label = "AppLab Bridge: Bootstrap Backend",
            description = "Install or repair the AppLab Termux backend from bundled script stdin.",
            waitingMessage = "Requested Termux backend bootstrap through bash stdin. Waiting for results/latest_result.json to update."
        )
    }

    private fun runTermuxCommand(
        path: String,
        arguments: Array<String>,
        stdin: String?,
        label: String,
        description: String,
        waitingMessage: String
    ): RunResult {
        val termuxInstalled = isTermuxInstalled()
        val runPermission = hasRunCommandPermission()
        if (!termuxInstalled) {
            return RunResult(
                started = false,
                phase = BridgeCommandPhase.LAUNCH_FAILED,
                message = "Termux is not installed or is not visible to this app. Install Termux or allow package visibility.",
                commandPath = path,
                arguments = arguments.toList(),
                termuxInstalled = false,
                runCommandPermission = runPermission
            )
        }
        if (!runPermission) {
            return RunResult(
                started = false,
                phase = BridgeCommandPhase.LAUNCH_FAILED,
                message = "AppLab Bridge does not have Termux RUN_COMMAND permission. Open this app's permissions and allow Run commands in Termux.",
                commandPath = path,
                arguments = arguments.toList(),
                termuxInstalled = true,
                runCommandPermission = false
            )
        }
        return try {
            val intent = Intent(TERMUX_RUN_COMMAND_ACTION).apply {
                setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
                putExtra(EXTRA_PATH, path)
                putExtra(EXTRA_ARGUMENTS, arguments)
                if (stdin != null) putExtra(EXTRA_STDIN, stdin)
                putExtra(EXTRA_WORKDIR, TERMUX_HOME)
                putExtra(EXTRA_BACKGROUND, true)
                putExtra(EXTRA_SESSION_ACTION, "0")
                putExtra(EXTRA_COMMAND_LABEL, label)
                putExtra(EXTRA_COMMAND_DESCRIPTION, description)
            }
            context.startService(intent)
            RunResult(
                started = true,
                phase = BridgeCommandPhase.WAITING_FOR_RESULT,
                message = waitingMessage,
                commandPath = path,
                arguments = arguments.toList(),
                stdinBytes = stdin?.toByteArray()?.size ?: 0,
                termuxInstalled = true,
                runCommandPermission = true
            )
        } catch (_: SecurityException) {
            RunResult(
                started = false,
                phase = BridgeCommandPhase.LAUNCH_FAILED,
                message = "Permission denied by Android while starting Termux. Reopen this app's permissions and verify Run commands in Termux is allowed.",
                commandPath = path,
                arguments = arguments.toList(),
                stdinBytes = stdin?.toByteArray()?.size ?: 0,
                termuxInstalled = true,
                runCommandPermission = true
            )
        } catch (_: ActivityNotFoundException) {
            RunResult(
                started = false,
                phase = BridgeCommandPhase.LAUNCH_FAILED,
                message = "Termux RunCommandService was not found. Check Termux version and installation source.",
                commandPath = path,
                arguments = arguments.toList(),
                stdinBytes = stdin?.toByteArray()?.size ?: 0,
                termuxInstalled = true,
                runCommandPermission = true
            )
        } catch (error: Exception) {
            RunResult(
                started = false,
                phase = BridgeCommandPhase.LAUNCH_FAILED,
                message = "Failed to start Termux action: ${error.message ?: error::class.java.simpleName}",
                commandPath = path,
                arguments = arguments.toList(),
                stdinBytes = stdin?.toByteArray()?.size ?: 0,
                termuxInstalled = true,
                runCommandPermission = true
            )
        }
    }

    companion object {
        const val TERMUX_PACKAGE = "com.termux"
        const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
        private const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
        private const val TERMUX_RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
        private const val BRIDGE_PATH = "/data/data/com.termux/files/home/.termux/applab/bridge_v2.sh"
        private const val TERMUX_BASH = "/data/data/com.termux/files/usr/bin/bash"
        private const val TERMUX_HOME = "/data/data/com.termux/files/home"

        private const val EXTRA_PATH = "com.termux.RUN_COMMAND_PATH"
        private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        private const val EXTRA_STDIN = "com.termux.RUN_COMMAND_STDIN"
        private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        private const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
        private const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_COMMAND_LABEL"
        private const val EXTRA_COMMAND_DESCRIPTION = "com.termux.RUN_COMMAND_COMMAND_DESCRIPTION"
    }
}

data class RunResult(
    val started: Boolean,
    val phase: BridgeCommandPhase,
    val message: String,
    val commandPath: String = "",
    val arguments: List<String> = emptyList(),
    val stdinBytes: Int = 0,
    val termuxInstalled: Boolean = false,
    val runCommandPermission: Boolean = false
)
