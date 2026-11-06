# CHANGE_PLAN — Custom timer (docs/CUSTOM_TIMER.md)

Every changed line must map to an item here. A location found later is added here first. "New" means a new file. Section numbers refer to docs/CUSTOM_TIMER.md.

## 1. Data (§4 Data)
- **D1 New `data/Custom.kt`:** `CustomTimer` (table `custom_timer`: id, bedLabel = MRN, name, intervalMin, startAt, anchorAt, fromAt, stopAt?, status RUNNING/STOPPED, stoppedAt?), `CustomSlot` (table `custom_slot`: id, timerId → CASCADE, slotAt, firedAt?, ackAt?, status PENDING/RINGING/ACKED/MISSED/CANCELLED, snoozeCount, snoozeUntil?, silenced), `CustomTimerWithSlots`, `CustomDraft`.
- **D2 `data/Ward.kt`:** custom queries added to `WardDao`, so `Engine`'s constructor stays as it is (§5, trap 8). `UndoAction.custom(id)` and `UndoAction.isCustom(ownerKey)` name a custom timer's undo entries.
- **D3 `data/AppDatabase.kt`:**
  - entities + `CustomTimer`, `CustomSlot`
  - version 3 → 4, `AutoMigration(from = 3, to = 4)`
  - the class comment
- **D4 New `app/schemas/.../4.json`:** from the CI artifact.
- **D5 `UndoAction` companion:** `custom(id) = "C$id"`.

## 2. Pure rules (§4 Name, Schedule, Alarm states, Snooze)
- **R1 New `schedule/CustomRules.kt`:**
  - **Series:** `next(timer, after)` = the first anchor + n × interval that is after both `after` and `fromAt`.
  - **`materialize`:** creates rows up to and including the first future alarm, so a next alarm always exists (trap: `TurnRules.LOOKAHEAD_MS` is not used). Passed rows whose next alarm has fired are created as MISSED. Nothing at or after `stopAt`, and nothing older than a day.
  - **`step`:** PENDING → RINGING; silenced at the missed timeout; MISSED once the next alarm fires; a finished snooze is cleared.
  - **The rest:** `isAudible`, `isOverdue`, `canSnooze` (now + snooze < next alarm, and under `Settings.MAX_SNOOZES`), `arm` (CHECKPOINT, PREALERT, TIMEOUT, MISS, SNOOZE, with `custom = true`), `shouldAutoStop`, and `editAnchor`: the interval changes → anchor = the last acknowledgement or the start, truncated to the minute, and `fromAt` = now.
- **R2 New, in the same file:** `CustomName`:
  - `clean`: control characters out, at most 30 code points
  - `valid`: 1–30 code points after trimming, not only whitespace or U+200B–U+200F, U+061C, U+FEFF
  - `key`: NFC, trim, spaces collapsed, lowercase in `Locale.ROOT`
- **R3 `schedule/Labels.kt`:** `DONE = "Done ✓"` and `every(min)`.

## 3. Engine, additive only (§5 Engine; traps 1, 3, 4, 5, 8)
- **E1 New `alarm/CustomSource.kt`:** the `TurnSource` shape for custom timers. Methods: refresh, arm, audible, overdue, snooze, acknowledge, stop, cancelOpen, plus the day's housekeeping.
- **E2 `alarm/Engine.kt`:**
  - `customs` source
  - added to `refreshLocked`, `armLocked`, `snooze`, `syncRinging` (empty check and spec), `syncOverdue` and `masker`
  - new `startCustom` (duplicates return the existing id, nothing changes), `editCustom`, `stopCustom`, `setCustomStop`, `acknowledgeCustom`, `onCustomPreAlert`
  - new undo steps in `applyUndoLocked`
  - Transfusion and turn code paths are not edited.
- **E3 `alarm/Undo.kt`:** `RestoreCustomSlot`, `RestoreCustomTimer` and `DeleteCustomTimer`, with codec lines `KS`, `KT`, `DK`.
- **E4 `schedule/ArmPlanner.kt`:** `AlarmRequest` gains `custom: Boolean = false`.
- **E5 `alarm/AlarmScheduler.kt`:**
  - URI space `tvs://alarm/CUSTOM/<kind>/<id>`
  - `cancelCustom`
  - `parse` reads it, and `Parsed` gains `custom` with a default
  - The two existing spaces are unchanged (trap 1).
