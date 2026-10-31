# Reviewers, round 1 (screenshots of commit 0abd136). Word for word, unedited.

# Persona 1 walkthrough: day-shift peds-onc nurse, 6 patients, one hand free, no training

Screens used: mainly `*__w412_f10`; also w360_f10 / w360_f20 where noted. Touch counts include the app icon (touch 1) where the task starts from the launcher.

## Walkthrough table

| # | Task | Budget | Screens and touches (from the screenshots only) | Count | Verdict |
|---|------|--------|--------------------------------------------------|-------|---------|
| 1 | See all beds, what's next, what's late | 1 | icon (1) -> `home_6beds`: all 6 beds fit on one screen, header "1 overdue - 2 due now", cards sorted by time, pills OVERDUE / DUE NOW / SOON / "in 55 min". | 1 | Within budget. Lateness reading of DUE NOW cards: see F1. |
| 2 | Acknowledge a transfusion check from the alarm | 1 | `alarm_1bed`: big gold "Measured & documented" (1) -> `alarm_done_undo` ("Bed 14 - 15-min check done" + Undo). With two beds (`alarm_2beds`) it is one tap per bed. | 1 (1 bed) / 1 per bed | Within budget. |
| 3 | Whole turning round, all as suggested | 2 from alarm | `round_alarm`: "Start round" (1) -> `round_checklist`: "All turned as suggested" (2). | 2 | Within budget. If a transfusion check rings at the same time: 3 (F7). |
| 4 | Start a transfusion | 4 | icon (1) -> `home_6beds` "Start transfusion" (2) -> `start_empty`: "Bed 5" (3) -> `start_bed_picked`: "Baseline done - Start" (4); an `undo_bar_on_home` appears. | 4 | At budget exactly. Planned duration is never seen on the way (F2). |
| 5 | Start a repositioning plan | 4 | icon (1) -> "Start turning" (2) -> `turn_start`: "Bed 6" (3) -> `turn_start_bed_picked`: "Left 30 / Back / Right 30" (4). | 4 | At budget exactly. |
| 6 | End a transfusion | 3 | icon (1) -> tap the bed card on Home (2) -> `detail_ringing`: "End transfusion" (3) -> `detail_ended_end_vs`. End-VS "Measured & documented" would be touch 4. | 3 (+1 for end VS) | At budget, but only if the nurse guesses that the card is tappable (F3). |
| 7 | Suspected reaction -> see steps | 2 | Home: icon (1) -> red "SUSPECTED REACTION?" bar (2) -> `reaction_1bed`: DO NOW steps 1-5 + "STOP & CALL IF YOU SEE". From a transfusion alarm: red bar (1). | 2 from Home / 1 from alarm | Within budget. Steps are readable at once with 1 bed (F11 for many beds). |
| 8 | Mark "Transfusion stopped" for a specific bed | 3 | icon (1) -> red bar (2) -> `reaction_1bed` "Transfusion stopped - Bed 1" or `reaction_20beds` "TRANSFUSION STOPPED FOR:" bed buttons (3) -> `home_stopped_reaction` ("STOPPED - REACTION"). | 3 | Within budget (a scroll inside the bed panel may be needed with many beds). |
| 9 | One bed in a round "can't turn" + reason | 3 from alarm | `round_alarm` "Start round" (1) -> `round_checklist` Bed 7 "Can't turn" (2) -> reason chip (3). Closing the round with the other beds is another "All turned as suggested" = 4. | 3 (4 to close the round) | At budget, but the reason chips are partly off-screen (F5) and the footer wording is ambiguous (F6). |

## Concrete findings

**F1. Home card "DUE NOW" hides how late a check is and has no one-touch action (tasks 1, 2).**
Screen `home_6beds` (also `home_20beds`). Elements: Bed 3 card "10:55 - DUE NOW" and Bed 14 card "11:05 - DUE NOW" under header clock 11:05. Expected: a check that is 10 min late reads differently from one due this minute, and, like the OVERDUE card, offers the gold "Measured & documented" button. Actual: Bed 3 (10 min past) and Bed 14 (0 min) carry the identical dark "DUE NOW" pill, no "+N min", no red border, and no inline button. Only the Bed 12 card ("OVERDUE +15 min") has the button. In `home_20beds` all 20 cards are "DUE NOW" at times from 10:17 to 11:05 (up to 48 min before the 11:05 clock) with no lateness shown. The nurse has to do the clock arithmetic herself, and the quick action appears only past an unexplained cutoff. Severity: medium.

