# Screenshot regression: custom timer (v0.6.0)

Baseline: the audit set from build run 42186380368, which is code identical to `main` (see BASELINE.md).
Compared against: the audit set from run 42188176050 on `custom-timer`, with no custom timer running.

## The audit set (182 screenshots, every screen × width × font × night × Arabic)
- **133 pixel-identical.** Everything that isn't Home.
- **49 differ. All of them are Home** (`home_*` and `undo_bar_on_home_*`).
  - In every one, the real change starts at least 58 % down the screen: the pinned area.
  - Above that, the only differences are at most two colour levels (out of 255) on card outlines. That is compositing noise, not visible.
  - What changed, measured with `PIL.ImageChops`:
    - The Start row has the third tile, "Start custom".
    - At 360 and 412 dp, all three labels wrap to two lines, so the row is 40 px taller.
    - Everything pinned above it (the primary button, the reaction banner, the setup warning, the undo bar) is the same picture, moved up exactly 40 px.
    - The list's visible area ends 40 px sooner.
  - These are the differences section 8 of docs/CUSTOM_TIMER.md allows.

## The 19 committed references (`app/src/test/screenshots`)
- **15 pass verify unchanged.**
- **4 re-recorded,** the four that show Home, after this check:

| Reference | First changed row | Old rows that moved up 40 px | Start row (old) |
|---|---|---|---|
| `transfusion_home` | 1915 of 2556 | 1915–2147 | 2364–2531 |
| `transfusion_home_empty` | 1553 of 2556 | 1553–2364 | 2364–2531 |
| `repositioning_home` | 4160 of 4800 | 4160–4608 | 4608–4775 |
| `repositioning_home_only` | 2324 of 2556 | 2324–2364 | 2364–2531 |

- Every old row either matches in place or matches 40 px higher.
- The only exception is the Start row itself, plus one pixel in `transfusion_home` at (70, 2147) that is off by one level (anti-aliasing on a rounded corner).

## The cost to task 1 (measured, for the reviewers)
- docs/CUSTOM_TIMER.md §2 estimated the wrap at about 12 dp and expected all six cards to stay in view at 360 × 800.
- **Measured:**
  - The row grows 40 px: 13.3 dp at 360 × 800, 15.2 dp at the references' density.
  - In `home_6beds__w360_f10`:
    - the sixth card ends at y = 1754
    - the list now shows content to about y = 1700
  - So the bottom **~18 dp of the sixth card** (its timeline dots) sits under the list's edge and its "more below" fade.
  - Its bed number, clock time and state ("in 55 min") stay in view.
- Task 1's tap-budget check ("every bed's state readable without a touch") still holds. Whether the clipped timeline is acceptable is for the design reviewer and the personas.
