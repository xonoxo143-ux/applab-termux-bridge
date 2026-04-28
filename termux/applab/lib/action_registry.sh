#!/data/data/com.termux/files/usr/bin/bash

write_action_registry() {
  REGISTRY_FILE="$CONFIG_DIR/actions.json"
  BACKEND_COMMIT="unknown"
  if [ -d "$BRIDGE_LOCAL_DIR/.git" ]; then
    BACKEND_COMMIT="$(git -C "$BRIDGE_LOCAL_DIR" rev-parse --short HEAD 2>/dev/null || echo unknown)"
  fi
  GENERATED_AT="$(date -Iseconds)"

  python3 - "$REGISTRY_FILE" "$GENERATED_AT" "$BRIDGE_REPO" "$BACKEND_COMMIT" <<'PY'
import json
import os
import sys

registry_path, generated_at, backend_repo, backend_commit = sys.argv[1:]

def action(
    id,
    label,
    group,
    description,
    risk="safe",
    visible=True,
    confirm=False,
    sort=100,
    requires_repo=False,
    requires_clean_tree=False,
    requires_patch_file=False,
    requires_network=False,
    advanced=False,
    parked=False,
):
    return {
        "id": id,
        "label": label,
        "group": group,
        "description": description,
        "risk": risk,
        "visible_by_default": visible,
        "confirm": confirm,
        "sort": sort,
        "requires_repo": requires_repo,
        "requires_clean_tree": requires_clean_tree,
        "requires_patch_file": requires_patch_file,
        "requires_network": requires_network,
        "advanced": advanced,
        "parked": parked,
    }

actions = [
    action("check_setup", "Run Termux Setup Check", "Setup", "Verify backend files, shared folder, tools, and GitHub auth.", sort=10),
    action("update_dispatcher", "Update Termux Dispatcher", "Setup", "Pull the bridge repo and reinstall live Termux backend files.", risk="mutating", confirm=True, requires_network=True, sort=20),
    action("list_actions", "List Backend Actions", "Setup", "Write the backend action registry and readable action report.", sort=30),

    action("list_projects", "Scan ~/projects for Repos", "Repo Workbench", "List git repositories found under the Termux projects folder.", sort=10),
    action("clone_bridge", "Clone Bridge Repo", "Repo Workbench", "Clone or repair the AppLab Bridge repo under ~/projects.", risk="mutating", confirm=True, requires_network=True, sort=20),
    action("clone_libreseed", "Clone LibreSeed Repo", "Repo Workbench", "Clone or repair the LibreSeed repo under ~/projects.", risk="mutating", confirm=True, requires_network=True, sort=30),
    action("show_active_repo", "Show Selected Repo", "Repo Workbench", "Show the configured and resolved active repo.", sort=40),
    action("set_active_bridge", "Select Bridge Repo", "Repo Workbench", "Set AppLab Bridge as the active repo.", risk="mutating", confirm=False, sort=50),
    action("set_active_libreseed", "Select LibreSeed Repo", "Repo Workbench", "Set LibreSeed as the active repo.", risk="mutating", confirm=False, sort=60),
    action("check_repo", "Check Selected Repo", "Repo Workbench", "Run git status for the selected repo.", requires_repo=True, sort=70),
    action("show_status", "Run Git Status", "Repo Workbench", "Show branch and full git status for the selected repo.", requires_repo=True, sort=80),
    action("fetch_repo", "Fetch Remotes", "Repo Workbench", "Fetch remotes and prune stale refs.", risk="network", requires_repo=True, requires_network=True, sort=90),
    action("pull_current", "Pull Current Branch", "Repo Workbench", "Pull the current branch with fast-forward only.", risk="mutating", requires_repo=True, requires_network=True, confirm=True, sort=100),
    action("show_branches", "List Branches", "Repo Workbench", "Show local and remote branches.", requires_repo=True, sort=110),
    action("checkout_staging", "Checkout Staging Branch", "Repo Workbench", "Switch the selected repo to staging.", risk="mutating", requires_repo=True, requires_clean_tree=True, confirm=True, sort=120),
    action("pull_staging", "Checkout + Pull Staging", "Repo Workbench", "Switch to staging and pull fast-forward only.", risk="mutating", requires_repo=True, requires_clean_tree=True, requires_network=True, confirm=True, sort=130),
    action("show_current_commit", "Show Current Commit", "Repo Workbench", "Show current branch and latest commit.", requires_repo=True, sort=140),
    action("list_changed_files", "List Changed Files", "Repo Workbench", "List changed and diffed files.", requires_repo=True, sort=150),
    action("show_diff_summary", "Show Diff Summary", "Repo Workbench", "Show diff stat and diff check output.", requires_repo=True, sort=160),
    action("show_full_diff", "Write Full Diff Report", "Repo Workbench", "Write a full diff report for review.", requires_repo=True, sort=170),

    action("run_patch_script", "Run patch.sh", "Patch Runner", "Run Documents/AppLabBridge/patches/patch.sh against the selected repo.", risk="mutating", requires_repo=True, requires_patch_file=True, confirm=True, sort=10),
    action("stage_all", "Stage All Changes", "Patch Runner", "Stage every changed file in the selected repo.", risk="mutating", requires_repo=True, confirm=True, sort=20),
    action("commit_no_apk", "Commit Staged Changes [no apk]", "Patch Runner", "Create a commit and append [no apk] when needed.", risk="publishing", requires_repo=True, confirm=True, sort=30),
    action("push_current", "Push Current Branch", "Patch Runner", "Push the selected repo's current branch to GitHub.", risk="publishing", requires_repo=True, requires_network=True, confirm=True, sort=40),

    action("check_latest_apk", "Check GitHub APK Artifact", "Build / APK", "Find the latest successful GitHub APK artifact.", risk="network", requires_network=True, sort=10),
    action("download_latest_apk", "Download GitHub APK Artifact", "Build / APK", "Download the latest debug APK artifact into the shared folder.", risk="install", requires_network=True, confirm=True, sort=20),

    action("create_debug_zip", "Create Debug Zip", "Results", "Create a debug bundle of reports, logs, config, and result files.", sort=10),

    action("decode_save", "Decode Clipboard Save", "Save Tools", "Parked save-code decoder action.", risk="experimental", visible=False, advanced=True, parked=True, sort=10),
    action("validate_save", "Validate Save", "Save Tools", "Parked save-code validation action.", risk="experimental", visible=False, advanced=True, parked=True, sort=20),

    action("find_server_calls", "Find Server Calls", "Source Audits", "Parked source audit for server/API calls.", risk="experimental", visible=False, advanced=True, parked=True, requires_repo=True, sort=10),
    action("find_save_paths", "Find Save Code Paths", "Source Audits", "Parked source audit for save-code paths.", risk="experimental", visible=False, advanced=True, parked=True, requires_repo=True, sort=20),
    action("find_root_manager", "Find Root Manager Code", "Source Audits", "Parked source audit for root manager code.", risk="experimental", visible=False, advanced=True, parked=True, requires_repo=True, sort=30),
    action("find_hacking_labels", "Find Labels", "Source Audits", "Parked source audit for labels.", risk="experimental", visible=False, advanced=True, parked=True, requires_repo=True, sort=40),
    action("find_todos", "Find TODO/FIXME", "Source Audits", "Parked source audit for TODO and FIXME comments.", risk="experimental", visible=False, advanced=True, parked=True, requires_repo=True, sort=50),
    action("find_android_permissions", "Find Android Permissions", "Source Audits", "Parked source audit for Android permissions.", risk="experimental", visible=False, advanced=True, parked=True, requires_repo=True, sort=60),
]

registry = {
    "schema_version": 1,
    "generated_at": generated_at,
    "backend_repo": backend_repo,
    "backend_commit": backend_commit,
    "groups": [
        "Setup",
        "Repo Workbench",
        "Patch Runner",
        "Build / APK",
        "Results",
        "Save Tools",
        "Source Audits",
    ],
    "risk_levels": ["safe", "network", "mutating", "publishing", "install", "experimental"],
    "actions": actions,
}

ids = [item["id"] for item in actions]
if len(ids) != len(set(ids)):
    raise SystemExit("duplicate action id in registry")

os.makedirs(os.path.dirname(registry_path), exist_ok=True)
with open(registry_path, "w", encoding="utf-8") as f:
    json.dump(registry, f, indent=2)
PY
}

