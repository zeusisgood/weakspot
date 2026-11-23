# 弱点破壊 Mod 追加仕様書（v1.13.6）

> **下書き**（ユーザーが「実装」と言うまで実装しない）

`SPEC_v1.13.5.md`（Mod 1.13.5）に対する**パッチ**の仕様（1.13.3 の点検で判断を保留した「文書・テスト・CI」6 件の決定と、使っていない版の仕組みの整理）。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.13.6.md`。

- 対象: Minecraft 1.12.2 / Forge・1.20.1 / Forge・1.21.1 / NeoForge の 3 つ（決まりどおり同じ PR で）
- 前提: 1.13.5 が `main` に取り込み済み
- 経緯: Issue #65「1.13.3 点検のフォローアップ」の「文書・テスト・CI」の 6 件を相談して決めた。あわせて、マイナーを上げてまで整理するものがあるかを洗い出した
- 決めたこと:
  - 版の知らせ方は、今の形（サーバーが確かめ、本人と管理者に知らせる）を残し、使っていない仕組みだけを消す（§4）。本人への知らせをクライアントに移す案、3 つの版とも `HelloMessage` に揃える案はとらない
    - 先駆者: ローダー（Forge 1.12.2 の `acceptableRemoteVersions`、Forge 1.20.1 の `SimpleChannel` の版、NeoForge の `versioned`）は、通信の版が合わなければ接続を断る。その下の細かい版の違いは、サーバーのプラグイン（ViaVersionStatus など）がサーバーで確かめて、本人と管理者に知らせる形が多い
    - Forge 1.20.1・NeoForge の接続時のやり取りには Mod の版が入らないので、1.20.1・1.21.1 は自前の `HelloMessage` が要る。1.12.2 は Forge が Mod の一覧で版を送るので、通信を足さない
  - マイナー（1.14.0）にはしない。洗い出した結果、通信の中身（`SyncedSettings` の 96 項目、16 種類のメッセージ）と設定の項目に、古い版のためだけのものはなかった
  - 古いデータを読む処理は、1.20.1・1.21.1 の分（確実に働かない）だけを消し、1.12.2 の分は残す（§7）
    - 先駆者: Minecraft 本体（DataFixerUpper）は古いワールドを直す仕組みをずっと持つ。Mod の多くは、大きな版の変わり目で古い形への対応をやめ、更新履歴に書く。ローダーの設定（`ForgeConfigSpec`・`ModConfigSpec`）は移行を持たず、読めない値をその場で初期値に直す

---

## 0. バージョンと互換性

- バージョンを 1.13.5 → **1.13.6**（パッチ）。版を書く 6 か所を揃える
- 通信内容・保存データ・設定の項目は変わらない。1.13.x 同士は接続できる（範囲 `"[1.13,1.14)"`・`PROTOCOL` `"1.13"` のまま）
  - `SyncedSettings.serverVersion` は残す（`/weakspot bug` の環境の欄が使う）。`HelloMessage` も残す（`VersionCheck` が使う）
- 遊び方・見た目は変わらない

## 1. README の BOOTH の URL を揃える（文書）

- 今: 日本語版は `https://booth.pm/ja/items/5036167`、英語版は `https://zeusisgood.booth.pm/items/5036167`（どちらも同じ商品）
- 変更: 日英とも `https://zeusisgood.booth.pm/items/5036167`（ショップの URL。言語が入らないので、どちらの README でも同じ）

## 2. `MiningStats.reset()` を消す（`common/`）

- 今: `reset()` は的当ての記録（`targetBest`・`targetRounds`）も消すが、呼ぶのはテスト（`MiningStatsTest.resetClearsEverything`）だけ。本物の「統計をリセット」は、3 つの版の `ServerStats.resetTotal` が新しい統計を作り、的当ての記録だけを写し直している
- 変更: `reset()` とテスト `resetClearsEverything` を消す

## 3. 1.12.2 のガイドの本の NBT `weakspotGuide` を書かない（1.12.2）

