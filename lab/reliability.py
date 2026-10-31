#!/usr/bin/env python3
"""Alarm reliability suite (docs/TEST_LOOP.md §3). Runs on one emulator through adb.

Every test arranges a real alarm with the debug-only LabReceiver, puts the device in the condition
under test, waits passively (dumpsys only: nothing here may wake the app) and asserts what actually
happened. Results go to <out>/results.json with the evidence next to it. A test that cannot prove
its claim reports FAIL, or N/A with the reason when Android has no such behaviour on this level.
"""
import argparse
import datetime as dt
import json
import pathlib
import re
import subprocess
import sys
import time
import traceback

PKG = "app.transfusionvs"
HOOK = f"{PKG}/.lab.LabReceiver"
HOME = f"{PKG}/.ui.MainActivity"
PIN = "1234"

args = None
api = 0
out = None
results = []


def run(cmd, timeout=120, check=False):
    p = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=timeout, errors="replace")
    if check and p.returncode:
        raise RuntimeError(f"{cmd}: {p.stderr or p.stdout}")
    return p.stdout + p.stderr


def sh(cmd, timeout=120):
    return run(f"adb shell {json.dumps(cmd)}", timeout)


def hook(action, **extras):
    parts = [f"am broadcast -n {HOOK} -a lab.{action} -f 0x20"]
    for k, v in extras.items():
        parts.append(f"{'--ei' if isinstance(v, int) else '--es'} {k} {json.dumps(v) if isinstance(v, str) else v}")
    raw = run(f"adb shell '{' '.join(parts)}'", 60)
    m = re.search(r'data="(.*)"\s*$', raw, re.S)
    if not m:
        raise RuntimeError(f"hook {action}: {raw}")
    state = json.loads(m.group(1))
    if "error" in state:
        raise RuntimeError(f"hook {action}: {state['error']}")
    return state


def wait_boot():
    run("adb wait-for-device", 300)
    for _ in range(150):
        if sh("getprop sys.boot_completed").strip() == "1":
            return
        time.sleep(2)
    raise RuntimeError("boot timeout")


def root():
    run("adb root")
    time.sleep(2)
    run("adb wait-for-device", 120)


def now_device():
    return int(sh("date +%s").strip()) * 1000


def set_clock(when: dt.datetime):
    sh("settings put global auto_time 0")
    sh(f"date {when.strftime('%m%d%H%M%Y.%S')}")


def device_time() -> dt.datetime:
    return dt.datetime.strptime(sh("date +%Y-%m-%dT%H:%M:%S").strip(), "%Y-%m-%dT%H:%M:%S")


def wake():
    sh("input keyevent KEYCODE_WAKEUP")


def sleep_screen():
    sh("input keyevent KEYCODE_SLEEP")
    time.sleep(1)


def set_pin():
    print(sh(f"locksettings set-pin {PIN}").strip())


def clear_pin():
    sh(f"locksettings clear --old {PIN}")


def unlock():
    wake()
    time.sleep(1)
    sh("wm dismiss-keyguard")
    time.sleep(1)
    sh("input keyevent 82")
    time.sleep(1)
    sh(f"input text {PIN}")
    sh("input keyevent 66")
    time.sleep(2)


def grant():
    if api >= 33:
        sh(f"pm grant {PKG} android.permission.POST_NOTIFICATIONS")
    if api >= 34:
        sh(f"appops set {PKG} USE_FULL_SCREEN_INTENT allow")
    if api >= 31 and api < 33:
        sh(f"appops set {PKG} SCHEDULE_EXACT_ALARM allow")
    sh(f"cmd notification allow_dnd {PKG}")
    if api < 28:
        sh(f"settings put secure enabled_notification_policy_access_packages {PKG}")
        time.sleep(1)


def reset():
    """Clean slate before each test: no data, no alarms, normal phone."""
    wake()
    sh("dumpsys battery reset")
    sh("settings put global low_power 0")
    set_dnd("off")
    sh(f"pm clear {PKG}")
    grant()
    hook("RINGER", mode="NORMAL")
    set_stream_volume(4, 5)
    sh("input keyevent KEYCODE_HOME")
    time.sleep(1)


