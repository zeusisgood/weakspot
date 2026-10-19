#!/usr/bin/env bash
# Minecraft の版ごとの配布の値（1 か所の表）。publish-*.sh が source で読む。
# 版を足すときは、ここと ci.yml の matrix に足す。
#   mc_info <Minecraft の版>  → LOADER（Modrinth の loaders）・CF_LOADER・CF_JAVA・CF_TYPE（CurseForge の版の種類の slug）
#   release_mc_versions <版>  → その版の Release に付いている jar の Minecraft の版（1 行に 1 つ）

mc_info() {
  case "$1" in
    1.12.2)
      LOADER=forge
      CF_LOADER=Forge
      CF_JAVA="Java 8"
      CF_TYPE=minecraft-1-12
      ;;
    *)
      echo "::error::Unknown Minecraft version: $1 (add it to .github/scripts/mc-versions.sh)"
      return 1
      ;;
  esac
  export LOADER CF_LOADER CF_JAVA CF_TYPE
}

# jar の名前は weakspot-<Minecraft の版>-<版>.jar
release_mc_versions() {
  local version="$1"
  gh release view "v$version" --json assets --jq '.assets[].name' |
    sed -n "s/^weakspot-\(.*\)-${version//./\\.}\.jar$/\1/p"
}
