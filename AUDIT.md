# AUDIT — current app (v0.4.0, main 5d32b55)

Phase 1 of docs/DESIGN_OVERHAUL.md. No app code was changed for this audit.

**Evidence.** Screenshots are the `audit` folder of the CI artifact `gates` (Lab workflow), named
`<screen>__<variant>.png`: `w360_f10` (360×800 dp, font 1.0), `w360_f20` (360 dp tall sheet, font 2.0),
`w412_f10` (412×915 dp), `*_night` (system dark on), `*_ar` (system language Arabic). Contact sheets:
docs/audit/. Emulator evidence (real alarms, notifications): artifact `lab-api34`.
Thresholds are docs/TEST_LOOP.md §4; colours measured with lab/color_check.py.

## 1. Tap count (current app)

Touches counted by `app/src/androidTest/.../TapBudget.kt` on the real activities (results in
`lab-api34/robustness/tap_budget.findings.txt`). The icon is tap 1 for launcher tasks.

| # | Task | Current touches | Budget | Result |
|---|------|----------------|--------|--------|
| 1 | See all my beds (6 beds) | TAPS_1 | 1 | RESULT_1 |
| 2 | Acknowledge a check from the alarm | TAPS_2 | 1 | RESULT_2 |
| 3 | Whole round as suggested (3 beds) | TAPS_3 | 2 | RESULT_3 |
| 4 | Start a transfusion | TAPS_4, plus typing | 4 | RESULT_4 |
| 5 | Start a repositioning plan | TAPS_5, plus typing | 4 | RESULT_5 |
| 6 | End a transfusion | TAPS_6 | 3 | RESULT_6 |
| 7 | Suspected reaction → steps (Home / alarm) | TAPS_7 | 2 | RESULT_7 |
| 8 | Mark "Transfusion stopped" for a bed | TAPS_8 | 3 | RESULT_8 |
| 9 | Round: one bed "can't turn" + reason | TAPS_9 | 3 | RESULT_9 |

## 2. Findings

Severity: **Critical** = can cost a check or a patient-safety step. **Major** = breaks a non-negotiable or
a threshold. **Minor** = polish. Each fix is a proposal; Phase 3 and 4 decide the final form.

### Alarm screen

**F01 · Critical · alarm_1bed.** One touch acknowledges a check, with no protection at all.
*Why:* the phone lives in a scrub pocket. A pocket touch silences the alarm and marks the check done; nobody
notices, and the check is simply gone. *Fix:* ignore touches for the first second and while the proximity
sensor is covered; 5-second undo snackbar plus "Undo last" on the bed card; a distinct confirmation haptic.

**F02 · Critical · round_7beds.** On a 7-bed round "Start round", the only control that stops the sound, is
below the fold. *Why:* a ringing phone in a dark room, and the stop button needs a scroll to find. *Fix:* pin the
primary action to the bottom of the screen; the bed list scrolls behind it.

**F03 · Major · alarm_transfusion_and_round.** A round due in the same minute starts below the fold (Bed 3 is
cut off) and nothing says more is below. *Why:* the nurse handles the transfusion and may never see the round.
*Fix:* a sticky "2 more: turning round · 2 beds" cue, and a footer that always shows what is left.

**F04 · Major · alarm_1bed.** Key numbers are under the threshold: the title is 34 sp and carries both the bed
and the check; the time is not on the screen at all. Threshold: ≥ 40 sp. *Why:* the bed cannot be read at arm's
length in 1 second. *Fix:* bed and due time as separate key numbers, ≥ 40 sp.

**F05 · Major · alarm_1bed.** The primary action sits mid-screen (top-aligned column), snooze below it, and the
actions are 56 dp (snooze) against a 64 dp threshold. *Why:* out of thumb reach one-handed. *Fix:* action stack in
the bottom third, all ≥ 64 dp.

**F06 · Minor · alarm_1bed.** "Measure T · HR · RR · BP · SpO₂ → document in Cerner" repeats the blocks right
under it. *Why:* more to read in a 1-second glance. *Fix:* keep the blocks only.

