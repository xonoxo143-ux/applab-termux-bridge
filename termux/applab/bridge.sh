#!/data/data/com.termux/files/usr/bin/bash
set -u

ACTION="${1:-check_setup}"
HOME_DIR="/data/data/com.termux/files/home"
PROJECTS_DIR="$HOME_DIR/projects"
SHARED_DIR="$HOME_DIR/storage/shared/Documents/AppLabBridge"
RESULTS_DIR="$SHARED_DIR/results"
REPORTS_DIR="$SHARED_DIR/reports"
LOGS_DIR="$SHARED_DIR/logs"
CONFIG_DIR="$SHARED_DIR/config"
APKS_DIR="$SHARED_DIR/apks"
RUN_ID="$(date +%Y%m%d_%H%M%S)_$$"
LOG_FILE="$LOGS_DIR/${RUN_ID}_${ACTION}.log"
BRIDGE_REPO="xonoxo143-ux/applab-termux-bridge"
BRIDGE_WORKFLOW="Debug APK"
BRIDGE_ARTIFACT="applab-termux-bridge-debug-apk"
BRIDGE_LOCAL_DIR="$PROJECTS_DIR/applab-termux-bridge"
LIVE_DISPATCHER="$HOME_DIR/.termux/applab/bridge.sh"

mkdir -p "$RESULTS_DIR" "$REPORTS_DIR" "$LOGS_DIR" "$CONFIG_DIR" "$PROJECTS_DIR" "$APKS_DIR" "$HOME_DIR/.termux/applab"

repo_json_fields() {
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    REPO_PATH="$(pwd)"
    REPO_NAME="$(basename "$REPO_PATH")"
    BRANCH="$(git branch --show-current 2>/dev/null || true)"
    CHANGED_FILES="$(git status --short 2>/dev/null | wc -l | tr -d ' ')"
    if [ "${CHANGED_FILES:-0}" -gt 0 ]; then
      DIRTY="True"
    else
      DIRTY="False"
    fi
    AHEAD="0"
    BEHIND="0"
    if git rev-parse --abbrev-ref --symbolic-full-name '@{u}' >/dev/null 2>&1; then
      COUNTS="$(git rev-list --left-right --count HEAD...@{u} 2>/dev/null || echo '0 0')"
      AHEAD="$(printf '%s' "$COUNTS" | awk '{print $1}')"
      BEHIND="$(printf '%s' "$COUNTS" | awk '{print $2}')"
    fi
    printf ',\n  "repo_path": "%s",\n  "repo_name": "%s",\n  "branch": "%s",\n  "dirty": %s,\n  "changed_files": %s,\n  "ahead": %s,\n  "behind": %s' "$REPO_PATH" "$REPO_NAME" "$BRANCH" "$DIRTY" "${CHANGED_FILES:-0}" "${AHEAD:-0}" "${BEHIND:-0}"
  fi
}

write_result() {
  STATUS="$1"
  TITLE="$2"
  SUMMARY="$3"
  EXIT_CODE="$4"
  REPORT_FILE="${5:-}"
  EXTRA_FIELDS="$(repo_json_fields || true)"

  python3 - "$RESULTS_DIR/latest_result.json" <<PY
import json, sys, os
path = sys.argv[1]
data = {
  "schema_version": 1,
  "run_id": "$RUN_ID",
  "action": "$ACTION",
  "status": "$STATUS",
  "title": "$TITLE",
  "summary": "$SUMMARY",
  "exit_code": int("$EXIT_CODE"),
  "report_file": "$REPORT_FILE",
  "log_file": "$LOG_FILE",
  "next_action": "",
  "artifacts": []$EXTRA_FIELDS
}
os.makedirs(os.path.dirname(path), exist_ok=True)
with open(path, "w", encoding="utf-8") as f:
  json.dump(data, f, indent=2)
PY
}

run_report() {
  REPORT_NAME="$1"
  shift
  REPORT_FILE="$REPORTS_DIR/$REPORT_NAME"
  {
    echo "Action: $ACTION"
    echo "Run: $RUN_ID"
    echo "Time: $(date)"
    echo
    "$@"
  } > "$REPORT_FILE" 2>&1
  CODE=$?
  cat "$REPORT_FILE" > "$LOG_FILE" 2>/dev/null || true
  if [ "$CODE" -eq 0 ]; then
    write_result "success" "Action completed" "Report written: $REPORT_NAME" "$CODE" "$REPORT_FILE"
  else
    write_result "failed" "Action failed" "Exit $CODE. See report/log." "$CODE" "$REPORT_FILE"
  fi
  exit "$CODE"
}

