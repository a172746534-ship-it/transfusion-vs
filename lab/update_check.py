#!/usr/bin/env python3
"""The update check of docs/CUSTOM_TIMER.md §9, on one emulator, with the PUBLISHED release files.

Install v0.5.0, start a transfusion and a turning plan through the real screens (a release build has
no debug hooks), install the new release over it, and check that both are still running and that
their alarms still fire. Uses the lab's own adb helpers from reliability.py. Writes results.json with
the evidence next to it, the same shape as the reliability suite.
"""
import argparse
import datetime as dt
import json
import pathlib
import re
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import reliability as L  # noqa: E402

PKG = L.PKG
TX_MRN = "7654812"
TURN_MRN = "7654802"


def node_centre(pattern, xml, last=False):
    hits = list(re.finditer(rf'text="[^"]*{pattern}[^"]*"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml))
    if not hits:
        return None
    m = hits[-1] if last else hits[0]
    return (int(m.group(1)) + int(m.group(3))) // 2, (int(m.group(2)) + int(m.group(4))) // 2


def wait_text(pattern, timeout=40):
    end = time.time() + timeout
    while time.time() < end:
        xml = L.dump_xml()
        if re.search(pattern, xml):
            return xml
        time.sleep(1.5)
    raise RuntimeError(f"'{pattern}' never showed")


def tap(pattern, last=False, timeout=40):
    xml = wait_text(pattern, timeout)
    xy = node_centre(pattern, xml, last)
    L.sh(f"input tap {xy[0]} {xy[1]}")
    time.sleep(2)


def hide_keyboard():
    """Only when it shows: Back with no keyboard open would leave the screen."""
    if "mInputShown=true" in L.sh("dumpsys input_method"):
        L.sh("input keyevent KEYCODE_BACK")
        time.sleep(1.5)


def version():
    m = re.search(r"versionName=(\S+)", L.sh(f"dumpsys package {PKG}"))
    return m.group(1) if m else "?"


def open_home():
    L.sh(f"am start -W -n {L.HOME}")
    time.sleep(3)


def next_turn_slot(local: dt.datetime) -> dt.datetime:
    """The default plan: every 2 h on the ward clock; less than 30 min away means the slot after."""
    slot = local.replace(minute=0, second=0, microsecond=0)
    slot += dt.timedelta(hours=1 if slot.hour % 2 else 2)
    if slot - local < dt.timedelta(minutes=30):
        slot += dt.timedelta(hours=2)
    return slot


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--old", required=True)
    p.add_argument("--new", required=True)
    p.add_argument("--out", required=True)
    args = p.parse_args()
    L.out = pathlib.Path(args.out)
    L.out.mkdir(parents=True, exist_ok=True)
    L.root()
    L.api = int(L.sh("getprop ro.build.version.sdk").strip())
    L.sh("settings put global auto_time 0")
    L.sh("settings put global auto_time_zone 0")
    L.sh("settings put global window_animation_scale 0")
    steps = []

    def step(text):
        print(text, flush=True)
        steps.append(text)

    # ---- v0.5.0, as a nurse has it today
    step(L.run(f"adb install -r -g {args.old}", 300).strip().splitlines()[-1])
    L.grant()
    old_version = version()
    step(f"installed {old_version}")
    open_home()
    L.sh("input keyevent KEYCODE_BACK")  # the first launch opens Setup
    time.sleep(2)
    open_home()
    started = L.device_time()

    tap("Start transfusion")
    time.sleep(1)
    L.sh(f"input text {TX_MRN}")
    hide_keyboard()
    tap("Baseline done")
    wait_text(TX_MRN)
    step(f"transfusion {TX_MRN} started at {started:%H:%M}")

    tap("Start turning")
    time.sleep(1)
    L.sh(f"input text {TURN_MRN}")
    wait_text("lying now")
    hide_keyboard()
    tap("Back", last=True)  # the position button; "Back" is also the header's
    xml = wait_text(TURN_MRN)
    L.screenshot("1_before_update")
    L.save("1_before_update", xml)
    alarms_before = L.sh(f"dumpsys alarm | grep -c {PKG}").strip()
    step(f"turning plan {TURN_MRN} started; alarm lines: {alarms_before}")

    # ---- the new release over it
    step(L.run(f"adb install -r {args.new}", 300).strip().splitlines()[-1])
    L.grant()
    new_version = version()
    step(f"updated {old_version} -> {new_version}")
    open_home()
    xml = L.dump_xml()
    L.screenshot("2_after_update")
    L.save("2_after_update", xml)
    kept = TX_MRN in xml and TURN_MRN in xml
    alarms_after = L.sh(f"dumpsys alarm | grep -c {PKG}").strip()
    step(f"after the update: both on Home={kept}; alarm lines: {alarms_after}")

    # ---- the transfusion's 15-min check still rings
    L.sleep_screen()
    L.set_clock(started + dt.timedelta(minutes=16))
    tx_rang = L.wait_alarm(90)
    time.sleep(3)
    xml = L.dump_xml()
    L.screenshot("3_transfusion_alarm")
    L.save("3_transfusion_alarm", xml)
    tx_rang = tx_rang and "15-min check" in xml
    step(f"transfusion 15-min check rang after the update: {tx_rang}")
    xy = node_centre("Measured", xml)
    if xy:
        L.sh(f"input tap {xy[0]} {xy[1]}")
        time.sleep(7)

    # ---- the turning plan's first slot still rings
    slot = next_turn_slot(started)
    L.sleep_screen()
    L.set_clock(slot + dt.timedelta(minutes=1))
    turn_rang = L.wait_alarm(90)
    time.sleep(3)
    xml = L.dump_xml()
    L.screenshot("4_turning_alarm")
    L.save("4_turning_alarm", xml)
    turn_rang = turn_rang and ("Start round" in xml or "Turning round" in xml)
    step(f"turning round for the {slot:%H:%M} slot rang after the update: {turn_rang}")

    L.set_clock(dt.datetime.now(dt.timezone.utc).replace(tzinfo=None))
    ok = kept and tx_rang and turn_rang and new_version != old_version
    result = {"id": "U01_update_over_v0_5_0", "api": L.api, "status": "PASS" if ok else "FAIL", "detail": "; ".join(steps)}
    (L.out / "results.json").write_text(json.dumps([result], indent=1))
    print(json.dumps(result, indent=1))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
