#!/data/data/com.termux/files/usr/bin/bash

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g' | tr -d '\r'
}

write_minimal_result() {
  STATUS="$1"
  TITLE="$2"
  SUMMARY="$3"
  EXIT_CODE="$4"
  REPORT_FILE="${5:-}"
  DIAGNOSTIC_HINT="${6:-}"
  FINISHED_AT="$(date -Iseconds)"
  HISTORY_FILE="$RESULT_HISTORY_DIR/${RUN_ID}.json"

  STATUS_J="$(json_escape "$STATUS")"
  TITLE_J="$(json_escape "$TITLE")"
  SUMMARY_J="$(json_escape "$SUMMARY")"
  REPORT_FILE_J="$(json_escape "$REPORT_FILE")"
  LOG_FILE_J="$(json_escape "$LOG_FILE")"
  DIAGNOSTIC_HINT_J="$(json_escape "$DIAGNOSTIC_HINT")"
  STARTED_AT_J="$(json_escape "$STARTED_AT")"
  FINISHED_AT_J="$(json_escape "$FINISHED_AT")"
  ACTION_J="$(json_escape "$ACTION")"

  mkdir -p "$RESULTS_DIR" "$RESULT_HISTORY_DIR"
  cat > "$RESULTS_DIR/latest_result.json" <<JSON
{
  "schema_version": 2,
  "run_id": "$RUN_ID",
  "action": "$ACTION_J",
  "status": "$STATUS_J",
  "title": "$TITLE_J",
  "summary": "$SUMMARY_J",
  "started_at": "$STARTED_AT_J",
  "finished_at": "$FINISHED_AT_J",
  "exit_code": $EXIT_CODE,
  "report_file": "$REPORT_FILE_J",
  "log_file": "$LOG_FILE_J",
  "next_action": "",
  "diagnostic_hint": "$DIAGNOSTIC_HINT_J",
  "artifacts": [],
  "result_mode": "minimal"
}
JSON
  cp "$RESULTS_DIR/latest_result.json" "$HISTORY_FILE" 2>/dev/null || true
}

write_result() {
  STATUS="$1"
  TITLE="$2"
  SUMMARY="$3"
  EXIT_CODE="$4"
  REPORT_FILE="${5:-}"
  DIAGNOSTIC_HINT="${6:-}"
  FINISHED_AT="$(date -Iseconds)"
  HISTORY_FILE="$RESULT_HISTORY_DIR/${RUN_ID}.json"

  write_minimal_result "$STATUS" "$TITLE" "$SUMMARY" "$EXIT_CODE" "$REPORT_FILE" "$DIAGNOSTIC_HINT"
  echo "Minimal result written: $RESULTS_DIR/latest_result.json" >> "$LOG_FILE" 2>/dev/null || true

  EXTRA_FILE="$RESULTS_DIR/${RUN_ID}_repo_metadata.json"
  printf '{}' > "$EXTRA_FILE" 2>/dev/null || true
  if command -v repo_json_fields >/dev/null 2>&1; then
    if command -v timeout >/dev/null 2>&1; then
      timeout 8s bash -c 'repo_json_fields > "$1"' bash "$EXTRA_FILE" 2>>"$LOG_FILE" || {
        echo "Repo metadata enrichment unavailable or timed out; minimal result remains usable." >> "$LOG_FILE" 2>/dev/null || true
        printf '{}' > "$EXTRA_FILE" 2>/dev/null || true
      }
    else
      repo_json_fields > "$EXTRA_FILE" 2>>"$LOG_FILE" || printf '{}' > "$EXTRA_FILE"
    fi
  fi

  python3 - "$RESULTS_DIR/latest_result.json" "$HISTORY_FILE" "$EXTRA_FILE" <<PY
import json, sys, os
latest_path = sys.argv[1]
history_path = sys.argv[2]
extra_path = sys.argv[3]
with open(latest_path, "r", encoding="utf-8") as f:
    data = json.load(f)
data["result_mode"] = "enriched"
try:
  with open(extra_path, "r", encoding="utf-8") as f:
    extra_text = f.read().strip() or "{}"
  extra = json.loads(extra_text)
  if isinstance(extra, dict):
    data.update(extra)
except Exception as error:
  data["metadata_error"] = str(error)
os.makedirs(os.path.dirname(latest_path), exist_ok=True)
os.makedirs(os.path.dirname(history_path), exist_ok=True)
with open(latest_path, "w", encoding="utf-8") as f:
  json.dump(data, f, indent=2)
with open(history_path, "w", encoding="utf-8") as f:
  json.dump(data, f, indent=2)
PY
  PY_CODE=$?
  rm -f "$EXTRA_FILE" 2>/dev/null || true
  if [ "$PY_CODE" -ne 0 ]; then
    echo "Result enrichment failed with exit $PY_CODE; minimal result remains." >> "$LOG_FILE" 2>/dev/null || true
  else
    echo "Result enrichment completed." >> "$LOG_FILE" 2>/dev/null || true
  fi
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
