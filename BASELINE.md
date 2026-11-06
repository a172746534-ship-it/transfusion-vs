# BASELINE: before the custom timer

Taken on code identical to `main` (v0.5.0, app code 9c1e37e), by commit 87365cc on branch `custom-timer`. That commit only saves the request and lets the lab run on this branch. docs/CUSTOM_TIMER.md §7 asks for this record; its §9 compares against it: **a check that passed here must still pass, and a check that failed here doesn't block the merge and isn't fixed in this change.**

- Lab run: **42186380405**, full matrix: API 26, 29, 31, 33, 34, 36.
- Build run: **42186380368**, #92.
- The audit screenshots of build #92 (182 PNG files) are kept outside git as the reference set for docs/SCREENSHOT_REGRESSION.md.

## Unit, lint, ktlint
- **Unit tests:** 165, 0 failures, 0 errors, 0 skipped. Every test that existed then still exists, unchanged, except the four Home screenshot references the request allows.
- **Android lint:** 0 errors, 22 warnings.
- **ktlint:** not clean before this change (2,721 report lines; its patch was never applied, SCORECARD.md says why).

## Area A: alarm reliability, 11/25

A check passes only if it passes on every API (N/A allowed where Android has no such behaviour).

| Check | 26 | 29 | 31 | 33 | 34 | 36 | Result |
|---|---|---|---|---|---|---|---|
| A01 timing, locked keyguard (2) | PASS | PASS | PASS | PASS | PASS | PASS | **PASS** |
| A02 Doze | PASS | PASS | PASS | PASS | PASS | PASS | **PASS** |
| A03 battery saver | PASS | PASS | PASS | PASS | PASS | PASS | **PASS** |
| A04 do not disturb | PASS | FAIL | PASS | PASS | PASS | PASS | FAIL |
| A05 silent | PASS | PASS | PASS | PASS | PASS | PASS | **PASS** |
| A06 vibrate | PASS | PASS | PASS | PASS | PASS | PASS | **PASS** |
| A07 process killed | PASS | PASS | PASS | PASS | PASS | PASS | **PASS** |
| A08 removed from recents | PASS | PASS | PASS | PASS | PASS | PASS | **PASS** |
| A09 force stop, restored on open | PASS | PASS | PASS | PASS | PASS | PASS | **PASS** |
| A10 app update | PASS | PASS | PASS | PASS | PASS | PASS | **PASS** |
| A11 reboot before unlock | PASS | PASS | PASS | PASS | PASS | PASS | **PASS** |
| A11b reboot after unlock | PASS | PASS | PASS | PASS | FAIL | PASS | FAIL |
| A12 clock change | PASS | PASS | PASS | PASS | FAIL | PASS | FAIL |
| A13 timezone change | PASS | PASS | PASS | PASS | FAIL | PASS | FAIL |
| A14 Egypt DST | N/A | N/A | N/A | PASS | FAIL | PASS | FAIL |
| A15 permissions revoked warn | N/A | N/A | PASS | PASS | FAIL | FAIL | FAIL |
| A16 alarm volume 0 warns | PASS | FAIL | FAIL | FAIL | FAIL | FAIL | FAIL |
| A17 pocket guard, first second | PASS | PASS | PASS | PASS | FAIL | PASS | FAIL |
| A18 pocket guard, proximity | PASS | PASS | PASS | PASS | FAIL | PASS | FAIL |
| A19 undo | PASS | FAIL | PASS | PASS | FAIL | PASS | FAIL |
| A20 same minute, one screen | PASS | PASS | PASS | PASS | FAIL | PASS | FAIL |
| A21 missed timeout, Overdue | PASS | PASS | PASS | PASS | FAIL | PASS | FAIL |
| A22 notification action | PASS | PASS | PASS | PASS | FAIL | PASS | FAIL |
| A23 round, locked, own sound | PASS | PASS | PASS | PASS | FAIL | PASS | FAIL |
| **M01 MRN masked (gate)** | PASS | PASS | PASS | PASS | FAIL | PASS | FAIL |

Why they failed (from the run's own details):
- **API 34:** the emulator was lost after A11b. Its "adb wait-for-device" timed out after 300 s, and every later case reports "device 'emulator-5554' not found", including the robustness suite. This is the lab's emulator, not the app; it still counts as FAIL here.
- **A04 on API 29:** zen_mode stayed 0 (the image didn't enter do-not-disturb); the alarm fired and played.
- **A15:** the Home warning count didn't rise after revoking (API 34: 0 → 0; API 36: 1 → 2 for two revoked).
- **A16:** the Home warning count didn't rise at alarm volume 0 (1 → 1).
- **A18 on API 34:** no proximity sensor on that image.
- **A19 on API 29:** no Undo after acknowledging.
- **A14:** N/A where the image's tzdata has no Egypt DST rule.

## Robustness, tap budget, UI checks (API 34 has none: the emulator was lost)

| API | Monkey | Rotation keeps the typed MRN | Process death keeps it | StrictMode | Cold start (median) | Leaks | Tap budget, tasks 1–9 (also at 360 dp) | Double tap, undo, widget | UI checks w360 f1.3 / w360 f2.0 / w412 f1.0 |
|---|---|---|---|---|---|---|---|---|---|
| 26 | 0 crashes, 0 ANR | FAIL | PASS | 0 | 895 ms | 0 | all in budget | 7 PASS | SPACING 8 / SPACING 9, MIDWORD 1 / SPACING 9 |
| 29 | 0 crashes, 0 ANR, didn't finish | FAIL | PASS | 0 | 641.5 ms | 0 | all in budget | 7 PASS | SPACING 9, MIDWORD 1 / SPACING 9, MIDWORD 1 / SPACING 9 |
| 31 | 0 crashes, 0 ANR | PASS | PASS | 1 (disk read) | 459 ms | 0 | all in budget | 7 PASS | SPACING 8 / SPACING 8, MIDWORD 1 / SPACING 9 |
| 33 | 0 crashes, 0 ANR | FAIL | PASS | 0 | 550.5 ms | 0 | all in budget | 7 PASS | SPACING 8 / SPACING 8, MIDWORD 1 / SPACING 9 |
| 34 | lost | FAIL | FAIL | – | – | – | not run | not run | not run |
| 36 | 0 crashes, 0 ANR | FAIL | PASS | 0 | 723 ms | 0 | all in budget | 7 PASS | SPACING 8 / SPACING 8, MIDWORD 1 / SPACING 9 |

The other UI-check categories (TOUCH, NOLABEL, TRUNCATED, SMALLTEXT, PRIMARY, ATF) are 0 on every API that ran.

## What this means for the custom timer
- **Must still pass:**
  - A01–A03, A05–A11
  - the tap budget for tasks 1–9
  - double tap / undo / widget
  - process death on the APIs that passed
  - 0 crashes and 0 leaks
  - lint 0 errors
  - every existing unit test
- **Failed here, so neither blocking nor fixed in this change:** A04, A11b–A23, M01 (on API 34), the rotation check, the UI-check SPACING and MIDWORD counts, and the StrictMode finding on API 31.
- **New gates** (`<id>@custom`, tasks 10–12, the custom UI and robustness checks) must pass on their own: SCORECARD.md, "Custom timer gates".
