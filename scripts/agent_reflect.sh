#!/usr/bin/env bash
# agent_reflect.sh — minimal three-stage Codex reflection pipeline for any repo
# Uses `codex --dangerously-bypass-approvals-and-sandbox` for all Codex exec calls
# Usage: agent_reflect.sh <repo|path> [--auto]

set -euo pipefail

usage() {
  cat <<EOF
Usage: $(basename "$0") <repo|path> [--auto]

Positional:
  repo|path   Absolute/relative path or basename of the repo to study.

Options:
  --auto      After drafting /tmp/<repo>-improvements.md, use Codex to rewrite
              AGENTS.md automatically (backup saved to /tmp/<repo>-backup-AGENTS.md).

Environment overrides:
  LOGS_ROOT   Defaults to ~. Codex logs are expected at \$LOGS_ROOT/.codex/sessions.

Stages:
  1. Collect all user-only Codex transcripts mentioning the repo into /tmp.
  2. Run two Codex reflections (meta + AGENTS recommendations) via codex exec.
  3. If --auto, apply recommendations to AGENTS.md.
EOF
}

ANALYSIS_PROMPT_TEMPLATE=$(cat <<'PROMPT'
You are running a short "agent reflection" pass for the repo at __REPO_DIR__.

Artifacts already prepared for you:
- User-only Codex transcripts live under __CORPUS_DIR__. Each .txt file includes metadata plus numbered user messages [0001], [0002], etc.
- The manifest at __MANIFEST__ lists every transcript with counts.
- Current guardrails live in AGENTS.md at __AGENTS_FILE__. Do not edit files; just read them for context.

Deliverable:
Produce a Markdown memo with the following sections:
1. Repeated Expectations — bullet the strongest recurring instructions the user gives agents. Cite transcripts as <relative/path.txt>:[0007].
2. Meta Themes — describe how the user wants the repo developed (tooling stance, UX tone, logging habits, etc.). Tie every claim to at least one transcript citation.
3. Debugging Expectations & Missing Directions — summarize exactly how the user tells agents to debug (tools, logs, comparisons, UX replays) and call out any repeated "missing" instructions they keep asking to see documented. Cite transcripts for every point.
4. Change Directions for AGENTS.md — list the categories of edits the user routinely asks for. Reference both the transcripts and any relevant spots in AGENTS.md (cite as AGENTS.md:#Lxx if helpful). Do not propose wording yet; just explain the intent.

Guidelines:
- Only summarize user-authored content from the transcripts.
- Treat the memo as guidance for another agent who will later rewrite AGENTS.md.
- Keep the tone direct and practical.
- You may run read-only shell commands (cat/sed/rg) to inspect transcripts, but do not modify files.
- Pay special attention to debugging workflows plus any repeated user complaints about missing guidance.
PROMPT
)

die() { printf "error: %s\n" "$*" >&2; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || die "missing dependency: $1"; }

abspath() {
  python3 - <<'PY' "$1"
import os, sys
print(os.path.abspath(os.path.expanduser(sys.argv[1])))
PY
}

slugify() {
  printf "%s" "$1" | tr '[:upper:]' '[:lower:]' | tr -cs '[:alnum:]' '-' | sed -E 's/^-+|-+$//g'
}

resolve_repo() {
  local arg="$1" cand
  [ -n "$arg" ] || die "missing repo argument"
  cand=$(abspath "$arg" 2>/dev/null || true)
  if [ -n "$cand" ] && [ -d "$cand" ]; then printf "%s" "$cand"; return; fi
  for base in "$PWD" "$HOME/Desktop/Development" "$HOME/Development" "$HOME/Desktop" "$HOME/code" "$HOME/projects" "$HOME"; do
    cand=$(abspath "$base/$arg" 2>/dev/null || true)
    if [ -n "$cand" ] && [ -d "$cand" ]; then printf "%s" "$cand"; return; fi
  done
  die "could not find repo directory for '$arg'"
}

REPO_ARG=""
AUTO=0

while [ $# -gt 0 ]; do
  case "$1" in
    --auto) AUTO=1 ;;
    -h|--help) usage; exit 0 ;;
    *) if [ -z "$REPO_ARG" ]; then REPO_ARG="$1"; else usage; exit 1; fi ;;
  esac
  shift
done

[ -n "$REPO_ARG" ] || { usage; exit 1; }

need python3
need codex
CODEX=(codex --dangerously-bypass-approvals-and-sandbox)