active_repo() {
  if [ -f "$CONFIG_DIR/active_repo.txt" ]; then
    REPO="$(cat "$CONFIG_DIR/active_repo.txt" | head -n 1)"
    if [ -n "$REPO" ] && [ -d "$REPO/.git" ]; then
      printf '%s\n' "$REPO"
      return 0
    fi
  fi

  if [ -d "$PROJECTS_DIR/libreseed-labs-android/.git" ]; then
    printf '%s\n' "$PROJECTS_DIR/libreseed-labs-android"
    return 0
  fi

  if [ -d "$PROJECTS_DIR/applab-termux-bridge/.git" ]; then
    printf '%s\n' "$PROJECTS_DIR/applab-termux-bridge"
    return 0
  fi

  return 1
}

need_repo() {
  REPO="$(active_repo || true)"
  if [ -z "${REPO:-}" ]; then
    write_result "failed" "No active repo" "Clone a repo or set config/active_repo.txt." 1 ""
    exit 1
  fi
  cd "$REPO" || exit 1
}

latest_bridge_run_id() {
  gh run list --repo "$BRIDGE_REPO" --workflow "$BRIDGE_WORKFLOW" --status success --limit 1 --json databaseId --jq '.[0].databaseId'
}

case "$ACTION" in
  check_setup)
    REPORT_FILE="$REPORTS_DIR/check_setup.txt"
    {
      echo "AppLab Termux Bridge setup check"
      echo
      echo "Termux home: $HOME_DIR"
      echo "Projects dir: $PROJECTS_DIR"
      echo "Shared dir: $SHARED_DIR"
      echo "Live dispatcher: $LIVE_DISPATCHER"
      echo
      echo "Tools:"
      command -v git || true
      command -v gh || true
      command -v python3 || true
      command -v zip || true
      echo
      echo "GitHub auth:"
      gh auth status 2>&1 || true
    } > "$REPORT_FILE" 2>&1
    cat "$REPORT_FILE" > "$LOG_FILE"
    write_result "success" "Setup checked" "Setup report written." 0 "$REPORT_FILE"
    ;;

  update_dispatcher)
    REPORT_FILE="$REPORTS_DIR/update_dispatcher.txt"
    {
      echo "Updating AppLab Bridge dispatcher from GitHub"
      echo "Repo: $BRIDGE_REPO"
      echo "Local repo: $BRIDGE_LOCAL_DIR"
      echo "Live dispatcher: $LIVE_DISPATCHER"
      echo
      cd "$PROJECTS_DIR"
      if [ ! -d "$BRIDGE_LOCAL_DIR/.git" ]; then
        echo "Bridge repo not found. Cloning."
        gh repo clone "$BRIDGE_REPO" applab-termux-bridge
      fi
      cd "$BRIDGE_LOCAL_DIR"
      git checkout main
      git pull --ff-only
      if [ ! -f termux/applab/bridge.sh ]; then
        echo "Repo dispatcher file missing: termux/applab/bridge.sh"
        exit 1
      fi
      mkdir -p "$(dirname "$LIVE_DISPATCHER")"
      cp termux/applab/bridge.sh "$LIVE_DISPATCHER"
      chmod +x "$LIVE_DISPATCHER"
      echo
      echo "Dispatcher updated."
      ls -l "$LIVE_DISPATCHER"
      echo
      echo "Current bridge repo commit:"
      git log -1 --oneline
    } > "$REPORT_FILE" 2>&1
    CODE=$?
    cat "$REPORT_FILE" > "$LOG_FILE"
    cd "$BRIDGE_LOCAL_DIR" 2>/dev/null || true
    if [ "$CODE" -eq 0 ]; then
      write_result "success" "Dispatcher updated" "Termux dispatcher copied from latest bridge repo." "$CODE" "$REPORT_FILE"
    else
      write_result "failed" "Dispatcher update failed" "Exit $CODE. See report/log." "$CODE" "$REPORT_FILE"
    fi
    exit "$CODE"
    ;;

  list_projects)
    REPORT_FILE="$REPORTS_DIR/list_projects.txt"
    {
      echo "Projects under $PROJECTS_DIR"
      echo
      cd "$PROJECTS_DIR" || exit 1
      find . -maxdepth 2 -name .git -type d | sed 's#^./##; s#/.git$##' | sort
    } > "$REPORT_FILE" 2>&1
    CODE=$?
    cat "$REPORT_FILE" > "$LOG_FILE"
    if [ "$CODE" -eq 0 ]; then
      write_result "success" "Projects listed" "Project list report written." "$CODE" "$REPORT_FILE"
    else
      write_result "failed" "Could not list projects" "See report/log." "$CODE" "$REPORT_FILE"
    fi
    exit "$CODE"
    ;;

  show_active_repo)
    REPORT_FILE="$REPORTS_DIR/show_active_repo.txt"
    {
      echo "Active repo config:"
      if [ -f "$CONFIG_DIR/active_repo.txt" ]; then
        cat "$CONFIG_DIR/active_repo.txt"
      else
        echo "No active_repo.txt set."
      fi
      echo
      echo "Resolved active repo:"
      active_repo || true
    } > "$REPORT_FILE" 2>&1
    cat "$REPORT_FILE" > "$LOG_FILE"
    need_repo
    write_result "success" "Active repo checked" "Active repo report written." 0 "$REPORT_FILE"
    ;;

  set_active_libreseed)
    TARGET="$PROJECTS_DIR/libreseed-labs-android"
    if [ ! -d "$TARGET/.git" ]; then
      write_result "failed" "LibreSeed repo missing" "Clone LibreSeed before selecting it." 1 ""
      exit 1
    fi
    printf '%s\n' "$TARGET" > "$CONFIG_DIR/active_repo.txt"
    REPORT_FILE="$REPORTS_DIR/set_active_libreseed.txt"
    {
      echo "Active repo set to:"
      cat "$CONFIG_DIR/active_repo.txt"
    } > "$REPORT_FILE" 2>&1
    cat "$REPORT_FILE" > "$LOG_FILE"
    cd "$TARGET" || exit 1
    write_result "success" "Active repo set" "LibreSeed is now active." 0 "$REPORT_FILE"
    ;;

  set_active_bridge)
    TARGET="$PROJECTS_DIR/applab-termux-bridge"
    if [ ! -d "$TARGET/.git" ]; then
      write_result "failed" "Bridge repo missing" "Clone the bridge repo before selecting it." 1 ""
      exit 1
    fi
    printf '%s\n' "$TARGET" > "$CONFIG_DIR/active_repo.txt"
    REPORT_FILE="$REPORTS_DIR/set_active_bridge.txt"
    {
      echo "Active repo set to:"
      cat "$CONFIG_DIR/active_repo.txt"
    } > "$REPORT_FILE" 2>&1
    cat "$REPORT_FILE" > "$LOG_FILE"
    cd "$TARGET" || exit 1
    write_result "success" "Active repo set" "Bridge repo is now active." 0 "$REPORT_FILE"
    ;;

  check_repo|show_status)
    need_repo
    run_report "${ACTION}.txt" bash -lc "pwd && git branch --show-current && echo && git status --short && echo && git status"
    ;;

  pull_current)
    need_repo
    run_report "pull_current.txt" git pull --ff-only
    ;;

  pull_staging)
    need_repo
    run_report "pull_repo.txt" bash -lc "git checkout staging && git pull --ff-only"
    ;;

  checkout_staging)
    need_repo
    run_report "checkout_staging.txt" bash -lc "git checkout staging && git status"
    ;;

  run_patch_script)
    need_repo
    PATCHES_DIR="$SHARED_DIR/patches"
    SCRIPT="$PATCHES_DIR/patch.sh"
    if [ ! -f "$SCRIPT" ]; then
      write_result "failed" "No patch script" "Put a script at Documents/AppLabBridge/patches/patch.sh." 1 ""
      exit 1
    fi
    chmod +x "$SCRIPT"
    run_report "run_patch_script.txt" bash -lc "cd \"$(pwd)\" && bash \"$SCRIPT\" && echo && git status --short"
    ;;

  stage_all)
    need_repo
    run_report "stage_all.txt" bash -lc "git status --short && git add -A && echo && git status --short"
    ;;

  commit_no_apk)
    need_repo
    MSG_FILE="$CONFIG_DIR/commit_message.txt"
    if [ -f "$MSG_FILE" ]; then
      MSG="$(cat "$MSG_FILE" | head -n 1)"
    else
      MSG="AppLab bridge update [no apk]"
    fi
    case "$MSG" in
      *"[no apk]"*|*"[skip apk]"*) ;;
      *) MSG="$MSG [no apk]" ;;
    esac
    run_report "commit_no_apk.txt" bash -lc "git status --short && git commit -m \"$MSG\" && git log -1 --oneline"
    ;;

  push_current)
    need_repo
    run_report "push_current.txt" git push
    ;;

  show_current_commit)
    need_repo
    run_report "show_current_commit.txt" bash -lc "git branch --show-current && git log -1 --oneline --decorate"
    ;;

  list_changed_files)
    need_repo
    run_report "list_changed_files.txt" bash -lc "git status --short && echo && git diff --name-status"
    ;;

  show_diff_summary)
    need_repo
    run_report "show_diff_summary.txt" bash -lc "git diff --stat && echo && git diff --check"
    ;;

  show_branches)
    need_repo
    run_report "show_branches.txt" bash -lc "git branch -vv && echo && git branch -r"
    ;;

  fetch_repo)
    need_repo
    run_report "fetch_repo.txt" git fetch --all --prune
    ;;

  check_latest_apk)
    REPORT_FILE="$REPORTS_DIR/check_latest_apk.txt"
    {
      echo "GitHub debug APK source"
      echo "Repo: $BRIDGE_REPO"
      echo "Workflow: $BRIDGE_WORKFLOW"
      echo "Artifact: $BRIDGE_ARTIFACT"
      echo
      echo "Latest successful workflow run:"
      gh run list --repo "$BRIDGE_REPO" --workflow "$BRIDGE_WORKFLOW" --status success --limit 3 || true
      echo
      echo "Local APKs:"
      find "$APKS_DIR" -name '*.apk' -type f 2>/dev/null | sort | tail -n 20
    } > "$REPORT_FILE" 2>&1
    CODE=$?
    cat "$REPORT_FILE" > "$LOG_FILE"
    if [ "$CODE" -eq 0 ]; then
      write_result "success" "APK update checked" "Latest APK report written." "$CODE" "$REPORT_FILE"
    else
      write_result "failed" "APK update check failed" "See report/log." "$CODE" "$REPORT_FILE"
    fi
    exit "$CODE"
    ;;

  download_latest_apk)
    REPORT_FILE="$REPORTS_DIR/download_latest_apk.txt"
    TMP_DIR="$SHARED_DIR/tmp_apk_download_$RUN_ID"
    {
      echo "Downloading latest AppLab Bridge debug APK"
      echo "Repo: $BRIDGE_REPO"
      echo "Workflow: $BRIDGE_WORKFLOW"
      echo "Artifact: $BRIDGE_ARTIFACT"
      echo
      RUN_DATABASE_ID="$(latest_bridge_run_id)"
      if [ -z "$RUN_DATABASE_ID" ] || [ "$RUN_DATABASE_ID" = "null" ]; then
        echo "No successful workflow run found."
        exit 1
      fi
      echo "Run database id: $RUN_DATABASE_ID"
      rm -rf "$TMP_DIR"
      mkdir -p "$TMP_DIR" "$APKS_DIR"
      gh run download "$RUN_DATABASE_ID" --repo "$BRIDGE_REPO" --name "$BRIDGE_ARTIFACT" --dir "$TMP_DIR"
      APK_FILE="$(find "$TMP_DIR" -name '*.apk' -type f | head -n 1)"
      if [ -z "$APK_FILE" ]; then
        echo "Artifact downloaded, but no APK file was found."
        exit 1
      fi
      OUT_FILE="$APKS_DIR/AppLabBridge-latest-${RUN_DATABASE_ID}.apk"
      cp "$APK_FILE" "$OUT_FILE"
      rm -rf "$TMP_DIR"
      echo "Downloaded APK: $OUT_FILE"
      ls -lh "$OUT_FILE"
    } > "$REPORT_FILE" 2>&1
    CODE=$?
    cat "$REPORT_FILE" > "$LOG_FILE"
    if [ "$CODE" -eq 0 ]; then
      write_result "success" "APK downloaded" "Latest AppLab Bridge APK downloaded to apks/." "$CODE" "$REPORT_FILE"
    else
      write_result "failed" "APK download failed" "Exit $CODE. See report/log." "$CODE" "$REPORT_FILE"
    fi
    exit "$CODE"
    ;;

  *)
    write_result "failed" "Unknown action" "Unknown action: $ACTION" 2 ""
    exit 2
    ;;
esac
