package com.applab.termuxbridge.bridge

enum class BridgeCommandPhase {
    IDLE,
    LAUNCH_REQUESTED,
    WAITING_FOR_RESULT,
    RESULT_RECEIVED,
    RESULT_MISMATCH,
    TIMED_OUT,
    LAUNCH_FAILED
}

data class BridgePendingCommand(
    val action: BridgeAction,
    val previousRunId: String,
    val phase: BridgeCommandPhase = BridgeCommandPhase.LAUNCH_REQUESTED
) {
    val expectedActionId: String get() = action.id
}

fun BridgeCommandPhase.userLabel(): String {
    return when (this) {
        BridgeCommandPhase.IDLE -> "Idle"
        BridgeCommandPhase.LAUNCH_REQUESTED -> "Termux command requested"
        BridgeCommandPhase.WAITING_FOR_RESULT -> "Waiting for saved result file"
        BridgeCommandPhase.RESULT_RECEIVED -> "Result received"
        BridgeCommandPhase.RESULT_MISMATCH -> "Different result received"
        BridgeCommandPhase.TIMED_OUT -> "Timed out waiting for result"
        BridgeCommandPhase.LAUNCH_FAILED -> "Could not launch Termux command"
    }
}
