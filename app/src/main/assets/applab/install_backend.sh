#!/data/data/com.termux/files/usr/bin/bash
set -u

HOME_DIR="/data/data/com.termux/files/home"
PROJECTS_DIR="$HOME_DIR/projects"
SHARED_DIR="$HOME_DIR/storage/shared/Documents/AppLabBridge"
REPORTS_DIR="$SHARED_DIR/reports"
LOGS_DIR="$SHARED_DIR/logs"
RESULTS_DIR="$SHARED_DIR/results"
CONFIG_DIR="$SHARED_DIR/config"
RUN_ID="$(date +%Y%m%d_%H%M%S)_$$"
REPORT_FILE="$REPORTS_DIR/bootstrap_backend.txt"
LOG_FILE="$LOGS_DIR/${RUN_ID}_bootstrap_backend.log"
BRIDGE_REPO="xonoxo143-ux/applab-termux-bridge"
BRIDGE_LOCAL_DIR="$PROJECTS_DIR/applab-termux-bridge"
LIVE_DIR="$HOME_DIR/.termux/applab"

mkdir -p "$PROJECTS_DIR" "$REPORTS_DIR" "$LOGS_DIR" "$RESULTS_DIR" "$CONFIG_DIR" "$LIVE_DIR/lib"

write_failure_result() {
  CODE="$1"
  SUMMARY="$2"
  cat > "$RESULTS_DIR/latest_result.json" <<JSON
{
  "schema_version": 2,
  "run_id": "$RUN_ID",
  "action": "check_setup",
  "status": "failed",
  "title": "Backend bootstrap failed",
  "summary": "$SUMMARY",
  "exit_code": $CODE,
  "report_file": "$REPORT_FILE",
  "log_file": "$LOG_FILE",
  "next_action": "",
  "diagnostic_hint": "Open the bootstrap report. Check GitHub auth, network access, and whether the bridge repo has local uncommitted changes.",
  "artifacts": []
}
JSON
}

{
  echo "AppLab Bridge backend bootstrap"
  echo "Run: $RUN_ID"
  echo "Time: $(date)"
  echo
  echo "Shared dir: $SHARED_DIR"
  echo "Bridge repo: $BRIDGE_REPO"
  echo "Local repo: $BRIDGE_LOCAL_DIR"
  echo "Live dir: $LIVE_DIR"
  echo

  command -v git
  command -v gh

  cd "$PROJECTS_DIR"
  if [ ! -d "$BRIDGE_LOCAL_DIR/.git" ]; then
    echo "Bridge repo missing. Cloning."
    gh repo clone "$BRIDGE_REPO" applab-termux-bridge
  else
    echo "Bridge repo found. Updating."
    cd "$BRIDGE_LOCAL_DIR"
    git checkout main
    git pull --ff-only
  fi

  cd "$BRIDGE_LOCAL_DIR"
  if [ ! -f termux/applab/bridge_v2.sh ]; then
    echo "Missing termux/applab/bridge_v2.sh in bridge repo."
    exit 20
  fi
  if [ ! -d termux/applab/lib ]; then
    echo "Missing termux/applab/lib in bridge repo."
    exit 21
  fi

  mkdir -p "$LIVE_DIR/lib"
  cp termux/applab/bridge_v2.sh "$LIVE_DIR/bridge_v2.sh"
  cp termux/applab/lib/*.sh "$LIVE_DIR/lib/"
  chmod +x "$LIVE_DIR/bridge_v2.sh" "$LIVE_DIR/lib/"*.sh

  echo
  echo "Installed live backend files:"
  ls -l "$LIVE_DIR"
  ls -l "$LIVE_DIR/lib"
  echo
  echo "Running v2 setup check..."
  "$LIVE_DIR/bridge_v2.sh" check_setup
} > "$REPORT_FILE" 2>&1
CODE=$?
cat "$REPORT_FILE" > "$LOG_FILE" 2>/dev/null || true

if [ "$CODE" -ne 0 ]; then
  write_failure_result "$CODE" "Bootstrap failed with exit $CODE. See bootstrap report."
fi

exit "$CODE"
