# 弱点破壊 Mod 追加仕様書（v1.11.12）

`SPEC_v1.11.11.md`（Mod 1.11.11）に対する**パッチ**の仕様（別の Minecraft の版への移植の下ごしらえ）。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.11.12.md`。移植そのものは `SPEC_v1.12.0.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.11.11（コメントの整理）がリリース済み。**開いている PR がない時期に作業する**（§2 でファイルを大きく動かすため）
- 考え方（`doc/roadmap.md` の「別の Minecraft の版への対応」・1.11.10 のあとの相談）:
  - リポジトリは 1 つのまま。版ごとに別々の Gradle のビルドを置き、版に依存しない計算（`common`）は**ソースで共有**する
  - 移植のあとも、1.12.2 版と機能を揃え続ける（ユーザーの判断）。同じ機能を 2 回書く量を減らすため、移植の前に、版に依存しない処理をできるだけ `common/` に寄せる
  - 配布は、1 つの版に Minecraft の版ごとの jar を並べる形にする（§5）。1.12.0 で jar が 2 つになる前に、この版で 1 つの jar のまま新しい流れを試しておく
- 遊び方・見た目・通信内容・設定・保存データ・動作は変えない
- PR は 1 つ（「1.11.12」）。コミットは §ごとに分ける

---

## 0. バージョンと互換性

- バージョンを 1.11.10 → **1.11.12** にする（`build.gradle` の `version` と `WeakSpotMod.VERSION`）
- 通信内容・設定・保存データ・翻訳キー（お知らせの 1 行を除く）は変わらない。`ServerFeatures.since` も要らない。1.11.x 同士はそのまま接続できる
- 翻訳の元データの形式（§3）と jar の名前（§5）が変わるが、ゲームに入るファイル（`.lang`）の中身は変わらない

## 1. `common/` を独立したディレクトリに出す

- `src/main/java/io/github/zeusisgood/weakspot/common/` → **`common/src/main/java/io/github/zeusisgood/weakspot/common/`**（`git mv`。パッケージ名は変えない）
- テストも同じく `common/src/test/java/…/common/` へ
- `common/` には Gradle のビルドを置かない。各版のビルドが `../common/src/main/java`（とテストの `../common/src/test/java`）をソースに足す（1.12.2 の Gradle 4.9 と新しい版の Gradle 8 は 1 つのマルチプロジェクトにできないため）
- `common/` の約束（今と同じ）: Minecraft・Forge・NeoForge のクラスを持ち込まない。1.12.2 版を残す限り **Java 8 の書き方**（`var`・レコード・テキストブロック・`switch` 式は使わない）
- `common/` のテストは、各版のビルドで流す（1.12.2 では JDK 8、新しい版ではその JDK で。同じテストが 2 回流れるのは、両方の JDK で通ることの確認になるので、そのままにする）

## 2. 1.12.2 のビルドを `mc1.12.2/` に移す

- `git mv` で次を `mc1.12.2/` へ: `src/`・`build.gradle`・`gradle.properties`・`gradlew`・`gradlew.bat`・`gradle/`（ラッパー）
- 直下に残すもの（全部の版で共通）: `doc/`・`common/`・`lang/`（§3）・`CHANGELOG.md`・`README.md`・`README.en.md`・`LICENSE`・`update.json`・`CLAUDE.md`・`.github/`
- できあがりの形:

```
common/            版に依存しない計算とそのテスト（Java 8 の書き方）
lang/              翻訳の元データ（en_us.json・ja_jp.json）
mc1.12.2/          1.12.2 + Forge のビルド（Gradle 4.9、JDK 8）
doc/ CHANGELOG.md README*.md LICENSE update.json   共通
```

- `mc1.12.2/build.gradle` で直すこと:
  - `sourceSets` に `../common/src/main/java`・`../common/src/test/java` を足す
  - jar に入れる `LICENSE` を `../LICENSE` に
  - jar の名前を **`weakspot-1.12.2-<版>.jar`** にする（§5）。`reobfJar` のあとの名前も同じ
- テストが直下のファイル（`update.json`・`CHANGELOG.md`・`README*.md`・`doc/`・`lang/`）を読む所（`ResourceFiles`）は、リポジトリの直下からの道筋で読む（Gradle の作業ディレクトリが `mc1.12.2/` になるので、`../` を付けるか、テストの `workingDir` を直下にする。どちらかは実装で決める）
- `run/`（runServer の作業場所）・`build/`・`.gradle/` の `.gitignore` を新しい場所に合わせる
- `build/release/`（リリースの jar の置き場所）は `mc1.12.2/build/release/` になる