def set_dnd(mode):
    if api >= 28:
        sh(f"cmd notification set_dnd {mode}")
    else:
        hook("DND", mode=mode)


def set_stream_volume(stream, value):
    if api >= 30:
        sh(f"cmd media_session volume --stream {stream} --set {value}")
    else:
        sh(f"media volume --stream {stream} --set {value}")


def activities():
    return sh("dumpsys activity activities")


def alarm_on_top():
    return re.search(r"(mResumedActivity|ResumedActivity|topResumedActivity)[^\n]*alarm\.AlarmActivity", activities()) is not None


def wait_alarm(timeout_s):
    """Passive wait: dumpsys only."""
    end = time.time() + timeout_s
    while time.time() < end:
        if alarm_on_top():
            return True
        time.sleep(2)
    return False


def diagnose(name):
    """Everything needed to tell a real failure from a test problem, before anything else changes."""
    parts = {
        "alarm": sh(f"dumpsys alarm | grep -A4 {PKG} | head -60"),
        "notification": sh(f"dumpsys notification --noredact | grep -A12 'pkg={PKG}' | head -80"),
        "services": sh(f"dumpsys activity services {PKG} | head -40"),
        "audio": "\n".join(l.strip() for l in sh("dumpsys audio").splitlines() if "usage=" in l and "state:" in l)[:1500],
        "users": sh("dumpsys user | grep -iE 'state:|running' | head -6"),
        "permissions": sh(f"dumpsys package {PKG} | grep -E 'POST_NOTIFICATIONS|USE_FULL_SCREEN_INTENT' | head -6") + sh(f"appops get {PKG} USE_FULL_SCREEN_INTENT"),
        "logcat": run(f"adb logcat -d -b all | grep -E '{PKG}|FullScreen|NotifInterrupt|BubbleMetadata|Suppressing notification|AndroidRuntime' | grep -v StrictMode | tail -150", 60),
    }
    (out / f"{name}.diagnosis.txt").write_text("\n\n".join(f"=== {k}\n{v}" for k, v in parts.items()), errors="replace")


def wait_ringing(timeout_s):
    """The phone is in use: Android shows the alarm as a heads-up, not full screen. Sound is the proof."""
    end = time.time() + timeout_s
    while time.time() < end:
        if audio_alarm_playing()[0]:
            return True
        time.sleep(2)
    return False


def screenshot(name):
    run(f"adb exec-out screencap -p > {out / (name + '.png')}")


def save(name, text):
    (out / (name + ".txt")).write_text(text, errors="replace")


def keyguard_info():
    a = activities()
    w = sh("dumpsys window")
    lines = [l.strip() for l in (a + "\n" + w).splitlines() if re.search(r"keyguard|occluded|Lockscreen", l, re.I)]
    return "\n".join(lines[:40])


def keyguard_up():
    info = keyguard_info()
    return bool(re.search(r"(mKeyguardShowing|KeyguardShowing|keyguardShowing|mShowingLockscreen|showing)=true", info)), info


def audio_alarm_playing():
    a = sh("dumpsys audio")
    hit = [l.strip() for l in a.splitlines() if "state:started" in l and re.search(r"usage=(USAGE_)?ALARM|usage=4\b", l)]
    return bool(hit), "\n".join(hit) or a[:0]


def vibrating():
    if api >= 31:
        raw = sh("dumpsys vibrator_manager")
        if re.search(r"vibratorIds=\[\]|mVibratorIds=\[\]", raw):
            return None, raw[:400]
        m = re.search(r"mCurrentVibration=(.*)|currentVibration=(.*)|CurrentVibration:(.*)", raw)
        on = bool(m and "null" not in m.group(0)) or bool(re.search(r"status=RUNNING|usage=ALARM.*(playing|started)", raw))
        return on, raw[:1500]
    raw = sh("dumpsys vibrator")
    if "not found" in raw.lower() or "can't find service" in raw.lower():
        return None, raw[:300]
    m = re.search(r"mCurrentVibration=(.*)", raw)
    return bool(m and "null" not in m.group(1)), raw[:1200]


