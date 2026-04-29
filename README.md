# AppLab Termux Bridge

Native Android control panel for approved Termux-backed AppLab workflows.

## Purpose

This app is a project cockpit, not a terminal emulator and not an IDE.

It sends approved action IDs to a Termux dispatcher script:

```text
Android GUI -> Termux RUN_COMMAND -> ~/.termux/applab/bridge_v2.sh <action> -> JSON/report/log files
```

The Android app owns:

- workflow screens
- Storage Access Framework folder permission
- repo chooser UI
- clipboard-to-file handoff
- result/report/log display
- APK install handoff

Termux owns:

- git / gh / python / zip work
- repo actions
- script execution
- durable logs and reports

## UI model

Normal screens are workflow-first:

```text
Home -> status, current repo, next step, pinned actions, last result
Setup -> access, backend, recovery
Repo -> current repo, choose repo, repo actions
Actions -> compact action runner and pin/hide management
Results -> last result, reports/logs, debug bundle
Updates -> check, download, install update
Advanced -> raw repo, backend, debug, and parked tools
```

Advanced is where low-level or rarely used tools belong.

## Required Termux setup

The live backend path is:

```bash
~/.termux/applab/bridge_v2.sh
```

Termux must also allow external command calls:

```bash
mkdir -p ~/.termux
echo 'allow-external-apps=true' >> ~/.termux/termux.properties
termux-reload-settings
```

The app uses the Termux `RUN_COMMAND` intent and requests `com.termux.permission.RUN_COMMAND`. You may need to grant that permission manually from Android app settings.

## Shared folder

Recommended folder:

```text
Documents/AppLabBridge
```

The app asks you to pick this folder with Android's folder picker. Termux should use the same location through:

```bash
~/storage/shared/Documents/AppLabBridge
```

## Build

GitHub Actions builds a debug APK artifact on pushes to `main` unless the workflow is skipped.

Local build if Gradle 8.13+ and Android SDK are available:

```bash
gradle :app:assembleDebug
```
