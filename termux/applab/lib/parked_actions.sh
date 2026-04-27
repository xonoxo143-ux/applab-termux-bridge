#!/data/data/com.termux/files/usr/bin/bash

park_action() {
  REPORT_NAME="$1"
  TITLE="$2"
  MESSAGE="$3"
  REPORT_FILE="$REPORTS_DIR/$REPORT_NAME"
  {
    echo "Action: $ACTION"
    echo "Run: $RUN_ID"
    echo "Started: $STARTED_AT"
    echo
    echo "$TITLE"
    echo
    echo "$MESSAGE"
  } > "$REPORT_FILE" 2>&1
  cat "$REPORT_FILE" > "$LOG_FILE"
  write_result "failed" "$TITLE" "$MESSAGE" 3 "$REPORT_FILE" "This action is recognized but not implemented in bridge v2 yet. It is parked intentionally."
  exit 3
}

handle_parked_action() {
  case "$ACTION" in
    decode_save)
      park_action "save_report.txt" "Save decoder parked" "Clipboard save decoding is not wired in bridge v2 yet."
      ;;
    validate_save)
      park_action "save_report.txt" "Save validator parked" "Save validation is not wired in bridge v2 yet."
      ;;
    find_server_calls)
      park_action "server_calls_report.txt" "Source audit parked" "Server-call source audit is not wired in bridge v2 yet."
      ;;
    find_save_paths)
      park_action "save_paths_report.txt" "Source audit parked" "Save-path source audit is not wired in bridge v2 yet."
      ;;
    find_root_manager)
      park_action "root_manager_report.txt" "Source audit parked" "Root manager source audit is not wired in bridge v2 yet."
      ;;
    find_hacking_labels)
      park_action "labels_report.txt" "Label audit parked" "Label audit is not wired in bridge v2 yet."
      ;;
    find_todos)
      park_action "todos_report.txt" "TODO audit parked" "TODO/FIXME source audit is not wired in bridge v2 yet."
      ;;
    find_android_permissions)
      park_action "android_permissions_report.txt" "Android permission audit parked" "Android permission source audit is not wired in bridge v2 yet."
      ;;
  esac
  return 1
}