**F2. Start-transfusion: the planned duration is below the fold and the Start button is already active (task 4).**
Screen `start_bed_picked` at w412_f10. Element: "Baseline done - Start" button (active after only picking Bed 5). Expected: the duration that will be used (and so the End-check time and alarms) is visible before the final tap. Actual: only "WHICH BED?" and "STARTED AT" are visible, and the second row of start-time chips is cut mid-chip by the footer. The "PLANNED DURATION" row (1 h / 2 h / 3 h / 4 h / Custom, with "2 h" pre-ticked) and the "TIME FROM START" preview appear only in the full-page capture `start_bed_picked__w360_f20`. A nurse following the 4-touch path starts every transfusion with a default she never saw (other screens show 1 h and 2 h transfusions). Severity: medium-high.

**F3. No visible way to open a bed or end a transfusion from Home (task 6).**
Screen `home_6beds`, cards Bed 3 / 14 / 21 / 7 / Isolation Bed 23. Expected: a visible affordance (chevron, "Open", "End") for the route to "End transfusion". Actual: the cards show no chevron or button (only the OVERDUE card has an inline "Measured & documented" button). "End transfusion" exists only at the bottom of the bed detail (`detail_ringing`), which is reached by tapping the card body. The only icon on Home is the sliders icon at top right, which leads to settings. A first-time user has no visible hint that the card is the way in. Severity: medium.

**F4. A stale "missed" check keeps an active gold button after the transfusion has ended or been stopped (tasks 6, 8).**
(a) `detail_ended_end_vs`: the "END VS - BED 12" panel has a gold "Bed 12 - Measured & documented" button, and the Checkpoints list below it has a second gold button with exactly the same label for the "missed" 30-min check. The two buttons have different meanings and nothing tells them apart. At w360_f20 they are stacked back to back.
(b) `detail_stopped_reaction`: the red banner says "Remaining alarms for this bed are cancelled", yet the 30-min check row (10:50 "missed") still shows the active gold "Bed 12 - Measured & documented" button.
Expected: after end/stop only the intended action (end VS, or nothing) is a primary button, or the retrospective one is labelled as such. Actual: a stale check looks like the main action. Severity: medium.

**F5. Reason chips for "Can't turn" are cut off by the sticky footer (task 9).**
Screen `round_checklist` at w412_f10. Elements: Bed 7 "Can't turn" (selected, white) and its reason chips. Expected: after touch 2 all reasons are visible so touch 3 is one tap. Actual: only "Off unit" and "Procedure" are visible, cut in half by the footer. The other three reasons (Asleep/comfort, Unstable, Other) appear only in the full-page capture `round_checklist__w360_f20`. For Bed 9 or Isolation Bed 23 the chips would sit even lower. A one-handed nurse has to scroll the list between touch 2 and touch 3. Severity: medium.

**F6. Footer "All turned as suggested" is unchanged while one bed is marked "Can't turn" (task 9).**
Screen `round_checklist`. Element: gold footer button. Expected: once Bed 7 is in "Can't turn" state, the footer shows that not all beds are turned (relabel, or "3 of 4 turned"), or it is clear that Bed 7 stays excluded. Actual: Bed 7's "Can't turn" is selected and the footer still says "All turned as suggested", which reads as if tapping it will record Bed 7 as turned too. It is also unclear whether choosing a reason finishes the round or needs another tap (this is the 4th touch in the table). Severity: medium.

**F7. Transfusion check plus due round: no way to the round from this screen, and the round's lateness is not shown (task 3 variant).**
Screen `alarm_transfusion_and_round`. Element: plain text "Then: Turning round - 10:00 - Bed 3, Bed 7". Expected: a nurse standing at a bed who needs to turn can open the round, and sees it is late. Actual: only the transfusion buttons are offered; the round line is not a button and shows a bare "10:00" (no "late", while the check above is 11:07). Round-only completion costs 3 touches here (ack check, Start round, All turned) against the budget of 2. Severity: low.

