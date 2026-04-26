# AppLab Termux Bridge

Native Android control panel for approved Termux-backed AppLab workflows.

## Purpose

This app is a project cockpit, not a terminal emulator and not an IDE.

It sends approved action IDs to a Termux dispatcher script:

```text
Android GUI -> Termux RUN_COMMAND -> ~/.termux/applab/bridge.sh <action> -> JSON/report/log files
```

The Android app owns:

- dashboard buttons
- Storage Access Framework folder permission
- clipboard-to-file save handoff
- result JSON/report/log display
- APK install handoff

Termux owns:

- git / gh / curl / jq / rg / python / zip work
- script execution
- durable logs and reports

## V1 architecture

- Native Android
- Kotlin
- Jetpack Compose
- Single Activity
- Gradle Android project
- GitHub Actions debug APK artifact
- Termux `RUN_COMMAND` dispatcher
- File-based result reader
- No arbitrary command box

## Required Termux setup

Install the AppLab Termux script pack so this path exists:

```bash
~/.termux/applab/bridge.sh
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

GitHub Actions builds a debug APK artifact on every push to `main`.

Local build if Gradle 8.13+ and Android SDK are available:

```bash
gradle :app:assembleDebug
```
