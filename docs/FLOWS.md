# Phase 3: flows and layout

Taps are counted the way docs/DESIGN_OVERHAUL.md §2 says: every touch is 1, the app icon is tap 1, unlocking doesn't count. `TapBudget.kt` measures these on an emulator. Strips with the tap numbers: `docs/flows/`.

| # | Task | Budget | Flow | Taps |
|---|---|---|---|---|
| 1 | See all my patients | 1 | ① icon. Home: every patient, most urgent first, each with its next clock time and state. Six fit without a scroll. | 1 |
| 2 | Acknowledge a transfusion check | 1 | ① Measured & documented ✓ | 1 |
| 3 | A whole round, all as suggested | 2 | ① Start round ② All turned as suggested ✓ | 2 |
| 4 | Start a transfusion | 4 | ① icon ② Start transfusion, type the MRN ③ Baseline done · Start | 3 + typing |
| 5 | Start a turning plan | 4 | ① icon ② Start turning, type the MRN ③ the position the patient lies in | 3 + typing |
| 6 | End a transfusion | 3 | ① icon ② the patient's card ③ End transfusion (Undo, no dialog) | 3 |
| 7 | Suspected reaction → the steps | 2 | Home: ① icon ② the red bar. Alarm or Detail: ① the red bar. | 2 / 1 / 1 |
| 8 | Mark "Transfusion stopped" | 3 | ① icon ② the red bar ③ Transfusion stopped — 7654812 | 3 |
| 9 | One patient "can't turn" with a reason | 3 | ① Start round ② Can't turn ③ the reason | 3 |

Tasks 4 and 5 type the MRN on the number pad, which opens by itself with the screen. That is the maintainer's decision (2026-10-31): some places in the hospital have no beds, and a list of numbers to pick from was clutter. No other task types anything.

## What changed, and why

- **No chooser.** It cost a tap on every start, and the budget has no room for it. Both starts are on Home.
- **MRN on the number pad.** Only digits can be entered, so a name can't be. The button that finishes the start stays above the keyboard.
- **Defaults, not questions.** Started now, last duration used. They stay on the screen, one tap to change, never required.
- **Confirmations merged into the action.** The tick box and Start became one button. The position tap starts the plan.
- **Undo, not dialogs.** A dialog costs a tap every time and gets tapped through; Undo costs nothing unless it is needed.
- **Pinned actions.** Primary actions sit in the bottom third and never scroll away, so a 7-patient round can't push Start round below the fold.
- **The Snooze slot stays.** When snoozes run out it reads "No snoozes left" in the same place, so a thumb that learned its place can't land on something else.
- **Reaction steps first.** DO NOW comes before the signs, and "Transfusion stopped" stays in view.
- **Same-minute collision.** The transfusion goes first, and the screen has a "Start round · 10:00 · 2 patients" button (or "· +65 min" when it is late). Once it is acknowledged, the round takes the screen.
