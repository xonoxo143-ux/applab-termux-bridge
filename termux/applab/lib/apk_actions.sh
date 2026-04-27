#!/data/data/com.termux/files/usr/bin/bash

handle_apk_action() {
  case "$ACTION" in
    check_latest_apk)
      REPORT_FILE="$REPORTS_DIR/check_latest_apk.txt"
      {
        echo "GitHub APK artifact source"
        echo "Repo: $BRIDGE_REPO"
        echo "Workflow: $BRIDGE_WORKFLOW"
        echo "Artifact: $BRIDGE_ARTIFACT"
        echo
        echo "Latest successful workflow runs:"
        gh run list --repo "$BRIDGE_REPO" --workflow "$BRIDGE_WORKFLOW" --status success --limit 5 || true
        echo
        echo "Local APKs:"
        find "$APKS_DIR" -name '*.apk' -type f 2>/dev/null | sort | tail -n 20
      } > "$REPORT_FILE" 2>&1
      CODE=$?
      cat "$REPORT_FILE" > "$LOG_FILE"
      if [ "$CODE" -eq 0 ]; then
        write_result "success" "APK artifact checked" "Latest APK report written." "$CODE" "$REPORT_FILE" ""
      else
        write_result "failed" "APK artifact check failed" "See report/log." "$CODE" "$REPORT_FILE" "Check GitHub auth and workflow availability."
      fi
      exit "$CODE"
      ;;

    download_latest_apk)
      REPORT_FILE="$REPORTS_DIR/download_latest_apk.txt"
      TMP_DIR="$SHARED_DIR/tmp_apk_download_$RUN_ID"
      {
        echo "Downloading latest AppLab Bridge APK artifact"
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
        write_result "success" "APK downloaded" "Latest AppLab Bridge APK downloaded to apks/." "$CODE" "$REPORT_FILE" "Open Android installer for the newest APK next."
      else
        write_result "failed" "APK download failed" "Exit $CODE. See report/log." "$CODE" "$REPORT_FILE" "Check GitHub auth, workflow success, and artifact name."
      fi
      exit "$CODE"
      ;;
  esac
  return 1
}
