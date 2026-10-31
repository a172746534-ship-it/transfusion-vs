# Project: Transfusion VS — Android alarm app for blood-transfusion vital signs

> Living spec. The "Decisions log" at the end records everything agreed after the original spec.

## Context
Audience: many nursing staff, each on their own phone (many brands and Android versions).

I'm a pediatric oncology nurse. During blood transfusions, vital signs must be measured
and documented in Cerner (our EMR) on a fixed schedule. Late or missed documentation keeps
showing up in quality reports. I currently use a two-sided paper reminder card. I want a
native Android app that turns that card into reliable alarms.

The card images are in /design:
- front.png = the schedule side
- back.png = the suspected-reaction side
They are the visual and wording source of truth.

I am not a programmer and cannot build locally. The APK must be built by GitHub Actions
and sideloaded (no Play Store). Explain every manual step I must do in simple Arabic,
as a numbered list.

## #1 requirement (non-negotiable)
The alarm MUST ring with sound and show a full-screen alert when:
- the screen is off/locked
- the phone is on silent or vibrate
- the device has been idle (Doze) for hours
- the app was swiped away from recents
- after a reboot or an app update
- on aggressive OEM battery savers (Xiaomi/Redmi/POCO, Oppo/Realme/OnePlus, Vivo,
  Samsung, Infinix/Tecno, Huawei/Honor)

If any feature conflicts with this requirement, this requirement wins. Verify every API,
permission and foreground-service rule against the current official Android docs for the
target SDK before using it. Do not rely on memory.

## Tech stack
- Kotlin, Jetpack Compose, Material 3, single module
- minSdk 26; targetSdk and compileSdk = latest stable
- Room, coroutines/Flow, MVVM; no DI framework (keep it simple)
- NO INTERNET permission. allowBackup=false. No analytics. No third-party SDKs beyond AndroidX.

## Alarm engine (recommended approach — confirm it or improve it)
- Schedule each checkpoint with AlarmManager.setAlarmClock(), using a unique
  PendingIntent per checkpoint.
- Permissions:
  - USE_EXACT_ALARM (API 33+), plus SCHEDULE_EXACT_ALARM with maxSdkVersion 32
  - POST_NOTIFICATIONS, USE_FULL_SCREEN_INTENT
  - RECEIVE_BOOT_COMPLETED, VIBRATE, WAKE_LOCK
  - REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
  - the correct foreground-service permission and type
- When an alarm fires:
  - a receiver starts a foreground service
  - the service loops the alarm sound with AudioAttributes USAGE_ALARM (so it rings on silent),
    plus vibration
  - it posts a full-screen-intent notification: category ALARM, NOT local-only,
    so smart bands and watches receive it
- AlarmActivity: setShowWhenLocked(true), setTurnScreenOn(true), keeps the screen on.
- Reschedule all pending alarms on BOOT_COMPLETED, MY_PACKAGE_REPLACED, TIME_SET
  and TIMEZONE_CHANGED.
- Ringing stops only on Acknowledge or Snooze.
- After 10 min unanswered: stop the sound, mark the checkpoint MISSED, and keep a persistent
  "Overdue — Bed X" notification until I acknowledge. A late acknowledge is still allowed
  and is logged as late.
- If several checkpoints (any beds) fire in the same minute, show ONE alarm screen
  listing all of them.

## Schedule rules (exactly as on the card)
- Starting:
  - Start is enabled only after I confirm "Baseline VS taken".
  - T0 = start time: default is now, editable (e.g. "started 5 min ago").
  - T0 is logged at Start and gets no alarm.
- Checkpoints: T0+15, T0+30, T0+60, then every 60 min at the SAME MINUTE as T0.
  Example: start 10:20 → 10:35, 10:50, 11:20, 12:20, 13:20 …
- Planned end:
  - Planned duration chips: 1h / 2h / 3h / 4h / custom.
  - At the planned end, fire an "End check" alarm: "Transfusion due to finish — measure end VS".
  - If the transfusion runs past the planned end, hourly checkpoints continue until I tap End.
- End: tapping End logs the end time and shows the End VS prompt.
- Time-limit warning: an alarm before the max-duration limit. Defaults: limit 4 h,
  warn 15 min before. Both are configurable.
- Pre-alert: optional, default OFF, choice of 1 / 2 / 5 min before each checkpoint.
  Gentle notification and a short vibration only, not a full alarm.
