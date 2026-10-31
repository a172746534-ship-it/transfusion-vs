#!/usr/bin/env bash
# Instrumented UI checks, monkey, leaks, StrictMode, cold start, rotation and process death.
set -u
API=$1
OUT=$2
PKG=app.transfusionvs
HOME_ACT=$PKG/.ui.MainActivity
mkdir -p "$OUT"
adb root >/dev/null 2>&1; sleep 2; adb wait-for-device
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard

grant() {
  [ "$API" -ge 33 ] && adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS
  [ "$API" -ge 34 ] && adb shell appops set $PKG USE_FULL_SCREEN_INTENT allow
  true
}

# --- Instrumented UI checks: tap budget, ATF, touch targets, text sizes, truncation -------------
adb uninstall $PKG >/dev/null 2>&1
adb install -g apks/debug/app-debug.apk
adb install -r -t apks/androidTest/debug/app-debug-androidTest.apk
grant

run_ui() { # name font_scale width_dp
  local name=$1 scale=$2 dp=$3
  adb shell settings put system font_scale "$scale"
  adb shell wm size reset
  local px; px=$(adb shell wm size | sed -n 's/.*: \([0-9]*\)x.*/\1/p' | tail -1 | tr -d '\r')
  adb shell wm density $(( px * 160 / dp ))
  sleep 3
  adb logcat -c
  adb shell am instrument -w -r -e class app.transfusionvs.uicheck.UiChecks \
    $PKG.test/androidx.test.runner.AndroidJUnitRunner > "$OUT/ui_$name.txt" 2>&1
  adb logcat -d -s UICHECK:I > "$OUT/ui_$name.findings.txt"
  adb shell wm density reset
  adb shell settings put system font_scale 1.0
}
run_ui w412_f10 1.0 412
run_ui w360_f20 2.0 360
run_ui w360_f13 1.3 360
adb shell am instrument -w -r -e class app.transfusionvs.uicheck.TapBudget \
  $PKG.test/androidx.test.runner.AndroidJUnitRunner > "$OUT/tap_budget.txt" 2>&1
adb logcat -d -s TAPS:I > "$OUT/tap_budget.findings.txt"
# The budget also holds on a 360 dp phone.
px=$(adb shell wm size | sed -n 's/.*: \([0-9]*\)x.*/\1/p' | tail -1 | tr -d '\r')
adb shell wm density $(( px * 160 / 360 )); sleep 3
adb logcat -c
adb shell am instrument -w -r -e class app.transfusionvs.uicheck.TapBudget \
  $PKG.test/androidx.test.runner.AndroidJUnitRunner > "$OUT/tap_budget_w360.txt" 2>&1
adb logcat -d -s TAPS:I > "$OUT/tap_budget_w360.findings.txt"
adb shell wm density reset
adb logcat -c
adb shell am instrument -w -r -e class app.transfusionvs.uicheck.Robustness \
  $PKG.test/androidx.test.runner.AndroidJUnitRunner > "$OUT/robust.txt" 2>&1
adb logcat -d -s ROBUST:I > "$OUT/robust.findings.txt"
adb shell "cat /sdcard/Android/data/$PKG/files/widget.png" > "$OUT/widget.png" 2>/dev/null

