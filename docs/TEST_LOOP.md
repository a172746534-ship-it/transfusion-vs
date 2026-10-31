# PART 2 — Autonomous test-and-score loop to 100/100

## 0. Ground rules
- NEVER ask me to test anything. You test everything yourself.
- You never grade your own work. Scores come only from binary automated checks and from
  independent reviewers (section 6).
- The thresholds in section 4 are fixed. Never change them.

## 1. Test lab
- Emulators:
  - in GitHub Actions with reactivecircus/android-emulator-runner (KVM)
  - use google_apis system images, so `adb root` works
  - also run an emulator locally if you can
- API matrix: 26, 29, 31, 33, 34 and the latest stable.
- Screen widths: 360dp and 412dp.
- Expectations adapt per API level.
  - Example: the full-screen-intent permission exists only on API 34+, and the
    notification permission from API 33.
  - Behavior Android doesn't have on a given level is "N/A" there — document it.
    It is neither a failure nor a free pass.
- Download every CI artifact with gh (logs, screenshots, screen recordings) and inspect it
  yourself. Actually LOOK at every screenshot.
- Real devices: if FIREBASE_* secrets exist in the repo, also run the alarm-reliability
  suite on real devices in Firebase Test Lab. If they don't exist, don't ask me — list it
  under "Unverified" in the final report.
- CI minutes:
  - fast loop: API 34 only
  - full matrix: every 3rd iteration, and for the final runs
  - if Actions minutes run out or jobs stop starting, STOP and report it. Never fail silently.

## 2. Testing time without waiting hours
- Scheduling logic: inject a Clock, and cover it with unit + property-based tests.
- Instrumented alarm tests: schedule real alarms 1–2 min ahead. For long schedules, move
  the emulator clock with `adb root`.
- Debug-only test hooks are allowed only if they are stripped from the release build.
  The release APK contains no test-only code path.

## 3. Test everything
Build a UI map: every screen × every state × every action — including the widget, the
notification actions and the app shortcuts. Every button, every setting and every state
transition gets an automated test.

### Alarm reliability
- **Timing:** fires on time (±60 s) with the screen off and locked. The full-screen alarm
  is verified on top of the keyguard.
- **Doze:** `dumpsys battery unplug`, screen off, then `dumpsys deviceidle force-idle`.
- **Battery Saver on.** **Do Not Disturb on.**
- **Silent and vibrate modes:**
  - the sound plays on the alarm stream (verify with `dumpsys audio`)
  - the vibration fires (verify with `dumpsys vibrator_manager`)
- **Process killed:** with `am kill`, and by removing the task from recents.
- **`am force-stop`:**
  - Android cancels alarms by design after a force-stop.
  - Test this: the next time the app opens, it restores all schedules and flags anything
    that was missed.
- **App update:** `adb install -r`.
- **Real reboot:** `adb reboot`, then wait for sys.boot_completed. A BOOT_COMPLETED
  broadcast alone does NOT count.
- **Clock:** time change, timezone change, and Egypt's DST transitions.
- **Permissions revoked mid-shift** (notifications, exact alarms, full-screen intent) →
  the app detects it and shows a warning.
- **Alarm volume at 0** → a warning is shown.
- **Pocket guards:**
  - touches are ignored for the first second
  - touches are ignored while the proximity sensor is covered
- **Undo:** both the snackbar and the "Undo last" on the bed card restore the exact
  previous state.
- **Collisions and follow-ups:**
  - a transfusion check and a round in the same minute
  - the missed timeout
  - the overdue notification
- **Sync:** acknowledging from a notification action or from the widget updates everything
  instantly.

### Logic and data
- Unit tests plus property-based tests: ≥ 10,000 random cases of start times, grids,
  intervals and DST dates.
- Room migration tests.
- The CSV export, verified against the database.

### Robustness
- Monkey test restricted to the app package (`-p`).
- Rotation and process death in the middle of every flow: state is restored.
- Double taps and rapid repeats are idempotent: no double start, no double acknowledge.
- The back gesture inside every flow.
- 0 beds, 1 bed, 20 beds.

### Environment
- Dark and light themes.
- Font scale 1.0 / 1.3 / 2.0.
- System language Arabic (ar-EG): times keep exactly the digits and format of the design.
  Choose Latin or Arabic-Indic digits once, and enforce it everywhere.
- Widths 360dp and 412dp.

### Quality gates
Android lint, detekt/ktlint, Compose accessibility checks (ATF), StrictMode,
LeakCanary (debug build), cold start.

## 4. Fixed thresholds
- **Contrast**, in both themes:
  - text ≥ 4.5:1
  - large text and UI components ≥ 3:1
  - key numbers (time, bed) ≥ 7:1
