#!/data/data/com.termux/files/usr/bin/bash

repo_json_fields() {
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    REPO_PATH="$(pwd)"
    REPO_NAME="$(basename "$REPO_PATH")"
    BRANCH="$(git branch --show-current 2>/dev/null || true)"
    STATUS_SHORT="$(git status --porcelain=v1 2>/dev/null || true)"
    CHANGED_FILES="$(printf '%s\n' "$STATUS_SHORT" | sed '/^$/d' | wc -l | tr -d ' ')"
    STAGED_FILES="$(printf '%s\n' "$STATUS_SHORT" | awk 'substr($0,1,2)!="??" && substr($0,1,1)!=" " && length($0)>0 {count++} END {print count+0}')"
    UNSTAGED_FILES="$(printf '%s\n' "$STATUS_SHORT" | awk 'substr($0,1,2)!="??" && substr($0,2,1)!=" " && length($0)>0 {count++} END {print count+0}')"
    UNTRACKED_FILES="$(printf '%s\n' "$STATUS_SHORT" | awk 'substr($0,1,2)=="??" {count++} END {print count+0}')"
    if [ "${CHANGED_FILES:-0}" -gt 0 ]; then DIRTY="true"; else DIRTY="false"; fi
    AHEAD="0"
    BEHIND="0"
    UPSTREAM=""
    if git rev-parse --abbrev-ref --symbolic-full-name '@{u}' >/dev/null 2>&1; then
      UPSTREAM="$(git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>/dev/null || true)"
      COUNTS="$(git rev-list --left-right --count HEAD...@{u} 2>/dev/null || echo '0 0')"
      AHEAD="$(printf '%s' "$COUNTS" | awk '{print $1}')"
      BEHIND="$(printf '%s' "$COUNTS" | awk '{print $2}')"
    fi
    CURRENT_COMMIT="$(git rev-parse --short HEAD 2>/dev/null || true)"
    CURRENT_COMMIT_MESSAGE="$(git log -1 --pretty=%s 2>/dev/null || true)"
    REMOTE_URL="$(git remote get-url origin 2>/dev/null || true)"
    if [ -f "$SHARED_DIR/patches/patch.sh" ]; then HAS_PATCH_FILE="true"; else HAS_PATCH_FILE="false"; fi

    python3 - "$REPO_PATH" "$REPO_NAME" "$BRANCH" "$DIRTY" "$CHANGED_FILES" "$STAGED_FILES" "$UNSTAGED_FILES" "$UNTRACKED_FILES" "$AHEAD" "$BEHIND" "$CURRENT_COMMIT" "$CURRENT_COMMIT_MESSAGE" "$UPSTREAM" "$REMOTE_URL" "$HAS_PATCH_FILE" <<'PY'
import json, sys
(
    repo_path, repo_name, branch, dirty, changed_files, staged_files,
    unstaged_files, untracked_files, ahead, behind, current_commit,
    current_commit_message, upstream, remote_url, has_patch_file,
) = sys.argv[1:]

def to_int(value):
    try:
        return int(value)
    except Exception:
        return 0

print(json.dumps({
    "repo_path": repo_path,
    "repo_name": repo_name,
    "branch": branch,
    "dirty": dirty == "true",
    "changed_files": to_int(changed_files),
    "staged_files": to_int(staged_files),
    "unstaged_files": to_int(unstaged_files),
    "untracked_files": to_int(untracked_files),
    "ahead": to_int(ahead),
    "behind": to_int(behind),
    "current_commit": current_commit,
    "current_commit_message": current_commit_message,
    "upstream": upstream,
    "remote_url": remote_url,
    "has_patch_file": has_patch_file == "true",
}), end="")
PY
  else
    printf '{}'
  fi
}