## 3. 翻訳の元データを `lang/*.json` にする

- 正本を **`lang/en_us.json`・`lang/ja_jp.json`**（キーと値の 1 段の JSON。キーの順は今の `.lang` の行の順のまま）にする。新しい版（1.13 以降）は翻訳に `.json` を使うので、そのまま読める
- 1.12.2 のビルドは、`processResources` でこの JSON から `assets/weakspot/lang/en_us.lang`・`ja_jp.lang` を作る（`キー=値` の 1 行ずつ）。**できる `.lang` の中身は、今のファイルと 1 文字も変わらない**ことを確かめる（並びを含めて。コメント行 2 つは消えてよい）
- `mc1.12.2/src/main/resources/assets/weakspot/lang/*.lang` は消す（手で直す場所を 1 つにする）
- 今の `.lang` の中の `\n`（バックスラッシュと n の 2 文字。Mod のコードが改行に直す）は、JSON では `"\\n"` と書き、`.lang` で同じ 2 文字に戻す
- `.lang` の今のコメント行（2 行）に書いてあることが要るなら、`doc/development.md` に移す
- 翻訳を読むものを全部 JSON に合わせる:
  - `LangFilesTest`・`ReleaseFilesTest`（日英のキーの一致、`weakspot.news.<版>` など）
  - `.github/scripts/changelog.sh`（`weakspot.news.<版>` を `jq` で読む）
  - `CLAUDE.md`・`doc/development.md`・`doc/architecture.md` の `en_us.lang` / `ja_jp.lang` の記述 → `lang/en_us.json` / `lang/ja_jp.json`

## 4. 版に依存しない処理を `common/` に寄せる

- 移植のときに 2 回書かずに済むよう、Minecraft のクラスを使っていない処理を `common/` に移す。**動作は変えない**
- 1.11.11（コメントの整理）で全部のファイルを読んだときに作った「`common/` に寄せられる処理の一覧」（1.11.11 の PR の本文）を出発点にする
- 対象（調べた時点で、`client/`・`server/`・`config/`・`network/` の中で Minecraft・Forge のクラスを `import` していないもの）: `TimedBoost`・`ServerFeatures`・`StatsScreenTab`・`TargetRecords`・`AnimalStates`・`KindSwitches`・`QueryThrottle`・`SettingGroups`・`SoundPreset`・`HitSound`・`ComboPosition`
  - それぞれ、`common/` の外のクラス（`WeakSpotConfig`・`ClientSettings` など）に頼っていないかを確かめ、頼っていなければ移す。頼っていれば、頼らない部分（計算・表）だけを分けて移すか、移さない（**迷ったら移さない**）
- 上のほか、`client/`・`server/` のクラスの中にある、Minecraft のクラスを受け取らない計算（数・文字列・表だけを扱うメソッド）も、見つけた分だけ `common/` の新しいクラスか既存のクラスに移す。量は目安を決めない（この版で全部やりきらなくてよい。移植の途中で見つけたら、そのときに寄せる）
- 移したクラスにテストがなければ、移したついでに足してよい（足さなくてもよい）
- `config/SettingGroups`（設定画面のまとめの並び）は両方の版で同じ並びにするので、特に移したい。設定の値の型（`@Config` の注釈）に頼っている所は、名前の文字列の表にして共有する
- `doc/architecture.md` の地図（クラスの置き場所）を直す

## 5. CI とリリースの流れを、版ごとの jar に合わせる

1.12.0 で jar が 2 つになるので、その形の流れをこの版で先に作る（jar は 1 つのまま）。

- **`ci.yml` の `build`**: Minecraft の版ごとの matrix にする（この版では `mc1.12.2`・JDK 8 の 1 つ）。各版のディレクトリで `./gradlew build`。テストの結果と jar を、版ごとの名前（`jar-1.12.2` など）で上げる
- **`release`**: 全部の版の `build` が通ったときだけ動く。全部の版の jar を集めて、タグ `v<版>` の Release を 1 つ作り、jar を全部付ける。本文は今と同じ（`CHANGELOG.md` のその版の節）
- **`publish-modrinth.sh` / `publish-curseforge.sh`**: 引数を `<版> <Minecraft の版>` にし、版ごとに 1 回ずつ呼ぶ
  - jar は `weakspot-<Minecraft の版>-<版>.jar`
  - Modrinth: バージョンの番号を **`<版>+<Minecraft の版>`**（例 `1.11.12+1.12.2`）、名前も同じにする。`game_versions`・`loaders` を Minecraft の版から決める（この版では `1.12.2`・`forge`）。上げ済みかは、この番号で確かめる
  - CurseForge: 表示名を `Weak Spot Mining <版> (<Minecraft の版>)` にする。版の ID（Minecraft の版・ローダー・Java）を Minecraft の版から決める（この版では今と同じ 1.12.2・Forge・Java 8）
  - Minecraft の版ごとの値（ローダー・Java・CurseForge の版の種類の slug）は、スクリプトの中の 1 か所の表にまとめる
