# SPDX-FileCopyrightText: 2026 The Apoapsis contributors
# SPDX-License-Identifier: MIT
"""How smoothly the ship's render position moves against time, from the takeoff CSV of TestDriverRecord.

Usage, from tools/dev:  python -E -P render_position_residual.py CSV

For each capture mode (first, third) the ship's render position (the ship_x/y/z columns: the position the renderer
draws, Entity.getPosition(partialTick)) is fitted with a cubic in time, one fit per axis. A ship that accelerates
smoothly is a cubic to well within a centimetre, so what is left over is jitter. This is done on two time axes:

  wall clock      System.nanoTime when the frame had been drawn (the RenderFrameEvent.Post stamp, "frame end"), and when
                  it started to draw (the Pre stamp, "frame start"). The end stamp includes the time the frame took to
                  draw, which varies from frame to frame (and so moves the world, not only the ship): no entity code
                  can remove that.
  game timeline   client_tick + partial_tick, the clock the frame's state was sampled at (one unit is 50 ms). This
                  isolates what the ship's own smoothing is responsible for.

Reported per mode and axis: the residual's RMS and maximum in blocks (vertical, and the 3-D length), the mean speed, the
frame rate, and how much the speed changes between consecutive frames (a smooth climb keeps it within a few percent; a
stuttering one swings by tens of percent). Frames without a ship (the pilot not seated) and the frames after a jump of
more than 50 blocks are left out.

When the CSV has the server's own ship (srv_tick, srv_x, srv_y, srv_z; singleplayer runs), two more lines are printed:
  - the same cubic residual of the server's flight itself, taken at its own ticks. That is the floor: the flight is not
    exactly a cubic, so no renderer can go below it on this metric;
  - the tracking error against the server's flight on the game timeline: the rendered vertical position minus the
    server's position at the same moment on the server's tick clock, after the best fixed delay and rate (the draw
    trails the server by a fraction of a second, and that is not jitter). It does not depend on the shape of the flight.
"""

import csv
import sys

import numpy as np

DEGREE = 3
JUMP_BLOCKS = 50.0
TICK_S = 0.05


def column(rows, name):
    return np.array([float(r[name]) for r in rows])


def segment(rows):
    """The longest run of consecutive rows with a ship and no jump of more than JUMP_BLOCKS."""
    runs, current = [], []
    last = None
    for r in rows:
        if r["ship_x"] in ("NaN", ""):
            if current:
                runs.append(current)
            current, last = [], None
            continue
        pos = np.array([float(r["ship_x"]), float(r["ship_y"]), float(r["ship_z"])])
        if last is not None and np.linalg.norm(pos - last) > JUMP_BLOCKS:
            runs.append(current)
            current = []
        current.append(r)
        last = pos
    if current:
        runs.append(current)
    return max(runs, key=len) if runs else []


def residuals(t, y):
    """Residual of a cubic least-squares fit of y against t (t in seconds from the first frame)."""
    scale = max(t[-1], 1e-9)
    coef = np.polyfit(t / scale, y, DEGREE)
    return y - np.polyval(coef, t / scale)


def truth_curve(part):
    """(ticks, y) of the server's ship at each distinct server tick seen in the rows, or None without server columns."""
    if "srv_tick" not in part[0]:
        return None
    pairs = {}
    for r in part:
        if r["srv_tick"] in ("NaN", ""):
            continue
        pairs[int(float(r["srv_tick"]))] = float(r["srv_y"])
    if len(pairs) < 20:
        return None
    keys = sorted(pairs)
    return np.array(keys, dtype=float), np.array([pairs[k] for k in keys])