def first_due(state):
    rows = [c for c in state["checkpoints"] if c["status"] == "PENDING"]
    return rows[0] if rows else None


def fired_row(state, cid):
    return next((c for c in state["checkpoints"] if c["id"] == cid), None)


def arrange_transfusion(ago=13, bed="Bed 12", dur=120):
    """T0 = now - ago min, so the 15-min check is between 1 and 2 min away."""
    state = hook("START_TRANSFUSION", ago=ago, bed=bed, dur=dur)
    due = first_due(state)
    if not due:
        raise RuntimeError(f"no pending checkpoint: {state}")
    return due


def assert_fired(due, name, timeout_extra=70):
    """Waits for the alarm screen, then checks the database timing (±60 s)."""
    wait_s = max(1, (due["scheduledAt"] - now_device()) / 1000) + timeout_extra
    if not wait_alarm(wait_s):
        screenshot(name)
        diagnose(name)
        return False, f"alarm screen not on top within {wait_s:.0f}s", None
    time.sleep(3)
    screenshot(name)
    state = hook("NOOP")
    row = fired_row(state, due["id"])
    if not row or row["status"] != "RINGING" or row["firedAt"] is None:
        return False, f"row not RINGING: {row}", state
    delta = (row["firedAt"] - row["scheduledAt"]) / 1000
    if abs(delta) > 60:
        return False, f"fired {delta:.0f}s from schedule", state
    return True, f"fired {delta:+.0f}s from schedule", state


class NA(Exception):
    pass


TESTS = []


def test(tid):
    def wrap(fn):
        TESTS.append((tid, fn))
        return fn
    return wrap


# ---------------------------------------------------------------- timing and phone states

@test("A01_timing_locked_keyguard")
def t_locked(name):
    set_pin()
    try:
        due = arrange_transfusion()
        sleep_screen()
        ok, msg, _ = assert_fired(due, name)
        up, info = keyguard_up()
        save(name, msg + "\n" + info)
        if not ok:
            return False, msg
        return up, msg + ("; alarm on top of the keyguard" if up else "; keyguard not reported as showing")
    finally:
        hook("ACK_ALL")
        unlock()
        clear_pin()


@test("A02_doze_force_idle")
def t_doze(name):
    # Android refuses deep idle while a wake-from-idle alarm is under min_time_to_alarm away (the
    # default is 30 min), so the setting is lowered: the alarm is set first, then the phone dozes.
    sh("settings put global device_idle_constants min_time_to_alarm=0")
    if api >= 31:
        sh("device_config put device_idle min_time_to_alarm 0")
    sh("dumpsys deviceidle enable deep")
    due = arrange_transfusion(ago=12)
    sh("dumpsys battery unplug")
    sleep_screen()
    time.sleep(3)
    idle = sh("dumpsys deviceidle force-idle deep")
    state = sh("dumpsys deviceidle get deep").strip()
    ok, msg, _ = assert_fired(due, name)
    save(name, f"{idle}\nstate before firing: {state}\n{msg}")
    if state != "IDLE":
        return False, f"device not in Doze ({state}); {msg}"
    return ok, msg + "; device was in deep Doze"


@test("A03_battery_saver")
def t_saver(name):
    sh("dumpsys battery unplug")
    sh("settings put global low_power 1")
    on = sh("settings get global low_power").strip()
    due = arrange_transfusion()
    sleep_screen()
    ok, msg, _ = assert_fired(due, name)
    save(name, f"low_power={on}\n{msg}")
    return ok and on == "1", msg + f"; low_power={on}"


