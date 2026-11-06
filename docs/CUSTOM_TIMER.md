# Change request: add a third timer type — "Custom" — NOTHING else changes

## 0. The app as it is today (v0.5.0). Build on these facts.
If the code disagrees with anything in this request, stop and report it before changing anything.

- **Home has no sections.** It is one list of `BedCard`s, most urgent first (`BedViews.sorted`). Starting is two `StartTile`s in one row (`StartActions`).
- **A patient is an MRN, typed every time** in `PatientField`, inline on the Start screens: digits only, 5 to 16 (`PatientInput`). There is no patient list, no "existing patient" behavior and no separate MRN screen. The code calls it `bedLabel`.
- **There is no log, no stats and no CSV.** The maintainer removed them: Cerner is the record. Nothing clinical is kept long: finished data goes after a day (`Engine.RETENTION_MS`).
- **The reaction guide belongs to transfusions only.** It is not on repositioning screens or on a round-only alarm. It stays out of everything Custom.
- **Two alarm sources share one engine:** transfusions directly in `Engine`, repositioning through `TurnSource` with the pure rules in `TurnRules`. Custom follows the `TurnSource` shape.
- **The core tasks are numbered 1 to 9.** The new ones are 10 to 12.
- **Layout is always left-to-right and the UI is English** (`supportsRtl="false"`). The Red Hat fonts have no Arabic, so Arabic text uses the system font.
- **The scorecard is not at 100/100 today,** and this request forbids fixing what holds it back. Section 9 is written for that.

## 1. What it is
A general-purpose timer for anything that isn't a transfusion or a turning round (e.g. pain reassessment, IV site check). The nurse types a task name and the patient's MRN, and picks an interval. The alarm repeats at that interval until it is stopped.

Patients and goals change all the time, so EVERY custom timer starts empty:
- no saved templates
- no prefilled name or interval
- no suggestions from previous timers

## 2. Where it lives and how it starts

### Entry points
- **Home:** a third `StartTile`, "Start custom", last in the same row, with the same component and the same `quiet` behavior. At large font sizes it stacks like the others. All three tiles are the same height in every state.
  - Why the same row: at 360 dp the labels wrap to two lines, which adds about 12 dp and leaves all six cards in view on a 360 × 800 phone (list about 531 dp, six cards about 510 dp). A separate row would add 64 dp and cut the sixth card by about 31 dp, breaking task 1.
- **App shortcut:** a third static shortcut, "Start custom", after the existing two.
- Nothing else on Home changes.

### The start screen
One screen, in this order. Same header, panels and pinned area as `TurnStartContent`.
1. **Task name.** A new field. The keyboard opens on it by itself. Its keyboard action is Next, which moves to the MRN.
2. **MRN.** The existing `PatientField`: same validation, digit handling and look. Its only change is one new parameter, `autoFocus: Boolean = true`, which Custom sets to false so it doesn't take the focus from the name. Every existing caller keeps the default.
3. **Interval buttons, which ARE the start buttons:** 15 min · 30 min · 1 h · 2 h · 4 h · 6 h · 8 h · 12 h · Other.
   - A 3 × 3 grid pinned at the bottom, in the style of `PositionButtons`: 64 dp tall, with `PRIMARY_TAG` on the container, and the same enabled and disabled looks.
   - They are active only when both the name and the MRN are valid. Until then a line above them reads "Enter the task name and MRN first".
   - While the keyboard is open the grid steps aside, as `TurnStartContent` does at large font sizes. The MRN's Done key closes the keyboard and brings the grid back, so the fields are never pushed off the screen.
   - **No interval is preselected.** The nurse always chooses one.
   - **"Other"** opens an `AlertDialog`, like the existing "Pick time…": a value with ± 1 h and ± 5 min buttons, kept within 15 min to 24 h, a Start button and Cancel. It opens at 15 min every time, never at a remembered value. This path is outside the budget.
- **Start:** tapping an interval starts the timer and returns to Home.
- **Undo:** every start can be undone for 5 seconds, plus "Undo last" on the card, the same as the other types.

## 3. Tap count
open (1) → Start custom (2) → type the name → type the MRN → tap an interval (3) = started.

- Typing is excluded from the count, including the keyboard's Next and Done keys. A scroll counts as a touch, as it does in `TapBudget`.
- Budget: 4 taps (target: 3), at 360 dp and 412 dp, font scale 1.0.
- This is the second exception to "no typing in a core task", after the MRN. The maintainer decided both.