**F8. Widget says "Done" where the app says "Measured & documented" (task 2).**
Screen `widget__w412_f10`. Element: gold button on the Bed 12 row. Expected: the same wording as the alarm and Home buttons ("Measured & documented"), because tapping it records that vitals were measured and charted. Actual: it says "Done ✓", which could also mean "I've seen it". Severity: low.

**F9. The last chip of the timeline is clipped (tasks 1, 6).**
Screens `detail_overdue_missed` and `detail_on_time_late` (TIME FROM START, 5th chip "12:20"); `turn_detail_on_track` and `turn_detail_stopped` (WARD CLOCK, 5th chip "16:00"), all at w412_f10. Expected: the chip is drawn whole inside the card. Actual: the chip's right edge/border is cut off at the card edge when there are 5 nodes. Severity: low.

**F10. Large font on a small phone breaks the bed detail layout (tasks 1, 6).**
Screen `detail_ended_end_vs__w360_f20` (the same 15 min / 30 min labels in `start_bed_picked__w360_f20`). Expected: no mid-word breaks. Actual: the Checkpoints times wrap as "10:2 / 0" and "10:3 / 5", "Documented" breaks as "Docum / ented", the timeline labels "15 min" and "30 min" run together ("15 min30 min"), and the 4th chip is truncated ("12:"). Severity: low-medium.

**F11. The pinned bed picker on the reaction screen hides most of the "STOP & CALL IF YOU SEE" list (tasks 7, 8, many running beds).**
Screen `reaction_20beds` at w412_f10. Element: pinned "TRANSFUSION STOPPED FOR:" panel. Expected: the signs list is readable at the same time as the picker. Actual: the panel takes the lower third; the list is cut after 3 of 7 bullets ("Breathless, wheeze"), and the bed buttons are cut at "Bed 5 / Bed 6", so Bed 14 or 23 needs an inner scroll. With 1 bed (`reaction_1bed`) everything fits, so this applies when several transfusions run. Severity: low for a 6-bed ward.

**F12. A bare time on the stopped card (task 8).**
Screen `home_stopped_reaction`. Element: "10:58" top right of the red "Bed 12 VS - STOPPED - REACTION" card. Expected: the time is labelled ("stopped 10:58"), since on every other card that position is the next-check time. Actual: unlabelled 10:58 beside "Transfusion stopped", which repeats the card title. The card gives no hint of the next step (the "Document in Cerner" step is only on the guide). Severity: low.

## Checked and not counted
- `alarm_snoozed` says "Snoozed - rings again at 11:07" while the due time shown is also 11:07 (and Home clock 11:05 shows the same bed as DUE NOW). This looks like a capture-clock artifact, so it is not reported.
- The reaction guide on transfusion screens only, and no reaction button on round/turn screens, are maintainer decisions and are not findings.
- "Start round" -> "All turned as suggested" in 2 touches, "Measured & documented" in 1 touch, and the 4-touch starts for transfusion and turning all meet their budgets.
- No confirmation or undo screen for "End transfusion" or "Stop plan" appears in any screenshot; I cannot tell from the images whether one exists, so no finding is raised.

# Persona 2 walkthrough: night-shift nurse, 3 AM, first time with the app

Source: screenshots only (mostly `w412_f10_night`; `w360_f20` full-page captures used to see content below the fold). Fixture clock on Home is 11:05.
Touch count: the app icon is touch 1 where a task starts from the launcher. Alarm-start tasks count from the alarm.

## Walkthrough table

