#!/usr/bin/env bash
# GitHub Release（タグ v<版>）の jar を、CurseForge のプロジェクトに新しいファイルとして上げる。
# 使い方: publish-curseforge.sh <版>
# 要るもの: 環境変数 CURSEFORGE_TOKEN（作者の画面の API トークン）・CURSEFORGE_PROJECT_ID・GH_TOKEN。
# トークンか ID がなければ何もせず成功で終わる。
# CurseForge のアップロード API では、上げ済みの版を確かめられない。二重に上げないよう、呼ぶ側が
# 「Release を今作ったときだけ」流す（ci.yml）か、手動で 1 回だけ流す（curseforge-publish.yml）。
set -euo pipefail

version="$1"
if [ -z "${CURSEFORGE_TOKEN:-}" ] || [ -z "${CURSEFORGE_PROJECT_ID:-}" ]; then
  echo "::notice::CURSEFORGE_TOKEN or CURSEFORGE_PROJECT_ID is not set; skipped publishing $version to CurseForge."
  exit 0
fi

api=${CURSEFORGE_API:-https://minecraft.curseforge.com/api}
auth="X-Api-Token: $CURSEFORGE_TOKEN"
work=$(mktemp -d)

# 失敗したら、CurseForge が返した理由（本文）も出す
call() {
  local what="$1"; shift
  if ! curl --fail-with-body -sS -o "$work/response.txt" "$@"; then
    echo "::error::CurseForge: $what failed."
    cat "$work/response.txt"; echo
    exit 1
  fi
}

# ゲームの版の ID を引く: Minecraft 1.12.2・Forge・Java 8（あれば、環境の Client・Server も）
call "reading version types" -H "$auth" "$api/game/version-types"
mv "$work/response.txt" "$work/types.json"
call "reading game versions" -H "$auth" "$api/game/versions"
mv "$work/response.txt" "$work/versions.json"
required=$(jq -c --slurpfile types "$work/types.json" '
  ($types[0] | map({(.slug): .id}) | add) as $t
  | [ .[] | select(
        (.gameVersionTypeID == $t["minecraft-1-12"] and .name == "1.12.2")
        or (.gameVersionTypeID == $t["modloader"] and .name == "Forge")
        or (.gameVersionTypeID == $t["java"] and .name == "Java 8")
      ) | .id ]' "$work/versions.json")
if [ "$(jq length <<< "$required")" != "3" ]; then
  echo "::error::Could not find the CurseForge version IDs for 1.12.2 / Forge / Java 8 (got $required)."
  exit 1
fi
environments=$(jq -c --slurpfile types "$work/types.json" '
  ($types[0] | map(select(.slug == "environment") | .id)) as $env
  | [ .[] | select((.gameVersionTypeID as $id | $env | index($id)) and (.name == "Client" or .name == "Server")) | .id ]
  ' "$work/versions.json")
ids=$(jq -c -n --argjson a "$required" --argjson b "$environments" '$a + $b')
echo "Game version IDs: $ids"

jar="weakspot-$version.jar"
gh release download "v$version" --pattern "$jar" --dir "$work"

changelog=$(bash "$(dirname "$0")/changelog.sh" "$version")
jq -n \
  --arg name "Weak Spot Mining $version" \
  --arg changelog "$changelog" \
  --argjson ids "$ids" \
  '{
    changelog: $changelog,
    changelogType: "markdown",
    displayName: $name,
    gameVersions: $ids,
    releaseType: "release"
  }' > "$work/metadata.json"

call "uploading the file" -X POST "$api/projects/$CURSEFORGE_PROJECT_ID/upload-file" \
  -H "$auth" \
  -F "metadata=<$work/metadata.json;type=application/json" \
  -F "file=@$work/$jar;type=application/java-archive"
echo "Published $version to CurseForge."
