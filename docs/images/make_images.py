"""Draws the README banner and charts (light and dark SVGs) in Attendo's colours.

The numbers come from the app's own attendance maths (`core/model/.../AttendanceSummary.kt`):
"can miss" = floor(present / target - held), "must attend" = ceil((target * held - present) / (1 - target)).
The subjects and their marks are an example, not anyone's real data.

Run from this folder (Windows, needs matplotlib and the Segoe UI font):  python make_images.py
"""
import math

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, Polygon, Rectangle

plt.rcParams.update({
    "font.family": ["Segoe UI", "DejaVu Sans"],
    "svg.fonttype": "path",  # text becomes shapes, so it looks the same everywhere
    "axes.unicode_minus": False,
})

# Attendo's palettes (core/designsystem/.../theme/Theme.kt). Series and status colours were
# checked with the dataviz palette validator against these surfaces (dark steps are a little
# darker than the app's so they stay inside the dark lightness band).
THEMES = {
    "light": dict(surface="#F8FAED", text="#232323", muted="#62645A", grid="#E1E4CC",
                  green="#50A65C", pink="#E0399A", blue="#2B8FE0"),
    "dark": dict(surface="#141414", text="#F2F2F2", muted="#A3A3A8", grid="#2C2C2C",
                 green="#45A052", pink="#E84A9A", blue="#3389E4"),
}
TARGET = 0.75
W, DPI = 9.0, 100  # 900 px wide


def style_axes(ax, c):
    ax.set_facecolor(c["surface"])
    for side in ("top", "right", "left"):
        ax.spines[side].set_visible(False)
    ax.spines["bottom"].set_color(c["grid"])
    ax.tick_params(colors=c["muted"], labelsize=11, length=0, pad=6)


def titles(fig, c, title, subtitle):
    fig.text(0.04, 0.93, title, fontsize=19, fontweight="semibold", color=c["text"], va="top")
    fig.text(0.04, 0.855, subtitle, fontsize=12, color=c["muted"], va="top")


def legend(fig, c, items, y):
    x = 0.04
    for label, color in items:
        fig.add_artist(Rectangle((x, y - 0.006), 0.022, 0.012, transform=fig.transFigure,
                                 color=color, linewidth=0))
        t = fig.text(x + 0.03, y, label, fontsize=11.5, color=c["text"], va="center")
        x += 0.05 + 0.0105 * len(label)


# ---- Chart 1: what missing or attending the next classes does ---------------------------------

def bunk_chart(mode):
    c = THEMES[mode]
    n = list(range(0, 13))
    miss = [34 / (40 + k) * 100 for k in n]          # 34 of 40 attended (85 %), then missed
    attend = [(28 + k) / (40 + k) * 100 for k in n]  # 28 of 40 attended (70 %), then attended
    can_miss = math.floor(34 / TARGET - 40 + 1e-9)                    # 5
    must = math.ceil((TARGET * 40 - 28) / (1 - TARGET) - 1e-9)       # 8

    fig = plt.figure(figsize=(W, 4.9), dpi=DPI, facecolor=c["surface"])
    titles(fig, c, "The 75% rule, worked out for you",
           "Attendo counts how many classes you can still miss, or must attend, to stay at your target.")
    legend(fig, c, [("Start at 85% and miss the next classes", c["pink"]),
                    ("Start at 70% and attend the next classes", c["blue"])], 0.775)
    ax = fig.add_axes([0.075, 0.13, 0.885, 0.6])
    style_axes(ax, c)
    ax.set_xlim(-0.3, 12.3)
    ax.set_ylim(62, 88)
    ax.set_yticks([65, 70, 75, 80, 85])
    ax.set_yticklabels([f"{v}%" for v in [65, 70, 75, 80, 85]])
    ax.set_xticks(range(0, 13, 2))
    ax.set_xlabel("Next classes", color=c["muted"], fontsize=11, labelpad=6)
    ax.grid(axis="y", color=c["grid"], linewidth=1, linestyle="-")
    ax.set_axisbelow(True)

    ax.axhline(75, color=c["text"], linewidth=1, alpha=0.55)
    ax.text(0.1, 75.6, "Your target: 75%", color=c["text"], fontsize=11, ha="left", va="bottom")

    for ys, color in ((miss, c["pink"]), (attend, c["blue"])):
        ax.plot(n, ys, color=color, linewidth=2, solid_capstyle="round", solid_joinstyle="round", zorder=3)

    def mark(x, y, color, label, dy, ha):
        ax.scatter([x], [y], s=70, color=color, edgecolors=c["surface"], linewidths=2, zorder=4)
        ax.annotate(label, (x, y), xytext=(0, dy), textcoords="offset points", ha=ha,
                    fontsize=11.5, color=c["text"], fontweight="semibold")

    mark(can_miss, miss[can_miss], c["pink"], f"Can miss {can_miss} and stay above 75%", 12, "left")
    mark(must, attend[must], c["blue"], f"Back to 75% after {must} classes", -22, "left")
    fig.savefig(f"chart-75-rule-{mode}.svg", facecolor=c["surface"])
    plt.close(fig)


# ---- Chart 2: every subject at a glance (like the Stats tab) ----------------------------------