| # | Task | Screens used | Touches I would make | Count | Budget | Result |
|---|------|--------------|----------------------|-------|--------|--------|
| 1 | See all beds, next, late | `home_6beds`, `home_both_features` | 1 app icon. Header "6 BEDS RUNNING / 1 overdue, 2 due now", cards sorted by time, overdue = red outline + "OVERDUE +15 min", later = "in 55 min". | 1 | 1 | PASS. About 7 cards fit; with 20 beds (`home_20beds`) only 7 are visible but the header count still tells the story. |
| 2 | Acknowledge a transfusion check from the alarm | `alarm_1bed` then `alarm_done_undo` | 1: "Measured & documented" (big gold button). Confirmation screen "Bed 14 15-min check done" + Undo. | 1 | 1 | PASS. Two beds due (`alarm_2beds`) = 1 per bed, so 2 touches. |
| 3 | Whole turning round, all as suggested | `round_alarm`, `round_checklist` | 1: "Start round". 2: "All turned as suggested" (sticky footer, no scroll needed). | 2 | 2 | PASS for a round-only alarm. FAIL when a transfusion check rings at the same time (finding 1): 3. |
| 4 | Start a transfusion | `home_empty`, `start_empty`, `start_bed_picked` | 1 icon. 2 "Start transfusion". 3 tap Bed N. 4 "Baseline done · Start". | 4 | 4 | PASS, zero slack. |
| 5 | Start a repositioning plan | `home_empty`, `turn_start`, `turn_start_bed_picked` | 1 icon. 2 "Start turning". 3 tap Bed N. 4 tap "Left 30° / Back / Right 30°" under "BED 6: LYING NOW? TAP TO START". | 4 | 4 | PASS, zero slack. |
| 6 | End a transfusion | `home_*`, `detail_upcoming`, `detail_ended_end_vs` | 1 icon. 2 tap the bed card (assumed tappable; no screenshot shows the tap). 3 "End transfusion". No confirm screen exists in the set. | 3 | 3 | PASS, zero slack. The End VS then needs a 4th tap (see finding 6). |
| 7 | Suspected reaction, see the steps | `home_*` or `alarm_1bed`, `reaction_1bed` | 1: red banner "SUSPECTED REACTION? STOP the transfusion & call". The steps (DO NOW 1-5) are on screen immediately. | 1 (2 from launcher) | 2 | PASS. |
| 8 | Mark "Transfusion stopped" for a specific bed | Home, `reaction_20beds`, `home_stopped_reaction` | 1: banner. 2: "Bed N" in "TRANSFUSION STOPPED FOR:". With one bed the button is "Transfusion stopped - Bed 1". Result card "STOPPED - REACTION". | 2 from Home (3 from launcher) | 3 | PASS with 1-4 beds. Beds 5+ are cut off (finding 4): 3-4 from Home. |
| 9 | Mark one bed "can't turn" with a reason | `round_alarm`, `round_checklist` | 1 "Start round". 2 "Can't turn" on the bed. 3 reason chip. | 3 for the first bed; 4+ for the 2nd bed or later | 3 | PASS only for the top bed. FAIL for later beds (finding 2). |

## Concrete findings

1. **Alarm with transfusion + round: the round cannot be opened directly.** Screen `alarm_transfusion_and_round`. The line "Then: Turning round - 10:00 - Bed 3, Bed 7" is plain text, not a button. The only actions are "Measured & documented ✓" (Bed 14), "Snooze 2 min" and the reaction banner. Expected: a way to go straight to the round (task 3 budget 2). Actual: to reach "Start round" I must first tap "Measured & documented" for a check I may not have done yet, so task 3 costs 3 touches and forces a documentation tap first.

2. **"Can't turn" reasons are cut off by the sticky footer for any bed that is not first.** Screen `round_checklist`, Bed 7 expanded. After tapping "Can't turn", the chips "Off unit" and "Procedure" sit at the very bottom edge, half hidden behind the "All turned as suggested" footer. The other chips ("Asleep/comfort", "Unstable", "Other", visible only in `round_checklist__w360_f20`) are off-screen. Expected: all reasons visible after one tap (task 3-touch budget). Actual: I must scroll the list first. Beds 9 and Isolation 23 are also below the fold from the start (only Bed 3 and Bed 7 fit), so task 9 is 4+ touches for the 2nd bed or later.

3. **The selected "Can't turn" button and the reason chips look the same.** Screen `round_checklist`, Bed 7. "Can't turn" (selected) is a solid white fill; the choices below it ("Off unit", "Procedure") are also solid white. Same in `round_checklist__w360_f20` for "Other position" / "Back". Expected: options look different from the selected state. Actual: I can't tell if a reason is already chosen, or which of the three white items I have to tap.

