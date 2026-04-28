---
name: ci-failure-triage
description: Use this skill when a GitHub Actions build, APK workflow, or CI check fails and the task is to diagnose and fix it without guessing.
---

# CI Failure Triage

## Purpose

Fix CI failures by reading the actual failing logs, isolating the smallest confirmed cause, and avoiding unrelated changes.

## When to Use

Use this skill when:

- GitHub Actions fails.
- APK build fails.
- Kotlin, Gradle, manifest, signing, or workflow errors appear.
- The user shares a GitHub Actions run/job/line URL.
- A previous build warning might be mistaken for a real failure.

## Rules

1. Inspect the failing job logs before explaining the cause.
2. Identify the failing step, task, file, and line if available.
3. Separate warnings from failures.
4. Fix the smallest confirmed cause first.
5. Do not mix build fixes with unrelated UX/backend changes.
6. After a compile break, check for nearby references that would fail next.
7. Commit build fixes with direct messages, not vague cleanup messages.

## Process

### 1. Locate the real failure

Find:

```text
workflow run
job
step
first compiler/runtime error
file path
line number
symbol or exception
```

Ignore unrelated noise until the primary failure is understood.

### 2. Classify the failure

Common classes:

```text
Kotlin unresolved reference
wrong function signature
missing import
manifest registration issue
Gradle dependency issue
asset/resource missing
signing/env secret issue
GitHub Actions runner warning
```

### 3. Patch narrowly

Examples:

```text
Unresolved function -> add or restore the function.
Wrong call signature -> update caller and callee together.
Missing manifest component -> register the service/activity/provider.
Wrong asset path -> add asset or correct loader path.
```

Do not refactor during a CI repair unless the refactor is the smallest repair.

### 4. Report clearly

Summarize:

```text
Failure: exact compiler/runtime error.
Cause: why it happened.
Fix: commit(s) made.
Risk: what still needs validation.
```

## GitHub Actions warning rule

Warnings such as Node.js deprecation notices are not build failures unless the step exits nonzero because of them.

If the actual failed task is `:app:compileDebugKotlin`, focus on Kotlin compile errors first.

## AppLab-specific notes

- APK workflow success only means the APK built.
- It does not prove Android install, Termux permission, SAF storage, or phone runtime behavior.
- For docs/skills-only changes, use `[no apk]` in commit messages if the workflow respects skip keywords.