REPO_DIR=$(resolve_repo "$REPO_ARG")
REPO_NAME=$(basename "$REPO_DIR")
AGENTS_FILE="$REPO_DIR/AGENTS.md"
[ -f "$AGENTS_FILE" ] || die "AGENTS.md not found at $AGENTS_FILE"

LOGS_ROOT=${LOGS_ROOT:-$HOME}
SESSIONS_DIR="$LOGS_ROOT/.codex/sessions"
[ -d "$SESSIONS_DIR" ] || die "Codex sessions directory not found at $SESSIONS_DIR"

SLUG=$(slugify "$REPO_NAME")
CORPUS_DIR="/tmp/${SLUG}-convos"
MANIFEST="$CORPUS_DIR/manifest.json"
REFLECTION_MD="/tmp/${SLUG}-reflection.md"
IMPROV_MD="/tmp/${SLUG}-improvements.md"
BACKUP_AGENTS="/tmp/${SLUG}-backup-AGENTS.md"

echo "== Stage 1: collecting user-only transcripts into $CORPUS_DIR"
rm -rf "$CORPUS_DIR"
mkdir -p "$CORPUS_DIR"

python3 - "$SESSIONS_DIR" "$REPO_DIR" "$CORPUS_DIR" "$MANIFEST" <<'PY'
import json, os, sys, datetime, pathlib

sessions = pathlib.Path(sys.argv[1]).expanduser()
repo_dir = os.path.abspath(sys.argv[2])
out_root = pathlib.Path(sys.argv[3])
manifest_path = pathlib.Path(sys.argv[4])
repo_name = os.path.basename(repo_dir)

matched = []
for path in sessions.rglob('*.jsonl'):
    try:
        blob = path.read_text('utf-8', errors='ignore')
    except Exception:
        continue
    if repo_dir in blob or repo_name in blob:
        matched.append(path)

def to_text(payload):
    if isinstance(payload, str):
        return payload
    if isinstance(payload, (int, float)):
        return str(payload)
    if isinstance(payload, dict):
        for key in ("content", "text", "message", "input"):
            if key in payload and payload[key]:
                return to_text(payload[key])
    return json.dumps(payload, ensure_ascii=False)

def to_iso(ts):
    if not ts:
        return ""
    if isinstance(ts, (int, float)):
        try:
            return datetime.datetime.fromtimestamp(ts).isoformat()
        except Exception:
            return str(ts)
    return str(ts)

manifest = []
total_msgs = 0

for src in sorted(matched):
    user_msgs = []
    try:
        lines = src.read_text('utf-8', errors='ignore').splitlines()
    except Exception:
        continue
    for line in lines:
        line = line.strip()
        if not line:
            continue
        try:
            evt = json.loads(line)
        except Exception:
            continue
        role = evt.get('role') or (evt.get('message') or {}).get('role')
        if role != 'user':
            continue
        content = evt.get('content') or evt.get('text') or (evt.get('message') or {}).get('content') or evt.get('input')
        text = to_text(content).rstrip()
        ts = evt.get('ts') or evt.get('timestamp') or evt.get('time')
        user_msgs.append((to_iso(ts), text))
    if not user_msgs:
        continue
    rel = src.relative_to(sessions)
    parts = rel.parts
    if len(parts) >= 4:
        out_dir = out_root / parts[-4] / parts[-3] / parts[-2]
    else:
        out_dir = out_root
    out_dir.mkdir(parents=True, exist_ok=True)
    out_file = out_dir / (src.stem + '.txt')
    with out_file.open('w', encoding='utf-8') as fh:
        fh.write(f"source={src}\nrepo={repo_dir}\nsession={src.stem}\nmessages={len(user_msgs)}\n---\n")
        for idx, (ts, text) in enumerate(user_msgs, 1):
            fh.write(f"[{idx:04d}] {ts}\n{text}\n\n")
    manifest.append({
        "session": src.stem,
        "source_log": str(src),
        "transcript": str(out_file.relative_to(out_root)),
        "user_message_count": len(user_msgs)
    })
    total_msgs += len(user_msgs)

if not manifest:
    manifest_path.write_text(json.dumps({"repo": repo_dir, "sessions": []}, indent=2), encoding='utf-8')
    sys.exit("No Codex sessions referenced the target repo. Nothing to reflect on.")

manifest_path.write_text(json.dumps({"repo": repo_dir, "sessions": manifest}, indent=2), encoding='utf-8')

