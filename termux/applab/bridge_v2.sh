#!/data/data/com.termux/files/usr/bin/bash
set -u

ACTION="${1:-check_setup}"
HOME_DIR="/data/data/com.termux/files/home"
PROJECTS_DIR="$HOME_DIR/projects"
SHARED_DIR="$HOME_DIR/storage/shared/Documents/AppLabBridge"
RESULTS_DIR="$SHARED_DIR/results"
RESULT_HISTORY_DIR="$RESULTS_DIR/history"
REPORTS_DIR="$SHARED_DIR/reports"
LOGS_DIR="$SHARED_DIR/logs"
CONFIG_DIR="$SHARED_DIR/config"
APKS_DIR="$SHARED_DIR/apks"
STARTED_AT="$(date -Iseconds)"
RUN_ID="$(date +%Y%m%d_%H%M%S)_$$"
LOG_FILE="$LOGS_DIR/${RUN_ID}_${ACTION}.log"
BRIDGE_REPO="xonoxo143-ux/applab-termux-bridge"
BRIDGE_WORKFLOW="Debug APK"
BRIDGE_ARTIFACT="applab-termux-bridge-debug-apk"
BRIDGE_LOCAL_DIR="$PROJECTS_DIR/applab-termux-bridge"
LIVE_DISPATCHER="$HOME_DIR/.termux/applab/bridge_v2.sh"
LIB_DIR="$HOME_DIR/.termux/applab/lib"

mkdir -p "$RESULTS_DIR" "$RESULT_HISTORY_DIR" "$REPORTS_DIR" "$LOGS_DIR" "$CONFIG_DIR" "$PROJECTS_DIR" "$APKS_DIR" "$HOME_DIR/.termux/applab" "$LIB_DIR"

source_helper() {
  HELPER_NAME="$1"
  if [ -f "$LIB_DIR/$HELPER_NAME" ]; then
    . "$LIB_DIR/$HELPER_NAME"
  elif [ -f "$BRIDGE_LOCAL_DIR/termux/applab/lib/$HELPER_NAME" ]; then
    . "$BRIDGE_LOCAL_DIR/termux/applab/lib/$HELPER_NAME"
  else
    return 1
  fi
}

bounded_cmd() {
  LIMIT_SECONDS="$1"
  shift
  if command -v timeout >/dev/null 2>&1; then
    timeout "$LIMIT_SECONDS" "$@"
  else
    "$@"
  fi
}

source_helper repo_state.sh || repo_json_fields() { printf '{}'; }
source_helper result_writer.sh || { write_result() { echo "Result writer missing" >&2; exit 1; }; run_report() { echo "Result writer missing" >&2; exit 1; }; }
source_helper apk_actions.sh || true
source_helper utility_actions.sh || true
source_helper parked_actions.sh || true
source_helper action_registry.sh || true

active_repo() {
  if [ -f "$CONFIG_DIR/active_repo.txt" ]; then
    REPO="$(cat "$CONFIG_DIR/active_repo.txt" | head -n 1)"
    if [ -n "$REPO" ] && [ -d "$REPO/.git" ]; then printf '%s\n' "$REPO"; return 0; fi
  fi
  if [ -d "$PROJECTS_DIR/libreseed-labs-android/.git" ]; then printf '%s\n' "$PROJECTS_DIR/libreseed-labs-android"; return 0; fi
  if [ -d "$PROJECTS_DIR/applab-termux-bridge/.git" ]; then printf '%s\n' "$PROJECTS_DIR/applab-termux-bridge"; return 0; fi
  return 1
}

need_repo() {
  REPO="$(active_repo || true)"
  if [ -z "${REPO:-}" ]; then
    write_result "failed" "No selected repo" "No active repo was found." 1 "" "Choose a repo first, or clone one under ~/projects."
    exit 1
  fi
  cd "$REPO" || exit 1
}

latest_bridge_run_id() {
  gh run list --repo "$BRIDGE_REPO" --workflow "$BRIDGE_WORKFLOW" --status success --limit 1 --json databaseId --jq '.[0].databaseId'
}

safe_repo_dir_name() {
  NAME="$1"
  printf '%s\n' "$NAME" | sed 's#^.*/##; s#[^A-Za-z0-9._-]#-#g'
}

if command -v handle_utility_action >/dev/null 2>&1; then handle_utility_action || true; fi
if command -v handle_apk_action >/dev/null 2>&1; then handle_apk_action || true; fi
if command -v handle_parked_action >/dev/null 2>&1; then handle_parked_action || true; fi