- Past start time: if the start time is in the past, checkpoints that have already passed
  are marked overdue and trigger one immediate alarm.
- Editing the start time or the duration reschedules everything.
- Code the schedule calculation as a pure Kotlin function with unit tests covering:
  - the 10:20 example
  - running past the planned end
  - checkpoints in the same minute across beds
  - time and timezone changes

## Screens (English UI, same wording as the card)

1. **Setup check** — shown on first launch, and reachable from Settings.
   - A live checklist with a green/red status for each item:
     - notifications allowed
     - exact alarms allowed
     - full-screen intent allowed (canUseFullScreenIntent on API 34+)
     - battery optimization ignored
     - alarm volume > 0
   - Every red item has a "Fix" button that deep-links to the right settings page.
   - Detect the manufacturer and show tailored Autostart/battery steps. Use deep links
     where they exist; otherwise fall back to the app-details settings page.
   - A big button: "Test alarm in 1 minute" (I lock the phone and wait).

2. **Home**
   - One card per running transfusion, showing:
     - bed label and its color
     - a mini timeline in the card's style
     - the next checkpoint with a countdown
     - an overdue state in red
   - A primary button: "Start transfusion".
   - A persistent red "Suspected reaction?" banner.

3. **Start transfusion**
   - Bed label: short free text such as "Bed 12". The hint text says: no names or MRN.
   - Color: auto-assigned.
   - Start-time picker, duration chips, and the required "Baseline VS taken" checkbox.
   - A full timeline preview with real clock times, like the card's example row.

4. **Transfusion detail**
   - The full timeline in the card layout.
   - Each checkpoint shows one status: upcoming / ringing / on time / late +N min / missed.
   - Buttons: End transfusion, Edit, Transfusion stopped (reaction).

5. **Alarm screen** (over the lock screen)
   - Navy full-screen background.
   - Big title, e.g. "15-min check — Bed 12".
   - The card's MEASURE (T · HR · RR · BP · SpO₂) → DOCUMENT (in Cerner, right away) blocks.
   - One huge button: "Measured & documented ✓".
   - A smaller "Snooze 2 min" button. Max 3 snoozes; after that it is no longer offered.
   - A red "Suspected reaction" link.
   - No swipe-to-dismiss.

6. **Suspected reaction** — the back card, exactly.
   - The "STOP & CALL IF YOU SEE" list.
   - The "DO NOW" steps 1–5 as a tappable checklist.
   - A "Transfusion stopped" button: cancels that bed's remaining alarms and marks it
     STOPPED_REACTION. No clinical details are stored.

7. ~~**Log & stats**~~ — **removed 2026-10-28** (Cerner is the record; see the Decisions log).
   Finished transfusions are deleted automatically 24 h after they end.

8. **Settings**
   - on-time tolerance (default ±5 min)
   - snooze length (1–2 min)
   - missed timeout (default 10 min)
   - pre-alert
   - time limit and warning
   - Sound+vibrate vs Vibrate-only (night mode)
   - alarm sound
   - a link to Setup check

## Visual design (match /design)

Colors (sampled from the card images):
- Navy #13233F: headers, filled dots, anchor time chips, body text
- Red #B3121F: reaction elements and overdue states
- Soft pink #FDECEC: the warning-signs panel
- Amber #F5A623: the DOCUMENT block and the small-caps label on navy
- Blue #1D4E9E: the MEASURE block
- Light panel #F5F7FB, muted text #5B6577, outline #D0D3D9

Shape and type:
- Rounded corners ~16dp, flat style, generous padding.
- Headings and numbers in a heavy geometric sans: bundle an OFL font close to the card's
  in res/font. Use tabular figures for times.
- Small-caps letter-spaced labels like "TIME FROM START".

Timeline:
- Dots: filled for start / end / done, hollow for upcoming, red for overdue.
- Time chips under the dots: navy-filled for start and hourly, outlined for 15 min, 30 min and End.

Icons: a blood drop on the running header; a warning triangle on reaction elements.

Theme and touch: light theme only; the alarm screen is navy. Minimum 56dp touch targets
(used with gloves).

## Data (no patient identifiers, no vital values — documentation stays in Cerner)
- Transfusion: id, bedLabel, colorIndex, startAt, plannedDurationMin, endAt?,
  status (RUNNING / ENDED / STOPPED_REACTION), baselineConfirmed