## 4. Rules

### Name
- Free text, 1–30 characters after trimming spaces, any language. Count code points, so a character is never cut in half. Input past 30 is ignored.
- One line. Control characters are removed on entry.
- A name made only of spaces or invisible characters (Unicode whitespace, U+200B–U+200F, U+061C, U+FEFF) is rejected.
- Label: "Task name — no patient names".
- No app-level autocomplete, history or suggestions. Leaving the screen and coming back shows it empty.
- Arabic, English and mixed text keep the correct letter order. The layout stays left-to-right; alignment follows the text's own direction.

### Schedule
- The start is truncated to the whole minute (`ScheduleCalculator.truncateToMinute`), because the engine groups alarms by the minute.
- Alarms fire at start + n × interval (n = 1, 2, 3 …), in elapsed time. It is not a wall-clock grid: daylight saving and timezone changes don't move the series, and no alarm is skipped or doubled. A manual clock change behaves as it does for the other types.
- A late acknowledgement does NOT shift the series.
- The next alarm always exists, whatever the interval. Do not reuse `TurnRules.LOOKAHEAD_MS` as it is: at 6 h, a 12 h or 24 h timer would have nothing armed.
- **Editing the name or the MRN** changes nothing in the schedule.
- **Editing the interval:**
  - the new series is anchored at the last acknowledgement (or the start, if none yet), truncated to the minute
  - the next alarm is the first anchor + n × new interval that is after now; a past time is never rung
  - an alarm already open stays open until it is acknowledged or the next one fires

### Alarm states (the same as rounds, in `TurnRules`)
- PENDING → RINGING → at the missed timeout the sound stops and the Overdue notification shows, still acknowledgeable → closes as MISSED when the next alarm fires.
- If the phone was off, alarms that passed are closed as MISSED and only the latest one rings.
- If the interval is shorter than the missed timeout, the next alarm closes the previous one first.

### Snooze
- Offered only if it ends before the next alarm, within the existing limit of `Settings.MAX_SNOOZES`.
- There is one Snooze button for everything sounding. If any sounding custom alarm can't be snoozed, the button isn't offered. With no custom alarm sounding, the rule is exactly as it is today.

### Per patient
- Several custom timers are allowed for the same patient.
- Two running timers with the same name for the same MRN are not. Names are compared after Unicode NFC, trimming, collapsing spaces and ignoring case.
- Starting a duplicate opens the existing timer: no new timer, no change to its alarms, no undo entry.
- Renaming onto a duplicate is refused: a line explains it and Save stays off.
- A stopped timer's name is free to use again.

### The timer's screen
A card opens the timer's Detail screen, as with the other types. It is new, and built from the existing `Panel`, `WardHeader`, `CapsLabel`, `PatientButton` and `SecondaryButton`:
- the MRN as the header's key number
- the task name, the next alarm's time, and "every 4 h"
- this timer's alarms from the last day, with their status
- the "End of my shift" chip (below), Edit, and Stop

No transfusion content: no MEASURE / DOCUMENT steps, no End VS, no reaction bar.

### Editing
Edit opens the start form filled in. The MRN sits behind the existing `ChangeRow`, as on the transfusion edit screen. The interval buttons select instead of starting, with the current one marked, and "Save changes" is the primary action. The empty-start rule applies only to new timers.

### Stopping
- The same placement and undo as "End transfusion": pinned at the bottom, in the quiet `SecondaryButton` style, when nothing is due. When an alarm is due, "Done ✓" takes the pinned place and Stop moves into the list.
- Stopping cancels everything pending and open, and clears its Overdue notification.
- A stopped timer leaves Home at once. Undo restores it exactly for 5 seconds. There is no Restart.
- Its row is deleted after `Engine.RETENTION_MS`, so its name, interval and MRN are never reused or suggested.

### End of my shift
- The existing chip's logic (`TurnGrid.endOfShift`), on the Detail screen only, never on the start screen.
- A tap sets the stop time and a second tap clears it.
- No alarm fires at or after it. The timer stops by itself once that time has passed and nothing is open, like `TurnRules.shouldAutoStop`.

