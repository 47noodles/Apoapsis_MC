"""Vendored from the Apoapsis orchestration tooling; keep the two copies in step.

Render a work graph (JSON) to a self-contained HTML page: /orchestrate job graphs and multi-stage project plans.

Usage:
  python render_graph.py GRAPH.json                      # render GRAPH.html next to it
  python render_graph.py GRAPH.json --out other.html     # render somewhere else (repeatable)
  python render_graph.py GRAPH.json --set R1 done        # update a node, then render
  python render_graph.py GRAPH.json --set R2 redeploy --round R2 2 --note R2 "missing sources"
  python render_graph.py GRAPH.json --open               # also open it in the default browser

GRAPH.json shape:
{
  "title": "Short job name",
  "request": "What was asked for, one or two sentences",
  "live": true,                       # optional; false stops the auto-refresh (plans)
  "stages": {"0": "Foundations"},     # optional; names for node "stage" values
  "nodes": [
    {"id": "R1", "name": "Space Engineers research", "does": "longer description",
     "model": "haiku", "writes": ["path/a.md"], "depends": [], "status": "running",
     "round": 1, "note": "optional latest review note",
     "stage": 0, "kind": "build", "owner": "agents", "ref": "...", "exit": "...", "est_weeks": [1, 2]}
  ]
}
name: the label on the card, 5 words max (longer names are cut to 5 words).
status: pending | running | review | redeploy | done | failed | self
(self = the orchestrator did it, e.g. after the 3-round cap or by design).
Optional fields (stage, kind, owner, ref, exit, est_weeks, track) show in the details panel; kind "gate" and
"release" cards get an amber edge.

FORMAT (the one look for every work graph; the project lead's rules, 11 Oct 2026):
- Left to right: each node sits one column right of everything it depends on, so the first work is on the far
  left and the end goal on the far right. No downward branches.
- Columns are centred on one midline, like a funnel into the hub, never stacked from the top.
- With a "stage" on the nodes, each stage is a band of columns with its name on top; stages never interleave.
- Nothing is clipped: there is no inner scroll box. The page itself scrolls sideways for wide graphs, and the
  header stays in view.
- Click a card to see its details; everything it needs and unblocks lights up. Esc clears.
While any node is unfinished (and "live" is not false) the page reloads itself every 10 s.
"""
import argparse
import html
import json
import os
import subprocess
import sys
import webbrowser
from datetime import datetime

STATUSES = ["pending", "running", "review", "redeploy", "done", "failed", "self"]
FINISHED = {"done", "failed", "self"}
W, H, GAP_X, GAP_Y, PAD = 172, 44, 56, 10, 14
BAND_HEAD = 46  # room above the cards for stage names


def esc(s):
    return html.escape(str(s if s is not None else ""), quote=True)


def short_name(n):
    words = (n.get("name") or n.get("does") or n["id"]).split()
    if n.get("name") and len(words) > 5:
        print(f"warning: name of {n['id']} has {len(words)} words; cut to 5", file=sys.stderr)
    return " ".join(words[:5])


