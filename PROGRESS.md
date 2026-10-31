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

## Maintainer's decisions on 2026-10-31 (Phase 2 stop answered)
- Direction: **Ward Card**, on its own. Overdue keeps Ward Card's red diamond (no octagon from Halo).
- The reaction guide belongs to transfusions only: it does not appear on repositioning screens or on a
  round-only alarm. This overrides "2 touches from anywhere" for those screens, and F41 is withdrawn.
  A reviewer finding that asks for it there is out of scope, and the rubric says so.

## MRN add-on (docs/MRN_ADDON.md), added to this plan on 2026-10-31
- Part of this plan, not a separate task. (Since replaced: the MRN is now how a patient is identified, see the end of this file.)
- A run of 5+ digits in a bed label counts as an MRN typed by mistake: masked in notifications, on the alarm over
  the lock screen and on the widget (`schedule/BedMask.kt`). Evidence: `BedMaskTest`, lab `M01`, `widget_mrn_masked`.
- No CSV exists, so that item holds by having none. M01 is a gate for the merge and carries no points.

## Where it stopped on 2026-10-31 (usage limit). Read this first.
- Ward Card is built: design system, all screens, bed grid, undo, pocket guards, night theme, widget, shortcuts, MRN mask, timezone fix. Phase 3 strips are in docs/flows and PR #1. v0.4.1 is released and merged into this branch.
- Lab run 41161823563 (commit 219b0a2, API 34 only): area A 25/25, nine tasks within budget, monkey clean, 0 leaks, cold start 598.5 ms, double tap, undo and widget PASS.
- Lab run 41164846208 (commit 5b75b86, API 34) was still running. It is the first with the persona fixes and M01. Read it next: `LAB_MATRIX=34 python lab/score.py <dir>`.
- Persona findings, round 1, are in docs/audit/reviewers-round1.md and mostly fixed in f497bde, not yet re-reviewed. The design reviewer's round was stopped before it finished: run a fresh one.

## Next, in order
1. Read run 41164846208. Fix what remains from run 41161823563 if it still shows:
   - TOUCH: `start` duration chips 46x56 dp ("1 h"): give them a minimum width. `round_checklist` "Back" chip 62x64.
   - SPACING: `round_checklist` chips against the pinned action 1.1 dp; `detail` last button against the reaction bar 6.5 dp: add padding above pinned areas.
   - f2.0: glyphs in fixed-size boxes count as TRUNCATED (step badges, ± steppers, ↑, "!"): let them grow. MIDWORD in `settings` chips.
   - StrictMode 23: disk reads about 215 ms and leaked closables, one a Cursor: find the sites in monkey.logcat.txt. LabReceiver's own `rows()` is not the leak (it uses `use`).
2. ktlint: run `ktlint -F` once in CI and apply the patch near the end, since it breaks exact-string patches. Add only `ktlint_function_naming_ignore_when_annotated_with = Composable`. Fix long lines by hand.
3. Set app/src/test/screenshots/MODE back to verify and commit the recorded references. Add the 3.json schema from the CI artifact.
4. Fresh design reviewer and 3 personas on new screenshots, then the full matrix (lab/config.json apis 26,29,31,33,34,36). Android 8 needs `grant_policy_access_by_hand` to work: check its log.
5. Update SCORECARD.md: add the iteration history, and add baseline APIs 29 (17 pass, 5 fail, 2 N/A) and 34 (emulator died at A11). Then loop to 100/100 on two full-matrix runs, merge, release.

## MRN instead of the bed number (maintainer, 2026-10-31)
SPEC.md has the rules. Code still says `bedLabel` and `BedView`; what they hold is the MRN. The reviewers' rubric must state this decision, and that the nurse types the MRN each time, with no list to pick from, is the maintainer's choice.

## Stopped on 2026-10-31 (maintainer)
The maintainer ended the emulator testing: the MRN change was the last thing wanted. So the full matrix did not run and 100/100 was not reached. SCORECARD.md lists what is and isn't proven. Merging and releasing without that stop condition is the maintainer's call, asked for separately. Screenshots are back to MODE=verify, with references from run 41204703448.

## Released as v0.5.0 (maintainer's decision, 2026-10-31)
The maintainer approved merging and releasing without the loop's 100/100 condition, knowing what SCORECARD.md lists as not proven. Colleagues should get it only after the maintainer has tried it on a real phone.
