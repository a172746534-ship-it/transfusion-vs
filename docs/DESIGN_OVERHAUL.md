# PART 1 — Design overhaul: point-by-point UX/UI review → creative redesign

## 0. Ground rules
- Before anything else, save PART 1 and PART 2 of this message verbatim as
  docs/DESIGN_OVERHAUL.md and docs/TEST_LOOP.md.
  - Re-read both at the start of every phase.
  - They are the source of truth — not your memory of this chat.
- Read SPEC.md. Both features (Transfusion, Repositioning rounds) work and are in daily use.
- Scope: experience and visual design only. The alarm engine, scheduling rules, data meaning
  and privacy rules do not change.
- This task SUPERSEDES the earlier rule "no new colors / identical to the current design".
  - New rule: both features look like ONE product inside ONE new design system.
  - Update SPEC.md accordingly.
- Clinical content is FROZEN.
  - These must match /design/front.png, /design/back.png and SPEC.md word for word:
    - the VS list (T · HR · RR · BP · SpO₂)
    - the reaction warning signs
    - the DO NOW steps 1–5
    - the transfusion checkpoint schedule
    - the round schedule
  - Redesign how they look, never what they say.
- Never hide or bury the reaction guide or the overdue state for the sake of minimalism.
- The UI language stays English.
- Work on a branch named `design-overhaul`. main stays installable and untouched until
  PART 2's stop condition is met.
- Keep your state in files so you can resume after any interruption or context reset:
  - SPEC.md, AUDIT.md, SCORECARD.md
  - PROGRESS.md (done / in progress / next)
  - Commit after every meaningful step.
  - After any restart, read PROGRESS.md first.
- At the end of each phase, report to me in Arabic, briefly.

## 1. Who uses this, and where
- Nurses on a busy pediatric oncology ward: 12-h shifts, constant interruptions.
- The phone lives in a scrub pocket. It's used one-handed, sometimes with gloves,
  in the middle of a task, under stress.
- Bright ward lights by day; dark patient rooms with sleeping children at night.
- They look at the screen for 1–2 seconds, not 20.
- Colleagues get the APK with zero training. If it needs explaining, the design failed.

## 2. The two non-negotiables
1. **Tap budget.** Every core task finishes in ≤ 4 taps total (target: 3), counted from
   the app icon. No typing in any core task.
2. **Zero-training usability.** A nurse who has never seen the app completes every core
   task on the first try, without help.

### What counts as a tap
- Every touch interaction = 1: a tap, a long-press, a swipe, a drag, or a scroll needed to
  reach the target.
- Tapping the app icon = tap 1. Unlocking the phone does not count.
- Preconditions: first-launch setup is done (permissions granted, ward beds entered).
  Setup itself is outside the budget.
- If the phone is in use when an alarm fires, the alarm arrives as a heads-up notification.
  Its action button counts the same as the alarm-screen button.

## 3. Core tasks and tap budgets
| # | Task | Max taps |
|---|------|----------|
| 1 | See the state of all my beds (what's next, what's late) | 1 (open = done) |
| 2 | Acknowledge a transfusion check from the alarm | 1 |
| 3 | Complete a whole turning round when every turn goes as suggested | 2 from the alarm |
| 4 | Start a transfusion | 4 |
| 5 | Start a repositioning plan | 4 |
| 6 | End a transfusion | 3 |
| 7 | Suspected reaction → see the steps | 2 from anywhere, including the alarm screen |
| 8 | Mark "Transfusion stopped" for a specific bed | 3 |
| 9 | Mark one bed in a round as "can't turn" with a reason | 3 from the alarm |

Settings, the setup check, stats and export are outside the budget, but they must still be obvious.

## 4. Ideas that make the budget possible (evaluate, improve, or replace with better ones)
- **Bed grid instead of typing.**
  - I enter the ward's bed numbers once in Settings; every flow picks a bed with one tap.
  - Beds with running timers are shown first.
  - "+ Other bed" with typing stays as a rare fallback.
  - Bed numbers are not patient identifiers.
- **Defaults instead of questions.** Start time = now. Duration is preselected (last used,
  or from Settings). Round interval = the ward default. Changing any of them is optional,
  never required.
- **Confirmations merged into the action.** One button "Baseline done · Start" instead of
  a checkbox plus a button.
- **Tapping the patient's current position IS the action that starts a turning plan.**
- **"All turned as suggested ✓"** completes the whole round; only exceptions need extra taps.
- **Undo instead of "Are you sure?" dialogs.**
- **Notification action buttons** ("Done ✓"), so a check can be acknowledged without
  opening anything.
- **A home-screen widget** showing each bed's next check as a CLOCK TIME. Widgets can't
  refresh every second, so no live countdowns on the widget.