- 今: 本を作るときに `weakspotGuide`（`GuideBook.TAG_FORMAT`、値は `FORMAT` = 2）を書くが、読む所がない。1.20.1・1.21.1 の本には、この印がない
- 変更: `TAG_FORMAT`・`FORMAT` と、書き込みの 1 行を消す。配ったあとの本に残った印は、そのまま（害はない）

## 4. 使っていない版の仕組みを消す（3 つの版・`common/`）

- 今: 同じマイナーの中で版が混ざっても、新しい機能や翻訳キーを安全に使うための仕組みがあるが、どれも呼ぶ所がない
  - `client/ServerFeatures`（サーバーの版で、クライアントの機能を止める）
  - `server/PlayerText.of`・`clientSince`（キーを持たない古いクライアントには、サーバーで文にして送る）
  - `server/ServerLang`（その文を、jar の中の言語ファイルから作る）
  - `common/ModVersions.hasKeysSince`（`clientSince` が使う）
- 変更:
  - 上の 4 つ（とそのテスト）を消す
  - `PlayerText` の残り（クライアントの版を覚える `clientVersion`・`onHello`・`forgetOnLogout`）は、`server/ClientVersions` に名前を変える。`VersionCheck` は今までどおりこれを使う（1.12.2 は Forge の Mod の一覧から、1.20.1・1.21.1 は `HelloMessage` から）
  - `SyncedSettings.serverVersion` のコメントを、`/weakspot bug` の環境の欄が使う旨に直す
  - `LangFilesTest` の、サーバーから送る文の形（`%s`／`%1$s` だけ）を確かめるテストは残す（コメントの `PlayerText` は直す）
- これからは、サーバーとクライアントの両方が要る機能、サーバーから送る文の新しい翻訳キー、メッセージの種類や送り先の追加は、マイナーで行う（§9）

## 5. 1.20.1・1.21.1 に版ごとのテストを足す

- 今: 1.20.1・1.21.1 で流れるのは `common/` のテストだけ（1.20.1 の `src/test` は空、1.21.1 には `src/test` がない）。翻訳と公開用のファイルの揃い具合は、1.12.2 のテストが 3 つの版の分をまとめて見ている。設定の項目（`WeakSpotConfig`）は版ごとに別のクラスなので、1.20.1・1.21.1 の項目に名前と説明が揃っているかを確かめるテストがない
- 変更: 1.20.1・1.21.1 に、Minecraft を起動しなくても流せるテストを足す（1.12.2 の `SettingGroupsTest` と `LangFilesTest` の設定の部分と同じ中身）
  - 設定の全項目に、日英の名前（`nameKey`）と説明（`tooltipKey`）がある
  - 設定の全項目が `SettingGroups` のまとめに入っていて、まとめに書いた名前がすべて設定にある
  - 説明の決まり（サーバーの項目は「マルチプレイ時、サーバー側設定が優先されます。」で始まる、最後の行がファイルのキー）を守っている
- 1.21.1 には `src/test` を作り、`build.gradle` のテストの設定（作業ディレクトリをリポジトリの直下にするなど）を 1.20.1 と揃える
- 実装のときに確かめること: 1.20.1・1.21.1 の `WeakSpotConfig.settings()` が、Minecraft を起動せずに（ローダーの設定の仕組みを読み込まずに）呼べること。呼べなければ、`ServerConfig`・`ClientConfig` のフィールドを直接読む形にする

## 6. CI とビルドの不揃いを直す

- `shellcheck`: 版ごとの 3 つのジョブがそれぞれ流している。版に依らないので、まとめ役の `build` ジョブで 1 回だけ流す（`ci.yml`）
- `org.gradle.caching=true`: 1.21.1 だけにある。1.20.1 にも入れる。1.12.2（ForgeGradle 3 + Gradle 4.9）は壊れやすいので入れない
- 1.20.1 の `options.compilerArgs << '-Xmaxerrs' << '2000'`（移植のときの名残）を消す

## 7. 1.20.1・1.21.1 の、古いワールドの近接のキーを読む処理を消す