@test("A04_do_not_disturb")
def t_dnd(name):
    set_dnd("priority")
    zen = sh("settings get global zen_mode").strip()
    due = arrange_transfusion()
    sleep_screen()
    ok, msg, _ = assert_fired(due, name)
    playing, lines = audio_alarm_playing()
    save(name, f"zen_mode={zen}\n{msg}\n{lines}")
    return ok and playing and zen != "0", f"{msg}; zen_mode={zen}; alarm stream playing={playing}"


def ringer_test(name, mode):
    hook("RINGER", mode=mode)
    ringer = sh("dumpsys audio | grep -i 'ringer mode' | head -3").strip()
    due = arrange_transfusion()
    sleep_screen()
    ok, msg, _ = assert_fired(due, name)
    playing, lines = audio_alarm_playing()
    vib, raw = vibrating()
    has_vibrator = hook("NOOP").get("hasVibrator", True)
    save(name, f"{ringer}\n{msg}\nAUDIO\n{lines}\nALL PLAYERS\n" + "\n".join(l.strip() for l in sh("dumpsys audio").splitlines() if "usage=" in l)[:1500] + f"\nVIBRATOR hasVibrator={has_vibrator}\n{raw}")
    if vib is None or not has_vibrator:
        vib_msg = "no vibrator on this emulator image: vibration N/A here"
        vib = True
    else:
        vib_msg = f"vibrating={vib}"
    return ok and playing and vib, f"{msg}; alarm stream playing={playing}; {vib_msg}"


@test("A05_silent_alarm_stream_and_vibration")
def t_silent(name):
    return ringer_test(name, "SILENT")


@test("A06_vibrate_alarm_stream_and_vibration")
def t_vibrate(name):
    return ringer_test(name, "VIBRATE")


# ---------------------------------------------------------------- process and package events

@test("A07_process_killed_am_kill")
def t_kill(name):
    due = arrange_transfusion()
    sh("input keyevent KEYCODE_HOME")
    time.sleep(1)
    sh(f"am kill {PKG}")
    time.sleep(2)
    gone = sh(f"pidof {PKG}").strip() == ""
    sleep_screen()
    ok, msg, _ = assert_fired(due, name)
    save(name, f"process gone after am kill: {gone}\n{msg}")
    return ok and gone, f"{msg}; process gone={gone}"


@test("A08_removed_from_recents")
def t_recents(name):
    due = arrange_transfusion()
    sh(f"am start -W -n {HOME}")
    time.sleep(2)
    # Real gesture: open Recents and fling the card away, then confirm the task is really gone.
    sh("input keyevent KEYCODE_APP_SWITCH")
    time.sleep(3)
    size = re.search(r"(\d+)x(\d+)", sh("wm size"))
    w, h = int(size.group(1)), int(size.group(2))
    sh(f"input swipe {w // 2} {h // 2} {w // 2} 20 200")
    time.sleep(2)
    sh("input keyevent KEYCODE_HOME")
    time.sleep(1)
    recents = sh("dumpsys activity recents")
    found = re.search(rf"\* Recent #\d+: Task\{{\w+ #(\d+)[^\n]*{re.escape(PKG)}", recents)
    how = "swipe"
    if found:
        # Same system call the launcher makes when a card is swiped away.
        sh(f"am stack remove {found.group(1)}")
        how = "am stack remove"
        time.sleep(2)
        recents = sh("dumpsys activity recents")
    still = re.search(rf"\* Recent #\d+:[^\n]*{re.escape(PKG)}", recents) is not None
    screenshot(name + "_after_swipe")
    sleep_screen()
    ok, msg, _ = assert_fired(due, name)
    save(name, recents[:3000] + "\n" + msg)
    if still:
        return False, f"task still in recents, so the swipe proved nothing; {msg}"
    return ok, msg + f"; task removed from recents ({how})"