case "$ACTION" in
  check_setup)
    REPORT_FILE="$REPORTS_DIR/check_setup.txt"
    {
      echo "AppLab Termux Bridge setup check"
      echo "Run: $RUN_ID"
      echo "Started: $STARTED_AT"
      echo
      echo "Termux home: $HOME_DIR"
      echo "Projects dir: $PROJECTS_DIR"
      echo "Shared dir: $SHARED_DIR"
      echo "Live dispatcher: $LIVE_DISPATCHER"
      echo "Lib dir: $LIB_DIR"
      echo
      echo "Live backend files:"
      ls -l "$HOME_DIR/.termux/applab" 2>&1 || true
      ls -l "$LIB_DIR" 2>&1 || true
      echo
      echo "Tools:"
      command -v git || echo "git missing"
      command -v gh || echo "gh missing"
      command -v python3 || echo "python3 missing"
      command -v zip || echo "zip missing"
      command -v timeout || echo "timeout missing; bounded checks may run unbounded"
      echo
      echo "GitHub auth status, bounded:"
      if command -v gh >/dev/null 2>&1; then
        bounded_cmd 8s gh auth status 2>&1 || echo "gh auth status unavailable, timed out, or returned nonzero"
      else
        echo "gh missing"
      fi
      echo
      echo "Setup check reached result writer."
    } > "$REPORT_FILE" 2>&1
    CODE=$?
    cat "$REPORT_FILE" > "$LOG_FILE" 2>/dev/null || true
    if [ "$CODE" -eq 0 ]; then
      write_result "success" "Setup checked" "Setup report written." "$CODE" "$REPORT_FILE" "Backend setup completed. If app still times out, inspect android_app_bridge.log and Termux pending-intent result logs."
    else
      write_result "failed" "Setup check failed" "Exit $CODE. See report/log." "$CODE" "$REPORT_FILE" "Setup report failed before completion. Inspect the report for the last completed section."
    fi
    exit "$CODE"
    ;;

  list_actions)
    REPORT_FILE="$REPORTS_DIR/list_actions.txt"
    if ! command -v write_action_registry >/dev/null 2>&1 || ! command -v print_action_registry_report >/dev/null 2>&1; then
      write_result "failed" "Action registry unavailable" "action_registry.sh was not loaded." 1 "" "Bootstrap or update the dispatcher so action_registry.sh is installed in ~/.termux/applab/lib."
      exit 1
    fi
    { write_action_registry; print_action_registry_report; } > "$REPORT_FILE" 2>&1
    CODE=$?
    cat "$REPORT_FILE" > "$LOG_FILE" 2>/dev/null || true
    if [ "$CODE" -eq 0 ]; then write_result "success" "Actions listed" "Action registry written to config/actions.json." "$CODE" "$REPORT_FILE" "Android can read config/actions.json."; else write_result "failed" "Action registry failed" "Exit $CODE. See report/log." "$CODE" "$REPORT_FILE" "Open list_actions report and check JSON generation."; fi
    exit "$CODE"
    ;;

  update_dispatcher)
    REPORT_FILE="$REPORTS_DIR/update_dispatcher.txt"
    {
      echo "Updating AppLab Bridge dispatcher from GitHub"
      cd "$PROJECTS_DIR"
      if [ ! -d "$BRIDGE_LOCAL_DIR/.git" ]; then gh repo clone "$BRIDGE_REPO" applab-termux-bridge; fi
      cd "$BRIDGE_LOCAL_DIR"
      git checkout main
      git pull --ff-only
      mkdir -p "$HOME_DIR/.termux/applab/lib"
      cp termux/applab/bridge_v2.sh "$HOME_DIR/.termux/applab/bridge_v2.sh"
      cp termux/applab/lib/*.sh "$HOME_DIR/.termux/applab/lib/"
      chmod +x "$HOME_DIR/.termux/applab/bridge_v2.sh"
      chmod +x "$HOME_DIR/.termux/applab/lib/"*.sh
      echo "Dispatcher v2 updated."
      ls -l "$HOME_DIR/.termux/applab/bridge_v2.sh"
      git log -1 --oneline
    } > "$REPORT_FILE" 2>&1
    CODE=$?
    cat "$REPORT_FILE" > "$LOG_FILE"
    cd "$BRIDGE_LOCAL_DIR" 2>/dev/null || true
    if [ "$CODE" -eq 0 ]; then write_result "success" "Dispatcher updated" "Termux dispatcher v2 copied from latest bridge repo." "$CODE" "$REPORT_FILE" "Run setup check again after dispatcher updates."; else write_result "failed" "Dispatcher update failed" "Exit $CODE. See report/log." "$CODE" "$REPORT_FILE" "Check GitHub auth, network access, and whether the bridge repo has local uncommitted changes."; fi
    exit "$CODE"
    ;;

  list_projects)
    REPORT_FILE="$REPORTS_DIR/list_projects.txt"
    {
      echo "Projects under $PROJECTS_DIR"
      echo
      echo "Local repos"
      cd "$PROJECTS_DIR" || exit 1
      find . -maxdepth 2 -name .git -type d | sed 's#^./##; s#/.git$##' | sort | while read -r REL; do
        [ -z "$REL" ] && continue
        NAME="$(basename "$REL")"
        printf 'local\t%s\t%s\t%s\n' "$REL" "$PROJECTS_DIR/$REL" "$NAME"
      done
      echo
      echo "GitHub repos"
      if command -v gh >/dev/null 2>&1; then
        bounded_cmd 20s gh repo list --limit 100 --json nameWithOwner,sshUrl,url --jq '.[] | [.nameWithOwner, (.sshUrl // .url)] | @tsv' 2>/dev/null | while IFS=$'\t' read -r FULL URL; do
          [ -z "$FULL" ] && continue
          printf 'github\t%s\t%s\t%s\n' "$FULL" "$URL" "$(safe_repo_dir_name "$FULL")"
        done || echo "gh repo list unavailable, timed out, or returned nonzero"
      else
        echo "gh missing"
      fi
    } > "$REPORT_FILE" 2>&1
    CODE=$?; cat "$REPORT_FILE" > "$LOG_FILE"
    if [ "$CODE" -eq 0 ]; then write_result "success" "Repos found" "Local and GitHub repo list report written." "$CODE" "$REPORT_FILE" "Choose a local or online repo from the Android Repo screen."; else write_result "failed" "Could not list repos" "See report/log." "$CODE" "$REPORT_FILE" "Confirm ~/projects exists and GitHub CLI is authenticated for online repos."; fi
    exit "$CODE"
    ;;

  select_configured_repo)
    SELECTED_FILE="$CONFIG_DIR/selected_repo.txt"
    CLONE_FILE="$CONFIG_DIR/selected_repo_clone_url.txt"
    SOURCE_FILE="$CONFIG_DIR/selected_repo_source.txt"
    NAME_FILE="$CONFIG_DIR/selected_repo_name.txt"
    REPORT_FILE="$REPORTS_DIR/select_configured_repo.txt"
    if [ ! -f "$SELECTED_FILE" ]; then write_result "failed" "No repo chosen" "Android has not written config/selected_repo.txt yet." 1 "" "Tap Find Repos, then choose a repo from the Repo screen."; exit 1; fi
    TARGET="$(cat "$SELECTED_FILE" | head -n 1)"
    CLONE_URL="$(cat "$CLONE_FILE" 2>/dev/null | head -n 1 || true)"
    SOURCE="$(cat "$SOURCE_FILE" 2>/dev/null | head -n 1 || true)"
    CHOICE_NAME="$(cat "$NAME_FILE" 2>/dev/null | head -n 1 || true)"
    if [ -z "$TARGET" ]; then write_result "failed" "Chosen repo unavailable" "Chosen repo path was blank." 1 "" "Run Find Repos again, then choose an existing repo."; exit 1; fi
    {
      echo "Requested repo: ${CHOICE_NAME:-$TARGET}"
      echo "Source: ${SOURCE:-unknown}"
      echo "Target: $TARGET"
      echo "Clone URL: ${CLONE_URL:-none}"
      echo
      if [ ! -d "$TARGET/.git" ]; then
        if [ -n "$CLONE_URL" ]; then
          mkdir -p "$PROJECTS_DIR"
          cd "$PROJECTS_DIR" || exit 1
          echo "Cloning online repo..."
          git clone "$CLONE_URL" "$TARGET"
        else
          echo "Chosen repo is missing and no clone URL was provided."
          exit 1
        fi
      fi
      printf '%s\n' "$TARGET" > "$CONFIG_DIR/active_repo.txt"
      echo "Active repo set to:"
      cat "$CONFIG_DIR/active_repo.txt"
      echo
      cd "$TARGET" || exit 1
      git branch --show-current
      git status --short
    } > "$REPORT_FILE" 2>&1
    CODE=$?
    cat "$REPORT_FILE" > "$LOG_FILE"
    if [ "$CODE" -eq 0 ]; then cd "$TARGET" || exit 1; write_result "success" "Repo selected" "Selected $(basename "$TARGET")." 0 "$REPORT_FILE" "Check Repo next."; else write_result "failed" "Repo selection failed" "Exit $CODE. See report/log." "$CODE" "$REPORT_FILE" "Open the repo selection report. Check GitHub auth/network if this was an online repo."; fi
    ;;

  set_active_bridge)
    TARGET="$PROJECTS_DIR/applab-termux-bridge"
    if [ ! -d "$TARGET/.git" ]; then write_result "failed" "Bridge repo missing" "Clone the bridge repo before selecting it." 1 "" "Expected ~/projects/applab-termux-bridge to exist and contain .git."; exit 1; fi
    printf '%s\n' "$TARGET" > "$CONFIG_DIR/active_repo.txt"
    REPORT_FILE="$REPORTS_DIR/set_active_bridge.txt"
    { echo "Active repo set to:"; cat "$CONFIG_DIR/active_repo.txt"; } > "$REPORT_FILE" 2>&1
    cat "$REPORT_FILE" > "$LOG_FILE"; cd "$TARGET" || exit 1
    write_result "success" "Selected repo changed" "Bridge repo is now selected." 0 "$REPORT_FILE" "Run Git Status next."
    ;;

  set_active_libreseed)
    TARGET="$PROJECTS_DIR/libreseed-labs-android"
    if [ ! -d "$TARGET/.git" ]; then write_result "failed" "LibreSeed repo missing" "Clone LibreSeed before selecting it." 1 "" "Expected ~/projects/libreseed-labs-android to exist and contain .git."; exit 1; fi
    printf '%s\n' "$TARGET" > "$CONFIG_DIR/active_repo.txt"
    REPORT_FILE="$REPORTS_DIR/set_active_libreseed.txt"
    { echo "Active repo set to:"; cat "$CONFIG_DIR/active_repo.txt"; } > "$REPORT_FILE" 2>&1
    cat "$REPORT_FILE" > "$LOG_FILE"; cd "$TARGET" || exit 1
    write_result "success" "Selected repo changed" "LibreSeed is now selected." 0 "$REPORT_FILE" "Run Git Status next."
    ;;

  show_active_repo)
    REPORT_FILE="$REPORTS_DIR/show_active_repo.txt"
    { echo "Active repo config:"; if [ -f "$CONFIG_DIR/active_repo.txt" ]; then cat "$CONFIG_DIR/active_repo.txt"; else echo "No active_repo.txt set."; fi; echo; echo "Resolved active repo:"; active_repo || true; } > "$REPORT_FILE" 2>&1
    cat "$REPORT_FILE" > "$LOG_FILE"; need_repo
    write_result "success" "Selected repo checked" "Selected repo report written." 0 "$REPORT_FILE" ""
    ;;

  check_repo|show_status)
    need_repo
    run_report "${ACTION}.txt" bash -lc "pwd && git branch --show-current && echo && git status --short && echo && git status"
    ;;

  fetch_repo)
    need_repo
    run_report "fetch_repo.txt" git fetch --all --prune
    ;;

  pull_current)
    need_repo
    run_report "pull_current.txt" git pull --ff-only
    ;;

  checkout_staging)
    need_repo
    run_report "checkout_staging.txt" bash -lc "git checkout staging && git status"
    ;;

  pull_staging)
    need_repo
    run_report "pull_repo.txt" bash -lc "git checkout staging && git pull --ff-only"
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

  stage_all)
    need_repo
    run_report "stage_all.txt" bash -lc "git status --short && git add -A && echo && git status --short"
    ;;

  commit_no_apk)
    need_repo
    MSG_FILE="$CONFIG_DIR/commit_message.txt"
    if [ -f "$MSG_FILE" ]; then MSG="$(cat "$MSG_FILE" | head -n 1)"; else MSG="AppLab bridge update [no apk]"; fi
    case "$MSG" in *"[no apk]"*|*"[skip apk]"*) ;; *) MSG="$MSG [no apk]" ;; esac
    run_report "commit_no_apk.txt" bash -lc "git status --short && git commit -m \"$MSG\" && git log -1 --oneline"
    ;;

  push_current)
    need_repo
    run_report "push_current.txt" git push
    ;;

  run_patch_script)
    need_repo
    SCRIPT="$SHARED_DIR/patches/patch.sh"
    if [ ! -f "$SCRIPT" ]; then write_result "failed" "No patch script" "Put a script at Documents/AppLabBridge/patches/patch.sh." 1 "" "Create patches/patch.sh in the shared bridge folder before running this action."; exit 1; fi
    chmod +x "$SCRIPT"
    run_report "run_patch_script.txt" bash -lc "cd \"$(pwd)\" && bash \"$SCRIPT\" && echo && git status --short"
    ;;

  *)
    write_result "failed" "Unknown action" "Unknown action: $ACTION" 2 "" "The installed app and live dispatcher may be out of sync. Update the dispatcher from GitHub."
    exit 2
    ;;
esac