4. **Reaction bed picker is a small strip at the bottom with the bed rows clipped.** Screen `reaction_20beds`. "TRANSFUSION STOPPED FOR:" shows Bed 1-4 and half of Bed 5/6 in about the bottom 22% of the screen. Above it, the "STOP & CALL IF YOU SEE" card is cut off after the 3rd sign ("Breathless, wheeze"). Expected: the bed I need is visible and tappable in one touch (task 8 budget 3). Actual: 5th and later running beds need a scroll inside the strip, in an emergency. In `reaction_20beds__w360_f20` only 4 beds fit.

5. **DUE NOW cards on Home have no acknowledge button, but OVERDUE cards do.** Screens `home_6beds` (Bed 12 overdue has "Bed 12 · Measured & documented ✓"; Bed 3 and Bed 14 "DUE NOW" have none) and `home_ringing`. Expected: the more urgent, currently ringing state is at least as quick to acknowledge. Actual: from Home it takes 1 tap when overdue but icon + card + button (3 from the launcher) when due now.

6. **Two identical gold "Bed 12 · Measured & documented ✓" buttons on one screen, with different meanings.** Screen `detail_ended_end_vs`: one under "END VS - BED 12" (y ~855), one under the missed "30-min check" (y ~1760). Neither says which check it closes. The same pattern is on `detail_stopped_reaction` ("Remaining alarms for this bed are cancelled" but a live gold button under "30-min check - missed"). Expected: the End VS button is labelled for end VS and no live button sits under a cancelled/missed row. Actual: I can't tell which button to press for the end VS.

7. **Home card for an ended transfusion does not say it ended or that End VS is due.** Screen `home_end_vs_pending` (and `__w360_f20`). The card reads "30-min check, 10:50, OVERDUE +15 min", and the header says "1 BED RUNNING", while the detail screen says "ENDED - Transfusion ended at 10:58 - END VS". The red marker is on the track's last node, but the text names a different check. Expected: "Ended / End VS due". Actual: the text says 30-min check, so I don't know what the gold button on that card documents.

8. **"End transfusion" sits about 10 dp under the red "SUSPECTED REACTION?" banner.** Screens `detail_ringing`, `detail_upcoming`, `detail_on_time_late`. Both are full-width, stacked at the bottom, one thumb-width apart. A tired thumb aiming at the banner can land on "End transfusion", which is silent and cancels the remaining alarms. No screenshot shows a confirm or Undo after ending, while starting does get an Undo toast (`undo_bar_on_home`). Expected: a mistaken tap on End is recoverable or separated. Actual: nothing visible.

9. **Planned duration is hidden when I press Start.** Screen `start_bed_picked` (412): after picking Bed 5 the visible area ends with half-cut "15 min ago / Pick time..." chips above "Baseline done · Start". The pre-selected "2 h" and the schedule only appear lower down (`start_bed_picked__w360_f20`). Expected: what I am committing to (duration, end time) is visible near the Start button. Actual: I press Start on a 2 h plan I have not seen. (Editable later via "Edit start time or duration".)

10. **The "transfusion started - Undo" bar covers the Start buttons.** Screen `undo_bar_on_home`. The bottom stack is the reaction banner plus the toast "Bed 5 · transfusion started [Undo]"; "Start transfusion" and "Start turning" are not visible. Expected: I can start the next bed straight away (common when several bags are hung together). Actual: the buttons are hidden while the toast shows.

11. **The same acknowledge action has four different labels.** Alarm: "Measured & documented ✓" (`alarm_1bed`). Two-bed alarm: "✓ Bed 12 - measured & documented" (`alarm_2beds`). Home/detail: "Bed 12 · Measured & documented ✓". Widget: "Done ✓" (`widget`). Expected: one wording. Actual: the widget's "Done ✓" drops the measure + document prompt that the alarm box spells out ("MEASURE T·HR·RR·BP·SpO₂ / DOCUMENT in Cerner, right away"), and the check mark moves from end to start.

12. **The widget is white in the night variant.** Screen `widget__w412_f10_night`. The whole widget is a white panel with dark text while every app screen in the same variant is near-black. Expected: dark at 3 AM in a dark patient room. Actual: a full-white rectangle on the home screen.