### Alarm screen and notification
- Built from the existing alarm components (`AlarmState`, `KeyNumbers`, `SnoozeSlot`, `AlarmPrimary`).
- Shows the custom icon and caps line; the task name, large; the MRN and due time at key-number size (≥ 40 sp); the interval (e.g. "every 4 h"); Snooze; and one big "Done ✓".
- **Several custom alarms:** selectable cards in the `BedRow` pattern, ordered by the earliest start, with one bottom button that names who it confirms.
- **With other types in the same minute:** one screen, ordered Transfusion > Round > Custom.
  - The transfusion screen stays as it is, and gains a one-line button below the round's: "Custom · N due".
  - When the transfusion and the round are done, the custom screen takes over.
  - The reaction bar appears only on a screen that shows a transfusion.
- **Notification:**
  - One custom alarm alone gets a "Done ✓" action. Two alone get one each. More, or a mix, open the alarm screen.
  - The existing actions for transfusions and rounds don't change.
  - The Overdue notification gets "Done ✓" too.
- **Pre-alert:** like a transfusion check, using the setting's value.

### Privacy
- Outside the app (notifications, the alarm over the lock screen, the widget), the MRN and any run of 5 or more digits in the task name are masked by `BedMask`. Inside the app on an unlocked phone they show in full.
- The rest of the task name shows as typed, also over the lock screen. The field's label is the only guard. This is the maintainer's decision; list it in the final report.
- Neither the MRN nor the task name is ever logged.
- Use fake MRNs and fake names only, in code, tests, screenshots and reports.

### Home and widget
- **Home:** custom timers join the same list, on `BedCard`, in the same order. At an equal rank and time, Custom comes after the others, and the order among the others doesn't change.
  - Card: the MRN as the key number, the task name where the check's name goes, the next time, the state pill and the bell or diamond. No rail. `ackLabel` is "Done ✓".
  - They count in the header's "N RUNNING" and in the summary.
- **Widget:** custom timers appear exactly the way the other types do.
- With no custom timer running, both are as they are today.

### Settings
- No new settings.
- On-time tolerance, pre-alert, snooze and missed timeout use the Transfusion feature's values.
- Vibrate-only applies as it does to every type. The custom sound is bundled and has no picker.

### Type cue
- **Icon:** its own (`ic_custom`, `ic_stat_custom`, `ic_shortcut_custom`), on the same 24 dp grid, stroke and round caps as the others.
- **Label:** "CUSTOM".
- **Stripe and tag color:** the existing `muted` token. No new colors, fonts or styles.
- **Sound:** a new bundled `custom_chime.wav`, generated with Python's standard library like `turn_chime.wav`, with nothing downloaded.
- **Vibration:** its own pattern in `AlarmPlayer`.
- **Priority when several sound together:** Transfusion > Round > Custom.

### Data
- New tables only (`custom_timer`, `custom_slot`), through a Room migration from version 3 to 4 with `AutoMigration`. Commit `4.json` and add a migration test.
- Nothing Custom goes into the transfusion, checkpoint or turn tables.

## 5. What must NOT change
- **Design:** the colors, typography, spacing, layout, icons, motion and wording of every existing screen, except the allowed differences in section 8.
- **Behavior:** the Transfusion and Repositioning features, their schedules and reliability, the reaction content, the MRN handling and the settings.
- **The engine:** no second engine, and no changed behavior for the existing types. Adding to it for the third type is required: a third source, its alarm kinds and its branches.
- **Code:**
  - no refactors, renames or formatting-only changes
  - no dependency or SDK upgrades
  - no new permissions
- **Tests:**
  - Existing tests and thresholds stay exactly as they are, and existing test code compiles unchanged. So new parameters have defaults, and `Engine`'s constructor stays as it is (custom queries can live in `WardDao`).
  - The one exception is in section 8: four screenshot references.

If you believe something else should change, write it in the final report as a suggestion. Do NOT do it.

## 6. Plan before code
1. Read SPEC.md, docs/DESIGN_OVERHAUL.md, docs/TEST_LOOP.md, docs/FLOWS.md, docs/MRN_ADDON.md, AUDIT.md, SCORECARD.md and PROGRESS.md.
2. Work on a branch named `custom-timer`, from main. main stays untouched until section 9 is met.
3. Baseline, BEFORE any app code changes:
   - The first commit on the branch only saves this request and adds the branch to the `on: push: branches` list in `.github/workflows/lab.yml`, since the lab has only ever run on `design-overhaul`.
   - Set `lab/config.json` to the full matrix (26, 29, 31, 33, 34, 36) and push with `[lab]` in the commit message. App code is identical to main, so this run is the baseline.
   - Save the results as BASELINE.md, with the run's ID. Keep the audit screenshots from the build's artifact as the reference set, outside git.
   - Some existing checks will fail. Record them; don't fix them.