- Checkpoint: id, transfusionId,
  type (START / M15 / M30 / HOURLY / PLANNED_END / LIMIT_WARNING),
  scheduledAt, firedAt?, ackAt?, snoozeCount,
  status (PENDING / RINGING / ACKED / MISSED / CANCELLED)
- Store times as epoch millis; display them with java.time in the device timezone.

## Build & release (GitHub Actions)
- Workflow on push to main and on manual dispatch: run unit tests → assembleRelease →
  upload the APK as an artifact.
- On tags v*: also create a GitHub Release with the APK attached, so I can download it
  from my phone.
- Signing:
  - Sign with ONE fixed release keystore from GitHub Secrets
    (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD).
  - This way updates install over the old version without losing data.
  - Generate the keystore for me, and add the secrets with the gh CLI if it's available;
    otherwise give me the exact steps.
  - Never commit the keystore or any password.
- versionCode = GitHub run number.
- If you can't build locally, use CI as your compiler: push, read the Actions logs, fix,
  and repeat until the build is green. Never hand me a red build.

## How to work
1. Save this spec as SPEC.md in the repo and keep it updated.
2. Use plan mode first (study /design, propose structure and phases, list assumptions,
   ask at most 5 questions, wait for OK).
3. Build in phases, with one commit per phase and an installable APK from each:
   - **Phase 1:** skeleton + CI/signing + alarm engine + Setup check + "Test alarm in 1 minute".
     STOP here so I can test on my real phone: locked, silent, and overnight.
   - **Phase 2:** the Start / Home / Detail / Alarm / Reaction screens with the full design.
   - **Phase 3:** Settings (Log & stats and CSV were dropped — see Decisions log).
   - **Phase 4:** polish, and run through the checklist below.
4. Don't add features that aren't in this spec without asking me first. Keep the code
   readable and commented.
5. After each phase, tell me briefly, in Arabic: what changed, how to install it,
   and exactly what to test.

## Definition of done (on a real phone)
- Locked screen + silent mode → full-screen alarm with sound.
- App swiped from recents and phone idle 2+ hours → the alarm still fires on time.
- Reboot with a running transfusion → alarms still fire.
- Installing an update over the old version → data and alarms are kept.
- Two beds with checkpoints in the same minute → one combined alarm.
- Snooze, missed timeout, late acknowledge, reaction stop and End are all logged correctly.
- Changing the phone's time or timezone → the schedule is recalculated correctly.

---

# Feature 2: Repositioning rounds (fixed 2-hour schedule) — added 2026-10-28

The transfusion feature is in daily use: do NOT break it.

## Why
Some patients must be repositioned every 2 hours (pressure-injury prevention). Turning happens
on a fixed ward clock, and turns get late or forgotten during a busy 12-h shift. Same reliable
alarm behaviour as transfusions.

## Rules
- Reuse the existing alarm engine (setAlarmClock, the foreground service, the full-screen
  alarm, re-arm after reboot/update/time change, the missed timeout and Overdue notification).
  Generalise it for multiple timer types — no second engine.
- Every #1 reliability requirement applies to turns too. Same privacy rules: bed label only.

## Fixed schedule
- Slots on a fixed local clock grid = multiples of the interval from 00:00, around the clock.
  Interval chips 1 h / 2 h (default) / 4 h — all keep 08:00 and 20:00 on the grid.
  The default ward interval is set in Settings.
- A late turn does NOT move the schedule (turned 10:25 → next still 12:00).
- First slot = the next slot after the start; if that is < 30 min away, the following one
  (start 09:10 → 10:00; start 09:45 → 12:00). Shown in the preview and changeable.
- Late acknowledge is allowed until the next slot fires; then the slot closes as MISSED and the
  new slot rings. At the missed timeout the sound stops and an Overdue notification lists
  the beds not ticked.
- "Remind me in 30 min" only if it lands before the next slot.
- Optional stop time, with a quick chip "End of my shift" (the next 08:00 or 20:00); no alarms
  at or after it. A plan can be restarted at any time.
- Egypt uses DST: a local slot that doesn't exist is skipped; a repeated hour rings once.