def tracking_error(t, y, ticks, truth):
    """RMS and max of render minus the server flight, after the best delay d and rate a: server tick = a t / 0.05 + d."""
    def err(a, d):
        tick = a * t / TICK_S + d
        inside = (tick >= ticks[0]) & (tick <= ticks[-1])
        if inside.sum() < 0.8 * len(t):
            return None
        return y[inside] - np.interp(tick[inside], ticks, truth)

    best = None
    for d in np.arange(ticks[0] - 10, ticks[0] + 40, 0.05):
        e = err(1.0, d)
        if e is not None:
            rms = np.sqrt((e ** 2).mean())
            if best is None or rms < best[0]:
                best = (rms, 1.0, d)
    if best is None:
        return None
    for _ in range(3):
        _, a, d = best
        for a2 in np.arange(a - 0.02, a + 0.02, 0.0005):
            for d2 in np.arange(d - 1, d + 1, 0.02):
                e = err(a2, d2)
                if e is not None:
                    rms = np.sqrt((e ** 2).mean())
                    if rms < best[0]:
                        best = (rms, a2, d2)
    e = err(best[1], best[2])
    return best[0], np.abs(e).max(), best[1], best[2]


def axis_report(label, t, ship):
    res = np.column_stack([residuals(t, ship[:, i]) for i in range(3)])
    length = np.linalg.norm(res, axis=1)
    dt = np.diff(t)
    speed = np.linalg.norm(np.diff(ship, axis=0), axis=1) / np.maximum(dt, 1e-9)
    mean_speed = np.linalg.norm(ship[-1] - ship[0]) / t[-1]
    # Speed spread between consecutive frames, ignoring frames shorter than 2 ms (timer noise).
    ok = dt > 0.002
    swing = np.abs(np.diff(speed[ok])) / max(mean_speed, 1e-9)
    print(f"  [{label}]")
    print(f"    vertical residual   RMS {np.sqrt((res[:, 1] ** 2).mean()):.4f} blocks, max {np.abs(res[:, 1]).max():.4f}")
    print(f"    3-D residual length RMS {np.sqrt((length ** 2).mean()):.4f} blocks, max {length.max():.4f}")
    print(f"    speed change between consecutive frames, share of the mean speed: "
          f"median {np.median(swing):.3f}, 95th percentile {np.percentile(swing, 95):.3f}")
    return mean_speed


def report(mode, rows):
    part = segment(rows)
    if len(part) < 20:
        print(f"{mode}: fewer than 20 usable frames")
        return
    nanos = column(part, "nanotime")
    wall = (nanos - nanos[0]) / 1e9
    ship = np.column_stack([column(part, "ship_x"), column(part, "ship_y"), column(part, "ship_z")])
    fps = (len(part) - 1) / wall[-1]
    game = None
    if "client_tick" in part[0]:
        game = (column(part, "client_tick") + column(part, "partial_tick")) * TICK_S
        game = game - game[0]
    mean_speed = np.linalg.norm(ship[-1] - ship[0]) / wall[-1]
    print(f"{mode} person: {len(part)} frames over {wall[-1]:.1f} s, {fps:.1f} fps, mean speed {mean_speed:.1f} m/s")
    axis_report("wall clock, frame end", wall, ship)
    if "pre_nanotime" in part[0] and float(part[0]["pre_nanotime"]) > 0:
        pre = (column(part, "pre_nanotime") - nanos[0]) / 1e9
        axis_report("wall clock, frame start", pre - pre[0], ship)
    if game is not None:
        axis_report("game timeline", game, ship)
    truth = truth_curve(part)
    if truth is not None and game is not None:
        ticks, ty = truth
        floor = residuals((ticks - ticks[0]) * TICK_S, ty)
        print(f"  floor (cubic residual of the server's own flight at its ticks): "
              f"RMS {np.sqrt((floor ** 2).mean()):.4f} blocks, max {np.abs(floor).max():.4f}")
        track = tracking_error(game, ship[:, 1], ticks, ty)
        if track is not None:
            print(f"  tracking error vs the server's flight (game timeline): RMS {track[0]:.4f} blocks, "
                  f"max {track[1]:.4f} (delay {track[3] - ticks[0]:.2f} ticks past the first sample, rate {track[2]:.4f})")


def main() -> None:
    if len(sys.argv) < 2:
        sys.exit("usage: render_position_residual.py CSV")
    with open(sys.argv[1], newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    for mode in ("first", "third"):
        report(mode, [r for r in rows if r["mode"] == mode])


if __name__ == "__main__":
    main()