4. Write CHANGE_PLAN.md: every file and location you will touch, the exact change, and how each trap in section 7 is handled.
5. Save this request verbatim as docs/CUSTOM_TIMER.md and keep PROGRESS.md updated. After any restart, re-read both.
6. Before each long run, say how long it will take. A full-matrix run takes at least 1 hour, and about 2 hours with the custom reliability suite.

## 7. Implementation rules
Touch ONLY the locations listed in CHANGE_PLAN.md. If you discover a missing location, add it to the plan first, then change it.

### Known traps in this code. Each one needs its own test.
1. **IDs collide across tables.** A custom alarm's id can equal a transfusion checkpoint's or a turn slot's. Anything that routes by a bare id would act on the wrong patient's alarm:
   - `Notifications.ackIntent`, `AlarmReceiver.ACTION_ACK` and `Engine.acknowledge`, as reached from the notification, from Home's primary button (`onAcknowledge(urgent.ackIds)`) and from the widget
   - the PendingIntent URIs in `AlarmScheduler` (`tvs://alarm/…` and `tvs://alarm/TURN/…`) and `tvs://ack/…`; Custom needs its own
   - `AlarmReceiver`'s pre-alert branch, which passes the id to `Engine.onPreAlert` as a checkpoint id
   - the notification tags: `syncOverdue` cancels everything under `TAG_OVERDUE` that isn't in its list, so Custom needs its own tags for Overdue and pre-alert
2. **`if`/`else` on the type compiles silently with a third type** and would treat Custom as a turning plan:
   - `BedType.icon`, `tag`, `spoken` and `color` in `Cards.kt`
   - `onOpen` and `ownerKey` in `HomeScreen.kt`
   - the row icon in `BedsWidget.kt`
3. **"Nothing is open" checks would close a custom alarm:**
   - `nothingOpen` in `AlarmActivity` closes the screen
   - `syncRinging` in `Engine` stops the service
   - `Notifications.alarm` and `RingSpec.turnOnly` pick the icon and text
   - All must count Custom, and with no custom alarm they must behave exactly as they do now.
4. **Sound:** `AlarmService.show` switches the player on one boolean (`playingTurn`), and `AlarmPlayer.start` takes one (`turn`). Three types need more, without changing the two existing paths.
5. **Masking has three call sites,** each building its list of active labels: `Engine.masker`, `AlarmActivity` and `BedsWidget`. All three must include custom MRNs, or the shortest unique suffix is wrong.
6. **Navigation:** new routes need entries in both `encode` and `decode` in `MainActivity`, or they don't survive process death.
7. **Home's empty state** must check that the custom list has loaded too, or it flashes the empty card.
8. **Undo:** `UndoStep` and `UndoCodec` need the new steps, and `ownerKey` needs its own prefix, such as "C12". Undo must restore the exact rows.

## 8. Proof that it works and that nothing else broke

### Existing tests
Every existing test passes, unchanged.

### Reliability
Cases A01–A14 and A17–A23 and M01 from `lab/reliability.py` run again for custom alarms on the full API matrix. A15 and A16 check the app as a whole and aren't type-specific, so they run once. Behavior that doesn't exist on an API level is N/A there, as `docs/TEST_LOOP.md` says.

### Schedule
Property-based tests with ≥ 10,000 random cases covering:
- start times × intervals (15 min – 24 h)
- DST transitions: no series shift, no skipped or doubled alarm
- late acknowledgements that do not shift the series
- rescheduling after editing the interval: never in the past; a rename or an MRN change doesn't reschedule
- the missed-before-next rule, including an interval shorter than the missed timeout
- snooze never passing the next alarm
- a next alarm always armed, for every interval up to 24 h

### Tap-budget tests for the new core tasks, in `TapBudget`, with typing marked
| # | Task | Max taps |
|---|------|----------|
| 10 | Start a custom timer — name and MRN typing excluded | 4 |
| 11 | Acknowledge a custom alarm (alarm screen or notification action) | 1 |
| 12 | Stop a custom timer | 3 |

Tasks 1–9 stay within budget, including task 1 at 360 dp with six patients.

### MRN entry parity
On the Custom screen, `PatientField` gives the same results as on the Start screens for valid MRNs, too-short and too-long ones, Arabic digits and non-digits. Existing callers keep their auto-focus.

### Empty-start rule
- Every new custom timer opens with an empty name and no preselected interval, also after leaving the screen half-filled and coming back.
- No suggestions appear, even after many timers have been created and ended.