## Positions
- Left 30° · Back · Right 30°, plus optional Prone and HOB up.
- Per bed: exclude positions; rotation = the selected positions cycling, order editable.
  An excluded position is never suggested. The current position is set when starting.

## Round alarm
- Each slot produces ONE alarm for all due beds: "Turning round · 10:00 · 4 beds".
- "Start round" stops the sound and shows a checklist: per bed the label, current → next
  position with icons, a ✓ (turned to the suggestion; a secondary action picks another
  position), and "Can't turn" with reasons Off unit · Procedure · Asleep/comfort · Unstable ·
  Other (logged as SKIPPED, not MISSED).
- Static line: "Check pressure points: occiput · ears · sacrum · heels".
- Snooze 2 min for the whole round, same limits as transfusions (max 3).
- A transfusion checkpoint in the same minute → one screen, transfusion items on top.
- Each type has its own alarm sound and vibration pattern.

## Design
- Must be indistinguishable from the existing app: built only from the design-system tokens and
  components; types differ only by icon, label and alarm sound — never by a new colour.
- Header "REPOSITIONING RUNNING" with a rotation-arrow icon; same timeline composable with
  slots; Home card / Start / Detail / Alarm mirror their transfusion counterparts.
- Position icons: one vector set (top-down figure on a bed), same stroke and grid as the others.
- Screenshot tests (Roborazzi) for every new screen and its transfusion counterpart, reviewed in a
  PR before the logic is wired; a CI check fails on colours or font sizes hard-coded outside
  the design system.

## UI structure
- Home: two sections, "Transfusions" and "Repositioning". The repositioning card shows the bed,
  current position + icon, last turn, next slot with countdown, and the red overdue state —
  it doubles as the handover summary.
- Start opens a type chooser: Transfusion / Repositioning.
- No Log & stats tab and no CSV (decision 2026-10-28). The plan's Detail shows today's slots
  and the **longest gap without a turn (last 24 h)**, computed from actual turn times.

## Data
- New tables with a proper Room migration (v1 → v2) and a migration test; existing transfusion
  data and pending alarms survive the update.
- Turn event fields: planId, slotAt, ackAt?, positionFrom, positionTo,
  status (ACKED / MISSED / SKIPPED), skipReason?, snoozeCount.
- Kept 24 h like transfusions (each running plan keeps its latest turn).

## Done = all of these pass on the phone
- Every transfusion test above still passes.
- The update installs over the current version: running transfusions, their alarms and Settings
  are kept.
- Locked + silent: the round alarm rings at 10:00 sharp, with its own sound.
- Turned at 10:25 → the next alarm is still at 12:00.
- Started at 09:45 → the first alarm is at 12:00 (changeable).
- 3 beds on a plan → one round alarm listing all 3.
- An excluded position is never suggested.
- A slot never acknowledged → MISSED when the next slot fires.
- The end-of-shift stop time works.
- A transfusion and a round in the same minute → one screen, transfusion first.

---

# Design overhaul — added 2026-10-31

The full brief is docs/DESIGN_OVERHAUL.md and the test loop is docs/TEST_LOOP.md; both are the source
of truth for this work. It **supersedes** the rule "no new colours / indistinguishable from the
existing design" in Feature 2 and the "light theme only" line under Visual design:
both features now look like ONE product inside ONE new design system, with day and night themes.
Clinical content stays frozen, word for word: the VS list, the warning signs, DO NOW 1–5 and both
schedules. The alarm engine, schedules, data meaning and privacy rules do not change.

---

## Decisions log

### 2026-10-28 — plan approved
- **Everything free.** No paid or external services, no API keys, no accounts beyond a free
  GitHub account. The app has no INTERNET permission. "API" in this repo only ever means
  Android's built-in system functions.
- **Audience:** many staff on many phone brands → the per-brand Setup guide is a core feature;
  README.md has Arabic install steps for any colleague. Data stays on each phone.
- **Repo:** public on GitHub (APK downloadable from Releases without login; unlimited free CI).
  It contains only code, this spec and the card images — no names, no secrets.
- **Same-minute alarm:** one screen; **each bed gets its own big ✓ button**. Ringing continues
  until every row is acknowledged or snoozed. A single row shows the one huge button.
- **Toolchain (verified 2026-10-28):** compileSdk/targetSdk 37 (Android 17), AGP 9.4.1 with
  built-in Kotlin, Kotlin 2.4.21, Gradle 9.6.1, JDK 17, Compose BOM 2026.09.00.
