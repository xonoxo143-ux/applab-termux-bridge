package com.applab.termuxbridge.ui

import com.applab.termuxbridge.actions.BackendActionDescriptor
import com.applab.termuxbridge.bridge.BridgeResult

enum class ActionAvailability {
    READY,
    WARNING,
    BLOCKED,
    HIDDEN
}

data class ActionRelevance(
    val availability: ActionAvailability,
    val reason: String,
    val tone: BridgeActionTone
) {
    val canRun: Boolean get() = availability == ActionAvailability.READY || availability == ActionAvailability.WARNING
}

fun relevanceForAction(
    descriptor: BackendActionDescriptor,
    result: BridgeResult,
    hasTermuxPermission: Boolean,
    latestApkName: String?
): ActionRelevance {
    if (descriptor.advanced || descriptor.parked) {
        return ActionRelevance(ActionAvailability.HIDDEN, "Advanced or parked action", BridgeActionTone.NEUTRAL)
    }

    if (!hasTermuxPermission) {
        return ActionRelevance(ActionAvailability.BLOCKED, "Termux command permission missing", BridgeActionTone.NEUTRAL)
    }

    if (descriptor.requiresRepo && result.repoName.isNullOrBlank() && result.branch.isNullOrBlank()) {
        return ActionRelevance(ActionAvailability.BLOCKED, "No selected repo loaded", BridgeActionTone.NEUTRAL)
    }

    if (descriptor.requiresPatchFile && result.hasPatchFile != true) {
        return ActionRelevance(ActionAvailability.BLOCKED, "patches/patch.sh missing", BridgeActionTone.NEUTRAL)
    }

    if (descriptor.requiresCleanTree) {
        when (result.dirty) {
            true -> return ActionRelevance(ActionAvailability.BLOCKED, "Repo has local changes", BridgeActionTone.NEUTRAL)
            null -> return ActionRelevance(ActionAvailability.WARNING, "Repo state unknown; run Git Status first", BridgeActionTone.WARNING)
            false -> Unit
        }
    }

    when (descriptor.id) {
        "commit_no_apk" -> {
            val staged = result.stagedFiles ?: 0
            if (staged <= 0) return ActionRelevance(ActionAvailability.BLOCKED, "No staged files", BridgeActionTone.NEUTRAL)
        }
        "push_current" -> {
            val ahead = result.ahead
            if (ahead == 0) return ActionRelevance(ActionAvailability.WARNING, "No local commits ahead detected", BridgeActionTone.WARNING)
            if (ahead == null) return ActionRelevance(ActionAvailability.WARNING, "Ahead/behind state unknown", BridgeActionTone.WARNING)
        }
        "download_latest_apk" -> {
            return ActionRelevance(ActionAvailability.WARNING, "Downloads latest GitHub APK artifact", BridgeActionTone.WARNING)
        }
        "check_latest_apk" -> {
            return ActionRelevance(ActionAvailability.WARNING, "Uses GitHub network access", BridgeActionTone.WARNING)
        }
    }

    if (descriptor.requiresNetwork) {
        return ActionRelevance(ActionAvailability.WARNING, "Uses network/GitHub", BridgeActionTone.WARNING)
    }

    if (descriptor.risk in setOf("mutating", "publishing", "install") || descriptor.confirm) {
        return ActionRelevance(ActionAvailability.WARNING, "Risky action; confirmation required", BridgeActionTone.WARNING)
    }

    if (descriptor.risk == "experimental") {
        return ActionRelevance(ActionAvailability.WARNING, "Experimental action", BridgeActionTone.WARNING)
    }

    if (descriptor.id == "download_latest_apk" && latestApkName != null) {
        return ActionRelevance(ActionAvailability.WARNING, "Existing APK found; download may replace or add another", BridgeActionTone.WARNING)
    }

    return ActionRelevance(ActionAvailability.READY, "Ready", BridgeActionTone.PRIMARY)
}

fun actionAvailabilityLabel(relevance: ActionRelevance): String {
    return when (relevance.availability) {
        ActionAvailability.READY -> "ready"
        ActionAvailability.WARNING -> "warning"
        ActionAvailability.BLOCKED -> "blocked"
        ActionAvailability.HIDDEN -> "hidden"
    }
}
