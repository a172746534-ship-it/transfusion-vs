# Independent reviewers — baseline (current app, v0.4.0)

Fresh subagents that saw only the screenshots and the rubric of docs/TEST_LOOP.md §6.
Their findings and scores are recorded word for word and are not edited.

## Design reviewer — SCORE 3/10, FAIL

1. [Critical] detail_* and turn_detail_*, "baseline first" / "late turns don't move it" label (w360_font2): wraps one letter per line down the right edge, so the first screen is nearly empty and the timeline and action sit a full scroll down. Fix: put the label full-width under the heading.
2. [Critical] All timelines, Home and detail (w360_font2): chips break as "10:/20", "End" as "En/d", and the 15/30 labels collide, so a nurse can misread a check time. Fix: below a width/font threshold, swap the timeline for a vertical list with no-wrap times.
3. [Critical] Home cards, "Next" line (w360_font2): renders "1-h chec/k at 11:2/0", so the one time the nurse glances for is split across lines. Fix: give it full card width, with no-wrap time and "in 15 min" on its own row.
4. [Critical] alarm_* MEASURE/DOCUMENT cards (w360_font2): headings clip to "MEAS"/"DOCU", with one-word lines beneath, pushing actions down during a ringing alarm. Fix: stack the cards in one column at large font, or drop them from alarms.
5. [Critical] Checkpoint rows, detail_* (w360_font2): narrow columns give "10:/20", "Docume/nted", "chec/k", and pills crowd the text, so the record is unreadable at a glance. Fix: put time and pill on one row, description full-width below.
6. [Critical] settings, chooser, setup_check, turn_detail (w360_font2): mid-word breaks ("Transfusio/n", "Miss/ed after", "On-ti/me toler/ance", "Of/f", "Longe/st") and truncation ("Right 3", "Le"), so alarm settings can be misread. Fix: labels above controls, word-only wrapping, no fixed columns.
7. [Critical] alarm_1bed_ar title and home_both_features_ar Next line (arabic_and_night): bidi scrambles text to "min check —-15" and "h check at 11:20-1 :Next", giving a garbled clinical instruction. Fix: LTR-isolate mixed strings, or disable RTL until translated.
8. [Critical] home_both_features_night (arabic_and_night): identical to the day version, a near-white full screen with phone in dark mode, which glares in a dark patient room and can wake a child. Fix: build a real dark theme for Home and all light screens.
9. [Critical] round_7beds, and alarm_transfusion_and_round (w360_font1): Start round and Snooze are below the fold while the alarm rings, and in the mixed alarm the reaction banner is too. A gloved nurse must scroll to act. Fix: pin the action bar to the bottom.
10. [Major] alarm_1bed title (w360_font1, arabic_and_night): "15-min check — Bed 14" is ~34sp, under the 40sp minimum, with the bed second and a hanging dash, so "where do I go" is slow to read. Fix: "Bed 14" alone at 48sp, check name below.
11. [Major] alarm_1bed (w360_font1): no due time anywhere, so the nurse cannot tell if the check is on time or late. Fix: add "Due 11:07" at 40sp or more, and elapsed lateness when overdue.
12. [Major] alarm_2beds rows (w360_font1): bed ~18sp and "due 11:20" ~12sp grey, far under 40sp, while the largest text is the count "2 beds due now". The nurse cannot pick a bed at a glance. Fix: make bed and due time the largest row elements.
13. [Major] round_alarm, round_7beds, round_checklist rows (w360_font1): bed ~16-18sp and position "Left 30° → Back" ~13sp, yet that is the whole instruction. Unreadable from pocket height in a dark room. Fix: bed and target position at 24sp or more, 40sp for the round time.
14. [Major] alarm_snoozed, "rings again at 11:07" (w360_font1): ~14sp amber line, far under 40sp, so the key time is easy to miss. Fix: show it as the large time on the screen.
15. [Major] alarm_*, Snooze button (arabic_and_night): ~55dp tall, under the 64dp alarm minimum, a gloved mis-tap risk. Fix: 64dp or more.
16. [Major] alarm_1bed, amber DOCUMENT card above the amber CTA (arabic_and_night): same colour and radius as the real button, so a hurried nurse taps a dead card. Fix: make instruction cards plain text, amber only on tappable actions.
17. [Major] alarm_snooze_used_up, alarm_snoozed (w360_font1): Snooze vanishes with no explanation in used_up and the red banner moves into its slot, so muscle memory lands on the reaction screen. Fix: keep the slot, with a disabled "Snooze used" row.
18. [Major] Home cards, "Next: ... 11:20" and "Overdue: ... (10:50)" (arabic_and_night): times ~16sp, under the 24sp minimum, though time is the card's main fact. Fix: time at 24sp or more on its own line, label smaller.
19. [Major] Home timeline chips (arabic_and_night, front): ~11sp with the paper's slashed zero, so 10:50 can read as 10:58. Fix: plain-zero tabular numerals at 16sp or more, showing fewer chips.
20. [Major] Home timeline (arabic_and_night): "End" dot is filled like a done check though still ahead, and chip fill follows the paper's hourly pattern while dots show status. Two encodings mislead at a glance. Fix: one state encoding, End hollow until done.
21. [Major] Home header slogan (w360_font1): "Vital signs → Cerner, on time" is the largest text, is static even with overdue beds, and takes two lines. Paper poster copy tells a nurse nothing. Fix: state-driven title such as "1 overdue · next 11:20".
22. [Major] home_both_features (w360_font1): header plus fixed reaction banner and Start use ~40% of the height; ~1.3 cards show, hard-cut, and Repositioning is off screen. Beds get missed. Fix: slim the footer to one row, use compact cards, add a fade.
23. [Major] home_20beds (w360_font1): about two cards per screen, no due/overdue summary, no visible urgency sort, and "now" is the same calm blue as "in 15 min". A due bed can be scrolled past. Fix: compact rows, urgency sort, a loud "now" state.
24. [Major] home_overdue_missed, home_both_features (w360_font1): red overdue card sits on top of the red reaction banner and they merge in a glance, so emergency and paperwork look alike. Fix: reserve solid red for reaction, give overdue a distinct treatment.
25. [Major] Amber across screens (w360_font1): used for CTA, DOCUMENT card, bed dot, "late" pill, setup warning, End VS card and kicker, so it has no fixed meaning under time pressure. Fix: amber means action only, new tokens for status.
26. [Major] home_empty, home_setup_warning (w360_font1): the full red reaction banner shows with no transfusion running, training staff to ignore red. Fix: quiet outlined link when none is active, full banner while running.
27. [Major] home_ringing card (w360_font1): "Ringing now" is ~14sp red text under the Next line with no action, weaker than an overdue card though more urgent. Fix: put it first with filled treatment and a direct action.
28. [Major] home_stopped_reaction, detail_stopped_reaction (w360_font1): the most serious state is small grey text or an amber kicker, while a red overdue card and CTA still dominate. Fix: red "STOPPED — REACTION" header, demote the check to a documentation note.
29. [Major] home_end_vs_pending (w360_font1): two identical "Measured & documented" buttons, one clipped by the footer, so the nurse cannot tell which check each closes. Fix: name the check on each button ("30-min done", "End VS done").
30. [Major] reaction_* (w360_font1, back): symptoms fill the top, step 4 is cut off, step 5 and "Which bed was stopped?" need scrolling, labels ~11sp. Slow in an emergency. Fix: DO NOW first, larger labels, pinned "Transfusion stopped" button.
31. [Major] detail_* (w360_font1): teaching block ("Hourly = same minute as start") stays on top, and the CTA floats mid-list at a different height per state, so it must be hunted for. Fix: collapse the block, pin the CTA to the bottom.
32. [Major] Secondary text everywhere (w360_font1): "Started..." ~14sp, checkpoint notes ~12sp, pills ~10-11sp, settings help ~12sp, chooser text ~13sp, section labels and kickers ~11sp, all under 16sp and unreadable in a glance. Fix: 16sp floor, fewer words.
33. [Major] turn_detail_* (w360_font1): red "Longest gap 2 h 19 min" is the loudest item while "Next: Right 30° at 12:00" is ~12sp, so a past statistic outranks what to do next. Fix: next turn and position at 24sp or more, gap neutral and small.
34. [Major] turn_detail_* timeline pictograms (w360_font1): position icons look near-identical at this size with no text, so a wrong position is possible at a glance. Fix: larger distinct icons paired with text labels.
35. [Major] All sub-screens, "‹ Back" (w360_font1): looks ~32dp tall in the far top corner, hard to hit one-handed with gloves. Fix: hit area of 56dp or more and rely on system back or a bottom close.
36. [Major] Chips "Now / 5 min ago", "Off unit / Procedure", "Open", settings steppers (w360_font1): by eye ~43-53dp, under 56dp, so gloved mis-taps set the wrong start time or reason. Fix: 56dp minimum with more spacing.
37. [Major] detail_ended_end_vs dialog (w360_font1): off-palette lilac, "Later" ~37dp, CTA ~53dp, tiny content over a busy scrim, weak for a required End check. Fix: full-screen palette-matched prompt, 64dp actions.
38. [Major] start_empty disabled CTA (w360_font1): wrapped grey-on-grey "Tick 'Baseline VS taken' first", unreadable under bright lights, and the checkbox is off screen. Fix: show readable helper text and scroll to or surface the checkbox.
39. [Major] All screens (w360_font2): body text doubles but titles and bed numbers barely grow, so "Bed 12" nears body size and hierarchy flattens for the nurses who need large text. Fix: scale key numbers too, shorten secondary text.
40. [Major] round_checklist (w360_font1): no "2 of 4 done" count and no done-row state shown, and open reason chips push other beds down, so a nurse can lose her place mid-round. Fix: pinned progress count, collapsed done rows.
41. [Major] All flows (w360_font1): no variant shows confirmation or undo after "Measured & documented" or "Turned to...", so a gloved mis-tap could silently clear a check. Fix: done state plus a timed Undo.
42. [Major] *_ar variants (arabic_and_night): Arabic phone shows English in a mirrored layout, the timeline runs backward with units first ("h 1", "min 30"), arrows stay → while chevrons flip. Disorienting. Fix: translate properly, or keep the layout LTR.
43. [Major] turn_detail_* "Stop plan" (w360_font2): same navy fill as Save and Start, so a destructive action that ends turn alarms looks routine. Fix: outlined destructive style with confirmation.
44. [Major] alarm_1bed_night (arabic_and_night): saturated amber and red blocks unchanged from day, glaring in a dark room beside a sleeping child. Fix: dimmed night palette that keeps contrast.
45. [Minor] Home card CTA (arabic_and_night, w360_font1): ✓ wraps alone onto a second line at font 1.0, adding height and looking broken. Fix: shorter label or a leading icon bound to the text.
46. [Minor] alarm_1bed CTA (arabic_and_night): two-line left-aligned label with empty space, while Snooze is centred, slowing the scan. Fix: one alignment for all alarm actions.
47. [Minor] MEASURE card (arabic_and_night, front): wraps to "· SpO₂" with a leading dot, while paper fits one line, so the vital looks like an afterthought. Fix: tighter layout, no leading separator.
48. [Minor] alarm_1bed subtitle (w360_font1): "Measure ... → document in Cerner" repeats the cards directly below, wasting prime height in a 1-2 second glance. Fix: keep one.
49. [Minor] Primary buttons (w360_font1): same action is amber on alarm and white on the Home red card; Start is navy but Start round amber. Nurses cannot learn "the action colour". Fix: one primary style.
50. [Minor] home_overdue_missed card (w360_font1): red outline, red fill and white button nest three layers in a small card, adding noise and losing width. Fix: remove one layer.
51. [Minor] Wording (w360_font1): same 10:50 check is "Overdue" on Home and "missed" in detail; "End VS/End check/End"; "Turning round/Repositioning round/plan". Handover confusion. Fix: one glossary.
52. [Minor] Home card order (w360_font1): "Next" sits above "Overdue", so the reading order starts with the less urgent item. Fix: overdue first.
53. [Minor] Home title (arabic_and_night, front): "on time" is orphaned on line two, while paper fits one line. Fix: shorter or state-driven title.
54. [Minor] detail_* header (w360_font1): "(ends / 12:20)" splits the end time across lines at ~13sp, so it is easy to miss. Fix: put "Ends 12:20" on its own larger line.
55. [Minor] start_*, turn_start, turn_edit (w360_font1): tiny red "Bed only — no names or MRN" persists even on a valid value and reads as an error. Fix: neutral helper text, red only for real errors.
56. [Minor] Bed colour dot (w360_font1): absent from the alarm_1bed title, misplaced by a section label on detail, and navy nearly vanishes on navy alarm rows, so the identity cue breaks where needed. Fix: consistent larger marker with visible colours.
57. [Minor] chooser and turn_detail icons (w360_font1): repositioning uses a red icon though not urgent, diluting red. Fix: neutral or feature colour.
58. [Minor] chooser (w360_font1): cards sit at the top with the lower half empty, out of thumb reach. Fix: anchor options low and make them larger.
59. [Minor] alarm_1bed layout (arabic_and_night): blank band under the banner, and the reaction block sits lowest where the thumb lands, rather than the main action. Fix: bottom-anchor the primary action, separate the reaction block.
60. [Minor] home_empty "Settings" (w360_font1): a floating button mid-page that, with beds running, falls below the list, so it is hard to reach mid-shift. Fix: fixed header icon.
61. [Minor] turn_start disabled CTA (w360_font1): grey with no reason, while start_empty states one, so the nurse must guess. Fix: state what is missing.
62. [Minor] setup_check status (w360_font1): tiny coloured dots carry state and ready rows have no text, weak in bright light and for colour-blind staff. Fix: add "OK" / "Fix" text labels.
63. [Minor] home_setup_warning (w360_font1): "alarms may not ring" is ~14sp on the same amber as ordinary actions, undersold for a safety failure. Fix: distinct warning style, larger text.
64. [Minor] round_* title (w360_font1): "Turning round ·" leaves a hanging separator with 10:00 buried mid-sentence. Fix: time on its own large line, bed count separate.
65. [Minor] start_* and turn_start inputs (w360_font1): pale hairline borders on white wash out under ward lights, so the field is hard to find. Fix: stronger border or filled field.
66. [Minor] turn_detail_overdue_missed rows (w360_font1): rows say only "Turn" while on-track rows list positions, inconsistent at the point of action. Fix: always show the target position.
67. [Minor] detail_upcoming rows (w360_font1): description squeezed between time and pill wraps to 4-5 tiny lines, hard to scan. Fix: full-width description below the row.
68. [Minor] alarm_1bed and detail for 15/30-min checks (w360_font1, front): paper's "stay close" cue is missing from the early checks. Fix: short "Stay close" tag on those checks.

