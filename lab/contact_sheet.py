#!/usr/bin/env python3
"""Tiles screenshots into labelled contact sheets, so every one really gets looked at.
Usage: contact_sheet.py <out.png> <columns> <cell width> <image or dir> [...]
       A dir contributes its *.png sorted by name. Each cell is captioned with the file name."""
import pathlib
import sys
from PIL import Image, ImageDraw

out, cols, width = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
files = []
for a in sys.argv[4:]:
    p = pathlib.Path(a)
    files += sorted(p.glob("*.png")) if p.is_dir() else [p]
cells = []
for f in files:
    im = Image.open(f).convert("RGB")
    h = int(im.height * width / im.width)
    cells.append((f.stem, im.resize((width, h), Image.LANCZOS)))
rows = [cells[i:i + cols] for i in range(0, len(cells), cols)]
gap, cap = 10, 18
H = sum(max(c[1].height for c in r) + cap + gap for r in rows) + gap
W = cols * (width + gap) + gap
sheet = Image.new("RGB", (W, H), "#777777")
d = ImageDraw.Draw(sheet)
y = gap
for r in rows:
    for i, (name, im) in enumerate(r):
        x = gap + i * (width + gap)
        d.text((x, y), name[:int(width / 6)], fill="white")
        sheet.paste(im, (x, y + cap))
    y += max(c[1].height for c in r) + cap + gap
sheet.save(out)
print(out, sheet.size, len(cells))
