#!/usr/bin/env bash
# Minecraft の版ごとの配布の値（1 か所の表）。publish-*.sh が source で読む。
# 版を足すときは、ここと ci.yml の matrix に足す。
#   mc_info <Minecraft の版>  → LOADER（Modrinth の loaders）・CF_LOADER・CF_JAVA・CF_TYPE（CurseForge の版の種類の slug）
#   release_mc_versions <版>  → その版の Release に付いている jar の Minecraft の版（1 行に 1 つ）
#   release_jar <版> <Minecraft の版>  → その Release の、その Minecraft の版の jar の名前（mc_info のあとに呼ぶ）
#   display_name <版> <Minecraft の版>  → 配布サイトの表示名（mc_info のあとに呼ぶ）

mc_info() {
  case "$1" in
    1.12.2)
      LOADER=forge
      CF_LOADER=Forge
      CF_JAVA="Java 8"
      CF_TYPE=minecraft-1-12
      ;;
    1.20.1)
      LOADER=forge
      CF_LOADER=Forge
      CF_JAVA="Java 17"
      CF_TYPE=minecraft-1-20
      ;;
    1.21.1)
      LOADER=neoforge
      CF_LOADER=NeoForge
      CF_JAVA="Java 21"
      CF_TYPE=minecraft-1-21
      ;;
    *)
      echo "::error::Unknown Minecraft version: $1 (add it to .github/scripts/mc-versions.sh)"
      return 1
      ;;
  esac
  export LOADER CF_LOADER CF_JAVA CF_TYPE
}

# jar の名前は weakspot-<Minecraft の版>-<ローダー>-<版>.jar。1.11.12〜1.12.1 の Release は weakspot-<Minecraft の版>-<版>.jar で、
# 手動で上げ直せるように、どちらの名前も読む
release_mc_versions() {
  local version="$1"
  gh release view "v$version" --json assets --jq '.assets[].name' |
    sed -n "s/^weakspot-\([0-9.]*\)-\([a-z]*-\)\{0,1\}${version//./\\.}\.jar$/\1/p"
}

release_jar() {
  local version="$1" mc="$2" names jar
  names=$(gh release view "v$version" --json assets --jq '.assets[].name')
  for jar in "weakspot-$mc-$LOADER-$version.jar" "weakspot-$mc-$version.jar"; do
    if grep -qxF "$jar" <<< "$names"; then
      echo "$jar"
      return 0
    fi
  done
  echo "::error::Release v$version has no jar for Minecraft $mc." >&2
  return 1
}

# Mod の版と Minecraft の版の数字が似ているので、MC とローダーの印で区別する（doc/development.md の「版の表記」）
display_name() {
  echo "Weak Spot Mining $1 for MC $2 ($CF_LOADER)"
}