SCORE: 3/10 · FAIL

## Persona 1 — day shift, 6 patients, interrupted, one hand: 22 findings

| Task | How | Touches |
|---|---|---|
| 1 See all beds | App icon, then scroll home. About 2 cards fit per screen, so 6 beds plus the Repositioning section take about 3 scrolls | 4 |
| 2 Acknowledge check from alarm | Tap "Measured & documented" (with 2 beds due, one tap per bed) | 1 |
| 3 Whole round, all as suggested | From alarm: Start round, "Turned to ..." on each of 6 beds, about 3 scrolls at 2 beds per screen. No finish step is shown | 10 |
| 4 Start transfusion | Icon, Start, Transfusion, Bed field, type, scroll, duration, tick baseline, start button (never shown enabled) | 9 |
| 5 Start repositioning plan | Icon, Start, Repositioning, Bed field, type, Start repositioning (2 h and positions left as default) | 6 |
| 6 End transfusion | Icon, Bed card, scroll detail, End control (not in any image), then "Measured & documented" in the End VS dialog. Route is a guess | at least 5 |
| 7 Suspected reaction | Alarm: red banner, 1. alarm_transfusion_and_round: scroll + banner, 2. Launcher: icon + banner, 2. Detail or round screen: Back + banner, 2 (round screens only if Back reaches home) | 1-2 |
| 8 Mark "Transfusion stopped" for a bed | Control not in any image. Guess: icon, Bed card, scroll detail, stopped control | at least 4 |
| 9 One bed "can't turn" with reason | From alarm: Start round, scroll if needed, Can't turn, reason chip. From launcher: icon, scroll home, plan card (no card in any home image), Open round, Can't turn, chip | 3-4 from alarm, 6 from launcher |

