# PROGRESS — design overhaul

Read this first after any restart, then docs/DESIGN_OVERHAUL.md and docs/TEST_LOOP.md.

## Done
- Branch `design-overhaul` created from main (5d32b55, v0.4.0). main untouched.
- docs/DESIGN_OVERHAUL.md and docs/TEST_LOOP.md saved verbatim. SPEC.md updated.
- Test lab: `.github/workflows/lab.yml` (runs on pushes to this branch with `[lab]` in the commit
  message; `lab/config.json` says which APIs), `lab/reliability.py`, `lab/robustness.sh`,
  `lab/score.py`, `lab/color_check.py`. Debug-only hooks in `app/src/debug`. Emulator UI checks and
  tap counting in `app/src/androidTest/.../uicheck`. Property tests in `SchedulePropertyTest`.
- Phase 1: AUDIT.md, docs/audit/, baseline SCORECARD.md, independent reviewers
  (docs/audit/reviewers-baseline.md).
- Phase 2: prototypes in `app/src/test/.../directions`, comparison in docs/directions/.

## In progress
- Phase 2 stop: PR open, waiting for the maintainer to pick a direction or a mix.

## Next (after the pick, without waiting again)
- Phase 3: flows that meet the tap budget, strips of screenshots with tap numbers in the PR.
- Phase 4: design system (tokens, components), screens, then widget, notifications, shortcuts.
  Bed list needs a Room migration and a migration test. Move chosen fonts from src/debug to src/main
  with their OFL licences. Rewrite TapBudget and UiChecks for the new UI (tests, not thresholds).
- Phase 5: the loop until 100/100 on two consecutive full-matrix runs, or 3 flat iterations.

## Real defects the baseline found (fix in the loop, highest clinical risk first)
- Lab, not app: on API 26 `lab.DND` throws "Notification policy access denied", and that fails every
  reliability test there. Fix first. Only A04 should touch do not disturb, and a setup error must fail
  one test, not all of them. Lab run 41151371477 had APIs 29 and 34 still running when the baseline
  was written; add their results to SCORECARD.md.
- A13: a plan whose first slot has not fired yet keeps that slot after a timezone change, off the new
  grid (two alarms 15 min apart in the test). `TurnGrid.slots` starts from `firstSlotAt` unconditionally.
- F33: on a phone in Arabic the whole UI mirrors and text scrambles (`supportsRtl="true"`).
- F41: a round alarm alone has no way to reach the reaction guide.
- StrictMode: disk reads on the main thread in `Settings` and `AlarmPlayer`, leaked closables.

## Decisions
- CSV export / stats do not exist: the maintainer removed them on 2026-10-28 (Cerner is the
  record) and they are not being re-added. The "export" check in TEST_LOOP is N/A, recorded
  as such in SCORECARD.md and in the final report.
- No local Android SDK: GitHub Actions is the compiler and the emulator lab.
- `workflow_dispatch` needs the workflow on the default branch and main must stay untouched, so the
  lab is triggered by `[lab]` in a commit message on this branch.
- A phone in use gets the alarm as a heads-up, by Android's design: lab tests expecting the full
  screen turn the screen off first; tests for the phone in use assert the sound and the action button.
- Doze: Android will not enter deep idle with a wake-up alarm under 30 min away, so the lab lowers
  `min_time_to_alarm` to prove an alarm set before Doze rings during Doze.
- Monkey uses --ignore-crashes/--ignore-timeouts so another app's ANR doesn't end the run; this app's
  crashes and ANRs are still counted from the log and must be zero.
