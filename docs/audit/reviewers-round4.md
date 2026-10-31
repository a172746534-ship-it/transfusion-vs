# Design review 6 - ward alarm companion (screenshots only)

Reviewer notes. Folder: scratchpad\review6. Measurements were taken from the PNGs (3 px = 1 dp at w412/w360 f10).
Severity: Critical / Major / Minor. IDs are stable (D-nn).

## Measured facts (so they are not re-argued)
- Home bottom actions: amber primary 64 dp tall, reaction bar 56 dp, Start buttons 56 dp, gaps 8 dp. Pass.
- Day theme: card subtitle #4C5870 on white = 7.15:1. Header label #C3CEE0 on #13233F = 9.9:1. Amber chip outline #D28000 on white about 3.1:1 (pass, barely). Unfilled rail line #87888D on white about 3.6:1. Pass.
- Key numbers on Home cards: cap height about 18 dp, so about 26 sp. Pass (>= 24 sp).
- Nothing truncated on Home at w360 f10, w360 f20.

## GROUP 1 - HOME (home_*, undo_bar_on_home)

### D-01 Minor - Type stripe disappears on exactly the cards that matter most
- Screen/element: Home list, every OVERDUE and DUE card. Files: home_both_features__w412_f10.png (cards 1-2), home_6beds__w412_f10.png (cards 1-3).
- What: upcoming cards carry the 6 dp type stripe (purple VS / teal TURN); overdue cards swap it for a red frame and due cards for a navy frame, so the stripe is gone. Type is then carried only by the 14 dp icon and the 13 sp "VS"/"TURN" word.
- Why it hurts: in a mixed list the nurse scans down the left edge for "which are transfusions"; the two most urgent cards break that scan line and the card direction the maintainer picked looks inconsistent.
- Fix: keep the stripe on every state and draw the red/navy frame around it (stripe stays 6 dp inside the frame), or add a stripe-coloured left cap inside the frame.