1. home_20beds, bed cards: expected 6 beds readable in one glance. Only 2 cards fit, because the header, each card's timeline and the pinned red banner + Start take most of the screen.
2. home_* header "Vital signs → Cerner, on time": expected a count of what is due or late. It is the same slogan in every state, including home_overdue_missed.
3. home_both_features, Repositioning section: expected turning beds next to transfusions. Bed 12 and Bed 14 fill the viewport, and no repositioning row or round-due cue is visible.
4. home_end_vs_pending, Bed 12 card: expected each button to name its check, as alarm_2beds does per bed. Two identical "Measured & documented" buttons appear, one for the overdue check, one for End VS.
5. home_stopped_reaction, Bed 12 card: expected a stopped bed to look different at a glance. The red "Overdue" block and orange-free card match home_overdue_missed; only a small line reads "Stopped — reaction".
6. detail_upcoming and detail_on_time_late: expected an End transfusion button within reach. The viewport shows only timeline and checkpoints, down to "14:37 Time-limit warning". No image shows an End control.
7. reaction_1bed, reaction_20beds, detail_ringing: expected a "Transfusion stopped" control for a chosen bed. None is visible and the reaction variants look identical; only the result appears, in detail_stopped_reaction.
8. detail_* screens, reaction entry: expected the red banner "from anywhere", as on home. No red banner is visible, so Back is needed first.
9. round_alarm, round_7beds, round_checklist, reaction entry: expected the red banner the transfusion alarm has. None is visible in any of the three.
10. alarm_transfusion_and_round: expected the red banner and the round action on screen. The round section pushes both out; the viewport ends at Bed 3, with neither banner nor Start round.
11. alarm_1bed, alarm_2beds, alarm_snooze_used_up, alarm_snoozed, red banner: expected one fixed spot to hit by habit. Its height changes in each variant, from mid-screen to the very bottom.
12. alarm_snooze_used_up, Snooze button: expected a note that snoozes ran out, as alarm_snoozed gives "rings again at". The button is simply gone, with no text.
13. alarm_1bed, instruction: expected it once. "Measure T · HR · RR · BP · SpO₂ → document in Cerner" sits directly above the 1 MEASURE / 2 DOCUMENT tiles saying the same, pushing the main button down.
14. round_7beds, Start round: expected a pinned button like Start on home. The viewport ends at a clipped Bed 6 with no button, so I must scroll past the list.
15. round_checklist, "Turned to ..." buttons: expected one "all as suggested" action. Each bed has its own button and only 2 beds fit per screen, so a round means repeated tap and scroll.
16. round_checklist, Can't turn reason chips on Bed 7: expected all reasons visible once opened. The last row (Asleep/comfort, Unstable) is cut by the screen edge.
17. reaction_*, layout: expected DO NOW first, since I already suspect a reaction. A 7-line sign list fills the top half, step 1 starts mid-screen and step 4 "Inform the blood bank" is clipped.
18. start_empty, disabled button: it says Tick "Baseline VS taken" first, so I expected the tick nearby. The checkbox is not in the viewport, which ends at a clipped PLANNED DURATION.
19. start_edit, PLANNED DURATION: expected the options readable. The pinned "Save changes" bar covers most of the row.
20. start_empty and turn_start, Bed field: expected tappable recent beds for one-hand use. It is free text only ("e.g. Bed 12"), so every start needs the keyboard.
21. turn_start, "Start repositioning": expected a reason it is disabled, as start_empty gives. It is a plain grey button with no hint of what is missing.
22. turn_detail_on_track, NOW block: expected the next turn to be the largest item. "2 h 19 min" longest gap is big and red, while "Next: Right 30° at 12:00" is small grey, on a plan named on track.

