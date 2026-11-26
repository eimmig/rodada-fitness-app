"""Generates the Rodada mark (circular tapered arrow around a clock) as Android vector drawables.
Geometry mirrors RodadaMark.kt: angles in degrees clockwise from 12 o'clock, units of a 108 viewport."""
import math, sys, os

RES = sys.argv[1]

R = 23.0        # arrow centre-line radius
W_MIN, W_MAX = 1.6, 8.0
HEAD_HALF = 7.6
TAIL, HEAD = 40.0, 318.0   # shaft from tail angle to head base angle
TIP = 24.0                  # tip angle ahead of head base
FACE = 13.0
HOUR = (305.0, 6.6)
MINUTE = (60.0, 9.6)

def pt(cx, cy, s, theta, r):
    a = math.radians(theta)
    return (cx + s * r * math.sin(a), cy - s * r * math.cos(a))

def f(v): return f"{v:.2f}".rstrip("0").rstrip(".")

def arrow_path(cx, cy, s):
    n = 72
    outer, inner = [], []
    for i in range(n + 1):
        t = i / n
        th = TAIL + (HEAD - TAIL) * t
        w = W_MIN + (W_MAX - W_MIN) * t ** 0.8
        outer.append(pt(cx, cy, s, th, R + w / 2))
        inner.append(pt(cx, cy, s, th, R - w / 2))
    pts = outer + [pt(cx, cy, s, HEAD, R + HEAD_HALF), pt(cx, cy, s, HEAD + TIP, R), pt(cx, cy, s, HEAD, R - HEAD_HALF)] + inner[::-1]
    # rounded tail cap
    d = "M" + " L".join(f"{f(x)},{f(y)}" for x, y in pts) + "Z"
    tx, ty = pt(cx, cy, s, TAIL, R)
    rr = s * W_MIN / 2
    d += f" M{f(tx - rr)},{f(ty)} a{f(rr)},{f(rr)} 0,1 1,{f(2*rr)},0 a{f(rr)},{f(rr)} 0,1 1,{f(-2*rr)},0Z"
    return d

def circle(cx, cy, r):
    return f"M{f(cx - r)},{f(cy)} a{f(r)},{f(r)} 0,1 1,{f(2*r)},0 a{f(r)},{f(r)} 0,1 1,{f(-2*r)},0Z"

def hand(cx, cy, s, theta, length):
    x, y = pt(cx, cy, s, theta, length)
    return f"M{f(cx)},{f(cy)} L{f(x)},{f(y)}"

def vector(size, viewport, body, comment):
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- {comment} Generated geometry, same as ui/components/RodadaMark.kt. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{size}dp"
    android:height="{size}dp"
    android:viewportWidth="{viewport}"
    android:viewportHeight="{viewport}">
{body}</vector>
'''

def stroke(d, color, width):
    return f'''    <path android:strokeColor="{color}" android:strokeWidth="{f(width)}" android:strokeLineCap="round"
        android:pathData="{d}" />
'''

def fill(d, color):
    return f'''    <path android:fillColor="{color}"
        android:pathData="{d}" />
'''

VIOLET, WHITE, ORANGE = "#5B3DF5", "#FFFFFF", "#FF6B2C"

# Launcher foreground: white arrow, white clock face, violet hands, orange hub.
c, s = 54.0, 1.0
body = fill(arrow_path(c, c, s), WHITE) + fill(circle(c, c, FACE), WHITE)
body += stroke(hand(c, c, s, *HOUR), VIOLET, 2.8) + stroke(hand(c, c, s, *MINUTE), VIOLET, 2.4)
body += fill(circle(c, c, 2.2), ORANGE)
open(os.path.join(RES, "drawable", "ic_launcher_foreground.xml"), "w").write(
    vector(108, 108, body, "Rodada mark: a turning arrow around a clock (one more day done)."))

# Themed-icon monochrome layer and notification icon: one colour, face as a ring.
def mono(c, s, color):
    b = fill(arrow_path(c, c, s), color)
    b += stroke(circle(c, c, s * (FACE - 1.3)), color, s * 2.6)
    b += stroke(hand(c, c, s, HOUR[0], HOUR[1] * 0.92), color, s * 2.8) + stroke(hand(c, c, s, MINUTE[0], MINUTE[1] * 0.92), color, s * 2.4)
    return b

open(os.path.join(RES, "drawable", "ic_launcher_monochrome.xml"), "w").write(
    vector(108, 108, mono(54.0, 1.0, WHITE), "Rodada mark, monochrome layer for themed icons."))

s24 = 11.4 / (R + HEAD_HALF)
open(os.path.join(RES, "drawable", "ic_stat_rodada.xml"), "w").write(
    vector(24, 24, mono(12.0, s24, WHITE), "Notification icon: Rodada mark, monochrome."))
print("ok")
