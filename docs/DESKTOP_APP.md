Change request: build the real app for Windows desktops (the Android app was the proof of concept)
0. Where things stand. Build on these facts.
If the code disagrees with anything here, stop and report before changing anything.
The reference and how it's built

* The reference is tag `v0.6.0` (commit 300b2c8) of `a172746534-ship-it/transfusion-vs` (public).
   * The Android app (`app/`) stops being developed and stays in the repo untouched.
   * It is the reference implementation: its rules, screens, words and tests define what the desktop app must do.
* Build:
   * AGP 9.4.1 with built-in Kotlin; Kotlin 2.4.21; KSP 2.3.12; Room 2.8.5.
   * CI uses Gradle 9.6.1 (there is no wrapper) and Java 21 (Temurin).
   * GitHub Actions is the only compiler: the maintainer's PC has no JDK or SDK.
* CI:
   * `android.yml` builds on every branch push and uploads screenshots, the APK and Room schemas (not test results).
   * A `v*` tag publishes an Android release.
   * README.md sends nurses' phones to `/releases/latest`.
   * `lab.yml` runs only on the `design-overhaul` and `custom-timer` branches.

What the reference contains, and what it means for the port

* `app/src/main/java/app/transfusionvs/schedule/`: 10 files.
   * `ArmPlanner`, `BedMask`, `CustomRules`, `Labels`, `ScheduleCalculator`, `Timeline`, `TurnGrid`, `TurnRules`, `TurnViews`, and `Urgency` (which holds `BedViews` and `NightRule`).
   * No Android imports. It does use the data entities (data/Entities.kt, `Turns.kt`, `Custom.kt`) and `data/Settings` (`CustomRules` uses `Settings.MAX_SNOOZES`). `Settings` takes an Android `Context`.
* Tests: rule tests and property tests (the property tests run ≥ 10,000 random cases each) in `app/src/test/`, using the fakes in `app/src/sharedTest/.../screenshots/` (`Fakes`, `TurnFakes`, `CustomFakes`). Their hidden dependencies:
   * `UrgencyTest` uses `alarm.PocketGuard`, `android.view.MotionEvent` and `alarm.UndoCodec` / `UndoStep`.
   * `CustomViewsTest` uses `alarm.RingSpec` / `CustomRingBuilder`.
   * `CustomRulesTest` and `CustomSchedulePropertyTest` use `Settings`.
   * `MigrationTest` uses `alarm.AlarmScheduler`.
   * `CustomDetailGuardTest` uses Robolectric's `ShadowSystemClock`.
   * `CustomEngineTest` and `UndoEngineTest` are Robolectric and construct `AlarmScheduler(context)` and `Settings(context)`; `CustomEngineTest` also asserts on `PendingIntent` / `NotificationManager`.
   * `WardColorsTest` (contrast and colour-blind proof) reads ../lab/palettes.json relative to its module.
* Instrumented tests (`app/src/androidTest/.../uicheck/`):
   * `TapBudget`: tasks 1–12
   * `UiChecks`: the docs/TEST_LOOP.md §4 thresholds
   * `Robustness`
* `alarm/`:
   * `Engine.kt` mixes the decisions (refresh, arming, ringing, snooze, missed timeout, MISSED when the next fires, undo, retention) with Android effects.
      * It reads the clock directly (`System.currentTimeMillis()`, line 69).
      * It imports `widget.BedsWidget` and `ui.screens.TurnPlanDraft`.
   * `TurnSource` and `CustomSource` follow the same pattern.
   * `RingSpec.kt` (what rings, the priority; it imports `Settings`) and `Undo.kt` are pure.
   * `AlarmActivity.kt`:
      * The Activity class: keyguard, proximity sensor, window flags, the night brightness.
      * The alarm composables. `AlarmScreen` uses `LocalContext`, `KeyguardManager` and `BedMask` masking. The rest are pure UI: `AlarmContent`, `TransfusionAlarm`, `RoundAlarm`, `CustomAlarm`, `SnoozeSlot`, `KeyNumbers`, `AlarmState`, `AlarmPrimary`, `DoneContent` and the rows.
* `ui/`:
   * Screens, components and the Ward Card theme. Icon parameters are typed `Int` (`R.drawable`), and ui/theme/Type.kt builds its `FontFamily` from `R.font`.
   * Android APIs in it:
      * `Common.kt` (TapGate) and `CustomDetailScreen.kt` use `android.os.SystemClock`.
      * The start screens use `android.text.format.DateFormat`.
      * Home and Setup start `AlarmActivity` with an `Intent` and use `SetupChecks` / `OemGuide`.
      * Settings uses `RingtoneManager` and an activity result.
   * `Night.kt` wraps `NightRule` with Android's `Configuration` and `App`. `MainActivity` and `MainViewModel` are in `ui/`.
   * Resources: `res/drawable`, `res/font` (Red Hat, OFL), and `res/raw` (`alarm_tone.wav`, `turn_chime.wav`, `custom_chime.wav`).
* Android-only, with no desktop equivalent:
   * `BedsWidget`, `PocketGuard`'s proximity part, `Haptics`
   * `setup/SetupChecks` and `OemGuide` (replaced by a desktop Setup check)
   * `AlarmService`, `Receivers`, notification actions, app shortcuts
* Data:
   * Room v4: `transfusion`, `checkpoint`, `turn_plan`, `turn_slot`, `undo_action`, `custom_timer`, `custom_slot`
   * kept for a day (`Engine.RETENTION_MS`)
   * no log, stats or CSV: Cerner is the record
   * MRN only (5–16 digits), never names or clinical values
* UI facts:
   * English, always left-to-right, Latin digits
   * Undo: a 5-second bar (`UNDO_BAR_MS`) plus "Undo last" on a card for 5 minutes (`Engine.UNDO_MS`)
   * `MAX_SNOOZES` = 3
   * the app's name is "A S"
* Settings that only make sense on a phone:
   * "Sound + vibrate / Vibrate only": Vibrate only means no sound
   * the ringtone pickers
   * "Follow the phone"
   * "Pre-alert (… short vibration)"
* The reference's own failing checks (from the root SCORECARD.md and BASELINE.md):
   * ktlint fails (its patch was never applied)
   * the last UI checks show SPACING 8–9 and MIDWORD 1 at font 2.0
* Read before writing CHANGE_PLAN: SPEC.md, docs/DESIGN_OVERHAUL.md, docs/MRN_ADDON.md, docs/TEST_LOOP.md, docs/FLOWS.md, docs/CUSTOM_TIMER.md, and the root AUDIT.md, SCORECARD.md and PROGRESS.md.
* Process references:
   * docs/TEST_LOOP.md: §2 no test-only code in the release; §4 thresholds; §5 scoring; §6 reviewers; §7 the loop and its anti-cheating rules.
   * docs/CUSTOM_TIMER.md is the last change made this way; match its rigor.
   * docs/FLOWS.md lists tasks 1–12.
   * scripts/check-design-tokens.sh is the hard-coded-style guard (it scans `app/` only).

