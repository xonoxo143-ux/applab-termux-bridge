# AppLab Termux Bridge Working Context

Branch: `main`.
Repo: `xonoxo143-ux/applab-termux-bridge`.

## Purpose

AppLab Termux Bridge is a phone-first control panel for AppLab repo work.

The app should let the phone act like a normal Git workstation while keeping the user out of repeated terminal typing.

Primary flow:

```text
prepare repo
inspect branch and status
run an approved action
read result files
review changed files
commit
push
install APK when needed
```

This is a control panel, not a terminal emulator and not a full IDE.

## Architecture

Keep the existing architecture:

```text
Android Compose UI
  -> Termux RUN_COMMAND
  -> ~/.termux/applab/bridge.sh <action_id>
  -> shared result/report/log files
  -> Android result reader
```

Android owns:

- dashboard buttons
- folder picker
- result display
- report/log openers
- clipboard handoff
- APK install handoff

Termux owns:

- git
- gh
- python
- shell scripts
- build commands
- logs and reports

## Current app state

The app already has:

- Kotlin and Jetpack Compose
- single Activity
- Termux command runner
- action allowlist
- result JSON parser
- shared folder manager
- clipboard bridge
- APK installer helper
- report/log opener
- GitHub Actions debug APK build

Current sections:

- Setup
- Repo
- APK
- Save Codes
- Audit
- Debug
- Latest Result

## Current gap

The app is useful but needs to become a stronger Git workbench.

Needed workflow actions:

- list projects
- clone repo
- show repo status
- fetch repo
- pull current branch
- show branches
- switch branch
- show current commit
- list changed files
- show diff summary
- run prepared patch script
- stage changes
- commit changes
- push current branch

Keep actions allowlisted. Do not add an arbitrary command box for v1.

## Safety rules

- Show status before commit.
- Show changed files before commit.
- Do not auto-merge branches.
- Do not force-push in v1.
- Do not delete local work automatically.
- Warn when a commit may trigger a build.
- Use `[no apk]` when a commit should not trigger an APK build.

## Main real use case

The immediate use case is AppLab project work from the phone.

For example, LibreSeed Root Manager work needs local repo editing because some files are better handled on-device than through connector writes.

The bridge should make this easy:

```text
clone repo
checkout branch
run patch script
inspect diff
commit with a safe message
push
```

## Implementation direction

Next Android work:

1. Expand `BridgeAction` with Git workbench actions.
2. Add a clearer Git Workbench section in the Compose UI.
3. Keep result display through `latest_result.json`.
4. Document the Termux dispatcher actions expected by Android.
5. Build a debug APK when ready.

## Files to inspect when returning

- `README.md`
- `app/src/main/java/com/applab/termuxbridge/ui/AppLabBridgeApp.kt`
- `app/src/main/java/com/applab/termuxbridge/bridge/BridgeAction.kt`
- `app/src/main/java/com/applab/termuxbridge/bridge/TermuxRunner.kt`

## Current stance

Be practical. The goal is a working phone Git cockpit.

Do not stall on polish before the basic clone/status/diff/commit/push loop works.