- **E6 `alarm/Receivers.kt`:**
  - a custom PREALERT goes to `onCustomPreAlert`
  - new `ACTION_ACK_CUSTOM` → `acknowledgeCustom` (trap 1)
- **E7 `alarm/RingSpec.kt`:**
  - `customs: List<CustomRing> = emptyList()`, `CustomRing`, `CustomRingBuilder`
  - `customOnly`, the title, and `masked` covering customs
  - `turnOnly` and the existing builders are unchanged (trap 3)
- **E8 `alarm/AlarmService.kt`:** `playingCustom` next to `playingTurn`, and a switch when either changes; custom uses the bundled chime (trap 4).
- **E9 `alarm/AlarmPlayer.kt`:** `start(…, custom = false)`, the `custom_chime` candidate, and its own vibration pattern. The two existing paths are unchanged.
- **E10 `alarm/Notifications.kt`:**
  - in `alarm`: the custom-only icon and text, inbox lines, and "Done ✓" actions only when no transfusion or round is in the spec
  - new `syncCustomOverdue` (tag `overdue_custom`), `customPreAlert` (tag `prealert_custom`) and `ackCustomIntent` (`tvs://ack-custom/…`)
  - The existing builders are unchanged (trap 1).

## 4. Screens (§2, §4)
- **U1 `ui/components/PatientField.kt`:** `autoFocus: Boolean = true` (§2.2). Default callers are unchanged.
- **U2 `ui/components/Cards.kt`:** `BedType.icon`, `tag`, `spoken` and `color` become `when`, adding CUSTOM: `ic_custom`, "CUSTOM", "Custom timer", `muted` (trap 2).
- **U3 `schedule/Urgency.kt`:**
  - `BedType.CUSTOM`
  - `BedViews.of(CustomTimerWithSlots, now, missedTimeoutMin)`: no rail, `ackLabel = Labels.DONE`
  - `sorted`: Custom after the others at an equal rank and time
- **U4 `ui/screens/HomeScreen.kt`:**
  - `HomeContent` gets new params with defaults (`custom`, `onStartCustom`, `onOpenCustom`, `onAcknowledgeCustom`)
  - custom views join the list
  - `onOpen`, `ownerKey` and the primary's acknowledge become `when` (trap 2); a custom primary names the task too ("<task> · Done ✓"), since one patient can have two
  - empty state waits for the custom list (trap 7)
  - `StartActions`: third tile "Start custom", one row with equal heights (`IntrinsicSize.Min`), and stacked at large fonts
  - `HomeScreen` wiring
- **U5 New `ui/screens/CustomStartScreen.kt`:**
  - task-name field (auto-focus, Next) and `PatientField(autoFocus = false)`
  - 3 × 3 interval grid, pinned with `PRIMARY_TAG`, hidden while the keyboard is open, with the "Enter the task name and MRN first" line; once both are valid that line reads "How often for <MRN>? One tap starts it.", and each interval button shows when it would first ring (not at large font) (reviews 1 and 2)
  - the "Other" dialog (± 1 h, ± 5 min, 15 min – 24 h, opens at 15 min); the steps look and act disabled at the limits
  - edit mode: MRN behind `ChangeRow`, a "How often?" line, buttons select, "Save changes", duplicate rename refused
- **U6 New `ui/screens/CustomDetailScreen.kt`:** MRN header, task and interval, the next alarm ("Now" while one is open; at large font the time under the state), then Edit (with an alarm open, Edit and Stop in one row); header "Custom timer running" / "Custom timer stopped", an "Auto-stop" panel with the "End of my shift" chip (a tap sets, a second clears) and a line saying whether it is on; "stops HH:MM" in the summary line; when stopped, when and how, the day's alarms. Pinned: Done when due, otherwise Stop. Stop ignores taps for 1 s after taking Done's place. No transfusion content.
- **U7 `alarm/AlarmActivity.kt`:**
  - `AlarmScreen` collects custom rings and masks them
  - `nothingOpen` counts customs (trap 3)
  - `AlarmContent` gets new params with defaults
  - new `CustomAlarm`: single and selectable, with no reaction bar
  - a "Custom · N due" button on the transfusion and round screens, only when customs are due
  - the snooze rule
  - one custom alarm: "every 4 h · next 15:00"; several: the title "N timers due", each row with the single alarm's due / overdue / snoozed chip, and the bottom button naming the task as well as the patient
  - several custom alarms listed earliest due first (alarms of the same minute by the earliest start, as §4 says); at large font the name on its own line (persona round 1)
  - after a custom Undo, `AlarmScreen` waits up to 2 s for the restored alarm before closing. Undo removes its entry and puts the alarm back in two writes, and the custom ringing query (a relation over two tables) can arrive after the entry's deletion. Lab A19@custom found it. Only a custom Undo sets the wait, so transfusions and rounds behave as before.