13. **Overdue is a different colour and word on Home vs the detail screens.** Home: red "OVERDUE +65 min" (Bed 7 TURN) and "OVERDUE +15 min" (Bed 12). `turn_detail_overdue_missed`: the 10:00 row's "overdue" is a light-blue pill (the colour Home uses for "DUE NOW" and the detail uses for "ringing"), while the 08:00 row is red "missed". `detail_overdue_missed` (Bed 12) calls the same 10:50 check "missed" in red while Home calls it OVERDUE. Expected: one word and colour per state. Actual: a blue "overdue" looks less urgent than a red "missed", and "missed" suggests it is too late to document, yet a gold button sits right under it.

14. **"Longest gap without a turn (24 h)" is a red statistic, not what I need.** Screens `turn_detail_on_track` (Bed 3, on time, "2 h 19 min" in red), `turn_detail_skipped` ("4 h 25 min"). Expected: how long the child has been in the current position now. Actual: a retrospective longest-gap figure in alarm red even when the bed is on track. This also reads as a statistic, which the maintainer decided not to have.

15. **Two-bed transfusion alarm is not ordered by due time.** Screen `alarm_2beds`: Bed 12 (11:20, 1-h check) is listed above Bed 14 (11:07, 15-min check). Home lists strictly by time. Expected: earliest/most late first. Actual: the later check is on top.

16. **Timeline strips are clipped or collide.** Screens `detail_overdue_missed` and `detail_on_time_late` (5 nodes): the last chip "12:20" has its right border cut off at the card edge. `turn_detail_on_track`: "16:00" is cut the same way. `start_bed_picked__w360_f20`: the labels "15 min30 min" run together and the last chips are cut ("1", "12:"). Expected: all nodes fully visible. Actual: truncated at the card edge.

## Noticed, not counted (staging, not concrete defects)

- `undo_bar_on_home` toast says Bed 5 started but Bed 5 is not in the list and the header says "2 BEDS RUNNING". I can't tell a bug from a staged screenshot.
- The round fixtures disagree on beds and positions (`round_alarm` Bed 3/7/9/23 vs "Bed 3, Bed 7" on the combined alarm vs Home), and `turn_start` says "10:00 is less than 30 min away" while Home clocks read 11:05.
- `alarm_snoozed` "rings again at 11:07" equals the due time shown above it (2 min after the 11:05 fixture clock).
- No screenshot shows a bed row after "Turned to X" or "Can't turn" + reason, so I couldn't judge the done state or whether "All turned as suggested" still applies after one "can't turn".
- Not flagged, per maintainer decisions: no reaction banner on round-only screens, no CSV/statistics feature, English UI, frozen clinical wording.

# Persona 3 walkthrough: gloved nurse, first-time user, screenshots only

Viewed (w360 first, then w412, f20 and night where they differ): home_*, alarm_*, round_*, start_*, turn_*, detail_*, turn_detail_*, reaction_*, undo_bar_on_home, widget. Touch counts include the app icon (touch 1) only when the task starts from the launcher.

## Walkthrough table

