# PROGRESS — the Windows desktop app

Read this first after any restart, then docs/DESKTOP_APP.md (the request, word for word).
Unqualified names (BASELINE, CHANGE_PLAN, PORT_DIFF, SCORECARD, AUDIT, INSTALL_AR) mean the files in
`desktop/docs/`. The root files of the same names are read-only references, called "root".

## Done
- 2026-11-08. Section 0 checked against tag v0.6.0 (300b2c8) before anything changed. Every fact
  agrees with the code: the versions and workflows, the 10 schedule files, the tests' hidden
  dependencies, `Engine.kt` line 69, the ui package's Android calls, Room v4's seven tables, "A S",
  `UNDO_BAR_MS`, the token guard, the root BASELINE/SCORECARD failures. Nothing to report.
- Step 1. Branch `desktop` from main's head 6495106. The request saved as docs/DESKTOP_APP.md. The
  paste began with 36 empty numbered list markers ("1." to "35.", then "1.") holding no words; they
  are left out, and the text from "Change request:" on is word for word.
- Step 2 started: `.github/workflows/desktop-reference.yml` (tag v0.6.0, `testDebugUnitTest`, ktlint
  1.8.0). Actions are pinned to commit SHAs.

- Step 2 done. Baseline run 42461704759 (3 min 18 s): 241 reference JVM tests, all PASSED; ktlint 1.8.0
  6442 errors in 89 files; 262 audit PNGs. BASELINE.md written from its artifact `reference-baseline`.

## In progress
- Step 3: CHANGE_PLAN.md, before any code.

## Next, in order
1. Step 0, the spike, on windows-2022 and windows-2025.
2. Steps A to E, then the loop.