@test("A09_force_stop_restored_on_open")
def t_force_stop(name):
    due = arrange_transfusion()
    sh(f"am force-stop {PKG}")
    pending_alarms = sh(f"dumpsys alarm | grep -c {PKG}").strip()
    wait = (due["scheduledAt"] - now_device()) / 1000 + 5
    time.sleep(max(1, wait))
    # Android cancels alarms by design: nothing may ring until the app is opened again.
    silent_while_stopped = not alarm_on_top()
    sh(f"am start -W -n {HOME}")
    fired = wait_ringing(30)
    time.sleep(2)
    screenshot(name)
    state = hook("NOOP")
    row = fired_row(state, due["id"])
    flagged = bool(row and row["status"] == "RINGING")
    save(name, f"alarm lines after force-stop: {pending_alarms}\nsilent while stopped: {silent_while_stopped}\nrow: {row}")
    return flagged and fired and silent_while_stopped, f"silent while stopped: {silent_while_stopped}; missed check flagged on open: {flagged}; ringing: {fired}"


@test("A10_app_update_install_r")
def t_update(name):
    due = arrange_transfusion()
    r = run(f"adb install -r {args.apk}", 180)
    if "Success" not in r:
        return False, f"install -r failed: {r[:200]}"
    grant()
    state = hook("NOOP")
    kept = fired_row(state, due["id"]) is not None
    sleep_screen()
    ok, msg, _ = assert_fired(due, name)
    save(name, r + "\n" + msg)
    return ok and kept, f"{msg}; data kept={kept}"


@test("A11_real_reboot_before_unlock")
def t_reboot(name):
    set_pin()
    try:
        # Long enough for the emulator to boot: the 15-min check rings about 5 min from now.
        due = arrange_transfusion(ago=10)
        time.sleep(30)  # let the permission grants reach the disk
        run("adb reboot", 60)
        time.sleep(5)
        wait_boot()
        root()
        armed = sh(f"dumpsys alarm | grep -c {PKG}").strip()
        sleep_screen()
        ok, msg, _ = assert_fired(due, name, timeout_extra=90)
        playing, _ = audio_alarm_playing()
        up, info = keyguard_up()
        vold = sh("getprop ro.crypto.type").strip()
        user_state = sh("dumpsys user | grep -i 'state:' | head -2").strip()
        state = hook("NOOP")
        row = fired_row(state, due["id"])
        diagnose(name)
        save(name, f"{msg}\narmed lines after boot: {armed}\nrow: {row}\nplaying: {playing}\ncrypto={vold}\n{user_state}\n{info}")
        return ok, f"{msg}; real reboot, never unlocked; {user_state.splitlines()[0] if user_state else ''}; alarm stream={playing}; row={row and row['status']}; crypto={vold or 'none'}"
    finally:
        try:
            hook("ACK_ALL")
        finally:
            unlock()
            clear_pin()


@test("A11b_real_reboot_after_unlock")
def t_reboot_unlocked(name):
    due = arrange_transfusion(ago=9)
    time.sleep(30)  # let the permission grants reach the disk
    run("adb reboot", 60)
    time.sleep(5)
    wait_boot()
    root()
    sh("wm dismiss-keyguard")
    time.sleep(2)
    sleep_screen()
    ok, msg, _ = assert_fired(due, name, timeout_extra=90)
    playing, _ = audio_alarm_playing()
    diagnose(name)
    return ok and playing, f"{msg}; real reboot, no PIN; alarm stream={playing}"


# ---------------------------------------------------------------- clock

@test("A12_clock_change")
def t_clock(name):
    t0 = device_time()
    due = arrange_transfusion(ago=0)
    sleep_screen()
    # 15-min check is about 15 min away. Moving the clock past it must ring at once.
    set_clock(t0 + dt.timedelta(minutes=16))
    fired = wait_alarm(60)
    time.sleep(2)
    screenshot(name)
    state = hook("NOOP")
    row = fired_row(state, due["id"])
    set_clock(dt.datetime.now(dt.timezone.utc).replace(tzinfo=None))
    save(name, f"{row}")
    return fired and bool(row and row["status"] == "RINGING"), f"after clock jump: alarm opened={fired}; row={row and row['status']}"


