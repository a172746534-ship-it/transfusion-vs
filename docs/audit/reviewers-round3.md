# Reviewers, round 3 (screenshots of commit bbace5b, the first with MRNs). Word for word, unedited.


<!-- Design reviewer -->

# Design review 5 - transfusion / repositioning alarm companion (screenshots only)

Method: every screen opened at w412_f10 and night; w360_f20 / w360_f10 checked for overflow. Contrast figures were measured from pixels (e.g. SOON border #D28000 on white = 3.08:1; body grey #4C5870 on white = 7.15:1; navy #13233F on amber #F5A623 = 7.73:1; navy on setup amber #D28000 = 5.10:1). Sizes are estimated from the 3x screenshots (cap-height / 0.7).

Severity: Critical = can cause a missed/wrong clinical action; Major = breaks a fixed threshold or the brief; Minor = polish / consistency.

## GROUP 1 - HOME (home_*, undo_bar_on_home, widget)

### H1 - MAJOR - Permanent red "SUSPECTED REACTION?" banner looks exactly like an overdue card
Screen: Home (every state with a transfusion). Files: home_1bed_upcoming__w412_f10.png, home_both_features__w412_f10.png, home_ringing__w412_f10.png, *_night.
Problem: a full-width 68 dp box with the same 2 dp dark-red outline and red text as the OVERDUE cards sits on screen when nothing is wrong (home_1bed_upcoming: one patient "SOON 2 min" and a red alarm-looking box). In home_both_features two real overdue cards and the standing guide share one visual language, so the red that should mean "act now" is also furniture. It also sits 8 dp above the two Start buttons in the thumb zone and reads as a button. Violates "quiet when all is fine" and "red means danger only".
Fix: make the guide a neutral entry (navy outline, white fill, red icon only, or a one-line link row "Reaction guide"), keep red borders/fills exclusively for state cards; leave a 16 dp gap before the Start row.

### H2 - MAJOR - Two identical amber slabs compete when an alarm is ringing
Screen: Home, alarm ringing. File: home_ringing__w412_f10.png (and _night).
Problem: "Alarm ringing. Open it >" (bell, chevron, bold) and "7654814 · Measured & documented ✓" (no icon, regular weight) are two full-width amber slabs 12 dp apart, same colour, same height. A nurse with 1-2 s cannot tell which is THE action; one navigates, the other documents. Violates "one primary action per screen". Under them sit the red guide and two more big buttons: four stacked full-width blocks occupy 32% of the screen. undo_bar_on_home__w412_f10.png adds a fifth slab (Undo bar) above the amber one.
Fix: one amber primary only (Measured & documented). Move "alarm ringing" onto the DUE NOW card itself (thick border + bell) or make it an outlined, non-filled strip. Show the undo bar as a snackbar that overlays, not as a stacked slab.

### H3 - MAJOR - Patient number inside the one-tap action button is ~18 sp and regular weight
Screen: Home. Files: home_both_features__w412_f10.png ("7654807 · Open round"), home_overdue_missed__w412_f10.png, home_ringing__w412_f10.png ("7654814 · Measured & documented ✓"), home_end_vs_pending__w412_f10.png, undo_bar_on_home__w412_f10.png.
Problem: the button that documents/opens a specific patient names that patient in the smallest, lightest text on the screen (cards show the same MRN at ~26 sp bold). Threshold: key numbers >= 24 sp on Home. With 5-20 patients and gloves, tapping the wrong patient's "documented" is a real risk. The label reads like a status line (check mark at the end), not a verb button; the weight differs from the bold Start buttons beneath it.
Fix: two-line button - MRN 28 sp bold on top, verb label 18 sp bold below; keep the check mark out of the label.

### H4 - MAJOR - Header: two near-identical times side by side, and the header changes height between states
Screens: Home. Files: home_1bed_upcoming__w412_f10.png ("All on time · next 11:07" butts against clock "11:05", ~8 dp gap, same weight), home_20beds__w412_f10.png ("next 11:06" / "11:05"), home_only_turning__w412_f10.png ("All on time · next 12:00" wraps to two lines, header grows 148 -> 205 px, clock drops 27 px), home_stopped_reaction__w412_f10.png and home_end_vs_pending__w412_f10.png ("NONE RUNNING · STILL OPEN" wraps, orphan "OPEN", header 185 px).
Problem: "next 11:06 11:05" reads as one string; at a glance the nurse reads the wrong time. The header height and the clock position jump from screen to screen, so the eye has no fixed anchor. On a ward the clock is the one thing read from a distance.
Fix: fixed-height header; clock on its own fixed right column; drop "next HH:MM" (the first card already shows it) or move it to a second row; shorten the eyebrow so it never wraps.

### H5 - MINOR - DUE NOW (alarm ringing) is the quietest card on the list
File: home_ringing__w412_f10.png / _night.
Problem: the card of the ringing patient is a plain white card with a small navy pill; OVERDUE gets thick red border + red pill + diamond. The more urgent moment (alarm sounding) has less shape change than "overdue". Only the bottom banner signals it.
Fix: give DUE NOW a 3 dp navy/amber border and bell glyph; keep red for overdue.

### H6 - MINOR - Amber is overloaded
Files: home_empty__w412_f10.png, home_both_features__w412_f10.png, home_setup_warning__w412_f10.png.
Problem: amber = header eyebrow in every state (even "NOTHING RUNNING" / "All quiet"), SOON badge, primary button fill and the setup warning (a second, darker amber #D28000). A calm empty Home starts with an amber warning-coloured line. The overdue patient's primary button is amber, not red.
Fix: eyebrow in neutral light blue-grey; amber only for SOON and the CTA; one amber value.

### H7 - MINOR - Setup warning banner is under-designed for the most safety-relevant message
File: home_setup_warning__w412_f10.png / _night.
Problem: "Setup: 2 to fix. Alarms may not ring" wraps with an orphan word ("ring"), no action word ("Fix"), only a chevron; it is in the top third, out of the one-handed zone; same amber family as the CTA. Night version swaps to a dark olive fill with yellow outline - a different component look from day.
Fix: icon + short line + a visible "Fix" pill on the right; same component in both themes.

### H8 - MINOR - Time glyphs use a slashed-zero monospace face, MRN uses a plain-zero sans
Files: home_both_features__w412_f10.png ("10:00", "12:00", "11:05" vs "7654800123").
Problem: slashed zeros inside times read as "Ø"; two numeric faces on one card. Key numbers should look like one family.
Fix: one tabular-figure face with plain zero for times and MRN.

### H9 - MINOR - Progress track on each card is a 5-node strip with 8-9 dp glyphs
Files: home_both_features__w412_f10.png, home_20beds__w412_f10.png.
Problem: tiny nodes (hollow circle, half-filled amber circle, red diamond with "!" about 9 dp) cannot be decoded in 1-2 s, duplicate what the pill already says and add 20 identical strips in the 20-patient list.
Fix: drop the strip or enlarge the single "you are here" marker to >= 16 dp and remove the rest.

### H10 - MINOR - 20 identical SOON cards: the list is uniformly amber and has no landmark
File: home_20beds__w412_f10.png / _night.
Problem: 8 visible cards all "VS / 15-min check / SOON" in amber; only a fade at the bottom hints at more; no scroll indicator or "+12 more". "Quiet when fine" is not met: the whole screen is amber outline noise.
Fix: show the SOON pill only for the next 2 cards or when < 2 min; others plain "in N min"; add a "12 more" footer chip.

### H11 - MINOR - End-VS pending card and its button wrap to 3 and 2 lines
File: home_end_vs_pending__w412_f10.png.
Problem: secondary text "Measure, then document in Cerner · 30-min check overdue" wraps to three grey lines; the amber button wraps with orphan "documented ✓". Card height 1.8x the others, glance time grows.
Fix: clamp secondary text to one line (long phrase belongs on the detail screen), allow the button two lines only with the MRN on its own line (see H3).

### H12 - MINOR - Night theme: full-width ochre slab is the brightest object in a dark room
Files: home_both_features__w412_f10_night.png, home_overdue_missed__w412_f10_night.png.
Problem: the amber CTA (#C89030) is a 400 x 68 dp bright block next to sleeping children; the red banner outline and the yellow eyebrow add to it. It outshines the overdue card, which is the thing that matters.
Fix: night CTA = deep amber (#7A5A14) with light text >= 7:1, or an outlined amber button.

### H13 - MINOR - Empty state is a bold sentence card with 75% blank screen
File: home_empty__w412_f10.png.
Problem: the instruction card is the same white rounded card and bold weight as alerts, sits at the very top far from the thumb; nothing explains Start turning.
Fix: move the hint just above the Start buttons in regular weight; add a one-line hint for turning.

### H14 - MINOR - Undo button is ~55 dp tall and has no visible time-out
File: undo_bar_on_home__w412_f10.png.
Problem: "Undo" is a dark block inset 4 dp inside the bar, about 91 x 55 dp (< 56 dp minimum), and nothing shows how long Undo lives. An action that disappears silently is hard to trust with gloves.
Fix: make the Undo button the full bar height (>= 64 dp) and add a thin countdown line along the bar's bottom edge.

### H15 - CRITICAL - Widget shows full MRNs and an unnamed "Measured & documented" button
File: widget__w412_f10.png (and _night, w360 variants).
Problem: (a) PRIVACY: the maintainer rule is "outside the app the number is masked", but the home-screen widget (visible to bystanders without unlocking the app) prints 7654807, 7654812, 7654800123 in full, bold. (b) With two overdue rows above it, the amber "Measured & documented ✓" button has no MRN: it is impossible to tell which patient a tap documents, and it sits 5 dp under the second row's text so it looks like part of that row. (c) Rows have almost no separation (MRN / OVERDUE / detail stack of 3 lines followed 5 dp later by the next MRN).
Fix: mask the MRN in the widget (last 3-4 digits) per the stated rule; put the target patient on the button (masked) or make the button per row; 12 dp gaps between rows and a hairline divider.

(H1 note: the same problem repeats on the alarm screens - alarm_1bed__w412_f10.png, alarm_2beds__w412_f10.png, alarm_snoozed__w412_f10.png - where a solid red 64 dp slab "SUSPECTED REACTION?" is the largest saturated block on a routine 15-min alarm, bigger in visual mass than the "DUE NOW" state and equal to the amber primary. Red is spent before anything has gone wrong.)

## GROUP 2 - ALARM screens, ROUND screens, REACTION screens

Measured (alarm_1bed__w412_f10.png): reaction slab 64 dp, Snooze 64 dp, primary 96 dp, gaps 12 dp, bottom margin 16 dp. All meet the 64 dp / 8 dp thresholds. MRN and time ~50 sp (>= 40 sp). Primary is in the bottom third. These are real strengths.

### A1 - MAJOR - "check done" confirmation shows the MRN at ~24 sp inside a sentence
Screen: alarm_done_undo. Files: alarm_done_undo__w412_f10.png (+ _night).
Problem: the confirmation exists to let the nurse verify the right child was documented, but "7654814 · 15-min check done" is one line of ~24 sp bold text, same size and weight as the words. Alarm-screen threshold for key numbers is >= 40 sp. The huge amber check mark (60 dp) outweighs the number; the screen is 70% empty navy.
Fix: MRN on its own line at >= 48 sp, "15-min check done" below at 24 sp, check mark reduced to 40 dp; add a thin countdown bar on the Undo button.

### A2 - MAJOR - "Start round" strip on the transfusion alarm is a muted, label-like slab with ~20 sp MRNs
Screen: alarm when a transfusion check and a round are both due. File: alarm_transfusion_and_round__w412_f10.png (+ _night).
Problem: "Start round · 10:00 · 7654803, 7654807" is a slate (#26375A) bar on navy (~1.4:1 edge), regular weight, no chevron: it reads as a status line, not a tappable action, yet it is the only route to the round. The MRNs are ~20 sp (threshold 40 sp) and the line already runs to 8 dp of the right edge with two patients, so three or four MRNs will truncate. It adds a fourth slab (round, reaction, snooze, primary) above the primary and sits between the instruction card and the red slab.
Fix: show as a clear secondary button (outline 2 dp, chevron, "Round due · 2 patients"), put MRNs on the round screen only, and keep one line only.

### A3 - MINOR - Progress strip on the day alarm is below 3:1
File: alarm_1bed__w412_f10.png vs alarm_1bed__w412_f10_night.png.
Measured: inactive line #3B4761 on #13233F = 1.69:1, inactive nodes #4C5870 = 2.19:1 (day); night line #767F91 = 4.9:1. Day is weaker than night, and below the 3:1 UI minimum. The "where am I in the schedule" cue disappears in bright light.
Fix: day inactive line/nodes >= #7F8CA6.

### A4 - MINOR - Snoozed and snooze-used-up states differ from the live alarm only by a small pill / one text line
Files: alarm_snoozed__w412_f10.png, alarm_snooze_used_up__w412_f10.png.
Problem: everything big (15-min check, MRN, 11:07, amber ring, red slab, amber primary) is identical to the ringing alarm; only the 16 sp pill (outlined instead of filled) and a floating one-line status (no container, no icon in the used-up case) change. A snoozed alarm should look quieter; "No snoozes left" should look stronger (it is the warning that the next ring cannot be postponed).
Fix: dim the amber ring to outline when snoozed; give "No snoozes left" an amber outlined chip with an icon.

### A5 - MINOR - Alarm layout shifts between 1 and 2 patients
Files: alarm_1bed__w412_f10.png (eyebrow at y=58) vs alarm_2beds__w412_f10.png (eyebrow at y=271, 98 dp of dead space on top).
Problem: the same eyebrow, pill and heading jump 100 dp; the muscle memory of "state pill top-left" breaks the moment two children are due. Per-patient amber buttons (2 identical slabs) are the only differentiator, and the first one sits in the middle third.
Fix: top-align both layouts; make the cards scroll instead of pushing the header down.

### A6 - MINOR - Round alarm has no state pill and misaligned header numbers
Files: round_alarm__w412_f10.png, round_7beds__w412_f10.png (+ _night).
Problem: the transfusion alarm says DUE NOW in an amber pill; the round alarm has none (urgency has colour only through the eyebrow), although 10:00 vs clock 11:05 means it is late. "4 patients" is baseline-misaligned with 10:00 (about 7 dp) and right-aligned at a different size than any other number. The instruction line "Check pressure points: occiput · ears · sacrum · heels" (~18 sp regular, grey) runs to 15 dp of the right edge at 412 and has no room to grow. The "From -> to" body pictograms differ by a 2-3 dp limb stroke (Left 30 vs Right 30), unreadable in 1-2 s; the text next to them has to carry it.
Fix: same pill component (DUE NOW / OVERDUE +n min); align on one baseline; enlarge pictograms to 40 dp with a filled limb.

### A7 - MINOR - "More below" cue on the round list is a plain white label jammed against Snooze
File: round_7beds__w412_f10.png.
Problem: "More below ↓" sits 4 dp above the Snooze button, no count (3 more), no scrollbar. With gloves a nurse may think the list ends at 4 patients and tap Start round with 3 unseen.
Fix: "+3 more" pill anchored right with 12 dp clearance, show a scrollbar.

### A8 - MINOR - Round checklist: per-patient actions have no hierarchy; chips are a second button style
Files: round_checklist__w412_f10.png, round_checklist__w412_f10_night.png.
Problem: "Turned to Left 30°" (the common action) is regular-weight outlined, visually equal to - and 9 dp from - "Can't turn", and lighter than "Other position" / "Can't turn" (bold). The stray white "Back" chip in its own row is a white-filled button style that appears nowhere else (night: periwinkle), and it is the brightest object on screen. 9 dp gaps between 64 dp buttons are at the minimum for gloved thumbs. Primary label wraps to two lines with an orphan ("suggested ✓").
Fix: make "Turned to ..." a filled/bold button, keep Can't turn as plain outline with 16 dp gap; one chip style (outlined) for all options; shorten bottom label to one line.

### R1 - MAJOR - Reaction picker for many patients: wall of 20 red MRN buttons beside a clipped guide
Files: reaction_20beds__w412_f10.png, reaction_20beds__w412_f10_night.png.
Problem: the guide's "STOP & CALL IF YOU SEE" list is cut at the 4th bullet by a fade (nested scroll) so red-flag symptoms are hidden at the worst moment, and beneath it "TRANSFUSION STOPPED FOR:" shows 20 identical solid-red buttons, 3 columns, each ~24 sp regular digits differing only in the last 1-2 digits. In a crisis this invites tapping the wrong child; 20 solid reds also means red no longer singles out anything.
Fix: pre-select the patient from the card or the due alarm; otherwise list only running transfusions, one column, outlined buttons (not solid red), bold last 3 digits; let the guide scroll as one page and pin the picker.

### R2 - MAJOR - Reaction CTA names the child in regular-weight ~24 sp digits
Files: reaction_1bed__w412_f10.png ("Transfusion stopped — 7654801"), reaction_1bed__w412_f10_night.png.
Problem: this button records a transfusion stop against one patient; the MRN is the same weight and size as the label (alarm-level threshold 40 sp). 
Fix: MRN on its own line at 40 sp bold above the verb label inside the button.

### R3 - MINOR - Reaction screen "Back" is top-left, farthest from the thumb
Files: reaction_1bed__w412_f10.png, reaction_no_bed__w412_f10.png.
Problem: the guide is entered from a bottom-of-screen banner (H1) that is easy to hit by accident, but the way out is the top-left corner. reaction_no_bed has no bottom action at all and 10% dead space.
Fix: a bottom "Close" outline button (64 dp) in reaction_no_bed; keep Back top-left for the other states.

### H16 - MINOR - Five hues compete with the urgency colours on Home
Files: home_both_features__w412_f10.png, home_both_features__w360_f10.png.
Problem: VS = purple bar + purple "VS", TURN = teal bar + teal "TURN", "you are here" node = amber half-circle on VS cards but a blue open circle on TURN cards (both mean "next"), plus red, amber and navy. Type is already carried by the droplet / rotate icon and a word, so the two extra hues only add noise and make red/amber harder to pick out.
Fix: one neutral colour for type (navy icon + word), one marker colour for "next".

## GROUP 3 - DETAIL, TURN DETAIL, START, SETTINGS, SETUP CHECK, 360 / f20, NIGHT

### D1 - MAJOR - The strongest button on a quiet detail screen is the one that ends monitoring
Screens: detail_upcoming__w412_f10.png ("End transfusion" solid navy, 64 dp, bottom), turn_detail_on_track__w412_f10.png and turn_detail_skipped__w412_f10.png ("Stop plan" solid navy, 64 dp, bottom), turn_detail_stopped ("Restart plan" same style).
Problem: when nothing needs doing the single filled primary button in the thumb zone cancels all alarms for that patient. One mis-tap with a gloved thumb while scrolling silences the child's checks; the same navy fill is used for harmless "Start transfusion" / "Save changes", so nothing marks it as terminal. On the ringing/overdue variants the roles flip (amber action on top, End outlined below with 8-16 dp gap), so the same button changes style from screen to screen.
Fix: terminal actions get one consistent quiet style (outlined, red text or an icon) and are never the filled primary; on a quiet screen there is no filled primary at all, or the primary is Back/"Done". Keep >= 16 dp between "Open round" and "Stop plan" (turn_detail_overdue_missed shows 8 dp).

### D2 - MINOR - "SUSPECTED REACTION? STOP the transfusion & call" banner on an ended transfusion
File: detail_ended_end_vs__w412_f10.png.
Problem: the transfusion has already ended (header "ENDED", "Transfusion ended at 10:58") yet an 80 dp outlined red banner tells the nurse to stop it. Red is spent on something no longer applicable, and it sits directly above the amber "End VS" primary. Same banner is permanent on every running detail (see H1).
Fix: hide it once ended, or show the reaction guide as a quiet link.

### D3 - MINOR - Detail checkpoint card is cut mid-line by the fade with no scroll cue
Files: detail_ringing__w412_f10.png ("Time-limit warning / 14:37" half-faded), detail_overdue_missed__w412_f10.png, detail_on_time_late__w412_f10.png ("Transfusion due to finish -" cut), detail_upcoming__w412_f10.png ("Edit start time or duration" button faded under the red banner).
Problem: content is clipped by an inner scroll area; the visible text stops mid-sentence, and a faded button looks broken. "Zero truncation" is not met at first glance.
Fix: let the whole page scroll with the actions pinned, or end the inner list on a whole row with a "more" chip.

### D4 - MINOR - Time chips on the timelines look like buttons
Files: detail_upcoming__w412_f10.png, turn_detail_on_track__w412_f10.png.
Problem: "10:52 / 11:07 / 11:22" are 2 dp outlined or filled rounded rectangles with bold type - the same look as the tappable chips on the Start screen - but nothing happens when tapped. Filled vs outlined changes with past vs future with no legend. The strip repeats exactly what the Checkpoints card below it says.
Fix: plain text under the dots, delete one of the two lists.

### D5 - MINOR - Two buttons sit just under 56 dp and the same action is styled two ways
File: detail_stopped_reaction__w412_f10.png.
Measured: in-card "30-min check . Measured & documented" outlined button = 55 dp and wraps to two lines; "Remove from Home" = 55.7 dp. Elsewhere the same action is the amber primary (detail_overdue_missed).
Fix: >= 64 dp and the same amber style whenever it documents an overdue check.

### T1 - MINOR - Arrow dangling after the old position
Files: turn_detail_on_track__w412_f10.png, turn_detail_stopped__w412_f10.png.
Problem: in the TURNS list "Right 30deg ->" sits on one line and the new position "Left 30deg" on the line below, so the arrow points at empty space and the two pictograms read as a list of two positions, not "from -> to". Regular-weight old, bold new, same size.
Fix: single row "[icon] Right 30 -> [icon] Left 30", or only the new position.

### T2 - MINOR - "Stop" node sits higher than its siblings
File: turn_detail_skipped__w412_f10.png.
Problem: the "Stop 20:00" column has its label above the circle, so the circle and chip sit 7 dp higher than 08:00 / 12:00 / 16:00; the row has no common baseline.
Fix: reserve the label row for every column.

### T3 - MINOR - Position pictograms differ by 2-3 dp of limb stroke
Files: turn_start__w412_f10.png, turn_start_options__w412_f10.png, round_checklist__w412_f10.png.
Problem: Left 30 and Right 30 are near mirror images at 24 dp, side by side in 8 dp-gapped buttons; at 1-2 s they cannot be told apart, so only the 18 sp label decides. Disabled position buttons (turn_start) look like ordinary outlined buttons.
Fix: 40 dp icons with a big filled limb/arrow, label bold 20 sp, a visibly greyed disabled state.

### S1 - MAJOR - New-patient path ("+ New MRN") is the last item of a 20-row inner scroller
Files: start_empty__w412_f10.png (list shows 16 recents and fades out), start_empty__w360_f20_p3.png (the "+ New MRN" button appears after 20 MRNs).
Problem: the brief says a new patient is typed once; that is the first-time case for any new admission, but its only entry is below every recent patient inside a nested scroll box with a fade and no scroll bar. A nurse sees 16 MRNs, no "New" and a disabled "Pick a patient first".
Fix: pin "+ New MRN" as the first tile (or a sticky row above the list), show the 6 most recent only, with "More" behind it.

### S2 - MAJOR - Night theme: the MRN input is nearly invisible
File: start_first_patient__w412_f10_night.png.
Measured: field border and floating label "Patient's MRN" #2A3B5F on #141F33 = 1.48:1 (needs 3:1 / 4.5:1). In a dark room with a sleeping child the single field the screen asks for cannot be found, and the cursor is a thin 1 dp line. Day version has a dark 2 dp border and is fine.
Fix: night field border >= #8FA0BF (3:1+), label >= 4.5:1, 3 dp focus ring in amber.

### S3 - MINOR - Start screens: the preview card is clipped to its column labels
Files: start_first_patient__w412_f10.png, start_bed_picked__w412_f10.png.
Problem: "TIME FROM START" is cut after "0 15 min 30 min 1 h End" with no times or dots, then the fade; it looks like a rendering error. The summary line "Started 11:05 . planned 2 h . ends 13:05" (bold ~17 sp) is separate from its "Change" link, an underlined bare text link whose touch area looks well under 56 dp; at f20 it wraps to three lines with Change jammed beside the middle one (start_empty__w360_f20_p4.png).
Fix: drop the preview from the form (it exists on the detail screen), make "Change" a 56 dp outlined button.

### S4 - MINOR - Start turning: instruction line is as heavy as a heading
File: turn_start_options__w412_f10.png ("7654806: lying now? Tap to start").
Problem: bold ~20 sp line, 12 dp above the three 8 dp-gapped buttons; the MRN inside it is the same weight as the words. After picking one the other two stay as full-strength navy, so the choice just made is invisible.
Fix: MRN 28 sp on its own line; the chosen position gets a check, the others lighten.

### ST1 - MINOR - Setup check: layout glitches
File: setup_check__w412_f10.png (+ night, + f20).
Problem: (a) heading "XIAOMI / REDMI / POCO - EXTRA STEPS" wraps with orphan "STEPS". (b) The step badge "1" is centred on the button, while its instruction "Autostart -> turn ON for A S." sits above, so badge and text are disconnected. (c) The "Never use Force stop..." warning is bold body text cut mid-line by the fade. (d) two identical amber "Fix:" buttons stacked 28 dp apart repeat the line above them word for word.
Fix: align badge to the first text line, shorten heading, put warning in a card, one "Fix" button per row.

### ST2 - MINOR - Settings: first card is "Recent patients", the selected chip is a fill that disappears at night
Files: settings__w412_f10.png, settings__w412_f10_night.png.
Problem: a 3-line grey sentence ("Nobody yet. A patient's MRN is added when...") is the first thing on an alarm settings screen; the bottom primary "Setup check & test alarm" is not related to the fields above it. At night the selected chip (#2A3B5F fill on #141F33, ~1.4:1) differs from unselected only by a check mark and slightly brighter border.
Fix: put Setup check first, recents last; at night use a bright 3 dp border or amber fill for the selected state.

### W1 - MAJOR - 360 dp: with two patients due, the second patient's documented button is cut off behind the red slab
File: alarm_2beds__w360_f10.png (and w360_f20).
Problem: the instruction card, two patient cards, red slab and Snooze are stacked in a nested scroll; the second card's amber button is half-faded behind the pinned red "SUSPECTED REACTION?" slab. The main action for patient 2 needs an inner scroll with a ringing alarm, and is not in the bottom third while a non-action (red slab) is.
Fix: scale the cards down (one line per patient: MRN + amber button) so both fit; make the red slab a small link on alarms.

### W2 - MINOR - 360 dp: patient number and time run together on the alarm
Files: alarm_1bed__w360_f10.png, alarm_transfusion_and_round__w360_f10.png.
Problem: "7654814 11:07" is one 50 sp line with a ~14 dp gap and the same weight, so it reads as one long number; at 10-digit MRNs (7654800123) it will wrap unpredictably. At 412 and f20 they sit on separate lines.
Fix: always stack them (MRN above, time below) or add a vertical rule.

### W3 - MINOR - Font scale 2.0: headers eat the first screen
Files: home_both_features__w360_f20.png, detail_overdue_missed__w360_f20.png, home_empty__w360_f20.png.
Problem: "5 / PATIENTS / RUNNING" wraps to three lines (orphan "5") with the clock and gear wedged beside it - header 145 dp; the detail header is 290 dp (36% of the screen) before the first card; "30-min / check" breaks beside "10:50". No truncation or overflow was found at f20 anywhere, which is good, but the first card on Home sits under 1/5 of the screen.
Fix: cap eyebrow at one line with ellipsis-free shortening ("5 running"), let the header collapse on scroll.

### W4 - MINOR - Round checklist at 360: stray white "Back" chip, selected state looks disabled
Files: round_checklist__w360_f10.png, round_checklist__w412_f10_night.png.
Problem: after "Other position" opens, the chosen chip becomes a grey slate fill that reads as disabled; "Back" appears alone on its own row as a white filled block (the brightest object on the screen, ~53 dp tall); wrapped "Other / position" breaks one chip into two lines while its neighbour is one line.
Fix: one chip style, 64 dp tall, selected = amber outline + check.

### N1 - MINOR - Night: OVERDUE and SOON are both outlined pills
Files: undo_bar_on_home__w412_f10_night.png, home_overdue_missed__w412_f10_night.png, detail_overdue_missed__w412_f10_night.png.
Problem: in day OVERDUE is a filled red pill and SOON an outlined amber pill (shape differs). At night both are outlined, differing in colour and text only; the 3 dp coral card border is the remaining shape cue. The brief demands colour AND shape AND text.
Fix: keep OVERDUE filled at night (dark red fill, white text).

### N2 - MINOR - Night undo bar has a light-white 2 dp border like a card selected state
File: undo_bar_on_home__w412_f10_night.png.
Problem: the Undo bar is the brightest-outlined element (white 2 dp border), brighter than the overdue card; its Undo button inset has a second border (double outline).
Fix: single dim outline, Undo as plain text button 64 dp.

## STRENGTHS (so the next pass does not break them)
Alarm screens: 64 dp snooze / 96 dp primary, key numbers ~50 sp, body >= 16 sp, 12 dp gaps; contrast measured >= 7.6:1 for MRN/time/primary in day and >= 9:1 in night. Overdue cards use colour + diamond + text + thicker border. Reaction screen and picker buttons are 64 dp+. No overflow at w360_f20 on any of the 46 screens.

## SCORE
Critical 1, Major 12, Minor 34 (one finding per ID above; 47 in total). Score 5/10 - the visual system is coherent and the alarm screens are strong, but several Majors (permanent red slab, ambiguous double amber, small MRN in action buttons, header jump, widget privacy, terminal button styled as primary, buried "+ New MRN", invisible night field, clipped actions at 360) are open. Pass criteria (>= 9/10 and no open Critical or Major) not met.


<!-- Persona 1: day shift -->

# review5 persona 1: day-shift pediatric oncology nurse, 6 patients, one hand free

Screens used: w412_f10 mainly. Cross-checked w360_f10, w360_f20 (the full-page w360_f20 capture of round_checklist shows the whole list) and the night variants. No source code opened.

## Walkthrough table

Touch 1 = the first tap. Where a task starts from the launcher, the app icon counts as touch 1.

| # | Task | Screens (in order) | Touches | Budget | Result |
|---|------|--------------------|---------|--------|--------|
| 1 | See all my patients, what's next, what's late | App icon, then `home_6beds`. All 6 cards fit on one screen, sorted by time. Header reads "1 overdue · 2 due now". The overdue card has a red border and a red pill. Each card has the purple drop + VS or teal TURN marker. (The widget shows the same list with 0 touches.) | 1 (icon) | 1 | OK. See finding 5 about the "DUE +10 min" card. |
| 2 | Acknowledge a transfusion check from the alarm | `alarm_1bed`, tap the big orange "Measured & documented ✓". Then `alarm_done_undo` ("7654814 · 15-min check done", Undo). With two patients due (`alarm_2beds`) each has its own named button, 1 touch per patient. From Home (`home_ringing` / `home_6beds`) the orange bar "7654814 · Measured & documented ✓" is also 1 touch. | 1 | 1 | OK |
| 3 | Complete a whole turning round, all as suggested | `round_alarm` (4 patients, from → to with icons), tap "Start round" (1). `round_checklist`, tap the orange footer (2). I only saw the footer in its mixed state ("The other 2 turned as suggested ✓"). I assume it reads "All 4 turned as suggested ✓" when nobody is an exception. If the round arrives with a transfusion alarm (`alarm_transfusion_and_round`), the "Start round · 10:00 · 7654803, 7654807" row is touch 1. From Home (`home_both_features`), the bar "7654807 · Open round" is touch 1. | 2 | 2 | OK (default footer wording not seen) |
| 4 | Start a transfusion | Home, "Start transfusion" (1). `start_empty`, tap my MRN (2). `start_bed_picked`, "Baseline done · Start" (3). Defaults are Now and 2 h, with the summary "Started 11:05 · planned 2 h · ends 13:05" next to the button. Result is `undo_bar_on_home` ("7654805 · transfusion started" + Undo). | 3 | 4 | OK. Scrolling is extra if my MRN is far down the list. |
| 5 | Start a repositioning plan | Home, "Start turning" (1). `turn_start`, tap MRN (2). `turn_start_bed_picked`, tap the lying position "Left 30° / Back / Right 30°" under "7654806: lying now? Tap to start" (3). Defaults: first turn 12:00, every 2 h. | 3 | 4 | OK |
| 6 | End a transfusion | Home, tap the patient's card (1). `detail_upcoming` or `detail_ringing`, "End transfusion" (2). `detail_ended_end_vs`, "End VS · Measured & documented ✓" (3). The ending itself is 2 touches. The end-VS documentation is the 3rd. | 3 | 3 | OK. See finding 8. |
| 7 | Suspected reaction, see the steps | Home, red "SUSPECTED REACTION? STOP the transfusion & call" card (1). `reaction_*` shows DO NOW steps 1-5 and the STOP & CALL list. The same red bar is on the transfusion alarm and detail screens. | 1 | 2 | OK |
| 8 | Mark "Transfusion stopped" for a specific patient | Home, red reaction card (1). Reaction screen, "TRANSFUSION STOPPED FOR:" grid, tap the MRN button (2). Single-patient variant is "Transfusion stopped — 7654801". Result is `home_stopped_reaction` / `detail_stopped_reaction` ("STOPPED — REACTION"). With 6 running patients it is 2 rows of 3. With 20 running patients (`reaction_20beds`) only the first 2 rows show, so one extra scroll. | 2 | 3 | OK |
| 9 | Mark one patient in a round "can't turn" with a reason | `round_alarm`, "Start round" (1). `round_checklist`, "Can't turn" on that patient (2). Reason chip: Off unit / Procedure / Asleep/comfort / Unstable / Other (3). The patient is now marked. The round is still open and needs "The other 3 turned as suggested ✓" to close. | 3 to mark, 4 to close the round | 3 | OK for marking. Over budget by 1 if closing the round counts. See finding 4. |

## Concrete findings

1. **Round checklist: unfinished exceptions look finished** (`round_checklist` w412_f10 and the full-page w360_f20 capture).
   - Element: the 7654807 and 7654809 cards, plus the orange footer.
   - Expected: if I tap "Can't turn" or "Other position" and have not yet chosen a reason or position, the card shows it is incomplete, or the footer says so.
   - Actual: 7654807 shows "✓ Can't turn" with five reason chips and none selected. 7654809 shows "✓ Other position" with a lone "Back" chip and none selected. Both ✓ marks look done. The footer "The other 2 turned as suggested ✓" stays bright orange. Nothing says 807 and 809 are still unresolved. A nurse interrupted mid-round, one-handed, can tap the footer and believe the round is closed.

2. **Round checklist: the position and reason pickers have no caption, and the position chip is called "Back"** (`round_checklist` w412_f10, 7654809 card, and the 7654807 reason chips in the w360_f20 capture).
   - Element: the white chip "Back" under "✓ Other position". The reason chips appear with no "Why?" line.
   - Expected: a short prompt such as "Turned to:", or a position icon on the chip.
   - Actual: a lone white text chip "Back" sits bottom-left of the card. Every other screen uses "‹ Back" as the navigation label in the top bar. Here it means "lying on the back". It can be read as "cancel and go back". The reason chips likewise have no heading.

3. **Widget: the orange "Measured & documented ✓" button does not say which patient** (`widget` w412_f10).
   - Element: the full-width orange button between the two overdue rows (7654807 TURN overdue, 7654812 transfusion overdue) and the upcoming rows.
   - Expected: the label names the patient it acts on, as Home does ("7654812 · Measured & documented ✓"). It should not appear to cover a turning row.
   - Actual: the button has no MRN and sits directly under the 7654812 row. The 7654807 turn row is the line above it. "Measured" makes no sense for a turn. On a quick glance it reads as covering both overdue lines.

4. **Task 9 takes 4 touches to close the round** (`round_checklist`).
   - Element: after the reason chip is tapped, the round still needs the footer "The other N turned as suggested ✓".
   - Expected: with the budget of 3 from the alarm, the round is done after marking one patient.
   - Actual: Start round, Can't turn, reason, footer = 4 touches to leave the round finished. Marking alone is 3. This is a finding only if the budget means "round finished".

5. **Home: "DUE +10 min" is counted as "due now", not overdue** (`home_6beds`, w412_f10 and w360_f10).
   - Element: the 7654803 card ("10:55 · 15-min check · DUE +10 min") and the header "1 overdue · 2 due now".
   - Expected: a 15-min check that is already 10 min late shows as late, red like the neighbouring "OVERDUE +15 min", or the label explains the difference.
   - Actual: it gets the same dark navy pill as "DUE NOW", with no red border, and is counted in "2 due now". Only 7654812 is flagged red. The cut-off between "DUE +10" and "OVERDUE +15" is not explained anywhere on screen. This may be an intended grace window, but it is not visible to the user.

6. **Detail of an ended transfusion: the overdue check has no action and no explanation** (`detail_ended_end_vs`).
   - Element: the red "overdue" pill on "10:50 · 30-min check", and the only button "End VS · Measured & documented ✓".
   - Expected: either a way to document that check, as `detail_stopped_reaction` offers ("30-min check · Measured & documented ✓"), or text saying the End VS measurement covers it.
   - Actual: a red "overdue" pill with nothing to tap. Home also still reads "· 30-min check overdue" with "1 overdue" in the header. I cannot tell from the screen whether the End VS tap clears it.

7. **Combined alarm: a round that is an hour late is the weakest thing on screen** (`alarm_transfusion_and_round` w412_f10).
   - Element: the dim grey-blue row "Start round · 10:00 · 7654803, 7654807".
   - Expected: if the round is late, as Home shows ("OVERDUE +65 min" on 7654807), the alarm says so and the row is easy to see.
   - Actual: it is a small low-contrast bar with the same weight as the "Snooze 2 min" outline. It carries no late marker. The "DUE NOW" chip and the orange primary button belong only to the transfusion check. The nurse has to compare "10:00" with the clock herself.

8. **Home header "NONE RUNNING · STILL OPEN" wraps and does not say what is open** (`home_stopped_reaction`, `home_end_vs_pending`, w412_f10).
   - Element: the small orange header line above "1 stopped" / "1 overdue".
   - Expected: one short line.
   - Actual: it breaks as "NONE RUNNING · STILL" / "OPEN" with "OPEN" alone on the second line, and the header grows taller. "STILL OPEN" does not say that a stopped or ended transfusion still needs an action.

9. **Home header: "next HH:MM" sits against the clock** (`home_20beds` w412_f10; `home_only_turning` wraps it).
   - Element: "All on time · next 11:06" immediately left of the clock "11:05".
   - Expected: the next-due time is clearly separate from the current time.
   - Actual: the two near-identical times sit about one character apart, so they read as "11:06 11:05". In `home_only_turning`, "All on time · next" wraps and leaves "12:00" alone on the second line under it. With 6 patients all on time the header would look the same.

## Notes (not counted as findings)
- `undo_bar_on_home` says "7654805 · transfusion started" but the list shows only 7654812 and 7654814 ("2 PATIENTS RUNNING"). I treated this as a sample-data fixture inconsistency, as instructed.
- `settings` says "Recent patients: Nobody yet" while the start screens list about 20 MRNs. Also treated as a fixture difference.
- Patients are not marked as "already running" in the MRN pickers of `start_empty` and `turn_start`. I cannot tell from those fixtures whether this matters, so it is not reported.
- Everything else I checked matched the maintainer decisions: no confirm dialogs, undo bars, reaction guide only on transfusion screens, the long lists that scroll, MRN-only identification.


<!-- Persona 2: night shift -->

# Review 5 - Persona 2 (night-shift nurse, 3 AM, first time, screenshots only)

Mostly judged on the `w412_f10_night` set. I opened the `w360_f10` day variants for the core screens as well. The `*_f20` files are 1080x7800 tall captures with a huge empty middle, so I did not judge large-font fit from them.

## Walkthrough table (icon = touch 1 where the task starts from the launcher)

| # | Task | Screens used | Touches (budget) | Verdict |
|---|------|--------------|------------------|---------|
| 1 | See all patients, what's next, what's late | `home_both_features` / `home_6beds` / `home_20beds` | icon (1) = **1** (budget 1) | PASS. Overdue rows are red-bordered and sorted on top, the header says "2 overdue" or "All on time - next 11:06", and every row has a time plus a pill (OVERDUE +65 min, SOON - 2 min). With 20 patients only about 8 rows show, but the header summary and the sort order carry the answer. The widget does it in 0 touches. |
| 2 | Acknowledge a transfusion check from the alarm | `alarm_1bed` then `alarm_done_undo` | amber "Measured & documented" (1) = **1** (budget 1) | PASS. With 2 patients due (`alarm_2beds`) it is 1 per patient, each button labelled with its MRN. From Home it is icon + amber bar = 2. |
| 3 | Whole turning round, all as suggested | `round_alarm` then `round_checklist` | "Start round" (1), then the amber all-turned button (2) = **2** (budget 2) | PASS. I inferred the all-turned label from the visible "The other 2 turned as suggested" version. From Home it is icon, "MRN - Open round", all-turned = 3. |
| 4 | Start a transfusion | `home_*` then `start_empty` then `start_bed_picked` | icon (1), Start transfusion (2), MRN button (3), "Baseline done - Start" (4) = **4** (budget 4) | PASS. Started-at (Now) and 2 h are defaulted and shown in a summary line, with "Change" for edits. |
| 5 | Start a repositioning plan | `home_*` then `turn_start` then `turn_start_bed_picked` | icon (1), Start turning (2), MRN (3), starting-position button Left 30 / Back / Right 30 (4) = **4** (budget 4) | PASS |
| 6 | End a transfusion | `home_*` then `detail_ringing` / `detail_upcoming` | icon (1), patient row (2), "End transfusion" (3) = **3** (budget 3) | PASS. The End-VS step that follows (`home_end_vs_pending`, one tap on the amber bar) is a separate +1. |
| 7 | Suspected reaction, see the steps | `home_*` or `alarm_1bed` then `reaction_1bed` | Home: icon (1), red "SUSPECTED REACTION?" bar (2) = **2**. Alarm: bar (1) = **1** (budget 2) | PASS |
| 8 | Mark "Transfusion stopped" for a specific patient | `home_*` then `reaction_20beds` | Home: icon (1), red bar (2), "TRANSFUSION STOPPED FOR:" MRN button (3) = **3**. Alarm with 1 patient: bar (1), "Transfusion stopped - MRN" (2) = **2** (budget 3) | PASS. With 20 patients the MRN grid is pinned at the bottom and shows 6 MRNs, so a later MRN needs a swipe. |
| 9 | One patient in a round "can't turn" + reason | `round_alarm` then `round_checklist` | "Start round" (1), "Can't turn" (2), reason chip (3) = **3 to record**. The round is still open, so closing it with "The other N turned as suggested" is a 4th touch = **4** (budget 3) | **Over budget by 1 if the round has to be closed** - see finding 3. |

## Findings (concrete only)

1. **Alarm with 2+ patients swaps the bottom slot from "done" to "Snooze" (task 2).**
   - Screens: `alarm_1bed__w412_f10_night` vs `alarm_2beds__w412_f10_night` (and w360 `alarm_2beds`).
   - Expected: the thumb-reach bottom slot keeps the same meaning. In `alarm_1bed` and `round_alarm` the bottom is the big amber primary ("Measured & documented", "Start round") and Snooze sits above it.
   - Actual: in `alarm_2beds` the bottom slot (y about 1830-1960) is the outline "Snooze 2 min". The red "SUSPECTED REACTION?" bar sits above it, and the amber ack buttons are inside the patient cards higher up. A nurse who learned "bottom button = done" on a single-patient alarm taps Snooze (or the reaction bar) on the 2-patient alarm instead of acknowledging.
   - Severity: medium. It is easy to learn the wrong habit at 3 AM, and a snooze delays a safety check by 2 min.

2. **The Undo screen puts a full-width Undo button exactly where the ack button was (task 2).**
   - Screen: `alarm_done_undo__w412_f10_night`.
   - Expected: after the ack, a stray or double tap at the bottom of the screen does nothing harmful. The nurse also sees how long Undo is offered.
   - Actual: the only control is "Undo", a full-width button at y about 1826-1962. That is the same spot as the amber "Measured & documented" button on `alarm_1bed` (y about 1755-1965). A second tap or a bounce on the ack button lands on Undo and silently reverts the acknowledgement. There is no visible countdown or progress for the "5 seconds", and no "Done" or "Close" alternative.
   - Severity: medium. The screenshots show no guard.

3. **Task 9 costs 4 touches, not 3, because "Can't turn" plus a reason does not close the round.**
   - Screen: `round_checklist__w412_f10_night` (and w360).
   - Expected: Start round (1), Can't turn (2), reason (3) and the round is done, as the budget implies.
   - Actual: after an exception, the cards for the other patients stay pending. The amber "The other 2 turned as suggested ✓" still has to be tapped (4th touch), or the nurse taps "Remind me in 30 min", which leaves the round open.
   - Severity: low to medium. This is the only core task whose count I got above budget.

4. **The "Other position" chip labelled "Back" reads as a navigation Back button.**
   - Screen: `round_checklist__w412_f10_night`, card 7654809, after tapping "✓ Other position".
   - Expected: a position choice looks like a position, as it does on `round_alarm` and `turn_start_bed_picked`, where each position has a figure icon next to its name.
   - Actual: the only choice offered is a text-only chip "Back" (x 62-200, y 815-950). It sits alone and left-aligned under the buttons, and the rest of the app uses "Back" at top-left for navigation. A tired nurse can read it as "cancel this choice" instead of "turn to Back (supine)". No icon, no "Lying on Back" wording.
   - Severity: low to medium.

5. **The widget's "Measured & documented ✓" button does not name the patient.**
   - Screen: `widget__w412_f10_night`.
   - Expected: the same MRN prefix as the Home bar and the alarm ("7654812 - Measured & documented ✓"), because the widget lists two overdue rows above it, a turn for 7654807 and a VS check for 7654812.
   - Actual: a bare "Measured & documented ✓" bar sits under both overdue rows. It does not say which one it records, and "Measured" cannot apply to the turn row.
   - Severity: medium if the widget is used for task 2.

6. **After the transfusion ends, the red "overdue" check has no visible action or explanation (task 6 follow-up).**
   - Screens: `detail_ended_end_vs__w412_f10_night`, `home_end_vs_pending__w412_f10_night`.
   - Expected: either a way to clear the overdue 30-min check, as `detail_stopped_reaction` gives ("30-min check - Measured & documented ✓", with the line "A check that was already overdue can still be documented below"), or a line saying End VS covers it.
   - Actual: the Checkpoints list shows a red "overdue" pill beside the 30-min check with no button. The Home row stays red-bordered ("... 30-min check overdue") and the header keeps saying "1 overdue". The only button is "End VS - Measured & documented ✓". The nurse cannot tell whether that one tap also clears the overdue check.
   - Severity: low to medium.

7. **Edit screens open with the full "WHICH PATIENT?" grid of tappable MRNs above the actual edit fields.**
   - Screens: `start_edit__w412_f10_night` ("Edit 7654812") and `turn_edit__w412_f10_night` ("Edit 7654800123").
   - Expected: the patient is fixed in edit mode, since the MRN is already in the title, and the edit fields come first.
   - Actual: about 65% of the screen is the 20-MRN grid with every MRN still tappable, so one mis-tap can change who is being edited. In `turn_edit` the 10-digit MRN is below the fold, so none of the visible MRNs is highlighted under the title "Edit 7654800123". The things to edit (time/duration behind "Change", "LYING NOW" Back / ✓ Right 30 deg) are squeezed into the bottom bar. In `turn_edit` the "LYING NOW" row offers only Back and Right 30 deg, not three positions as on `turn_start`.
   - Severity: low. Not one of the 9 core tasks.

8. **Header status text collides with, or wraps around, the clock.**
   - Screens: `home_1bed_upcoming__w412_f10_night`, `home_only_turning__w412_f10_night`, `home_20beds__w360_f10`, `home_end_vs_pending__w360_f10`.
   - Expected: the status line and the clock read as two separate things.
   - Actual:
     - On `home_1bed_upcoming`, "All on time - next 11:07" ends about 18 px (about 8 dp) before the clock "11:05", so two near-identical times sit side by side.
     - On `home_only_turning`, "next 12:00" wraps to a second line, the header grows and the clock drops to a different baseline.
     - On `home_20beds` at 360 wide, both header lines wrap ("20 PATIENTS / RUNNING", "next / 11:06") and the header takes about 14% of the screen.
     - On `home_end_vs_pending` at 360 wide, the row text "document in Cerner" butts against the "ENDED - END VS" badge, and the row runs to 4 lines.
   - Severity: low.

## Observations I did NOT count as findings (not verifiable or by design)

- `round_checklist` exists only in a scrolled-to-bottom state. With 4 patients the amber all-turned button needs a swipe. I cannot tell from the screenshot whether the footer is pinned, so I did not report it. `round_alarm` pins its buttons and shows "More below".
- `home_6beds` shows "DUE +10 min" in calm blue (grouped under "due now" in the header) while "+15 min" is red OVERDUE. The pill does state the minutes, so this looks like a deliberate grace window.
- `alarm_2beds` at w360: the second patient's ack button is half-faded behind the pinned footer. This is a scrolling list, which is by design.
- Sample-data inconsistencies were ignored as instructed (for example `undo_bar_on_home` shows a new patient in the toast but not in the list, and times that do not match the 11:05 clock).


<!-- Persona 3: gloved hands -->

# review5 / persona 3 - gloved nurse, first-time user, screenshots only

Scale used for sizes: w360 screenshots are shown at 2.5 px per dp. All screens below were viewed in w360_f10 first, then checked in w412_f10, night, and (cropped) f20 where relevant. The f20 files are 1080x7800 full-height renders, so I used them for layout only, not for judging what fits on a real screen.

## Walkthrough table

Touch 1 = the app icon wherever the task starts from the launcher.

| # | Task | Screens (in order) | Touches | Budget | Result |
|---|------|--------------------|---------|--------|--------|
| 1 | See all patients, what is next, what is late | `home_both_features` / `home_6beds` / `home_20beds` (header "5 PATIENTS RUNNING / 2 overdue", rows sorted by time, red OVERDUE pills first, SOON and "in 55 min" below). Widget (`widget`) shows the same with 0 touches. | 1 (icon) | 1 | OK. With 20 patients the header says "All on time . next 11:06" and 7 rows fit, so no scroll is needed to know the state. |
| 2 | Acknowledge a transfusion check from the alarm | `alarm_1bed` -> tap "Measured & documented" -> `alarm_done_undo` (check mark, "7654814 . 15-min check done", Undo) | 1 | 1 | OK. Two patients due (`alarm_2beds`): 1 touch each, 2 total. At w360 the second patient's button is half behind the fade, so one scroll is needed between them. |
| 3 | Whole turning round, all as suggested | `round_alarm` -> "Start round" -> `round_checklist` -> bottom orange "The other N turned as suggested" | 2 | 2 | OK. I only saw the label "The other 2 ...", not the all-as-suggested wording, so "All N ..." is inferred. If a VS alarm is due at the same time (`alarm_transfusion_and_round`) the entry is the small "Start round . 10:00 ..." bar (still 2 touches, but see finding 3). From Home the orange "7654807 . Open round" is 1 touch (3 from the launcher). |
| 4 | Start a transfusion | icon -> `home_*` "Start transfusion" -> `start_empty` tap MRN -> `start_bed_picked` "Baseline done . Start" -> `undo_bar_on_home` ("7654805 . transfusion started", Undo) | 4 | 4 | OK, exactly on budget, with defaults "Now" and "2 h". Any change to time or duration goes through the "Change" link (finding 5). |
| 5 | Start a repositioning plan | icon -> "Start turning" -> `turn_start` tap MRN -> `turn_start_bed_picked` tap the position she is lying in (Left 30 / Back / Right 30), text "7654806: lying now? Tap to start" | 4 | 4 | OK, exactly on budget. |
| 6 | End a transfusion | icon -> Home row of the patient -> `detail_upcoming` / `detail_ringing` "End transfusion" -> `detail_ended_end_vs` | 3 | 3 | OK. The follow-up "End VS . Measured & documented" is a 4th touch but is a separate check, not part of ending. |
| 7 | Suspected reaction, see the steps | From the alarm: red "SUSPECTED REACTION? STOP the transfusion & call" (1). From Home: icon + same card (2). Lands on `reaction_*`. | 1 from alarm, 2 from Home | 2 | OK. All five DO NOW steps are readable on the first screen at w360_f10. |
| 8 | "Transfusion stopped" for a specific patient | icon -> Home red reaction card -> `reaction_1bed` "Transfusion stopped - 7654801", or `reaction_20beds` "TRANSFUSION STOPPED FOR:" grid, tap the MRN | 3 | 3 | OK. With 20 patients only 7654801-806 are visible without scrolling (scroll is deliberate). From the alarm it is probably 2 touches (not shown). |
| 9 | One patient "can't turn" with a reason | `round_alarm` "Start round" (1) -> that patient's "Can't turn" (2) -> reason chip: Off unit / Procedure / Asleep/comfort / Unstable / Other (3) | 3 | 3 | OK, on budget. Patients after the first card need a scroll. The round stays open afterwards: a 4th touch on the orange "The other N turned as suggested" closes it, which is outside this task. |

Also checked and fine: undo after Measured & documented, snooze states (`alarm_snoozed`, `alarm_snooze_used_up`), the End VS pending state on Home (`home_end_vs_pending`), the stopped state (`home_stopped_reaction`, `detail_stopped_reaction`), the night variants, and the setup warning banner on Home.

## Findings (5)

1. **Widget: "Measured & documented" has no MRN and sits between unrelated rows.**
   - Screen and element: `widget__w360_f10` and `widget__w412_f10`, the orange "Measured & documented" row.
   - Expected: like `alarm_2beds`, `home_6beds` and `detail_*`, the label names the patient ("7654812 . Measured & documented").
   - Actual: the label is bare. It sits below two overdue rows of different kinds, a turn (7654807, 10:00) and a VS check (7654812, 10:50), and above 7654814 at 11:07. It could mean 7654807, 7654812, both overdue, or the next one.
   - Effect: a nurse with gloves cannot tell which patient the tap documents, so she either risks acknowledging the wrong patient or opens the app, which defeats the widget.

2. **Alarm: MRN and due time run together on the small phone.**
   - Screen and element: `alarm_1bed__w360_f10`, and the same big line in `alarm_snoozed`, `alarm_snooze_used_up` and `alarm_transfusion_and_round` at w360_f10. The big line reads "7654814 11:07".
   - Expected: separated as at w412 and in `alarm_2beds`, where the time is right-aligned with a wide gap.
   - Actual: MRN and time have the same size and weight with one space between them, so they read as one string ("7654814 11:07") and the MRN boundary is lost. The MRN is what she matches against the bag and wristband.

3. **Combined alarm: the only way into the round looks like a disabled status line.**
   - Screen and element: `alarm_transfusion_and_round__w360_f10` (also w412 night), the bar "Start round . 10:00 . 7654803, 7654807".
   - Expected: a clear secondary button, such as outlined like "Snooze 2 min" or amber-tinted, because Task 3 starts here.
   - Actual: a slate fill with no border and small regular-weight text. It is the lowest-ranked control on the screen, under the red reaction button, the outlined Snooze and the large orange button. It reads like an information banner, so a glance finds "Measured & documented" and misses the round.

4. **Round checklist: the position tile under "Other position" is a lone, bare "Back".**
   - Screen and element: `round_checklist__w360_f10` and `w412_f10` (and night), the white tile under "Other position" / "Can't turn" in the 7654809 card.
   - Expected: the same wording as the card's other buttons ("Turned to Left 30", "Turned to Right 30") and a position pictogram like the row above it.
   - Actual: the single word "Back" in the brightest white tile on the screen, with no icon. It is the same word as the navigation "Back" at the top-left of every other screen, so it reads as "close this picker". Tapping it records a turn to the Back position (undoable, but it is a wrong record). The position name must stay (frozen wording), but the icon and "Turned to" phrasing would remove the clash.

5. **Start screens: "Change" is a bare text link, the smallest target in the flow.**
   - Screen and element: `start_bed_picked__w360_f10` and `turn_start_bed_picked__w360_f10`, the underlined "Change" next to "Started 11:05 . planned 2 h . ends 13:05" and "First turn 12:00, then every 2 h".
   - Expected: a button-sized target (like the 56 dp "5 min ago" and "2 h" chips on `start_first_patient`), because it is needed whenever the transfusion began earlier than now or is not 2 h.
   - Actual: about 68 dp wide by 16 dp of visible text height, with the large "Baseline done . Start" button only about 22 dp below it. A gloved miss lands on plain text or on Start, which commits the default time and duration (5 s undo). The hit area cannot be seen in the screenshot, so this is the visible size only.