- 今: 3 つの版の `ServerStats.read` は、`meleeHits` がなければ古いキー `critHits`（1.8.x 以前）を読む（`MiningStats.legacySaveKey`）
- 1.20.1・1.21.1 は 1.12.0・1.13.0 からの版で、`critHits` を書いたことがない。保存先も 1.12.2 と別（1.21.1 はデータアタッチメント）なので、1.12.2 のワールドを変換しても持ち込まれない
- 変更: 1.20.1・1.21.1 の `ServerStats.read` から、`critHits` を読む処理を消す
- 1.12.2 の 3 つは残す: `critHits` を読む処理、昔の節目の累計 `rewardHits` の片付け、1.9.x 以前の設定ファイルの移行（`WeakSpotConfig.migrate`）。`MiningStats.legacySaveKey` は 1.12.2 が使うので残す

## 8. README・doc

- `CHANGELOG.md`: 「## 1.13.6」に §1〜§7 の要約と、遊び方・通信内容に変更がないこと
- README（日英）の「最近の更新」に 1.13.6。お知らせ `weakspot.news.1.13.6`: 日本語「内部の整理（遊び方に変更はありません）」、英語「Internal cleanup (no gameplay changes)」
- `doc/spec/README.md`: 表の一番上に 1.13.6（パッチ）
- `update.json` は「リリースの手順」どおり
- `doc/architecture.md`: 版の仕組みの説明（`ServerFeatures`・`PlayerText.of`・`ServerLang` の行）を、§4 の形（`ClientVersions` と `VersionCheck`、新しい機能・翻訳キーはマイナーで足す）に直す。`HelloMessage` の行と「クライアントを信じる範囲」の `PlayerText.clientVersion` を `ClientVersions` に直す。テストの説明に、1.20.1・1.21.1 の版ごとのテスト（§5）を足す
- `doc/development.md`: テストと CI の説明があれば、§5・§6 に合わせる
- 直さない文書: `doc/play.md`・ガイドの本（遊び方は変わらない）・`doc/config.md`（設定の項目は変わらない）・`doc/store/description.md`・`doc/install.md`

## 9. `CLAUDE.md` に書くこと

- 「バージョン」の現行と「次の作業」を 1.13.6 にする（範囲は変えない）
- **（確認待ち）** 版の仕組みの決まりを、§4 に合わせて書き換える:
  - 消す: 「パッチで、サーバーとクライアントの両方が要る機能を足すときは、クライアント側を `ServerFeatures.since` で囲む。サーバー側は `PlayerText.clientSince` で確かめる」
  - 消す: 「パッチで足したキーは `server/PlayerText.of`（キーを持たない古いクライアントにだけ `ServerLang` の文章）」「マイナーを上げたら、`PlayerText.of` の `since` が新しい下限以下になったものを `TextComponentTranslation` に直す」
  - 消す: 「今あるメッセージを、形も意味も同じまま送り先だけ増やすのはパッチ（新しい版の相手にだけ送る）」
  - 足す: 「サーバーとクライアントの両方が要る機能、サーバーから送る文の新しい翻訳キー、メッセージの種類や送り先の追加は、マイナーで行う（同じマイナー同士しか接続しないので、版を確かめる仕組みは持たない）」
  - 相手の版を知る方法の説明（1.12.2 は Forge の Mod の一覧、1.20.1・1.21.1 は `HelloMessage`）は、`VersionCheck` のためとして残す
- 「アーキテクチャの約束事」の `resources/` のテストの説明に、1.20.1・1.21.1 の版ごとのテスト（§5）を足す

## 10. ユーザーに確認してもらうこと

- 3 つの版: 遊び方・見た目が 1.13.5 と同じであること（設定画面・統計画面・ガイドの本を開いて、いつもどおりに出ること）
- 3 つの版（マルチプレイ）: サーバーとクライアントの版が違うとき（例: クライアントが 1.13.5 のまま）、今までどおり「版が違う」お知らせが本人と管理者に出ること
- 1.12.2: 新しく受け取ったガイドの本が、今までどおり開けて読めること
- GitHub: PR の CI で、`shellcheck` が 1 回だけ流れ、3 つの版のビルドとテストが通ること（1.20.1・1.21.1 のテストの数が増えていること）
