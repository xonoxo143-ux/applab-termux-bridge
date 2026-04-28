package com.applab.termuxbridge.bridge

import android.app.IntentService
import android.content.Intent
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TermuxResultService : IntentService("TermuxResultService") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return
        val executionId = intent.getIntExtra(EXTRA_EXECUTION_ID, 0)
        val expectedAction = intent.getStringExtra(EXTRA_EXPECTED_ACTION).orEmpty()
        val resultBundle = intent.getBundleExtra(EXTRA_PLUGIN_RESULT_BUNDLE)
        val logText = buildString {
            appendLine("[${timestamp()}] termux.result executionId=$executionId expected=$expectedAction")
            if (resultBundle == null) {
                appendLine("bundle=null")
            } else {
                appendLine("exitCode=${resultBundle.getInt(EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE, Int.MIN_VALUE)}")
                appendLine("err=${resultBundle.getInt(EXTRA_PLUGIN_RESULT_BUNDLE_ERR, Int.MIN_VALUE)}")
                appendLine("errmsg=${resultBundle.getString(EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG, "")}")
                appendLine("stdoutOriginalLength=${resultBundle.getString(EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH, "")}")
                appendLine("stderrOriginalLength=${resultBundle.getString(EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH, "")}")
                appendLine("stdout=<<<")
                appendLine(resultBundle.getString(EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT, ""))
                appendLine(">>>")
                appendLine("stderr=<<<")
                appendLine(resultBundle.getString(EXTRA_PLUGIN_RESULT_BUNDLE_STDERR, ""))
                appendLine(">>>")
            }
        }
        appendInternalLog(logText)
        Log.d("AppLabTermuxResult", logText)
    }

    private fun appendInternalLog(text: String) {
        runCatching {
            val dir = File(filesDir, "diagnostics")
            dir.mkdirs()
            File(dir, "termux_result_service.log").appendText(text + "\n")
        }
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
    }

    companion object {
        const val EXTRA_EXECUTION_ID = "com.applab.termuxbridge.EXTRA_EXECUTION_ID"
        const val EXTRA_EXPECTED_ACTION = "com.applab.termuxbridge.EXTRA_EXPECTED_ACTION"

        // TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_PENDING_INTENT
        const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"

        // TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE and nested result keys.
        const val EXTRA_PLUGIN_RESULT_BUNDLE = "com.termux.service.extra.PLUGIN_RESULT_BUNDLE"
        const val EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT = "com.termux.service.extra.PLUGIN_RESULT_BUNDLE_STDOUT"
        const val EXTRA_PLUGIN_RESULT_BUNDLE_STDERR = "com.termux.service.extra.PLUGIN_RESULT_BUNDLE_STDERR"
        const val EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE = "com.termux.service.extra.PLUGIN_RESULT_BUNDLE_EXIT_CODE"
        const val EXTRA_PLUGIN_RESULT_BUNDLE_ERR = "com.termux.service.extra.PLUGIN_RESULT_BUNDLE_ERR"
        const val EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG = "com.termux.service.extra.PLUGIN_RESULT_BUNDLE_ERRMSG"
        const val EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH = "com.termux.service.extra.PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH"
        const val EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH = "com.termux.service.extra.PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH"
    }
}
