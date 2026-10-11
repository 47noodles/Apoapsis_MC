"""Render docs/plan/work-graph.json into a self-contained HTML page.

Usage (from the repo root):
  python tools/plan/render_plan.py                       # docs/plan/work-graph.json -> docs/plan/work-graph.html
  python tools/plan/render_plan.py --set 0.11 running    # update a node's status, then render

The graph runs left to right: every node sits in a column right of everything it
depends on, and each stage is a band of columns, so the start is on the far left
and 1.0 on the far right. The JSON is the source of truth; edit it, then re-render.
"""
import argparse
import html
import json
import os

STATUSES = ["pending", "running", "review", "redeploy", "done", "failed", "self"]
W, H, GAP_X, GAP_Y, PAD, BAND_TOP = 196, 54, 44, 10, 24, 64


def layout(g):
    nodes = g["nodes"]
    by = {n["id"]: n for n in nodes}
    deps = {n["id"]: [d for d in n.get("depends", []) if d in by] for n in nodes}
    kids = {i: [] for i in by}
    for i, ds in deps.items():
        for d in ds:
            kids[d].append(i)

    col = {}
    stages = sorted({n["stage"] for n in nodes})
    start = 0
    bands = {}
    for s in stages:
        members = [n["id"] for n in nodes if n["stage"] == s]
        pending = list(members)
        while pending:
            progressed = False
            for i in list(pending):
                if all(d in col for d in deps[i]):
                    col[i] = max([start] + [col[d] + 1 for d in deps[i]])
                    pending.remove(i)
                    progressed = True
            if not progressed:  # dependency on a later stage or a cycle: place at the band start
                for i in pending:
                    col[i] = start
                pending = []
        end = max(col[i] for i in members)
        bands[s] = (start, end)
        start = end + 1

    ncols = start
    cols = [[n["id"] for n in nodes if col[n["id"]] == c] for c in range(ncols)]
    order = {i: k for c in cols for k, i in enumerate(c)}

    def sweep(c, nbrs):
        def key(i):
            ys = [order[x] for x in nbrs[i]]
            return sum(ys) / len(ys) if ys else order[i]
        c.sort(key=key)
        for k, i in enumerate(c):
            order[i] = k

    for _ in range(6):
        for c in range(1, ncols):
            sweep(cols[c], deps)
        for c in range(ncols - 2, -1, -1):
            sweep(cols[c], kids)

    pos = {}
    for c, members in enumerate(cols):
        for k, i in enumerate(members):
            pos[i] = (PAD + c * (W + GAP_X), PAD + BAND_TOP + k * (H + GAP_Y))
    width = PAD * 2 + ncols * (W + GAP_X)
    height = PAD * 2 + BAND_TOP + max(len(c) for c in cols) * (H + GAP_Y)
    return pos, bands, width, height, deps, kids


def esc(s):
    return html.escape(str(s if s is not None else ""), quote=True)


