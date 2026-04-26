package com.applab.termuxbridge.bridge

enum class BridgeAction(
    val id: String,
    val label: String,
    val section: BridgeSection,
    val expectedReportName: String
) {
    CHECK_SETUP("check_setup", "Check Termux Setup", BridgeSection.SETUP, "check_setup.txt"),

    CHECK_REPO("check_repo", "Check Repo", BridgeSection.REPO, "check_repo.txt"),
    PULL_STAGING("pull_staging", "Pull Staging", BridgeSection.REPO, "pull_repo.txt"),
    SHOW_CURRENT_COMMIT("show_current_commit", "Show Current Commit", BridgeSection.REPO, "show_current_commit.txt"),
    LIST_CHANGED_FILES("list_changed_files", "List Changed Files", BridgeSection.REPO, "list_changed_files.txt"),

    CHECK_LATEST_APK("check_latest_apk", "Check Latest APK", BridgeSection.APK, "check_latest_apk.txt"),
    DOWNLOAD_LATEST_APK("download_latest_apk", "Download Latest APK", BridgeSection.APK, "download_latest_apk.txt"),

    DECODE_SAVE("decode_save", "Decode Clipboard Save", BridgeSection.SAVE, "save_report.txt"),
    VALIDATE_SAVE("validate_save", "Validate Save", BridgeSection.SAVE, "save_report.txt"),

    FIND_SERVER_CALLS("find_server_calls", "Find Server Calls", BridgeSection.AUDIT, "server_calls_report.txt"),
    FIND_SAVE_PATHS("find_save_paths", "Find Save Code Paths", BridgeSection.AUDIT, "save_paths_report.txt"),
    FIND_ROOT_MANAGER("find_root_manager", "Find Root Manager Code", BridgeSection.AUDIT, "root_manager_report.txt"),
    FIND_HACKING_LABELS("find_hacking_labels", "Find Hacking Labels", BridgeSection.AUDIT, "hacking_labels_report.txt"),
    FIND_TODOS("find_todos", "Find TODO/FIXME", BridgeSection.AUDIT, "todos_report.txt"),
    FIND_ANDROID_PERMISSIONS("find_android_permissions", "Find Android Permissions", BridgeSection.AUDIT, "android_permissions_report.txt"),

    CREATE_DEBUG_ZIP("create_debug_zip", "Create Debug Zip", BridgeSection.DEBUG, "debug_bundle_report.txt");

    companion object {
        fun fromId(id: String): BridgeAction? = entries.firstOrNull { it.id == id }
        fun forSection(section: BridgeSection): List<BridgeAction> = entries.filter { it.section == section }
    }
}
