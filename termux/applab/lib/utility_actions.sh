#!/data/data/com.termux/files/usr/bin/bash

handle_utility_action() {
  case "$ACTION" in
    clone_bridge)
      REPORT_FILE="$REPORTS_DIR/clone_bridge.txt"
      {
        echo "Cloning or updating bridge repo"
        mkdir -p "$PROJECTS_DIR"
        cd "$PROJECTS_DIR"
        if [ -d "$BRIDGE_LOCAL_DIR/.git" ]; then
          cd "$BRIDGE_LOCAL_DIR"
          git fetch --all --prune
          git pull --ff-only
        else
          gh repo clone "$BRIDGE_REPO" applab-termux-bridge
        fi
        printf '%s\n' "$BRIDGE_LOCAL_DIR" > "$CONFIG_DIR/active_repo.txt"
        cd "$BRIDGE_LOCAL_DIR"
        git status
      } > "$REPORT_FILE" 2>&1
      CODE=$?
      cat "$REPORT_FILE" > "$LOG_FILE"
      cd "$BRIDGE_LOCAL_DIR" 2>/dev/null || true
      if [ "$CODE" -eq 0 ]; then
        write_result "success" "Bridge repo ready" "Bridge repo cloned or updated and selected." "$CODE" "$REPORT_FILE" "Run Git Status next."
      else
        write_result "failed" "Bridge repo clone failed" "Exit $CODE. See report/log." "$CODE" "$REPORT_FILE" "Check GitHub auth and network access."
      fi
      exit "$CODE"
      ;;

    clone_libreseed)
      REPORT_FILE="$REPORTS_DIR/clone_libreseed.txt"
      TARGET="$PROJECTS_DIR/libreseed-labs-android"
      {
        echo "Cloning or updating LibreSeed repo"
        mkdir -p "$PROJECTS_DIR"
        cd "$PROJECTS_DIR"
        if [ -d "$TARGET/.git" ]; then
          cd "$TARGET"
          git fetch --all --prune
          git pull --ff-only || true
        else
          gh repo clone xonoxo143-ux/libreseed-labs-android libreseed-labs-android
        fi
        printf '%s\n' "$TARGET" > "$CONFIG_DIR/active_repo.txt"
        cd "$TARGET"
        git status
      } > "$REPORT_FILE" 2>&1
      CODE=$?
      cat "$REPORT_FILE" > "$LOG_FILE"
      cd "$TARGET" 2>/dev/null || true
      if [ "$CODE" -eq 0 ]; then
        write_result "success" "LibreSeed repo ready" "LibreSeed repo cloned or updated and selected." "$CODE" "$REPORT_FILE" "Run Git Status next."
      else
        write_result "failed" "LibreSeed repo clone failed" "Exit $CODE. See report/log." "$CODE" "$REPORT_FILE" "Check GitHub auth, network access, and repo permissions."
      fi
      exit "$CODE"
      ;;

    show_full_diff)
      need_repo
      run_report "show_full_diff.txt" bash -lc "git diff --stat && echo && git diff"
      ;;

    create_debug_zip)
      REPORT_FILE="$REPORTS_DIR/debug_bundle_report.txt"
      ZIP_FILE="$SHARED_DIR/debug_zips/AppLabBridge-debug-${RUN_ID}.zip"
      {
        echo "Creating AppLab Bridge debug zip"
        mkdir -p "$SHARED_DIR/debug_zips"
        cd "$SHARED_DIR"
        zip -r "$ZIP_FILE" results reports logs config -x '*.apk' || true
        echo
        echo "Debug zip: $ZIP_FILE"
        ls -lh "$ZIP_FILE"
      } > "$REPORT_FILE" 2>&1
      CODE=$?
      cat "$REPORT_FILE" > "$LOG_FILE"
      if [ "$CODE" -eq 0 ]; then
        write_result "success" "Debug zip created" "Debug zip written to debug_zips/." "$CODE" "$REPORT_FILE" "Open latest debug zip from Results."
      else
        write_result "failed" "Debug zip failed" "Exit $CODE. See report/log." "$CODE" "$REPORT_FILE" "Check zip availability and shared folder permissions."
      fi
      exit "$CODE"
      ;;
  esac
  return 1
}