print_action_registry_report() {
  REGISTRY_FILE="$CONFIG_DIR/actions.json"
  if [ ! -f "$REGISTRY_FILE" ]; then
    write_action_registry
  fi
  python3 - "$REGISTRY_FILE" <<'PY'
import json
import sys

path = sys.argv[1]
with open(path, "r", encoding="utf-8") as f:
    data = json.load(f)
print("Backend Action Registry")
print(f"Schema: {data.get('schema_version')}")
print(f"Generated: {data.get('generated_at')}")
print(f"Backend repo: {data.get('backend_repo')}")
print(f"Backend commit: {data.get('backend_commit')}")
print()
for group in data.get("groups", []):
    actions = [a for a in data.get("actions", []) if a.get("group") == group]
    if not actions:
        continue
    print(group)
    for item in sorted(actions, key=lambda a: a.get("sort", 100)):
        flags = []
        if item.get("confirm"):
            flags.append("confirm")
        if item.get("requires_repo"):
            flags.append("repo")
        if item.get("requires_clean_tree"):
            flags.append("clean-tree")
        if item.get("requires_patch_file"):
            flags.append("patch-file")
        if item.get("requires_network"):
            flags.append("network")
        if item.get("advanced"):
            flags.append("advanced")
        if item.get("parked"):
            flags.append("parked")
        flag_text = f" [{' '.join(flags)}]" if flags else ""
        print(f"  - {item['id']}: {item['label']} ({item['risk']}){flag_text}")
    print()
print(f"Total actions: {len(data.get('actions', []))}")
print(f"Registry file: {path}")
PY
}
