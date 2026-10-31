#!/usr/bin/env python3
"""Phase 3 evidence: each core task as a strip of real screenshots with the tap numbers on them.
Usage: flow_strips.py <dir with the audit screenshots> <out dir>"""
import pathlib
import sys
from PIL import Image, ImageDraw, ImageFont

src, out = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
out.mkdir(parents=True, exist_ok=True)
W = 300
TAG = "w412_f10"

HOME = "home_both_features"
BOTTOM = 0.955  # the pinned primary action
FLOWS = {
    "task1_see_all_beds": ("1 tap. Budget 1.", [("home_6beds", "Tap 1: app icon. Every bed, most urgent first.", [])]),
    "task2_ack": ("1 tap. Budget 1.", [
        ("alarm_1bed", "The alarm.", [(1, 0.5, 0.77)]),
        ("alarm_done_undo", "Done. Undo for 5 seconds.", []),
    ]),
    "task3_whole_round": ("2 taps. Budget 2.", [
        ("round_alarm", "Stops the sound.", [(1, 0.5, 0.85)]),
        ("round_checklist", "Finishes every bed.", [(2, 0.5, 0.85)]),
    ]),
    "task4_start_transfusion": ("4 taps, no typing. Budget 4.", [
        (HOME, "Tap 1: app icon.", [(2, 0.29, BOTTOM)]),
        ("start_empty", "One tap on the bed.", [(3, 0.16, 0.305)]),
        ("start_bed_picked", "The confirmation is in the action.", [(4, 0.5, 0.95)]),
        ("undo_bar_on_home", "Running. Undo for 5 seconds.", []),
    ]),
    "task5_start_turning": ("4 taps, no typing. Budget 4.", [
        (HOME, "Tap 1: app icon.", [(2, 0.77, BOTTOM)]),
        ("turn_start", "One tap on the bed.", [(3, 0.39, 0.305)]),
        ("turn_start_bed_picked", "The position is the action.", [(4, 0.5, 0.95)]),
    ]),
    "task6_end_transfusion": ("3 taps. Budget 3.", [
        (HOME, "Tap 1: app icon.", [(2, 0.5, 0.5)]),
        ("detail_upcoming", "Undo instead of a dialog.", [(3, 0.5, BOTTOM)]),
        ("detail_ended_end_vs", "End VS prompt follows.", []),
    ]),
    "task7_reaction": ("2 taps from Home, 1 from the alarm. Budget 2.", [
        (HOME, "Tap 1: app icon.", [(2, 0.5, 0.875)]),
        ("reaction_1bed", "The steps first.", []),
        ("alarm_1bed", "From a transfusion alarm.", [(1, 0.5, 0.945)]),
    ]),
    "task8_mark_stopped": ("3 taps. Budget 3.", [
        (HOME, "Tap 1: app icon.", [(2, 0.5, 0.875)]),
        ("reaction_1bed", "Always in view.", [(3, 0.5, BOTTOM)]),
        ("detail_stopped_reaction", "Undo for 5 seconds.", []),
    ]),
    "task9_cant_turn": ("3 taps. Budget 3.", [
        ("round_alarm", "Stops the sound.", [(1, 0.5, 0.85)]),
        ("round_checklist", "Then the reason.", [(2, 0.72, 0.70), (3, 0.43, 0.78)]),
    ]),
}


def font(size):
    for name in ("DejaVuSans-Bold.ttf", "arialbd.ttf", "seguisb.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            pass
    return ImageFont.load_default()


num = font(18)
cap = font(15)
head = font(20)


def wrap(d, text, f, width):
    lines, line = [], ""
    for word in text.split():
        t = (line + " " + word).strip()
        if d.textlength(t, font=f) > width and line:
            lines.append(line)
            line = word
        else:
            line = t
    return lines + [line]


for name, (summary, steps) in FLOWS.items():
    cells = []
    for stem, caption, marks in steps:
        im = Image.open(src / f"{stem}__{TAG}.png").convert("RGB")
        im = im.resize((W, int(im.height * W / im.width)), Image.LANCZOS)
        d = ImageDraw.Draw(im)
        for n, fx, fy in marks:
            x, y = int(fx * im.width) + 34, int(fy * im.height) - 12
            r = 14
            d.ellipse((x - r - 3, y - r - 3, x + r + 3, y + r + 3), fill="#FFFFFF")
            d.ellipse((x - r, y - r, x + r, y + r), fill="#6A1B9A")
            d.text((x, y), str(n), font=num, fill="#FFFFFF", anchor="mm")
        cells.append((im, caption))
    gap = 14
    top, bottom = 40, 56
    sheet = Image.new("RGB", (gap + (W + gap) * len(cells), top + cells[0][0].height + bottom), "#FFFFFF")
    d = ImageDraw.Draw(sheet)
    d.text((gap, 10), summary, font=head, fill="#13233F")
    for i, (im, caption) in enumerate(cells):
        x = gap + i * (W + gap)
        sheet.paste(im, (x, top))
        d.rectangle((x, top, x + W - 1, top + im.height - 1), outline="#999999")
        for j, line in enumerate(wrap(d, caption, cap, W)[:2]):
            d.text((x, top + im.height + 6 + j * 20), line, font=cap, fill="#13233F")
    sheet.save(out / f"{name}.png", optimize=True)
    print(name, sheet.size)