**F07 · Major · alarm_1bed, all.** Touch targets inside the alarm: "Other position", "Can't turn" and reason chips
are 56 dp; threshold for alarm actions is 64 dp. *Fix:* 64 dp on every alarm screen control.

**F08 · Major · round_checklist.** A round as suggested takes one touch per bed after "Start round". *Why:* a
7-bed round is 8 touches with scrolling, in the middle of the physical work. *Fix:* "All turned as suggested ✓"
completes the round; only exceptions need extra touches.

### Home

**F09 · Critical · home_both_features, home_20beds.** Beds are in start order, not urgency order; a bed that is
overdue can be off-screen, and nothing at the top says so. *Why:* the whole point of Home is "what's late": this
is how a missed check stays missed. *Fix:* sort by urgency; a live summary line ("1 overdue · 1 due now").

**F10 · Major · home_both_features.** Only 1.5 beds fit above the fold at 360 dp: card ≈ 250 dp tall, header 95 dp,
reaction banner 110 dp. Six beds need several scrolls; task 1's budget is one touch. *Fix:* cards ≈ 90 dp, single
line reaction bar, header reduced to the live summary.

**F11 · Major · home_both_features.** The next check's time is 16 sp inside a sentence ("Next: 1-h check at 11:20")
and timeline chips are 11 sp. Threshold on Home cards: ≥ 24 sp. *Fix:* the next clock time is the card's headline.

**F12 · Major · home, all.** No "due soon" state at all, and states differ by a red border only. The brief asks for
calm → due soon → due now → overdue by colour AND shape AND text. *Fix:* the urgency language of the chosen direction.

**F13 · Major · home_1bed_upcoming.** The two features have no type cue: cards are identical apart from a position
row; only the section label says which is which. *Fix:* icon + label + one consistent cue.

**F14 · Minor · home_empty.** The header ("Vital signs → Cerner, on time") is a slogan taking 95 dp of every Home
state. *Fix:* use it for live status.

**F15 · Minor · home_both_features.** Settings is the last list item, below every bed: with 20 beds it takes many
scrolls to reach. *Fix:* a fixed icon button in the header.

**F16 · Major · home_end_vs_pending.** "End VS" stays as a block on Home but the same card can stack Overdue + End VS
and push other beds away. *Fix:* one compact pending row per bed.

### Start flows

**F17 · Major · start_empty.** Starting a transfusion needs typing the bed on a keyboard, a required duration choice
(no default) and a separate baseline checkbox: Start, Transfusion, field, typing, chip, checkbox, Start. *Why:*
typing with gloves is the slowest thing on the ward. *Fix:* ward bed grid, duration preselected, one button
"Baseline done · Start".

**F18 · Major · turn_start.** Starting a plan is a 2,600 dp form of seven panels, with typing. *Why:* nurses won't
use it mid-shift. *Fix:* bed grid → tap the current position → running; everything else optional, reached later.

**F19 · Major · start_empty.** The disabled button reads "Tick Baseline VS taken first" in grey on grey, the only
explanation of why Start is off; on 360 dp the checkbox itself is below the fold. *Fix:* merge confirmation into the
action, nothing disabled.

**F20 · Minor · start_empty.** "Colour is assigned automatically." and "Slots stay on the ward clock (08:00 and 20:00
are always slots)." explain the machine, not the task. *Fix:* remove; show the result in the preview.

**F21 · Major · start_empty.** The Material checkbox target is 48 dp; threshold ≥ 56 dp (turn_start has five of them).
*Fix:* whole row is the target.

### Detail

**F22 · Major · detail_upcoming.** End transfusion is below the fold at 360 dp, after two panels, then a confirm
dialog: icon, card, scroll, End, confirm. Budget is 3. *Fix:* bottom action bar; Undo instead of the dialog.

**F23 · Minor · detail_upcoming.** Three stacked actions (End, Edit, Stopped) with the reaction one last. *Fix:* one
primary action, reaction always in the fixed bar.

