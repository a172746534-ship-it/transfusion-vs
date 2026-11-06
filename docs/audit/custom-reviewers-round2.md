# Custom timer: reviewers, round 2 (2026-11-06)

**Who reviewed.** New fresh subagents on the screenshots of build 42227214792 (commit 368fc6c): a design reviewer and three personas (day shift; night 3 AM; gloved hands at font 2.0), tasks 10–12. Before them, a fresh scope auditor checked `git diff main...custom-timer` against CHANGE_PLAN.md.

## Scope audit: 9 places, all handled
| # | Place | Decision |
|---|---|---|
| 1 | AlarmActivity: wait after Undo (shared code) | Kept, limited to a custom Undo (`UndoAction.isCustom`); transfusions and rounds behave exactly as before. Added to U7. |
| 2 | `ui/components/TaskName.kt` not in the plan | Added as U5b |
| 3 | Transfusion alarm set tighter when a custom alarm is due | **Reverted**: §4 says the transfusion screen stays as it is |
| 4 | "Custom · N due" with the MRN and lateness | **Reverted** to the request's words, a one-line button |
| 5 | Custom variants of A13 and A14 | §8 requires A01–A14 for custom: L2 updated |
| 6 | score.py gate lines rewritten | The existing lines print exactly as before; the verdict line is added after them |
| 7 | A second MigrationTest | Added to T2 |
| 8 | Four more Robustness tests | Added to T4 |
| 9 | UiChecks `homeWithCustom` | Added to T4 |

## Personas: taps
- Task 10: 2 taps from Home (3 with the icon). Budget 4.
- Task 11: 1 tap for a single custom alarm, or for the preselected one of several.
- Task 12: 3 taps, also with an alarm open (night persona).

All three said tasks 10–12 are within budget on the main path.

## Fixed after round 2
| Finding | Persona | Fix |
|---|---|---|
| custom_detail_ringing: at 360 dp the Edit / Stop row has slipped below the fold; at f2.0 it's far below | day, gloves | The actions sit right under "Now" / "Next", above the stop-time and alarm panels |
| custom_detail: the "STOP" heading reads like the stop action, over the filled "No stop time" chip | all three | Heading "Stop time" |
| custom_start_valid f2.0: the MRN field is under the pinned grid, so the patient can't be checked before the one tap that starts | gloves | The line above the grid names the MRN: "How often for 7654822? One tap starts it." |
| custom_detail_ringing f2.0: "DUE +5 / min" wraps next to the big time | gloves | At large font the time goes under the state |

## Required by docs/CUSTOM_TIMER.md (kept, reported)
- **Task 11 with a transfusion or round in the same minute takes 2 taps.** "Custom · N due" only opens the custom screen, and its label is the request's own. On a 360 dp phone the transfusion's steps card scrolls under it.
- **Several custom alarms: a non-preselected one takes 2 taps.** At f2.0 the third card is below the fold. This is the `BedRow` pattern.
- **"Other" opens at 15 min with ±1 h / ±5 min,** so a whole number of hours without a preset takes extra taps. §2: "It opens at 15 min every time".
- **Stop is the quiet outline button in Done's pinned place once nothing is due; no Restart.** §4. Stop ignores a tap for 1 s after taking that place (`CustomDetailGuardTest`), and Undo lasts 5 s.
- **The ready interval tiles are filled, also at night.** §2: "in the style of `PositionButtons` … the same enabled and disabled looks".
- **At f2.0 the Start tiles stack and fill much of Home.** §2: "At large font sizes it stacks like the others".

## Untouched screens (suggestions)
- "Snooze 2 min" doesn't say what it covers with several alarms (existing `SnoozeSlot`).
- Home's empty card names only transfusion and turning ("Nothing else on Home changes").
- At f2.0, card pills like "◆ OVERDUE +65 / min" wrap on every type's card (existing `StateText`).

## Design reviewer, round 2: 6.5/10, FAIL (0 Critical, 6 Major, 23 Minor)
| # | Finding | Decision |
|---|---|---|
| 1 Major | home_empty: the empty card names only transfusion and turning | Required: "Nothing else on Home changes" (and no-custom Home must match the baseline). A suggestion. |
| 2 Major | custom_start_valid: no MRN and no first-alarm time before the one tap | Fixed: "How often for <MRN>? One tap starts it." and each interval button shows its first alarm's time |
| 3 Major | Several alarms / Home: the Done button names only the MRN; one patient can have two tasks | Fixed: "<task> · Done ✓" with the MRN |
| 4 Major | Widget: identical Done buttons, masked MRN only | Fixed for custom rows: the task is named. The row layout is the existing widget's. |
| 5 Major | f2.0: "Custom · N due" in the pinned stack pushes the transfusion's MRN under the buttons | Required: §4 "below the round's", the transfusion screen stays as it is |
| 6 Major | Detail: inline stop chips compete with Done; one tap sets 20:00 | The request's single "End of my shift" chip (§4: a tap sets, a second clears), with a line saying On / Off, and "stops 20:00" in the summary |
| 7 | NOW pill touches the time at f2.0 | Fixed: stacked at large font |
| 8 | Edit label differs between states; Edit hidden at 360 | Fixed: "Edit" in both, placed under Now/Next |
| 9 | Header "CUSTOM TIMER" / "STOPPED" | Fixed: "Custom timer running" / "Custom timer stopped" |
| 10 | Stopped: when and why | Fixed: "Stopped at 11:00." / "Stopped at its stop time, 20:00." |
| 11, 12 | "STOP" heading; chip widths | Fixed: "Auto-stop", one chip |
| 14 | Edit: grid without a heading; "EDIT / Edit 7654821" | Fixed: "How often?" and "Edit custom timer" |
| 21 | Counter on the left | Fixed: right-aligned |
| 24 | Overdue row: small white text, not the red chip | Fixed: each row has the single alarm's chip |
| 25 | "3 custom timers due" wraps | Fixed: "3 timers due" |
| 26 | Single alarm: no next time | Fixed: "every 4 h · next 15:00" |
| 13 | "DUE +5 min" vs "ringing" on one screen | Kept: the same words and pills as the other types (§4) |
| 15, 19, 16, 22, 23 | MRN size in ChangeRow; field order; Other opens at 15 min; Custom · N due style and overdue | Required by the request (existing `ChangeRow`; name first; §2; §4) |
| 17 | Disabled − steps | Kept: the app's own disabled style (as `PrimaryButton`'s) |
| 18, 20, 27, 28, 29 | Dialog margins; two field styles at f2.0; widget wording and wrap; tile alignment at 412 dp | Left as Minor: existing components or Home's tile, which the request keeps |