- **手動のワークフロー**（`modrinth-publish.yml` / `curseforge-publish.yml`）: 入力に「Minecraft の版」を足す（空なら全部の版）。CurseForge は二重に上げないよう、失敗した版だけを選んで流せるようにする
- `update.json` は今のまま（`1.12.2` の節だけ）。1.12.0 で `1.21.1` の節を足す
- 今までの Modrinth のバージョン（番号が `1.11.10` の形）はそのまま残す（直さない）

## 6. `doc/roadmap.md` の整理（この仕様書の PR で行う）

- 1.11.0 のあとの「優先度低めのメモ」のうち、次の 3 つを「見送ったもの」に移す（ユーザーの判断。公開サーバーで使われる予定がないため。話が出たら両方の版で作る）
  - 的当てのサーバー設定 `targetPracticeEnabled`
  - 的当ての記録を消す管理コマンド `/weakspot resettarget <プレイヤー>`
  - 的当ての不正対策（案 A）
- 的当ての数字（大きさ・距離・速さ・ご褒美のしきい値）は今の値で決める（ユーザーの判断）
- 「別の Minecraft の版への対応」の節を、決まったこと（1.21.1 + NeoForge・1.12.0・揃え続ける・配布の形）に書き換え、この仕様書と `SPEC_v1.12.0.md` を指す

## 7. README・doc

- `CHANGELOG.md`: 「## 1.11.12」に「内部の整理（別の Minecraft の版への対応の下ごしらえ）。遊び方・見た目に変更なし」「通信内容・設定・保存データに変更なし。1.11.x 同士はそのまま接続可能」「配布するファイルの名前を `weakspot-1.12.2-1.11.12.jar` の形に変更」
- README（日英）の「最近の更新」に 1.11.12 を足し、一番古いものを消す
- お知らせ `weakspot.news.1.11.12`: 日本語「内部の整理（遊び方は変更なし）」、英語「Internal cleanup (no gameplay changes)」
- `update.json` の `promos` と `"1.12.2"` の 1 行
- `doc/development.md`: ビルドのコマンド（`cd mc1.12.2 && ./gradlew build`）・JDK 8 の手順・`common/` だけのテストの流し方・runServer の場所・リリースの手順（翻訳は `lang/*.json`、jar の名前、`build/release/` の場所）・配布サイト（版ごとに上げる、Modrinth の番号の形）
- `doc/architecture.md`: ディレクトリの形と、`common/` に移したクラス
- `doc/install.md`: jar の名前が変わったことに触れている所があれば直す
- 直さない文書: `doc/play.md`・`doc/config.md`・ガイドの本・`doc/store/description.md`（遊び方・設定が変わらないため）。PR の本文に理由を書く

## 8. `CLAUDE.md` に書くこと

- 「開発環境・コマンド」: ビルドは `mc1.12.2/` の中で（`cd mc1.12.2 && JAVA_HOME=… ./gradlew build -q`）、jar は `mc1.12.2/build/libs/weakspot-1.12.2-<版>.jar`
- 「アーキテクチャの約束事」: `common/` の置き場所（直下の `common/`。Java 8 の書き方）。翻訳の正本は `lang/*.json`（「画面の文字列は `en_us.lang` と `ja_jp.lang` の両方に」→ `lang/en_us.json` と `lang/ja_jp.json`）
- 版の 2 か所の道筋（`mc1.12.2/build.gradle`）
- 現行の版を 1.11.12 に

## 9. ユーザーに確認してもらうこと

- 動作は変わらない。CI（ビルドとテスト）が通れば十分
- 1.11.12 の jar（名前が `weakspot-1.12.2-1.11.12.jar`）で、ゲームの文字（日英）が今までどおり出ること（翻訳を JSON から作るように変えたため。設定画面・ガイドの本・チャットを少し見てもらう）
- Merge のあと、GitHub の Release・Modrinth・CurseForge に新しい名前で上がること（Modrinth の番号は `1.11.12+1.12.2`）