- **App-icon shortcuts:** long-press → Start transfusion / Start repositioning.

## 5. Pocket-safe alarms (one tap must never be an accident)
- Ignore touches for the first second after the alarm screen appears.
- Ignore touches while the proximity sensor is covered (phone in a pocket).
- Every acknowledgement can be undone:
  - a 5-second undo snackbar
  - plus "Undo last" on that bed's card for 5 minutes
- Every acknowledgement gives a distinct confirmation haptic.

## 6. Creative brief
We are NOT building an alarm clock. Build a calm, intelligent clinical companion:
quiet when everything is fine, unmistakable when something needs me.

**Rule: be creative in LOOK, boring in BEHAVIOR.**
- Creativity lives in color, typography, shape, the signature element and motion.
- Interaction patterns stay standard and predictable. Nobody should have to discover how
  something works.

**Avoid the generic.**
- No default Material 3 baseline palette and no template look.
- Each direction has its own palette, typography and signature element.

### Directions to explore
- **Urgency language.** The UI changes its "temperature" as a check approaches:
  calm → due soon → due now → overdue. Use color AND shape AND text together, so it reads
  at arm's length and for color-blind users.
- **A signature element** evolved from the paper card's timeline (dots on a line).
  Example: a live progress ring or line per bed that fills toward the next check.
  It becomes the app's identity.
- **Day and night themes.**
  - A day theme for bright wards.
  - A true night theme: dark and low-glare, no white flash at 3 AM, and an alarm screen
    that doesn't light up a sleeping child's room more than it has to.
  - Night switches on automatically 20:00–08:00 (configurable) or follows the system setting.
- **Haptics with meaning.** Different patterns for due / overdue / confirmed.

### Color rules
- Red = danger only (reaction, overdue). Never decorative — the blood-drop icon does NOT
  use the danger red.
- Transfusion vs repositioning must be recognizable instantly
  (icon + label + one consistent type cue).
- The light and dark versions are designed together, not just inverted.
- At least ONE of the three directions evolves the paper card's identity (navy/red/amber),
  so the app and the card still feel like one system. The other two may go further.
- The contrast and color-blind thresholds are in PART 2. Meet them.

### Fonts
Bundle OFL fonts in res/font. No downloadable fonts: the app has no INTERNET permission
and must work without Google Play services.

## 7. Phases

### Phase 1 — Point-by-point audit (no app-code changes; building the test lab is allowed)
1. Screenshot every screen in every state, including the widget, the notifications and
   the shortcuts:
   - empty, 1 bed, 20 beds
   - upcoming, due, ringing, overdue, missed, reaction
   - light and dark
   - font scale 1.0 and 2.0
   - a 360dp-wide phone
2. Count the taps for each core task in the CURRENT app, in a table:
   task | current taps | budget | pass/fail.
3. Review each screen against:
   - the tap budget and the no-typing rule
   - glanceability: can the next action, the bed and the time be read in 1 second at
     arm's length?
   - one primary action per screen, placed in the thumb zone
   - Nielsen's 10 usability heuristics
   - the touch-target, contrast and color-blind thresholds in PART 2
   - font scaling and TalkBack labels
   - microcopy: short verbs, ward vocabulary, no ambiguity
   - consistency between the two features
   - error prevention and recovery (undo, pocket touches)
4. Write AUDIT.md with numbered findings. Each finding has:
   - the screen and a screenshot
   - the problem
   - why it matters on a busy ward
   - severity: Critical / Major / Minor
   - the proposed fix
5. Run the PART 2 scorecard once on the current app. That is the BASELINE score.

Then continue to Phase 2 without waiting.

### Phase 2 — Three creative directions ⛔
For each direction, provide:
- a name, and the idea in 2 lines
- the palette, light and dark, with hex values, including all urgency-state colors
- typography
- the signature element
- the urgency language
- rendered screenshots of Home, Alarm and Round alarm, in light and dark

Show the three side by side in a PR. ⛔ STOP and wait: I pick one, or a mix.
This is the only point where you wait for me.

### Phase 3 — Flows and layout
- Redesign the core-task flows to meet the tap budget.
- Document each flow in the PR as a strip of screenshots with the tap numbers (①②③④)
  marked on them.
- Continue without waiting.

### Phase 4 — Build
- Order: the design system first (tokens, components), then the screens, then the widget,
  notifications and shortcuts.
- No hard-coded colors or font sizes outside the design system.
- The alarm engine, schedules, data and privacy rules stay untouched.
- If any stored data must change (e.g. the bed list), use a Room migration and write a
  migration test.

### Phase 5 — Prove it
- Execute PART 2 completely.
- The work is done only when PART 2's stop condition is met.