### Reaction

**F24 · Critical · reaction_1bed.** "Transfusion stopped" is two screens down, then a dialog. During a reaction the
nurse has no free hand; budget is 3 touches, current is 5. *Fix:* bed buttons fixed at the bottom of the screen,
Undo rather than a dialog.

**F25 · Major · reaction_20beds.** With many beds the list of "Transfusion stopped — Bed n" buttons is a long scroll
below the clinical content. *Fix:* compact bed grid with running beds first.

### Colour and contrast (thresholds §4)

**F26 · Major · all.** Measured failures: amber block on white 2.03:1 and amber banner on panel 1.89:1 (UI ≥ 3:1);
chip border 1.50:1 (UI ≥ 3:1); white on green "on time" pill 4.21:1 (text ≥ 4.5:1); white on red 6.95:1
(key numbers ≥ 7:1). *Fix:* the chosen palette, already verified by lab/color_check.py.

**F27 · Major · detail_on_time_late.** Under deuteranopia "on time" green and "missed" red differ by ΔE 8.9
(threshold 15). The pills differ by word, not shape. *Fix:* urgency colours from lab/palettes.json, plus shape.

**F28 · Major · home_both_features.** The drop icon is coral (#E8524A), reading as danger red; the brief forbids it.
*Fix:* tinted with the transfusion type colour.

**F29 · Major · alarm_1bed__night, home_both_features__night.** No dark theme: at 3 AM the app and its first frame are
bright (`windowBackground` panel). *Fix:* night theme, automatic 20:00–08:00 or system; dark first frame.

**F30 · Minor · Notifications.kt.** NAVY and RED are raw ints outside the design system; the CI guard misses
`0xFF…toInt()`. *Fix:* tokens, and extend the guard.

### Text and scaling

**F31 · Major · all.** Body text under 16 sp everywhere: 15, 14, 13, 12, 11 and 9 sp are all in use.
*Fix:* type scale with 16 sp as the floor.

**F32 · Critical · home_both_features__w360_f20, detail_upcoming__w360_f20, settings__w360_f20.** At font 2.0 the app
breaks: timeline columns wrap to "10:\n20" and scatter, "TIME FROM START · baseline first" falls vertically down the
panel, labels split mid-word ("Transfusi\non", "Miss\ned"), the "MEASURE"/"DOCUMENT" titles clip to "MEAS"/"DOCU".
*Why:* nurses with large system text cannot read the clinical schedule. *Fix:* layouts built for scale: wrap, stack
and fixed-format times that never split.

**F33 · Major · home_both_features__w360_f10_ar.** System language Arabic: the manifest has `supportsRtl="true"`, so an
English UI mirrors. See the `*_ar` shots. *Fix:* force LTR on the root (the UI language stays English); Latin digits
are already correct (`ClinicalContentTest.timesKeepLatinDigitsOnAnArabicPhone`).

### Missing features the brief expects

**F34 · Major.** No widget and no launcher shortcuts. **F35 · Major.** The notification has ✓ for at most two beds and
none for a round. **F36 · Major.** No undo anywhere: acknowledge, End and Stop are final or guarded by dialogs.
**F37 · Major.** No ward bed list: every flow types a label. **F38 · Major.** No "due soon" or "overdue" haptics, and
no confirmation haptic.

### TalkBack

**F39 · Major · all.** Timeline dots have no semantics: a screen-reader user gets a list of times with no status.
State pills are text, but overdue on a card is signalled by the border. *Fix:* one merged description per bed:
"Bed 12, overdue, 30-minute check, due 10:50".

### Consistency

**F40 · Minor · turn_detail_on_track vs detail_upcoming.** Detail screens share a skeleton but label their primary
action differently and put the reaction control in different places. *Fix:* one bed-detail layout with the type as a
parameter.

## 3. Findings from the emulator lab

LAB_FINDINGS

## 4. Findings from independent reviewers

REVIEWER_FINDINGS