| # | Task | Screens and touches | Touches | Budget | Verdict |
|---|------|---------------------|---------|--------|---------|
| 1 | See all beds, next, late | Icon -> Home (home_both_features). Header "5 BEDS RUNNING 11:05 / 2 overdue", cards sorted by time, red OVERDUE +65 min / +15 min badges, SOON / "in 55 min" labels. | 1 at w412. At w360 the 5th bed (Isolation Bed 23) is hidden, so 2 (see F1). | 1 | OK at 412, fails at 360 |
| 2 | Acknowledge transfusion check from alarm | alarm_1bed -> amber "Measured & documented" (alarm_2beds: one amber button per bed, label carries the bed number). Then alarm_done_undo ("Bed 14 - 15-min check done" + Undo). | 1 per bed | 1 | OK from the alarm. From Home on a DUE NOW card it is 2 (F5). |
| 3 | Whole turning round, all as suggested | round_alarm -> "Start round" (1) -> round_checklist -> "All turned as suggested" (2). | 2 | 2 | OK for a round-only alarm. A combined transfusion+round alarm costs 3 (F2). |
| 4 | Start a transfusion | Icon (1) -> "Start transfusion" (2) -> bed chip (3) -> "Baseline done - Start" (4). Undo toast follows. | 4 | 4 | At budget. No slack for the scroll in F12. |
| 5 | Start repositioning plan | Icon (1) -> "Start turning" (2) -> bed chip (3) -> lying position under "BED 6: LYING NOW? TAP TO START" (4). | 4 | 4 | At budget. |
| 6 | End a transfusion | Icon (1) -> Bed card on Home (2) -> detail_ringing "End transfusion" (3). No confirm screen in the set, so I assume none. The end-of-transfusion vitals need one more "Measured & documented" (detail_ended_end_vs). | 3 (+1 for end VS) | 3 | At budget |
| 7 | Suspected reaction steps | Home or any alarm -> red "SUSPECTED REACTION? STOP the transfusion & call" -> reaction_1bed. 5 numbered steps plus a symptom list. | 1 from Home or alarm, 2 from launcher | 2 | OK |
| 8 | "Transfusion stopped" for a specific bed | Icon (1) -> red banner (2) -> "TRANSFUSION STOPPED FOR:" bed button (3). With 1 bed the button reads "Transfusion stopped - Bed 1". | 3 (2 from Home) | 3 | OK with few beds. With many beds it needs a scroll (F6). |
| 9 | One bed "can't turn" with reason | round_alarm -> "Start round" (1) -> that bed's "Can't turn" (2) -> reason chip: Off unit / Procedure / Asleep/comfort / Unstable / Other (3). The remaining beds still need "All turned as suggested" (+1, not in budget). | 3 | 3 | At budget |

## Findings (15)

1. **Home, 5th and later beds hidden at w360 (task 1).** Screen home_both_features and home_6beds at w360_f10. The sticky stack (red SUSPECTED REACTION banner plus the two Start buttons) takes about 20% of the screen height. I expected all running beds visible after touch 1, as at w412. At w360 only 4 of 5 cards show. The 5th, "Isolation Bed 23 TURN", is a clipped sliver (just a "T") under the banner. In home_6beds the 5th card is cut mid-progress-line and the 6th is not visible. Seeing everything needs a scroll, so 2 touches against a budget of 1.

2. **Combined alarm, the round is not tappable (task 3).** Screen alarm_transfusion_and_round (w360, w412). The round is only a bold text line, "Then: Turning round - 10:00 - Bed 3, Bed 7". The only actions are "Measured & documented", "Snooze 2 min" and the reaction banner. I expected a way to start the round from this alarm (budget 2). Actually the nurse must acknowledge the transfusion first and then reach the round alarm, which is 3 touches. The round is also much later than the check (10:00 against 11:07) but is shown as an afterthought.

3. **Home, STOPPED card and SUSPECTED REACTION banner look the same (tasks 7 and 8).** Screen home_stopped_reaction, w360 and night. The "Bed 12 VS STOPPED - REACTION" card and the "SUSPECTED REACTION?" button are the same solid red (at night the same maroon with the same coral outline). I expected the record card to look different from the control. Actually a glance cannot tell which of the two red blocks is the button, and the card sits directly above the banner.

4. **Stopped-bed detail still offers a check button and a STOP banner (task 8).** Screen detail_stopped_reaction, w360 and w412. The banner says "Remaining alarms for this bed are cancelled". Yet the row "10:50 30-min check missed" still carries the amber "Bed 12 - Measured & documented" button. The red "SUSPECTED REACTION? STOP the transfusion & call" banner is also still pinned at the bottom of an already-stopped bed. I expected no action button on a cancelled check, and no "STOP the transfusion" prompt on a stopped bed.

5. **Home, DUE NOW card has no one-tap acknowledge (task 2 from Home).** Screens home_ringing, home_6beds (w360). OVERDUE cards (Bed 12, +15 min) carry the amber "Measured & documented". DUE NOW cards (Bed 14 11:07 in home_ringing; Bed 3 "10:55 DUE NOW" at 11:05 in home_6beds, so 10 min late) have no button. The nurse must tap the red "Alarm ringing - tap to open" bar, then the button, which is 2 touches. Bed 3 is 10 min late yet is not labeled OVERDUE, and the cut-over is unexplained.