- **U5b New `ui/components/TaskName.kt`:** `TaskNameField` (the start and edit screens' name field: §4 Name) and `TaskNameText` (the alarm's large name: §8 Names). The field wraps instead of scrolling a long name out of view (still one line of text); at large font its label sits above the box, word for word.
- **U8 `ui/MainActivity.kt`:**
  - routes `CustomStart`, `CustomDetail` and `CustomEdit`, with `encode`/`decode` (trap 6)
  - `ACTION_START_CUSTOM`
  - Home wiring
- **U9 `ui/MainViewModel.kt`:** `customs`, `customTimer(id)`, and start, edit, stop, set-stop and acknowledge calls.
- **U10 `widget/BedsWidget.kt`:**
  - custom views
  - mask labels include custom MRNs, and `masked` also masks `what` (no existing `what` holds 5 digits, so nothing changes for them)
  - the row icon becomes `when` (trap 2), with a per-type acknowledge intent; a custom row's Done names the task too
- **U11 Resources:**
  - new `ic_custom`, `ic_stat_custom`, `ic_shortcut_custom` (24 dp grid, 2.4 stroke, round caps)
  - new `raw/custom_chime.wav`, generated with Python's standard library
  - `xml/shortcuts.xml`: third shortcut
  - `values/strings.xml`: `shortcut_custom`

## 5. Debug and lab (§6, §8)
- **L1 `app/src/debug/.../LabReceiver.kt`:** `lab.START_CUSTOM`; `ACK_ALL` also acknowledges custom alarms; the dump adds `customSlots`.
- **L2 `lab/reliability.py`:**
  - `--kind` (transfusion, custom or both): the arrange, assert and row helpers read the kind
  - custom variants of A13, A14, A20–A23 and M01: the cases written around a transfusion or a round. The others, A19 among them, run for custom alarms as they are, through the helpers.
  - custom results get ids `<id>@custom`
- **L3 `lab/score.py`:** `@custom` ids become gates (no points). The existing gate lines print as before; after them, one line says how many gates pass on area A's rule (PASS or N/A on every API, PASS on at least one).
- **L4 `.github/workflows/lab.yml`, `lab/config.json`:** done in commit 87365cc (baseline).

## 6. Tests (§8). Existing tests are unchanged, except the four Home references.
- **T1 New JVM tests:**
  - `CustomRulesTest` (rules, names)
  - `CustomSchedulePropertyTest` (≥ 10,000 cases)
  - `CustomEngineTest` (start, duplicate, edit, stop, undo, the id collision across all three types, pre-alert routing, masker labels)
  - `CustomViewsTest` (views, sort, no record-keeping)
  - `CustomNameFitTest` (the task name at font 2.0 on 360 dp, measured from the text layout; §8 Names)
  - `ui/CustomDetailGuardTest` (Stop ignores a tap for 1 s after taking Done's place; a deliberate tap stops)
- **T2 `data/MigrationTest`:** new tests: v3 → v4 keeps everything; custom alarms parse from their own address.
- **T3 New `screenshots/CustomScreenshots.kt`:** audit states: Home with custom, start empty / valid / Other dialog / edit, detail, the alarm single / several / with a transfusion and a round, the widget. Its fake data is `sharedTest/.../screenshots/CustomFakes.kt`, shared with the emulator tests as `Fakes` and `TurnFakes` are.
- **T4 androidTest:**
  - `TapBudget`: tasks 10–12
  - `UiChecks`: custom start, edit, detail and alarm, and Home with custom cards
  - `Robustness`: custom widget masking and Done from the widget; a double tap on Done and on an interval acts once; Undo of a custom Done restores the exact row
- **T5 Screenshot references:** only `transfusion_home`, `transfusion_home_empty`, `repositioning_home` and `repositioning_home_only` are recorded again, after a pixel diff shows only the allowed regions. The proof goes in docs/SCREENSHOT_REGRESSION.md.

## 7. Docs
- **K1** SPEC.md (Custom timer section), docs/FLOWS.md (tasks 10–12), PROGRESS.md, BASELINE.md, SCORECARD.md (gates), and docs/SCREENSHOT_REGRESSION.md.
