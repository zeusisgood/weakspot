#!/usr/bin/env bash
# 配布サイトに載せる更新内容を出す: 英語のお知らせ weakspot.news.<版>（lang/en_us.json）の 1 行と、GitHub の Release へのリンク。
# 使い方: changelog.sh <版>
set -euo pipefail
version="$1"
news=$(jq -r --arg k "weakspot.news.$version" '.[$k] // empty' lang/en_us.json)
release_url="https://github.com/zeusisgood/weakspot/releases/tag/v$version"
if [ -n "$news" ]; then
  printf '%s\n\nFull changelog: %s\n' "$news" "$release_url"
else
  printf 'Full changelog: %s\n' "$release_url"
fi