1. What to build
The real product: a Windows desktop app for the hospital's PCs, with the reference's capabilities.

* the same three timer types
* the same rules and schedules
* the same screens, words and look, in a desktop window

2. The maintainer's decisions (asked and answered; don't reopen them)

* Windows 10 and 11 only.
* One PC per nurse. Each PC holds only that nurse's timers.
* Every PC stands alone:
   * no network, no server, no sync between PCs, no link to the phone
   * the released app opens no network connection at all
* The maintainer can install software (admin rights). Installers and updates reach the PCs on a USB stick or a shared folder: one offline file that brings everything it needs, the Java runtime included.
* When an alarm fires:
   * a large alarm window comes up over every other program, Cerner included
   * it plays its sound even if the PC's volume is low or muted
* Locked screen and sleep:
   * The app starts with Windows and keeps running in the tray.
   * While any timer is running, the PC must not go to sleep by itself (idle sleep).
   * The sound rings even while the screen is locked. Windows allows no window over the lock screen, so the alarm window is waiting when the nurse unlocks.
* The full MRN shows everywhere in the app: the PC is personal and in a staff area. No `•••4821` masking. Names and clinical values stay forbidden.
* All three timer types: transfusion, turning and custom.
   * No always-on-top mini window (no widget equivalent).
   * The tray icon and its tooltip are enough.