6. **Reaction guide, bed picker clipped when several transfusions run (task 8).** Screen reaction_20beds at w360_f10 and w412_f10. The "TRANSFUSION STOPPED FOR:" panel is pinned at the bottom and shows 2 columns by 2 rows plus a half row. "Bed 5 / Bed 6" are cut in half at the screen edge. At w360 the panel also hides most of the "STOP & CALL IF YOU SEE" list. I expected the target bed to be reachable in the budgeted 3 touches. Reaching a bed beyond Bed 6 needs a scroll inside the small panel (the worst case is the 20-bed fixture).

7. **End transfusion sits 20 px below the reaction banner (task 6).** Screens detail_ringing and detail_overdue_missed at w360_f10. The red banner and the dark "End transfusion" button are separated by about 20 px on the 900 px render, roughly 8 dp, and both sit at the thumb's lower edge. Gloved thumbs can hit the wrong one, and ending a transfusion is the costlier error. The same 8 dp gap separates "Snooze 2 min" from the banner in alarm_1bed.

8. **Home card after ending a transfusion says the wrong thing (task 6).** Screen home_end_vs_pending (w360) against detail_ended_end_vs. The Home card reads "30-min check 10:50 OVERDUE +15 min", and its timeline diamond sits on the last dot. Opening it shows "ENDED - END VS - BED 12, Transfusion ended at 10:58, MEASURE / DOCUMENT". I expected the card to say the transfusion has ended and that end vitals are due. Actually nothing on Home says "ended" or "end VS", the header still says "1 BED RUNNING", and the amber button text is the same as for a normal check.

9. **Round checklist, "Remind me in 30 min" is ambiguous and 20 px under the primary (task 3).** Screen round_checklist, w360 f10/f20. It is an outlined button right below "All turned as suggested", with only about 8 dp between them. Its label does not say what is postponed: the whole round, or only the unfinished beds. The alarm screen calls the same idea "Snooze 2 min". A mis-tap while gloved pushes the round out by 30 minutes.

10. **Undo toast covers the Start buttons (task 4).** Screen undo_bar_on_home, w360_f10. After "Bed 5 - transfusion started" the toast sits over the two dark Start buttons. Only their top edges peek out, as sliver shapes with no readable text. For the toast's lifetime the nurse cannot start the next transfusion or turning plan.

11. **Edit position, selected state is inverted (task 5 edit path).** Screen turn_edit (Isolation Bed 23), w360_f10. In "WHICH BED?" selected is dark-filled with a check, as in start_bed_picked. In "LYING NOW" the dark-filled "Back" button has no check, and the white outlined "Right 30°" has the check. I expected dark plus check to mean selected in both groups. Actually the dark-filled button looks selected but is not.

12. **Start screens, key line cut off by the sticky footer (tasks 4 and 5).** Screens start_empty and start_bed_picked: the "STARTED AT 11:05" block is sliced in half. Screens turn_start and turn_start_bed_picked: "First turn 12:00, then every 2 h on the ward clock" is cut after "ward clock". Nothing signals that more content scrolls. "Ward clock" is never defined before the nurse commits (the detail screen only says "late turns don't move it"). I expected the first-turn time and the term to be fully readable before the final tap.

13. **"Start transfusion" breaks mid-word at w360 (task 4).** Screens home_empty, home_both_features and the other Home variants at w360_f10. The dark button reads "Start / transfusio / n" over three lines. At w412 it is "Start / transfusion". This is the entry button for the task and it looks broken at the small size.

14. **"Isolation Bed 23" chip breaks mid-word at w360 (tasks 4 and 5).** Screens start_empty, turn_start, start_edit and turn_edit at w360_f10. The chip reads "Isolatio / n Bed / 23", three lines. It also makes row 5 of the grid taller (turn_edit) and is the least readable chip.

15. **The check mark is stranded on its own line at w360 (tasks 1 and 2).** Screens alarm_1bed w360_f10 ("Measured & documented" then a lone check on line 2) and the Home cards "Bed 12 - Measured & documented / check" at w360_f10. At w412 it is one line. The button is readable, but the layout looks unfinished for the most-used control.
