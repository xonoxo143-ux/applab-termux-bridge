#!/data/data/com.termux/files/usr/bin/bash

write_result() {
  STATUS="$1"
  TITLE="$2"
  SUMMARY="$3"
  EXIT_CODE="$4"
  REPORT_FILE="${5:-}"
  DIAGNOSTIC_HINT="${6:-}"
  FINISHED_AT="$(date -Iseconds)"
  EXTRA_JSON="$(repo_json_fields || printf '{}')"
  HISTORY_FILE="$RESULT_HISTORY_DIR/${RUN_ID}.json"

  python3 - "$RESULTS_DIR/latest_result.json" "$HISTORY_FILE" "$EXTRA_JSON" <<PY
import json, sys, os
latest_path = sys.argv[1]
history_path = sys.argv[2]
extra_text = sys.argv[3] if len(sys.argv) > 3 else "{}"
data = {
  "schema_version": 2,
  "run_id": "$RUN_ID",
  "action": "$ACTION",
  "status": "$STATUS",
  "title": "$TITLE",
  "summary": "$SUMMARY",
  "started_at": "$STARTED_AT",
  "finished_at": "$FINISHED_AT",
  "exit_code": int("$EXIT_CODE"),
  "report_file": "$REPORT_FILE",
  "log_file": "$LOG_FILE",
  "next_action": "",
  "diagnostic_hint": "$DIAGNOSTIC_HINT",
  "artifacts": []
}
try:
  extra = json.loads(extra_text)
  if isinstance(extra, dict):
    data.update(extra)
except Exception:
  pass
os.makedirs(os.path.dirname(latest_path), exist_ok=True)
os.makedirs(os.path.dirname(history_path), exist_ok=True)
with open(latest_path, "w", encoding="utf-8") as f:
  json.dump(data, f, indent=2)
with open(history_path, "w", encoding="utf-8") as f:
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
    echo "Started: $STARTED_AT"
    echo "Time: $(date)"
    echo
    "$@"
  } > "$REPORT_FILE" 2>&1
  CODE=$?
  cat "$REPORT_FILE" > "$LOG_FILE" 2>/dev/null || true
  if [ "$CODE" -eq 0 ]; then
    write_result "success" "Action completed" "Report written: $REPORT_NAME" "$CODE" "$REPORT_FILE" ""
  else
    write_result "failed" "Action failed" "Exit $CODE. See report/log." "$CODE" "$REPORT_FILE" "Open the report and log. Check selected repo, branch, and working-tree state."
  fi
  exit "$CODE"
}
