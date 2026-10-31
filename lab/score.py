#!/usr/bin/env python3
"""Reads a downloaded lab run and prints the evidence tables for SCORECARD.md.
Usage: score.py <dir holding one folder per artifact, each with api<N>/...>

Area A rule (docs/TEST_LOOP.md §5): a check passes only if it passes on every API of the matrix.
N/A is allowed on a level where Android has no such behaviour, but a check that was N/A everywhere
proved nothing, so it does not pass."""
import json
import pathlib
import re
import sys

sys.stdout.reconfigure(encoding="utf-8")
root = pathlib.Path(sys.argv[1])
MATRIX = [26, 29, 31, 33, 34, 36]
POINTS = {
    "A01_timing_locked_keyguard": 2, "A02_doze_force_idle": 1, "A03_battery_saver": 1, "A04_do_not_disturb": 1,
    "A05_silent_alarm_stream_and_vibration": 1, "A06_vibrate_alarm_stream_and_vibration": 1, "A07_process_killed_am_kill": 1,
    "A08_removed_from_recents": 1, "A09_force_stop_restored_on_open": 1, "A10_app_update_install_r": 1,
    "A11_real_reboot_before_unlock": 1, "A11b_real_reboot_after_unlock": 1, "A12_clock_change": 1, "A13_timezone_change": 1,
    "A14_egypt_dst": 1, "A15_permissions_revoked_warns": 1, "A16_alarm_volume_zero_warns": 1, "A17_pocket_guard_first_second": 1,
    "A18_pocket_guard_proximity": 1, "A19_undo_snackbar_and_undo_last": 1, "A20_collision_same_minute_one_screen": 1,
    "A21_missed_timeout_overdue_notification": 1, "A22_notification_action_ack_syncs": 1, "A23_round_alarm_locked_own_sound": 1,
}
# Must pass to merge; worth no points, the 100 stay as the spec split them (docs/MRN_ADDON.md).
GATES = ["M01_mrn_masked_outside_the_app"]
results = {}
for path in root.rglob("reliability/results.json"):
    for r in json.loads(path.read_text(encoding="utf-8")):
        results.setdefault(r["id"], {})[r["api"]] = r
apis = sorted({a for per in results.values() for a in per})
print("APIs with results:", apis, "· matrix:", MATRIX)
print("\n| Check | Pts | " + " | ".join(str(a) for a in MATRIX) + " | Result |")
print("|---|---|" + "---|" * len(MATRIX) + "---|")
total = 0
for cid, pts in POINTS.items():
    per = results.get(cid, {})
    cells = [per[a]["status"] if a in per else "not run" for a in MATRIX]
    ok = all(c in ("PASS", "N/A") for c in cells) and "PASS" in cells
    total += pts if ok else 0
    print(f"| {cid} | {pts} | " + " | ".join(cells) + f" | {'PASS' if ok else 'FAIL'} |")
print(f"\nArea A: {total}/{sum(POINTS.values())}\n")
for cid in POINTS:
    for a, r in sorted(results.get(cid, {}).items()):
        if r["status"] != "PASS":
            print(f"- {cid} API {a} {r['status']}: {r['detail'][:230]}")

print("Gates:")
for cid in GATES:
    print(" ", cid, {a: r["status"] for a, r in sorted(results.get(cid, {}).items())}, next((r["detail"][:200] for r in results.get(cid, {}).values() if r["status"] != "PASS"), ""))

print("\n--- robustness per API ---")
for rob in sorted(root.rglob("robustness")):
    api = re.search(r"api(\d+)", str(rob)).group(1)
    print(f"\nAPI {api}")
    for name in ("monkey.summary.txt", "rotation.txt", "process_death.txt"):
        f = rob / name
        print(" ", name, "::", f.read_text(errors="replace").replace("\n", " ; ")[:200] if f.exists() else "missing")
    f = rob / "cold_start.txt"
    if f.exists():
        print("  cold start ::", (f.read_text(errors="replace").strip().splitlines() or ["empty"])[-1])
    f = rob / "leaks.txt"
    if f.exists():
        print("  leaks ::", f.read_text(errors="replace").replace("\n", " ; ")[:240] or "no LeakCanary report")
    f = rob / "tap_budget.findings.txt"
    if f.exists():
        print("  taps ::", " ".join(sorted(re.findall(r"task=(\d+ taps=\d+) typing=\w+ budget=\d+ (\w+)", f.read_text(errors="replace")) and [m[0].replace(" taps=", ":") + m[1][0] for m in re.findall(r"task=(\d+ taps=\d+) typing=\w+ budget=\d+ (\w+)", f.read_text(errors="replace"))])))
    f = rob / "robust.findings.txt"
    if f.exists():
        print("  double tap, undo, widget ::", " ".join(sorted(set(re.findall(r"ROBUST *: *(\w+ PASS)", f.read_text(errors="replace"))))))
    for f in sorted(rob.glob("ui_*.findings.txt")):
        text = f.read_text(errors="replace")
        counts = {}
        for kind in ("TOUCH", "NOLABEL", "SPACING", "TRUNCATED", "MIDWORD", "SMALLTEXT", "PRIMARY", "ATF"):
            counts[kind] = len(set(re.findall(rf"UICHECK *: *({kind} .*)", text)))
        print(" ", f.name.replace(".findings.txt", ""), counts)
