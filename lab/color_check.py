#!/usr/bin/env python3
"""Fixed colour thresholds of docs/TEST_LOOP.md §4, computed from lab/palettes.json.

  text ≥ 4.5:1 · large text and UI components ≥ 3:1 · key numbers ≥ 7:1
  every pair of urgency-state colours differs by CIEDE2000 ΔE ≥ 15 under deuteranopia,
  protanopia and tritanopia (Machado, Oliveira & Fernandes 2009, severity 1.0).

Standard library only. Exit code 1 if anything fails; every number is printed.
"""
import itertools
import json
import math
import pathlib
import sys

MACHADO = {
    "protanopia": [[0.152286, 1.052583, -0.204868], [0.114503, 0.786281, 0.099216], [-0.003882, -0.048116, 1.051998]],
    "deuteranopia": [[0.367322, 0.860646, -0.227968], [0.280085, 0.672501, 0.047413], [-0.011820, 0.042940, 0.968881]],
    "tritanopia": [[1.255528, -0.076749, -0.178779], [-0.078411, 0.930809, 0.147602], [0.004733, 0.691367, 0.303900]],
}


def to_linear(c):
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def to_srgb(c):
    c = min(1.0, max(0.0, c))
    return 12.92 * c if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055


def rgb(hexa):
    h = hexa.lstrip("#")
    return tuple(int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))


def luminance(hexa):
    r, g, b = (to_linear(x) for x in rgb(hexa))
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast(a, b):
    la, lb = luminance(a), luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)


def simulate(hexa, kind):
    lin = [to_linear(x) for x in rgb(hexa)]
    return tuple(to_srgb(sum(m * v for m, v in zip(row, lin))) for row in MACHADO[kind])


def lab(srgb):
    r, g, b = (to_linear(x) for x in srgb)
    x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / 0.95047
    y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
    z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / 1.08883
    f = lambda t: t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116
    fx, fy, fz = f(x), f(y), f(z)
    return 116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)


def ciede2000(c1, c2):
    L1, a1, b1 = c1
    L2, a2, b2 = c2
    C1, C2 = math.hypot(a1, b1), math.hypot(a2, b2)
    Cm = (C1 + C2) / 2
    G = 0.5 * (1 - math.sqrt(Cm ** 7 / (Cm ** 7 + 25 ** 7)))
    a1p, a2p = (1 + G) * a1, (1 + G) * a2
    C1p, C2p = math.hypot(a1p, b1), math.hypot(a2p, b2)
    h1p = math.degrees(math.atan2(b1, a1p)) % 360 if C1p else 0
    h2p = math.degrees(math.atan2(b2, a2p)) % 360 if C2p else 0
    dL = L2 - L1
    dC = C2p - C1p
    if C1p * C2p == 0:
        dh = 0
    elif abs(h2p - h1p) <= 180:
        dh = h2p - h1p
    elif h2p - h1p > 180:
        dh = h2p - h1p - 360
    else:
        dh = h2p - h1p + 360
    dH = 2 * math.sqrt(C1p * C2p) * math.sin(math.radians(dh / 2))
    Lm = (L1 + L2) / 2
    Cmp = (C1p + C2p) / 2
    if C1p * C2p == 0:
        hm = h1p + h2p
    elif abs(h1p - h2p) <= 180:
        hm = (h1p + h2p) / 2
    elif h1p + h2p < 360:
        hm = (h1p + h2p + 360) / 2
    else:
        hm = (h1p + h2p - 360) / 2
    T = 1 - 0.17 * math.cos(math.radians(hm - 30)) + 0.24 * math.cos(math.radians(2 * hm)) \
        + 0.32 * math.cos(math.radians(3 * hm + 6)) - 0.20 * math.cos(math.radians(4 * hm - 63))
    dTheta = 30 * math.exp(-(((hm - 275) / 25) ** 2))
    Rc = 2 * math.sqrt(Cmp ** 7 / (Cmp ** 7 + 25 ** 7))
    Sl = 1 + 0.015 * (Lm - 50) ** 2 / math.sqrt(20 + (Lm - 50) ** 2)
    Sc = 1 + 0.045 * Cmp
    Sh = 1 + 0.015 * Cmp * T
    Rt = -math.sin(math.radians(2 * dTheta)) * Rc
    return math.sqrt((dL / Sl) ** 2 + (dC / Sc) ** 2 + (dH / Sh) ** 2 + Rt * (dC / Sc) * (dH / Sh))


def check_theme(direction, theme, p):
    fails = []

    def need(fg, bg, minimum, what):
        ratio = contrast(p[fg], p[bg])
        if ratio < minimum:
            fails.append(f"{direction}/{theme}: {what} {fg} {p[fg]} on {bg} {p[bg]} = {ratio:.2f} < {minimum}")
        return ratio

    for bg in ("bg", "card"):
        need("ink", bg, 7, "key number")
        need("muted", bg, 4.5, "text")
    for state in ("calm", "soon", "now", "overdue"):
        need(f"on_{state}", state, 7 if state in ("now", "overdue") else 4.5, f"text on {state} fill")
        need(state, "card", 3, f"{state} component on card")
    for extra, minimum in p.get("_extra", {}).items():
        fg, bg = extra.split("/")
        need(fg, bg, minimum, "pair")
    need("danger", "card", 4.5, "danger text")
    need("on_danger", "danger", 4.5, "text on danger")
    need("type_tx", "card", 3, "transfusion cue")
    need("type_rp", "card", 3, "repositioning cue")
    need("alarm_ink", "alarm_bg", 7, "alarm key number")
    need("alarm_on_action", "alarm_action", 4.5, "alarm action text")
    need("alarm_action", "alarm_bg", 3, "alarm action component")

    worst = 999.0
    pairs = list(itertools.combinations(("calm", "soon", "now", "overdue"), 2))
    for kind in [None, *MACHADO]:
        for a, b in pairs:
            ca = rgb(p[a]) if kind is None else simulate(p[a], kind)
            cb = rgb(p[b]) if kind is None else simulate(p[b], kind)
            d = ciede2000(lab(ca), lab(cb))
            worst = min(worst, d)
            if d < 15:
                fails.append(f"{direction}/{theme}: ΔE {a}/{b} under {kind or 'normal vision'} = {d:.1f} < 15")
    print(f"{direction:12} {theme:5} worst ΔE {worst:5.1f}   ink/card {contrast(p['ink'], p['card']):5.2f}   alarm {contrast(p['alarm_ink'], p['alarm_bg']):5.2f}   lum(alarm_bg) {luminance(p['alarm_bg']):.4f}")
    return fails


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    data = json.loads((pathlib.Path(__file__).parent / "palettes.json").read_text(encoding="utf-8"))
    fails = []
    for direction, themes in data.items():
        for theme, palette in themes.items():
            fails += check_theme(direction, theme, palette)
    for f in fails:
        print("FAIL", f)
    print(f"{len(fails)} failures")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