def layout(nodes):
    ids = [n["id"] for n in nodes]
    by = {n["id"]: n for n in nodes}
    deps = {i: [d for d in (by[i].get("depends") or []) if d in by] for i in ids}
    kids = {i: [k for k in ids if i in deps[k]] for i in ids}
    staged = any("stage" in n for n in nodes)

    # Column = how far right a node sits. Without stages: its dependency depth. With stages: the same, but every
    # stage starts right of the previous one, so stages form clean bands.
    col = {}

    def depth(i, stack=()):
        if i not in col:
            col[i] = 0 if not deps[i] or i in stack else 1 + max(depth(x, stack + (i,)) for x in deps[i])
        return col[i]

    bands = []
    if staged:
        stage_of = {i: by[i].get("stage", 0) for i in ids}
        start = 0
        for s in sorted(set(stage_of.values()), key=lambda v: (str(type(v)), v)):
            members = [i for i in ids if stage_of[i] == s]
            todo = list(members)
            while todo:
                ready = [i for i in todo if all(d in col or d not in members for d in deps[i])]
                for i in ready or todo:  # a cycle or a forward reference: place it at the band start
                    col[i] = max([start] + [col[d] + 1 for d in deps[i] if d in col])
                    todo.remove(i)
            end = max(col[i] for i in members)
            bands.append((s, start, end))
            start = end + 1
    else:
        for i in ids:
            depth(i)

    hub = max(ids, key=lambda i: (len(deps[i]), col[i], -ids.index(i))) if ids else None
    ncols = max(col.values(), default=0) + 1
    cols = [[i for i in ids if col[i] == c] for c in range(ncols)]
    order = {i: k for c in cols for k, i in enumerate(c)}

    def sweep(c, nbrs):
        def key(i):
            ys = [order[x] - (len(cols[col[x]]) - 1) / 2 for x in nbrs[i]]
            return sum(ys) / len(ys) if ys else order[i] - (len(c) - 1) / 2
        c.sort(key=key)
        for k, i in enumerate(c):
            order[i] = k

    for _ in range(4):
        for c in range(1, ncols):
            sweep(cols[c], deps)
        for c in range(ncols - 2, -1, -1):
            sweep(cols[c], kids)

    top0 = PAD + (BAND_HEAD if staged else 0)
    tallest = max((len(c) for c in cols), default=1)
    full_h = tallest * (H + GAP_Y) - GAP_Y
    pos = {}
    for c, members in enumerate(cols):
        top = top0 + (full_h - (len(members) * (H + GAP_Y) - GAP_Y)) / 2
        for k, i in enumerate(members):
            pos[i] = (PAD + c * (W + GAP_X), top + k * (H + GAP_Y))

    width = PAD * 2 + ncols * (W + GAP_X) - GAP_X
    height = top0 + full_h + PAD
    band_boxes = [(s, PAD + a * (W + GAP_X) - GAP_X / 2, PAD + (b + 1) * (W + GAP_X) - GAP_X / 2)
                  for s, a, b in bands]
    return pos, width, height, hub, deps, band_boxes


def edge_paths(nodes, pos, deps):
    status = {n["id"]: n.get("status", "pending") for n in nodes}
    incoming = {}
    for n in nodes:
        for dep in deps[n["id"]]:
            incoming.setdefault(n["id"], []).append(dep)
    out = []
    for child, srcs in incoming.items():
        srcs.sort(key=lambda s: pos[s][1])
        cx, cy = pos[child]
        for k, dep in enumerate(srcs):
            dx, dy = pos[dep]
            frac = 0.5 if len(srcs) == 1 else 0.18 + 0.64 * k / (len(srcs) - 1)
            sx, sy, ex, ey = dx + W, dy + H / 2, cx, cy + H * frac
            mx = (sx + ex) / 2
            path = f"M{sx:.0f},{sy:.0f} C{mx:.0f},{sy:.0f} {mx:.0f},{ey:.0f} {ex:.0f},{ey:.0f}"
            cls = "edge done" if status.get(dep) in FINISHED else "edge"
            out.append(f'<path class="{cls}" data-a="{esc(dep)}" data-b="{esc(child)}" d="{path}"/>')
    return out


