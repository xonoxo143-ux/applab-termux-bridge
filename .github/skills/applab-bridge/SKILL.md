---
name: applab-bridge
description: Use this skill when working on AppLab Bridge, the Android app that coordinates ChatGPT, GitHub, Termux, and phone-first repo/APK workflows.
---

# AppLab Bridge Skill

## Purpose

AppLab Bridge is a phone-first Android control app for repo work, patching, GitHub APK updates, and Termux-backed automation. The app should reduce manual terminal work, not increase it.

Use this skill whenever modifying or reasoning about this repository.

## Core Rules

### Branch discipline

- Treat `main` as the active AppLab Bridge branch unless the user explicitly says otherwise.
- Before changing files, state the target branch and why.
- Keep commits small and reviewable.
- Use `[no apk]` in commit messages for docs, skills, workflow text, or other changes that should not trigger APK builds.
- Do not trigger builds unless requested or clearly required by the current step.

### Phone-first discipline

- The user works primarily from an Android phone.
- APK testing is the real validation path.
- GitHub Actions success proves the APK was built, not that Android permissions, installers, SAF paths, or Termux behavior work on-device.
- Avoid workflows that require repeated manual Termux copy/paste unless file size or connector limits force it.

### Termux boundary discipline

Termux is a backend executor, not the main user interface.

Prefer this flow:

1. Android GUI button.
2. Official Termux `RUN_COMMAND` intent.
3. Termux writes structured result JSON.
4. Android reads and displays the result.
5. Reports/logs are opened only for detail or debugging.

Do not fall into the manual Termux trap:

- Do not keep asking the user to paste commands to copy backend files.
- If backend files are stale or missing, improve the app's bootstrap/repair flow.
- The app should be able to repair `~/.termux/applab/bridge_v2.sh` and helper files through the bootstrap path.

### Termux invocation standard

Use the official Termux `RUN_COMMAND` path:

- action: `com.termux.RUN_COMMAND`
- service: `com.termux.app.RunCommandService`
- permission: `com.termux.permission.RUN_COMMAND`
- package: `com.termux`
- absolute command paths under `/data/data/com.termux/files/...`

Required Termux-side condition:

```text
~/.termux/termux.properties contains allow-external-apps=true
```

The Android app must use:

- unique request codes for pending-intent results
- `RUN_COMMAND_PENDING_INTENT` result callback
- logs of stdout, stderr, exit code, Termux `err`, and `errmsg`
- explicit checks for Termux installed and RUN_COMMAND permission

For bootstrap, prefer `bash` plus stdin over making Termux read an Android SAF-written file path.

### Result model discipline

Use this mental model:

```text
latest_result.json = state/control signal
reports/*.txt = readable summary
logs/*.log = raw debug detail
android_app_bridge.log = Android-side launch/polling diagnostics
termux_result_service.log = Termux pending-intent callback diagnostics
```

The app should wait for `results/latest_result.json`, not for a latest log file.

Polling should track:

- expected action
- previous run ID
- new run ID
- matching or mismatched action
- timeout reason

If a result changes but action differs, show a mismatch, not success.

### Backend layout

Current preferred backend layout:

```text
termux/applab/bridge_v2.sh
termux/applab/lib/repo_state.sh
termux/applab/lib/result_writer.sh
termux/applab/lib/apk_actions.sh
termux/applab/lib/utility_actions.sh
termux/applab/lib/parked_actions.sh
app/src/main/assets/applab/install_backend.sh
```

Live phone target:

```text
~/.termux/applab/bridge_v2.sh
~/.termux/applab/lib/*.sh
```

Shared folder:

```text
Documents/AppLabBridge/
  apks/
  bootstrap/
  config/
  debug_zips/
  inbox/
  logs/
  patches/
  reports/
  results/
  save_codes/
```

### UX discipline

The app should feel like a guided control cockpit, not a terminal shortcut board.

Group related workflows:

- Home
- Repo Workbench
- Patch Runner
- Build / APK
- Results
- Advanced Tools
- Setup

Use explicit button labels. Avoid vague labels like `Refresh` or `Update` unless the object is clear.

Good labels:

- Reload Saved Result File
- Run Git Status
- Bootstrap / Repair Termux Backend
- Check GitHub APK Artifact
- Open Android Installer for Newest APK

Bad labels:

- Refresh
- Check
- Update
- Open

### Safety and confirmation

Require confirmation for actions that mutate state:

- bootstrap or dispatcher update
- clone repo
- checkout/pull staging
- run patch script
- stage all
- commit
- push
- download APK

Before patching, display or verify:

- selected repo
- branch
- dirty/clean state
- changed file counts
- patch file availability

Patch metadata should eventually be preferred:

```text
patches/patch.json
patches/patch.sh
```

Patch metadata should define target repo, target branch, expected files, clean-tree requirement, and commit message.

### Build discipline

When GitHub Actions fails:

1. Inspect the workflow logs before guessing.
2. Identify the exact compile/runtime line.
3. Fix the smallest cause first.
4. Do not mix unrelated UX or backend changes into a build-fix commit.

Node deprecation warnings from GitHub Actions are not APK build failures unless the action actually fails.

### Diagnostics before guessing

When a Termux action times out, do not guess. Check:

1. Android app log: `Documents/AppLabBridge/logs/android_app_bridge.log`
2. Termux pending-intent callback log: app internal `termux_result_service.log`
3. `results/latest_result.json`
4. action report under `reports/`
5. backend log under `logs/`

If the app-side log shows `startService` returned but there is no pending-intent callback, investigate Termux service/result delivery.

If pending-intent callback shows exit code or stderr, fix the backend script.

If Termux result succeeds but `latest_result.json` does not change, fix shared folder or result writer logic.

## Default next move hierarchy

When stuck, prefer this order:

1. Add instrumentation that distinguishes failure modes.
2. Fix the narrow confirmed failure.
3. Improve app-side recovery.
4. Only then ask the user for manual Termux commands.

Manual Termux commands are acceptable for investigation, but they should not become the normal workflow.