readme = out_root / 'README.md'
readme.write_text(f"""Repo-specific Codex user transcripts\n\n- Repo: {repo_dir}\n- Source logs: {sessions}\n- Transcript root: {out_root}\n\nEach transcript lists user-only messages with counters like [0001].\nWhen citing, use <relative transcript path>:[0001] to point at a message.\nManifest file: manifest.json\n""", encoding='utf-8')

print(f"Matched {len(manifest)} sessions / {total_msgs} user messages -> {out_root}")
PY

echo "== Stage 2A: generating meta reflection -> $REFLECTION_MD"
ANALYSIS_PROMPT_TEMPLATE="$ANALYSIS_PROMPT_TEMPLATE" python3 - "$REPO_DIR" "$CORPUS_DIR" "$MANIFEST" "$AGENTS_FILE" <<'PY' |
"${CODEX[@]}" exec -C "$REPO_DIR" - > "$REFLECTION_MD"
import os, sys
template = os.environ["ANALYSIS_PROMPT_TEMPLATE"]
replacements = {
    "__REPO_DIR__": sys.argv[1],
    "__CORPUS_DIR__": sys.argv[2],
    "__MANIFEST__": sys.argv[3],
    "__AGENTS_FILE__": sys.argv[4],
}
for key, value in replacements.items():
    template = template.replace(key, value)
print(template, end="")
PY

echo "== Stage 2B: drafting AGENTS.md recommendations -> $IMPROV_MD"
"${CODEX[@]}" exec -C "$REPO_DIR" - > "$IMPROV_MD" <<PROMPT
You now own the AGENTS.md upgrade plan for repo ${REPO_DIR}.

Inputs:
- Current guardrails: ${AGENTS_FILE}
- User-only transcript corpus: ${CORPUS_DIR} (manifest: ${MANIFEST})
- Prior reflection notes: ${REFLECTION_MD}

Goal:
Produce a Markdown document that another contributor can apply directly without more digging. Structure it exactly as:
1. Summary of Meta Themes — table or bullets capturing the repeated expectations (cite transcripts <relative.txt>:[0004]).
2. Gap Analysis — map each theme against what AGENTS.md currently says (cite AGENTS.md:#Lxx when covered, otherwise mark as missing).
3. Insertion-Ready Recommendations — group by theme (e.g., Demo Scope, Research Protocol, UX Prescription, Tool/Prompt Governance, Typing & Guardrails, Local Dev/Webhooks, Testing & Logging). For every item:
   - Quote or paraphrase the supporting user instruction with transcript citation(s).
   - State whether AGENTS.md already covers it; reference the relevant section/line or say "missing".
   - Provide exact wording/bullet(s) to add, noting the target subsection or suggesting a new subsection.

Rules:
- Cite every factual statement back to the transcript corpus (format: <relative.txt>:[0007]). Include AGENTS.md locations when comparing coverage.
- Keep recommendations exhaustive: if the user repeated an instruction, it must appear.
- Output must be valid Markdown and large enough to be actionable.
- Do NOT edit any files here; just write the plan to stdout.
PROMPT

if [ $AUTO -eq 1 ]; then
  echo "== Stage 3: applying recommendations with Codex (--auto)"
  cp "$AGENTS_FILE" "$BACKUP_AGENTS"
  echo "Backup saved to $BACKUP_AGENTS"
  "${CODEX[@]}" exec -C "$REPO_DIR" - <<PROMPT
You now need to update AGENTS.md at ${AGENTS_FILE} using the detailed plan stored in ${IMPROV_MD}.

Process:
1. Read ${IMPROV_MD} and the transcript corpus under ${CORPUS_DIR} so you fully understand each recommended change.
2. Update only AGENTS.md, inserting the new language exactly where specified (create new subsections if needed). Preserve existing guidance that still applies.
3. After editing, show a concise `git diff AGENTS.md` so the operator can review.
4. Do not touch other files; respect the backup already saved at ${BACKUP_AGENTS}.

Finish when AGENTS.md reflects every recommendation from ${IMPROV_MD}.
PROMPT
else
  echo "== Manual merge step"
  echo "Review $IMPROV_MD then edit $AGENTS_FILE yourself or re-run with --auto."
fi

echo "\nDone. Key artifacts:"
printf '  - %s\n' "$CORPUS_DIR" "$REFLECTION_MD" "$IMPROV_MD"
if [ $AUTO -eq 1 ]; then
  printf '  - %s\n' "$BACKUP_AGENTS"
fi
