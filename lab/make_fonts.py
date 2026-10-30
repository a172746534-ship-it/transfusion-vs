#!/usr/bin/env python3
"""Cuts static weights from variable OFL fonts (the Android font loader and the screenshot renderer
need one file per weight) and drops the rest. Usage: make_fonts.py <raw dir> <res/font dir>"""
import pathlib
import sys
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

raw, out = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
out.mkdir(parents=True, exist_ok=True)

JOBS = {
    "redhatdisplay_RedHatDisplay[wght].ttf": ("rh_display", {"bold": 700, "black": 900}),
    "redhattext_RedHatText[wght].ttf": ("rh_text", {"regular": 400, "bold": 700}),
    "figtree_Figtree[wght].ttf": ("figtree", {"regular": 400, "bold": 700, "black": 900}),
    "lexend_Lexend[wght].ttf": ("lexend", {"regular": 400, "bold": 700, "black": 900}),
    "jetbrainsmono_JetBrainsMono[wght].ttf": ("jetbrains", {"bold": 700, "black": 800}),
}
for src, (stem, weights) in JOBS.items():
    for name, wght in weights.items():
        font = TTFont(raw / src)
        feats = {r.FeatureTag for r in font["GSUB"].table.FeatureList.FeatureRecord} if "GSUB" in font else set()
        static = instancer.instantiateVariableFont(font, {"wght": wght}, inplace=False)
        target = out / f"{stem}_{name}.ttf"
        static.save(target)
        print(target.name, target.stat().st_size, "tnum" in feats, "zero" in feats)

for src, name in {
    "barlowsemicondensed_BarlowSemiCondensed-Regular.ttf": "barlow_regular",
    "barlowsemicondensed_BarlowSemiCondensed-Medium.ttf": "barlow_medium",
    "barlowsemicondensed_BarlowSemiCondensed-Bold.ttf": "barlow_bold",
}.items():
    (out / f"{name}.ttf").write_bytes((raw / src).read_bytes())
    print(name)