- **Key-number size:** ≥ 24sp on Home cards, ≥ 40sp on alarm screens. Body text ≥ 16sp.
- **Touch targets:** ≥ 56dp; alarm actions ≥ 64dp; ≥ 8dp of space between adjacent targets.
- **Color-blind safety:**
  - under deuteranopia, protanopia and tritanopia simulation, every pair of urgency-state
    colors differs by CIEDE2000 ΔE ≥ 15
  - the states ALSO differ by shape and by text
- **Cold start:** median of 10 `am start -W` runs ≤ 1200 ms on the API 34 CI emulator.
- **Monkey:** ≥ 20,000 events, 0 crashes, 0 ANRs.
- **Alarm timing:** within ±60 s.
- **Truncation:** zero — at font scale 2.0, 360dp width, 20 beds and the longest bed label.

## 5. The 100-point scorecard
- Every point is a binary check: it PASSES with evidence (test name + log line, or a
  screenshot path), or it scores 0.
- No partial credit. "Should work" = 0. Not run = 0.

| Area | Points | Passes only if |
|---|---|---|
| A. Alarm reliability | 25 | every reliability test passes on every API in the matrix |
| B. Scheduling & data correctness | 15 | all unit, property-based, migration and export tests pass |
| C. Tap budget & flows | 15 | all 9 core tasks stay within budget in automated UI tests that count every touch |
| D. Glanceability | 10 | the size, contrast and truncation thresholds pass; one primary action per screen, in the bottom third |
| E. Accessibility | 10 | zero ATF errors; the contrast, touch-target and color-blind thresholds pass; full TalkBack labels |
| F. Visual design & consistency | 10 | zero hard-coded colors or font sizes; both themes complete; both features share components; the design reviewer passes (section 6) |
| G. Robustness & performance | 10 | monkey, zero leaks, zero StrictMode violations, zero lint/detekt errors, cold start, rotation and process-death checks all pass |
| H. Simulated usability | 5 | no persona reviewer has a concrete finding (section 6) |

In SCORECARD.md, split each area into its individual checks, each with its own points.

## 6. Independent reviewers
- Spawn fresh subagents that have NOT seen the build conversation.
- Spawn NEW ones for every re-score. Never reuse the same instance.
- Give them only the screenshots, the screen recordings, the UI tree and the rubric.

### Design reviewer (area F, and the visual side of area D)
- Searches deliberately in every category: hierarchy, spacing, typography, color,
  consistency, states, the night theme, polish.
- Lists EVERY weakness it finds. Each one must be concrete:
  screen + element + screenshot + why it hurts on a ward + the fix.
  Vague remarks don't count.
- Scores out of 10. It gives 10 only if no concrete weakness remains.
- The F check passes at ≥ 9/10 with no open Critical or Major weakness.

### 3 persona reviewers (area H)
Each one is a first-time user who has never seen the app:
1. Day shift, 6 patients, interrupted every minute, one hand free.
2. Night shift, 3 AM, dark room, a sleeping child, exhausted.
3. Gloved hands, mid-procedure, can only glance at the screen.

- Each persona walks through all 9 core tasks using only the screenshots and recordings.
- A finding counts ONLY if it is concrete: screen + element + what they expected + what
  actually happened. A speculative "might be confusing" with no concrete element is rejected.

### Reviewer findings
They go into AUDIT.md and into the loop like any other failure.

## 7. The loop
1. Run the full suite. Write SCORECARD.md: the total out of 100, and every check marked
   PASS or FAIL with its evidence.
2. Fix the failure with the highest clinical risk first: A > B > C > E > D > G > F > H.
3. Re-run the WHOLE suite, not just the test you fixed.
4. Update SCORECARD.md (append the iteration to its history: score, what changed, what's
   next), update PROGRESS.md, and commit.
5. Repeat.

### Stop ONLY when one of these is true
- **100/100 on two consecutive full-matrix runs.**
  → Merge design-overhaul into main, bump the version, and release an APK that installs
    over the current version without losing data.
- **3 consecutive iterations with no increase in the score.**
  → Do NOT merge. Report exactly what blocks each missing point, and what it would take
    to get it.

### Anti-cheating rules (breaking any of them invalidates the run)
- Never delete, skip, @Ignore or loosen a test or a threshold to gain points.
  Never retry a test until it happens to go green.
- A flaky test is a bug: fix its cause.
- Never mark a check PASS without evidence you actually observed.
- Never change a threshold from section 4. If you believe one is wrong, explain why in
  the report and keep it.
- Don't change the alarm engine's behavior unless a reliability test proves it is broken.
- Never edit a reviewer's findings or scores.

## 8. Final report (short, in Arabic)
- The baseline score, the final score, and the SCORECARD history (how the score moved
  per iteration).
- What changed in the app.
- "Unverified": anything no emulator or CI can prove — for example real OEM battery killers
  on Xiaomi/Samsung, or real nurses on a real shift. State it honestly and never count it
  as PASS.
- The merge and the APK — only if the 100/100 stop condition was met.
