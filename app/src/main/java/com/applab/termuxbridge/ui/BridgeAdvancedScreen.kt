package com.applab.termuxbridge.ui

import androidx.compose.runtime.Composable
import com.applab.termuxbridge.bridge.BridgeAction

@Composable
fun BridgeAdvancedScreen(onRunAction: (BridgeAction) -> Unit) {
    BridgeSectionCard("Repo Recovery") {
        BridgeHintText("Use these when the normal Choose Repo flow cannot find or select a repo. These are deliberately kept out of normal Repo.")
        BridgeActionButton(BridgeAction.LIST_PROJECTS, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_ACTIVE_REPO, onRunAction)
        BridgeActionButton(BridgeAction.SET_ACTIVE_BRIDGE, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.SET_ACTIVE_LIBRESEED, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.CLONE_BRIDGE, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.CLONE_LIBRESEED, onRunAction, tone = BridgeActionTone.WARNING)
    }

    BridgeSectionCard("Raw Repo Tools") {
        BridgeHintText("Low-level git inspection and branch tools. Normal Repo only exposes Choose, Check, Update, and Changes.")
        BridgeActionButton(BridgeAction.SHOW_STATUS, onRunAction)
        BridgeActionButton(BridgeAction.FETCH_REPO, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_BRANCHES, onRunAction)
        BridgeActionButton(BridgeAction.CHECKOUT_STAGING, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.PULL_STAGING, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.SHOW_CURRENT_COMMIT, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_DIFF_SUMMARY, onRunAction)
        BridgeActionButton(BridgeAction.SHOW_FULL_DIFF, onRunAction)
    }

    BridgeSectionCard("Backend / Debug") {
        BridgeHintText("Backend registry, dispatcher, and debug bundle actions. Use only when normal screens are stale or broken.")
        BridgeActionButton(BridgeAction.LIST_ACTIONS, onRunAction)
        BridgeActionButton(BridgeAction.UPDATE_DISPATCHER, onRunAction, tone = BridgeActionTone.WARNING)
        BridgeActionButton(BridgeAction.CREATE_DEBUG_ZIP, onRunAction)
    }

    BridgeSectionCard("Parked Tools") {
        BridgeHintText("Recognized by the backend but parked until those workflows are designed and tested.")
        BridgeActionButton(BridgeAction.DECODE_SAVE, onRunAction, tone = BridgeActionTone.NEUTRAL)
        BridgeActionButton(BridgeAction.VALIDATE_SAVE, onRunAction, tone = BridgeActionTone.NEUTRAL)
        BridgeActionButton(BridgeAction.FIND_SERVER_CALLS, onRunAction, tone = BridgeActionTone.NEUTRAL)
        BridgeActionButton(BridgeAction.FIND_SAVE_PATHS, onRunAction, tone = BridgeActionTone.NEUTRAL)
        BridgeActionButton(BridgeAction.FIND_ROOT_MANAGER, onRunAction, tone = BridgeActionTone.NEUTRAL)
        BridgeActionButton(BridgeAction.FIND_HACKING_LABELS, onRunAction, tone = BridgeActionTone.NEUTRAL)
        BridgeActionButton(BridgeAction.FIND_TODOS, onRunAction, tone = BridgeActionTone.NEUTRAL)
        BridgeActionButton(BridgeAction.FIND_ANDROID_PERMISSIONS, onRunAction, tone = BridgeActionTone.NEUTRAL)
    }
}
