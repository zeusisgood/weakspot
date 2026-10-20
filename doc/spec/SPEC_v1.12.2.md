# 弱点破壊 Mod 追加仕様書（v1.12.2）

`SPEC_v1.12.1.md`（Mod 1.12.1）に対する**パッチ**の仕様（配布するファイルの名前と、配布サイトの表示名の決まりを作る）。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.12.2.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860 と 1.21.1 / NeoForge 21.1.264（両方の jar を同じ番号で出す）
- 前提: 1.12.1 が `main` に取り込み済み
- ユーザーの困りごと: CurseForge のファイルの一覧で、`Weak Spot Mining 1.12.0 (1.12.2)` のような表示名が分かりづらい
  - Mod の版（1.12.0）と Minecraft の版（1.12.2）の数字が似ていて、区別しにくい
  - 括弧の中が Minecraft の版だと書いていない
  - ローダー（Forge / NeoForge）が名前から読み取れない
- ユーザーとの相談で決まったこと:
  - 表示名は「Mod 名 Mod の版 for MC Minecraft の版 (ローダー)」（多くの Mod と同じ「for」の形。`MC` の印で数字を区別する）
  - jar の名前にローダーを入れる（JEI と同じ「Minecraft の版 → ローダー → Mod の版」の並び）
  - Modrinth の名前も同じ表示名に揃える。バージョン番号は変えない
  - 数字の重なりを避けるためだけに Mod の版を 2.0.0 に上げることはしない
  - CurseForge に上げ済みのファイルの表示名は直さない
  - 版は 1.12.2（パッチ）

---

## 0. バージョンと互換性

- バージョンを 1.12.1 → **1.12.2** にする（版を書く 4 か所）
- 変えるのは jar の名前と、配布サイトに上げるときの名前だけ。Mod の中身（遊び方・見た目・通信内容・設定・保存データ）は変わらない。1.12.x 同士はそのまま接続できる
- 版の番号 1.12.2 は、1.12.2 + Forge 版の Minecraft の版と同じ数字になるが、§1 の表示名（`… 1.12.2 for MC 1.12.2 (Forge)`）で区別できる。これが今回の決まりの例

## 1. 配布サイトの表示名

- CurseForge の表示名・Modrinth のバージョンの名前: `Weak Spot Mining <版> for MC <Minecraft の版> (<ローダー>)`
  - 例: `Weak Spot Mining 1.12.2 for MC 1.12.2 (Forge)`・`Weak Spot Mining 1.12.2 for MC 1.21.1 (NeoForge)`
  - ローダーは CurseForge の表記（`Forge` / `NeoForge`。`.github/scripts/mc-versions.sh` の `CF_LOADER`）
- Modrinth のバージョン番号は今のまま `<版>+<Minecraft の版>`（例 `1.12.2+1.12.2`。semver のビルド情報の書き方。重複の確認に使っているので変えない）
- CurseForge に上げ済みのファイル（1.12.1 まで）の表示名は直さない

## 2. jar の名前

- `weakspot-<Minecraft の版>-<ローダー>-<版>.jar`（ローダーは小文字。Modrinth の loaders と同じ `forge` / `neoforge`）
  - 例: `weakspot-1.12.2-forge-1.12.2.jar`・`weakspot-1.21.1-neoforge-1.12.2.jar`
- 1.12.1 までの Release の jar は `weakspot-<Minecraft の版>-<版>.jar`。手動で配布サイトに上げ直す処理（「CurseForge publish」「Modrinth publish」）は、新しい名前が無ければ古い名前の jar を使う（1.11.12〜1.12.1 も上げ直せるように）

## 3. 版の数字の決め方

- Mod の版は、今までどおり `CLAUDE.md` の「バージョン」の規則（パッチ・マイナー）だけで決める。Minecraft の版と数字が重なるかどうかは考えない
- 区別は、§1 の表示名の印（`MC`・ローダー名）と、§2 の jar の名前の並びで付ける
- 2.0.0 に上げるなら、大きな区切り（版の対応をやめる、大きな新しい仕組みを入れるなど）に合わせる。数字の重なりだけを理由にしない

## 4. 置き場所

- `mc1.12.2/build.gradle` の `archivesBaseName`・`mc1.21.1/build.gradle` の `base.archivesName` にローダーを足す
- `.github/scripts/mc-versions.sh`: Release に付いている jar の名前から Minecraft の版を取り出す処理（`release_mc_versions`）を、新旧どちらの名前でも読めるようにする。jar の名前を作る関数を足し、`publish-curseforge.sh`・`publish-modrinth.sh` から使う
- `publish-curseforge.sh`・`publish-modrinth.sh`: §1 の表示名
- `ci.yml` の Release を作る所（`weakspot-*-<版>.jar`）は新しい名前も拾うので、変えない

## 5. README・doc

- `CHANGELOG.md`: 「## 1.12.2」に「配布するファイルの名前を `weakspot-1.12.2-forge-1.12.2.jar` の形に変更（ローダーを名前に追加）。CurseForge・Modrinth の表示名を「Weak Spot Mining 1.12.2 for MC 1.12.2 (Forge)」の形に変更」「遊び方・見た目に変更なし」「通信内容・設定・保存データに変更なし。1.12.x 同士はそのまま接続可能」
- README（日英）の「最近の更新」に 1.12.2 を足し、一番古いもの（1.11.12）を消す
- お知らせ `weakspot.news.1.12.2`: 日本語「配布ファイルの名前にローダー名を追加（遊び方は変更なし）」、英語「Release files now include the loader name (no gameplay changes)」
- `update.json`: `promos` の 4 つ（`latest`・`recommended` とも）と、`"1.12.2"`・`"1.21.1"` の両方の節に 1 行
- `doc/development.md`: 「配布サイト」に「版の表記」の節（§1〜§3）。ビルドの成果物の名前と、CurseForge の表示名の記述を直す
- `doc/install.md`: 表の jar の名前
- `SPEC_v1.12.0.md`: jar の名前の所に「（注: 1.12.2 から `weakspot-1.21.1-neoforge-<版>.jar`）」を足す（当時の決定は書き換えない）
- 直さない文書: `doc/play.md`・`doc/config.md`・`doc/architecture.md`・ガイドの本（遊び方・設定・仕組みは変わらない）、`doc/store/description.md`（版の番号もファイル名も書いていない）、`doc/roadmap.md`（1.12.0 の移植の相談の記録）。PR の本文に理由を書く

## 6. `CLAUDE.md` に書くこと

- ビルドの成果物の名前を新しい形に
- 「バージョン」に、表記の決まりは `doc/development.md` の「版の表記」にあること
- 現行の版を 1.12.2 に

## 7. ユーザーに確認してもらうこと

- 添付の `weakspot-1.12.2-forge-1.12.2.jar`・`weakspot-1.21.1-neoforge-1.12.2.jar` が、今までどおり動くこと（ゲームの中身は変わらない）
- Merge のあと、CurseForge と Modrinth に新しい表示名（`Weak Spot Mining 1.12.2 for MC 1.12.2 (Forge)` など）で上がること
- 前の版の jar を `mods/` から消してから入れること（名前が変わるので、上書きにならない）
