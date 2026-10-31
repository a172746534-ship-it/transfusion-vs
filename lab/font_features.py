#!/usr/bin/env python3
"""Lists the OpenType features of the given fonts: we need tabular figures, slashed zero if any."""
import sys
from fontTools.ttLib import TTFont

for path in sys.argv[1:]:
    f = TTFont(path)
    feats = sorted({r.FeatureTag for r in f["GSUB"].table.FeatureList.FeatureRecord}) if "GSUB" in f else []
    axes = [(a.axisTag, a.minValue, a.maxValue) for a in f["fvar"].axes] if "fvar" in f else []
    print(path.split("/")[-1].split("\\")[-1], "axes", axes, "tnum" in feats, "zero" in feats, "ss", [x for x in feats if x.startswith(("ss", "cv"))][:10])
