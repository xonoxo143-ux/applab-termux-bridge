---
name: phone-first-android-ux
description: Use this skill when designing or modifying Android UI for a phone-first workflow, especially control-panel apps with setup, repo, patch, APK, and diagnostic screens.
---

# Phone-First Android UX

## Purpose

Make Android screens usable on a phone without assuming desktop space, mouse precision, or browser-style workflows.

## When to Use

Use this skill when:

- adding or reorganizing Compose screens
- creating setup/debug controls
- grouping many workflow actions
- fixing cramped or ambiguous UI
- adding installer, repo, or Termux controls

## Principles

### 1. Group by workflow, not implementation

Good groups:

```text
Setup
Repo Workbench
Patch Runner
Build / APK
Results
Advanced Tools
```

Bad groups:

```text
All buttons
Scripts
Misc
Tools
```

### 2. Use explicit button labels

Good:

```text
Bootstrap / Repair Termux Backend
Reload Saved Result File
Check GitHub APK Artifact
Open Android Installer for Newest APK
Run Git Status
```

Bad:

```text
Refresh
Check
Update
Open
Run
```

### 3. Separate safe, risky, and destructive actions

Use clear visual and workflow separation:

```text
Safe read-only actions
State-changing actions
Publishing/install actions
Experimental/parked actions
```

Risky actions should explain what they change before confirmation.

### 4. Respect system insets

Root app content should account for:

```text
status bar
navigation bar / gesture area
keyboard / IME
```

Use Compose inset padding where appropriate:

```kotlin
.statusBarsPadding()
.navigationBarsPadding()
.imePadding()
```

### 5. Do not hide diagnostics

If an operation can fail silently, expose:

```text
what was attempted
where files were written
which command path was invoked
what result was expected
where logs are stored
```

### 6. Suggested next step must be concrete

A dashboard suggestion should be a real action or screen jump, not generic advice.

Example:

```text
Open Patch Runner
Run Git Status
Open Last Action Report
Bootstrap / Repair Termux Backend
```

## AppLab-specific screen intent

### Home

Dashboard and next best action.

### Setup

Shared folder, backend bootstrap/repair, permissions, and inbox.

### Repo Workbench

Repo selection, clone/scan, inspect, fetch/pull/checkout.

### Patch Runner

Patch readiness, run patch, review diff, stage/commit/push.

### Build / APK

Check artifact, download APK, open Android installer.

### Results

Latest structured result, report/log openers, debug zip creation.

### Advanced Tools

Parked/experimental tools and low-frequency maintenance actions.

## Failure-mode UX

Avoid generic timeouts. Prefer:

```text
Termux launch requested but no pending-intent result arrived.
Termux returned exit code 127.
Result JSON did not change after command exited.
Android wrote bootstrap file but Termux could not access it.
```

If the app cannot distinguish these yet, add instrumentation first.