def render(g, out):
    pos, bands, width, height, deps, kids = layout(g)
    nodes = g["nodes"]
    by = {n["id"]: n for n in nodes}
    stage_names = g.get("stages", {})

    band_html = []
    for s, (c0, c1) in bands.items():
        members = [n for n in nodes if n["stage"] == s]
        lo = sum(n["est_weeks"][0] for n in members)
        hi = sum(n["est_weeks"][1] for n in members)
        done = sum(1 for n in members if n["status"] in ("done", "self"))
        x0 = PAD + c0 * (W + GAP_X) - GAP_X / 2
        x1 = PAD + (c1 + 1) * (W + GAP_X) - GAP_X / 2
        band_html.append(
            f'<div class="band b{s % 2}" id="stage-{s}" style="left:{x0:.0f}px;width:{x1 - x0:.0f}px;height:{height:.0f}px">'
            f'<div class="bandhead"><b>Stage {s}</b> {esc(stage_names.get(str(s), ""))}'
            f'<span>{done}/{len(members)} done · {lo:g}–{hi:g} person-weeks</span></div></div>')

    edges = []
    for n in nodes:
        cx, cy = pos[n["id"]]
        for d in deps[n["id"]]:
            dx, dy = pos[d]
            sx, sy, ex, ey = dx + W, dy + H / 2, cx, cy + H / 2
            mx = (sx + ex) / 2
            cls = "e done" if by[d]["status"] in ("done", "self") else "e"
            edges.append(f'<path class="{cls}" data-a="{esc(d)}" data-b="{esc(n["id"])}" '
                         f'd="M{sx:.0f},{sy:.0f} C{mx:.0f},{sy:.0f} {mx:.0f},{ey:.0f} {ex:.0f},{ey:.0f}"/>')

    cards = []
    for n in nodes:
        x, y = pos[n["id"]]
        cards.append(
            f'<button class="card s-{esc(n["status"])} k-{esc(n["kind"])}" data-id="{esc(n["id"])}" '
            f'data-stage="{n["stage"]}" data-track="{esc(n["track"])}" data-owner="{esc(n["owner"])}" '
            f'data-status="{esc(n["status"])}" title="{esc(n["id"] + " " + n["name"])}" style="left:{x:.0f}px;top:{y:.0f}px">'
            f'<span class="l1"><span class="nid">{esc(n["id"])}</span><span class="kind">{esc(n["kind"])}</span>'
            f'<span class="who">{esc(n["model"])}</span></span>'
            f'<span class="nm">{esc(n["name"])}</span></button>')

    data = json.dumps({"nodes": nodes, "owners": g.get("owners", {}), "stages": stage_names}, ensure_ascii=False)
    tracks = sorted({n["track"] for n in nodes})
    owners = sorted({n["owner"] for n in nodes})
    lo = sum(n["est_weeks"][0] for n in nodes)
    hi = sum(n["est_weeks"][1] for n in nodes)
    counts = {s: sum(1 for n in nodes if n["status"] == s) for s in STATUSES}

    def opts(vals):
        return "".join(f'<option value="{esc(v)}">{esc(v)}</option>' for v in vals)

    stage_links = "".join(
        f'<a href="#stage-{s}" data-jump="{s}">{s} {esc(stage_names.get(str(s), ""))}</a>' for s in bands)

    page = TEMPLATE
    for k, v in {
        "@@TITLE@@": esc(g["title"]),
        "@@REQUEST@@": esc(g.get("request", "")),
        "@@UPDATED@@": esc(g.get("updated", "")),
        "@@SUMMARY@@": (f'{len(nodes)} work items · {counts["done"] + counts["self"]} done · {counts["running"]} in progress · '
                        f'estimate {lo:g}–{hi:g} person-weeks ({lo / 4.33:.0f}–{hi / 4.33:.0f} person-months)'),
        "@@STAGES@@": stage_links,
        "@@TRACKS@@": opts(tracks),
        "@@OWNERS@@": opts(owners),
        "@@W@@": f"{width:.0f}",
        "@@H@@": f"{height:.0f}",
        "@@BANDS@@": "".join(band_html),
        "@@EDGES@@": "".join(edges),
        "@@CARDS@@": "".join(cards),
        "@@DATA@@": data.replace("</", "<\\/"),
    }.items():
        page = page.replace(k, v)
    with open(out, "w", encoding="utf-8") as f:
        f.write(page)


