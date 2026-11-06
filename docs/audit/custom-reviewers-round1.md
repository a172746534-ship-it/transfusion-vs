# Custom timer: reviewers, round 1 (2026-11-06)

**Who reviewed.** Three fresh persona subagents, none of which had seen the build. Each got only the screenshots of build 42192171232 (`build/audit`), the persona and tasks 10–12.
- Day shift, 6 patients, one hand.
- Night, 3 AM, night theme.
- Gloved hands, font 2.0.

**Not yet reviewed.** The design reviewer and the scope audit stopped on a usage limit before reporting. They run again, fresh, on the next build.

**Taps, as the personas counted them:**
- Task 10: 3 (4 if the keyboard has to be closed).
- Task 11: 1 for the alarm screen's own target.
- Task 12: 3 with nothing due.

The lab measured the same on API 34: tasks 10, 11 and 12 take 3, 1 and 3 taps.

## Fixed in 368fc6c

| # | Finding (personas) | Fix |
|---|---|---|
| 1 | custom_start_valid: once the fields are valid, nothing says a tap on an interval starts the timer, unlike "Other" which has Start (all three) | A line "How often? One tap starts it." replaces "Enter the task name and MRN first" |
| 2 | custom_start_other: "− 1 h" / "− 5 min" look tappable at the 15 min minimum (all three) | The steps look and act disabled at 15 min and 24 h |
| 3 | custom_alarm_3: order 11:00, 11:05, 10:50, so the most overdue is last and not preselected; no overdue mark (all three) | Earliest due first (same minute: earliest start); an overdue row has a red edge and "◆ OVERDUE +n min" |
| 4 | "Custom · N due" gives no patient and no lateness, unlike "Start round · OVERDUE +65 min" above it (all three) | Tried in 368fc6c, then reverted: the scope audit held it to the request's wording (see below) |
| 5 | custom_alarm_with_transfusion: "in Cerner, right away" cut off above the new button (day, gloves) | Tried in 368fc6c, then reverted: the request says the transfusion screen stays as it is (see below) |
| 6 | custom_detail_ringing: no Stop in view at 360 dp (day, gloves) | With an alarm open, Edit and Stop share one row, both in view |
| 7 | Detail: Stop takes Done's exact place once the alarm is done, so a second tap could stop the timer (night, gloves) | Stop ignores taps for 1 s after taking that place (`CustomDetailGuardTest`) |
| 8 | Detail: "End of my shift" has no visible on/off state; the caption reads as if it were on (day, night) | A "Stop" panel with "No stop time" / "End of my shift (20:00)", as on the turning plan's start screen |
| 9 | Detail: "NEXT" above an alarm that is already due (night) | "Now" while an alarm is open |
| 10 | custom_detail_upcoming f2.0: "10:3(" clipped (gloves) | The time column grows instead of clipping |
| 11 | custom_start f2.0: the task-name label wraps through the box's border (gloves) | At large font the label sits above the box, word for word |
| 12 | custom_start_other f2.0: dialog text ignores the font setting (gloves) | A screenshot artifact: a dialog is its own window. The harness now sets the real font scale |

Found by the lab in the same iteration:
- **A19@custom:** after Undo, the alarm screen closed before the restored alarm reached it. It now waits up to 2 s after a custom Undo (only a custom one, so transfusions and rounds behave as before).
- **UI checks:** a 30-character name scrolled out of the single-line field, so the field now wraps; at font 2.0 the alarm row broke the name mid-word, so the name now gets its own line.
- **A14@custom:** the lab case didn't sleep the screen first.

## Required by docs/CUSTOM_TIMER.md, so not changed (listed in the final report)
- **"Custom · N due" has only a count**, and on a 360 dp phone the transfusion steps card scrolls under it ("in Cerner, right away" under the fade, still reachable). §4: "The transfusion screen stays as it is, and gains a one-line button below the round's: Custom · N due". The scope audit flagged both attempted changes as beyond the plan.
- **Task 11 with a transfusion or round in the same minute takes 2 taps** ("Custom · N due", then Done). The request orders Transfusion > Round > Custom on one screen.
- **Picking another of several custom alarms takes 2 taps** (its card, then Done). This is the `BedRow` pattern the request names, and transfusions work the same way.
- **An interval tap starts the timer at once, with no review step.** The request makes the interval buttons the start buttons. Undo (5 s, and "Undo last" on the card) is the safety net, and the new line says so up front.
- **Stop looks like Edit (quiet outline); no Restart after a stop.** The request asks for the quiet `SecondaryButton` and "There is no Restart".
- **At font 2.0 the Start tiles stack and fill about half of Home** (gloves). The request says "At large font sizes it stacks like the others".
- **Home's custom card doesn't show the interval** (gloves). The request puts the task name where the check's name goes. Detail shows "every 4 h".

## Untouched screens, so out of scope (suggestions)
- **Home's empty card** names only transfusion and turning (day). "Nothing else on Home changes."
- **"Snooze 2 min"** doesn't say it snoozes everything sounding (day). This is the existing `SnoozeSlot`.
- **The "DUE +5 min" pill stays bright white in the night theme** (night). This is the existing `AlarmState`, the same on transfusion alarms.
- **A long MRN drops the type word on a card** (night). This is the existing card rule (`LONG_MRN`).
