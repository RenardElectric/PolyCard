#!/usr/bin/env bash
set -euo pipefail

ai_dir="${AI_WORKSPACE:-.release-ai}"
notes="$ai_dir/release-notes.md"
prompt="$ai_dir/prompt.md"

if [[ -e "$notes" ]]; then
  echo "::error::release-notes.md unexpectedly exists before Copilot runs."
  exit 1
fi

if [[ ! -s "$prompt" ]]; then
  echo "::error::The Copilot prompt is missing or empty."
  exit 1
fi

(
  cd "$ai_dir"
  copilot \
    --prompt "$(cat prompt.md)" \
    --allow-tool=read \
    --allow-tool='write(release-notes.md)' \
    --no-ask-user
)

if [[ ! -s "$notes" ]]; then
  echo "::error::Copilot did not generate release notes."
  exit 1
fi
