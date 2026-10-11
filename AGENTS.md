# Agents in Apoapsis

This guide tells AI agents how work is split, sized, checked and committed in this repo. Read it before you change anything, and follow the same rules as any human contributor.

## Roles

- **Opus** designs, decides module boundaries, reviews every agent branch and runs the merge. It is the only role that merges.
- **Sonnet** takes rendering, visual UI, worldgen, cross-system client and server lifecycle, second-round nodes, and any task that needs much exploration or many files at once. It may hand a scoped part to Haiku.
- **Haiku** takes server logic, data, hardening, small fixes, docs and research, each as one bounded task.
- **47noodles** approves design gates, go/no-go calls and releases.

Every node in `docs/plan/work-graph.json` names its `model` (`sonnet` or `haiku`) and its `owner`. A node with a human or Opus owner is not agent work.

## Starting a task

- Read the node in `docs/plan/work-graph.json` (by id, using a line-range read) and check its `owner`. Do not start a node owned by a human or by Opus.
- Read only the files the node names, plus `CONTRIBUTING.md` for licence rules.
- Check that every dependency is `done` before you write any code.
- Work on one branch in your own worktree. Never reuse a worktree that another agent is using.

## Why

A review of 21 Haiku branches found the code clean: none was rejected and none broke the build. Every real failure was visual or only showed up at runtime (a grey planet, textures dropped on dimension change, overlapping labels, a plume covering the screen), and compiling and tests missed all of them. So work that needs judgement about how things look or how systems fit together goes to Sonnet, and every visual change is checked in the running game.

## Sizing Haiku tasks

- Haiku runs with a 100k-token auto-compact window to keep costs down. A task whose reading does not fit stalls in a compaction loop and is stopped, so size it small or give it to Sonnet.
- One concern per task. If the brief needs "and also", split it.
- Read at most about 5 files or 40k tokens. Use line-range reads and `file:line` pointers, not whole files.
- Pipe long command output through `tail` or `grep`. Never print a whole file over 150 lines.
- Edit JSON by script, not by hand, and check that it parses afterwards.
- Stop and report when the work needs a decision about how systems fit together.

## Verification

- Run the check named in the node before every commit. For mod code, that is the Gradle build and test tasks for the touched modules.
- `./gradlew runGameTestServer` must pass before a branch is handed back.
- Visual nodes run the client and take screenshots at the size the node names. Check for overlaps and clipping.
- Performance is measured on a real save, not an empty world or a test fixture (a planet map timed at 47 ms in the flat GameTest world took 66 s on the real save). Record the number in the node.
- Check free memory before starting another game client, and close the old one first.
- Docs and plan files: run the renderer and grep for forbidden strings.
- Tests are not weakened or deleted to make a check pass. A failing check is fixed in the change or reported.

## Work graph

- The source of truth is `docs/plan/work-graph.json`. Regenerate the page with `python -X utf8 tools/plan/render_graph.py docs/plan/work-graph.json`.
- Change a node's `model`, `status` or `owner` in the JSON, then re-render. Never edit the HTML by hand.
- Dependencies flow left to right. A node starts only when every node it depends on is `done`.

## Repo rules

- Generated art comes from reproducible source through `tools/art/gen_textures.py`. Do not hand-edit generated textures.
- Never create or edit `eula.txt`, and never run a dedicated server, without the lead's say-so. Agents do not accept the EULA.
- Do not edit `.env`, credentials, deploy scripts or CI workflows unless the node names them.
- Follow `CONTRIBUTING.md` for licences and clean-room rules. Do not copy code, art or text from non-open projects.
- Do not write personal names, personal email addresses or local machine paths into committed files. Use GitHub handles only.
- Several agents work in parallel, each in its own worktree. Do not touch another worktree.
- Agents never push, merge or release. Commit on your own branch in your own worktree, then report and let Opus merge.
- Every commit ends with exactly these two trailers, and is signed off with `git commit -s`:
  - `Signed-off-by: 47noodles <167390129+47noodles@users.noreply.github.com>`
  - `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`
- If a rule here conflicts with a node, stop and report the conflict. Do not pick one silently.