### Validation
- The interval buttons stay inactive until both the name and the MRN are valid.
- Empty names and names made only of spaces or invisible characters are rejected.

### Per-patient rules
- Duplicate names, including different case and extra spaces, open the existing timer and change nothing.
- The same name on another MRN is allowed.
- Several custom timers for the same patient work side by side.

### Names
- 30 characters; Arabic; mixed Arabic and English; letter order correct.
- At font scale 2.0 on a 360 dp screen, a typical 30-character name wraps to at most 2 lines, on the card and on the alarm screen, by shrinking to no less than 16 sp nominal. It is never clipped.
- A worst-case name of 30 wide letters may take 3 lines, and is never clipped.

### Combined alarms
- Ordering together with transfusion checks and rounds, one screen, with the right sound for what is sounding.
- Several custom alarms in the same minute, ordered by the earliest start.
- With no custom alarm, the alarm screens match the baseline pixel for pixel.

### ID collisions
With a checkpoint, a turn slot and a custom alarm that all have the same id: Done, snooze, pre-alert, cancel, undo and stop on each one leave the other two exactly as they were.

### No record-keeping
Nothing Custom adds a log, stats or an export. A stopped timer's row, and with it the MRN and the name, is gone after `Engine.RETENTION_MS`.

### Stopping
Stopping shows no transfusion-specific content. The "End of my shift" chip appears only on the Detail screen.

### Home and widget
- Custom cards appear only while a custom timer is running, and sort as section 4 says.
- The widget shows custom timers the same way it shows the other types.

### Masking
Custom notifications, the alarm over the keyguard and the widget are masked, including a 7-digit number typed into the name. `dumpsys notification --noredact` and logcat contain neither the test MRN nor the test name.

### Screenshot regression against the baseline
- Allowed differences ONLY:
  - Home's Start row with the third tile; everything pinned above it moving up by the row's extra height; and the list's visible area ending that much sooner
  - custom cards on Home
  - custom timers on the widget
  - the Custom start flow, Detail and alarm screens, and the "Custom · N due" button when a custom alarm is due
- With no custom timers running, every other screen must be pixel-identical to the baseline.
- Of the 19 committed references in `app/src/test/screenshots`, exactly four show Home and may be recorded again, after their differences are shown to be only the ones above: `transfusion_home`, `transfusion_home_empty`, `repositioning_home`, `repositioning_home_only`. The other 15 must pass as they are.

### Scope audit
- A fresh subagent with no build context compares `git diff main...custom-timer` against CHANGE_PLAN.md.
- Every changed line must map to a plan item. Anything else gets reverted.

### Scorecard and reviewers
- The new checks go in as gates, the way `lab/score.py` already treats M01 in `GATES`: pass or fail, worth no points. The 100-point math and the area weights don't change, so the score stays comparable to the baseline.
- Run the loop from docs/TEST_LOOP.md with ALL of its rules, except that the design reviewer and the personas judge only what this change added or moved, and the personas walk through tasks 10–12. Anything they find on untouched screens goes in the report as a suggestion.
- Fast loop on API 34. Full matrix every 3rd iteration and for the final runs.

## 9. Stop condition
- **Merge only when all of these are true, on two consecutive full-matrix runs:**
  - every new check and gate in section 8 passes
  - every scorecard check that passed in BASELINE.md still passes (a check that failed there doesn't block this, and isn't fixed here)
  - on the new screens, the design reviewer gives ≥ 9/10 with no open Critical or Major, and the personas have no concrete finding on tasks 10–12
  - the screenshot regression is clean
  - the scope audit finds zero unplanned changes
- **Then:** merge, tag `v0.6.0`, and release an APK. Before calling it done, download the published file and install it on an emulator over v0.5.0, with a running transfusion and a running turning plan: both keep running and their alarms still fire.
- **If 3 iterations pass without progress:** do NOT merge. Report exactly what blocks it.
- **If the maintainer ends the loop early:** stop, verify what exists, and say plainly which targets weren't met. Merge and release only on an explicit go-ahead.

## 10. Final report (short, in Arabic)
- The baseline score vs the final score, and which checks changed between them.
- The list of changed locations (from CHANGE_PLAN.md).
- Any suggestion you did NOT implement.
- The decisions to confirm: the task name shows over the lock screen.
- "Unverified" items, never counted as PASS: real phones and OEM battery killers, and the keyboard's real height on real phones.