### D-02 Minor - Amber is used for three different meanings
- Screen/element: Home. Primary button "Measured & documented / Open round" (home_6beds__w412_f10.png), SOON chip + half-filled rail node (same file), setup banner (home_setup_warning__w412_f10.png).
- What: one amber (#F5A623 fill, #D28000 outline) means "soon", "do this now", and "your phone is misconfigured".
- Why it hurts: at a 1-2 s glance a big amber slab at the bottom reads as a warning state, not as the button; the nurse starts to read the SOON chip as "tap here".
- Fix: give the primary action its own treatment (solid navy/ink with white text, or the type colour of the task) and keep amber only for "soon" and "setup".

### D-03 Minor - "Alarm ringing. Open it" banner is as quiet as a secondary button
- Screen/element: Home, ringing banner. File: home_ringing__w412_f10.png (night: home_ringing__w412_f10_night.png).
- What: white box, thin navy border, 20 sp text, tiny bell. It has the same weight as the reaction bar and the Start buttons below it, and the DUE NOW card above looks exactly like a non-ringing DUE card (home_6beds__w412_f10.png).
- Why it hurts: "unmistakable when something needs them". Ringing is the highest-urgency state in the app, yet its only Home cue is neutral.
- Fix: fill the banner with the DUE colour (navy day / light-blue night) with inverted text and a pulsing bell, and give the ringing card a distinct marker (bell in the chip).

### D-04 Minor - Setup banner text wraps with an orphan word
- Screen/element: Setup warning banner. File: home_setup_warning__w412_f10.png (and _night).
- What: "Setup: 2 to fix. Alarms may not / ring" - the word "ring" sits alone on line 2 while a 56 dp box is reserved.
- Why it hurts: looks unfinished at the most common width (412) for a safety message; the actionable word is detached.
- Fix: shorten padding / chevron gap or set the text to balanced wrap so it breaks "Setup: 2 to fix. / Alarms may not ring".

### D-05 Minor - Overdue rail marker "!" is unreadable
- Screen/element: progress rail, overdue diamond. File: home_both_features__w412_f10.png (cards 1-2, x about 440 disp).
- What: the diamond is about 13 dp wide and the "!" inside is about 3 x 6 dp, a pixel smudge at arm's length.
- Why it hurts: it is the marker meant to say "the next check is late" and it carries no information beyond a red dot; shape-difference requirement is met only by the diamond silhouette.
- Fix: make the overdue marker 18-20 dp and drop the "!" (the diamond silhouette plus red is enough) or enlarge the glyph to 10 dp tall.

### D-06 Minor - Rail has no key; "next node" accent vanishes at night
- Screen/element: progress rail on upcoming cards. Files: home_6beds__w412_f10.png (blue ring on the 2-h card) vs home_6beds__w412_f10_night.png (grey ring).
- What: day uses a bright blue ring for "next check"; night turns it into a dull grey-blue ring that looks almost like the unfilled future nodes. Nothing explains what the nodes stand for.
- Why it hurts: zero-training colleagues cannot tell what the dots mean; at night the only position cue weakens.
- Fix: use a night accent (light blue #8CB4FF) 3:1 against the card for the "next" ring, and make the ring 2 dp heavier than future nodes in both themes.

### D-07 Minor - Ended-VS card breaks the card grid and the time is unlabelled
- Screen/element: end-VS card. File: home_end_vs_pending__w412_f10.png (night: _night.png).
- What: subtitle wraps to two lines with "check overdue" orphaned; the status chip "ENDED - END VS" is pushed left under the text instead of sitting right under the time like on every other card; "10:58" at top right has no label (end time? due time?).
- Why it hurts: the one card with a different layout is the one that asks the nurse to act now; chip position is the learned place to look.
- Fix: put the chip right-aligned under the time as elsewhere, put "Ended 10:58" as a small caption above the time, and let the sentence wrap inside a fixed two-line slot.

### D-08 Minor - Header status line gives no urgency cue
- Screen/element: Home header. Files: home_6beds__w412_f10.png ("1 overdue - 2 due"), home_overdue_missed__w412_f10.png, home_empty__w412_f10.png ("All quiet").
- What: "All quiet", "All on time", "2 overdue" all render in the same white 28 sp on the same navy bar.
- Why it hurts: the first thing the eye lands on at the top does not change when the state changes; urgency is only visible 1-2 cards lower.
- Fix: tint the header bar (or a 6 dp bottom edge) with the worst state colour and put the same state icon as the chip in front of the status text.

### D-09 Minor - Settings gear has no visible target
- Screen/element: gear at top-right, every Home file.
- What: a bare 22 dp glyph with no container or pressed shape; cannot be confirmed as >= 56 dp.
- Why it hurts: gloved thumb at the top corner of a tall phone; the hit-area is invisible, and it is the only route to setup check.
- Fix: draw a 56 dp tonal circle/rounded square behind the gear, or move it to the bottom cluster.

### D-10 Major - At font 2.0 the Start buttons leave the pinned bottom zone and the header breaks the list
- Screen/element: Start transfusion / Start turning at f20. Files: home_both_features__w360_f20.png, home_ringing__w360_f20.png, home_end_vs_pending__w360_f20.png, home_20beds__w360_f20.png.
- What: at f10 the order from bottom is Start, reaction, primary, banner. At f20 Start jumps to directly under the last card (scrolls with the list) while reaction + primary stay at the bottom; with 20 patients Start is not on the screen at all.
- Why it hurts: the most-used action moves out of the bottom third and its place changes with the font setting; an older nurse using large text must scroll to start a transfusion.
- Fix: keep Start pinned at the bottom at f20 (two stacked 56 dp buttons are about 120 dp) and let the cluster above it collapse into a single-line reaction link.

### D-11 Minor - Header wraps badly at font 2.0
- Screen/element: Home header at f20. Files: home_both_features__w360_f20.png ("5 / RUNNING" on two lines, 11:05 touching it), home_20beds__w360_f20.png ("20 / RUNNING / All on / time", three lines), home_end_vs_pending__w360_f20.png ("1 STILL / OPEN").
- What: the small label and the status line break into 3-4 fragments; the clock is jammed against "RUNNING" with no gap; the gear shrinks relative to text.
- Why it hurts: the header becomes 130 dp of broken words; no truncation, but unreadable at a glance.
- Fix: let the clock drop to its own row at large font, keep label and status on one line each (label may drop to 12 sp), and reserve 16 dp between text and clock.

### D-12 Minor - Night and day: cards sit at about 1.14:1 against the page; night amber slab is the brightest object
- Screen/element: night Home. Files: home_6beds__w412_f10_night.png, home_empty__w412_f10_night.png.
- What: card #131C30-ish on page #0A101C gives roughly 1.1:1, so card edges are only implied by the stripe/text; the primary button is a full-width 64 dp amber (#C8902F, about 70% of the day luminance) and is the brightest object by far.
- Why it hurts: in a dark room with sleeping children a 380 x 64 dp amber block lights the face; the card edges are unclear for gloved tapping.
- Fix: night primary becomes a dark-amber fill (#6B4A12) with an amber outline and light text, and cards get a 1 dp #2A3550 outline.

### D-13 Minor - Empty state is bare
- Screen/element: Home empty. File: home_empty__w412_f10.png.
- What: one white text card at the top, 70% of the screen is blank, the two Start buttons are the only colour. No icon, no hint that the second Start exists for rounds (the text only mentions a transfusion).
- Why it hurts: it is the screen colleagues see first with zero training, and the "creative in look" brief gets nothing here; the sentence leads people to the left button only.
- Fix: add a calm illustration (droplet + turning arrow in the two type colours) above the sentence and place the card nearer the buttons.

### D-14 Minor - Tight row at 360 with 10-digit numbers
- Screen/element: card header row with a 10-digit MRN. File: home_both_features__w360_f10.png (7654800123 TURN 12:00 gap about 7 dp), home_6beds__w360_f10.png.
- What: "TURN" touches the time with a 7 dp gap; nothing is cut but the row reads as one string.
- Fix: drop the type word to 12 sp when the MRN is > 8 digits or move it under the number.


## GROUP 2 - ALARM SCREENS (alarm_*)

Measured: alarm actions are 64 dp (reaction, snooze) and 96 dp (primary), gaps 12 dp, side margin 16 dp. MRN digits about 35 dp tall (about 48 sp) on the single-patient alarm, about 29 dp (about 40 sp) on the two-patient cards. Contrast of all alarm text >= 7:1. These pass.

### D-15 Major - Two-patient alarm: second card has its own small confirm button that looks like the big one
- Screen/element: inline amber "Measured & documented" inside the 7654812 card. Files: alarm_2beds__w412_f10.png, alarm_2beds__w412_f10_night.png, alarm_2beds__w360_f10.png, alarm_2beds__w360_f20.png.
- What: the screen has two amber confirm buttons with identical label: a 51 dp one inside the second patient's card (mid-screen) and the 96 dp one at the bottom, which carries the number 7654814 (the first patient). The first card has no button of its own.
- Why it hurts: this is a documentation-safety hazard. A nurse who taps the first amber thing she sees confirms the wrong patient's check; the inline button is also below the 64 dp alarm-action minimum (and below 56 dp), and it breaks "one primary action per screen, in the bottom third".
- Fix: remove the inline button. Keep one primary at the bottom that names the patient and make the two cards a selector (tap card to choose which one the primary confirms, selected card gets the amber frame), or stack two 64 dp+ buttons at the bottom, each carrying its own MRN.

### D-16 Major - On a 360 x 800 phone the "what to do" steps are cut off in the two-patient and combined alarms
- Screen/element: MEASURE / DOCUMENT steps box. Files: alarm_2beds__w360_f10.png (step 2 "DOCUMENT in Cerner, right away" is not visible, box fades out after step 1), alarm_transfusion_and_round__w360_f10.png (step 2 text half-faded at the box bottom edge). At w412 (alarm_2beds__w412_f10.png) it fits.
- What: the middle region is shorter than its content at the most common Android size; the box gets a fade-out edge instead of showing step 2.
- Why it hurts: "zero truncation". The only text that tells an untrained colleague what to do next is hidden exactly when two patients are due, which is the busiest moment; nobody scrolls a ringing alarm.
- Fix: collapse the steps to one 2-line row when space is short ("Measure T - HR - RR - BP - SpO2, then document in Cerner now"), put MRN and time on one row in the combined and 2-patient layouts, and make the MRN 40 sp instead of 48 sp there.

### D-17 Major - Done/Undo screen: the only action sits in the upper-middle of the screen
- Screen/element: "Undo" button. Files: alarm_done_undo__w412_f10.png (button at about 43-50 percent of screen height), alarm_done_undo__w412_f10_night.png.
- What: after the thumb taps the bottom amber button, the screen becomes a centred stack (tick, number, text, Undo, "Closes in 5 s") with the Undo button about 330 dp above where the thumb just was, and the bottom 45 percent is empty.
- Why it hurts: Undo is the mistap recovery for a clinical confirmation and exists for 5 seconds; it must be reachable by the same thumb without regripping. It violates "primary action in the bottom third".
- Fix: anchor Undo at the bottom (same slot as the amber button, 96 dp, same width), centre the tick and number above it, and show the 5 s as a draining bar inside the Undo button instead of the static caption "Closes in 5 s".

### D-18 Minor - "DUE NOW" is amber on the alarm but navy on Home; amber means "soon" on Home
- Files: alarm_1bed__w412_f10.png (amber filled chip) vs home_6beds__w412_f10.png (navy chip DUE NOW, amber outline SOON).
- Why it hurts: the same state is drawn in two different colours on the two most-used screens and the same colour means two different states; colleagues learn "amber = relax" on Home, then see amber on a ringing alarm.
- Fix: choose one mapping across the app: due = filled navy/white (day) and filled light-blue (night) on Home AND on the alarm chip, or recolour Home DUE to the alarm amber and move SOON to a neutral outline.

### D-19 Minor - Red reaction slab is the loudest object on a routine alarm
- Files: alarm_1bed__w412_f10.png, alarm_snoozed__w412_f10.png.
- What: a full-width filled red 64 dp block with white text sits above Snooze on every routine 15-min check; the eye goes to it before the amber primary.
- Why it hurts: red should mean "danger" and a routine vitals reminder is not; the persistent red dilutes it, and it sits 12 dp above Snooze so a thumb aiming for Snooze can land on the reaction flow.
- Fix: red outline + red icon on the dark surface (fill only on the reaction screen itself), and put 24 dp of space or the Snooze button first, reaction last.

### D-20 Minor - Combined alarm hides the round's 65 minutes of lateness in a neutral button
- File: alarm_transfusion_and_round__w412_f10.png (night: _night.png).
- What: "Start round - 10:00 - +65 min" is a white-outline button in 16 sp regular text; three facts on one line; no red, no OVERDUE word, although Home draws the same item as the loudest red card.
- Why it hurts: states are not drawn the same way twice; at a 1-2 s glance a 65-minute overdue turning round looks like an optional shortcut.
- Fix: split into label and value: "ROUND OVERDUE" red chip + "+65 min" in 20 sp bold and keep the outline for the button body.

### D-21 Minor - Big time "11:07" on the alarm has no caption
- Files: alarm_1bed__w412_f10.png, alarm_snoozed__w412_f10.png.
- What: a 46 sp time under the MRN with no "Due" / "Was due". In the snoozed state the same 11:07 appears three times (chip, big time, "rings again at 11:07") and the original due time is lost.
- Fix: caption "DUE" above the big time; in the snoozed state show "was due 11:05" small, "rings again 11:07" once.

### D-22 Minor - Snoozed and No-snoozes-left states are hard to tell from a live alarm
- Files: alarm_snoozed__w412_f10.png (small outline chip, snooze button replaced by plain text), alarm_snooze_used_up__w412_f10.png (just the text "No snoozes left").
- What: the whole screen looks like the live due alarm; only a 13 sp chip and one line of body text change. The last-chance state has no stronger shape or colour.
- Why it hurts: the nurse cannot tell whether the phone is still ringing or the app is waiting, and an exhausted-snooze alarm should read as the most urgent.
- Fix: snoozed = dim the primary to an outline and show a count-down ring; no-snooze-left = chip "LAST CALL" with the same shape as the Home OVERDUE chip.

### D-23 Minor - The steps box looks like a checklist but is not tappable; adds a fourth accent hue
- Files: alarm_1bed__w412_f10.png (numbered light-blue circles 1, 2, light-blue labels).
- Why it hurts: round filled circles with numerals read as radio/check buttons to a nurse in a hurry; blue is a fourth accent next to amber, red and white on a screen that should have two.
- Fix: draw the numerals as plain text in white (no circle) or make the step label amber-tinted white and use a thin vertical connector line.

### D-24 Minor - Redundant headline on the two-patient alarm
- File: alarm_2beds__w412_f10.png.
- What: chip "DUE NOW" directly above the headline "2 patients due now".
- Fix: drop the chip or change the headline to "2 patients".


## GROUP 3 - TRANSFUSION DETAIL (detail_*)

Measured: on detail screens the reaction bar and "End transfusion" are 53 dp tall (Home draws the same reaction bar at 56 dp), amber action 61 dp, gap between pinned buttons 18 dp. Header MRN digits 17 dp tall (about 25 sp); NEXT time 29 dp (about 40 sp). Contrast is fine everywhere.

### D-25 Major - Reaction bar and End transfusion are 53 dp on Detail (limit 56 dp)
- Screen/element: pinned "SUSPECTED REACTION?" bar and "End transfusion" button. Files: detail_ringing__w412_f10.png, detail_overdue_missed__w412_f10.png, detail_upcoming__w412_f10.png (and every w360 variant); pixel measure 160 px / 3 = 53.3 dp, while home_both_features__w412_f10.png shows the same bar at 56 dp.
- Why it hurts: gloved thumb, one hand, and this is the safety-critical bar; it is also inconsistent: same component, two heights.
- Fix: set min height 56 dp on both (64 dp for the reaction bar on alarm-like screens) and keep one component for the bar everywhere.

### D-26 Minor - Detail on a 360 x 800 phone: the pinned stack eats the screen, checkpoints are hidden
- Files: detail_overdue_missed__w360_f10.png (CHECKPOINTS card shows one row and a faded half row), detail_ended_end_vs__w360_f10.png (CHECKPOINTS card is entirely under the fade), detail_stopped_reaction__w360_f10.png (the inline "30-min check - Measured & documented" button is cut in half), detail_upcoming__w360_f10.png (End check row cut). At f20 (detail_on_time_late__w360_f20.png) the pinned stack is about 370 dp, nearly half a phone.
- Why it hurts: the header wraps to 4 lines at 360 (about 150 dp), the pinned stack is about 200 dp, so the scroll window is about 120 dp; the record of which checks were late is invisible without scrolling and a half-hidden button reads as broken.
- Fix: make the header one 56 dp bar (Back arrow + number + type word on one row, "Started ... planned ..." in the first card), drop the caption under TIME FROM START at narrow widths, and fold End transfusion into the scroll area (see D-28).

### D-27 Minor - The same checkpoint times are drawn three times
- Files: detail_upcoming__w412_f10.png (NEXT 11:07, chip 11:07, list row 11:07), detail_on_time_late__w412_f10.png.
- What: NEXT card, TIME FROM START chips and CHECKPOINTS list all repeat the times; the chips use only 60 percent of the card width (right 40 percent blank) with "15 min"/"30 min" labels 8 dp apart; chips look like buttons but are not tappable; in the list "logged" and "upcoming" are the same grey bold, so a finished row and a future row differ only by the word.
- Fix: keep NEXT and the list; turn the timeline into a thin full-width bar of the same dots as the Home rail, and give list statuses an icon (check for logged/on time, clock for upcoming).

### D-28 Minor - "End transfusion" sits in the lowest slot, below the confirm button
- Files: detail_ringing__w412_f10.png, detail_overdue_missed__w412_f10.png (order bottom to top: End transfusion, amber confirm, reaction).
- Why it hurts: on the alarm the lowest slot is the confirm button; here the lowest slot is a destructive end action in the same outline style as every secondary button, 17 dp from the confirm. A thumb that learned "bottom = confirm" can end a transfusion.
- Fix: put the confirm button last (lowest), move End transfusion above the reaction bar or into the scroll area as a plain text-weight action with a confirm step.

### D-29 Minor - Red diamond marker has a stray navy stub on both sides
- Files: detail_overdue_missed__w412_f10.png (30 min node, x about 520, y about 1085 in full-res), detail_stopped_reaction__w412_f10.png, detail_ended_end_vs__w412_f10.png, detail_on_time_late__w412_f10.png. Confirmed at 4x zoom: a navy rectangle sticks out of the diamond's left and right corners.
- Why it hurts: it reads as a rendering glitch in the one marker that should look deliberate.
- Fix: draw the diamond above a rail segment that is cut at the node, or fill the stub with the card colour.

### D-30 Minor - Patient number is smaller than the time on Detail
- Files: detail_upcoming__w412_f10.png.
- What: header MRN about 25 sp, NEXT time about 40 sp, so the page shouts the time and whispers who the patient is (the number sits next to a pale grey-blue droplet, not the VS purple).
- Fix: MRN 32 sp+ and use the VS type colour for the droplet to match the card system.

### D-31 Minor - Stopped-reaction page: documenting button restyled as a secondary and wraps to two lines
- Files: detail_stopped_reaction__w412_f10.png (night: _night.png), detail_stopped_reaction__w360_f20.png.
- What: everywhere else "Measured & documented" is the amber confirm; here it is a white outline button inside the list, centre-aligned in two lines, directly above a second outline button "Remove from Home" (same weight, 12 dp apart).
- Why it hurts: the same action looks different on this one page, and the destructive-sounding "Remove from Home" is the same style as the useful one next to it.
- Fix: use the same amber confirm component (full width, one line "Document 30-min check") and push "Remove from Home" below with 24 dp of space and text-weight styling.


(D-10 re-rated Major: the Start action is the app's second most-used control and its position changes with the font setting; at f20 with a long list it is not on screen at all. Files: home_20beds__w360_f20.png, home_both_features__w360_f20.png. D-12 measured: card vs page 1.13:1 day, 1.14:1 night, no outline, and the cards are tappable.)

## GROUP 4 - REACTION GUIDE (reaction_*)

Measured: nothing truncated at w412 and the numbered steps are easy to read. Red is used only on this screen and on danger chips. Pass on colour use.

### D-32 Major - The symptom list "STOP & CALL IF YOU SEE" is hidden behind the pinned bar on the most common phone size
- Files: reaction_1bed__w360_f10.png (3 of 7 bullets visible, the 4th is a ghost under the fade), reaction_1bed__w412_f10.png (the 7th bullet "New distress or irritability" is faded under the red bar), reaction_20beds__w412_f10.png and reaction_20beds__w360_f20.png (only 3 bullets, or none, visible above the patient chooser). reaction_no_bed__w412_f10.png shows the full list, so the content itself fits when no bar is pinned.
- Why it hurts: this is the safety card. The five DO NOW cards use about 64 dp each with 8 dp of air, and they push the symptom list under the pinned stop button; a nurse reading the list while a child is reacting has to scroll an inner pane that is only 2-3 lines tall.
- Fix: tighten the step cards (bold action + grey qualifier on one line where it fits), or put the symptom list in a two-column grid, so that steps + list fit above a 64 dp pinned button at 360 x 800.

### D-33 Minor - With many running transfusions the patient chooser eats 45 percent of the reaction screen
- Files: reaction_20beds__w412_f10.png (2-column grid shows 4 of 20 numbers), reaction_20beds__w412_f10_night.png, reaction_20beds__w360_f20.png (single column, 1.5 numbers visible; a large blank gap above it in the tall capture).
- Why it hurts: two nested scroll areas (steps above, numbers below) in an emergency; the nurse cannot see the guide and her patient together.
- Fix: collapse the chooser to a single 64 dp "Which patient? 7654801 v" row that opens a sheet, or put the transfusion most recently checked first.

### D-34 Minor - The "7654801 / Transfusion stopped" bar reads as a status, not a button
- Files: reaction_1bed__w412_f10.png, reaction_1bed__w412_f10_night.png (night: bright coral fill, the loudest thing in a dark room).
- What: a flat red slab, same fill as the header and the discs, a 48 sp number and a past-tense caption; no icon, no chevron, no border.
- Why it hurts: it is the only action on the screen (it records the stop), yet it has no button affordance; on a red page nothing says "tap".
- Fix: give it a leading stop-square icon and a white 2 dp border (or invert: white fill with red text), and cap the number at 32 sp so it stops competing with the heading.

## GROUP 5 - TURNING ROUND (round_*)

### D-35 Minor - A 7-patient round cannot be seen on one screen
- Files: round_7beds__w412_f10.png (4 cards visible, "More below" text), round_7beds__w360_f10.png (2.5 cards visible).
- Why it hurts: each patient card is about 84 dp tall, of which the MRN row is 40 dp; at 360 x 800 a nurse cannot see which numbers are due without scrolling the alarm, while Snooze and Start round stay pinned.
- Fix: compact rows (MRN left, "Left 30 -> Back" right, 56 dp) so 7 patients fit in about 400 dp, and turn "More below" into a count chip ("3 more").

### D-36 Minor - A round 65 minutes late is drawn as an amber "DUE +65 min", while Home draws the same round as a red OVERDUE card with a diamond
- Files: round_alarm__w412_f10.png, round_7beds__w412_f10.png vs home_both_features__w412_f10.png (red frame, diamond).
- Why it hurts: the same state has two colours and two shapes; urgency must differ by colour AND shape AND text and be the same everywhere. See also D-18 and D-20.
- Fix: one OVERDUE chip component (red fill, diamond, "OVERDUE +65 min") used on Home, alarm and widget.

### D-37 Minor - Selected state of "Other position" and "Can't turn" is a barely lighter fill
- Files: round_checklist__w412_f10.png (7654809 card: pressed "Other position" is a lighter slate on #283750), round_checklist__w412_f10_night.png.
- Why it hurts: everywhere else in the app a selected option is a solid fill plus a tick; here it is about 1.4:1 and has no tick, so a nurse cannot tell which of the two reasons she picked.
- Fix: reuse the chip-selected style (solid fill + check) here.

### D-38 Minor - Bulk-confirm button label wraps and the warning above it is squeezed
- Files: round_checklist__w412_f10.png, round_checklist__w360_f20.png.
- What: "The other 2 turned as / suggested" breaks after "as"; "2 above still waiting for your answer" is a 16 sp caption sitting 8 dp over the button and 8 dp under "Remind me in 30 min".
- Why it hurts: this one tap documents two patients the nurse is not looking at; the caption that says which two is easy to miss.
- Fix: move the caption into the button as a second line, use a one-line label, and give "Remind me" 24 dp of separation.

## GROUP 6 - SETTINGS AND SETUP CHECK

### D-39 Minor - A pinned navy "Setup check & test alarm" button hides the settings it sits on
- Files: settings__w412_f10.png (the Snooze row is cut by the fade, the pinned button takes 20 percent), settings__w360_f10.png (the "Change Transfusion sound" button is cut in half), settings__w360_f20.png.
- Why it hurts: a navigation link is drawn as the screen's primary action and steals scroll area; the real job of the page (changing settings) happens with 8-10 rows hidden.
- Fix: put it at the top of the list as a plain card ("Check this phone"), unpin it.

### D-40 Minor - Option chips wrap into ragged rows
- Files: settings__w412_f10.png ("By the clock / Follow the phone" in row 1, "Always day / Always night" in row 2, four different widths); "Sound + vibrate" wraps to two lines at 360 (settings__w360_f10.png).
- Why it hurts: a four-way exclusive choice looks like two unrelated pairs and the right edge is ragged.
- Fix: a 2 x 2 grid of equal-width chips, one line each.

### D-41 Minor - Stepper glyphs are tiny
- Files: settings__w412_f10.png (Night starts / Night ends: "-" is an 8 dp dash, "+" 10 dp inside 56 dp buttons).
- Fix: 24 dp glyphs, and show the value at 24 sp bold.

### D-42 Minor - Three equally loud buttons on Setup check
- Files: setup_check__w412_f10.png (two amber "Fix: ..." buttons plus the pinned navy "Test alarm in 1 minute"); at f20 (setup_check__w360_f20.png) the two Fix labels wrap to two lines each.
- What: the "Fix:" button repeats the heading directly above it ("Full-screen alarm allowed").
- Fix: label the buttons "Allow" / "Raise volume", keep amber for Fix only while something is wrong, and make Test the single primary.

### D-43 Minor - Step number disc is aligned to the button, not to its instruction
- Files: setup_check__w412_f10.png (Xiaomi step 1: the disc sits beside "Open phone settings", the instruction text above it has no disc and a different left edge).
- Fix: align the disc with the first line of the instruction and put the button under the text with the same left edge.

## GROUP 7 - START / EDIT SCREENS (start_*, turn_start*, turn_edit, start_edit, turn_detail_*, widget)

Measured at w412 f10: footer primary 64 dp, position buttons 64 dp, MRN field 56 dp, typed MRN about 30 sp, hint line contrast 15:1, disabled label "Enter the MRN first" 6.3:1. The "Change" button next to the footer summary is 33 dp tall (100 px / 3); the same outlined chip is 32 dp next to the MRN on the edit screens.

### D-44 Major - "Change" buttons are 32-33 dp tall
- Files: start_bed_picked__w412_f10.png (bottom-right of the footer), start_empty__w412_f10.png, turn_start__w412_f10.png ("First turn 12:00, then every 2 h - Change"), start_edit__w412_f10.png and turn_edit__w412_f10.png (MRN row "Change", 32 dp).
- Why it hurts: touch targets must be at least 56 dp. This is a gloved, one-handed thumb at the right edge of the screen; the visible shape (an outlined chip) shows that the hit area is meant to be that small. It is also the only way to change the start time or interval, and it sits 12 dp above the 64 dp primary.
- Fix: make it a 56 dp button (or make the whole summary row a 56 dp tappable row with a chevron).

### D-45 Major - At font 2.0 the pinned footer plus header leave no room for the MRN field once the number pad opens
- Files: turn_start__w360_f20.png (footer about 425 dp: three-line summary + three stacked buttons; header about 230 dp), start_empty__w360_f20.png (footer about 225 dp, header about 228 dp, MRN card about 247 dp).
- Why it hurts: the number pad is about 260-300 dp. 800 - 290 - 72 (status and nav bars) = about 440 dp for the app. Header + footer alone are 650+ dp at f20 for Start turning, so the field the nurse has to type into is hidden. At f10 it fits with only about 10 dp to spare (turn_start__w360_f10.png: header 117 dp + card 152 dp + footer 160 dp = 429 dp), so any extra line breaks it.
- Fix: when the pad is open, collapse the footer to the single primary (or the three position buttons) and hide the summary row; cap the header text at one line so "NEW TURNING PLAN" never wraps.

### D-46 Minor - Start transfusion: options cropped mid-row by the footer fade
- Files: start_empty__w412_f10.png ("Time from start" labels visible, chips cut), start_empty__w360_f10.png (duration row shows 1h, 2h, 3h, 4h; "Custom" is not visible), start_bed_picked__w412_f10_night.png.
- Why it hurts: a half row under a gradient looks like a rendering bug and "Custom" cannot be discovered at 360 without scrolling.
- Fix: collapse the "Time from start" preview into a single summary line (it is already repeated in the footer: "Started 11:05 - planned 2 h - ends 13:05"), freeing about 160 dp.

### D-47 Minor - Orphan caption on Start turning
- Files: turn_start__w412_f10.png, turn_start_bed_picked__w412_f10.png ("10:00 is less than 30 min away, so the first turn is / 12:00.").
- What: bare grey text on the page background between two cards, unboxed, wrapping so that the key time "12:00." is alone on a line; it looks like an error message but is neutral.
- Fix: put it inside the footer summary or style it as a small info row with an icon.

### D-48 Minor - Edit positions: unticked rows keep live arrows; checkboxes are 20 dp drawn
- Files: turn_edit__w412_f10.png (row "Left 30 - not for this patient" has two full-strength arrow buttons; the checkbox is a 20 dp square).
- Fix: dim and disable the arrows on unticked rows, and draw the checkbox at 28 dp inside a 56 dp row.

### D-49 Minor - Turn plan timeline: the "Stop 20:00" node sits above the baseline of its neighbours
- File: turn_detail_skipped__w412_f10.png (08:00, 12:00, 16:00 chips share one y; the 20:00 chip and its "Stop" label are about 7 dp higher).
- Fix: reserve the caption row for every node so all chips share a baseline.

### D-50 Minor - "Stop plan" has the same outline style as "Edit positions..." and sits in the lowest slot
- Files: turn_detail_on_track__w412_f10.png, turn_detail_overdue_missed__w412_f10.png (order: amber Open round, then Stop plan lowest, 56 dp).
- Why it hurts: same pattern as D-28: a destructive action in the slot where a thumb rests, drawn like a harmless one.
- Fix: text-weight red label "Stop plan" with a confirm step, placed in the scroll area.

### D-51 Minor - "missed" and "overdue" are the same red pill
- File: turn_detail_overdue_missed__w412_f10.png (and the night file).
- Why it hurts: the two states (gone vs still actionable) differ only by the word; urgency states must differ by colour AND shape AND text.
- Fix: "missed" = outlined red pill with a cross, "overdue" = filled red pill with the Home diamond.

### D-52 Minor - Night: primary buttons and selected chips are tonal (1.5-1.7:1)
- Files: start_bed_picked__w412_f10_night.png ("Baseline done - Start" fill #2A3B5F on #0A1220 = 1.69:1), settings__w412_f10_night.png (selected chip fill 1.48:1 against the card).
- Why it hurts: in a dark room the one primary at the bottom of the form looks like a disabled button; only the light border and a tick separate them.
- Fix: night primary = light fill with dark text (or the amber used on alarms); selected chip = light fill with dark text.

### D-53 Minor - Widget: text under 16 sp and a 4 dp gap above the button
- Files: widget__w412_f10.png (the OVERDUE and "30-min check" lines measure about 14-15 sp; the "30-min check" line ends 4 dp above the amber button; the button label is about 14 sp), widget__w412_f10_night.png.
- What: the widget drops the type stripe and rail, so the type is told only by a 14 dp grey icon; only one row gets a confirm button.
- Fix: 16 sp for sublines and the button label, a 12 dp gap, and a 4 dp type-coloured bar at the row start.

### D-54 Minor - Back control is a bare text link in the top-left corner
- Files: every secondary screen (start_empty__w412_f10.png, settings__w412_f10.png, detail_upcoming__w412_f10.png, reaction_1bed__w412_f10.png).
- Why it hurts: it is the farthest point from a right thumb on a tall phone, with no container or visible 56 dp target.
- Fix: draw it as a 56 dp tonal chip, or add a "Close" row at the bottom of the settings and detail screens.

## WHAT WORKS (so it is kept)
- Bottom stack on Home and alarms is correct: primary 64-96 dp, 8-12 dp gaps, all in the bottom third; contrast of all measured text 6.3:1 to 15:1; key numbers 26 sp on Home and 40-48 sp on alarms.
- Type stripe, rail and time chips are consistent on upcoming cards and the numbers are the largest thing on every card.
- Night theme is genuinely dark (page #0A1220 / #060B14) and keeps every state readable; coral and amber are legible at 7:1.
- The reaction guide at 412 is clear and well ordered (numbered discs, bold action, grey qualifier).
- No clipped or overlapping text at w360 f10 on any screen except the scroll-fade cases listed above; at f20 nothing is truncated (only wrapped).

## SCORE AND COUNTS
- Score: 7.0 / 10. Not a pass (needs >= 9 with no open Critical or Major).
- Critical: 0
- Major: 8 (D-10, D-15, D-16, D-17, D-25, D-32, D-44, D-45)
- Minor: 46
- Total weaknesses: 54

Priority fix order: D-15 (two confirm buttons on the two-patient alarm), D-17 (Undo out of the thumb zone), D-25 and D-44 (touch targets), D-16 (steps cut at 360), D-32 (reaction list hidden), D-45 and D-10 (font 2.0), then the colour and state consistency items D-18, D-20, D-36, D-51.