SUBJECTS = [  # name, attended, held (example numbers)
    ("Programming", 30, 34),
    ("Maths", 25, 32),
    ("Digital Logic", 22, 31),
    ("Data Structures", 31, 33),
    ("Operating Systems", 20, 30),
]


def subjects_chart(mode):
    c = THEMES[mode]
    fig = plt.figure(figsize=(W, 4.6), dpi=DPI, facecolor=c["surface"])
    titles(fig, c, "Every subject at a glance",
           "Example semester. Green is on track; pink tells you exactly how many classes to attend.")
    ax = fig.add_axes([0.2, 0.08, 0.5, 0.7])
    style_axes(ax, c)
    ax.spines["bottom"].set_visible(False)
    ax.set_xlim(0, 100)
    ax.set_xticks([])
    rows = list(reversed(SUBJECTS))
    ax.set_ylim(-0.6, len(rows) - 0.4)
    ax.set_yticks(range(len(rows)))
    ax.set_yticklabels([r[0] for r in rows], fontsize=12, color=c["text"])

    # The bar thickness in data units that gives about 20 px.
    px_per_unit_y = ax.get_window_extent().height / (len(rows))
    thick = 20 / px_per_unit_y
    r_x = 4 / (ax.get_window_extent().width / 100)  # 4 px rounding, in x units

    for i, (name, present, held) in enumerate(rows):
        pct = present / held * 100
        ok = pct + 1e-9 >= TARGET * 100
        color = c["green"] if ok else c["pink"]
        ax.add_patch(Rectangle((0, i - thick / 2), 100, thick, color=c["grid"], linewidth=0, zorder=1))
        ax.add_patch(FancyBboxPatch((0, i - thick / 2), pct, thick, color=color, linewidth=0, zorder=2,
                                    boxstyle=f"round,pad=0,rounding_size={r_x}", mutation_aspect=thick / r_x / 2.5))
        ax.add_patch(Rectangle((0, i - thick / 2), max(pct - r_x, 0), thick, color=color, linewidth=0, zorder=2))
        if ok:
            note = f"✓  {pct:.0f}%  ·  can miss {math.floor(present / TARGET - held + 1e-9)}"
        else:
            need = math.ceil((TARGET * held - present) / (1 - TARGET) - 1e-9)
            note = f"!  {pct:.0f}%  ·  attend the next {need}"
        ax.text(101.5, i, note, va="center", fontsize=11.5, color=c["text"],
                fontweight="semibold" if not ok else "normal", clip_on=False)

    ax.plot([75, 75], [-0.55, len(rows) - 0.45], color=c["text"], linewidth=1, alpha=0.55, zorder=3)
    ax.text(75, len(rows) - 0.42, "75% target", ha="center", va="bottom", fontsize=10.5, color=c["muted"])
    fig.savefig(f"chart-subjects-{mode}.svg", facecolor=c["surface"])
    plt.close(fig)


# ---- Banner -----------------------------------------------------------------------------------

def banner():
    ink, cream, green, muted = "#232323", "#F8FAED", "#50A65C", "#B9BCAA"
    fig = plt.figure(figsize=(W, 2.6), dpi=DPI, facecolor=ink)
    ax = fig.add_axes([0, 0, 1, 1])
    ax.set_xlim(0, 900)
    ax.set_ylim(0, 260)
    ax.axis("off")
    ax.add_patch(FancyBboxPatch((0, 0), 900, 260, boxstyle="round,pad=0,rounding_size=18",
                                color=ink, linewidth=0))
    # The launcher icon's pentagon mascot (app/src/main/res/drawable/ic_launcher_foreground.xml).
    s, ox, oy = 2.3, 18, 268  # scale, offsets (icon y grows downwards)
    pts = [(54, 28), (78, 45.5), (69, 74), (39, 74), (30, 45.5)]
    poly = [(ox + x * s, oy - y * s) for x, y in pts]
    ax.add_patch(Polygon(poly, closed=True, facecolor=green, edgecolor=green, linewidth=8 * s * 0.72,
                         joinstyle="round"))
    for x in (47, 61):
        ax.plot([ox + x * s, ox + x * s], [oy - 47 * s, oy - 52 * s], color=ink, linewidth=3 * s * 0.72,
                solid_capstyle="round")
    t = [i / 20 for i in range(21)]
    qx = [(1 - u) ** 2 * 46 + 2 * (1 - u) * u * 54 + u ** 2 * 62 for u in t]
    qy = [(1 - u) ** 2 * 58 + 2 * (1 - u) * u * 67 + u ** 2 * 58 for u in t]
    ax.plot([ox + x * s for x in qx], [oy - y * s for y in qy], color=ink, linewidth=3 * s * 0.72,
            solid_capstyle="round")

    ax.text(250, 160, "Attendo", fontsize=46, fontweight="bold", color=cream, va="center")
    ax.text(252, 103, "Attendance, tasks, focus and friends for college students.",
            fontsize=15, color=cream, va="center")
    ax.text(252, 72, "Offline first  ·  your data in plain CSV files  ·  free", fontsize=12.5,
            color=muted, va="center")
    fig.savefig("banner.svg", facecolor=ink, transparent=True)
    plt.close(fig)


if __name__ == "__main__":
    for m in THEMES:
        bunk_chart(m)
        subjects_chart(m)
    banner()
    print("wrote banner.svg, chart-75-rule-{light,dark}.svg, chart-subjects-{light,dark}.svg")
