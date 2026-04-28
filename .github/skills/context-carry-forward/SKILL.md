---
name: context-carry-forward
description: Use this skill when preserving project state, decisions, failures, and next steps across turns, chats, or handoff documents.
---

# Context Carry-Forward

## Purpose

Keep project continuity compact, accurate, and useful without hauling chat noise.

## When to Use

Use this skill when:

- A task spans multiple turns.
- A build fails and the cause/fix must be remembered.
- The user wants a working context, skill file, README, status note, or handoff.
- The conversation risks losing state.

## What to Preserve

Preserve facts that change future decisions:

```text
current branch
latest commits
what was implemented
what failed
why it failed
known constraints
exact next move
manual steps already tried
hard limits
```

Do not preserve noise:

```text
apologies
repeated speculation
dead-end guesses without lesson
chat filler
obsolete plans
```

## Format

Use compact sections:

```text
Objective
Current branch
Implemented
Known failures
Confirmed facts
Open risks
Next move
```

## Failure tracking

When something fails, record:

```text
Symptom
Evidence
Cause, if confirmed
Rejected causes
Fix attempted
Result
Next diagnostic
```

Rejected causes matter. They prevent loops.

Example:

```text
Rejected: Termux allow-external-apps missing. User verified allow-external-apps=true.
Rejected: Android RUN_COMMAND permission missing. User screenshot showed permission allowed.
Still open: whether Termux receives the RUN_COMMAND intent and returns pending-intent result.
```

## Compression rule

A carry-forward should make the next assistant faster and less wrong. If a detail does not affect future action, remove it.

## AppLab-specific reminders

Always preserve:

- phone-first workflow
- APK testing is real validation
- avoid manual Termux-copy loops
- `latest_result.json` is state
- reports/logs are detail
- Android app diagnostics are needed when Termux may not start
- pending-intent receiver is the correct next instrumentation layer

## Final handoff shape

Use this when handing off after repo work:

```text
Branch: main
Commits: ...
Current build state: pass/fail/unknown
Last confirmed failure: ...
Last fix: ...
Next test: ...
Do not repeat: ...
```