# --- Rotation and process death mid-flow: the picked bed must survive ---------------------------
adb shell pm clear $PKG; grant
adb shell am start -W -n $HOME_ACT >/dev/null; sleep 3
adb shell input keyevent KEYCODE_BACK; sleep 1   # leave first-run Setup
dump() { adb shell "uiautomator dump /sdcard/lab.xml >/dev/null 2>&1; cat /sdcard/lab.xml"; }
tap_text() { # taps the first node whose text matches
  local xy
  xy=$(dump | python3 -c "
import re,sys
m=re.search(r'text=\"$1\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"',sys.stdin.read())
print((int(m[1])+int(m[3]))//2,(int(m[2])+int(m[4]))//2) if m else print('')")
  [ -n "$xy" ] && adb shell input tap $xy && sleep 1
}
adb shell am broadcast -n $PKG/.lab.LabReceiver -a lab.ADD_BEDS --es beds "75-78" -f 0x20 > /dev/null; sleep 1
tap_text "Start transfusion"; tap_text "Bed 77"
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1; sleep 3
if dump | grep -q '✓ Bed 77'; then echo "PASS rotation keeps the picked bed"; else echo "FAIL rotation lost the picked bed"; fi | tee "$OUT/rotation.txt"
adb shell settings put system user_rotation 0; sleep 2
adb shell input keyevent KEYCODE_HOME; sleep 2
adb shell am kill $PKG; sleep 2
adb shell am start -W -n $HOME_ACT >/dev/null; sleep 3
if dump | grep -q '✓ Bed 77'; then echo "PASS process death keeps the picked bed"; else echo "FAIL process death lost the picked bed"; fi | tee "$OUT/process_death.txt"

# --- Monkey (debug build: LeakCanary and StrictMode are on) --------------------------------------
adb shell pm clear $PKG; grant
adb shell input keyevent KEYCODE_HOME; sleep 20   # let the launcher settle: its own ANRs are not ours
adb logcat -c
# --ignore-*: a launcher ANR on a slow emulator must not end the run. Crashes and ANRs of this app are
# still counted from the log below, and that count must be zero.
adb shell monkey -p $PKG --throttle 25 --pct-syskeys 0 --pct-anyevent 0 --ignore-crashes --ignore-timeouts -s 4242 -v 20000 > "$OUT/monkey.txt" 2>&1
tail -3 "$OUT/monkey.txt"
adb logcat -d > "$OUT/monkey.logcat.txt"
echo "crashes: $(grep -A2 'FATAL EXCEPTION' "$OUT/monkey.logcat.txt" | grep -c "Process: $PKG")" | tee "$OUT/monkey.summary.txt"
echo "anrs: $(grep -c 'ANR in app.transfusionvs' "$OUT/monkey.logcat.txt")" | tee -a "$OUT/monkey.summary.txt"
echo "strictmode: $(grep -c 'StrictMode policy violation' "$OUT/monkey.logcat.txt")" | tee -a "$OUT/monkey.summary.txt"
grep -c 'Monkey finished' "$OUT/monkey.txt" | sed 's/^/monkey finished: /' | tee -a "$OUT/monkey.summary.txt"
grep 'Events injected' "$OUT/monkey.txt" | tee -a "$OUT/monkey.summary.txt"
# Force the heap analysis: LeakCanary alone waits for 5 retained objects, so silence would prove nothing.
adb logcat -c
adb shell am broadcast -n $PKG/.lab.LabReceiver -a lab.DUMP_HEAP -f 0x20 > /dev/null
: > "$OUT/leaks.txt"
for i in $(seq 1 40); do
  sleep 6
  if adb logcat -d -s LeakCanary:D | grep -q 'APPLICATION LEAKS'; then break; fi
done
adb logcat -d -s LeakCanary:D > "$OUT/leaks.full.txt"
grep -E 'HEAP ANALYSIS RESULT|[0-9]+ APPLICATION LEAKS|[0-9]+ LIBRARY LEAKS|Analysis failed|Heap dump' "$OUT/leaks.full.txt" | sed -E 's/^.*LeakCanary: //' | head -8 | tee "$OUT/leaks.txt"
adb shell am force-stop $PKG

# --- Cold start: non-debuggable build, median of 10 am start -W ----------------------------------
adb uninstall $PKG >/dev/null 2>&1
adb install -g apks/lab/app-lab.apk
grant
adb shell am start -W -n $HOME_ACT >/dev/null; sleep 3; adb shell input keyevent KEYCODE_BACK; sleep 1
: > "$OUT/cold_start.txt"
for i in 1 2 3 4 5 6 7 8 9 10; do
  adb shell am force-stop $PKG; sleep 2
  adb shell am start -W -n $HOME_ACT | tr -d '\r' | grep TotalTime | tee -a "$OUT/cold_start.txt"
  sleep 2
done
python3 - "$OUT/cold_start.txt" <<'EOF' | tee -a "$OUT/cold_start.txt"
import re,statistics,sys
v=[int(x) for x in re.findall(r'TotalTime: (\d+)',open(sys.argv[1]).read())]
print("median_ms",statistics.median(v) if v else None,"runs",len(v))
EOF
adb uninstall $PKG >/dev/null 2>&1
exit 0