@test("A13_timezone_change")
def t_tz(name):
    original = sh("getprop persist.sys.timezone").strip()
    state = hook("START_PLAN", interval=60, bed="Bed 2")
    before = next(s for s in state["slots"] if s["status"] == "PENDING")
    tr = hook("START_TRANSFUSION", ago=0, bed="Bed 12", dur=120)
    tr_due = first_due(tr)["scheduledAt"]
    sh("service call alarm 3 s16 Asia/Kathmandu")  # +05:45: the hourly grid must move by 15 min
    time.sleep(6)
    zone = sh("getprop persist.sys.timezone").strip()
    state = hook("NOOP")
    pend = [s for s in state["slots"] if s["status"] == "PENDING"]
    tr_after = first_due(state)["scheduledAt"]
    sh(f"service call alarm 3 s16 {original or 'GMT'}")
    save(name, f"zone={zone}\nbefore={before}\nafter={pend[:2]}\ntr {tr_due}->{tr_after}")
    if zone != "Asia/Kathmandu":
        return False, f"timezone did not change on this image: {zone}"
    local = dt.datetime.utcfromtimestamp(pend[0]["slotAt"] / 1000) + dt.timedelta(hours=5, minutes=45)
    on_grid = local.minute == 0
    return on_grid and tr_after == tr_due, f"next slot {local:%H:%M} local (on the new grid: {on_grid}); transfusion check unchanged: {tr_after == tr_due}"


@test("A14_egypt_dst")
def t_dst(name):
    original = sh("getprop persist.sys.timezone").strip()
    # Egypt: last Friday of April, 00:00 jumps to 01:00. 30 Apr 2027 is such a Friday.
    sh("service call alarm 3 s16 Africa/Cairo")
    time.sleep(2)
    set_clock(dt.datetime(2027, 4, 29, 23, 58, 0))
    time.sleep(2)
    offset_before = sh("date +%z").strip()
    state = hook("START_PLAN", interval=60, bed="Bed 2")
    slot = next(s for s in state["slots"] if s["status"] == "PENDING")
    # The 00:00 slot doesn't exist that night; if this image's tzdata lacks Egypt's 2023 rule, N/A.
    set_clock(dt.datetime(2027, 4, 30, 2, 0, 0))
    time.sleep(2)
    offset_after = sh("date +%z").strip()
    hour = sh("date +%H").strip()
    save(name, f"{offset_before} {offset_after} {hour} {state}")
    sh(f"service call alarm 3 s16 {original or 'GMT'}")
    set_clock(dt.datetime.now(dt.timezone.utc).replace(tzinfo=None))
    if offset_before != "+0200":
        raise NA(f"cairo offset before transition is {offset_before}")
    if offset_after != "+0300":
        raise NA(f"this image's tzdata has no Egypt DST rule (offset {offset_after} on 30 Apr 2027, hour {hour}); the rule is covered by SchedulePropertyTest on the JVM")
    slot_local = dt.datetime.utcfromtimestamp(slot["slotAt"] / 1000) + dt.timedelta(hours=3)
    return slot_local.hour == 1 and slot_local.minute == 0, f"first slot after the gap: {slot_local:%H:%M} (+03:00)"


# ---------------------------------------------------------------- warnings

def home_text():
    sh(f"am start -W -n {HOME}")
    time.sleep(3)
    for _ in range(4):
        raw = sh("uiautomator dump /sdcard/lab.xml >/dev/null 2>&1; cat /sdcard/lab.xml")
        if "<hierarchy" in raw:
            return raw
        time.sleep(2)
    return ""


def skip_first_run():
    """The first launch opens Setup; going back lands on Home."""
    sh(f"am start -W -n {HOME}")
    time.sleep(3)
    sh("input keyevent KEYCODE_BACK")
    time.sleep(1)


WARN = r"Setup check: \d+ item"