TEMPLATE = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Apoapsis work graph</title>
<style>
:root {
  --bg: #0d1013; --plate: #14181d; --raised: #1d232a; --edge: #2a323b; --hair: #56616d;
  --ink: #e8e2d4; --muted: #a39c8e; --brass: #c9a25a; --signal: #3ad1c1; --go: #74d465;
  --alert: #ec5b45; --hydrogen: #5aa9f5; --copper: #c46f3e; --band0: #11151a; --band1: #0f1216;
}
@media (prefers-color-scheme: light) {
  :root:not([data-theme="dark"]) {
    --bg: #f4f1ea; --plate: #ffffff; --raised: #f7f4ee; --edge: #d8d2c6; --hair: #9a958b;
    --ink: #1d232a; --muted: #5d5850; --brass: #8a6a34; --signal: #137f74; --go: #2f7d25;
    --alert: #b23a28; --hydrogen: #2f6fb3; --copper: #9b4f25; --band0: #efebe2; --band1: #f4f1ea;
  }
}
* { box-sizing: border-box; }
body { margin: 0; background: var(--bg); color: var(--ink); font: 14px/1.4 system-ui, -apple-system, "Segoe UI", sans-serif; }
header { padding: 16px; display: grid; gap: 8px; border-bottom: 1px solid var(--edge); }
h1 { margin: 0; font-size: 20px; letter-spacing: .02em; }
.sub { color: var(--muted); max-width: 980px; }
.sum { color: var(--ink); }
.bar { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.bar select, .bar input, .bar button { background: var(--plate); color: var(--ink); border: 1px solid var(--edge); border-radius: 3px; padding: 6px 8px; font: inherit; min-height: 34px; }
.bar button { cursor: pointer; }
.bar button:focus-visible, .bar select:focus-visible, .bar input:focus-visible, .card:focus-visible { outline: 2px solid var(--signal); outline-offset: 2px; }
.stages { display: flex; flex-wrap: wrap; gap: 6px; }
.stages a { color: var(--brass); text-decoration: none; border: 1px solid var(--edge); border-radius: 3px; padding: 3px 8px; font-size: 13px; }
.stages a:hover { border-color: var(--brass); }
.legend { display: flex; flex-wrap: wrap; gap: 12px; color: var(--muted); font-size: 12px; }
.legend i { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin-right: 4px; vertical-align: -1px; }
#wrap { position: relative; overflow: auto; height: calc(100vh - 220px); min-height: 360px; }
#canvas { position: relative; transform-origin: 0 0; }
.band { position: absolute; top: 0; border-left: 1px solid var(--edge); }
.band.b0 { background: var(--band0); } .band.b1 { background: var(--band1); }
.bandhead { position: sticky; top: 0; padding: 10px 12px; font-size: 15px; color: var(--brass); }
.bandhead span { display: block; color: var(--muted); font-size: 12px; }
svg { position: absolute; left: 0; top: 0; pointer-events: none; }
.e { fill: none; stroke: var(--hair); stroke-width: 1.2; opacity: .45; }
.e.done { stroke: var(--go); opacity: .6; }
.e.hi { stroke: var(--signal); opacity: 1; stroke-width: 2; }
.card { position: absolute; width: 196px; height: 54px; text-align: left; padding: 6px 9px; background: var(--plate);
  border: 1px solid var(--edge); border-left: 4px solid var(--hair); border-radius: 3px; color: var(--ink); cursor: pointer; font: inherit; }
.card .l1 { display: flex; gap: 6px; font-size: 11px; color: var(--muted); align-items: baseline; }
.card .nid { font-family: ui-monospace, Consolas, monospace; color: var(--ink); font-weight: 600; }
.card .who { margin-left: auto; }
.card .nm { display: block; font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.card.s-done, .card.s-self { border-left-color: var(--go); }
.card.s-running, .card.s-review { border-left-color: var(--signal); }
.card.s-redeploy, .card.s-failed { border-left-color: var(--alert); }
.card.k-gate { background: var(--raised); border-color: var(--brass); border-left-width: 4px; }
.card.k-release .nm { color: var(--brass); }
.card.dim { opacity: .18; }
.card.sel { outline: 2px solid var(--signal); outline-offset: 1px; }
#panel { position: fixed; right: 16px; bottom: 16px; width: min(420px, calc(100vw - 32px)); max-height: 60vh; overflow: auto;
  background: var(--plate); border: 1px solid var(--edge); border-top: 2px solid var(--brass); border-radius: 3px; padding: 14px; display: none; }
#panel h2 { margin: 0 0 6px; font-size: 16px; }
#panel .meta { color: var(--muted); font-size: 12px; margin-bottom: 8px; }
#panel a { color: var(--signal); cursor: pointer; }
#panel button { float: right; background: none; border: 0; color: var(--muted); font-size: 18px; cursor: pointer; }
details { margin: 16px; } summary { cursor: pointer; color: var(--brass); }
table { border-collapse: collapse; width: 100%; font-size: 13px; }
td, th { border-bottom: 1px solid var(--edge); padding: 4px 6px; text-align: left; vertical-align: top; }
th { color: var(--muted); font-weight: 500; }
</style>
</head>
<body>
<header>
  <h1>@@TITLE@@</h1>
  <div class="sub">@@REQUEST@@</div>
  <div class="sum">@@SUMMARY@@ · updated @@UPDATED@@</div>
  <nav class="stages" aria-label="Jump to stage">@@STAGES@@</nav>
  <div class="bar">
    <input id="q" type="search" placeholder="Search work items" aria-label="Search work items">
    <select id="ftrack" aria-label="Track"><option value="">All tracks</option>@@TRACKS@@</select>
    <select id="fowner" aria-label="Owner"><option value="">All owners</option>@@OWNERS@@</select>
    <select id="fstatus" aria-label="Status"><option value="">Any status</option><option>pending</option><option>running</option><option>done</option></select>
    <button id="zout" aria-label="Zoom out">−</button><button id="zin" aria-label="Zoom in">+</button><button id="zfit">Fit height</button>
  </div>
  <div class="legend"><span><i style="background:var(--go)"></i>done</span><span><i style="background:var(--signal)"></i>in progress</span>
    <span><i style="background:var(--hair)"></i>pending</span><span><i style="background:var(--brass)"></i>gate (an approval) or release</span>
    <span>Click a card for details; its inputs and outputs light up.</span></div>
</header>
<div id="wrap"><div id="canvas" style="width:@@W@@px;height:@@H@@px">@@BANDS@@
<svg width="@@W@@" height="@@H@@" aria-hidden="true">@@EDGES@@</svg>@@CARDS@@</div></div>
<aside id="panel" aria-live="polite"></aside>
<details><summary>All work items as a list</summary><div id="list"></div></details>
<script>
const G = @@DATA@@;
const byId = Object.fromEntries(G.nodes.map(n => [n.id, n]));
const kids = {}; G.nodes.forEach(n => (n.depends || []).forEach(d => (kids[d] = kids[d] || []).push(n.id)));
const canvas = document.getElementById('canvas'), wrap = document.getElementById('wrap'), panel = document.getElementById('panel');
let zoom = 1;
function setZoom(z) { zoom = Math.max(.15, Math.min(1.5, z)); canvas.style.transform = `scale(${zoom})`;
  canvas.parentElement.style.setProperty('--z', zoom); }
document.getElementById('zin').onclick = () => setZoom(zoom * 1.25);
document.getElementById('zout').onclick = () => setZoom(zoom / 1.25);
document.getElementById('zfit').onclick = () => setZoom(wrap.clientHeight / canvas.offsetHeight);
document.querySelectorAll('[data-jump]').forEach(a => a.onclick = e => { e.preventDefault();
  const b = document.getElementById('stage-' + a.dataset.jump); wrap.scrollTo({left: b.offsetLeft * zoom - 8, behavior: 'smooth'}); });
function chain(id, map, seen = new Set()) { (map(id) || []).forEach(x => { if (!seen.has(x)) { seen.add(x); chain(x, map, seen); } }); return seen; }
function link(id) { return `<a data-go="${id}">${id} ${byId[id].name}</a>`; }
function show(id) {
  const n = byId[id];
  const up = chain(id, i => byId[i].depends), down = chain(id, i => kids[i]);
  document.querySelectorAll('.card').forEach(c => { const i = c.dataset.id;
    c.classList.toggle('dim', i !== id && !up.has(i) && !down.has(i)); c.classList.toggle('sel', i === id); });
  document.querySelectorAll('.e').forEach(e => e.classList.toggle('hi', (e.dataset.b === id) || (e.dataset.a === id)));
  panel.innerHTML = `<button aria-label="Close" id="pclose">×</button><h2>${n.id} · ${n.name}</h2>
    <div class="meta">Stage ${n.stage} ${G.stages[n.stage] || ''} · ${n.track} · ${n.kind} · ${G.owners[n.owner] || n.owner} · ${n.status} · ${n.est_weeks[0]}–${n.est_weeks[1]} weeks</div>
    <p>${n.does}</p>${n.exit ? `<p><b>Done when:</b> ${n.exit}</p>` : ''}${n.ref ? `<p class="meta">See ${n.ref}</p>` : ''}
    <p><b>Needs:</b> ${(n.depends || []).map(link).join(', ') || 'nothing'}</p>
    <p><b>Unblocks:</b> ${(kids[id] || []).map(link).join(', ') || 'nothing'}</p>`;
  panel.style.display = 'block';
  document.getElementById('pclose').onclick = clear;
  panel.querySelectorAll('[data-go]').forEach(a => a.onclick = () => focusCard(a.dataset.go));
}
function clear() { panel.style.display = 'none'; document.querySelectorAll('.card').forEach(c => c.classList.remove('dim', 'sel'));
  document.querySelectorAll('.e').forEach(e => e.classList.remove('hi')); applyFilters(); }
function focusCard(id) { const c = document.querySelector(`.card[data-id="${CSS.escape(id)}"]`);
  wrap.scrollTo({left: c.offsetLeft * zoom - wrap.clientWidth / 3, top: c.offsetTop * zoom - wrap.clientHeight / 3, behavior: 'smooth'}); show(id); c.focus({preventScroll: true}); }
document.querySelectorAll('.card').forEach(c => c.onclick = () => show(c.dataset.id));
document.addEventListener('keydown', e => { if (e.key === 'Escape') clear(); });
function applyFilters() {
  const q = document.getElementById('q').value.toLowerCase(), t = document.getElementById('ftrack').value,
        o = document.getElementById('fowner').value, s = document.getElementById('fstatus').value;
  document.querySelectorAll('.card').forEach(c => { const n = byId[c.dataset.id];
    const hit = (!q || (n.id + ' ' + n.name + ' ' + n.does).toLowerCase().includes(q)) && (!t || n.track === t) &&
      (!o || n.owner === o) && (!s || n.status === s);
    c.classList.toggle('dim', !hit); });
}
['q', 'ftrack', 'fowner', 'fstatus'].forEach(id => document.getElementById(id).addEventListener('input', applyFilters));
const list = document.getElementById('list');
list.innerHTML = Object.keys(G.stages).map(s => `<h3>Stage ${s} · ${G.stages[s]}</h3><table><tr><th>Id</th><th>Work</th><th>Owner</th><th>Status</th><th>Needs</th></tr>` +
  G.nodes.filter(n => String(n.stage) === s).map(n => `<tr><td>${n.id}</td><td><b>${n.name}</b><br>${n.does}</td><td>${n.owner}</td><td>${n.status}</td><td>${(n.depends || []).join(', ')}</td></tr>`).join('') + '</table>').join('');
setZoom(Math.min(1, Math.max(.5, wrap.clientHeight / canvas.offsetHeight)));
</script>
</body>
</html>
"""


def main():
    ap = argparse.ArgumentParser()
    root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    ap.add_argument("graph", nargs="?", default=os.path.join(root, "docs", "plan", "work-graph.json"))
    ap.add_argument("--set", nargs=2, action="append", metavar=("ID", "STATUS"), default=[])
    a = ap.parse_args()
    with open(a.graph, encoding="utf-8") as f:
        g = json.load(f)
    by = {n["id"]: n for n in g["nodes"]}
    for i, st in a.set:
        if st not in STATUSES:
            ap.error(f"status must be one of {STATUSES}")
        by[i]["status"] = st
    if a.set:
        with open(a.graph, "w", encoding="utf-8") as f:
            json.dump(g, f, indent=1, ensure_ascii=False)
    out = os.path.splitext(a.graph)[0] + ".html"
    render(g, out)
    print(out)


if __name__ == "__main__":
    main()