FINDINGS: 22

## Persona 2 — night shift, 3 AM, dark room, sleeping child: 23 findings

| Task | How | Touches |
|---|---|---|
| 1 See all beds | Icon opens Home and the cards are read there. home_both_features needs a scroll to get past Bed 14 to Repositioning. home_20beds fits 2 cards per view. | 1 (one bed); 2 (two beds + repositioning); about 10 (20 beds) |
| 2 Acknowledge check from alarm | Alarm is already full-screen: tap "Measured & documented" (alarm_1bed). alarm_2beds has one button per bed. | 1 per bed |
| 3 Whole round, all as suggested | round_alarm: Start round, then "Turned to..." on each bed (round_checklist), scrolling as needed. No finish control is shown; add 1 if one exists. | 6 (4 beds); about 12 (7 beds, where Start round needs a scroll first) |
| 4 Start transfusion | Icon, Start, Transfusion, tap Bed, type, scroll, tick "Baseline VS taken" (box not shown, only named), Start button (start_empty). Started at defaults to Now. | 8; 9 if Planned duration has no default (its chips are cut off) |
| 5 Start repositioning plan | Icon, Start, Repositioning, tap Bed, type, Start repositioning (turn_start). 2 h is preselected. | 6 |
| 6 End transfusion | Icon, Bed card, scroll past checkpoints, End control (guessed, in no screenshot), then "Measured & documented" in the End VS dialog (detail_ended_end_vs). | At least 5 (guessed) |
| 7 Reaction steps | Tap the red "SUSPECTED REACTION?" banner on the alarm or Home. alarm_transfusion_and_round needs a scroll first. Bed detail needs Back first. | 1 from alarm; 2 from launcher, alarm_transfusion_and_round or bed detail; +1 scroll to read step 4 onward |
| 8 "Transfusion stopped" for a bed | Control is in no screenshot; only the result is shown (detail_stopped_reaction). Guess: icon, red banner, scroll past "Inform the blood bank", bed's control. | At least 4 (guessed) |
| 9 One bed "can't turn" with reason | round_alarm: Start round, "Can't turn" on the bed, reason chip (round_checklist). | 3 from alarm; 4 if the bed needs a scroll |

