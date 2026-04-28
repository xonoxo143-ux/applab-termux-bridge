package com.applab.termuxbridge.bridge

enum class BridgeAction(
    val id: String,
    val label: String,
    val section: BridgeSection,
    val expectedReportName: String
) {
    CHECK_SETUP("check_setup", "Run Termux Setup Check", BridgeSection.SETUP, "check_setup.txt"),
    LIST_ACTIONS("list_actions", "List Backend Actions", BridgeSection.SETUP, "list_actions.txt"),
    UPDATE_DISPATCHER("update_dispatcher", "Update Termux Dispatcher", BridgeSection.SETUP, "update_dispatcher.txt"),

    LIST_PROJECTS("list_projects", "Scan ~/projects for Repos", BridgeSection.REPO, "list_projects.txt"),
    CLONE_LIBRESEED("clone_libreseed", "Clone LibreSeed Repo", BridgeSection.REPO, "clone_libreseed.txt"),
    CLONE_BRIDGE("clone_bridge", "Clone Bridge Repo", BridgeSection.REPO, "clone_bridge.txt"),
    SHOW_ACTIVE_REPO("show_active_repo", "Show Selected Repo", BridgeSection.REPO, "show_active_repo.txt"),
    SET_ACTIVE_LIBRESEED("set_active_libreseed", "Select LibreSeed Repo", BridgeSection.REPO, "set_active_libreseed.txt"),
    SET_ACTIVE_BRIDGE("set_active_bridge", "Select Bridge Repo", BridgeSection.REPO, "set_active_bridge.txt"),
    CHECK_REPO("check_repo", "Check Selected Repo", BridgeSection.REPO, "check_repo.txt"),
    SHOW_STATUS("show_status", "Run Git Status", BridgeSection.REPO, "show_status.txt"),
    FETCH_REPO("fetch_repo", "Fetch Remotes", BridgeSection.REPO, "fetch_repo.txt"),
    PULL_CURRENT("pull_current", "Pull Current Branch", BridgeSection.REPO, "pull_current.txt"),
    PULL_STAGING("pull_staging", "Checkout + Pull Staging", BridgeSection.REPO, "pull_repo.txt"),
    SHOW_BRANCHES("show_branches", "List Branches", BridgeSection.REPO, "show_branches.txt"),
    CHECKOUT_STAGING("checkout_staging", "Checkout Staging Branch", BridgeSection.REPO, "checkout_staging.txt"),
    SHOW_CURRENT_COMMIT("show_current_commit", "Show Current Commit", BridgeSection.REPO, "show_current_commit.txt"),
    LIST_CHANGED_FILES("list_changed_files", "List Changed Files", BridgeSection.REPO, "list_changed_files.txt"),
    SHOW_DIFF_SUMMARY("show_diff_summary", "Show Diff Summary", BridgeSection.REPO, "show_diff_summary.txt"),
    SHOW_FULL_DIFF("show_full_diff", "Write Full Diff Report", BridgeSection.REPO, "show_full_diff.txt"),
    RUN_PATCH_SCRIPT("run_patch_script", "Run patch.sh", BridgeSection.REPO, "run_patch_script.txt"),
    STAGE_ALL("stage_all", "Stage All Changes", BridgeSection.REPO, "stage_all.txt"),
    COMMIT_NO_APK("commit_no_apk", "Commit Staged Changes [no apk]", BridgeSection.REPO, "commit_no_apk.txt"),
    PUSH_CURRENT("push_current", "Push Current Branch", BridgeSection.REPO, "push_current.txt"),

    CHECK_LATEST_APK("check_latest_apk", "Check GitHub APK Artifact", BridgeSection.APK, "check_latest_apk.txt"),
    DOWNLOAD_LATEST_APK("download_latest_apk", "Download GitHub APK Artifact", BridgeSection.APK, "download_latest_apk.txt"),

    DECODE_SAVE("decode_save", "Decode Clipboard Save", BridgeSection.SAVE, "save_report.txt"),
    VALIDATE_SAVE("validate_save", "Validate Save", BridgeSection.SAVE, "save_report.txt"),

    FIND_SERVER_CALLS("find_server_calls", "Find Server Calls", BridgeSection.AUDIT, "server_calls_report.txt"),
    FIND_SAVE_PATHS("find_save_paths", "Find Save Code Paths", BridgeSection.AUDIT, "save_paths_report.txt"),
    FIND_ROOT_MANAGER("find_root_manager", "Find Root Manager Code", BridgeSection.AUDIT, "root_manager_report.txt"),
    FIND_HACKING_LABELS("find_hacking_labels", "Find Labels", BridgeSection.AUDIT, "labels_report.txt"),
    FIND_TODOS("find_todos", "Find TODO/FIXME", BridgeSection.AUDIT, "todos_report.txt"),
    FIND_ANDROID_PERMISSIONS("find_android_permissions", "Find Android Permissions", BridgeSection.AUDIT, "android_permissions_report.txt"),

    CREATE_DEBUG_ZIP("create_debug_zip", "Create Debug Zip", BridgeSection.DEBUG, "debug_bundle_report.txt");

    companion object {
        fun fromId(id: String): BridgeAction? = entries.firstOrNull { it.id == id }
        fun forSection(section: BridgeSection): List<BridgeAction> = entries.filter { it.section == section }
    }
}
