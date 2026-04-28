---
name: long-context-decomposition
description: Use this skill when a task spans many files, many prior decisions, or a large repo and needs controlled decomposition instead of one giant patch.
---

# Long Context Decomposition

## Purpose

Turn large, risky work into controlled, inspectable batches without losing the overall objective.

## When to Use

Use this skill when:

- The task touches many files.
- The user asks for a whole UX overhaul or large backend change.
- A file is too large or connector patches fail.
- The repo contains source-wide terminology or workflow changes.
- Work needs to be continued across multiple turns.

## Operating Rule

Do not collapse a large task into a single risky patch. Split it by responsibility:

```text
model/state
backend helper
UI screen
wiring
diagnostics
docs/skills
```

Each batch should leave the repo in a coherent state.

## Process

### 1. Define the outcome

Write the target in one sentence:

```text
Make backend bootstrap self-repairing from the Android app.
```

or:

```text
Make every backend action visible in a grouped phone-first UX.
```

### 2. Map file groups

Example grouping:

```text
BridgeAction / state model
TermuxRunner / backend launch
Bootstrapper / storage bridge
Screens / user flow
Diagnostics / logs and receiver
Backend scripts / Termux actions
```

### 3. Patch in dependency order

Preferred order:

```text
new isolated helper files
small model additions
wiring changes
UI exposure
diagnostics
cleanup
```

Avoid editing the largest file first.

### 4. Stop on strange patch behavior

If a connector blocks a patch or the file becomes risky:

- do not resend the same payload
- split into helper files
- add hooks instead of replacing a large file
- use Termux fallback only when the repo connector cannot safely apply small patches

### 5. Summarize checkpoint state

After each meaningful batch, report:

```text
commits added
files touched
what now works
what is still missing
next safest move
```

## AppLab-specific patterns

For backend growth, prefer:

```text
bridge_v2.sh = dispatcher
lib/*.sh = focused helpers
```

For Android growth, prefer:

```text
new screen/helper file first
then app-shell wiring
then small UI cleanup
```

Do not turn one app shell or one dispatcher file into an unpatchable blob.