def render(g):
    nodes = g.get("nodes", [])
    pos, w, h, hub, deps, bands = layout(nodes)
    counts = {s: sum(1 for n in nodes if n.get("status", "pending") == s) for s in STATUSES}
    finished = sum(counts[s] for s in FINISHED)
    total = len(nodes) or 1
    live = g.get("live", True) and any(n.get("status", "pending") not in FINISHED for n in nodes)
    stage_names = g.get("stages", {})

    band_html = []
    for k, (s, x0, x1) in enumerate(bands):
        members = [n for n in nodes if n.get("stage", 0) == s]
        done = sum(1 for n in members if n.get("status") in FINISHED)
        est = [n.get("est_weeks") for n in members if n.get("est_weeks")]
        est_txt = f" · {sum(e[0] for e in est):g}–{sum(e[1] for e in est):g} weeks" if est else ""
        band_html.append(
            f'<div class="band b{k % 2}" id="stage-{esc(s)}" style="left:{x0:.0f}px;width:{x1 - x0:.0f}px">'
            f'<div class="bandhead"><b>Stage {esc(s)}</b> {esc(stage_names.get(str(s), ""))}'
            f'<span>{done}/{len(members)} done{esc(est_txt)}</span></div></div>')

    cards = []
    for n in nodes:
        x, y = pos[n["id"]]
        st = n.get("status", "pending")
        rnd = int(n.get("round") or 1)
        kind = n.get("kind", "")
        cls = f"card s-{esc(st)}" + (" hub" if n["id"] == hub else "") + (f" k-{esc(kind)}" if kind else "")
        cards.append(
            f'<div class="{cls}" data-id="{esc(n["id"])}" tabindex="0" role="button" '
            f'style="left:{x:.0f}px;top:{y:.0f}px" title="{esc(n["id"] + " " + n.get("name", ""))}">'
            f'<div class="l1"><span class="nid">{esc(n["id"])}</span>'
            f'<span class="model">{esc(n.get("model", ""))}</span>'
            f'{f"<span class=round>r{rnd}</span>" if rnd > 1 else ""}'
            f'<span class="st t-{esc(st)}">{esc(st)}</span></div>'
            f'<div class="name">{esc(short_name(n))}</div></div>'
        )

    rows = []
    for n in nodes:
        st = n.get("status", "pending")
        writes = n.get("writes") or []
        wtxt = "<br>".join(esc(x) for x in writes) if isinstance(writes, list) else esc(writes)
        rows.append(
            f"<tr><td class='mono'>{esc(n['id'])}</td><td>{esc(short_name(n))}</td>"
            f"<td class='small'>{esc(n.get('does', ''))}</td><td>{esc(n.get('model', ''))}</td>"
            f"<td class='mono small'>{wtxt or esc(n.get('ref', ''))}</td>"
            f"<td class='mono'>{esc(', '.join(n.get('depends') or []) or '-')}</td>"
            f"<td><span class='st t-{esc(st)}'>{esc(st)}</span></td>"
            f"<td>{esc(n.get('round', 1))}</td><td class='small'>{esc(n.get('note', ''))}</td></tr>"
        )

    legend = "".join(f"<span class='st t-{s}'>{s} {counts[s]}</span>" for s in STATUSES if counts[s])
    stage_links = "".join(f'<a href="#stage-{esc(s)}">{esc(s)} {esc(stage_names.get(str(s), ""))}</a>'
                          for s, _, _ in bands)
    refresh = '<meta http-equiv="refresh" content="10">' if live else ""
    data = json.dumps([{k: v for k, v in n.items() if k in (
        "id", "name", "does", "model", "writes", "depends", "status", "note", "stage", "kind", "owner", "track",
        "ref", "exit", "est_weeks")} for n in nodes], ensure_ascii=False).replace("</", "<\\/")
    dark = ("--bg:#141412;--panel:#1d1d1a;--ink:#ecece6;--muted:#a3a39b;--line:#33332e;--edge:#5f5f58;--edge-done:#4fbf7d;"
            "--pending:#8f8f88;--running:#5b93f0;--review:#e0a922;--redeploy:#ef8243;--done:#45b072;--failed:#e05656;--self:#a07ae6;"
            "--band0:#191916;--band1:#141412;--hi:#5b93f0")
    return f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
{refresh}<title>{esc(g.get('title', 'Work graph'))}</title>
<style>
:root{{--bg:#f6f6f3;--panel:#fff;--ink:#1d1d1b;--muted:#6b6b66;--line:#dcdcd5;--edge:#b3b3ab;--edge-done:#3f9d63;
--pending:#8a8a84;--running:#2f6fdb;--review:#c58a00;--redeploy:#d9631e;--done:#2e8b57;--failed:#c62f2f;--self:#7a4fc9;
--band0:#efefe9;--band1:#f6f6f3;--hi:#2f6fdb}}
@media (prefers-color-scheme: dark){{:root:not([data-theme="light"]){{{dark}}}}}
:root[data-theme="dark"]{{{dark}}}
*{{box-sizing:border-box}} body{{margin:0;background:var(--bg);color:var(--ink);font:13px/1.4 system-ui,-apple-system,Segoe UI,sans-serif}}
.top{{position:sticky;left:0;width:100vw;padding:16px 16px 0}}
.top>div,.top>p,.top>nav{{max-width:1200px;margin-left:auto;margin-right:auto}}
.head{{display:flex;flex-wrap:wrap;align-items:baseline;gap:6px 12px;justify-content:center;text-align:center}}
h1{{font-size:17px;margin:0}} .count{{font-weight:600}}
.req{{color:var(--muted);font-size:12px;margin:4px auto 8px;max-width:760px;text-align:center}}
.bar{{height:5px;background:var(--line);border-radius:3px;overflow:hidden;max-width:520px;margin:0 auto 8px}} .bar>div{{height:100%;background:var(--done)}}
.meta{{display:flex;flex-wrap:wrap;gap:6px;justify-content:center;align-items:center;color:var(--muted);font-size:11px;margin-bottom:8px}}
.stages{{display:flex;flex-wrap:wrap;gap:6px;justify-content:center;margin-bottom:8px}}
.stages a{{color:var(--ink);text-decoration:none;border:1px solid var(--line);border-radius:999px;padding:1px 9px;font-size:11.5px;background:var(--panel)}}
.graph{{width:max-content;min-width:100%;padding:8px 16px 16px}} .canvas{{position:relative;margin:0 auto}}
.band{{position:absolute;top:0;bottom:0;border-radius:8px}} .band.b0{{background:var(--band0)}} .band.b1{{background:var(--band1)}}
.bandhead{{padding:8px 10px;font-size:13px}} .bandhead span{{display:block;color:var(--muted);font-size:11px}}
svg{{position:absolute;inset:0;overflow:visible;pointer-events:none}}
.edge{{fill:none;stroke:var(--edge);stroke-width:1.4}} .edge.done{{stroke:var(--edge-done)}} .edge.hi{{stroke:var(--hi);stroke-width:2.4}}
.card{{position:absolute;width:{W}px;height:{H}px;background:var(--panel);border:1px solid var(--line);border-left:4px solid var(--pending);border-radius:7px;padding:4px 8px;overflow:hidden;cursor:pointer}}
.card:focus-visible{{outline:2px solid var(--hi);outline-offset:2px}}
.card.hub{{border-width:2px 2px 2px 5px;box-shadow:0 2px 10px rgba(0,0,0,.08)}}
.card.k-gate,.card.k-release{{border-top-color:var(--review);border-right-color:var(--review);border-bottom-color:var(--review)}}
.card.dim{{opacity:.2}} .card.sel{{box-shadow:0 0 0 2px var(--hi)}}
.l1{{display:flex;align-items:center;gap:5px;font-size:10.5px;line-height:15px}}
.nid{{font-weight:700;font-family:ui-monospace,Consolas,monospace;font-size:11px}}
.model{{color:var(--muted);text-transform:uppercase;letter-spacing:.04em;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}}
.round{{color:var(--redeploy);font-weight:700}}
.l1 .st{{margin-left:auto}}
.name{{font-size:12.5px;font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:20px}}
.st{{font-size:10px;padding:0 6px;border-radius:999px;color:#fff;background:var(--pending);white-space:nowrap;line-height:15px}}
{''.join(f'.t-{s}{{background:var(--{s})}} .s-{s}{{border-left-color:var(--{s})}}' for s in STATUSES)}
.s-running{{box-shadow:0 0 0 2px color-mix(in srgb,var(--running) 30%,transparent)}}
#panel{{position:fixed;right:16px;bottom:16px;width:min(400px,calc(100vw - 32px));max-height:60vh;overflow:auto;background:var(--panel);
border:1px solid var(--line);border-radius:8px;padding:12px 14px;box-shadow:0 6px 24px rgba(0,0,0,.18);display:none;z-index:5}}
#panel h2{{font-size:14px;margin:0 0 4px}} #panel .m{{color:var(--muted);font-size:11px}} #panel a{{color:var(--hi);cursor:pointer}}
#panel button{{float:right;border:0;background:none;color:var(--muted);font-size:18px;cursor:pointer;line-height:1}}
.lower{{position:sticky;left:0;width:100vw;padding:0 16px 32px}} .lower>details{{max-width:1200px;margin:0 auto}}
summary{{cursor:pointer;color:var(--muted);font-size:12px;text-align:center}}
.tablewrap{{overflow-x:auto;margin-top:8px}}
table{{width:100%;border-collapse:collapse;background:var(--panel);border:1px solid var(--line);font-size:12px}}
th,td{{text-align:left;vertical-align:top;padding:5px 7px;border-bottom:1px solid var(--line)}} th{{color:var(--muted);font-weight:600}}
.mono{{font-family:ui-monospace,Consolas,monospace}} .small{{font-size:11px}}
</style></head><body>
<div class="top">
<div class="head"><h1>{esc(g.get('title', 'Work graph'))}</h1><span class="count">{finished}/{len(nodes)} finished</span></div>
<p class="req">{esc(g.get('request', ''))}</p>
<div class="bar"><div style="width:{100 * finished / total:.0f}%"></div></div>
<div class="meta">{legend}<span>updated {esc(g.get('updated', ''))}{' · refreshes every 10 s' if live else ''} · click a card for details</span></div>
{f'<nav class="stages">{stage_links}</nav>' if stage_links else ''}
</div>
<div class="graph"><div class="canvas" style="width:{w:.0f}px;height:{h:.0f}px">
{''.join(band_html)}
<svg width="{w:.0f}" height="{h:.0f}">{''.join(edge_paths(nodes, pos, deps))}</svg>
{''.join(cards)}
</div></div>
<aside id="panel" aria-live="polite"></aside>
<div class="lower"><details><summary>Node details</summary><div class="tablewrap"><table><thead><tr><th>node</th><th>name</th><th>does</th><th>model</th><th>writes</th><th>depends on</th><th>status</th><th>round</th><th>latest note</th></tr></thead>
<tbody>{''.join(rows)}</tbody></table></div></details></div>
<script>
const N={data},by=Object.fromEntries(N.map(n=>[n.id,n])),kids={{}},P=document.getElementById('panel');
N.forEach(n=>(n.depends||[]).forEach(d=>(kids[d]=kids[d]||[]).push(n.id)));
const e=s=>String(s??'').replace(/[&<>"]/g,c=>({{'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}})[c]);
function reach(id,f,seen=new Set()){{(f(id)||[]).forEach(x=>{{if(!seen.has(x)){{seen.add(x);reach(x,f,seen)}}}});return seen}}
const lk=i=>by[i]?`<a data-go="${{e(i)}}">${{e(i)}} ${{e(by[i].name)}}</a>`:e(i);
function show(id){{const n=by[id],up=reach(id,i=>by[i].depends),dn=reach(id,i=>kids[i]);
document.querySelectorAll('.card').forEach(c=>{{const i=c.dataset.id;c.classList.toggle('dim',i!==id&&!up.has(i)&&!dn.has(i));c.classList.toggle('sel',i===id)}});
document.querySelectorAll('.edge').forEach(p=>p.classList.toggle('hi',p.dataset.a===id||p.dataset.b===id));
const meta=[n.stage!==undefined?'Stage '+n.stage:'',n.kind,n.owner||n.model,n.status,n.est_weeks?n.est_weeks[0]+'–'+n.est_weeks[1]+' weeks':''].filter(Boolean).map(e).join(' · ');
P.innerHTML=`<button aria-label="Close">×</button><h2>${{e(n.id)}} · ${{e(n.name)}}</h2><div class="m">${{meta}}</div><p>${{e(n.does)}}</p>`+
(n.exit?`<p><b>Done when:</b> ${{e(n.exit)}}</p>`:'')+(n.writes&&n.writes.length?`<p class="m">Writes: ${{n.writes.map(e).join(', ')}}</p>`:'')+
(n.ref?`<p class="m">See ${{e(n.ref)}}</p>`:'')+(n.note?`<p class="m">Note: ${{e(n.note)}}</p>`:'')+
`<p><b>Needs:</b> ${{(n.depends||[]).map(lk).join(', ')||'nothing'}}</p><p><b>Unblocks:</b> ${{(kids[id]||[]).map(lk).join(', ')||'nothing'}}</p>`;
P.style.display='block';P.querySelector('button').onclick=clear;
P.querySelectorAll('[data-go]').forEach(a=>a.onclick=()=>go(a.dataset.go))}}
function clear(){{P.style.display='none';document.querySelectorAll('.card').forEach(c=>c.classList.remove('dim','sel'));document.querySelectorAll('.edge').forEach(p=>p.classList.remove('hi'))}}
function go(id){{const c=document.querySelector(`.card[data-id="${{CSS.escape(id)}}"]`);c.scrollIntoView({{behavior:'smooth',block:'center',inline:'center'}});show(id);c.focus({{preventScroll:true}})}}
document.querySelectorAll('.card').forEach(c=>{{c.onclick=()=>show(c.dataset.id);c.onkeydown=k=>{{if(k.key==='Enter'||k.key===' '){{k.preventDefault();show(c.dataset.id)}}}}}});
document.addEventListener('keydown',k=>{{if(k.key==='Escape')clear()}});
</script>
</body></html>"""


def exclude_from_git(out_dir):
    """Keep the graph folder out of git status when it lives inside a repo (not for docs folders)."""
    try:
        top = subprocess.run(["git", "-C", out_dir, "rev-parse", "--show-toplevel", "--git-dir"],
                             capture_output=True, text=True, timeout=10)
        if top.returncode != 0:
            return
        root, git_dir = top.stdout.split("\n")[:2]
        if not os.path.isabs(git_dir):
            git_dir = os.path.join(out_dir, git_dir)
        rel = os.path.relpath(out_dir, root).replace("\\", "/")
        if rel.startswith("..") or not rel.split("/")[-1].startswith(".orchestrate"):
            return
        pattern = "/" + rel.rstrip("/") + "/"
        exclude = os.path.join(git_dir, "info", "exclude")
        existing = open(exclude, encoding="utf-8").read() if os.path.exists(exclude) else ""
        if pattern not in existing.splitlines():
            os.makedirs(os.path.dirname(exclude), exist_ok=True)
            with open(exclude, "a", encoding="utf-8") as f:
                f.write(("\n" if existing and not existing.endswith("\n") else "") + pattern + "\n")
    except Exception:
        pass


def main():
    ap = argparse.ArgumentParser(description="Render a work graph to HTML.")
    ap.add_argument("graph")
    ap.add_argument("--out", action="append", default=[], help="output path (repeatable); default GRAPH.html")
    ap.add_argument("--set", nargs=2, action="append", metavar=("ID", "STATUS"), default=[])
    ap.add_argument("--note", nargs=2, action="append", metavar=("ID", "TEXT"), default=[])
    ap.add_argument("--round", nargs=2, action="append", metavar=("ID", "N"), default=[])
    ap.add_argument("--open", action="store_true")
    a = ap.parse_args()

    with open(a.graph, encoding="utf-8") as f:
        g = json.load(f)
    by_id = {n["id"]: n for n in g.get("nodes", [])}
    for nid, st in a.set:
        if nid not in by_id:
            sys.exit(f"unknown node {nid}")
        if st not in STATUSES:
            sys.exit(f"status must be one of {STATUSES}")
        by_id[nid]["status"] = st
    for nid, txt in a.note:
        by_id[nid]["note"] = txt
    for nid, n in a.round:
        by_id[nid]["round"] = int(n)
    g["updated"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    with open(a.graph, "w", encoding="utf-8") as f:
        json.dump(g, f, indent=1, ensure_ascii=False)

    page = render(g)
    for out in a.out or [os.path.splitext(a.graph)[0] + ".html"]:
        with open(out, "w", encoding="utf-8") as f:
            f.write(page)
        exclude_from_git(os.path.dirname(os.path.abspath(out)))
        print(os.path.abspath(out))
        if a.open:
            webbrowser.open("file:///" + os.path.abspath(out).replace("\\", "/"))


if __name__ == "__main__":
    main()