- **Foreground service type:** `systemExempted` — permitted for apps holding
  USE_EXACT_ALARM/SCHEDULE_EXACT_ALARM, no timeout, not on the Android 15 BOOT_COMPLETED ban list.
- **Android 17 background-audio rule:** background audio needs a running FGS + the exact-alarm
  permission + USAGE_ALARM. The app only ever plays sound from the alarm service.
- **Direct Boot:** the database and settings live in device-protected storage, so alarms are
  re-armed after a reboot even before the first unlock. A bundled tone is the fallback sound.
- **Alarm notification** carries action buttons (✓ / Snooze) for heads-up use and watches.
- **Setup check** also shows "Do Not Disturb isn't on Total silence" (Total silence mutes alarms).
- **Missed timer** = 10 min from the first ring; snoozes (max 3) don't extend it.
  On time = acknowledged within ±tolerance of the scheduled time; otherwise "late +N min".
- **PLANNED_END** replaces whatever fixed checkpoint falls on the same minute (e.g. the 2-h
  check for a 2-h transfusion); a custom duration gets its own End check.
- **Schedule math** is real elapsed time from T0 (Instant + Duration). A timezone change only
  changes the displayed clock; T0 is truncated to the whole minute.
- **Edits / past start / time changes:** done checkpoints stay as history; pending ones are
  recomputed; any that are now in the past ring together as one immediate alarm.
- **Internal columns** added to the data model: Checkpoint.offsetMin, Checkpoint.snoozeUntil,
  Transfusion.isTest. Internal checkpoint types TEST and END are added.
- **Phase 1 test tools:** "Test alarm in 1 minute" plus "in 2 h" / "in 6 h" for the overnight
  and reboot tests. Test alarms are excluded from stats.
- **Release build** is not minified (reliability over size) at least until Phase 4.

### 2026-10-28 — Phase 1 tested on the maintainer's phone
- Locked + silent, Snooze, Missed timer, 6-hour idle and reboot tests all passed (v0.1.0).
  Other brands still to be tested by colleagues.

### Phase 2 decisions
- **Navigation:** a small back stack inside MainActivity (no navigation library).
- **Font:** Inter (SIL OFL, from Google Fonts; licence in app/src/main/assets/licenses).
  Times and big numbers use tabular figures and the slashed zero, like the card.
- **Bed colours:** 8 auto-assigned colours (lowest one not used by a running bed).
  Red is reserved for reactions and overdue states.
- **Start time:** chips Now / 5 / 10 / 15 min ago, or "Pick time…". A picked clock time
  means its most recent past occurrence (T0 is never in the future).
- **Custom duration:** 10–600 min.
- **End VS prompt:** an in-app prompt (no alarm) shown right after End. If postponed
  ("Later"), it stays on the Home card until documented.
- **Reaction screen** also opens from the alarm screen, over the lock screen (no unlock
  needed). Its DO NOW ticks live only on screen; nothing clinical is stored.
- **Overdue / ringing** checkpoints can be acknowledged from the Home card and Detail too.

### 2026-10-28 — Phase 2 tested on the maintainer's phone
- All 8 tests passed (update keeps data, start, past start, two beds in one minute,
  detail/edit, missed, End, reaction stop) — v0.2.0.

### Phase 3 decisions
- **Log & stats, CSV export and "Delete all data" were removed** at the maintainer's request:
  "Cerner is responsible for that." The app keeps no history: the reminder side only.
- **Auto-cleanup:** a finished transfusion (ended or stopped) is deleted 24 h after its end,
  unless something still needs action (ringing, a missed check not acknowledged, End VS
  not documented). Runs on every refresh (alarm, boot, app open).
- **Settings ranges:** tolerance ±1–15 min, snooze 1–2 min, missed 5–30 min, limit 1–8 h
  (15-min steps), warning 5–60 min (always shorter than the limit). Every change re-arms.
- **Alarm sound:** the system alarm-sound picker; "default" follows the phone's setting.
  If the chosen sound can't be played, the phone default and then the bundled tone are used.

### 2026-10-28 — Repositioning: plan approved
- **No Log & stats / CSV for turns either** (Cerner is the record). Detail shows the longest
  gap without a turn over the last 24 h instead.
