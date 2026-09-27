#!/usr/bin/env bash
# GitHub Release（タグ v<版>）の jar を、Modrinth のプロジェクトに新しいバージョンとして上げる。
# 使い方: publish-modrinth.sh <版>
# 要るもの: 環境変数 MODRINTH_TOKEN（スコープ「バージョンを作成する」「バージョンを読む」「プロジェクトを読む」）・
# MODRINTH_PROJECT_ID・GH_TOKEN。トークンか ID がなければ何もせず成功で終わる。
# Modrinth にその版がもうあれば何もしない（何度流しても二重にならない）。
set -euo pipefail

version="$1"
if [ -z "${MODRINTH_TOKEN:-}" ] || [ -z "${MODRINTH_PROJECT_ID:-}" ]; then
  echo "::notice::MODRINTH_TOKEN or MODRINTH_PROJECT_ID is not set; skipped publishing $version to Modrinth."
  exit 0
fi

api=${MODRINTH_API:-https://api.modrinth.com/v2}
ua="zeusisgood/weakspot (GitHub Actions)"

# 審査中・非公開のプロジェクトは、認証なしでは見えないので、トークンを付けて聞く
count=$(curl -fsS -H "User-Agent: $ua" -H "Authorization: $MODRINTH_TOKEN" \
  "$api/project/$MODRINTH_PROJECT_ID/version" |
  jq --arg v "$version" '[.[] | select(.version_number == $v)] | length')
if [ "$count" != "0" ]; then
  echo "Modrinth already has $version; nothing to do."
  exit 0
fi

work=$(mktemp -d)
jar="weakspot-$version.jar"
gh release download "v$version" --pattern "$jar" --dir "$work"

# 更新内容: 英語のお知らせ weakspot.news.<版> の 1 行と、GitHub の Release へのリンク
news=$(awk -v k="weakspot.news.$version" 'index($0, k "=") == 1 {print substr($0, length(k) + 2)}' \
  src/main/resources/assets/weakspot/lang/en_us.lang)
release_url="https://github.com/zeusisgood/weakspot/releases/tag/v$version"
if [ -n "$news" ]; then
  changelog="$news"$'\n\n'"Full changelog: $release_url"
else
  changelog="Full changelog: $release_url"
fi

jq -n \
  --arg project "$MODRINTH_PROJECT_ID" \
  --arg version "$version" \
  --arg changelog "$changelog" \
  '{
    project_id: $project,
    name: $version,
    version_number: $version,
    changelog: $changelog,
    game_versions: ["1.12.2"],
    loaders: ["forge"],
    version_type: "release",
    featured: false,
    dependencies: [],
    file_parts: ["file"],
    primary_file: "file"
  }' > "$work/data.json"

curl -fsS -X POST "$api/version" \
  -H "User-Agent: $ua" \
  -H "Authorization: $MODRINTH_TOKEN" \
  -F "data=<$work/data.json;type=application/json" \
  -F "file=@$work/$jar;type=application/java-archive" > /dev/null
echo "Published $version to Modrinth."