1. home_both_features_night, page and Bed cards: expected dark surfaces in dark mode beside a sleeping child. Actual: light page, white cards and a white "Measured & documented" button, same as the day screen.
2. alarm_snooze_used_up, Snooze slot: expected a "no snoozes left" note and buttons staying put. Actual: Snooze vanishes with no text and the red reaction banner moves up into its spot (compare alarm_1bed).
3. alarm_transfusion_and_round, reaction banner: expected it in view as on alarm_1bed. Actual: the round section fills the screen below Snooze, with no red banner or Start round visible until I scroll.
4. round_7beds, Start round and Snooze: expected an action visible on a ringing alarm. Actual: six bed rows fill the screen with no button in view. On round_alarm (4 beds), Snooze is clipped at the bottom.
5. detail_on_time_late, detail_ringing, turn_detail_on_track, round_checklist, reaction entry: expected it reachable from anywhere. Actual: no red banner in view on these screens; bed details offer only Back, round_checklist shows neither.
6. reaction_1bed, DO NOW list: expected the actions first in an emergency. Actual: a 7-item warning-sign list comes first, DO NOW starts halfway down, and step 4 is clipped at the bottom edge.
7. reaction_1bed and reaction_20beds, "Transfusion stopped": expected a bed-specific control. Actual: view ends at "Inform the blood bank", step 1 "STOP the transfusion" is plain text, and the visible screen is the same with 1, 20 or no beds.
8. detail_upcoming and detail_on_time_late, End control: expected an End transfusion button pinned like Start on Home. Actual: only checkpoint rows in view, down to 14:37 on detail_upcoming. Home cards show no End control either.
9. start_empty, bottom button 'Tick "Baseline VS taken" first': expected the checkbox beside it. Actual: view shows Bed, Started at and a clipped Planned duration, with no checkbox, so I must scroll to hunt for it.
10. round_checklist, per-bed buttons: expected one control for "all turned as suggested". Actual: no bulk button in view, only one "Turned to..." tap per bed, with about 2 beds fitting per view.
11. round_checklist, Bed 7 after "Can't turn": expected "Turned to Right 30°" to dim and all reasons to show. Actual: the amber button stays fully lit just above, and the "Asleep/comfort" and "Unstable" chips are clipped at the bottom.
12. home_overdue_missed, header "Vital signs → Cerner, on time": expected the header to report state, such as "1 overdue". Actual: the same slogan stays while Bed 12's card shows "Overdue: 30-min check (10:50)".
13. home_20beds, bed list: expected a compact list or a count of due and late beds. Actual: only Bed 1 and Bed 2 fit between the header and the pinned reaction banner and Start button, with no count anywhere.
14. home_both_features, Repositioning section: expected turn status alongside transfusions. Actual: view ends at a clipped Bed 14 with no repositioning row. home_overdue_missed shows only the heading and a clipped line.
15. home_both_features, Bed 12 card: expected the late item first with minutes late. Actual: "Next: 1-h check at 11:20 in 15 min" sits above "Overdue: 30-min check (10:50)", which gives only a clock time.
16. home_20beds, Bed 2 dot: expected dot colour to mean status. Actual: Bed 2 has a green dot while its check reads "now", and start_empty says "Colour is assigned automatically", so green only identifies the bed.
17. home_stopped_reaction, Bed 12 card: expected a distinct alert for a stopped transfusion. Actual: same red border as an overdue card, the state is only a small grey "Stopped · reaction" line, and the header still says "on time".
18. home_ringing, banner "Alarm ringing — tap to open": expected the bed name. Actual: no bed named. Bed 14's "Ringing now: 15-min check" line is at the card bottom with no button, unlike overdue cards.
19. home_end_vs_pending, Bed 12 card: expected different labels for different actions. Actual: two identical "Measured & documented" buttons in one card, one for the overdue 30-min check and one for End VS.
20. start_empty and turn_start, Bed field: expected to tap from a list of beds. Actual: a free-text "e.g. Bed 12" field, so each new start needs the keyboard (2 touches).
21. turn_start, disabled "Start repositioning" button: expected a reason, as start_empty gives. Actual: a grey button with no explanation of what is missing.
22. home_both_features_ar and alarm_1bed_ar, text: expected Arabic. Actual: layout flips but labels stay English and scramble: "in 15 min h check at 11:20-1 :Next" and the alarm title "min check —-15".
23. home_both_features, overdue button: expected "Measured & documented ✓" on one line. Actual: at small-phone width the checkmark wraps alone onto a second line, as also on home_overdue_missed and the _ar and _night variants.