* Technology: Kotlin with Compose Multiplatform for Desktop. The reference's code and tests are ported, not reinvented.
* No code-signing certificate (it isn't free). An Arabic install guide covers what Windows shows.
* The same strict process as the custom timer.
* Everything free: no paid service, account, API key or certificate.
* One full Windows lab run satisfies the stop condition.
* The maintainer never tests. Never ask them to test anything. What CI can't prove is reported as Unverified, with the reason.

3. Architecture (the plan may refine it; any departure must be justified in desktop/docs/CHANGE_PLAN.md)
Where it lives

* An independent Gradle project in `desktop/`, in the same repo, on branch `desktop`.
   * The Gradle module is `desktop/` itself, so ../lab/palettes.json resolves for the ported `WardColorsTest`.
   * Kotlin JVM, or the Kotlin Multiplatform plugin with a single `jvm()` target if Room, Compose resources or Roborazzi need it.
   * Pinned:
      * Gradle 9.6.1 and Java 21 (Temurin)
      * Kotlin 2.4.21, KSP 2.3.12 and Room 2.8.5, as in the reference, so class (a) compiles unchanged
      * The Compose Multiplatform and JetBrains lifecycle versions that match, recorded in CHANGE_PLAN.
* Allowed paths: `desktop/**`, `.github/workflows/desktop-*.yml`, docs/DESKTOP_APP.md.
* Frozen, as `app/` is:
   * root `*.md` (README, SPEC, PROGRESS, SCORECARD, BASELINE, AUDIT, CHANGE_PLAN)
   * `scripts/`, `lab/`
   * the existing workflows, `.gitignore`, the root Gradle files
* Process files live in `desktop/docs/`: BASELINE, CHANGE_PLAN, PORT_DIFF, SCORECARD, PROGRESS, AUDIT, INSTALL_AR.
   * Unqualified, these names mean the `desktop/docs/` files. The root files of the same names are read-only references, always called "root" in this request.
* desktop/.gitattributes: `* text=auto eol=lf`, and binary for wav, ttf, png and ico.

The port: three classes, nothing in between
Packages stay `app.transfusionvs.*`, so test IDs match the baseline's.

* (a) Byte-identical to the v0.6.0 git blob:
   * `schedule/*.kt`
   * data/Entities.kt, `Turns.kt`, `Custom.kt`, `Ward.kt`, `Dao.kt`, `TurnDao.kt` (the DAOs are pure Room, suspend and Flow)
   * alarm/RingSpec.kt, alarm/Undo.kt
   * the three sharedTest fakes
   * these test files: `BedMaskTest`, `CustomRulesTest`, `CustomSchedulePropertyTest`, `CustomViewsTest`, `ScheduleCalculatorTest`, `SchedulePropertyTest`, `TimelineTest`, `TurnGridTest`, `TurnRulesTest`
   * A desktop `app.transfusionvs.data.Settings` provides the companion constants these files use (`MAX_SNOOZES` and every range and default, verbatim), so they compile unchanged.
* (b) Identical except hunks listed in PORT_DIFF:
   * `ui/components/*` except `Night.kt`, `ui/theme/*`
   * `ui/screens/*` except Settings and Setup
   * alarm/AlarmContent.kt: `AlarmActivity.kt`'s composables, diffed against `AlarmActivity.kt`
   * `UrgencyTest`: only `pocketGuardIgnoresTheFirstSecondAndACoveredPhone` and its two imports are removed; it is recorded as "replaced"
   * `WardColorsTest`, `ClinicalContentTest`, `CustomNameFitTest`, `CustomDetailGuardTest`: `WardColorsTest` runs from Step C, since it needs `ui/theme`
   * Allowed hunk kinds only:
      * imports
      * `R.*` references
      * the icon parameter type
      * `SystemClock` → the desktop uptime function
      * `DateFormat.is24HourFormat` → `true`
      * `Intent` / `startActivity` → a callback
      * `System.currentTimeMillis()` → `AppClock`
      * removing whole declarations that class (c) replaces (the Activity class, `AlarmScreen`)
      * Android-only calls (`LocalContext`, `SetupChecks`, `OemGuide`) → the desktop Setup check or a callback
      * `androidx.activity.compose.BackHandler` → `androidx.compose.ui.backhandler.BackHandler`, with the opt-in set in the build
      * desktop pointer and focus affordance in `ui/components` (hover cursor, focus indication)
      * in `AlarmContent.kt` only: when `bed.isTest`, the subtitle shows "If you can hear and see this, alarms work on this PC". `Labels.kt` and `RingSpec.kt` stay byte-identical.
      * in test files only: Robolectric's `@RunWith` / `@Config` / `@GraphicsMode`, `RuntimeEnvironment.setQualifiers` and `ShadowSystemClock` become a desktop JUnit rule
         * The rule applies the same width, height, density and font scale from the same qualifiers string, and controls the desktop uptime function.
         * `org.json` comes from `org.json:json`.
         * A stand-in that ignores the qualifiers is forbidden: it would silently stop testing 360 dp at font 2.0.
   * `app.transfusionvs.ui.MainViewModel` is kept: an androidx ViewModel from `org.jetbrains.androidx.lifecycle`, with every member class (b) uses, under the same names and types, plus `PRIMARY_TAG`. `collectAsStateWithLifecycle` and `LifecycleResumeEffect` come from the same JetBrains lifecycle artifacts.
   * Option, decided in CHANGE_PLAN: a desktop `R` object plus a `painterResource(Int)` shim keeps the icon signatures byte-identical.
* (c) Rewritten, with the reference as the spec:
   * `AlarmScreen`, SettingsScreen, SetupScreen, `Night.kt`
   * `MainActivity` (→ `main()`); `MainViewModel`'s Android wiring (its members stay, see class b)
   * `AppDatabase`, `Settings` storage
   * `Engine`, `TurnSource`, `CustomSource`: decisions ported line by line, effects replaced, the clock via `AppClock`
   * `CustomEngineTest` and `UndoEngineTest`: rewritten under the same class and method names, against the desktop fakes (trap 2)
* Mapped in the port map, not ported:
   * `ArabicPhoneTest` and the four screenshot classes: replaced by the desktop screenshot tests
   * `MigrationTest`: N/A, the desktop schema starts at 1
* PORT_DIFF.md holds, per file, the exact unified diff against v0.6.0.
   * CI regenerates it on ubuntu from git blobs and fails on any byte of difference.
   * The spike (section 7) copies `ui/` and the alarm composables unchanged into the desktop module and records every compile error. That list sizes class (b).

One clock

* `AppClock` is the only source of wall time for the engine, the scheduler and every composable that reads the time. The lab MSI can drive it.
* Its zone is the JVM default zone. Ported code keeps calling `ZoneId.systemDefault()` (as `Labels.clock` and the start screens do).
   * Re-detection after a Windows change: `System.clearProperty("user.timezone")` then `TimeZone.setDefault(null)`.
   * The lab sets a zone with `TimeZone.setDefault(...)`.
   * Test: after a zone change, the alarm's clock text, Home's next time and the engine's next due time agree.

Settings: the only differences from the reference

* "Sound + vibrate / Vibrate only" is removed: an alarm always makes sound. A test proves no setting can make an alarm silent.
* The ringtone pickers are removed: bundled sounds only.
   * `alarm_tone.wav` becomes the transfusion sound (on the phone it was the fallback).
   * The plan reports its length and peak level.
* "Follow the phone" → "Follow Windows".
* The pre-alert label drops "vibration".
* New "Alarm volume" row: 50–100% in steps of 10, default 80%.
* Everything else is verbatim.
* Storage: a file in the data folder, written atomically (temp + rename); a corrupt file falls back to defaults.

Data

* Room on the JVM (`BundledSQLiteDriver`), the same entities. The desktop schema is its own version 1, exported and committed. Every later change gets a migration and a migration test.
* Data folder: `%LOCALAPPDATA%\TransfusionVS\data\`, never inside the install folder.
* If the database can't be opened:
   * move the file aside, deleting it `RETENTION_MS` later
   * run on an in-memory database with a red banner and red tray state: "Timers could not be read: re-enter them"
   * never crash-loop
* A read-only data folder behaves the same way.

Windows integration (JNA, free)

* A hidden top-level window (`WS_POPUP`, `WS_EX_TOOLWINDOW`, never shown; not message-only), owned by a dedicated thread with its own message loop, receives:
   * `WM_TIMECHANGE`, `WM_SETTINGCHANGE`
   * `WM_QUERYENDSESSION` / `WM_ENDSESSION`
   * resume, via `RegisterSuspendResumeNotification`
   * session changes, via `WTSRegisterSessionNotification(NOTIFY_FOR_THIS_SESSION)`
   * Each one triggers an engine refresh.
   * `ShutdownBlockReasonCreate` is called from that thread.
* Power:
   * `PowerCreateRequest` / `PowerSetRequest`, held by an app-lifetime object.
   * SystemRequired while any timer runs.
   * DisplayRequired while an alarm is unanswered, and also while any timer runs on a Modern Standby PC.
* Core Audio: the default device's volume and mute, the app's own session volume and mute, and an opt-out from communications ducking.
* The alarm window's styles (section 4).
* The render API is fixed in CHANGE_PLAN (software unless the spike proves DirectX is needed), so an old GPU driver can't draw a blank alarm window.
* Native libraries (JNA, the bundled SQLite driver, Skiko) load from the install folder only, never unpacked to `%TEMP%`.
* At start, the app:
   * sets its working directory to the data folder (`SetCurrentDirectoryW`)
   * turns off Windows' crash dialog for itself (`WerSetFlags` / `SetErrorMode`)
   * The launcher uses a relative `-XX:ErrorFile=hs_err_%p.log`; that file is deleted on the next start.
   * The launcher also sets `-XX:-CreateCoredumpOnCrash`: on Windows Server the JVM writes a memory minidump by default, and memory may hold MRNs.

Packaging and release

* The MSI:
   * The Compose plugin builds the app image (`createDistributable`). The MSI is built by `jpackage --app-image` with a custom WiX template in `desktop/packaging/wix`, or by WiX directly.
   * WiX 3.14.x (MS-RL): JDK 21's jpackage supports it. Never WiX v6 or later, whose licence may charge.
   * Per-machine under Program Files, installed once by the maintainer as admin. A per-user install lands under `%LOCALAPPDATA%`, where AppLocker and Defender rules often block executables.
   * The MSI writes the HKLM Run value (autostart for whichever user logs on).
   * On an upgrade or uninstall, its deferred custom actions run the installed `<exe> --installer-stop` before any file changes. That writes the "installing" marker. A first install has no exe yet and no running app, so it skips this.
      * Where: `%ProgramData%\TransfusionVS\installing`. SYSTEM and Administrators write it; Users read it.
      * What it holds: the time and msiexec's PID.
      * Running apps in every session check it every tick, restore the volume, and exit. The custom action waits up to 20 s for every other `TransfusionVS.exe` in any session to exit, then ends the rest.
      * A launch exits at once while the marker is valid. It ignores a marker older than 15 minutes, or one whose PID is no longer msiexec, so a failed install never silences the app.
      * The marker is cleared at the end of the install, and by a rollback custom action.
   * On uninstall they delete every task whose name starts with `TransfusionVS `(all users).
   * A fixed upgrade UUID: a new version replaces the old one and keeps the data. Versions are the tag's numbers; dev builds never outrank a release.
* Names: "A S" is the display name. The exe, install folder, task folder, mutex and AppUserModelID use `TransfusionVS`. Icon: the reference's launcher icon as `.ico`, plus a red overdue tray variant. A Start-menu shortcut.
* Two MSIs from one commit:
   * release: no hook class and no hook loader. CI scans the jars and bytecode, per docs/TEST_LOOP.md §2.
   * lab: with the hook. It is a workflow artifact only, never attached to a release or a draft.
      * its own UpgradeCode, the product name "A S LAB" and the folder `TransfusionVS-Lab`
      * it aborts if the release product is installed
   * `desktop-release.yml` fails if any attached asset contains the hook class.

4. Desktop behaviour
Running, restarting, quitting

* Starts at Windows logon (HKLM Run) and lives in the tray.
* The tray:
   * One left click on the icon opens Home. It's a mouse listener, not the double-click action.
   * Closing the main window only hides it.
* "While any timer runs" (Quit, power requests, the shutdown reason) means: `ArmPlanner` has a next alarm, or anything is RINGING, or anything is MISSED and not yet acknowledged.
* Quit: not offered while any timer runs (the menu says "Stop all timers to quit"). After a deliberate Quit with no timers, nothing relaunches the app until the next logon or a manual start.
* Every launch states its source:
   * none: a user start from the Start menu. It opens Home.
   * `--logon`: the Run value and the logon trigger
   * `--watchdog`: the restart task
   * `--backstop`: the backstop task
* IT and installer modes (`--installer-stop`, `--setup-check`, `--test-alarm`) are handled first, before the mutex, AWT, the data folder and the working directory. They never take the mutex, never create the data folder and never open the database.
* Only a user start brings the running app to the front.
   * The other three never show, restore or activate the main window, so nothing ever steals focus from Cerner.
   * The alarm window still appears, without activation, whenever an alarm is due, whatever started the process.
* One instance: a named mutex, checked before heavy start-up.
   * A second launch pings the first through its hidden window: `SendMessageTimeout` with `SMTO_ABORTIFHUNG`, 5 s; the reply passes through both the scheduler thread and the UI thread.
   * If it answers: a user start brings it to the front (`AllowSetForegroundWindow`) and exits. The others exit within 2 s; `--backstop` also tells it to re-check due alarms.
   * If it doesn't answer and has written no heartbeat in the last 30 s of unbiased uptime (`QueryUnbiasedInterruptTime`, so sleep doesn't count): the second launch ends the old process and takes over.
   * The heartbeat is written only after a round trip through the scheduler thread and the UI thread, like the ping reply. A blocked UI thread stops it.
* Restart task: `\TransfusionVS Restart <user SID>` in the root task folder, created by the app for the current user. A standard user can't create a task folder, and task names are machine-wide. It has a logon trigger and a repeating 1-minute trigger.
* Backstop: `\TransfusionVS Backstop <user SID>`, at the next alarm's time, repeating every minute until the alarm is answered, with "wake the computer".
   * The app keeps it pointing at the next alarm.
   * It survives a crash, a hang and a reboot (with the user logged on), and doubles as the wake timer.
* Both tasks are registered from XML with:
   * `InteractiveToken`
   * `MultipleInstancesPolicy Parallel`: the mutex prevents duplicates, and a launch that became the app never blocks the task's next run
   * `DisallowStartIfOnBatteries false`, `StopIfGoingOnBatteries false`
   * `ExecutionTimeLimit PT0S`
   * `Priority 4`
   * `StartWhenAvailable true`
   * `WakeToRun true` (backstop)
   * triggers in UTC (`…Z`)
* Missed alarms: after any gap, alarms that came due ring at once, under the reference's rules.
   * Gaps: crash, hang, sleep, reboot, the PC switched off.
   * Rules: missed timeout; MISSED when the next one fires; the latest one rings.
* Shutdown and logoff: while a timer runs, the app registers the reason "Timers are still running".
   * It refuses a user-initiated `WM_QUERYENDSESSION` once per shutdown attempt, so Windows shows its "Shut down anyway" screen with that reason.
   * It always accepts a `WM_QUERYENDSESSION` carrying `ENDSESSION_CLOSEAPP` (Restart Manager, upgrades).
   * Forced and update restarts ignore the reason.
   * Nothing is registered once the last timer stops.
* No user logged on, no alarm. Windows needs a logged-on user to play sound and show windows. Setup check says so, and shows "Windows restart pending" in red and the active hours.

Timing

* The same schedules and rules as the reference, from the ported code, through `AppClock`.
* On time = rings 0–5 s after the due time, never before. A ≤ 1-second tick re-checks due times against the wall clock.
* Time zones:
   * On `WM_TIMECHANGE` / `WM_SETTINGCHANGE` the app re-detects the zone (clear `user.timezone`, `TimeZone.setDefault(null)`) and passes the new `ZoneId` to the engine.
   * The bundled runtime's time-zone data must hold Egypt's current rules (DST since 2023). The test runs inside the installed app image's runtime.
   * Setup check compares the app's UTC offset and hour:minute with Windows' (`GetLocalTime`), read in the same tick. Any difference is red.
   * Every tick, if the app's UTC offset differs from Windows' (`GetLocalTime` against `GetSystemTime`), it re-detects the zone as on `WM_TIMECHANGE`. If it still differs, the tray icon turns red.

Sleep, lock, screen

* Power requests as in section 3. Waking for an alarm holds SystemRequired + DisplayRequired until it's answered.
* Power requests stop only idle sleep. Start → Sleep, the power button and the lid still sleep the PC. Then:
   * the backstop's wake timer wakes it, where wake timers are allowed
   * otherwise missed alarms ring on wake
* Setup check shows:
   * wake timers allowed or not
   * the power-button and lid actions (red if Sleep while wake timers are off)
   * Modern Standby
   * the sleep-request policy
   * It never calls `powercfg /requests`, which needs admin.

The alarm window

* At most one window per monitor: the primary, plus the mouse's monitor if different. All show the same content; an action in one applies to all.
* Placement: a fixed width (in CHANGE_PLAN), up to the work-area height, docked to the right edge, draggable by its header. It keeps its place until answered.
   * Cerner on the left stays visible, so the nurse can document before "Measured & documented ✓".
* Styles: `TOPMOST | NOACTIVATE | APPWINDOW`, with no system menu.
   * It never takes keyboard focus: keystrokes keep going to Cerner. Mouse clicks and touch still work.
   * `WM_CLOSE`, `SC_CLOSE` and `SC_MINIMIZE` from any source are ignored.
   * Every tick: if it is cloaked (`DWMWA_CLOAKED`, e.g. after a virtual-desktop switch) or minimised, recreate it on the current virtual desktop. Re-assert `HWND_TOPMOST` with `SWP_NOACTIVATE`.
   * The taskbar button flashes until the alarm is answered.
* Kept on screen: every tick and on `WM_DISPLAYCHANGE`, each window is clamped so that its header and primary action are fully inside a work area.
* Updating while unfocused: the window is never focused and may never reach Lifecycle RESUMED. Its rows and countdown must still update.
* The input guard:
   * It ignores all input for 2 seconds: deliberately longer than the reference's 1 second, because of typing in Cerner.
   * It re-arms when:
      * an alarm item is added, or the screen switches type
      * a window is created or re-shown
      * a re-assert uncloaks it, or brings it back above a visible window of another process that overlapped its rectangle for ≥ 500 ms
         * Hidden windows, tooltips and menus don't count.
         * Re-asserts re-arm a window at most once every 10 s, so a window fighting for the top can never keep the alarm unanswerable.
      * the session is unlocked or reconnected (`WTS_SESSION_UNLOCK`, `WTS_CONSOLE_CONNECT`, `WTS_REMOTE_CONNECT`)
      * the display turns on (`GUID_CONSOLE_DISPLAY_STATE` via `RegisterPowerSettingNotification`)
      * the PC resumes
      * its rows reflow for any reason other than a click in that same window
   * The countdown and clock text don't re-arm it.
   * A press that began while guarded is ignored until released.
   * Enter, Space and Esc never trigger Done, Snooze or Stop.
* Double clicks (class c, in both windows):
   * A mouse press that Windows counts as part of a double-click (AWT `clickCount ≥ 2`, i.e. within `GetDoubleClickTime()` and the double-click rectangle) is consumed at the window root before any control sees it, except inside a text field.
   * The reference's 400 ms `TapGate` is shorter than Windows' 500 ms default. Without this, a slow double-click on the pinned primary would acknowledge the next patient, whom the button names right after the first.
* The same content, actions and rules as the reference's alarm screen:
   * transfusion > round > custom, one screen for what's due in the same minute
   * "Custom · N due"
   * the reaction guide only with a transfusion
   * Snooze rules, Done / Measured & documented, Undo
   * the round's checklist with positions and "Can't turn" reasons
   * the reference's night theme on the alarm window (`TvsTheme(night = …)` from `NightRule`)
   * The reference's screen-brightness dimming (`NIGHT_BRIGHTNESS`) is N/A in the port map: Windows has no per-window brightness, and the same monitor shows Cerner.
* Sound: the reference's three sounds, one at a time, the same priority between types.
   * Restore record, written before any change: device ID, volume, mute, app-session volume, app-session mute, time.
   * While sounding: unmute the default device, raise it to max(current, setting), and set the app's session unmuted at 100%.
   * When the sound stops: restore only what hasn't changed since we set it. Restore on the next start after a crash too.
   * If the audio stream can't open: the window shows a red line and the app retries every 5 s.
   * If the default device changes mid-alarm: reopen on the new default within 1 s.
   * No device at all: red in the window and in Setup check.
   * Setup check shows the default output (name, form factor, jack state). Headphones, HDMI without speakers or an unplugged jack is orange.
* If saving Done or Snooze fails, the window shows an error and the alarm keeps sounding.
* Missed timeout: the sound stops as in the reference. The window stays showing OVERDUE, and the tray icon is red until the alarm is answered.
* Pre-alert (if set): a tray balloon with no MRN and no task name, since Windows keeps balloons in its notification history. Nothing else depends on it.

Main window

* The reference's screens and flows:
   * Home, Start and Detail for all three types
   * the reaction guide, Settings, Setup check
   * "Undo last" on cards, Undo bars
* Layout:
   * Content is a centred column of at most 600 dp.
   * The plan states the default window size.
   * Home shows more cards only as the window gets taller; no other layout change.
   * Units are dp unless written as px.
* Supported screens and text sizes:
   * 1366 × 768 at 100–125% scaling, and 1920 × 1080 at 100–200%.
   * The minimum window fits the smallest work area; below that the content scrolls.
   * Windows "Text size" maps to font scale; test 1.0, 1.3 and 2.0.
* Physical keyboard: typing the MRN and task name works. Tab moves between fields. Enter never starts a timer on its own.
* Digits and direction: Latin digits and left-to-right whatever the Windows display language or region.
* New words:
   * New text is unavoidable: the tray menu, Quit, the shutdown reason, the Setup check rows, the volume setting, the warnings, the database banner, the test alarm's "on this PC" line.
   * CHANGE_PLAN lists every new string under "New words". Nothing else is new.
* The tray icon:
   * New tray icons go into the hidden overflow, so the install guide pins the icon for each nurse's account.
   * Setup check shows whether it is pinned only where Windows offers a documented way to read that; otherwise it shows "unknown". Focus assist / Do Not Disturb has no documented API, so it is "unknown" too.
   * No undocumented APIs anywhere. Allowed sources include `UISettings.TextScaleFactor` (text size) and `Microsoft.Update.SystemInfo.RebootRequired` (restart pending). A reading with no documented source shows "unknown".
   * The tooltip: "A S — N running · next HH:MM · N overdue", within 127 characters. No MRN and no task name: Explorer keeps tooltip text in the user's registry, beyond the one-day limit.
* Tap budgets (docs/FLOWS.md tasks 1–12): clicks count as taps. The tray click counts as the app-icon tap; the overflow click is reported but not counted. Task 1 is measured at the default size on 1920 × 1080 at 100%.

Setup check (replaces the Android permission checks)

* autostart (HKLM Run), the restart task and the backstop task present
* the power requests active while timers run
* wake timers, the power-button and lid actions, Modern Standby, the sleep-request policy
* "Windows restart pending", and the active hours
* "Other users signed in" (orange): their alarms ring silently in a disconnected session
* the audio device and output, the alarm volume
* the tray icon pinned or "unknown"
* the app's time against Windows' time
* "Test alarm in 1 minute"
* IT diagnostics in the release build (INSTALL_AR documents both):
   * `--setup-check <file>`: asks the running instance, through its hidden window, to write its current Setup check rows to the file (no MRN, no task name), and exits 0 once written. If no instance answers within 10 s, it writes "not running" and exits 2. A separate process can't read the app's power requests without admin. The smoke reads the file.
   * `--test-alarm`: asks the running instance, through its hidden window, to do exactly what "Test alarm in 1 minute" does, then exits 0.
* Home's setup warning counts every red and orange row. A PC whose default output has no speakers would otherwise ring silently with only Setup check saying so.

Privacy and data

* The full MRN in the app (decision above). Names and clinical values are never collected.
* No log files. Nothing writes an MRN or task name to disk outside the database and settings.
* Kept for a day, like the reference. No export.
* The released app opens no network socket.
* Fake MRNs and names only in code, tests, screenshots, lab logs and reports (the reference's Fakes).

Installation (no certificate)

* The Arabic install guide (desktop/docs/INSTALL_AR, shipped with the release as self-contained right-to-left HTML and as .md) covers:
   * the UAC "Unknown publisher" prompt
   * SmartScreen's "Windows protected your PC" (More info → Run anyway)
   * SmartScreen's offline "can't be reached"
   * "Open File – Security Warning" from a network share
   * what to tell IT if their antivirus holds the file
   * Windows 11 Smart App Control: how to see whether it's on (Windows Security → App & browser control). If it is on, it blocks unsigned apps with no "Run anyway": only IT, or switching it off, lets the app run.
   * pinning the tray icon for each nurse's account
   * where the data folder is and how to delete it
   * linking by tag, never `/releases/latest`
* Screenshots: from CI where they can be reproduced (setting the download mark and launching via `ShellExecute`); otherwise annotated illustrations, marked as such.

5. What must NOT change

* The Android app and every frozen path (section 3). The Android build stays green on every desktop push, and `git diff main...desktop` touches only allowed paths.
* The clinical content, word for word: the transfusion card's schedule, the VS list, the warning signs, DO NOW 1–5, the round's positions and reasons.
* The rules' results: every ported rule and property test gives the reference's results.
* No log, stats or CSV.

6. Traps (each one needs its own test or lab row, listed in the traceability table)

1. The port's hidden dependencies (section 0):
   * `Settings` in `CustomRules`, `RingSpec` and two tests
   * `PocketGuard`, `MotionEvent` and `UndoCodec` in `UrgencyTest`
   * `AlarmScheduler` in `MigrationTest`
   * `AlarmScheduler(context)` and `Settings(context)` in both engine tests
   * `ShadowSystemClock`
   * `Engine` importing `BedsWidget` and `TurnPlanDraft` and reading the clock directly
   * `HomeScreen` calling `SetupChecks.evaluate(LocalContext.current)`
   * `PRIMARY_TAG` and `MainViewModel` used by most screens
   * `UrgencyTest`'s PocketGuard cases are replaced by the desktop input-guard test and recorded as "replaced", not "passed".
2. The engine's Android effects: the reference engine tests' `PendingIntent` / `NotificationManager` assertions become one-to-one assertions on the desktop scheduler and notifier fakes.
3. Resources: `R.drawable` (as `Int` parameters) and `R.font` (a top-level `FontFamily`) need a mechanism decided in CHANGE_PLAN. The `R` shim, or Compose resources, with `Font()` being @Composable.
4. Focus stealing: an alarm that takes focus mid-typing swallows a Cerner entry. It never takes focus.
5. Window survival: foreground-lock, Show Desktop, taskbar close, virtual desktops, and a topmost full-screen window in front (Citrix/RDP-style).
6. Volume: it affects the nurse's other programs. Restore exactly, also after a crash.
7. Process death and hangs: on Android the OS held each alarm. Here the restart task, the ping-based takeover and the repeating backstop replace it.
8. Thread-owned power requests vanish with their thread: use `PowerCreateRequest`, held for the app's lifetime.
9. Install scope and hospital security: per-user installs under `%LOCALAPPDATA%` are blocked by AppLocker/Defender, and are useless if made under the maintainer's account. Native libraries unpacked to `%TEMP%` are blocked too.
10. Broadcast messages: a message-only window receives none of them, and Restart Manager reaches only top-level windows. Hence the hidden top-level window.
11. GitHub's Windows runners:
   * Hosted images are Windows Server 2022 and 2025, not 10/11.
   * The Windows Audio service is disabled and there is no audio device.
   * The display is 1024 × 768.
   * Compose exposes its content through the Java Access Bridge, not UI Automation.
   * Images are updated weekly: screenshot tests force software rendering, with a pixel tolerance fixed in CHANGE_PLAN before the first recording and never changed.
12. Time:
   * The JVM caches its zone, and the JVM's zone data can disagree with Windows'.
   * Moving the runner's clock: only inside one PowerShell step, in try/finally that restores the time (from a stopwatch offset) and restarts w32time; each move ≤ 2 h; the step lasting ≤ 10 min.
   * Real clock moves are used only for the clock-change and tzutil rows. DST and long gaps use `AppClock` in the lab MSI.
13. Tags: `v*` publishes an Android release and moves "Latest". Desktop tags are `desktop-v*`, and releases are drafts with `--latest=false` until smoke-tested.
14. Line endings: Windows runners check files out with CRLF. Hence desktop/.gitattributes; the PORT_DIFF check runs on ubuntu from git blobs.
15. Nurses are standard users. Hosted runners run as administrator, so anything that works only with admin rights passes in CI and fails on the ward. The lab runs the app with a non-admin token (section 8).
16. The reference fails some of its own checks (ktlint, spacing, a word break at font 2.0) in files the port may not change. Those count against the baseline, not against zero (section 9).

7. Process (in order; nothing skipped)
Rules for the whole run (from docs/CUSTOM_TIMER.md and docs/TEST_LOOP.md)

* Touch only the locations in CHANGE_PLAN. Add a missing one to the plan first.
* Suggestions go in the report, not the code.
* After any restart, re-read desktop/docs/PROGRESS.md and this request first.
* Before each long run, say how long it will take.
* If Actions minutes run out or jobs stop starting, stop and report.
* docs/TEST_LOOP.md §2 (no test-only code in the release), §4 thresholds and §7 anti-cheating rules apply: never delete, skip or loosen a test; a flaky test is a bug.
   * Section 8's desktop values replace TEST_LOOP §4's Android-only ones (cold start by `am start -W` on the emulator, ANRs, ATF, ±60 s alarm timing). This request sets them; it is not a threshold change under TEST_LOOP §7.

Workflows

* `desktop-reference.yml`: checks out tag `v0.6.0` and runs `gradle testDebugUnitTest` and ktlint 1.8.0, uploading `app/build/test-results`, `app/build/audit` and the ktlint report. It runs only when its own file changes or the commit message has `[baseline]`.
* `desktop-spike.yml`: runs only with `[spike]`.
* `desktop-build.yml`, on every `desktop` push: ubuntu + windows-2025; compile, JVM and UI tests, PORT_DIFF, the desktop token guard, both MSIs.
* `desktop-lab.yml`: runs only with `[win-lab]`.
* `desktop-release.yml`:
   * On an `-rc` tag: it builds nothing.
      * It takes the release-MSI artifact of the final full lab run (run ID in SCORECARD), checks that the tagged commit differs from that run's commit only in `desktop/docs/**`, and checks the SHA-256 against that run's log.
      * It attaches that MSI to a draft (`--latest=false`) and runs the smoke job.
      * When the smoke passes, it attaches `SMOKE_OK.txt` (the smoke run ID and the SHA-256). A draft is "smoked" only with that file.
   * Every full lab run uploads its release MSI as an artifact (90-day retention) and logs its SHA-256.
   * On a final `desktop-v*` tag (no `-rc`): it builds nothing. It copies the assets of the newest smoked `-rc` draft, checks their SHA-256, and publishes with `gh release edit <tag> --draft=false --latest=false`.
* CHANGE_PLAN records each workflow's measured duration.

Steps

1. Branch `desktop` from main's head (6495106), so the final fast-forward merge works. Save this request word for word as docs/DESKTOP_APP.md.
2. Baseline: run `desktop-reference.yml`. BASELINE.md lists:
   * every test class, method and result, with the run ID
   * the reference's ktlint count
   * the UI-check counts from the root SCORECARD.md's last run
   * The audit set (the visual reference) stays that run's artifact.
3. CHANGE_PLAN.md before any code:
   * the three port classes, file by file
   * the plugin and version set; the resource mechanism; the render API; the alarm window's width; the default window size
   * "New words"
   * the port map: every reference JVM test, every `uicheck` instrumented test, every audit screenshot and every reliability case (lab A01–A23, A11b, M01) mapped to a desktop test or Windows-lab row, or "N/A" with the reason
      * for example: M01's masking and the widget tests are N/A by decision or because they're Android-only; `MigrationTest` is N/A because the schema starts at 1; `BedMaskTest` is byte-copied and simply passes
   * new JVM engine tests for the transfusion and round decisions, using the scenarios of A07, A12–A14, A20 and A21
   * a traceability table: every section 4 bullet and section 6 trap mapped to a named test or lab row, or Unverified with the runner limitation
4. Step 0, the spike (after CHANGE_PLAN, before Step A), in `desktop/spike/`, on both Windows images. It proves:
   * The port: `ui/` and the alarm composables copied unchanged, with every compile error recorded.
   * Install, upgrade and uninstall: a silent per-machine install from the custom WiX template; an upgrade over a running app with a timer (exit code 0, not 3010); an uninstall while the app runs, leaving no task, no Run value and no process.
   * The alarm window: topmost, non-activating, over a topmost borderless full-screen window. Clicks register; it survives Win+D, Win+M, `WM_CLOSE` and a virtual-desktop switch.
   * Windows messages: each one arrives at the hidden window while the main window is hidden (Set-Date, tzutil, a test-sent `WM_QUERYENDSESSION`, an MSI upgrade).
   * The shutdown reason: `WM_QUERYENDSESSION` returns FALSE and `ShutdownBlockReasonQuery` returns the reason. Whether Windows displays it is Unverified, under "logoff and reboot".
   * As a standard user (non-admin token): creating the tasks, Core Audio and the power requests all work.
   * A failed or cancelled install leaves no valid marker behind.
   * Power and tasks: the power requests; the task XML read back with `schtasks /query /xml`; restart of a killed process; the ping-based takeover.
   * Audio: Core Audio set and restore on a free, signed virtual audio device (named with its licence). If none installs on hosted runners, the audio rows are proved against a fake Core Audio endpoint in JVM tests and listed Unverified on runners, with the spike log as the reason. If fewer than two endpoints install, the same applies to the default-device-change row alone ("one audio endpoint"). This is the only allowed downgrade of a MUST-PASS row.
   * Native libraries loading from the install folder.
   * Results go into CHANGE_PLAN. A "no" changes the architecture before Step A. The spike is deleted before the final full run.
5. Step A: the project skeleton, class (a) and its tests. All pass with BASELINE's results; PORT_DIFF is green.
6. Step B: data, `AppClock` and the engine, with the ported and new engine tests.
7. Step C: the UI (class b and c), screenshot tests, and a desktop copy of the token guard.
8. Step D: the Windows integrations, Setup check, packaging and INSTALL_AR.
9. Step E: the Windows lab, then the reviewers and personas.
10. The loop:
   * The fast loop runs on `windows-2025`. The full run is `windows-2022` + `windows-2025` (pinned, no ARM), every 3rd iteration and for the final run.
   * An iteration is: fix, re-run the whole suite, update SCORECARD and PROGRESS, commit.
   * SCORECARD lists every row of the traceability table and port map as PASS / FAIL / N/A / Unverified, with its evidence (run ID and log line or file) and which MSI it ran on. Not run = FAIL.
   * Progress means the PASS count rises or the open Critical/Major findings fall.
11. Keep going without waiting for the maintainer.
   * Report in Egyptian Arabic, short. Never "please test".
   * "انجز": stop and report.
   * "انجز وانشر": merge and publish only if every MUST-PASS row passed on the last full run, and still through all of section 9's "Then" steps. Otherwise merge nothing and report.

8. Proof
Ported rules
The reference's rule and property tests (≥ 10,000 cases each) pass on desktop, with BASELINE's names and results, except those recorded as "replaced".
Desktop JVM and UI tests

* The engine: the ported scenarios (start, edit, stop, undo, snooze, the missed timeout, the id collision across the three types, retention) and the new transfusion and round scenarios.
* The scheduler, through `AppClock`: on time (0–5 s, never early); clock and time-zone jumps; DST; gaps.
* Settings: no setting can silence an alarm. The test sets every setting to every allowed value, with the device at 0% and muted.
* Compose Desktop UI tests:
   * tap budgets 1–12, in clicks
   * the input guard: 2 s; re-arming rules; a press begun while guarded; Enter/Space/Esc never answering
   * Latin digits under `Set-Culture ar-EG`
   * Undo restoring the exact previous state
   * a double click acting once: two `SendInput` clicks on the alarm's primary with two transfusion beds due, 100 ms apart and `GetDoubleClickTime()` − 20 ms apart. Each time exactly one bed is acknowledged. The same on Home's pinned primary.
* docs/TEST_LOOP.md §4 thresholds on desktop, at every supported size, scaling and font scale:
   * targets ≥ 56 dp (alarm ≥ 64 dp), ≥ 8 dp gaps
   * body text ≥ 16 sp; key numbers ≥ 24/40 sp
   * contrast and colour-blind ΔE as written
   * one primary action in the bottom third; zero truncation
   * cold start: median of 10 launches of the installed app to its first frame, ≤ 3 s on windows-2025
   * monkey: 20,000 random hook clicks and keys, 0 crashes
   * lint: ktlint/detekt with 0 errors on class (c) and new code. Class (a)/(b) files are excluded, and their counts are reported.
   * A threshold the reference itself fails (BASELINE: SPACING, MIDWORD, ktlint) blocks only if the desktop result is worse.
   * The Accessibility Test Framework is N/A on desktop. Screen-reader support (Java Access Bridge) is Unverified.
* Screenshot references:
   * every screen and state of the reference's audit set, day and night
   * software rendering, the fixed tolerance, verified on `windows-2025` only
   * committed once the design reviewer scores ≥ 9/10 with no Critical or Major

Windows lab

* Two MSIs:
   * Rows about the shipped binary (DLLs, sockets, modules, single instance, upgrade, uninstall) run on the release MSI.
   * Rows that need the hook run on the lab MSI.
* How the lab drives the app:
   * As a standard user: the installed app runs with a non-admin token, via `runas /trustlevel:0x20000` on the runner's own logged-on account (never a second local user: its tasks would have no session), for every row except install, upgrade and uninstall.
      * Instances that the tasks start run under the session token; the row records that.
      * A MUST-PASS row must still pass. Only its standard-user aspect may be Unverified, with this reason.
   * The hook talks to the lab only through files and window messages, never a socket.
   * Release-MSI rows that need timers (the upgrade row, among others):
      1. Create the timers in a lab-MSI session; it shares the data folder.
      2. Uninstall the lab MSI. The data folder stays.
      3. Install the release MSI.
      * The "Test alarm" button itself is proven on the lab MSI; the smoke uses `--test-alarm`.
   * Real `SendInput` is used for the input rows.
   * UI Automation only for window-level facts.

MUST PASS (never N/A, except the audio downgrade in step 4):

* the alarm on time (0–5 s) for each type
* the window above a topmost borderless full-screen window; clicks register
* text typed into a foreground Notepad while the alarm appears arrives complete
* No focus stealing by the safety tasks: for 5 minutes, with timers running and both tasks firing, `GetForegroundWindow` is sampled every 100 ms while `SendInput` types into Notepad. The foreground never changes and the text arrives complete.
* The input guard re-arms on every trigger in section 4: for each one, a `SendInput` click 100 ms after the trigger is ignored, and one at 2.1 s works. Unlock, display-on and resume are faked by the hook; the real events are Unverified.
* Fight for the top: a visible topmost window overlapping the alarm re-asserts `HWND_TOPMOST` every 200 ms for 60 s. A `SendInput` click on Done, made while `WindowFromPoint` is the alarm, works within 15 s.
* Kept on screen: move the window to x = −2000 px with `SetWindowPos`, then change the resolution in the same second. The window is back in view within 1 s.
* Scaling:
   * a JVM test of the placement and clamp function at 1.0, 1.25, 1.5 and 2.0, using the 1366 × 768 @ 125% and 1920 × 1080 @ 200% work areas
   * the placement and kept-on-screen rows re-run on the lab MSI with `-Dsun.java2d.uiScale=1.25` and `2.0`
* after posting `SC_CLOSE`, Esc and `WM_CLOSE` to the alarm window, and sending Win+D and Win+M with `SendInput`: it is visible, not minimised, topmost and not cloaked, and `GetAncestor(WindowFromPoint(centre), GA_ROOT)` is the alarm window
* the volume and mute raised and restored, including after a crash mid-alarm
* restart and takeover:
   * killed 30 s before an alarm: it rings no more than 75 s late
   * process suspended: no more than 90 s late
   * UI thread blocked 30 s before an alarm: no more than 90 s late
* each task's XML matches section 4 field by field, with the SID-named tasks created by a standard user
* A failed install doesn't silence the app: kill msiexec right after `--installer-stop`, and separately force a failing install. Within 2 minutes the app runs, the next alarm rings, and Setup check is clean.
* clock change and `tzutil` change on the real clock; DST night and long gaps on `AppClock`
* the missed timeout, and MISSED when the next one fires
* the same-minute order
* a second instance focuses the first
* the power requests as specified
* the backstop pointing at the next alarm and repeating until answered
* the shutdown reason present while a timer runs (`ShutdownBlockReasonQuery`) and gone after; `ENDSESSION_CLOSEAPP` accepted
* the corrupt-database and read-only-data-folder paths: in-memory DB, red banner and tray, alarms set in the session ring, no crash
* the runtime pieces:
   * Java Sound, the AWT tray, JNA, SQLite and Skiko are each driven once and show their DLLs in the loaded-module list.
   * `jdk.localedata` is proven by `ModuleLayer.boot().findModule("jdk.localedata")` plus an ar-EG `DecimalFormatSymbols` read.
   * tzdb is proven by `ZoneRulesProvider.getVersions("Africa/Cairo")` reporting the bundled version.
   * The hook logs both.
* no loaded module under a user-writable path: every path is in the install folder or under `%SystemRoot%`, except those listed in CHANGE_PLAN with their owner
* no network socket: sampled every second for the whole lab run, plus a logging firewall block rule with zero hits (release MSI)
* MRN and task name absent from every file the app writes outside its database and settings, and from a `reg export` of HKCU after the run, searched as text and after decoding every REG_BINARY value as UTF-16LE and as ASCII, each with and without ROT13 (Explorer's tray streams are stored that way)
* Upgrade: the lab builds a 1.0.1 MSI from the same commit (same upgrade UUID) and upgrades a running 1.0.0 with one timer of each type. All keep running, the next alarm fires, and the exit code is 0 (no reboot).
* Clean uninstall: no Run value, no app task for any user, no process. The data folder stays, as documented.
* every section 4 behaviour in the traceability table without its own row above: taskbar flashing, the red tray icon, Quit hidden while timers run, the balloon without MRN, the tooltip ≤ 127 characters, Restart Manager exit, the crash file deleted, each Setup check row

Unverified allowed only for:

* real sleep and wake, the lock screen, logoff and reboot, display power
* multiple monitors, Modern Standby, Citrix/RDP, real speakers
* screen readers (Java Access Bridge), touch input, Windows 10/11 client editions
* real Windows display scaling above 100%: runners run at 100% and can't change it without a logoff
* Each names the runner limitation. The report states that the runners are Windows Server.

Setup check rows: a row passes when each of its states renders correctly from an OS reading faked by the lab hook, with the runner's real reading logged. A real state the runner can't produce (other users signed in, restart pending, Modern Standby, the lid action) is Unverified.
"Setup check is clean" (used by the failed-install row and the smoke):

* No row is red.
* A row that is red only because of a runner limitation listed in CHANGE_PLAN (no audio device, restart pending) is logged with the runner's reading and counted Unverified.
* Orange and "unknown" rows are allowed and logged.

Fresh independent reviewers (docs/TEST_LOOP.md §6, new instances every round)

* A design reviewer.
   * It gets each screen at the default size, day and night, plus the extremes (minimum size, 200%, the longest name, 20 patients), each beside the reference's audit shot.
* Three personas walking tasks 1–12 on the desktop:
   * a nurse at a PC between Cerner entries
   * a night shift
   * a nurse who uses only the keyboard and mouse
* A scope audit of `git diff main...desktop` against CHANGE_PLAN: every changed line maps to a plan item; nothing outside the allowed paths changed; PORT_DIFF matches.
* Every reviewer, design and personas alike, is told before it starts:
   * "New words" and the list of what this request requires are not defects.
   * A finding that applies equally to the reference's audit shot, or whose fix needs any class (a) change or a class (b) change outside the allowed hunk kinds, is a suggestion.
   * Suggestions go in AUDIT.md and the final report. They never lower the design score and never count as a persona finding.

9. Stop condition

* After the final full run, commits may change only `desktop/docs/**`. Any other change needs a new full run.
* Merge only when all of these are true, on one full Windows lab run:
   * every ported rule and engine test gives the reference's results
   * every MUST-PASS row passes; every other row passes or is Unverified with its reason
   * the design reviewer gives ≥ 9/10 with no open Critical or Major
   * the personas have no concrete finding on tasks 1–12 that this request doesn't itself require and that isn't a suggestion under section 8
   * the Android build is green and nothing outside the allowed paths changed
   * the scope audit finds zero unplanned changes
* Then:
   1. Tag the `desktop` head `desktop-v1.0.0-rc.N`. The release workflow attaches the final full run's release MSI to a draft (`--latest=false`).
   2. Smoke on that draft's MSI on a Windows runner:
      * silent install, the app running
      * `--setup-check`: Setup check is clean (section 8)
      * after `--test-alarm`, a topmost window owned by `TransfusionVS.exe` appears 55–65 s later (`EnumWindows`)
      * a clean uninstall
   3. Fast-forward merge, and tag `desktop-v1.0.0` on the same commit. `desktop-release.yml` publishes the smoked `-rc` assets (same SHA-256) with INSTALL_AR, `--latest=false`, and builds nothing.
   4. `gh api …/releases/latest` must still return the Android tag. Otherwise, set the release back to a draft and report.
   * If the smoke fails, nothing is merged and nothing is published.
* If 3 iterations pass without progress: do NOT merge. Report exactly what blocks it.
* If the maintainer says "انجز": stop and report what isn't met. Merge only on an explicit go-ahead.

10. Final report (Egyptian Arabic, short)

* what the desktop app does
* how to install it from the USB stick (INSTALL_AR)
* what was proven and how
* what is Unverified on GitHub's Windows Server runners, and why
* the remaining risks:
   * it is heard only near this PC (the phone went with the nurse)
   * sleeping it by hand rings only if wake timers work
   * "Switch user" silences the other user's alarms
   * Citrix/RDP full screen is unverified
   * after a power cut or an automatic update restart, nothing rings until the nurse signs in again
   * a PC whose default output has no speakers rings silently; Setup check and Home show it in orange
* every decision taken on the maintainer's behalf (name, install scope, the 2-second guard, the alarm window's place, the volume default, the Settings delta, balloons without MRN, the render API)