@test("A15_permissions_revoked_warns")
def t_perm(name):
    if api < 31:
        raise NA("no revocable notification, exact-alarm or full-screen permission below API 31 by adb")
    done = []
    skip_first_run()
    if api >= 33:
        sh(f"pm revoke {PKG} android.permission.POST_NOTIFICATIONS")
        done.append("notifications")
    if api in (31, 32):
        sh(f"appops set {PKG} SCHEDULE_EXACT_ALARM deny")
        done.append("exact alarms")
    if api >= 34:
        sh(f"appops set {PKG} USE_FULL_SCREEN_INTENT deny")
        done.append("full-screen intent")
    time.sleep(1)
    xml = home_text()
    screenshot(name)
    save(name, xml)
    warned = re.search(WARN, xml) is not None
    return warned, f"revoked {done}; Home warning shown={warned}"


@test("A16_alarm_volume_zero_warns")
def t_volume(name):
    skip_first_run()
    set_stream_volume(4, 0)
    level = sh("dumpsys audio | grep -A3 'STREAM_ALARM' | head -5")
    xml = home_text()
    screenshot(name)
    save(name, level + "\n" + xml)
    set_stream_volume(4, 5)
    warned = re.search(WARN, xml) is not None
    return warned, f"Home warning shown={warned}"


# ---------------------------------------------------------------- not built yet

@test("A17_pocket_guard_first_second")
def t_guard_second(name):
    return False, "not in this build: touches are accepted from the first frame"


@test("A18_pocket_guard_proximity")
def t_guard_prox(name):
    return False, "not in this build: no proximity guard"


@test("A19_undo_snackbar_and_undo_last")
def t_undo(name):
    return False, "not in this build: acknowledgements cannot be undone"


# ---------------------------------------------------------------- collisions and follow-ups

@test("A20_collision_same_minute_one_screen")
def t_collision(name):
    t = device_time()
    set_clock(t.replace(minute=59, second=0))
    state = hook("START_PLAN", interval=60, bed="Bed 2")
    state = hook("START_TRANSFUSION", ago=14, bed="Bed 12", dur=120)
    due = first_due(state)
    slot = next(s for s in state["slots"] if s["status"] == "PENDING")
    same = due["scheduledAt"] == slot["slotAt"]
    sleep_screen()
    ok, msg, state = assert_fired(due, name, timeout_extra=40)
    xml = sh("uiautomator dump /sdcard/lab.xml >/dev/null 2>&1; cat /sdcard/lab.xml")
    save(name, xml)
    both = "Bed 12" in xml and "Bed 2" in xml
    set_clock(dt.datetime.now(dt.timezone.utc).replace(tzinfo=None))
    return ok and same and both, f"{msg}; same minute={same}; both beds on one screen={both}"


@test("A21_missed_timeout_overdue_notification")
def t_missed(name):
    due = arrange_transfusion()
    sleep_screen()
    ok, msg, _ = assert_fired(due, name)
    if not ok:
        return False, msg
    hook("SET_MISSED_TIMEOUT", min=5)
    # Move the clock past the 5-min timer rather than waiting; the armed timer must close the check.
    t = device_time()
    set_clock(t + dt.timedelta(minutes=11))
    time.sleep(20)
    state = hook("NOOP")
    row = fired_row(state, due["id"])
    notif = sh("dumpsys notification --noredact")
    overdue = "Overdue" in notif
    playing, _ = audio_alarm_playing()
    screenshot(name + "_overdue")
    save(name, f"{row}\n{notif[:6000]}")
    set_clock(dt.datetime.now(dt.timezone.utc).replace(tzinfo=None))
    good = bool(row and row["status"] == "MISSED") and overdue and not playing
    return good, f"row={row and row['status']}; Overdue notification={overdue}; sound stopped={not playing}"