FINDINGS: 23

## Persona 3 — gloved hands, mid-procedure, glance only: 20 findings

| Task | How | Touches |
|---|---|---|
| 1 See all beds | App icon, read Home cards. Repositioning needs a scroll, even with 2 beds. | 1 (one bed); 2 (home_both_features); about 11 (20 beds, about 2 cards per screen) |
| 2 Acknowledge check from alarm | Yellow "Measured & documented" on the alarm. | 1; 2 when 2 beds are due (one button per bed) |
| 3 Whole round as suggested | Round alarm: "Start round", then "Turned to…" per bed, with scrolls. Finish control not shown. | At least 6 (4 beds: 1 + 4 + 1 scroll); at least 12 (7 beds: 1 scroll to Start round, 1 + 7 + about 3 scrolls) |
| 4 Start transfusion | Icon, Start, Transfusion, tap Bed, type, scroll, duration, tick Baseline VS, start. Checkbox not shown. | 8 (default duration) or 9; needs typing |
| 5 Start repositioning plan | Icon, Start, Repositioning, tap Bed, type, "Start repositioning" (defaults kept). | 6; needs typing |
| 6 End transfusion | Icon, Bed card, scroll past checkpoints, End control (in no screenshot), then End VS dialog. | At least 5 (estimate) |
| 7 Reaction steps | Red "Suspected reaction?" button on transfusion alarm or Home. Detail screens: Back, then red button. Round alarm: no button shown. | 1 (alarm or open Home); 2 (launcher or detail); not possible as shown from round alarm |
| 8 Mark "Transfusion stopped" for a bed | Icon, Bed card (or red button), scroll, control. Control is in no screenshot. | At least 4 (estimate) |
| 9 One bed "can't turn" with reason | Round alarm: Start round, "Can't turn", reason chip. From launcher: icon, scroll, plan card, "Open round", "Can't turn", reason. Plan card on Home not shown. | 3 (4 if clipped reason or lower bed); about 6 from launcher |

