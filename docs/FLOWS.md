# Phase 3: flows and layout

Taps are counted the way docs/DESIGN_OVERHAUL.md §2 says: every touch is 1, the app icon is tap 1, unlocking doesn't count. `TapBudget.kt` measures these on an emulator. Strips with the tap numbers: `docs/flows/`.

| # | Task | Budget | Flow | Taps |
|---|---|---|---|---|
| 1 | See all my beds | 1 | ① icon. Home: every bed, most urgent first, each with its next clock time and state. Six fit without a scroll. | 1 |
| 2 | Acknowledge a transfusion check | 1 | ① Measured & documented ✓ | 1 |
| 3 | A whole round, all as suggested | 2 | ① Start round ② All turned as suggested ✓ | 2 |
| 4 | Start a transfusion | 4 | ① icon ② Start transfusion ③ the bed ④ Baseline done · Start | 4 |
| 5 | Start a turning plan | 4 | ① icon ② Start turning ③ the bed ④ the position the patient lies in | 4 |
| 6 | End a transfusion | 3 | ① icon ② the bed's card ③ End transfusion (Undo, no dialog) | 3 |
| 7 | Suspected reaction → the steps | 2 | Home: ① icon ② the red bar. Alarm or Detail: ① the red bar. | 2 / 1 / 1 |
| 8 | Mark "Transfusion stopped" | 3 | ① icon ② the red bar ③ Transfusion stopped — Bed 12 | 3 |
| 9 | One bed "can't turn" with a reason | 3 | ① Start round ② Can't turn ③ the reason | 3 |

No core task needs typing. "+ Other bed" is the rare fallback.

## What changed, and why

- **No chooser.** It cost a tap on every start, and the budget has no room for it. Both starts are on Home.
- **Bed grid.** Typing was the slowest part of every start and a chance to type a name. Beds with running timers come first.
- **Defaults, not questions.** Started now, last duration used. They stay on the screen, one tap to change, never required.
- **Confirmations merged into the action.** The tick box and Start became one button. The position tap starts the plan.
- **Undo, not dialogs.** A dialog costs a tap every time and gets tapped through; Undo costs nothing unless it is needed.
- **Pinned actions.** Primary actions sit in the bottom third and never scroll away, so a 7-bed round can't push Start round below the fold.
- **The Snooze slot stays.** When snoozes run out it reads "No snoozes left" in the same place, so a thumb that learned its place can't land on something else.
- **Reaction steps first.** DO NOW comes before the signs, and "Transfusion stopped" stays in view.
- **Same-minute collision.** The transfusion goes first, and the screen says "Then: Turning round · 10:00 · Bed 3, Bed 7". Once it is acknowledged, the round takes the screen.