@test("A22_notification_action_ack_syncs")
def t_notif_action(name):
    due = arrange_transfusion()
    # Phone in use: unlocked. The heads-up carries the action button.
    sh(f"am start -W -n {HOME}")
    wait_s = max(1, (due["scheduledAt"] - now_device()) / 1000) + 70
    if not wait_ringing(wait_s):
        screenshot(name)
        diagnose(name)
        return False, f"no alarm sound within {wait_s:.0f}s"
    screenshot(name + "_heads_up")
    sh("input keyevent KEYCODE_HOME")
    time.sleep(1)
    sh("cmd statusbar expand-notifications")
    time.sleep(2)
    xml = sh("uiautomator dump /sdcard/lab.xml >/dev/null 2>&1; cat /sdcard/lab.xml")
    screenshot(name + "_shade")
    save(name, xml)
    m = re.search(r'text="([^"]*(?:MEASURED|Measured)[^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if not m:
        sh("cmd statusbar collapse")
        return False, "notification action button not found in the shade"
    x, y = (int(m.group(2)) + int(m.group(4))) // 2, (int(m.group(3)) + int(m.group(5))) // 2
    sh(f"input tap {x} {y}")
    time.sleep(4)
    sh("cmd statusbar collapse")
    state = hook("NOOP")
    row = fired_row(state, due["id"])
    return bool(row and row["status"] == "ACKED"), f"tapped '{m.group(1)}'; row={row and row['status']}"


@test("A23_round_alarm_locked_own_sound")
def t_round(name):
    set_pin()
    try:
        t = device_time()
        set_clock(t.replace(minute=59, second=0))
        state = hook("START_PLAN", interval=60, bed="Bed 2")
        hook("START_PLAN", interval=60, bed="Bed 3")
        slot = next(s for s in state["slots"] if s["status"] == "PENDING")
        sleep_screen()
        fired = wait_alarm(90)
        time.sleep(3)
        screenshot(name)
        playing, lines = audio_alarm_playing()
        xml = sh("uiautomator dump /sdcard/lab.xml >/dev/null 2>&1; cat /sdcard/lab.xml")
        state = hook("NOOP")
        ringing = [s for s in state["slots"] if s["status"] == "RINGING"]
        delta = (ringing[0]["firedAt"] - ringing[0]["slotAt"]) / 1000 if ringing else 9999
        up, _ = keyguard_up()
        save(name, f"{lines}\n{xml}")
        good = fired and playing and len(ringing) == 2 and abs(delta) <= 60 and "Turning round" in xml
        return good, f"fired {delta:+.0f}s; one alarm for {len(ringing)} beds; alarm stream={playing}; keyguard={up}"
    finally:
        try:
            hook("START_ROUND")
            hook("TURN_ALL")
        finally:
            unlock()
            clear_pin()
            set_clock(dt.datetime.now(dt.timezone.utc).replace(tzinfo=None))


# ---------------------------------------------------------------- driver

def main():
    global args, api, out
    p = argparse.ArgumentParser()
    p.add_argument("--apk", required=True)
    p.add_argument("--out", required=True)
    p.add_argument("--only", default="")
    args = p.parse_args()
    out = pathlib.Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    root()
    api = int(sh("getprop ro.build.version.sdk").strip())
    print(f"API {api}")
    run("adb logcat -G 16M")
    print(run(f"adb install -r -g {args.apk}", 300))
    sh("settings put global auto_time 0")
    sh("settings put global auto_time_zone 0")
    sh("settings put global window_animation_scale 0")
    for tid, fn in TESTS:
        if args.only and not re.search(args.only, tid):
            continue
        started = time.time()
        status, detail = "FAIL", ""
        try:
            reset()
            ok, detail = fn(tid)
            status = "PASS" if ok else "FAIL"
        except NA as e:
            status, detail = "N/A", str(e)
        except Exception as e:
            detail = f"{type(e).__name__}: {e}"
            (out / f"{tid}.exception.txt").write_text(traceback.format_exc())
        results.append({"id": tid, "api": api, "status": status, "detail": detail, "seconds": round(time.time() - started)})
        print(f"[{status}] {tid}: {detail}", flush=True)
        (out / "results.json").write_text(json.dumps(results, indent=1))
        run(f"adb logcat -d -t 400 > {out / (tid + '.logcat.txt')}", 60)
    print(json.dumps({k: sum(1 for r in results if r["status"] == k) for k in ("PASS", "FAIL", "N/A")}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
