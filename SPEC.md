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

7. **Log & stats**
   - Per day / week / month: % on-time, average delay, missed count.
   - The list of checkpoints.
   - CSV export through the Android share sheet.
   - "Delete all data", with a confirmation step.

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
   - **Phase 3:** Log & stats, CSV export, Settings.
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
