# CHANGE_PLAN: the Windows desktop app

The request is docs/DESKTOP_APP.md (word for word); "§n" below means its section n. The reference is
tag `v0.6.0` (300b2c8). BASELINE.md is reference run 42461704759. Written before any code (§7 step 3).
Only the locations in §1 of this plan are touched; a missing one is added here first, then changed.

Status of each decision: **fixed** (decided now, changed only by a spike "no", recorded in §12), or
**spike** (the spike measures it and §12 records the result before Step A).

## 1. Locations (every path this change touches)

Allowed paths only (§3): `desktop/**`, `.github/workflows/desktop-*.yml`, `docs/DESKTOP_APP.md`.
Nothing else in the repo changes: `app/`, root `*.md`, `scripts/`, `lab/`, the existing workflows,
`.gitignore` and the root Gradle files stay as they are.

| Path | What |
|---|---|
| `docs/DESKTOP_APP.md` | the request, word for word (done, 945b8de) |
| `.github/workflows/desktop-reference.yml` | the baseline (done, run 42461704759) |
| `.github/workflows/desktop-spike.yml` | Step 0, only with `[spike]`; deleted with `desktop/spike/` before the final full run |
| `.github/workflows/desktop-build.yml` | every `desktop` push: ubuntu + windows-2025 |
| `.github/workflows/desktop-lab.yml` | only with `[win-lab]` |
| `.github/workflows/desktop-release.yml` | `desktop-v*` tags only |
| `desktop/.gitattributes` | `* text=auto eol=lf`; `*.wav *.ttf *.png *.ico binary` |
| `desktop/settings.gradle.kts`, `build.gradle.kts`, `gradle.properties` | the independent Gradle project; the module is `desktop/` itself |
| `desktop/schemas/app.transfusionvs.data.AppDatabase/1.json` | Room's exported desktop schema v1 |
| `desktop/src/main/java/app/transfusionvs/**` | the port, classes (a), (b), (c) in §4, same relative paths as `app/src/main/java/app/transfusionvs/**` |
| `desktop/src/main/java/app/transfusionvs/desktop/**` | new desktop code (class c): clock, scheduler loop, windows, tray, Win32 bindings, Setup check |
| `desktop/src/main/resources/{drawable,font,raw}/` | byte copies of the reference's `res/drawable/ic_*.xml`, `res/font/*.ttf`, `res/raw/*.wav` (checked by PORT_DIFF) |
| `desktop/src/main/resources/icons/` | `tvs.ico`, `tvs_overdue.ico`, tray PNGs, made from `ic_launcher_foreground.xml` |
| `desktop/src/lab/java/app/transfusionvs/lab/**` | the lab hook (lab MSI only) |
| `desktop/src/test/java/app/transfusionvs/**` | ported and new JVM / Compose Desktop tests |
| `desktop/src/sharedTest/java/app/transfusionvs/screenshots/{Fakes,TurnFakes,CustomFakes}.kt` | byte copies |
| `desktop/src/test/screenshots/` | desktop screenshot references (committed after the design reviewer's ≥ 9/10) |
| `desktop/packaging/wix/*.wxs`, `desktop/packaging/*.ps1` | the MSIs (WiX 3.14 directly) |
| `desktop/scripts/port-diff.sh`, `check-port.sh`, `check-design-tokens.sh`, `scan-release.sh` | PORT_DIFF, the token guard (desktop copy), the release scan |
| `desktop/lab/**` | the Windows lab (PowerShell + Python standard library) |
| `desktop/spike/**` | Step 0 only; deleted before the final full run |
| `desktop/docs/*` | BASELINE, CHANGE_PLAN, PORT_DIFF, SCORECARD, PROGRESS, AUDIT, INSTALL_AR (`.md` and `.html`), reviewer rounds |

## 2. Plugin and version set (fixed; verified on Maven Central / Google Maven on 2026-11-08)

| Piece | Version | Why |
|---|---|---|
| Gradle | 9.6.1 (no wrapper, as `android.yml`) | §3 |
| JDK | Temurin 21 (also the bundled runtime, via jlink) | §3 |
| Kotlin JVM plugin, Compose compiler plugin | 2.4.21 | §3: class (a) compiles unchanged |
| KSP | 2.3.12 | §3 |
| Room (`androidx.room` plugin, `room-runtime`, `room-compiler`) | 2.8.5 | §3; the JVM variant is published |
| `androidx.sqlite:sqlite-bundled` | 2.6.2 | what Room 2.8.5 depends on (`androidx.sqlite:sqlite:2.6.2`) |
| Compose Multiplatform (`org.jetbrains.compose`) | 1.12.2 | latest stable; brings `ui-backhandler` 1.12.2 and Material3 1.9.0 |
| JetBrains lifecycle (`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`) | 2.11.0 | the same number as the reference's androidx lifecycle 2.11.0; CMP 1.12.2's own `ui` already pulls viewmodel-compose 2.11.0 |
| `kotlinx-coroutines-swing` | 1.11.0 | `Dispatchers.Main` on the desktop (`MainViewModel` uses it) |
| JNA, `jna-platform` | 5.19.1 (Apache-2.0) | Win32/COM calls, free |
| `org.json:json` | 20260814 | `WardColorsTest` reads `lab/palettes.json` with `org.json` (§3) |
| JUnit | 4.13.2 | as the reference |
| ktlint (CLI) | 1.8.0 | as the reference's lab |
| WiX Toolset | 3.14.x (MS-RL) | §3; never v6 |

**Kotlin JVM, not Multiplatform** (fixed): Room's JVM variant, KSP and the Room Gradle plugin work on a
Kotlin JVM module, and no Compose resources are used (§3.1). If the spike shows Room's plugin refusing a JVM
module, the module switches to the Multiplatform plugin with one `jvm()` target, recorded in §12.

## 3. Decisions

### 3.1 Resources: an `R` shim (fixed)
- `desktop/src/main/java/app/transfusionvs/R.kt`: `object R { object drawable { const val ic_back = 1 … }; object font { … }; object raw { … } }`, one `Int` per reference resource, same names. `import app.transfusionvs.R` stays byte-identical in every class (b) file, and icon parameters stay `Int` (no "icon parameter type" hunk).
- `app.transfusionvs.res.painterResource(id: Int): Painter` (@Composable) loads `drawable/<name>.xml` (the reference's Android vector XML, byte-copied) once and caches the `ImageVector`. `app.transfusionvs.res.Font(id: Int, weight, style): Font` (not @Composable) reads `font/<name>.ttf` from the classpath into `androidx.compose.ui.text.platform.Font(identity, data, weight, style)`, so `Type.kt`'s top-level `FontFamily` stays top-level (trap 3).
- Class (b) files change only their import line: `androidx.compose.ui.res.painterResource` → `app.transfusionvs.res.painterResource`, and in `Type.kt` `androidx.compose.ui.text.font.Font` → `app.transfusionvs.res.Font`.
- Why not Compose resources: its `Font()` is @Composable, which would turn `Type.kt`'s top-level families into composables (a non-allowed hunk).
- The vector XML parser: Compose Desktop's `loadXmlImageVector`, or, if the spike finds it missing or deprecated-for-removal in 1.12.2, a 60-line parser of the reference's own subset (`vector`, `path`, `group`, the attributes those files use), with a test that every `ic_*.xml` parses.

### 3.2 Render API (spike, default fixed)
`-Dskiko.renderApi=SOFTWARE_FAST` in the launcher. Software rendering never depends on the GPU driver, so an
old driver can't draw a blank alarm window. The spike draws the alarm window both ways over a topmost
full-screen window; DIRECT3D is chosen only if the spike proves software fails (§12 records it).

### 3.3 The alarm window (fixed)
- **Width 412 dp** (the reference's own `w412` design width), height = the work area's height, docked to the right edge of the work area. Draggable by its header; once dragged it keeps that place until answered; a new alarm docks again.
- At 1920 × 1080 @ 100% that leaves 1508 dp for Cerner; at 1366 × 768 @ 125%, 681 dp.
- Monitors: the primary, plus the mouse's monitor if different (§4).
- Content: the reference's `AlarmContent` (class b) in `TvsTheme(night = NightRule…)`, inside a desktop root (class c) that adds the header drag, the input guard and the double-click filter.

### 3.4 The main window (fixed)
- **Default client size 600 × 860 dp**, centred in the primary work area and clamped to it (1366 × 768 @ 125% gives about 600 × 545 dp).
- **Minimum client size 400 × 520 dp**: it fits the smallest supported work area (1366 × 768 @ 125%: about 1093 × 576 dp with a 48 px taskbar, minus the title bar). Below that the content scrolls (the reference's screens already scroll).
- Content is a centred column of at most 600 dp (`widthIn(max = 600.dp)` in the desktop root, class c). Home shows more cards only as the window gets taller.
- Title "A S". Closing it hides it (§4).

### 3.5 Input (fixed)
- **Input guard** (alarm window): 2 s; re-arm triggers exactly as §4; at most one re-assert re-arm per window per 10 s; the countdown and clock text never re-arm it. Pure state machine `desktop/InputGuard.kt` (JVM-tested) plus a root `pointerInput` on `PointerEventPass.Initial` that consumes every pointer event while guarded, and keeps consuming a press that began while guarded until its release.
- **Double clicks**, both windows: the same root handler consumes a press whose AWT `MouseEvent.clickCount ≥ 2` before any control sees it, unless the AWT cursor at that moment is `TEXT_CURSOR` (Compose sets it over text fields), so double-click word selection in the MRN and task-name fields keeps working without a class (b) change.
- **Keys**: the alarm window never has focus, so Enter, Space and Esc never reach it. In the main window, Enter inside a text field never acts (the reference's fields have no `keyboardActions` submit; ImeAction Next moves focus, Done clears it), Tab moves between fields, and Enter or Space activate only the control that has the visible keyboard focus. Esc on the main window goes Back (as Android's back), never Done/Snooze/Stop.

### 3.6 Clock, zone, locale (fixed)
- `desktop/AppClock.kt`: `AppClock.now()` = `System.currentTimeMillis()` unless the lab hook (lab MSI only) or a test sets its source. The only wall-time source for the engine, the scheduler, and every composable (class (b) hunks `System.currentTimeMillis()` → `AppClock.now()`).
- `desktop/Uptime.kt`: `Uptime.millis()` = `System.nanoTime() / 1_000_000` unless a test rule sets it; replaces `SystemClock.uptimeMillis()` in class (b).
- Zone: the JVM default. On `WM_TIMECHANGE` / `WM_SETTINGCHANGE` and on any per-tick offset mismatch (`GetLocalTime` against `GetSystemTime`): `System.clearProperty("user.timezone"); TimeZone.setDefault(null)`, then `engine.rearm()`.
- Locale: `main()` sets `Locale.setDefault(Locale.US)` first, so `String.format` can't produce Arabic-Indic digits under an ar-EG region (Java's CLDR data gives `ar-EG` the `arab` numbering system). The layout is left-to-right as the reference's `TvsTheme` forces it.

### 3.7 Settings (fixed; §3 "Settings")
- `data/Settings.kt` (class c): `Settings(file: Path)`, a Java `Properties` file in the data folder, written to a temp file then `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`; an unreadable or corrupt file gives the defaults. The companion object is **verbatim** (`HOUR_RANGE`, `MAX_SNOOZES = 3`, `TOLERANCE_RANGE`, `SNOOZE_RANGE`, `MISSED_RANGE`, `LIMIT_RANGE`, `WARN_RANGE`, `PRE_ALERT_CHOICES`, `TURN_INTERVALS`, `TURN_WINDOW_RANGE`), and every getter's default and clamping is the reference's.
- Removed: `soundOn`, `alarmSoundUri`, `turnSoundUri` (and their rows). The engine passes `RingSpec.build(soundOn = true, soundUri = null, turnSoundUri = null, …)`: `RingSpec.kt` stays byte-identical.
- New: `alarmVolumePct`, 50..100 step 10, default 80, added to the companion as `ALARM_VOLUME_RANGE`, `ALARM_VOLUME_STEP`, `ALARM_VOLUME_DEFAULT` (new constants only).
- Settings screen deltas, nothing else: the "Sound + vibrate / Vibrate only" row and its night-mode warning go; both sound pickers go; "Follow the phone" → "Follow Windows"; "Pre-alert (gentle notification + short vibration)" → "Pre-alert (gentle notification)"; a new "Alarm volume" row in the Alarm panel.

### 3.8 Sounds (fixed)
| Type | File | Length | Peak | Note |
|---|---|---|---|---|
| Transfusion | `alarm_tone.wav` | 1.320 s | −3.31 dBFS (22380/32767), RMS −10.58 dBFS | 22 050 Hz mono 16-bit; was the phone's fallback, now the transfusion sound (§3) |
| Round | `turn_chime.wav` | 2.090 s | −6.27 dBFS | unchanged |
| Custom | `custom_chime.wav` | 1.670 s | −0.21 dBFS | unchanged |

Looped with Java Sound (`Clip.loop(LOOP_CONTINUOUSLY)`), one at a time, priority transfusion > round > custom
from `RingSpec` (the reference's `AlarmService` choice). Volume handling is §4 "Sound": Core Audio (§3.11).

### 3.9 Data (fixed)
- Room on the JVM with `BundledSQLiteDriver`, the reference's seven entities and the byte-identical DAOs. `AppDatabase` (class c) is desktop **version 1**, `exportSchema = true`, schema committed. Later changes: a migration and a migration test each.
- Folder `%LOCALAPPDATA%\TransfusionVS\data\`: `tvs.db` (+ `-wal`, `-shm`), `settings.properties`, `volume-restore.properties`, `heartbeat`. Never inside the install folder.
- Can't open (corrupt, locked, read-only folder): move `tvs.db*` aside as `tvs.db.unreadable-<epoch>` (deleted `RETENTION_MS` later by the next start), run on `Room.inMemoryDatabaseBuilder`, show the red banner "Timers could not be read: re-enter them" on Home and the alarm window, red tray icon. A read-only folder: same path, nothing moved. No crash, no loop.

### 3.10 Windows integration (fixed; JNA 5.19.1)
- **Hidden window** `TransfusionVS.Hidden` (`WS_POPUP`, `WS_EX_TOOLWINDOW`, never shown, a real top-level window) on its own thread with `GetMessage` loop. Receives `WM_TIMECHANGE`, `WM_SETTINGCHANGE`, `WM_QUERYENDSESSION`/`WM_ENDSESSION`, `WM_POWERBROADCAST` (via `RegisterSuspendResumeNotification` and `RegisterPowerSettingNotification(GUID_CONSOLE_DISPLAY_STATE)`), `WM_WTSSESSION_CHANGE` (`WTSRegisterSessionNotification(NOTIFY_FOR_THIS_SESSION)`), `WM_DISPLAYCHANGE`, `WM_COPYDATA` (the IT modes) and `WM_APP+1` (ping). Each OS message triggers `engine.rearm()` on the scheduler thread. `ShutdownBlockReasonCreate/Destroy` are called from this thread.
- **Power** `desktop/PowerRequests.kt`: one `PowerCreateRequest(POWER_REQUEST_CONTEXT_SIMPLE_STRING "Timers are still running")` for the app's lifetime; `PowerSetRequest/PowerClearRequest` SystemRequired while any timer runs; DisplayRequired while an alarm is unanswered, and while any timer runs on a Modern Standby PC (`SYSTEM_POWER_CAPABILITIES.AoAc`).
- **Core Audio** (`desktop/win/CoreAudio.kt`, COM vtables via JNA): `IMMDeviceEnumerator` default render endpoint, `IAudioEndpointVolume` (master scalar, mute), `IAudioSessionManager2` → our process's `ISimpleAudioVolume`, `IAudioSessionControl2::SetDuckingPreference(TRUE)`, `PKEY_AudioEndpoint_FormFactor`, `PKEY_Device_FriendlyName`, `IKsJackDescription` for jack state where the driver exposes it (else "unknown"). Default-device changes are polled every 500 ms (within 1 s, §4).
- **Theme and text size**: WinRT `Windows.UI.ViewManagement.UISettings` (`RoActivateInstance`, documented): `IUISettings3::GetColorValue(Foreground)` for "Follow Windows" (Microsoft's documented dark-mode test) and `IUISettings2::TextScaleFactor` → Compose font scale.
- **Other readings** (documented sources only; anything else shows "unknown"): wake timers, power-button and lid actions and the sleep-request policy from `PowerGetActiveScheme` + `PowerReadACValueIndex/DCValueIndex` (sub-groups `SUB_SLEEP`, `SUB_BUTTONS`, settings `RTCWAKE`, `PBUTTONACTION`, `LIDACTION`, "Allow System Required Policy") and the documented policy keys under `HKLM\SOFTWARE\Policies\Microsoft\Power\PowerSettings`; restart pending from `Microsoft.Update.SystemInfo.RebootRequired` (WUAPI COM, IDispatch via jna-platform); active hours from the documented Windows Update registry values (policy first), else "unknown"; other users from `WTSEnumerateSessions`; time from `GetLocalTime`/`GetSystemTime`; the tray icon's pinned state and Focus assist: "unknown" (no documented API).
- **Native libraries** from the install folder only: `-Djna.nounpack=true -Djna.boot.library.path=$APPDIR`, `-Dandroidx.sqlite.driver.bundled.path=$APPDIR` (the driver reads this property; `sqliteJni.dll` is copied out of the jar into the app image at build time), `-Dskiko.library.path=$APPDIR`.
- **At start**: `SetCurrentDirectoryW(data folder)`, `SetErrorMode(SEM_FAILCRITICALERRORS | SEM_NOGPFAULTERRORBOX)` and `WerSetFlags(WER_FAULT_REPORTING_NO_UI)`, delete `hs_err_*.log` in the data folder. Launcher options `-XX:ErrorFile=hs_err_%p.log -XX:-CreateCoredumpOnCrash`.
- **AppUserModelID** `TransfusionVS` (`SetCurrentProcessExplicitAppUserModelID`, and on the Start-menu shortcut).

### 3.11 Running, restarting, quitting (fixed; §4)
- `desktop/TimersRunning.kt`: running = `ArmPlanner` (and the turn and custom sources) has a next alarm, or anything is RINGING, or a transfusion check is MISSED and not acknowledged.
- Launch sources: none (user), `--logon`, `--watchdog`, `--backstop`. IT/installer modes `--installer-stop`, `--setup-check <file>`, `--test-alarm` are dispatched first in `main()`, before the mutex, AWT, the data folder and the working directory.
- **Single instance**: mutex `Local\TransfusionVS` (one instance per session). Ping: `SendMessageTimeout(hidden, WM_APP+1, SMTO_ABORTIFHUNG, 5000)`; the hidden thread answers only after a round trip through the scheduler thread and the EDT. Heartbeat file: written every 10 s after the same round trip, holding `QueryUnbiasedInterruptTime` and the PID. No answer and heartbeat older than 30 s of unbiased time → `TerminateProcess` the old PID, wait for the mutex, take over.
- **Tasks** registered from XML with `schtasks /create /xml <file> /tn <name> /f` as the current user: `\TransfusionVS Restart <SID>` (logon trigger for this user + time trigger repeating PT1M, `--watchdog`), `\TransfusionVS Backstop <SID>` (time trigger at the next alarm in UTC, repeating PT1M, `WakeToRun`, `--backstop`). Both: `InteractiveToken`, `MultipleInstancesPolicy Parallel`, `DisallowStartIfOnBatteries false`, `StopIfGoingOnBatteries false`, `ExecutionTimeLimit PT0S`, `Priority 4`, `StartWhenAvailable true`. XML built by a pure function (JVM-tested field by field).
- **Quit**: the tray menu shows "Quit" only when nothing runs, else a disabled "Stop all timers to quit". A deliberate Quit deletes both tasks (so nothing relaunches until the next logon via HKLM Run, or a manual start).
- **Shutdown**: while running, `ShutdownBlockReasonCreate("Timers are still running")`; a user-initiated `WM_QUERYENDSESSION` is refused once per shutdown attempt (a new attempt = a new `WM_QUERYENDSESSION` after the previous one was answered and no `WM_ENDSESSION(TRUE)` followed within 30 s); `ENDSESSION_CLOSEAPP` is always accepted (and the app exits, restoring the volume); `ShutdownBlockReasonDestroy` once nothing runs.

### 3.12 Packaging (fixed; departure: WiX directly, which §3 allows)
- `createDistributable` (Compose plugin) builds the app image `TransfusionVS/` (launcher `TransfusionVS.exe`, `app/`, `runtime/` from jlink with `java.desktop`, `jdk.localedata`, `jdk.unsupported`, `java.sql`, `jdk.crypto.ec`, `java.naming` as the spike shows needed; tzdb is in `java.base`).
- The MSI is built by WiX 3.14 `heat`/`candle`/`light` from `desktop/packaging/wix/Product.wxs`: per-machine (`ALLUSERS=1`, `InstallScope="perMachine"`, `ProgramFiles64Folder\TransfusionVS`), HKLM Run value `TransfusionVS` = `"…\TransfusionVS.exe" --logon`, Start-menu shortcut "A S" with the AppUserModelID, `MajorUpgrade` (`Schedule="afterInstallInitialize"`), version from the tag (`desktop-v1.0.0` → 1.0.0; dev builds 0.0.<run number>, never above a release).
- **UpgradeCodes**: release `339E9AD7-36B0-473F-A69F-3487CBE45F54`; lab `9355B348-B589-4E4E-85B2-D1DBC995762F` (product "A S LAB", folder `TransfusionVS-Lab`, launch condition: aborts if a product with the release UpgradeCode is installed).
- **Custom actions** (deferred, `Impersonate="no"`, before `RemoveFiles` on upgrade and uninstall, skipped on a first install): run the installed `TransfusionVS.exe --installer-stop`, which writes `%ProgramData%\TransfusionVS\installing` (ACL: SYSTEM and Administrators full, Users read; content: the time and msiexec's PID), waits up to 20 s for every other `TransfusionVS.exe` in any session to exit, then ends the rest. On uninstall another deferred action runs `schtasks` deletion of every task whose name starts with `TransfusionVS ` (all users). A commit-time action and a rollback action delete the marker.
- **The marker**: a running app checks it every tick; a valid one makes it restore the volume and exit. A launch exits at once while it's valid. Valid = younger than 15 min and its PID is a running `msiexec.exe`.

### 3.13 The lab hook and the two MSIs (fixed; TEST_LOOP §2)
- Source set `lab` (`desktop/src/lab/java/app/transfusionvs/lab/`): `LabMain` (the lab image's main class: installs the hook, then calls the real `main`) and `LabHook`. The hook talks to the lab only through files under `%LOCALAPPDATA%\TransfusionVS-Lab\io\` and window messages; it never opens a socket.
- The release image's main class is `app.transfusionvs.MainKt`; its jars contain no `app/transfusionvs/lab/` class, and no class mentions `LabMain`, `LabHook` or `app.transfusionvs.lab` (`desktop/scripts/scan-release.sh` scans every jar and constant pool; `desktop-release.yml` re-runs it on every attached asset).
- Both MSIs come from one commit: `gradle createDistributable` and `gradle -Plab createDistributable` into separate folders.

### 3.14 Screenshot tests (fixed before the first recording; never changed)
- Rendered headless with Compose Desktop's test scene (CPU Skia raster), bundled fonts; recorded and verified on `windows-2025` only.
- **Tolerance**: a pixel differs when any of R, G, B, A differs by more than **8** (of 255); a screenshot passes when at most **0.1 %** of its pixels differ.
- Desktop variants: main window `d600_f10` (600 × 860 dp, density 1), `d600_f10_night`, `d600_f13`, `d600_f20` (600 × 2600 dp sheet, for truncation as the reference's `w360_f20`), `min_f10` (400 × 520 dp), `s200` (600 × 860 dp at density 2.0); alarm window `a412_f10` (412 × 1032 dp), `a412_f10_night`, `a412_small` (412 × 576 dp, the 1366 × 768 @ 125% work area), `a412_f20` (412 × 2600 dp).

### 3.15 Thresholds on the desktop (§8; TEST_LOOP §4 where it applies)
- UI checks ported from the reference's `UiChecks` logic to Compose Desktop semantics: TOUCH (≥ 56 dp, alarm ≥ 64 dp), SPACING (≥ 8 dp), NOLABEL, TRUNCATED, MIDWORD, SMALLTEXT (< 16 sp body), key numbers (≥ 24 sp Home, ≥ 40 sp alarm), PRIMARY (exactly one `PRIMARY_TAG`, in the bottom third). Run at 1366 × 768 @ 1.0 and 1.25 and 1920 × 1080 @ 1.0, 1.25, 1.5, 1.75, 2.0, each at the default (clamped) and minimum window, × font 1.0, 1.3, 2.0, and the alarm window at each work-area height.
- Baseline rule (§8, trap 16): SPACING ≤ 9 and MIDWORD ≤ 1 per variant at font 2.0, as the reference; every other category 0.
- Contrast and colour-blind ΔE: the ported `WardColorsTest` (the same palette from `ui/theme`).
- Cold start: median of 10 launches of the installed release app to its first non-uniform frame (`PrintWindow` from the lab script), ≤ 3 s on windows-2025.
- Monkey: 20,000 random clicks and keys injected by the hook over both windows, 0 uncaught exceptions, process alive.
- Lint: ktlint 1.8.0 with the repo's `.editorconfig`: 0 errors on class (c) and new code; class (a)/(b) files reported against their BASELINE per-file counts and never worse.
- ATF: N/A on desktop. Screen readers (Java Access Bridge): Unverified.

## 4. The port, file by file

Reference paths are under `app/src/main/java/app/transfusionvs/` (main) and `app/src/test/java/app/transfusionvs/`
(test); desktop paths are the same under `desktop/src/…`. PORT_DIFF.md holds the unified diff of every class
(a) and (b) file against its v0.6.0 blob, regenerated on ubuntu from git blobs by `desktop/scripts/port-diff.sh`
(CI fails on any byte of difference). Class (c) engine files are listed there too, for review.

### Class (a): byte-identical (24 main + 3 shared + 9 test files)
- `schedule/ArmPlanner.kt`, `BedMask.kt`, `CustomRules.kt`, `Labels.kt`, `ScheduleCalculator.kt`, `Timeline.kt`, `TurnGrid.kt`, `TurnRules.kt`, `TurnViews.kt`, `Urgency.kt`
- `data/Entities.kt`, `Turns.kt`, `Custom.kt`, `Ward.kt`, `Dao.kt`, `TurnDao.kt`
- `alarm/RingSpec.kt`, `alarm/Undo.kt`
- `sharedTest/…/screenshots/Fakes.kt`, `TurnFakes.kt`, `CustomFakes.kt`
- tests: `schedule/BedMaskTest`, `CustomRulesTest`, `CustomSchedulePropertyTest`, `CustomViewsTest`, `ScheduleCalculatorTest`, `SchedulePropertyTest`, `TimelineTest`, `TurnGridTest`, `TurnRulesTest`
- Resources: every `drawable/ic_*.xml` that `R.drawable` names, `font/rh_*.ttf` (4), `raw/*.wav` (3), byte copies.
- They compile against the desktop `data/Settings` companion (§3.7) and `ui/screens/TurnPlanDraft` (class b, `TurnStartScreen.kt`).

### Class (b): identical except the hunks in PORT_DIFF (allowed kinds only, §3)
| Desktop file | From | Expected hunks (the spike's compile list confirms them) |
|---|---|---|
| `ui/components/Blocks.kt`, `Positions.kt`, `Rail.kt`, `TaskName.kt`, `Timeline.kt` | same | none, or the `painterResource` import |
| `ui/components/Cards.kt` | same | `painterResource` import; hover cursor on clickable cards |
| `ui/components/Common.kt` | same | `android.os.SystemClock` import → `Uptime`; `SystemClock.uptimeMillis()` → `Uptime.millis()`; `System.currentTimeMillis()` → `AppClock.now()`; `painterResource` import; focus indication and hover cursor in `wardClick`/`WardButton` |
| `ui/components/PatientField.kt` | same | focus indication only if the spike shows none |
| `ui/components/UndoBar.kt` | same | `System.currentTimeMillis()` → `AppClock.now()` |
| `ui/theme/Theme.kt`, `Tokens.kt` | same | none expected |
| `ui/theme/Type.kt` | same | `Font` import → `app.transfusionvs.res.Font` |
| `ui/screens/HomeScreen.kt` | same | `Intent`/`startActivity(AlarmActivity)` → a callback; `SetupChecks.evaluate(LocalContext.current)` → the desktop Setup check's red + orange count; `LocalContext`, `Intent`, `AlarmActivity`, `SetupChecks` imports |
| `ui/screens/StartScreen.kt`, `TurnStartScreen.kt` | same | `DateFormat.is24HourFormat(context)` → `true`; `LocalContext`/`DateFormat` imports and the `val context` line; `System.currentTimeMillis()` → `AppClock.now()` |
| `ui/screens/CustomDetailScreen.kt` | same | `SystemClock` → `Uptime` |
| `ui/screens/CustomStartScreen.kt`, `DetailScreen.kt`, `ReactionScreen.kt`, `TurnDetailScreen.kt` | same | imports only, if any |
| `alarm/AlarmContent.kt` | `alarm/AlarmActivity.kt` | removes the `AlarmActivity` class and `AlarmScreen` (class c replaces them) and their imports; `SystemClock`/`currentTimeMillis` swaps; the `isTest` subtitle "If you can hear and see this, alarms work on this PC" |
| `schedule/UrgencyTest.kt` (test) | same | removes `pocketGuardIgnoresTheFirstSecondAndACoveredPhone` and the `PocketGuard`, `MotionEvent` imports (recorded "replaced") |
| `ui/WardColorsTest.kt`, `screenshots/ClinicalContentTest.kt`, `screenshots/CustomNameFitTest.kt`, `ui/CustomDetailGuardTest.kt` (tests) | same | Robolectric `@RunWith`/`@Config`/`@GraphicsMode` → the desktop JUnit rule `DesktopDisplay("<same qualifiers>")`; `ShadowSystemClock.advanceBy` → `DesktopDisplay.advanceBy`; `createComposeRule` import → the desktop rule's (which applies width, height, density and font scale from the qualifiers string; `@Config` with no qualifiers gets Robolectric 4.17's default qualifiers, written in the rule); `org.json` from `org.json:json` |
| `BackHandler` | wherever class (b) uses `androidx.activity.compose.BackHandler` | → `androidx.compose.ui.backhandler.BackHandler`, opt-in `-opt-in=androidx.compose.ui.ExperimentalComposeUiApi` in the build |
| `collectAsStateWithLifecycle`, `LifecycleResumeEffect` | screens | same imports, from the JetBrains lifecycle 2.11.0 artifacts: no hunk |

### Class (c): rewritten, the reference as the spec
| Desktop file | Replaces | What it keeps |
|---|---|---|
| `alarm/Engine.kt` | `alarm/Engine.kt` | every decision line by line (the same public functions, order and locking); effects through `AlarmScheduler` (desktop) and `AlarmEffects`; `now()` = `AppClock.now()`; no `BedsWidget`, no `Haptics`; masking is the identity (§2: the full MRN everywhere) |
| `alarm/TurnSource.kt`, `CustomSource.kt` | same | line by line; already Android-free apart from the scheduler type |
| `alarm/AlarmScheduler.kt` | `alarm/AlarmScheduler.kt`, `Receivers.kt` routing | an in-process table keyed by the reference's addresses (`tvs://alarm/<kind>/<id>`, `…/TURN/…`, `…/CUSTOM/…`, `RING_NOW`); `set`, `cancel`, `cancelTurn`, `cancelCustom`, `setRingNow`; `armed(address)`; the ≤ 1 s tick fires due entries the way `AlarmReceiver` routes them (PREALERT → `onPreAlert`/`onCustomPreAlert`, anything else → `onAlarm`) |
| `alarm/AlarmEffects.kt` | `AlarmService`, `AlarmPlayer`, `Notifications`, `Haptics` | ring(spec)/stop, overdue sets, pre-alert balloon, tray state; the engine tests' fake records each call |
| `alarm/AlarmScreen.kt` | `AlarmScreen` in `AlarmActivity.kt` | the same flows and `AlarmContent` call; plus, as §4 asks, missed-but-unacknowledged transfusion checks stay on the window as OVERDUE (polled `dao.missedUnacknowledged()`), no masking, the desktop root |
| `ui/components/Night.kt` | same | `NightRule` from Settings, the clock (`AppClock`) and Windows' theme (UISettings) |
| `ui/screens/SettingsScreen.kt`, `SetupScreen.kt` | same | reference layout and words except §3.7 and the Setup rows of §5 |
| `ui/MainViewModel.kt` | same | every member, name and type (`home`, `ringing`, `anyTransfusion`, `transfusion(id)`, `turns`, `turnPlan`, `turnIntervalMin`, `turnWindowMin`, `startTurnPlan`, `editTurnPlan`, `stopTurnPlan`, `restartTurnPlan`, `customs`, `customTimer`, `startCustom`, `editCustom`, `stopCustom`, `setCustomStop`, `acknowledgeCustom`, `undo`, `undo(id)`, `toleranceMin`, `limitMin`, `warnBeforeMin`, `missedTimeoutMin`, `lastDurationMin`, `start`, `edit`, `end`, `stopForReaction`, `forgetStopped`, `acknowledge`) and `PRIMARY_TAG`; `ViewModel` from JetBrains lifecycle; `App` is the desktop container |
| `ui/MainWindow.kt` | `ui/MainActivity.kt` | the back stack and routes (encode/decode kept for the hide/show cycle), the same screens and callbacks |
| `App.kt`, `Main.kt` | `App.kt`, `MainActivity` | the container (db, settings, engine, scope) and `main()` |
| `data/AppDatabase.kt` | same | the same entities and DAOs, desktop version 1 |
| `data/Settings.kt` | same | §3.7 |
| `R.kt`, `res/Resources.kt` | Android `R`, `painterResource`, `Font` | §3.1 |
| `desktop/**` (new) | — | AppClock, Uptime, InputGuard, DoubleClickFilter, Placement, AlarmWindow, Tray, Tooltip, HiddenWindow, PowerRequests, SingleInstance, Heartbeat, Tasks, InstallerMarker, Sound, VolumeGuard, SetupCheck, DataFolder, TimersRunning, `win/*` bindings |
| tests `alarm/CustomEngineTest.kt`, `alarm/UndoEngineTest.kt` | same | the same class and method names; `Robolectric` → plain JUnit; `Room.inMemoryDatabaseBuilder` with the bundled driver; `PendingIntent` checks → `FakeScheduler.armed(<same address>)`; `NotificationManager` checks → `FakeEffects` records (trap 2) |

Not ported, mapped in §6: `widget/BedsWidget`, `alarm/PocketGuard` (proximity), `Haptics`, `setup/SetupChecks`,
`setup/OemGuide`, `AlarmService`, `Receivers` (beyond the routing above), notification actions, app shortcuts,
`AlarmPlayer`, `Notifications`, `src/debug` (LabReceiver), the Android manifests and `res/layout`, `res/xml`,
`res/values`, `res/mipmap`, the widget and shortcut drawables.

## 5. New words (every new string; nothing else is new)

- Window titles: "A S" (main), "A S — alarm" (the alarm window's taskbar button).
- Tray: tooltip "A S — {N} running · next {HH:MM} · {N} overdue" (parts with nothing to say are dropped: "A S — nothing running"); menu "Open A S", "Setup check", "Quit", "Stop all timers to quit".
- Shutdown reason: "Timers are still running".
- Database banner: "Timers could not be read: re-enter them".
- Alarm window errors: "No sound: the speakers can't be opened. Retrying…", "No sound device", "Couldn't save. The alarm keeps ringing — try again".
- The test alarm's subtitle: "If you can hear and see this, alarms work on this PC".
- Pre-alert balloon: title "A S", text "In {N} min: a timer is due".
- Settings: "Follow Windows" (was "Follow the phone"); "Pre-alert (gentle notification)" (was "… + short vibration"); "Alarm volume", hint "While an alarm sounds, the PC's volume is raised to at least this.", values "50 %" … "100 %".
- Setup check: caps "This PC" (was "This phone"); subtitle unchanged ("{n} of {m} ready"); rows (title · states):
  - "Starts with Windows" · ok / "Not set: reinstall the app"
  - "Restart task" · ok / "Missing"
  - "Backstop task" · ok / "Missing" / "Not needed: no timer running"
  - "Keeps the PC awake while timers run" · ok / "Not active" / "Not needed: no timer running"
  - "Wake timers" · "Allowed" / "Off: a sleeping PC can't wake for an alarm"
  - "Power button" · "{Sleep|Hibernate|Shut down|Do nothing|Turn off the display}"; "Closing the lid" · same states / "No lid" (red when Sleep or Hibernate while wake timers are off)
  - "Modern Standby" · "Yes" / "No"
  - "Apps may keep the PC awake" · "Allowed" / "Blocked by policy"
  - "Windows restart pending" · "No" / "Yes: restart before your shift"; "Active hours" · "{HH:MM}–{HH:MM}" / "unknown"
  - "Other users signed in" · "None" / "{N}: their alarms ring silently"
  - "Sound output" · "{device} ({Speakers|Headphones|HDMI|…})" / "Headphones or HDMI: check you can hear it" / "Jack unplugged" / "No sound device"
  - "Alarm volume" · "{N} %"
  - "Tray icon pinned" · "unknown"; "Focus assist" · "unknown"
  - "App time matches Windows" · "Yes" / "No: app {HH:MM} (UTC{±h:mm}), Windows {HH:MM} (UTC{±h:mm})"
  - "Someone must be signed in" · "Alarms ring only while you are signed in to Windows"
  - the reference's test wording, PC instead of phone: "Lock the PC now and wait — it must ring even when muted." and "Restart the PC and sign in — it must still ring on time."; "Never close the app from Task Manager — use Quit in the tray." (replaces the phone's "Force stop" line)
  - version line "A S · Transfusion VS {version} · Windows"
- IT modes' file output (`--setup-check`): "{title}\t{state}\t{red|orange|ok|unknown}" lines, and "not running".
- Home's existing setup alert keeps its words; it now counts every red and orange row.

## 6. Port map

### 6.1 Reference JVM tests (BASELINE: 241 in 22 classes)
| Reference class (methods) | Desktop | Disposition |
|---|---|---|
| `schedule.BedMaskTest` (5), `CustomRulesTest` (19), `CustomSchedulePropertyTest` (7), `CustomViewsTest` (6), `ScheduleCalculatorTest` (17), `SchedulePropertyTest` (9), `TimelineTest` (8), `TurnGridTest` (13), `TurnRulesTest` (10) | the same file, byte-identical | ported; every method must give BASELINE's PASSED |
| `schedule.UrgencyTest` (14) | class (b) | 13 ported; `pocketGuardIgnoresTheFirstSecondAndACoveredPhone` **replaced** by `desktop.InputGuardTest` + lab W07 |
| `alarm.CustomEngineTest` (17), `alarm.UndoEngineTest` (11) | class (c), same names | rewritten against `FakeScheduler`/`FakeEffects` (trap 2). `masksCountCustomMrns_soTheSuffixStaysUnique`: same name, now asserts the desktop's decision (pre-alerts go to their own type and carry no MRN or name; the app never masks) — recorded "rewritten (decision)" |
| `data.MigrationTest` (5) | — | **N/A**: the desktop schema starts at version 1; `desktop.DatabaseSchemaTest` checks v1 against the committed JSON |
| `ui.WardColorsTest` (3), `screenshots.ClinicalContentTest` (3), `screenshots.CustomNameFitTest` (4), `ui.CustomDetailGuardTest` (1) | class (b) | ported (WardColorsTest from Step C) |
| `screenshots.AuditScreenshots` (46), `CustomScreenshots` (20), `RepositioningScreenshots` (9), `TransfusionScreenshots` (10), `ArabicPhoneTest` (4) | `screenshots.DesktopScreenshots` + `DesktopAudit` | **replaced** by the desktop screenshot tests (§6.3); `ArabicPhoneTest` by `desktop.LatinDigitsTest` + lab W49. Widget methods: N/A (no widget, §2) |

### 6.2 Instrumented tests (`app/src/androidTest/.../uicheck/`)
| Reference | Desktop |
|---|---|
| `TapBudget.task1_seeAllBeds` … `task12_stopCustom` (14) | `desktop.TapBudgetTest`, same method names, clicks counted (tray click = the icon tap; the overflow click reported, not counted); task 1 at the default size on 1920 × 1080 @ 100% with six patients |
| `UiChecks.*` (28: `homeEmpty` … `customAlarmWithTransfusion`) | `desktop.UiChecksTest`, same screens and method names, at every §3.15 variant |
| `Robustness.doubleTapOnAckAcknowledgesOnce`, `doubleTapOnCustomDoneAcknowledgesOnce`, `doubleTapOnStartStartsOnce`, `doubleTapOnAnIntervalStartsOnce`, `doubleTapOnHomeOpensOneScreen` | `desktop.RobustnessTest` (same names) + lab W48 (real `SendInput` double clicks) |
| `Robustness.undoOnAlarmRestoresTheExactPreviousState`, `undoLastOnTheCardRestoresTheExactPreviousState`, `undoOnCustomDoneRestoresTheExactPreviousState` | `desktop.RobustnessTest` (same names) |
| `Robustness.widgetMasksAnMrn`, `widgetShowsClockTimesAndAcknowledges`, `widgetMasksACustomMrnAndName`, `widgetDoneAcknowledgesACustomAlarm` | **N/A**: no widget (§2) |

### 6.3 The audit set (BASELINE: 65 screens)
Each screen below is rendered by `DesktopAudit.<name>` in the main-window variants of §3.14 (alarm screens in the
alarm-window variants) and shown beside the reference's `w412_f10` / `w412_f10_night` shot.
- Main window, same content: `home_empty`, `home_setup_warning`, `home_1bed_upcoming`, `home_ringing`, `home_overdue_missed`, `home_both_features`, `home_6beds`, `home_20beds`, `home_end_vs_pending`, `home_stopped_reaction`, `home_only_turning`, `home_only_custom`, `home_with_custom`, `undo_bar_on_home`, `start_empty`, `start_edit`, `start_options`, `start_bed_picked`, `detail_upcoming`, `detail_ringing`, `detail_on_time_late`, `detail_overdue_missed`, `detail_ended_end_vs`, `detail_stopped_reaction`, `reaction_1bed`, `reaction_20beds`, `reaction_no_bed`, `turn_start`, `turn_start_options`, `turn_start_bed_picked`, `turn_edit`, `turn_detail_on_track`, `turn_detail_overdue_missed`, `turn_detail_skipped`, `turn_detail_stopped`, `custom_start_empty`, `custom_start_valid`, `custom_start_long_name`, `custom_start_other`, `custom_edit`, `custom_detail_upcoming`, `custom_detail_ringing`, `custom_detail_overdue`, `custom_detail_stop_set`, `custom_detail_stopped`.
- Main window, class (c) screens (the reference shot is the spec, §3.7 deltas allowed): `settings`, `setup_check`.
- Alarm window: `alarm_1bed`, `alarm_2beds`, `alarm_snoozed`, `alarm_snooze_used_up`, `alarm_done_undo`, `alarm_transfusion_and_round`, `round_alarm`, `round_checklist`, `round_7beds`, `custom_alarm_1`, `custom_alarm_3`, `custom_alarm_long_name`, `custom_alarm_wide_name`, `custom_alarm_with_round`, `custom_alarm_with_transfusion`, `custom_alarm_with_transfusion_and_round`.
- **N/A**: `widget`, `custom_widget` (no widget, §2); the `w360_f10_ar` shots → `desktop.LatinDigitsTest` + lab W49 (a desktop doesn't mirror; the region can't change digits).
- New desktop shots: `alarm_test` (the "on this PC" line), `home_db_banner`, `alarm_db_banner`, `alarm_no_sound`, `alarm_save_failed`, the Setup check row states.

### 6.4 Reliability cases (`lab/reliability.py`)
| Case | Desktop |
|---|---|
| A01 timing, locked keyguard | W01–W03 (on time, each type); the lock screen itself: Unverified U02 |
| A02 Doze, A03 battery saver | N/A (Android power modes); the desktop's idle sleep: W26 + U01 |
| A04 do not disturb | N/A as such; Focus assist row shows "unknown" (W44); real Focus assist: Unverified (no documented API) |
| A05 silent, A06 vibrate | W12 (muted / low device volume is raised, then restored) |
| A07 process killed | W13; JVM `DesktopEngineScenarioTest.a07_*` |
| A08 removed from recents | W11 (taskbar close, `WM_CLOSE`), W13 |
| A09 force stop, restored on open | W13 (killed → restart task), W43 |
| A10 app update | W35 |
| A11 reboot before unlock, A11b after unlock | Unverified U03 (no reboot on a hosted runner); restart-task and HKLM Run XML: W16, W44 |
| A12 clock change | W18; JVM `a12_*` |
| A13 timezone change | W19; JVM `a13_*` |
| A14 Egypt DST | W20 (AppClock); JVM `a14_*`; tzdb version W31 |
| A15 permissions revoked warns | W44 (each Setup row's red/orange state raises Home's count) |
| A16 alarm volume zero warns | W12 (raised, no warning needed); "No sound device"/output rows W44 |
| A17 pocket guard, first second | W07 (input guard, 2 s) |
| A18 pocket guard, proximity | N/A (no proximity sensor on a PC) |
| A19 undo | `RobustnessTest.undo*`, JVM `UndoEngineTest` |
| A20 same minute, one screen | W24; JVM `a20_*` |
| A21 missed timeout, Overdue | W22, W23; JVM `a21_*` |
| A22 notification action | N/A (no notification actions, §0); the alarm window's buttons: W01–W03 |
| A23 round, locked, own sound | W02 (own sound, from `RingSpec`); lock screen U02 |
| M01 MRN masked (gate) | **N/A by decision** (§2: the full MRN shows everywhere; nothing leaves the app with an MRN: W34, W40, W41) |
| The `@custom` variants | the same desktop rows, run for the custom type |

### 6.5 Not ported by decision or platform
`NIGHT_BRIGHTNESS` dimming: N/A (no per-window brightness; the monitor shows Cerner). Haptics: N/A. Widget,
shortcuts, notification actions, `OemGuide`, `SetupChecks`: N/A (replaced by the desktop Setup check).

## 7. New tests

### 7.1 JVM engine scenarios (`desktop/src/test/java/app/transfusionvs/alarm/DesktopEngineScenarioTest.kt`)
Real engine, Room in memory (bundled driver), `FakeScheduler`, `FakeEffects`, `AppClock` driven by the test:
- `a07_restartAfterTheDueTime_ringsWhatCameDueAtOnce` (engine rebuilt on the same database after the due time)
- `a07_restartAfterHours_onlyTheLatestRings_theRestAreMissed` (transfusion, round, custom)
- `a12_clockJumpPastTheCheck_ringsAtOnce`, `a12_clockJumpBack_neverRingsEarly`
- `a13_zoneChange_movesThePendingSlotOntoTheNewGrid_andKeepsTheTransfusionCheck` (Asia/Kathmandu, as the lab case)
- `a14_egyptDstNight_firstSlotAfterTheGapIs0100` (2027-04-30, Africa/Cairo)
- `a20_transfusionAndRoundInTheSameMinute_oneAlarm_transfusionFirst`, `a20_customJoinsAsCustomNDue`
- `a21_missedTimeout_marksMissed_stopsTheSound_staysOverdue`, `a21_roundSilencedAtTimeout_missedWhenTheNextSlotFires`
- `retention_finishedGoesAfterADay`, `idCollision_acrossTheThreeTypes` (with the ported custom collision tests)

### 7.2 Desktop unit and UI tests (`desktop/src/test/java/app/transfusionvs/desktop/`)
`SchedulerTimingTest` (0–5 s, never early; clock and zone jumps; DST; gaps), `NoSilentSettingTest` (every
setting × every allowed value, device at 0 % and muted → the alarm plays and the endpoint is raised),
`SettingsFileTest`, `TimersRunningTest`, `ZoneAgreementTest` (alarm clock text = Home's next time = the engine's
next due, after a zone change), `PlacementTest` (1.0, 1.25, 1.5, 2.0 × 1366 × 768 @ 125% and 1920 × 1080 @ 200%),
`InputGuardTest`, `AlarmWindowGuardUiTest` (2 s; each re-arm; a press begun while guarded; Enter/Space/Esc),
`AlarmWindowUnfocusedTest` (rows and countdown update at Lifecycle STARTED), `DoubleClickFilterTest`,
`TooltipTest` (≤ 127 characters, no MRN, no name), `VolumeGuardTest` (fake endpoint: raise, restore only what we
set, crash restore), `TaskXmlTest`, `InstallerMarkerTest`, `LaunchModeTest` (IT modes take no mutex, create no
data folder, open no database), `TakeoverDecisionTest`, `DataFailureTest` (corrupt, locked, read-only),
`DatabaseSchemaTest`, `LatinDigitsTest` (ar-EG default locale), `SetupCheckRowsTest` (every row state from faked
readings, and Home's red + orange count), `HiddenWindowTest` (windows-2025 job only: each message reaches the
hidden window while the main window is hidden), `TapBudgetTest`, `UiChecksTest`, `RobustnessTest`,
`DesktopScreenshots`, `DesktopAudit`, `ResourcesTest` (every `R` id loads; every `ic_*.xml` parses).

## 8. Windows lab rows (`desktop/lab/`, `desktop-lab.yml`)

The app runs with a non-admin token (`runas /trustlevel:0x20000` on the runner's own account) except install,
upgrade and uninstall. "MSI" says which one the row runs on. **M** = MUST PASS (§8).

| Row | What | MSI | |
|---|---|---|---|
| W01 / W02 / W03 | the alarm rings 0–5 s after due, never early: transfusion / round / custom (sound playing per the hook's player log, window up) | lab | M |
| W04 | the alarm window above a topmost borderless full-screen window; a `SendInput` click registers | lab | M |
| W05 | Notepad in front, `SendInput` typing while the alarm appears: the text arrives complete | lab | M |
| W06 | 5 min, timers running, both tasks firing: `GetForegroundWindow` every 100 ms never changes; Notepad text complete | lab | M |
| W07 | input guard re-arms on every §4 trigger: a click 100 ms after is ignored, one at 2.1 s works (unlock, reconnect, display-on, resume faked by the hook) | lab | M |
| W08 | a topmost window re-asserting every 200 ms for 60 s over the alarm: a click on Done while `WindowFromPoint` is the alarm works within 15 s | lab | M |
| W09 | `SetWindowPos` to x = −2000 px, then a resolution change in the same second: back in view within 1 s | lab | M |
| W10 | W04 and W09 again at `-Dsun.java2d.uiScale=1.25` and `2.0` | lab | M |
| W11 | after `SC_CLOSE`, Esc, `WM_CLOSE`, Win+D, Win+M: visible, not minimised, topmost, not cloaked, `GA_ROOT` of `WindowFromPoint(centre)` is the alarm | lab | M |
| W12 | device volume and mute raised while sounding and restored exactly; again after killing the app mid-alarm (restored at the next start) | lab | M* |
| W13 / W14 / W15 | killed 30 s before an alarm → ≤ 75 s late; suspended → ≤ 90 s; UI thread blocked (hook) 30 s before → ≤ 90 s | lab | M |
| W16 | both tasks' XML (`schtasks /query /xml`) field by field, created by the standard user, SID-named | lab | M |
| W17 | msiexec killed right after `--installer-stop`; separately a forced failing install: within 2 min the app runs, the next alarm rings, Setup check is clean | release | M |
| W18 / W19 | real clock change; real `tzutil` change (one PowerShell step, try/finally, ≤ 2 h moves, ≤ 10 min) | lab | M |
| W20 / W21 | DST night and long gaps on `AppClock` | lab | M |
| W22 / W23 | the missed timeout; MISSED when the next one fires | lab | M |
| W24 | the same-minute order (transfusion > round > custom) | lab | M |
| W25 | a second instance (user start) brings the first to the front; `--logon`, `--watchdog`, `--backstop` exit within 2 s without activating it | release | M |
| W26 | the power requests: SystemRequired while running, DisplayRequired while unanswered (read back with the hook's own `PowerRequests` state and `powercfg /requests` run by the admin lab script, never by the app) | lab | M |
| W27 | the backstop points at the next alarm and repeats every minute until answered | lab | M |
| W28 | `ShutdownBlockReasonQuery` returns the reason while running and nothing after; a test-sent `WM_QUERYENDSESSION` is refused once; `ENDSESSION_CLOSEAPP` accepted and the app exits | lab | M |
| W29 / W30 | corrupt database; read-only data folder: in-memory DB, red banner and tray, alarms set in the session ring, no crash | lab | M |
| W31 | Java Sound, the AWT tray, JNA, SQLite and Skiko each driven once and their DLLs in the module list; `jdk.localedata` present + an ar-EG `DecimalFormatSymbols` read; `ZoneRulesProvider.getVersions("Africa/Cairo")` = the bundled tzdb version | lab | M |
| W32 | every loaded module is in the install folder or under `%SystemRoot%` (exceptions listed in §10 with their owner) | release | M |
| W33 | no network socket, sampled every second for the whole run, plus a logging firewall block rule with zero hits | release | M |
| W34 | the test MRN and task name absent from every file the app wrote outside its database and settings, and from `reg export HKCU` (text, and every REG_BINARY as UTF-16LE and ASCII, each with and without ROT13) | release | M |
| W35 | a 1.0.1 MSI (same commit, same UpgradeCode) over a running 1.0.0 with one timer of each type: all keep running, the next alarm fires, exit code 0 | release | M |
| W36 | clean uninstall: no Run value, no `TransfusionVS ` task for any user, no process; the data folder stays | release | M |
| W37 | the taskbar button flashes until answered (`FlashWindowEx` state via the hook) | lab | |
| W38 | the red tray icon: overdue, database error, zone mismatch | lab | |
| W39 | Quit hidden while timers run | lab | |
| W40 | the pre-alert balloon has no MRN and no task name | lab | |
| W41 | the tooltip ≤ 127 characters, no MRN, no name | lab | |
| W42 | Restart Manager (`ENDSESSION_CLOSEAPP`) makes the app exit and restore the volume | lab | |
| W43 | a crash file `hs_err_*.log` is deleted on the next start; no minidump written | lab | |
| W44 | each Setup check row: every state renders from a hook-faked reading; the runner's real reading logged; Home's count = red + orange | lab | |
| W45 | cold start median of 10 ≤ 3 s (windows-2025) | release | |
| W46 | monkey: 20,000 hook clicks and keys, 0 crashes | lab | |
| W47 | `--setup-check <file>` and `--test-alarm` on the installed release; "not running" and exit 2 with no instance | release | |
| W48 | double click acts once: two `SendInput` clicks 100 ms and `GetDoubleClickTime()` − 20 ms apart on the alarm's primary with two transfusion beds due, and on Home's pinned primary: exactly one acknowledged each time | lab | |
| W49 | Latin digits under `Set-Culture ar-EG` (the hook reads the rendered texts) | lab | |
| W50 | "Test alarm in 1 minute" button: rings 55–65 s later with the "on this PC" line | lab | |
| W51 | the installer marker: a launch exits while it's valid; a stale or non-msiexec marker is ignored | release | |
| W52 | the hidden window gets each message while the main window is hidden (Set-Date, tzutil, test-sent `WM_QUERYENDSESSION`, an MSI upgrade) | lab | |
| W53 | the lab MSI refuses to install over the release product | lab | |
| W54 | default-device change mid-alarm reopens on the new device within 1 s | lab | M* |
| W55 | audio stream can't open (hook) → red line, retry every 5 s; saving fails (hook) → error, keeps sounding | lab | |
| W56 | the zone agreement after a real `tzutil` change: the alarm's clock text, Home's next time and the engine's next due | lab | |
| W57 | the main window never activates for `--logon`/`--watchdog`/`--backstop` launches (W06 covers the 5-minute sample) | lab | |

\* The only allowed downgrade (§7 step 4): if no free, signed virtual audio device installs on the hosted
runners, W12 is proved by `VolumeGuardTest` and `NoSilentSettingTest` and is Unverified on runners with the spike
log as the reason; with fewer than two endpoints, W54 alone ("one audio endpoint").

**Unverified** (§8; each with its runner limitation; the runners are Windows Server 2022/2025): U01 real sleep
and wake; U02 the lock screen; U03 logoff and reboot (and whether Windows displays the shutdown reason); U04
display power; U05 multiple monitors (one display, 1024 × 768); U06 Modern Standby; U07 Citrix/RDP full screen;
U08 real speakers; U09 screen readers (Java Access Bridge); U10 touch input; U11 Windows 10/11 client editions;
U12 real display scaling above 100% (runners can't change it without a logoff). Setup states the runner can't
produce (other users signed in, restart pending, Modern Standby, the lid action) are Unverified under W44.

## 9. Traceability (§4 bullets and §6 traps)

| Requirement | Proof |
|---|---|
| Starts at logon (HKLM Run), lives in the tray | W36 (value present after install, gone after uninstall), W44 row "Starts with Windows"; logon itself U03 |
| One left click on the tray opens Home (mouse listener); closing hides | W39 (hook drives the tray listener), `TapBudgetTest.task1_seeAllBeds` |
| "Running" definition | `TimersRunningTest` |
| Quit hidden while running; nothing relaunches after Quit | W39; `TaskXmlTest` (Quit deletes the tasks) |
| Launch sources; IT modes first, no mutex/data/db | `LaunchModeTest`, W47, W57 |
| Only a user start brings the app forward; alarm window without activation | W06, W25, W57 |
| One instance, ping through both threads, heartbeat takeover | W25, W13–W15, `TakeoverDecisionTest` |
| Restart task, backstop, XML fields | W16, W27, `TaskXmlTest` |
| Missed alarms after any gap | W13–W15, W21, `a07_*`; sleep/off U01, U03 |
| Shutdown reason; refuse once; `ENDSESSION_CLOSEAPP` | W28, W42; display U03 |
| No user logged on → no alarm; Setup says so; restart pending, active hours | W44 rows; real states U03 / Unverified under W44 |
| On time 0–5 s, ≤ 1 s tick | W01–W03, `SchedulerTimingTest` |
| Zone re-detection, Egypt tzdb, app vs Windows time, per-tick mismatch → red tray | W19, W31, W44, W56, W38, `ZoneAgreementTest` |
| Power requests; Modern Standby DisplayRequired | W26; Modern Standby U06 |
| Sleep by hand → backstop wake timer, or missed alarms ring on wake | W27 (`WakeToRun` in XML), U01 |
| Setup: wake timers, buttons, lid, Modern Standby, policy; never `powercfg /requests` | W44, `SetupCheckRowsTest`, W32 (no powercfg child process in the app's process tree, logged by the lab) |
| Alarm window per monitor | `PlacementTest` (monitor choice), U05 |
| Placement: fixed width, right edge, drag, keeps place | `PlacementTest`, W09, W10 |
| Styles; never focus; ignores close/minimise; recreate when cloaked; re-assert topmost; flashing | W04, W05, W06, W11, W37 |
| Kept on screen (every tick, `WM_DISPLAYCHANGE`) | W09, `PlacementTest` |
| Updates while unfocused | `AlarmWindowUnfocusedTest` |
| Input guard (2 s, re-arm list, 10 s cap, countdown doesn't re-arm, press-while-guarded, keys) | W07, W08, `InputGuardTest`, `AlarmWindowGuardUiTest` |
| Double clicks consumed at the root (not in text fields) | W48, `DoubleClickFilterTest`, `RobustnessTest.doubleTap*` |
| Same content, actions and rules; order; "Custom · N due"; reaction guide only with a transfusion; night theme | W24, `a20_*`, `ClinicalContentTest`, `DesktopAudit` alarm shots, `UiChecksTest.alarm*` |
| `NIGHT_BRIGHTNESS` N/A | §6.5 |
| Sound: one at a time, priority; restore record; raise; restore only ours; crash restore | W12, `VolumeGuardTest`, `NoSilentSettingTest` |
| Stream can't open → red, retry 5 s; device change ≤ 1 s; no device; output form factor | W55, W54, W44, `VolumeGuardTest` |
| Save failure → error, keeps sounding | W55 |
| Missed timeout: sound stops, window OVERDUE, tray red | W22, W38, `a21_*` |
| Pre-alert balloon without MRN or name | W40, `CustomEngineTest.collision_preAlertsGoToTheirOwnType` |
| Main window screens, layout (600 dp column, default size, more cards when taller) | `DesktopAudit`, `TapBudgetTest.task1_seeAllBeds`, `UiChecksTest` |
| Supported screens and text sizes; minimum window scrolls | `UiChecksTest` matrix; real scaling U12 |
| Keyboard: typing, Tab, Enter never starts | `RobustnessTest.keyboard*` (new methods), keyboard persona |
| Latin digits, left-to-right | `LatinDigitsTest`, W49 |
| New words only | §5; scope audit |
| Tray pinned/Focus assist "unknown"; documented sources only | W44, `SetupCheckRowsTest`; scope audit |
| Tooltip | W41, `TooltipTest` |
| Tap budgets 1–12 | `TapBudgetTest` |
| Setup check rows; IT modes; Home counts red + orange | W44, W47, `SetupCheckRowsTest` |
| Privacy: no log files, nothing with an MRN on disk outside db/settings, no export, no socket | W33, W34, W40, W41 |
| Installation guide | INSTALL_AR (`.md`, `.html`), reviewed by the scope audit; screenshots from CI where reproducible (W47 setting the download mark + `ShellExecute`), else marked illustrations |
| Trap 1 hidden dependencies | class (a)/(b) compile (Step A/C), `DesktopEngineScenarioTest`, `UrgencyTest` "replaced" entry, PORT_DIFF |
| Trap 2 engine effects | `CustomEngineTest`, `UndoEngineTest` (fakes one-to-one) |
| Trap 3 resources | `ResourcesTest`, `DesktopAudit` |
| Trap 4 focus stealing | W05, W06, W57 |
| Trap 5 window survival | W04, W08, W11, W09 |
| Trap 6 volume | W12, `VolumeGuardTest` |
| Trap 7 process death and hangs | W13–W15, W27 |
| Trap 8 thread-owned power requests | W26 (requests survive their creating thread's exit; created with `PowerCreateRequest`) |
| Trap 9 install scope, %TEMP% DLLs | W32, W36 (per-machine path) |
| Trap 10 broadcasts need a top-level window | W52, `HiddenWindowTest` |
| Trap 11 runners | §8 Unverified list; screenshot tolerance §3.14; software rendering |
| Trap 12 time | W18–W21, the PowerShell try/finally step in `desktop/lab/clock.ps1` |
| Trap 13 tags | `desktop-release.yml` (`desktop-v*` only, drafts, `--latest=false`); §9 step 4 check |
| Trap 14 line endings | `desktop/.gitattributes`; PORT_DIFF on ubuntu from blobs |
| Trap 15 standard users | every lab row under `runas /trustlevel:0x20000`; W16 |
| Trap 16 the reference's own failures | §3.15 baseline rule, BASELINE per-file ktlint |

## 10. Runner limitations (for "Setup check is clean", §8)
Red only because of: no audio device (Windows Audio disabled, no endpoint) → "Sound output: No sound device";
a pending restart on the image → "Windows restart pending: Yes". Logged with the runner's reading, counted
Unverified. Allowed loaded modules outside the install folder and `%SystemRoot%`: none expected; the spike lists
any it sees (e.g. a runner's antivirus or monitoring agent), with its owner.

## 11. Workflows and measured durations
| Workflow | Trigger | Measured |
|---|---|---|
| `desktop-reference.yml` | its own file changes, or `[baseline]` | 3 min 18 s (run 42461704759) |
| `desktop-spike.yml` | `[spike]` | (Step 0) |
| `desktop-build.yml` | every `desktop` push | (Step A) |
| `desktop-lab.yml` | `[win-lab]` | (Step E) |
| `desktop-release.yml` | `desktop-v*` tags | (§9) |

## 12. Step 0, the spike (`desktop/spike/`, `desktop-spike.yml`, windows-2022 and windows-2025)
What it must prove (§7 step 4), each with its log:
1. The port: `ui/` and the alarm composables copied unchanged into a Compose Desktop module with the §2 versions; every compile error recorded (sizes class (b)).
2. Install, upgrade, uninstall from the custom WiX template: silent per-machine install; an upgrade over a running app with a timer (exit 0, not 3010); an uninstall while it runs leaving no task, no Run value, no process.
3. The alarm window: topmost, non-activating, over a topmost borderless full-screen window; clicks register; survives Win+D, Win+M, `WM_CLOSE`, a virtual-desktop switch; software vs DirectX rendering.
4. Windows messages at the hidden window while the main window is hidden: Set-Date, tzutil, a test-sent `WM_QUERYENDSESSION`, an MSI upgrade.
5. The shutdown reason: `WM_QUERYENDSESSION` returns FALSE and `ShutdownBlockReasonQuery` returns the reason.
6. As a standard user (`runas /trustlevel:0x20000`): creating the tasks, Core Audio, the power requests.
7. A failed or cancelled install leaves no valid marker.
8. Power and tasks: the requests; the task XML read back; restart of a killed process; the ping takeover.
9. Audio: Core Audio set and restore on a free, signed virtual audio device, named with its licence; or the downgrade.
10. Native libraries load from the install folder (module list), nothing unpacked to `%TEMP%`.
11. WiX 3.14 present on both images (or downloaded, MS-RL); Room's Gradle plugin on a Kotlin JVM module; `loadXmlImageVector` availability; the desktop compose test rule's size and density control.

Results: (written here after the spike; a "no" changes the architecture before Step A).