1. home_overdue_missed, header "Vital signs → Cerner, on time": expected a live status. The same large text stays on while Bed 12 shows "Overdue: 30-min check", so at a glance it reads as "on time".
2. home_20beds, bed list: expected a due/late count or summary. Only Bed 1 and Bed 2 fit between header and pinned buttons, with no count, so about 10 scrolls are needed to see all beds.
3. home_both_features, Repositioning section: expected transfusions and turns in one glance. Bed 14 is cut off by the pinned buttons and no Repositioning card is in view, so a scroll is needed with only 2 beds.
4. home_end_vs_pending, Bed 12 card: expected buttons naming what they confirm. Two identical "Measured & documented" buttons sit in one card, one for the overdue check and one for End VS.
5. home_stopped_reaction, Bed 12 card: expected the reaction stop to dominate. "Stopped — reaction" is a small subtitle, while the red "Overdue: 30-min check" and its button dominate, so it looks like a running bed.
6. detail_on_time_late, detail_overdue_missed, detail_upcoming, checkpoint list: expected a visible End transfusion button. None is in view; detail_upcoming shows only checkpoints down to 14:37. Only the End VS dialog shows ending exists.
7. reaction_1bed and all detail screens, "Transfusion stopped": expected this button beside "STOP the transfusion" or on the Bed screen. It is in no screenshot, so I must scroll blind to look for it.
8. reaction_1bed, title and body: expected the one running bed to be named. No bed appears in view, and it looks the same above the fold as reaction_20beds and reaction_no_bed.
9. reaction_1bed, "Do now" steps: expected STOP first. Seven warning-sign lines fill the top half, steps start mid-screen, and step 4 "Inform the blood bank" sits at the cut-off edge.
10. round_alarm, action buttons: expected the red reaction button as on the transfusion alarm. Only "Start round" and "Snooze 2 min" show. round_7beds and round_checklist have no red button in view either.
11. detail_stopped_reaction and the other detail screens, top and bottom: expected reaction steps one tap away. No red button is in view, so I must hit the small plain-text "‹ Back" at top-left first.
12. round_7beds, "Start round": expected a pinned button like Home's Start. The list runs off at Bed 6 with no Start round in view, so a scroll is needed before the round can begin.
13. alarm_transfusion_and_round, round section: expected a visible round action. The section sits below the reaction button and cuts off at Bed 3, with no "Start round" in view.
14. round_checklist, per-bed buttons: expected one "all turned as suggested" action. Each bed has its own "Turned to…" tap with only about 2 beds per screen, and no progress counter or finish button is visible.
15. round_checklist, "Can't turn" reasons on Bed 7: expected all reasons in view after tapping. "Asleep/comfort" and "Unstable" are clipped at the bottom edge, so a scroll is needed to pick them.
16. start_empty and turn_start, Bed field: expected a tap-to-pick bed or recent beds list. Only a free-text "e.g. Bed 12" field shows; I cannot type with gloves, so both start flows are blocked.
17. start_empty, disabled button 'Tick "Baseline VS taken" first': expected that checkbox in view. No checkbox is visible, and the pinned button covers the "Planned duration" section, so I must scroll to find both.
18. turn_start, greyed "Start repositioning": expected a reason, as start_empty gives. The button is grey with no explanation of what is missing.
19. start_empty and turn_start, red "Bed only — no names or MRN": expected neutral help text. Red text under an untouched empty field reads as a validation error before any input.
20. alarm_snooze_used_up, Snooze area: expected a message that snoozes are used up. The "Snooze 2 min" button is simply gone, with no text explaining why.

FINDINGS: 20