- **v0.3.0** (published by a command that was stopped, with Log/CSV) was deleted from GitHub;
  v0.3.1 is the current release.
- **Design system first (R0):** every font size, colour, radius and spacing used by the screens
  becomes a named token with its exact current value (zero visual change, proven by Roborazzi
  baselines recorded before the refactor). The off-8dp spacings already in the transfusion
  screens (6/10/14/18) stay, as named tokens — zero visual change wins over re-spacing.
- **Engine:** one generic alarm-item core with per-type sources (transfusion, turn).
  Transfusion items: silence = miss = first ring + timeout (unchanged). Turn items: silence at
  the timeout (Overdue shown), MISSED when the next slot fires.
- **Sounds:** transfusion keeps the chosen/default alarm sound and the long vibration;
  turns get a separate bundled chime and a double-pulse vibration. One picker per type.
- **Screenshot tooling** (test-only, free, not in the APK): Robolectric + Roborazzi;
  CI moves to JDK 21.

### 2026-10-29 — Repositioning: design approved, engine wired
- **App name on the phone: "A S"** (the designer's mark). Inside the app the wording is unchanged.
  Setup steps use the app's display name so they match what staff see in phone settings.
- **Font files:** static Inter 4.1 weights (400/500/700/900) from the Inter project; identical on
  phones and in screenshot tests, keeping the slashed zero and tabular figures.
- **Engine:** turns are a second source (`TurnSource`, pure rules in `TurnRules`) on the same
  scheduler, service, notifications, snooze and re-arm path. Transfusion alarm addresses are
  unchanged, so alarms set by earlier versions keep firing after the update.
- **Database v2:** adds `turn_plan` and `turn_slot` only (automatic migration, `MigrationTest`).
- **Combined alarm:** one Snooze covers everything; the transfusion sound wins while a transfusion
  sounds; once only turns sound, the turning chime and double-pulse vibration play.
- **"Other position"** offers allowed positions except the suggestion and the current one.
- **Phones set to Arabic (v0.4.1):** the layout is left-to-right on every phone (`supportsRtl="false"`, and forced in
  `TvsTheme`). The UI language is English; mirroring it scrambled the alarm's text. Digits were already Latin.

## Ward Card (chosen 2026-10-31)
The maintainer chose direction 1, Ward Card. Colours in `ui/theme/Theme.kt`, verified from `lab/palettes.json`. Red Hat Display and Red Hat Text (OFL). The rail is the signature: the paper card's dots on a line, alive. Overdue is a red diamond with "!".

**The reaction guide belongs to transfusions.** It is on transfusion alarms, transfusion Detail, and Home while there is blood on the ward: running, or finished within the last day, since a reaction can come late. It is not on a round-only alarm, on repositioning screens, or on Home when only turning plans exist. This overrides "2 touches from anywhere" for those places.

**Flows** (docs/FLOWS.md): the Start chooser is gone. Home has Start transfusion and Start turning side by side. Beds are picked from the ward's list, entered once in Settings or Setup, and typing stays as "+ Other bed", which also adds that bed to the list. Start time = now, duration = the last one used. "Baseline done · Start" is one button. Tapping the patient's current position starts a turning plan. "All turned as suggested ✓" finishes a round. No "Are you sure?" dialogs: every acknowledge, start, end and stop can be undone for 5 seconds, and "Undo last" stays on the bed's card for 5 minutes.

**Data:** Room v3 adds `ward_bed` and `undo_action` by automatic migration. Nothing existing changes meaning. Stopped-for-reaction beds stay on Home until removed or until the daily cleanup.

**Alarm safety:** touches are ignored for the first second and while the proximity sensor is covered. The alarm stays 5 seconds after the last acknowledge, to offer Undo. Haptics: long buzz = due, three pulses = overdue, two quick taps = confirmed. At night the alarm window is dimmed.

**Language:** the layout is left-to-right on every phone (`supportsRtl="false"` and forced in the theme), the UI language stays English, digits stay Latin.

**MRN (add-on, docs/MRN_ADDON.md):** the rule stays: bed only, no MRN field. A run of 5 or more digits in a bed label counts as an MRN typed by mistake, and outside the app (notifications, the alarm over the lock screen, the widget) it shows as "•••4821": the shortest suffix, at least 4 digits, unique among active labels. The full value shows only inside the app on an unlocked phone. Nothing logs bed labels.
