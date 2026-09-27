#!/usr/bin/env bash
# 配布サイトに載せる更新内容を出す: 英語のお知らせ weakspot.news.<版>（en_us.lang）の 1 行と、GitHub の Release へのリンク。
# 使い方: changelog.sh <版>
set -euo pipefail
version="$1"
news=$(awk -v k="weakspot.news.$version" 'index($0, k "=") == 1 {print substr($0, length(k) + 2)}' \
  src/main/resources/assets/weakspot/lang/en_us.lang)
release_url="https://github.com/zeusisgood/weakspot/releases/tag/v$version"
if [ -n "$news" ]; then
  printf '%s\n\nFull changelog: %s\n' "$news" "$release_url"
else
  printf 'Full changelog: %s\n' "$release_url"
fi
