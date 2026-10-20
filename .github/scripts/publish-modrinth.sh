#!/usr/bin/env bash
# GitHub Release（タグ v<版>）の、Minecraft の版の jar を、Modrinth のプロジェクトに新しいバージョンとして上げる。
# バージョンの番号は <版>+<Minecraft の版>（例 1.11.12+1.12.2）。名前は display_name（mc-versions.sh）。
# 使い方: publish-modrinth.sh <版> <Minecraft の版>
# 要るもの: 環境変数 MODRINTH_TOKEN（スコープ「バージョンを作成する」「バージョンを読む」「プロジェクトを読む」）・
# MODRINTH_PROJECT_ID・GH_TOKEN。トークンか ID がなければ何もせず成功で終わる。
# Modrinth にその番号がもうあれば何もしない（何度流しても二重にならない）。
set -euo pipefail

version="$1"
mc="$2"
# shellcheck source=SCRIPTDIR/mc-versions.sh
source "$(dirname "$0")/mc-versions.sh"
mc_info "$mc"
number="$version+$mc"
if [ -z "${MODRINTH_TOKEN:-}" ] || [ -z "${MODRINTH_PROJECT_ID:-}" ]; then
  echo "::notice::MODRINTH_TOKEN or MODRINTH_PROJECT_ID is not set; skipped publishing $number to Modrinth."
  exit 0
fi

api=${MODRINTH_API:-https://api.modrinth.com/v2}
ua="zeusisgood/weakspot (GitHub Actions)"

# 審査中・非公開のプロジェクトは、認証なしでは見えないので、トークンを付けて聞く
count=$(curl -fsS -H "User-Agent: $ua" -H "Authorization: $MODRINTH_TOKEN" \
  "$api/project/$MODRINTH_PROJECT_ID/version" |
  jq --arg v "$number" '[.[] | select(.version_number == $v)] | length')
if [ "$count" != "0" ]; then
  echo "Modrinth already has $number; nothing to do."
  exit 0
fi

work=$(mktemp -d)
jar=$(release_jar "$version" "$mc")
gh release download "v$version" --pattern "$jar" --dir "$work"

changelog=$(bash "$(dirname "$0")/changelog.sh" "$version")

jq -n \
  --arg project "$MODRINTH_PROJECT_ID" \
  --arg number "$number" \
  --arg name "$(display_name "$version" "$mc")" \
  --arg changelog "$changelog" \
  --arg mc "$mc" \
  --arg loader "$LOADER" \
  '{
    project_id: $project,
    name: $name,
    version_number: $number,
    changelog: $changelog,
    game_versions: [$mc],
    loaders: [$loader],
    version_type: "release",
    featured: false,
    dependencies: [],
    file_parts: ["file"],
    primary_file: "file"
  }' > "$work/data.json"

# 失敗したら、Modrinth が返した理由（本文）も出す
curl --fail-with-body -sS -X POST "$api/version" \
  -H "User-Agent: $ua" \
  -H "Authorization: $MODRINTH_TOKEN" \
  -F "data=<$work/data.json;type=application/json" \
  -F "file=@$work/$jar;type=application/java-archive" -o "$work/response.txt" ||
  { echo "::error::Modrinth: uploading the version failed."; cat "$work/response.txt"; echo; exit 1; }
echo "Published $number to Modrinth."
