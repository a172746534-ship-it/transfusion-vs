#!/usr/bin/env python3
"""Side-by-side comparison of the three directions for the PR.
Usage: directions_sheet.py <dir with direction__screen__theme.png> <out dir>"""
import pathlib
import sys
from PIL import Image, ImageDraw, ImageFont

src, out = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
out.mkdir(parents=True, exist_ok=True)
DIRS = [("ward_card", "1 · Ward Card"), ("halo", "2 · Halo"), ("ward_board", "3 · Ward Board")]
SCREENS = [("home", "Home"), ("alarm", "Alarm"), ("round", "Round alarm")]
W, GAP, HEAD = 400, 24, 64
try:
    font = ImageFont.truetype("arialbd.ttf", 30)
except OSError:
    font = ImageFont.load_default()


def sheet(theme, bg, fg):
    probe = Image.open(src / f"ward_card__home__{theme}.png")
    h = int(probe.height * W / probe.width)
    canvas = Image.new("RGB", (GAP + 3 * (W + GAP), HEAD + 3 * (h + GAP)), bg)
    d = ImageDraw.Draw(canvas)
    for c, (name, label) in enumerate(DIRS):
        x = GAP + c * (W + GAP)
        d.text((x, 16), label, fill=fg, font=font)
        for r, (screen, _) in enumerate(SCREENS):
            im = Image.open(src / f"{name}__{screen}__{theme}.png").convert("RGB").resize((W, h), Image.LANCZOS)
            canvas.paste(im, (x, HEAD + r * (h + GAP)))
    canvas.quantize(colors=240, dither=Image.Dither.NONE).save(out / f"compare_{theme}.png", optimize=True)


sheet("day", "#FFFFFF", "#111111")
sheet("night", "#000000", "#EEEEEE")
for name, label in DIRS:
    for screen, _ in SCREENS:
        for theme in ("day", "night"):
            im = Image.open(src / f"{name}__{screen}__{theme}.png").convert("RGB")
            im = im.resize((im.width // 2, im.height // 2), Image.LANCZOS)
            im.quantize(colors=200, dither=Image.Dither.NONE).save(out / f"{name}__{screen}__{theme}.png", optimize=True)
print("done", sorted(p.name for p in out.glob("compare*")))
