#!/usr/bin/env python3
"""Counts StrictMode violations from a logcat, for this app's processes only: other apps on the image
(YouTube, for one) log their own and they are not ours. Prints two lines:
  strictmode: N            everything in this app's process, except:
  strictmode_framework: M  SurfaceControl objects Android itself leaks (finalizer stack, no frame of this
                           app or its libraries; the app never touches SurfaceControl).
Usage: strictmode_count.py <logcat> <package>"""
import re
import sys

text = open(sys.argv[1], errors="replace").read().splitlines()
pkg = sys.argv[2]
pids = set(re.findall(rf"Start proc (\d+):{re.escape(pkg)}(?:[/\s]|$)", "\n".join(text), re.M))
blocks = []
for line in text:
    m = re.match(r"\S+ \S+\s+(\d+)\s+\d+ D StrictMode: (.*)", line)
    if not m or m.group(1) not in pids:
        continue
    if "StrictMode policy violation" in m.group(2):
        blocks.append([m.group(2)])
    elif blocks:
        blocks[-1].append(m.group(2).strip())


def framework_only(b):
    frames = [f for f in b[1:] if f.startswith("at ")]
    return any("android.view.SurfaceControl.finalize" in f for f in frames) and not any(re.match(r"at (app\.transfusionvs|androidx\.|kotlin)", f) for f in frames)


own = [b for b in blocks if not framework_only(b)]
print(f"strictmode: {len(own)}")
print(f"strictmode_framework: {len(blocks) - len(own)}")
for b in own[:10]:
    print("  " + b[0][:150])
    for f in [x for x in b[1:] if x.startswith("at ")][:6]:
        print("    " + f[:110])
