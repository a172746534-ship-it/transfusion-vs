# Reviewers, round 2 (screenshots of commit 3e41bec). Word for word, unedited.

# Design review 4 - transfusion / repositioning companion (Android)

Method: every screen opened in w412_f10 and night; w360_f20 opened for overflow; w360_f10 spot-checked. Pixel colours sampled from the PNGs for contrast. Screenshot = 1236 px wide at 412 dp, so 3 px = 1 dp.
Palette seen: navy #13233F, red #A80F1C, amber #F5A623 (button), amber #D28000 (SOON chip), brown #8A5A00 (VS stripe/label), track #C9CCD4, page #EEF1F6.

## GROUP 1 - HOME (home_* + undo_bar_on_home)

### HOME-1  MAJOR - Permanent solid-red "SUSPECTED REACTION?" slab shouts when nothing is wrong
Files: home_1bed_upcoming__w412_f10.png, home_6beds__w412_f10.png, home_both_features__w412_f10.png, home_ringing__w412_f10.png (and every Home with a running transfusion; night: *_night.png).
Element: full-width red (#A80F1C) bar with warning triangle, directly above the two Start buttons.
Why it hurts: it is the biggest, most saturated block on Home in the "all on time" state (home_1bed_upcoming reads "All on time" in the header and a red alarm bar at the bottom). The brief says quiet when fine, red = danger only. A nurse glancing for 1-2 s sees red every time, so red stops meaning anything and the real red (OVERDUE, STOPPED) loses its force. It also sits 8 dp above Start transfusion/Start turning, so a gloved thumb aimed at Start can land on a 100 %-red "reaction" control.
It also stays on screen when the only transfusion is already stopped or ended (home_stopped_reaction, home_end_vs_pending): a red "STOP the transfusion & call" bar under a card that already says STOPPED - REACTION is contradictory noise (it is correctly absent on home_empty and home_only_turning).
Fix: at rest make it a quiet reference row (white/navy-outline row, red icon and red text only, no red fill, 56 dp, 16 dp+ gap above the Start pair). Reserve a solid red fill for the reaction screens and the stopped-for-reaction state.

### HOME-2  MAJOR - "Alarm ringing - tap to open" uses the same red as the reaction banner, sits in the top third, and has no affordance
Files: home_ringing__w412_f10.png, home_ringing__w412_f10_night.png
Element: red rounded bar under the header; text only.
Why it hurts: (a) a routine 15-min vital-signs alarm is rendered in the exact danger red of "suspected reaction" (red-means-danger-only broken; the screen now has two identical red slabs top and bottom); (b) no bell icon, chevron or button shape, so a colleague with no training reads it as a heading, not a control; colour + text only, no shape; (c) the single most important action on the screen is at the very top, outside one-handed thumb reach; (d) at night it turns into dark maroon with no outline while the permanent reaction banner below it keeps a bright salmon outline, so the ringing alarm looks quieter than the idle reaction reminder - urgency inverted.
Fix: use an alarm treatment that is not the danger red (amber/ink with bell icon and "Open" chevron), min 64 dp high, pinned in the bottom third above Start; at night keep an outline at least as strong as any other element.

### HOME-3  MAJOR - Five equal-weight slabs compete; the real primary action is not in the bottom third
Files: home_both_features__w412_f10.png (+ _night), home_6beds__w412_f10.png, undo_bar_on_home__w412_f10.png
Element: amber in-card buttons ("Open round", "Bed 12 - Measured & documented"), red reaction bar, navy Start transfusion, navy Start turning.
Why it hurts: home_both_features shows 2 amber + 1 red + 2 navy full-width slabs, all 56 dp, all rounded rectangles. The brief demands one primary action per screen in the bottom third. The clinically primary action (confirm the check) lives in the top/middle of the list, moving down as cards change; the always-present Start pair - the quietest colour on screen (same navy as the header) - occupies the thumb zone. With gloves and 1-2 s of attention the nurse cannot tell which button is "the" one.
Fix: one primary slot above the Start row: the most urgent due item's confirm button, pinned in the bottom third (and removed from the other cards, which just open on tap). Demote Start to a secondary pair (outlined) when something is due, and keep it filled only when nothing is due.

### HOME-4  MINOR - Amber/brown means "transfusion", "soon" and "tap me" at once
Files: home_6beds__w412_f10.png, home_1bed_upcoming__w412_f10.png, home_both_features__w412_f10.png (night too)
Element: left stripe (#8A5A00), drop icon, "VS" label, SOON chip (#D28000), amber action buttons (#F5A623), amber header overline, amber Setup banner.
Why it hurts: every VS card carries a permanent amber/brown identity colour even when perfectly on time, so in home_6beds the whole screen reads "warning" before any state is shown; the SOON chip then looks like part of the card's identity instead of a state, and the amber action button looks like a status. State colour must be distinguishable from category colour at a glance.
Fix: give transfusion a non-state identity (e.g. blue/indigo drop + stripe, or neutral navy) and reserve amber strictly for "soon / needs action".

### HOME-5  MAJOR - DUE NOW chip is quieter than SOON chip (urgency inverted)
Files: home_6beds__w412_f10.png (Bed 3, Bed 14 vs Bed 21, Bed 7), home_20beds__w412_f10.png, home_ringing__w412_f10.png
Element: "DUE NOW" pill = navy fill (#13233F) with white text; "SOON - 2 min" pill = amber fill.
Why it hurts: the thing that needs action now is painted in the same navy as the header and Start buttons and reads like a static label; the thing that is merely coming is amber and louder. In home_20beds twenty identical navy pills give the eye nothing to land on. A state differs from the next only by a word, and DUE NOW has no icon in the chip.
Fix: DUE NOW = filled amber (or high-contrast inverse) with a bell/clock glyph in the chip; SOON = outline chip, no fill. Keep OVERDUE red.

### HOME-6  MAJOR - Setup warning ("alarms may not ring") is a text strip with no icon and no visible control
Files: home_setup_warning__w412_f10.png, home_setup_warning__w412_f10_night.png
Element: amber (#D38300) rounded bar, two lines, bold navy text, orphan "not ring" on line 2.
Why it hurts: the worst failure the app can have (silent alarm) gets the weakest component: no warning triangle (colour + text only, no shape), no chevron/button (tap affordance invisible), wraps to two lines at 412 dp with an orphan. At night it becomes a bright pale-yellow slab, the brightest object in a dark room, yet still without an icon.
Fix: one-line bar, 56 dp, warning-triangle icon at left, "Fix" button/chevron at right; keep the amber hue but add the shape; in night use a dimmer outlined amber.

### HOME-7  MINOR - Same state, different affordance
File: home_6beds__w412_f10.png (Bed 3 DUE NOW has no button, Bed 14 DUE NOW has one); home_20beds shows 20 DUE NOW cards with none.
Why it hurts: the nurse cannot predict where to tap to confirm; one-tap-from-Home works for some beds and not others.
Fix: either every actionable card has the control or (better, see HOME-3) only the single pinned primary does.

### HOME-8  MINOR - "Start transfusion" wraps, "Start turning" does not; icon/label composition differs
Files: every home_* w412_f10 (e.g. home_empty__w412_f10.png)
Element: bottom button pair. "Start / transfusion" is two lines with the drop icon pinned far left and text centred in the rest; "Start turning" is one line with the icon hugging the text.
Why it hurts: the two halves of a single action look unlike each other; the icon in the left button floats ~30 px from the text. Looks unfinished, and the pair has different visual weight.
Fix: stack icon above label in both, or reduce side padding/label size so both are one line ("Start transfusion" fits at 16 sp with 12 dp padding at 412 dp), identical structure.

### HOME-9  MINOR - Overdue outline collides with the category stripe
Files: home_overdue_missed__w412_f10.png, home_both_features__w412_f10.png, home_6beds__w412_f10.png (crop verified)
Element: left 4 dp stripe is square and extends beyond the rounded 3 dp red outline at the top-left and bottom-left corners; the outline paints over the stripe.
Why it hurts: the most important cards on the screen have the roughest edge; looks like a rendering bug.
Fix: clip the stripe to the card shape and inset it inside the border (or drop the stripe when the red outline is on).

### HOME-10  MINOR - Header: clock buried, settings icon unlabeled/ambiguous, same fact three times
Files: home_empty__w412_f10.png, home_stopped_reaction__w412_f10.png, home_end_vs_pending__w412_f10.png
Element: amber overline "NO BEDS RUNNING - 11:05", title "Nothing running", card "No beds running"; sliders glyph top right.
Why it hurts: the current time (key number) is a ~16 sp letter-spaced overline, the smallest thing in the header; the empty state repeats "no beds running" three times; the sliders glyph reads as "filter", not Settings, has no label and no visible 56 dp container (glyph is ~20 dp). In home_end_vs_pending the header says NO BEDS RUNNING while the title says 1 overdue.
Fix: show the clock large (>=24 sp, white) at right of the title row; drop the redundant overline or card; use a gear icon with a 56 dp container and content description.

### HOME-11  MINOR - Special cards break the card grid
Files: home_stopped_reaction__w412_f10.png, home_end_vs_pending__w412_f10.png
Element: STOPPED card has no stripe, left padding ~21 dp vs 29 dp on normal cards, icon/title and body text start at different x; END VS card wraps the body to three lines with the orphan "missed" beside the ENDED - END VS chip, and the button label wraps to two lines.
Why it hurts: breaks the scan column that the rest of Home trains the eye on; the wrapped lines collide with the chip.
Fix: same card scaffold for all states (stripe slot, 29 dp text inset, chip on its own row), body max two lines with the chip below it.

### HOME-12  MINOR - Track and chevron contrast
Files: home_6beds__w412_f10.png, home_20beds__w412_f10_night.png
Element: inactive progress track #C9CCD4 on white = about 1.6:1 (needs 3:1); night inactive track is dark slate on navy at a similar ratio; hollow future dots are ~10 dp. Chevron #4C5870 is fine but sits in the margin beside in-card buttons, so a card with a button has two overlapping tap targets (card vs button) that look equal.
Fix: track at >=3:1, 2 dp thick; remove chevron on cards that carry a button or make the button visually the only target.

### HOME-13  MINOR - Night: pale pastel chips and slabs glare, buttons have weak fill
Files: home_20beds__w412_f10_night.png (20 pale-blue DUE NOW chips), home_setup_warning__w412_f10_night.png (pale-yellow bar), home_both_features__w412_f10_night.png (two large #C98F35 amber slabs), home_empty__w412_f10_night.png
Why it hurts: in a dark room with sleeping children the chips are the brightest pixels; the Start buttons (#1E2B45 fill on #0A101C) rely on a 2 dp outline for their edge.
Fix: night chips = dim fill + bright text/border; keep one bright element per screen; give the Start pair a clearly lighter fill.

### HOME-14  MINOR - 20-bed list: hard clip and no grouping
File: home_20beds__w412_f10.png
Element: Bed 9 card cut by the footer with no fade; 20 identical rows, no section for overdue/due/later; footer eats ~15 % of height.
Fix: add a soft fade or "+12 more" indicator; group by urgency with sticky section labels.

### HOME-15  MINOR - Undo toast is cramped between list and red bar
File: undo_bar_on_home__w412_f10.png
Element: white toast with double outline (navy border + navy Undo button with own border), 8 dp from the red bar, no countdown cue.
Why it hurts: three stacked bars, toast outline visually merges with the Undo button (Undo button edge is ~3 dp from the toast edge), no indication how long Undo stays.
Fix: 12-16 dp breathing room, a thin shrinking timer line, Undo as a text-style button with a 56 dp target.


## GROUP 2 - ALARM SCREENS (alarm_*)

### ALARM-1  MAJOR - Ringing, snoozed and "no snoozes left" are the same screen; the state is a button label
Files: alarm_1bed__w412_f10.png, alarm_snoozed__w412_f10.png, alarm_snooze_used_up__w412_f10.png (+ _night)
Element: the outlined 64 dp row under the amber button. Ringing = "Snooze 2 min" (about 24 sp); snoozed = "Snoozed - rings again at 11:07" and used-up = "No snoozes left" (about 18-19 sp, dimmer border).
Why it hurts: apart from that one line the three screens are pixel-identical (same header, same amber, same layout). Colour, shape and text must all change between urgency states; here only the text changes, in the smallest type on the screen, so a nurse who looks for 1-2 s cannot tell "ringing now" from "snoozed, relax". The two status lines also stay inside a button outline, so they look like disabled buttons that might be tapped (false affordance).
Fix: snoozed = remove the outline, show a clock icon + one status line (no border), tint the header band (e.g. calmer slate) so the screen itself changes; used-up = drop the row and keep the layout stable with a caption above the primary button; keep type size >= the button label size.

### ALARM-2  MAJOR - The alarm screen itself carries no urgency signal (no DUE NOW/overdue chip, bell or shape)
Files: alarm_1bed__w412_f10.png, alarm_1bed__w412_f10_night.png, alarm_transfusion_and_round__w412_f10.png
Element: header block: amber overline "BLOOD TRANSFUSION RUNNING", white "15-min check", Bed 14, 11:07, progress track.
Why it hurts: this is the full-screen interrupt the brief calls "unmistakable", but it is visually the same calm navy page as a detail view. Home labels the same event with a DUE NOW chip and ring marker; the alarm screen drops both, and there is no overdue variant at all (a 20-minutes-late check looks like an on-time one). Urgency is carried only by the sound/vibration, not by colour+shape+text on screen.
Fix: add a state chip next to the time ("DUE NOW" with bell icon; "OVERDUE +n min" in red with the diamond marker when late) and a 4 dp coloured top edge, so the screen alone says how urgent it is.

### ALARM-3  MAJOR - Two-bed alarm: confirm buttons are 59 dp and float mid-screen; the bottom third holds Snooze and the red bar
File: alarm_2beds__w412_f10.png (+ _night)
Element: two amber "Bed N - Measured & documented" buttons (measured 178 px = 59 dp), top edges at 38 % and 66 % of screen height; below them a 300 dp void, then Snooze and the red reaction bar.
Why it hurts: alarm actions must be >= 64 dp and the primary action must sit in the bottom third. Here the two confirms are under the limit and out of thumb reach, while the least important control (Snooze) and a danger bar own the thumb zone; the red bar is the lowest, easiest-to-hit element on the screen.
Fix: stack the confirms bottom-aligned, 64 dp+ each, above Snooze; push the info card up; put the reaction bar above Snooze with a larger gap (see ALARM-4).

### ALARM-4  MINOR - Red reaction bar is the bottom-edge target, 13 dp under Snooze
Files: alarm_1bed__w412_f10.png, alarm_snoozed__w412_f10.png
Element: red bar (63 dp) directly below the outlined Snooze button (13 dp gap) at the lowest position on the screen.
Why it hurts: the lowest edge is where a one-handed thumb rests and slips; a gloved tap on Snooze that lands 13 dp low starts the reaction flow. Same solid-red-at-rest problem as HOME-1.
Fix: move the bar above the primary action as a thin outlined row, or at least 24 dp separation plus a lighter fill until the nurse chooses it.

### ALARM-5  MINOR - The round (turning) task is a hairline ghost in a double-alarm
File: alarm_transfusion_and_round__w412_f10.png (+ _night)
Element: outlined button "Start round . 10:00 . Bed 3, Bed 7" (about 16 sp, 64 dp) directly under the checklist card.
Why it hurts: it has the same outline, radius and grey as the info card above and the Snooze button below, so three different roles (information, action, action) share one style; no turning icon (used on Home), no overdue/due state for a round that is 65 min late, bed numbers at body size instead of key-number size. A nurse can easily read it as part of the checklist and skip a due round.
Fix: give the round its own compact card with the turn icon + TURN label, bed numbers >= 24 sp, and the same DUE NOW/OVERDUE chip as Home; keep it clearly secondary to the amber action but not identical to the info card.

### ALARM-6  MINOR - Step list uses two arbitrary hues; subscript is tiny
Files: alarm_1bed__w412_f10.png, alarm_2beds__w412_f10.png
Element: step 1 blue circle + blue "MEASURE", step 2 amber circle + amber "DOCUMENT"; "SpO2" subscript about 10 sp.
Why it hurts: blue appears nowhere else on this screen and amber already means "tap this" (the primary button), so the amber "DOCUMENT" heading reads as a second button label. The subscript 2 is below the 16 sp body minimum and disappears under gloves/glare.
Fix: one neutral hue for both steps (white numbers in outlined circles), amber only for the action; write "SpO2" in full-size type.

### ALARM-7  MINOR - Dead zone and small body type on the alarm screen
File: alarm_1bed__w412_f10.png
Element: 300 dp void between the checklist card and the buttons; checklist body about 16 sp.
Why it hurts: there is room to make the two instructions 20 sp+ and the bed/time larger; the screen is top-heavy and the eye has nothing between the card and the button.
Fix: vertically distribute (bed+time block larger, 20 sp instruction text) or move the checklist down toward the buttons.

### ALARM-8  MINOR - "Done" confirmation is unanchored
File: alarm_done_undo__w412_f10.png (+ _night)
Element: left-aligned, wrapped 36 sp text "check mark Bed 14 . 15-min / check done" floating at 43 % height; Undo (64 dp outline) alone at the bottom.
Why it hurts: the headline breaks after "15-min", the check mark is a text glyph hanging left of the baseline, 40 % of the screen above it is empty, no timer shows how long Undo lasts, and the screen has no shape/colour of success (a big check icon in a circle). It looks like an unstyled fallback rather than a designed confirmation.
Fix: centred 96 dp check icon, one-line label, a thin countdown bar on Undo.

### ALARM-9  MINOR - At font scale 2.0 the alarm screen is taller than a phone
Files: alarm_1bed__w360_f20.png, alarm_2beds__w360_f20.png, alarm_transfusion_and_round__w360_f20.png (compacted view)
Element: overline wraps to three lines with the drop floating mid-block; Bed and 11:07 stack on separate lines; checklist wraps SpO2 onto its own line; footer = 2-line amber button + Snooze + a 4-line red bar.
Why it hurts: content measures about 1,025 dp (alarm_1bed) to 1,325 dp (alarm_2beds), far above a 640-780 dp phone; no truncation, but the confirm button can only be reached by scrolling unless the footer is pinned, and the red bar becomes the biggest block (about 130 dp).
Fix: cap the scaling of decorative text (overline, step captions) at 1.3x, drop the overline to an icon at scale >= 1.5, keep Bed + time on one row at 360 dp, and pin the footer.

## GROUP 3 - TRANSFUSION DETAIL (detail_*)

### DETAIL-1  MAJOR - Footer owns the thumb zone with the destructive action; the real primary sits mid-list
Files: detail_ringing__w412_f10.png, detail_on_time_late__w412_f10.png, detail_overdue_missed__w412_f10.png (+ _night)
Element: pinned footer = red reaction bar + filled navy "End transfusion" (irreversible, 64 dp) in the bottom third; the amber "Measured & documented" confirm is a 2-line, 56 dp button embedded in the Checkpoints list at 47-62 % of screen height.
Why it hurts: the brief wants one primary in the bottom third. Here the only filled, heaviest, lowest button ends the transfusion, directly under a red "stop the transfusion" bar, while the action the nurse actually came for scrolls with the list and moves around. A gloved thumb goes for the big navy slab.
Fix: pin the due check's confirm (amber) as the bottom primary when something is ringing/overdue; demote "End transfusion" to an outlined/secondary row with a confirm step, and keep the reaction row thin.

### DETAIL-2  MAJOR - "Time from start" row is clipped at the right edge (fails zero truncation)
Files: detail_upcoming__w360_f20.png, detail_overdue_missed__w360_f20.png, detail_on_time_late__w360_f20.png
Element: marker + chip row (0 / 15 min / 30 min / 1 h / End with 10:52 / 11:07 / ...): the End and 1 h columns are cut off ("E", "1", "11" visible) with no scroll hint; the card edge slices through the chip.
Why it hurts: at large text the end time and the hourly checks are unreadable in that card; a clipped time looks like a wrong time. The same times exist in the list below, which makes the row dead weight at exactly the font sizes where space is scarce.
Fix: wrap the chips onto two rows (flow layout) or turn the row into a vertical list at font scale > 1.3; never clip.

### DETAIL-3  MAJOR - No hero: the next check is not emphasised and the same times appear twice
Files: detail_upcoming__w412_f10.png, detail_ringing__w412_f10.png, detail_on_time_late__w412_f10.png
Element: "TIME FROM START" card (chips) followed by "CHECKPOINTS" card; every row is the same bold-label + bold grey status, five to six rows all saying "upcoming".
Why it hurts: on a 1-2 s glance nothing says "next: 15-min check at 11:07, in 2 min". The first screenful (about 350 px / 160 dp) is spent on a timeline whose times are repeated verbatim in the list below; the Time-limit warning (14:37) has the same weight as the next vital-signs check; "Baseline first - hourly = same minute as start" is instruction text on a status screen.
Fix: add one hero row on top (next check name + 24-40 sp time + countdown), fold the timeline into the list (marker column) or drop it, grey-out future rows and show only the next row at full strength.

### DETAIL-4  MINOR - Marker and chip vocabulary is inconsistent across Home, Detail and Alarm
Files: detail_ringing__w412_f10.png, detail_on_time_late__w412_f10.png, home_6beds__w412_f10.png
Element: timeline markers: filled navy circle = done, hollow ring = upcoming, red rounded square = overdue AND ringing; "End" is always a filled circle although it is in the future; hourly chips (0, 1 h) are dark-filled while others are outlined; a +9-min late check shows the same dot as an on-time one. List chips: grey text (logged/upcoming), tick+text (on time), red chip without icon (overdue), amber chip (late), navy chip (ringing). Home uses a target ring for due now and a diamond-with-! for overdue.
Why it hurts: red is used for a routine "ringing" check (red = danger only); the future End looks completed; late is visible only in the list; the overdue chip has colour+text but no shape, unlike the diamond on Home.
Fix: one marker set app-wide: ring = due now (amber), diamond-! = overdue (red), check = done, check-with-clock = late, hollow = upcoming, flag = End; use it in the list rows as well.

### DETAIL-5  MINOR - Stopped screen: the key action has the weakest style, stale confirm still offered
File: detail_stopped_reaction__w412_f10.png (+ _night)
Element: "Remove from Home" and "Open the reaction steps" are identical outlined buttons; the bottom one is the important one. An amber "30-min check - Measured & documented" button is still shown under the line "Remaining alarms for this bed are cancelled".
Why it hurts: danger-state screen with two equal outlined buttons (one nearly destructive) and a live-looking amber CTA that contradicts the red header. 
Fix: "Open the reaction steps" = filled (navy or red), "Remove from Home" = plain text button; hide the confirm button when the bed is stopped.

### DETAIL-6  MINOR - Pinned footer slices content mid-component
Files: detail_overdue_missed__w412_f10.png (a sliver of the "Edit start time or duration" outline shows above the red bar at y 1640), detail_ended_end_vs__w412_f10.png (list card cut by the bar), detail_on_time_late__w360_f10.png (row cut in half)
Why it hurts: looks like a rendering glitch; no fade or divider tells the nurse the list scrolls.
Fix: add a 16 dp top fade/shadow on the footer and clip the scroll area at the footer edge with padding.

### DETAIL-7  MINOR - End VS screen: two same-weight amber confirms; colour of the steps differs from the alarm screen
File: detail_ended_end_vs__w412_f10.png (+ night)
Element: "End VS - Measured & documented" (top) and "30-min check - Measured & documented" (lower) are identical amber buttons; step circles here are bright blue 1 and brown 2 (MEASURE blue #2F6FDB, DOCUMENT brown) vs light-blue and amber on the alarm screens; at night step 1 is a dull grey-blue so MEASURE is dimmer than DOCUMENT.
Fix: one primary (End VS) in the bottom third, the missed 30-min check as a quiet row; share one step style between alarm and detail.

### DETAIL-8  MINOR - At large type the checkpoint rows lose their reading order
File: detail_overdue_missed__w360_f20.png, detail_upcoming__w360_f20.png
Element: the row reflows to "10:52 ........ logged" on line 1 and "Start" on line 2, so the status sits next to the time instead of the label; the overline wraps to 3 lines.
Fix: keep label + status together (status under the label) when the row wraps.

### DETAIL-9  MINOR - Back is a small top-left target
Files: all detail_* w412_f10
Element: chevron + "Back" at the top-left, text-height about 18 sp, no visible 56 dp container.
Why it hurts: it is the farthest point from a one-handed thumb on a tall phone and there is no bottom-reach alternative.
Fix: 56 dp target with visible pressed state; keep system back working; consider a bottom "Back to Home" row on read-only states.

## GROUP 4 - ROUND ALARM / CHECKLIST (round_*) AND REACTION (reaction_*)

### ROUND-1  MAJOR - Bed numbers on alarm screens are below the 40 sp key-number threshold
Files: round_alarm__w412_f10.png, round_7beds__w412_f10.png (+ _night); alarm_2beds__w412_f10.png
Element: "Bed 3 / Bed 7 / Bed 9 / Isolation Bed 23" row titles on the round alarm measure about 24 sp (cap height about 16.7 dp); on alarm_2beds the "Bed 14 / Bed 12" titles are about 38-39 sp. Only the time "10:00" and "4 beds" reach 40 sp+.
Why it hurts: on a round alarm the bed number IS the task; the nurse reads which beds to turn in a 1-2 s glance. The brief fixes >= 40 sp for key numbers on alarm screens; the biggest type is spent on the clock and a bed count that carry no action.
Fix: bed number >= 40 sp (two-column grid of bed tiles, or a bed number in a 56 dp rounded tile with the position pictogram beside it); keep the time at 40 sp but demote "4 beds" to body size.

### ROUND-2  MINOR - Round checklist has two competing primaries and a weak selected state
Files: round_checklist__w412_f10.png, round_checklist__w412_f10_night.png
Element: per-bed "Turned to Back (check)" is an outlined button identical to "Other position" and "Can't turn" (all 2 dp light outline on navy, 64 dp); the filled amber "The other 2 turned as suggested (check)" pinned below is a second primary. After a tap, "(check) Can't turn" differs from the unselected neighbour only by a slightly lighter fill.
Why it hurts: at 1-2 s the nurse cannot tell which of five outline buttons per bed is the expected one, nor which choice is already registered; a tap on the wrong outline looks the same as the right one.
Fix: make "Turned to X" the filled per-bed action and the other two plain text-style actions; selected = filled with check icon at 3:1 against its neighbour.

### ROUND-3  MINOR - Position pictograms are indistinguishable at a glance
Files: round_alarm__w412_f10.png, round_7beds__w412_f10.png, turn_start__w412_f10.png, turn_detail_on_track__w412_f10.png (crop verified)
Element: 24 dp outlined pictogram of a body in a rectangle; Left 30 / Back / Right 30 differ only by a 2 dp tick on one side; the "to" arrow between from and to is a 10 dp glyph; at font scale 2.0 (round_checklist__w360_f20.png, turn_detail_*__w360_f20.png) the from -> to pair stacks and the arrow is left dangling at the end of the first line.
Why it hurts: the pictogram is the only non-text cue for "which side", yet it reads as the same icon three times; the arrow is the weakest element of the "from -> to" idea.
Fix: pictogram >= 40 dp with a filled body and a clearly tilted torso for left/right; arrow >= 20 dp stroke; at large text put "to" on its own line with the arrow leading.

### ROUND-4  MINOR - No progress and a 400 dp dead zone on the round alarm
Files: round_alarm__w412_f10.png, round_checklist__w412_f10.png
Element: round_alarm: list ends at 60 % of height, then about 400 dp of empty navy before "Start round"; round_checklist: header keeps the static "4 beds" and never shows "2 of 4 turned".
Why it hurts: with 4 beds the screen is mostly void while the data is small; with 7 beds the list runs into the buttons (see X-1). No progress means a nurse interrupted mid-round does not know where she was.
Fix: scale the bed tiles to fill the space (see ROUND-1); replace "4 beds" with "2 of 4 done" once started.

### ROUND-5  MINOR - Night: round checklist reason chips glare
File: round_checklist__w412_f10_night.png
Element: "Off unit / Procedure / Asleep/comfort / Unstable / Other" are near-white (#EAEDF5) filled pills on a near-black screen, 64 dp high.
Why it hurts: they are the brightest objects on a night screen in a room with sleeping children, brighter than the amber primary.
Fix: night chips = dark fill, light text, 2 dp outline; keep one bright object (the primary) per screen.

### REACT-1  MINOR - Reaction picker with many beds clips the guide and needs scrolling in an emergency
Files: reaction_20beds__w412_f10.png (+ _night), reaction_20beds__w360_f20.png
Element: the "Stop & call if you see" card is cut mid-line ("SpO2 BP or HR" half visible) by the bed picker; "TRANSFUSION STOPPED FOR:" then shows Bed 1-6 and half of row 3 of the 20 solid-red 56 dp buttons; at font 2.0 the picker sits after about 1,900 dp of scroll.
Why it hurts: the reaction screen should be readable in one view and the stop confirmation reachable at once; here the guide is clipped and the bed is found by scrolling a wall of identical red buttons (at night: 20 salmon slabs, the brightest block in the app).
Fix: show only beds with a running transfusion (usually 1-4), one pinned confirm row; allow the guide to scroll above it; at night use outlined salmon chips.

### REACT-2  MINOR - Reaction screen with no bed context has no bottom action
File: reaction_no_bed__w412_f10.png
Element: guide ends with about 200 dp of empty page; the only way out is the small top-left "Back"; header title "Suspected transfusion reaction" wraps with the orphan "reaction".
Fix: a full-width 64 dp "Back to Home" at the bottom; title on one line (smaller overline, or shorter title).

## GROUP 5 - SETUP CHECK, SETTINGS, START FORMS (setup_check, settings, start_*, turn_start*, turn_edit)

### SETUP-1  MINOR - Setup check: three competing buttons, repeated labels, clipped heading
Files: setup_check__w412_f10.png, setup_check__w412_f10_night.png, setup_check__w360_f20.png
Element: two solid-red "Fix: ..." buttons (56 dp) in the middle and a navy pinned "Test alarm in 1 minute"; each Fix label repeats the row title above it; header title wraps with the orphan "missed"; the "XIAOMI / REDMI / POCO - EXTRA STEPS" card heading wraps ("STEPS") and is cut by the footer; an unrelated "Ward beds" card sits between the checks and the device steps; at font 2.0 the icon column steals about 25 % of the width and sentences break every two words.
Why it hurts: the nurse's next step is a red Fix button, but the pinned primary at the bottom says Test; red here means "not set" rather than danger; the long page buries the Xiaomi steps.
Fix: one primary at the bottom that changes from "Fix next: Full-screen alarm" to "Test alarm in 1 minute"; Fix rows as amber-outline buttons; move Ward beds to Settings only.

### SETUP-2  MINOR - Settings: flat card stack, mis-aligned caption, weak night selected state
Files: settings__w412_f10.png, settings__w412_f10_night.png
Element: all sections are identical white cards; "Bed only - no names or MRN" is bold, near-black and indented about 12 dp from the field edge, so it reads like an error message; the "-" and "+" steppers are 56 dp boxes with a 10 dp glyph; at night the selected chip "(check) By the clock" is a barely lighter navy with a thin outline (about 1.3:1 against unselected chips) and relies on the check.
Fix: group by task with 24 dp gaps and dividers; caption regular weight aligned to the field edge; stepper glyphs 24 dp; selected chip = filled light with dark text at night.

### START-1  MINOR - Bed grid: cell content and row height change on selection
Files: turn_edit__w412_f10.png (Isolation Bed 23 selected: "(check) / Isolation / Bed 23" becomes three lines and row 5 grows from 63 dp to about 80 dp, pushing "+ Other bed" and all cards down), start_empty__w412_f10.png ("+ Other bed" and "Isolation Bed 23" wrap to two lines while other cells are one line).
Fix: fixed cell height 64 dp with two-line capacity, check mark as a corner badge not inline text.

### START-2  MINOR - Half-controls and a blank pill in the start/edit forms
Files: turn_edit__w412_f10.png (an empty white pill at 69 % height, "TURN EVERY" chips cut in half by the footer), start_empty__w412_f10.png / start_bed_picked__w412_f10.png ("STARTED AT" chips followed by the top half of another chip row cut by the footer), turn_start*__w412_f10.png.
Why it hurts: the scrolled form shows half-controls that look broken; the empty pill looks like a missing card. Scroll cue is counted in X-1; the blank pill is a separate polish defect.
Fix: do not render empty containers; add a 16 dp fade above the footer.

### START-3  MINOR - Turning start footer is crowded and its instruction reads as a caption
Files: turn_start__w412_f10.png, turn_start_bed_picked__w412_f10.png
Element: footer = bold summary line, letter-spaced caps instruction ("BED 6: LYING NOW? TAP TO START", about 17 sp) and three equal navy 66 dp buttons with the icon above the label, 8 dp apart.
Why it hurts: the one instruction that tells a new colleague what the buttons do is the same size and weight as a section label; three equal "primaries".
Fix: instruction in sentence case at 18-20 sp above the buttons; one visually primary option or a clear segmented control.

## GROUP 6 - REPOSITIONING DETAIL (turn_detail_*)

### TURN-1  MAJOR - Timeline markers show skipped, late and overdue as "done"
Files: turn_detail_skipped__w412_f10.png, turn_detail_on_track__w412_f10.png, turn_detail_overdue_missed__w412_f10.png (+ _night)
Element: "TURN TIMES" dots: skipped 08:00 (list says "skipped . Procedure") is a filled navy dot, identical to a completed turn; the +25-min late turn at 10:00 is the same filled navy dot as the on-time one; "missed" 08:00 and "overdue" 10:00 share one red rounded-square marker.
Why it hurts: pressure-injury prevention depends on seeing which turns did not happen; the strip a nurse glances at tells the opposite of the list below it (colour and shape say "done", only small grey text says skipped). The state vocabulary is not colour AND shape AND text.
Fix: marker set: check = done on time, check + clock = late (amber), dash = skipped (grey outlined), diamond-! = missed/overdue (red, same as Home), hollow = upcoming.

### TURN-2  MAJOR - At font 2.0 the skipped row overwrites its own time and the Stop column is clipped
File: turn_detail_skipped__w360_f20.png
Element: the Turns list renders "08skipped . Procedure" (the "08:00" time is overwritten by the status text); in "TURN TIMES" the "Stop / 20:00" column is cut at the right edge ("20:0" visible).
Why it hurts: the time is a key number and is destroyed; violates zero truncation.
Fix: at wrap, put the status on its own line under the label (as the other rows do); wrap the timeline chips to a second row.

### TURN-3  MINOR - "Longest gap without a turn (24 h)" is a statistic and visually heavy
Files: turn_detail_on_track__w412_f10.png, turn_detail_overdue_missed__w412_f10.png, turn_detail_*__w360_f20.png
Element: grey inset box, two-line label at left, "4 h 05 min" at right, as heavy as the "Lying: Left 30" status above it; at font 2.0 the label wraps to 5 lines squeezing the number.
Why it hurts: it conflicts with the maintainer's decision (2) (no statistics, Cerner is the record) and competes with the one fact the nurse needs (current position and next turn).
Fix: remove it, or if kept, a single quiet body-size line below the timeline.

### TURN-4  MINOR - Timeline and list polish
Files: turn_detail_skipped__w412_f10.png (the "Stop / 20:00" column sits about 5 dp higher than the other columns so its dot, chip and label are off the shared baseline), turn_detail_on_track__w412_f10.png (from line "Right 30 ->" ends in a dangling arrow, then the bold "Left 30" line; row heights of turned vs upcoming rows differ).
Fix: align all timeline columns to one baseline; "from -> to" on one row with the pictograms or stacked with the arrow leading.

### TURN-5  MINOR - Destructive "Stop plan" is the heaviest button at the bottom; "Edit" is clipped behind it
Files: turn_detail_on_track__w412_f10.png (the outlined "Edit positions, interval or stop time" is cut behind "Stop plan"), turn_detail_overdue_missed__w412_f10.png, turn_detail_stopped__w412_f10.png
Element: "Stop plan" and "Restart plan" are the same filled navy slab as "Start", at the lowest, easiest-to-hit position; the real primary ("Open round", amber) is mid-list.
Why it hurts: same issue as DETAIL-1; stopping a plan with a gloved thumb is one tap.
Fix: demote Stop plan to an outlined/text action with a confirm step; pin "Open round" as the bottom primary when a turn is due.

## GROUP 7 - UNDO TOAST, WIDGET, CROSS-SCREEN

### UNDO-1  MAJOR - "Undo" breaks into "Und / o" at font scale 2.0
File: undo_bar_on_home__w360_f20.png
Element: the toast "Bed 5 . transfusion started" wraps to three lines and the Undo button is squeezed so the word breaks mid-word as "Und" over "o".
Why it hurts: Undo is the only recovery from an accidental Start; its label is the one word that must never break. Violates zero truncation.
Fix: toast becomes a column at large text (message above, 64 dp full-width "Undo" below); give the button min-width = text width.

### WIDGET-1  MAJOR - Widget breaks the status word "OVERDUE" and balloons at font 2.0
File: widget__w360_f20.png
Element: "OVER / DUE . 30-min check" splits mid-word, the amber "Measured & documented (check)" button grows to a 4-line block taking half the row width, rows wrap to 4-6 lines.
Why it hurts: the status word is the only urgency text on the widget; a broken word reads as two words; the widget is looked at for under 1 s.
Fix: no wrap for the status word (own line/chip); limit the widget to bed, time and a diamond/bell icon; drop the inline confirm button from the widget.

### WIDGET-2  MINOR - Widget columns are ragged and rows inconsistent
Files: widget__w412_f10.png, widget__w412_f10_night.png
Element: the bed-name column starts at five different x positions because time and icon columns are content-width; only the Bed 12 row has a (3-line, about 15 sp) confirm button, Bed 7 (also overdue) has none; no row dividers; the white sheet shows no widget bounds.
Fix: fixed-width time column (one-line 28 sp) and icon column, bed name aligned; an action on every overdue row or on none.

### X-1  MAJOR - Content is hard-clipped behind pinned footers with no scroll cue
Files and elements:
- round_7beds__w412_f10.png: header says "7 beds" but only Bed 1-6 are visible; Bed 7 is hidden under "Start round", 8 dp gap, no fade - a nurse can skip a bed.
- home_20beds__w412_f10.png: "Bed 9" cut in half by the red bar.
- round_checklist__w412_f10.png: a card slice peeks above the amber button; detail_overdue_missed__w412_f10.png / turn_detail_on_track__w412_f10.png: the top outline of "Edit start time or duration" / "Edit positions..." is cut behind the footer; detail_ended_end_vs__w412_f10.png: the checkpoint card is cut by the red bar.
- start_empty / start_bed_picked / turn_edit / setup_check / settings (w412_f10): half-visible chip rows and card headings ("TURN EVERY", "XIAOMI / REDMI / POCO - EXTRA STEPS", the end of the ALARM card).
Why it hurts: the most dangerous variant is a clipped bed or overdue item that looks like the end of the list; at 1-2 s a nurse reads "all done". Hard cuts also look like rendering bugs.
Fix: a 16-24 dp gradient fade at the bottom of every scroll region, scroll region padded by the footer height, and a "+1 more" cue when the visible list is shorter than the count in the header.

### HOME-16  MINOR - "TURN" tag collides with the time on long bed names
Files: home_6beds__w412_f10.png ("Isolation Bed 23 VS 12:00": about 4 dp between the VS tag and the time), home_both_features__w360_f10.png (name wraps to two lines "Isolation / Bed 23" and the TURN tag drops to the baseline beside 12:00).
Fix: tag on its own line under the bed name (14 sp), time right-aligned and never sharing the name row.

### HOME-17  MINOR - Navigation and commit share one amber button style
Files: home_both_features__w412_f10.png ("Open round" navigates; "Bed 12 - Measured & documented (check)" commits a check), home_overdue_missed__w412_f10.png
Why it hurts: identical buttons, two kinds of consequence (go to a screen vs document); only the label differs, and the commit has just an Undo toast.
Fix: navigation = outlined or chevron row; commit = the only filled amber button.

## SUMMARY

What works (keep): strong type scale (key times 30 sp+ on Home, 50 sp on alarm), consistent card/chip/button system, all touch targets measured >= 56 dp with >= 8 dp gaps except those listed, secondary text 7.1:1 on white, coherent night theme, good use of icon+text on the reaction bar and OVERDUE diamond marker, ticks in selected chips (not colour-only).

Open Critical: 0
Open Major (17): HOME-1, HOME-2, HOME-3, HOME-5, HOME-6, ALARM-1, ALARM-2, ALARM-3, DETAIL-1, DETAIL-2, DETAIL-3, ROUND-1, TURN-1, TURN-2, UNDO-1, WIDGET-1, X-1
Open Minor (39): HOME-4, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17 (12); ALARM-4 to 9 (6); DETAIL-4 to 9 (6); ROUND-2 to 5 (4); REACT-1, 2 (2); SETUP-1, 2 (2); START-1, 2, 3 (3); TURN-3, 4, 5 (3); WIDGET-2 (1)

SCORE: 6 / 10 - does not pass (needs >= 9 with no open Major).


# Review 4 - Persona 1 (day-shift pediatric oncology nurse, 6 patients, one hand free)

Method: screenshots only (w412_f10 mainly; w360_f10, w360_f20 and night checked where relevant). Touch 1 = app icon where the task starts from the launcher. No source code read.

## Walkthrough table

| # | Task | Screens used | Touches I would make | Count | Budget | Result |
|---|------|--------------|----------------------|-------|--------|--------|
| 1 | See all beds, next, late | home_6beds / home_both_features (widget also lists the same with 0 touches) | 1 app icon. Header says "1 overdue . 2 due now", the late card has a red border and sits on top, the rest is sorted by time. | 1 | 1 | PASS |
| 2 | Acknowledge a transfusion check from the alarm | alarm_1bed (alarm_2beds when two are due) -> alarm_done_undo | 1 "Measured & documented" (with two beds due, one tap per bed, each button names its bed). The done screen shows "Bed 14 . 15-min check done" + Undo. | 1 (1 per bed) | 1 | PASS |
| 3 | Whole turning round, all as suggested | round_alarm -> round_checklist | 1 "Start round", 2 sticky orange "All N turned as suggested". (I only see the "The other 2 turned as suggested" variant in screenshots; the all-clear label is inferred.) From Home: "Open round" on the overdue turn card, then the same button (icon + 2 + 1 = 3). From the combined alarm the entry is the small "Start round . 10:00 . Bed 3, Bed 7" row (see F3). | 2 | 2 | PASS |
| 4 | Start a transfusion | home -> start_empty -> start_bed_picked | 1 icon, 2 "Start transfusion", 3 bed tile, 4 "Baseline done . Start". (Button reads "Pick a bed first" and is grey until a bed is chosen. Home then shows "Bed 5 . transfusion started" + Undo.) | 4 | 4 | PASS (at budget) |
| 5 | Start a repositioning plan | home -> turn_start -> turn_start_bed_picked | 1 icon, 2 "Start turning", 3 bed tile, 4 position tile under "BED 6: LYING NOW? TAP TO START". | 4 | 4 | PASS (at budget) |
| 6 | End a transfusion | home -> detail_ringing / detail_upcoming | 1 icon, 2 bed card, 3 "End transfusion" (always pinned at the bottom). Then Home shows an "ENDED . END VS" card with its own one-tap button (+1 for the end vital signs, not part of the ending itself). | 3 | 3 | PASS (at budget) |
| 7 | Suspected reaction -> see steps | home -> reaction_1bed / reaction_20beds; alarm -> same | From Home: 1 red "SUSPECTED REACTION?" bar (2 with the icon). From a transfusion alarm: 1. The guide opens on the 5 "DO NOW" steps at once. | 1 (2 from launcher) | 2 | PASS |
| 8 | Mark "Transfusion stopped" for a specific bed | home -> reaction_20beds -> detail_stopped_reaction | 1 icon, 2 red bar, 3 "Bed N" under "TRANSFUSION STOPPED FOR:" (with exactly one transfusion the button reads "Transfusion stopped - Bed 1" directly). Result card on Home: "STOPPED - REACTION, Stopped at 10:58". From the alarm: 2. | 3 | 3 | PASS (at budget) |
| 9 | One bed "can't turn" with a reason | round_alarm -> round_checklist | 1 "Start round", 2 "Can't turn" on that bed, 3 reason chip (Off unit / Procedure / Asleep/comfort / Unstable / Other). Finishing the round afterwards = +1 ("The other N turned as suggested"). | 3 | 3 | PASS (at budget) |

All nine tasks fit their touch budgets on the screenshots. The findings below are about clarity and reach, not about touch counts.

## Concrete findings

1. **Home, one-tap "Measured & documented" button is present on some DUE NOW cards and missing on others (home_6beds__w412_f10).**
   Element: card "Bed 3 . 15-min check . 10:55 . DUE NOW". Expected: like "Bed 14 . 11:05 . DUE NOW" (same badge, same progress marker) and the OVERDUE Bed 12 card, it carries the orange "Bed 3 . Measured & documented" button, or the card shows why not. Actual: Bed 3 has no button and no explanation; Bed 14 and Bed 12 have one. In home_20beds__w412_f10 none of the 20 DUE NOW cards has a button. I cannot tell from the screen which due card I can finish from Home, so for Bed 3 I would have to open the card first (extra touch and a second screen) while interrupted. Severity: medium.

2. **Home, "DUE NOW" badge hides how late a check is (home_6beds__w412_f10).**
   Element: Bed 3 card, time "10:55", badge "DUE NOW", with the header clock at 11:05. Expected (task 1, "what's late"): a check that is 10 minutes past gets a lateness cue like Bed 12's "OVERDUE +15 min". Actual: the badge is identical to Bed 14's "DUE NOW" at 11:05 (zero minutes late); I have to subtract 10:55 from 11:05 myself, and the header counts it as "due now", not late. Severity: low to medium.

3. **Combined alarm, "Start round" is the weakest and highest-placed control (alarm_transfusion_and_round__w412_f10; also w360_f10).**
   Element: outlined row "Start round . 10:00 . Bed 3, Bed 7", about 43 percent down the screen, under the MEASURE/DOCUMENT box. Expected: a round that is already late (the same fixture family shows Bed 7's turn at +65 min on Home) is reachable in the thumb zone like the other actions. Actual: all big actions (orange "Measured & documented", "Snooze 2 min", red reaction bar) sit in the bottom 30 percent, and the only way into the turning round is a small regular-weight row mid-screen above a large empty area. One-handed I have to stretch for it; it still costs only 2 touches. Severity: low.

4. **Round checklist, reason chips open half-hidden under the sticky button (round_checklist__w360_f10).**
   Element: reason chips under "Can't turn" on Bed 7 ("Off unit", "Procedure", ...). Expected: after the tap that reveals them, the chips are readable and tappable. Actual at 360 wide: only the top third of two chips shows, cut off by the sticky orange "The other 2 turned as suggested" button, so task 9's third touch needs an extra scroll first (at 412 wide Bed 7's five chips just fit, but the same will happen for lower beds such as Bed 9 and Isolation Bed 23). Severity: low (extra gesture, not extra touch).

5. **Stopped-reaction detail, text says alarms are cancelled but a live overdue check button remains (detail_stopped_reaction__w412_f10).**
   Element: card "Stopped at 10:58. Remaining alarms for this bed are cancelled." directly above the checkpoint "10:50 30-min check . overdue" with an active orange "30-min check . Measured & documented" button. Expected: on a bed stopped for a suspected reaction, nothing still invites me to record a routine 30-minute check (or the screen says what tapping it does). Actual: the red "overdue" badge and the orange button look just like a running transfusion, contradicting the sentence above; I would not know whether tapping logs a routine check or what. The same live "30-min check" button also shows on detail_ended_end_vs__w412_f10 below the "End VS" button. Severity: low to medium (documentation clarity on the most sensitive screen).

## Considered and not reported (not concrete, or maintainers' decisions / fixture artefacts)
- Reaction guide missing from round-only alarm and repositioning screens (maintainer decision).
- Long lists scrolling (round_7beds hides Bed 7; reaction_20beds clips the sign list; home_20beds) - on purpose.
- undo_bar_on_home shows "Bed 5 . transfusion started" while the list shows only Beds 12 and 14, and alarm times such as 11:20 under a "due now" 11:07 alarm, "10:00 is less than 30 min away" on turn_start with an 11:05 clock - sample-fixture inconsistencies, per the brief.
- No visible countdown on the Undo buttons (alarm_done_undo, undo bar) - only a guess about confusion, no concrete failure shown.
- Reason chips under "Can't turn" have no heading and the button already shows a check before a reason is chosen - could not tell from screenshots whether a reason is required.

# review4 - persona 2 (night-shift nurse, 3 AM, first-time user)

Screenshots only (night theme w412_f10_night mainly; w360_f10 / f20 and day only to confirm). No source read.
Touch 1 = app icon where the task starts from the launcher. "Inferred" = the exact screen was not in the folder.

## Walkthrough

| # | Task | Screens used | Touches | Budget | Result |
|---|------|--------------|---------|--------|--------|
| 1 | See all beds, next, late | home_both_features / home_6beds: header "5 BEDS RUNNING - 11:05 / 2 overdue", rows sorted by time, badges OVERDUE +65 min / DUE NOW / SOON / in 55 min (widget shows the same list with 0 touches) | icon = 1 | 1 | OK |
| 2 | Acknowledge a transfusion check | alarm_1bed -> gold "Measured & documented" -> alarm_done_undo ("Bed 14 - 15-min check done" + Undo). 2 beds in one alarm (alarm_2beds): one gold button per bed | 1 (1 per bed) | 1 | OK |
| 3 | Whole turning round, all as suggested | round_alarm -> "Start round" -> round_checklist -> sticky gold "All N turned as suggested" (screenshot only shows the variant "The other 2 turned as suggested", so the all-clean label is inferred) | 2 | 2 | OK (inferred last label) |
| 4 | Start a transfusion | home -> "Start transfusion" -> start_empty -> tap "Bed 5" -> start_bed_picked -> "Baseline done - Start" (time defaults to Now, 2 h) -> Home with Undo bar | icon + 3 = 4 | 4 | OK, no slack |
| 5 | Start repositioning plan | home -> "Start turning" -> turn_start -> tap "Bed 6" -> turn_start_bed_picked ("BED 6: LYING NOW? TAP TO START") -> tap Left 30 / Back / Right 30 | icon + 3 = 4 | 4 | OK, no slack |
| 6 | End a transfusion | home -> tap bed card -> detail_ringing / detail_upcoming -> "End transfusion" (then Home shows End VS card with its own gold button: not counted) | icon + 2 = 3 | 3 | OK, no slack |
| 7 | Suspected reaction -> steps | home or any transfusion alarm -> red "SUSPECTED REACTION?" banner -> reaction_1bed (DO NOW 1-5 and STOP & CALL list fully visible, no scroll) | 1 (2 from launcher) | 2 | OK |
| 8 | "Transfusion stopped" for a bed | home -> red banner -> reaction_20beds: "TRANSFUSION STOPPED FOR:" bed buttons (1 bed: "Transfusion stopped - Bed 1") -> tap the bed -> home_stopped_reaction card | 2 from Home (3 from launcher) | 3 | OK |
| 9 | One bed "can't turn" + reason | round_alarm -> "Start round" -> round_checklist -> "Can't turn" on the bed -> reason chip (Off unit / Procedure / Asleep-comfort / Unstable / Other) | 3 to mark; 4 to leave the round finished (sticky "The other N turned as suggested") | 3 | Marking meets budget; finishing the round = 4 (finding 4) |

## Concrete findings

1. round_checklist (night, Bed 7 card; also w360_f20 Bed 9): the check mark means two different things. Expected: only the active choice carries a check. Actual: "Turned to Right 30 deg [check]" (check at the end, action label) and "[check] Can't turn" (check at the start, selected state) are both shown on the same card, so Bed 7 reads as both "turned" and "can't turn". Same on Bed 9: "Turned to Left 30 deg [check]" together with "[check] Other position". An untouched bed (Bed 3, "Turned to Back [check]") looks exactly like a chosen one, so I cannot tell which state each bed is in.

2. round_checklist (night), reason chips "Off unit / Procedure / Asleep/comfort / Unstable / Other": expected a dimmed night style like the dark outlined buttons around them. Actual: solid near-white fill (#E8EDF6, identical in the day variant), brighter than the gold primary button (#C98F36) and than every other control on the screen. In a dark room with a sleeping child they are the brightest block on the screen exactly at the moment of marking a bed.

3. round_checklist (w360_f10, Bed 7 after "Can't turn"): expected the revealed reason chips to be tappable in view. Actual: only the top edge of the first two chips ("Off unit", "Procedure") shows above the sticky gold "The other 2 turned as suggested" bar; the reason row is hidden behind it, so choosing the reason needs an extra scroll before touch 3. The only large target visible beside the clipped chips is the finish button.

4. round_checklist, sticky gold button "The other 2 turned as suggested [check]" (task 9): expected that marking Can't turn + reason (3 touches from the alarm) leaves the round done. Actual: the button text explicitly excludes the flagged bed, and no screenshot shows any state where the reason chip closes the round, so leaving the round finished costs a 4th touch (alarm -> Start round -> Can't turn -> reason -> sticky button), one over the budget of 3. (Marking alone is 3.)

5. Transfusion detail timelines (detail_upcoming, detail_ringing, detail_overdue_missed, detail_on_time_late), "TIME FROM START" row, marker above "End 11:52": expected an upcoming marker to be hollow like "15 min" and "30 min". Actual: "End" is a filled #E8EDF6 dot, pixel-identical to the completed "0" dot (10:52 logged), so a scan of the row reads the end as already reached. Same on turn_detail_skipped: "Stop 20:00" is filled although it is in the future.

6. turn_detail_skipped, "TURN TIMES" strip, 08:00 marker: expected a skipped turn to look different from a turned one. Actual: the skipped 08:00 is the same filled white dot as a completed turn (turn_detail_on_track 08:00 / 10:00), unlike missed turns (salmon squares in turn_detail_overdue_missed). Only the small "skipped - Procedure" text in the list below tells them apart.

7. start_empty vs start_bed_picked (and turn_start vs turn_start_bed_picked), bottom commit buttons: expected the button to look more active once a bed is picked. Actual: disabled "Pick a bed first" / the three disabled position buttons are #394252, the enabled "Baseline done - Start" / Left 30 / Back / Right 30 are darker #1E2C47. Enabling makes the button dimmer, so in a dark room only the label text shows that it became live; the commit action is also navy, not the gold used for primary actions elsewhere.

8. home_end_vs_pending vs detail_ended_end_vs, Bed 12 30-min check: expected the same word and state in both places. Actual: Home card text says "Measure, then document in Cerner - 30-min check missed" (reads as closed, and Home offers only the End VS gold button), while the detail screen labels the same check "overdue" and still offers a live gold "30-min check - Measured & documented" button. After tapping End VS on Home the nurse cannot tell that one more item remains behind the card.

9. detail_stopped_reaction (Bed 12, red header): expected the reaction follow-up to be the most prominent control after stopping. Actual: the most prominent element is a gold routine button "30-min check - Measured & documented" next to a red "overdue" tag, directly under "Stopped at 10:58. Remaining alarms for this bed are cancelled."; "Open the reaction steps" is a low-emphasis navy outline pinned at the very bottom, below an empty gap and below "Remove from Home".

# Review 4 - Persona 3 (gloved nurse, mid-procedure, first-time user, screenshots only)

Screens read: w412_f10, w360_f10 (main), w360_f20 and night variants as cross-checks. No source code opened.
Note: f20 captures are tall full-page renders (1080x7800), so I did not judge on-device overflow from them, only wrapping and content.

## Walkthrough table

| # | Task | Screens used | Touches (icon = 1 where it starts from the launcher) | Budget | Result |
|---|------|--------------|------------------------------------------------------|--------|--------|
| 1 | See all beds, next, late | home_both_features, home_6beds, home_20beds (and widget) | App icon -> Home answers it: header "5 BEDS RUNNING / 2 overdue", red-outlined overdue cards first, "SOON - 2 min", "in 55 min". = 1 | 1 | PASS (20 beds: ~7 cards visible, rest scrolls = intended) |
| 2 | Acknowledge transfusion check from alarm | alarm_1bed (or alarm_2beds), then alarm_done_undo | "Measured & documented ✓" (big amber button, bottom). = 1. Two beds due = one button per bed (2 total, each labelled "Bed 14 / Bed 12 - Measured & documented"). Result screen shows "✓ Bed 14 - 15-min check done" + Undo. | 1 | PASS |
| 3 | Whole turning round, all as suggested | round_alarm -> round_checklist | "Start round" (1) -> sticky bottom amber button "...turned as suggested ✓" (2). (Shown with label "The other 2 turned as suggested ✓" when 2 beds were already marked; with nothing marked I infer it covers all beds.) = 2 | 2 | PASS (exactly at budget) |
| 4 | Start a transfusion | home_empty -> start_empty -> start_bed_picked | icon (1) -> "Start transfusion" (2) -> "Bed N" (3) -> "Baseline done · Start" (4) = 4. Defaults "Now / 2 h" shown only as text in the footer on w360 (see finding 4). | 4 | PASS (exactly at budget) |
| 5 | Start a repositioning plan | home_empty -> turn_start -> turn_start_bed_picked | icon (1) -> "Start turning" (2) -> "Bed N" (3) -> "Left 30° / Back / Right 30°" under "BED 6: LYING NOW? TAP TO START" (4) = 4. Bottom row of beds (Bed 19, Isolation Bed 23, + Other bed) is half under the sticky panel at w360 (scrolls = intended). | 4 | PASS (exactly at budget) |
| 6 | End a transfusion | home_* -> detail_upcoming / detail_ringing | icon (1) -> bed card (2) -> navy "End transfusion" pinned at the bottom (3) = 3. Then Home shows "ENDED - END VS" with its own amber button (separate follow-up, detail_ended_end_vs). | 3 | PASS (exactly at budget) |
| 7 | Suspected reaction -> steps | home_* or alarm_* -> reaction_1bed / reaction_20beds | From Home: icon (1) -> red "SUSPECTED REACTION? STOP the transfusion & call" banner (2) = 2. From an alarm: banner (1). Steps 1-5 "DO NOW" are readable without scrolling at w360. | 2 | PASS |
| 8 | "Transfusion stopped" for a specific bed | home_* -> reaction_1bed / reaction_20beds | icon (1) -> banner (2) -> red "Transfusion stopped - Bed N" (3) = 3. One bed: single named button. 20 beds: "TRANSFUSION STOPPED FOR:" grid of Bed buttons in a bottom panel (only ~9 visible at w360, rest scrolls = intended). From alarm: 2. | 3 | PASS (exactly at budget) |
| 9 | One bed in a round "can't turn" + reason | round_alarm -> round_checklist | "Start round" (1) -> "Can't turn" on that bed (2) -> reason chip: Off unit / Procedure / Asleep/comfort / Unstable / Other (3) = 3. Closing the round afterwards needs the sticky "The other N turned as suggested ✓" = 4th touch (not counted in the budget). | 3 | PASS on count, but see finding 3 (chips clipped at w360) |

## Concrete findings

1. **Home list: a "DUE NOW" card has no one-touch acknowledge button while its neighbours do** (home_6beds, w360_f10, w412_f10 and w360_f20).
   Element: card "Bed 3 VS - 15-min check - 10:55 - DUE NOW". Expected: like "Bed 12 OVERDUE" and "Bed 14 DUE NOW" directly above and below it, an amber "Bed 3 · Measured & documented ✓" button. Actual: Bed 3 has only the chevron, no button, and nothing on the card says why. The bed that fell due earliest (10:55) is the one without the shortcut, while Bed 14 (11:05) has it. From Home, Bed 3 costs 2 touches after the icon (open card, then button) instead of 1. At a glance two identical "DUE NOW" pills look like they act the same way but do not.

2. **Home-screen widget at 360 wide breaks the word "OVERDUE" in half** (widget, w360_f10).
   Element: row for Bed 12 (10:50), the text block next to the amber "Measured & documented ✓" button. Expected: "OVERDUE · 30-min check" readable in 1-2 lines, as at w412_f10. Actual: the button takes ~40% of the row so the text column is ~100 px wide and wraps as "OVER / DUE · / 30-min / check" (5 lines); "OVERDUE" is split mid-word. The Bed 12 row is more than twice as tall as the Bed 7 row above it. The late item in the glance view is the hardest one to read.

3. **Round checklist: the reason chips for "Can't turn" are cut off by the sticky amber bar** (round_checklist, w360_f10; the same layout is fully visible at w412_f10).
   Element: reason chips "Off unit / Procedure / Asleep/comfort / Unstable / Other" under Bed 7 after "✓ Can't turn". Expected: after the 2nd touch the 3rd target (a reason) is on screen. Actual at w360: only the top ~70 px of the first chip row peeks out above the sticky "The other 2 turned as suggested ✓" button; the chips are unreadable until the list is scrolled. The only fully visible, large target on screen is the amber "turned as suggested" bar, which a gloved thumb can hit instead and leave Bed 7 as "Can't turn" with no reason. The screenshot shows no auto-scroll; if the app scrolls the chips into view on tap, this is moot, but nothing in the captured state shows it.

4. **Start screens at 360 wide show no sign that there are more options below the bed grid** (start_bed_picked and turn_start_bed_picked / turn_start_options, w360_f10).
   Element: footer line "Started 11:05 · planned 2 h · ends 13:05" (transfusion) and "First turn 12:00, then every 2 h" (turning). Expected: a visible cue (as w412_f10 gives with the cut-off "STARTED AT" card) that start time, planned duration (1/2/3/4 h, Custom) and turn interval/positions can be changed. Actual: at w360_f10 the bed grid fills the screen and ends exactly above the footer, and the footer reads as plain text. The duration and interval controls only appear in the w360_f20 capture (below the grid). A first-time nurse with a 4 h bag or a 4-hourly turn plan has no visible way to know she can change the defaults.

5. **Turning detail for an overdue bed labels the turn that is due now as "Next: Back at 12:00"** (turn_detail_overdue_missed, w360_f10 and w412_f10).
   Element: NOW card, line "Lying: Left 30° / Next: Back at 12:00", read together with the TURNS list ("08:00 Turn missed", "10:00 Turn overdue") and the TURN TIMES icons (12:00 shows the Right-30° icon; Home card for this bed says "Left 30° → Back" at 10:00 OVERDUE +65 min). Expected: "Back" shown as the overdue, do-it-now turn (10:00). Actual: "Back" is paired with 12:00, so it reads as if the turn to Back is scheduled at 12:00 and nothing is late; the overdue rows (08:00, 10:00) show only "Turn" with no target position. The only cue that action is needed now is the amber "Open round" button.

## Things checked that were fine (not findings)

- Alarm screens: bed number, time, check name, MEASURE/DOCUMENT steps and the single big amber button all read at a glance in day and night; Snooze states ("Snoozed - rings again at...", "No snoozes left") are clear.
- Undo bars: "✓ Bed 14 - 15-min check done / Undo" and "Bed 5 · transfusion started / Undo" are visible and large.
- Reaction banner appears on transfusion screens only, as decided; absent on home_only_turning, home_empty and round screens.
- Bed pickers fit 22 buttons on one screen for transfusion at w360/w412.
