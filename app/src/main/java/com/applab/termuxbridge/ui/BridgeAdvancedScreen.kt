package com.applab.termuxbridge.ui

import androidx.compose.runtime.Composable
import com.applab.termuxbridge.bridge.BridgeAction

@Composable
fun BridgeAdvancedScreen(onRunAction: (BridgeAction) -> Unit) {
    BridgeSectionCard("Repo Setup") {
        BridgeHintText("Use these when a repo is missing on the phone or you need to reselect the active repo. Clone actions also select the repo after success.")
        BridgeActionButton(BridgeAction.CLONE_BRIDGE, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.CLONE_LIBRESEED, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.LIST_PROJECTS, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_ACTIVE_REPO, onRunAction)
    }

    BridgeSectionCard("Debug Bundle") {
        BridgeHintText("Create a zip of AppLabBridge reports, logs, config, and result files. APK files are excluded.")
        BridgeActionButton(BridgeAction.CREATE_DEBUG_ZIP, onRunAction)
    }

    BridgeSectionCard("Save Tools") {
        BridgeHintText("These are recognized by the backend but intentionally parked until the save workflow is designed and tested.")
        BridgeActionButton(BridgeAction.DECODE_SAVE, onRunAction, tone = BridgeActionTone.NEUTRAL)
        BridgeActionButton(BridgeAction.VALIDATE_SAVE, onRunAction, tone = BridgeActionTone.NEUTRAL)
    }

    BridgeSectionCard("Source Audits") {
        BridgeHintText("These actions are recognized by the backend and return clear parked reports for now. Keep them grouped away from normal repo work.")
        BridgeActionButton(BridgeAction.FIND_SERVER_CALLS, onRunAction, tone = BridgeActionTone.NEUTRAL)
        BridgeActionButton(BridgeAction.FIND_SAVE_PATHS, onRunAction, tone = BridgeActionTone.NEUTRAL)
        BridgeActionButton(BridgeAction.FIND_ROOT_MANAGER, onRunAction, tone = BridgeActionTone.NEUTRAL)
        BridgeActionButton(BridgeAction.FIND_HACKING_LABELS, onRunAction, tone = BridgeActionTone.NEUTRAL)
        BridgeActionButton(BridgeAction.FIND_TODOS, onRunAction, tone = BridgeActionTone.NEUTRAL)
        BridgeActionButton(BridgeAction.FIND_ANDROID_PERMISSIONS, onRunAction, tone = BridgeActionTone.NEUTRAL)
    }
}
