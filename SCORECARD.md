# SCORECARD

Rules are in [docs/TEST_LOOP.md](docs/TEST_LOOP.md) §4 and §5. Every point is binary: it passes with evidence or it
scores 0. Not run = 0.

## History

| Iteration | Score | What changed | What's failing |
|---|---|---|---|
| 0, baseline (v0.4.0 + lab) | **26/100** | Nothing in the app. Lab run 41151371477, commit 7f2f463 | See below |

## Baseline: 26/100

| Area | Points | Score |
|---|---|---|
| A. Alarm reliability | 25 | 0 |
| B. Scheduling & data correctness | 15 | 15 |
| C. Tap budget & flows | 15 | 3 |
| D. Glanceability | 10 | 0 |
| E. Accessibility | 10 | 0 |
| F. Visual design & consistency | 10 | 1 |
| G. Robustness & performance | 10 | 7 |
| H. Simulated usability | 5 | 0 |

**Why A is 0.** Area A passes a check only if it passes on every API in the matrix. Android 8 (API 26) could not be
measured, because the lab's own do-not-disturb hook is refused there (`SecurityException: Notification policy access
denied`). That is a lab bug, to fix first in the loop. APIs 29 and 34 were still running when this was written. On the
APIs that were measured (31, 33, 36), area A stands at **21/25**, which would make the baseline 47/100 once the rest
agree. The strict number is the one recorded.

## A. Alarm reliability (25)

Evidence: `lab-api<N>/*/reliability.log` and `results.json` in the run's artifacts. Each check below is one point,
except A01 (2). "Measured" means APIs 31, 33 and 36.

| Check | Pts | On 31 / 33 / 36 | Score | Evidence |
|---|---|---|---|---|
| A01 timing, locked, keyguard | 2 | PASS | 0 | fired +0 s on all three |
| A02 Doze | 1 | PASS | 0 | deep idle entered, alarm rang |
| A03 battery saver | 1 | PASS | 0 | |
| A04 do not disturb | 1 | PASS | 0 | |
| A05 silent: alarm stream and vibration | 1 | PASS | 0 | |
| A06 vibrate: alarm stream and vibration | 1 | PASS | 0 | |
| A07 process killed | 1 | PASS | 0 | |
| A08 removed from recents | 1 | PASS | 0 | swipe on 33, `am stack remove` on 31 and 36 |
| A09 force stop, restored on open | 1 | PASS | 0 | |
| A10 app update (install -r) | 1 | PASS | 0 | |
| A11 real reboot, before unlock | 1 | PASS | 0 | RUNNING_LOCKED, alarm stream on, +0 s |
| A11b real reboot, after unlock | 1 | PASS | 0 | +0 s |
| A12 clock change | 1 | PASS | 0 | |
| A13 timezone change | 1 | **FAIL** | 0 | real defect: next slot 07:45 local, off the new grid |
| A14 Egypt DST | 1 | PASS (N/A on 31, old tzdata) | 0 | |
| A15 permissions revoked warns | 1 | PASS | 0 | |
| A16 alarm volume zero warns | 1 | PASS | 0 | |
| A17 pocket guard, first second | 1 | **FAIL** | 0 | not in this build (F01) |
| A18 pocket guard, proximity | 1 | **FAIL** | 0 | not in this build (F01) |
| A19 undo snackbar and undo last | 1 | **FAIL** | 0 | not in this build |
| A20 same-minute collision, one screen | 1 | PASS | 0 | |
| A21 missed timeout, overdue notification | 1 | PASS | 0 | |
| A22 notification action syncs | 1 | PASS | 0 | |
| A23 round alarm, locked, own sound | 1 | PASS | 0 | |

## B. Scheduling & data correctness (15)

| Check | Pts | Result | Evidence |
|---|---|---|---|
| Unit tests | 4 | PASS | CI unit build #28, green |
| Property tests ≥ 10,000 cases | 6 | PASS | `SchedulePropertyTest`, 9 rules × 12,000 cases |
| Room migration | 3 | PASS | `MigrationTest` |
| Clinical wording frozen | 2 | PASS | `ClinicalContentTest` |
| Export | 0 | **N/A** | CSV and stats were removed by the maintainer on 2026-10-28 (Cerner is the record) and are not being re-added. Worth 0 points: the area's 15 points sit on the four checks above. Stated here so it is not mistaken for a pass. |

## C. Tap budget & flows (15)

Measured by `TapBudget.kt`, which counts every touch (tag `TAPS`).

| Task | Pts | Measured / budget | Result |
|---|---|---|---|
| 1 | 2 | 3 / 1 | FAIL |
| 2 | 2 | 1 / 1 | **PASS** |
| 3 | 2 | 4 / 2 | FAIL |
| 4 | 2 | 8 + typing / 4 | FAIL |
| 5 | 2 | 5 + typing / 4 | FAIL |
| 6 | 1 | 4 / 3 | FAIL |
| 7 | 2 | 1 from a transfusion alarm, 2 from Home, impossible from a round alarm (F41) / 2 | FAIL |
| 8 | 1 | 4–5 / 3 | FAIL |
| 9 | 1 | 3 / 3 | **PASS** |

## D. Glanceability (10)

| Check | Pts | Result | Evidence |
|---|---|---|---|
| Home key numbers ≥ 24 sp | 1 | FAIL | F11: next check's time is 16 sp inside a sentence, timeline chips 11 sp |
| Alarm key numbers ≥ 40 sp | 1 | FAIL | F04: the title is 34 sp and the time is not on the screen |
| Body ≥ 16 sp | 2 | FAIL | `SMALLTEXT` 144 at w412, font 1.0 |
| Contrast | 2 | FAIL | F26: amber on white 2.03:1, white on red 6.95:1 for key numbers |
| Zero truncation at font 2.0, 360 dp | 2 | FAIL | `TRUNCATED` 13–14, `MIDWORD` 44–46 at w360 f2.0 |
| One primary action in the bottom third | 2 | FAIL | F02: Start round below the fold at 7 beds |

## E. Accessibility (10)

| Check | Pts | Result | Evidence |
|---|---|---|---|
| Zero ATF results | 3 | FAIL | 0 on 26, 31, 33, 36; 1 (45 dp target) on API 34 f2.0 in run 41144824208 |
| Contrast | 1 | FAIL | F26 |
| Touch targets ≥ 56 / 64 dp | 2 | FAIL | `TOUCH` 15 at w412 |
| ≥ 8 dp between targets | 1 | FAIL | `SPACING` 5 |
| Colour-blind ΔE ≥ 15 | 1 | FAIL | F27: on time and missed differ by ΔE 8.9 under deuteranopia |
| TalkBack labels | 2 | FAIL | `NOLABEL` 4 |

## F. Visual design & consistency (10)

| Check | Pts | Result | Evidence |
|---|---|---|---|
| No hard-coded colours or sizes | 2 | FAIL | F30: raw ints in `Notifications.kt` |
| Both themes complete | 3 | FAIL | F29: no dark theme |
| Both features share components | 1 | **PASS** | `BedCard`, `ActionBlock`, `CardTimeline` used by both |
| Design reviewer passes | 4 | FAIL | 3/10, [docs/audit/reviewers-baseline.md](docs/audit/reviewers-baseline.md) |

## G. Robustness & performance (10)

Monkey, leaks, rotation and process death are from APIs 26, 31, 33 and 36. Provisional until 29 and 34 report.

| Check | Pts | Result | Evidence |
|---|---|---|---|
| Monkey ≥ 20,000 events, 0 crashes, 0 ANRs | 2 | **PASS** | 20,000 injected, 0 and 0 on each API |
| Zero leaks | 1 | **PASS** | forced heap analysis: 0 application leaks, 0 library leaks |
| Zero StrictMode violations | 1 | FAIL | 0 on 26, 12 on 31, 16 on 33, 5 on 36 |
| Zero lint errors | 1 | **PASS** | 0 errors, 17 warnings |
| ktlint | 1 | FAIL | fails, 1455 lines of output |
| Cold start ≤ 1200 ms on API 34 | 1 | **PASS** | 834.5 ms on API 34, run 41144824208; same app code. Elsewhere in this run: 665, 908.5, 888, 1002 ms |
| Rotation | 1 | **PASS** | `rotation.txt` |
| Process death | 1 | **PASS** | `process_death.txt` |
| Double-tap idempotence | 1 | FAIL | not run, so 0 |

## H. Simulated usability (5)

| Check | Pts | Result | Evidence |
|---|---|---|---|
| Persona 1, no concrete finding | 2 | FAIL | 22 findings |
| Persona 2, no concrete finding | 2 | FAIL | 23 findings |
| Persona 3, no concrete finding | 1 | FAIL | 20 findings |
