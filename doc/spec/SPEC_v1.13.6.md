# 弱点破壊 Mod 追加仕様書（v1.13.6）

`SPEC_v1.13.5.md`（Mod 1.13.5）に対する**パッチ**の仕様（1.13.3 の点検で判断を保留した「文書・テスト・CI」6 件の決定と、使っていない版の仕組みの整理）。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.13.6.md`。

- 対象: Minecraft 1.12.2 / Forge・1.20.1 / Forge・1.21.1 / NeoForge の 3 つ（決まりどおり同じ PR で）
- 前提: 1.13.5 が `main` に取り込み済み
- 経緯: Issue #65「1.13.3 点検のフォローアップ」の「文書・テスト・CI」の 6 件を相談して決めた。あわせて、マイナーを上げてまで整理するものがあるかを洗い出した。仕様書の段階で PR（Draft）を作り、レビューで直した
- 決めたこと:
  - 版の知らせ方は、今の形（サーバーが確かめ、本人と管理者に知らせる）を残し、使っていない仕組みだけを消す（§4）。本人への知らせをクライアントに移す案、3 つの版とも `HelloMessage` に揃える案はとらない
    - 先駆者: ローダー（Forge 1.12.2 の `acceptableRemoteVersions`、Forge 1.20.1 の `SimpleChannel` の版、NeoForge の `versioned`）は、通信の版が合わなければ接続を断る。その下の細かい版の違いは、サーバーのプラグイン（ViaVersionStatus など）がサーバーで確かめて、本人と管理者に知らせる形が多い
    - Forge 1.20.1・NeoForge の接続時のやり取りには Mod の版が入らないので、1.20.1・1.21.1 は自前の `HelloMessage` が要る。1.12.2 は Forge が Mod の一覧で版を送るので、通信を足さない
  - マイナー（1.14.0）にはしない。洗い出した結果、通信の中身と設定の項目に、古い版のためだけのものはなかった（`SyncedSettings` は 1.12.2 で 95 項目・1.20.1・1.21.1 で 96 項目、メッセージは 1.12.2 で 15 種類・1.20.1・1.21.1 で 16 種類。どれも使っている）
  - 古いワールド・古い設定ファイルを読む処理は、3 つの版とも残す（1.12.2 の `critHits`・`rewardHits`・`WeakSpotConfig.migrate`、1.20.1・1.21.1 の `critHits`）
    - 先駆者: Minecraft 本体（DataFixerUpper）は古いワールドを直す仕組みをずっと持つ。Mod の多くは、大きな版の変わり目で古い形への対応をやめ、更新履歴に書く
    - 1.20.1 の統計の保存先は 1.12.2 と同じ（`getPersistentData()` の `PlayerPersisted` の下）なので、1.12.2 のワールドを移すと古いキーが残りうる。1.21.1 だけ消すと、3 つの版の `ServerStats.read` の形が揃わなくなる。消せるのは数行なので、残す
  - 1.20.1・1.21.1 の版ごとのテストは、設定の形が 3 つの版で同じかを確かめるものだけを足す（§5）。1.12.2 の `LangFilesTest`・`SettingGroupsTest` を写す案は、`WeakSpotConfig` がローダーに依存して手間に見合わないので、とらない。延期もしない（1.21.1 にだけ設定を足して翻訳を忘れても、今のテストでは見つからない。設定を足すときに直す所は 1 か所増えるが、テストが教えてくれるので、忘れても CI で分かる）

---

## 0. バージョンと互換性

- バージョンを 1.13.5 → **1.13.6**（パッチ）。版を書く 6 か所を揃える
- 通信内容・保存データ・設定の項目は変わらない。1.13.x 同士は接続できる（範囲 `"[1.13,1.14)"`・`PROTOCOL` `"1.13"` のまま）
  - `SyncedSettings.serverVersion` は残す（`/weakspot bug` の環境の欄が使う）。`HelloMessage` も残す（`VersionCheck` が使う）
- 遊び方・見た目は変わらない

## 1. README の BOOTH の URL を揃える（文書）

- 今: 日本語版（`README.md`）は `https://booth.pm/ja/items/5036167`、英語版（`README.en.md`）は `https://zeusisgood.booth.pm/items/5036167`（どちらも同じ商品）
- 変更: 日英とも `https://zeusisgood.booth.pm/items/5036167`（ショップの URL。言語が入らないので、どちらの README でも同じ）

## 2. `MiningStats.reset()` を消す（`common/`）

- 今: `reset()` は的当ての記録（`targetBest`・`targetRounds`）も消すが、本体からは呼ばれていない。本物の「統計をリセット」は、3 つの版の `ServerStats.resetTotal` が新しい統計を作り、的当ての記録だけを写し直している
- 変更: `reset()` を消す。`MiningStatsTest` の次の 5 本を直す
  - `resetClearsEverything`: 消す（`reset()` だけを確かめるテスト）
  - `maxStreakKeepsTheLargestAndResets`・`animalHitsAreCountedSeparatelyAndReset`・`fishingHitsAreCountedSeparatelyAndReset`・`countsEachKindAndTheTotal`: `reset()` を呼んで 0 に戻ることを確かめる部分を消す（残りの確かめはそのまま。名前の「AndReset」「AndResets」も外す）

## 3. 1.12.2 のガイドの本の NBT `weakspotGuide` を書かない（1.12.2）

- 今: 本を作るときに `weakspotGuide`（`GuideBook.TAG_FORMAT`、値は `FORMAT` = 2）を書くが、読む所がない。1.20.1・1.21.1 の本には、この印がない
- 変更: `TAG_FORMAT`・`FORMAT` と、書き込みの 1 行を消す。配ったあとの本に残った印は、そのまま（害はない）

## 4. 使っていない版の仕組みを消す（3 つの版・`common/`）

- 今: 同じマイナーの中で版が混ざっても、新しい機能や翻訳キーを安全に使うための仕組みがあるが、どれも呼ぶ所がない
  - `client/ServerFeatures`（サーバーの版で、クライアントの機能を止める）
  - `server/PlayerText.of`・`understands`・`clientSince`（キーを持たない古いクライアントには、サーバーで文にして送る）
  - `server/ServerLang`（その文を、jar の中の言語ファイルから作る）
  - `common/ModVersions.hasKeysSince`（`clientSince` が使う。呼ぶのはテストだけ）
- 変更:
  - 上の 4 つと、そのテスト（`ModVersionsTest` の `hasKeysSince` の部分）を消す
  - `PlayerText` の残りは `VersionCheck` に移し、`PlayerText` を消す（使うのは `VersionCheck` だけなので、クラスを分けない）
    - 1.12.2: `clientVersion`（Forge が受け取った Mod の一覧から読む）だけを移す
    - 1.20.1・1.21.1: `clientVersion`・`onHello`（`HelloMessage` から呼ぶ）と、覚えた版の表を移す。ログアウトのときに版を忘れる処理は、今ある `VersionCheck.forgetOnLogout` に入れる。`HitGate` の `PlayerText.forgetOnLogout` の行は消す（すぐ上に `VersionCheck.forgetOnLogout` の行がもうあるので、足さない）
  - コメントを直す: `SyncedSettings.serverVersion`（`/weakspot bug` の環境の欄が使う）、`VersionCheck` の「（PlayerText）」、1.20.1・1.21.1 の `HelloMessage` の「PlayerText.clientSince・VersionCheck で使う」（`VersionCheck` だけが使う）
  - `LangFilesTest` の、サーバーから送る文の形（`%s`／`%1$s` だけ）を確かめるテストは残す（根拠は、クライアントの翻訳がこの形しか扱えないことなので、`ServerLang` を消しても変わらない）。コメントの `PlayerText` は直す
- これからは、サーバーとクライアントの両方が要る機能、サーバーから送る文の新しい翻訳キー、メッセージの種類や送り先の追加は、マイナーで行う（§9）

## 5. 設定の形が 3 つの版で同じかを確かめるテストを足す（3 つの版）

- 今: 1.12.2 の `LangFilesTest` は、1.12.2 の設定の一覧から翻訳（名前と説明）を確かめている。3 つの版の設定の名前が同じである限り、これで 3 つの版の分を見ていることになる。ただし、3 つの版の設定の名前が同じかを確かめるテストはない（1.21.1 にだけ設定を足して翻訳を忘れても、見つからない）
  - 今は 3 つの版とも名前が同じ（初期値は版で違うものがある）。項目の数（`ServerConfig` で 119 前後、`ClientConfig` で 40 前後）は、実装のときに正本を作って確かめる
- 変更:
  - 設定の一覧の正本 `common/settings.txt` を置く。1 行に 1 項目で `ServerConfig.Mining.boostMultiplier` の形（`ServerConfig`・`ClientConfig` の入れ子のクラスの名前と、フィールドの名前）。並びは決めない（読みやすさのため、書いた順に並べる）
  - 3 つの版に、同じ中身のテスト `config/SettingNamesTest` を 1 本ずつ足す（1.20.1・1.21.1 には `src/test/java` を作る。どちらも今は `common/` のテストしか流していない）。自分の版の `ServerConfig`・`ClientConfig` の入れ子のクラスの public なフィールドを読み、`common/settings.txt` と、集合として一致するかを確かめる（`getDeclaredFields()` の並びは Java の仕様で決まっていないので、並びは比べない）。足りない項目と余分な項目を、失敗のメッセージに出す
  - 1.20.1・1.21.1 の `ServerConfig`・`ClientConfig`・`Config`（注釈）は、Minecraft もローダーも import していないので、Minecraft なしで流せる。`WeakSpotConfig` は読み込まない
  - 翻訳の確かめは、今の 1.12.2 の `LangFilesTest` のまま（3 つの版が正本と一致すれば、3 つの版の分を見ていることになる）
- 設定を足す・消すときは、`common/settings.txt` も直す（§9）
- 実装のときに確かめること: 1.20.1（ForgeGradle 6）・1.21.1（ModDevGradle）の `src/test/java` が、今の `test.java.srcDir '../common/src/test/java'` と一緒にコンパイルされて流れること

## 6. CI とビルドの不揃いを直す

- `shellcheck`: 版ごとの 3 つのジョブがそれぞれ流している。`ci.yml` に小さなジョブ `scripts`（`actions/checkout` と `shellcheck -x .github/scripts/*.sh` だけ）を足し、版ごとのビルドと並べて 1 回だけ流す。版ごとのジョブからは消す
  - まとめ役の `build` ジョブは、`needs: [build-mc, scripts]` にし、判定の行を `test "${{ needs.build-mc.result }}" = success && test "${{ needs.scripts.result }}" = success` にする（両方が成功したときだけ成功する。`needs` に足すだけでは、`scripts` が赤でも `build` は緑のまま）。Merge に必須のチェックは `build` の名前のままなので、ブランチの保護の設定は変えなくてよい
- `org.gradle.caching=true`: 1.21.1 だけにある。1.20.1 の `gradle.properties` にも入れる。1.12.2（ForgeGradle 3 + Gradle 4.9）は壊れやすいので入れない
- 1.20.1 の `options.compilerArgs << '-Xmaxerrs' << '2000'`（移植のときの名残）を消す

## 7. （取りやめ）古いワールドの近接のキーを読む処理

- 下書きでは、1.20.1・1.21.1 の `ServerStats.read` から `critHits` を読む処理を消すとしていたが、取りやめた（理由は冒頭の「決めたこと」）。3 つの版とも今のまま

## 8. README・doc

- `CHANGELOG.md`: 「## 1.13.6」に §1〜§6 の要約と、遊び方・通信内容に変更がないこと
- README（日英）: §1 の BOOTH の URL。「最近の更新」に 1.13.6。お知らせ `weakspot.news.1.13.6`: 日本語「内部の整理（遊び方に変更はありません）」、英語「Internal cleanup (no gameplay changes)」
- `doc/spec/README.md`: 表の一番上の 1.13.6 の行から「（下書き）」を外す
- `update.json` は「リリースの手順」どおり
- `doc/architecture.md`:
  - 版の仕組みの説明（`ServerFeatures`・`PlayerText.of`・`ServerLang` の行）を、§4 の形（`VersionCheck` が版を覚えて確かめる。新しい機能・翻訳キーはマイナーで足す）に直す
  - 通信の表の `HelloMessage` の行と、「クライアントを信じる範囲」の `PlayerText.clientVersion` を、`VersionCheck` に直す
  - テストの説明に、§5 の `SettingNamesTest` と `common/settings.txt` を足す
  - 設定を足すときに直す所に、`common/settings.txt` を足す
- `doc/development.md`: 「リリースの手順」の 1.・9.・10. を、§9 の仕様書の段階から PR を作るやり方に直す（9. のコミットの形は「Add the draft spec for X.Y.Z」→ レビューの直し（0 個以上）→ 機能のコミット（1 つ以上）→「Release X.Y.Z: 〜」）。CI の説明があれば §6 に合わせる
- 直さない文書: `doc/play.md`・ガイドの本（遊び方は変わらない）・`doc/config.md`（設定の項目は変わらない）・`doc/store/description.md`・`doc/install.md`

## 9. `CLAUDE.md` に書くこと

- 「バージョン」の現行と「次の作業」を 1.13.6 にする（範囲は変えない）
- 版の仕組みの決まりを、§4 に合わせて書き換える:
  - 消す: 「バージョン」の「今あるメッセージを、形も意味も同じまま送り先だけ増やすのはパッチ（新しい版の相手にだけ送る）」
  - 消す: 「アーキテクチャの約束事」の「パッチで、サーバーとクライアントの両方が要る機能を足すときは、クライアント側を `ServerFeatures.since` で囲む。サーバー側は `PlayerText.clientSince` で確かめる」。相手の版を知る方法（1.12.2 は Forge の Mod の一覧、1.20.1・1.21.1 は `HelloMessage`）の説明は、`VersionCheck` のためとして残す
  - 「サーバーからプレイヤーに送る文は翻訳キーで送る」の行は、`PlayerText.of`・`ServerLang`・「マイナーを上げたら `PlayerText.of` を直す」の部分だけを消す。翻訳キーで送ること（`TextComponentTranslation`／`Component.translatable`）、翻訳の値は `%s`／`%1$s` の形だけであること、`LangFilesTest` が確かめる対象（`weakspot.version.*`・`weakspot.breed.*`・`weakspot.milestone.broadcast.*`・`weakspot.welcome.*`・`weakspot.target.updateGift` の 5 つ。今の CLAUDE.md は 3 つしか書いていないので直す）は残す
  - 「コードのコメント」の行の「ただし `ServerFeatures.since` などで動作に関わる版は書く」を消す
  - 足す（「バージョン」）: 「サーバーとクライアントの両方が要る機能、サーバーから送る文の新しい翻訳キー、メッセージの種類や送り先の追加は、マイナーで行う（同じマイナー同士しか接続しないので、版を確かめる仕組みは持たない）」
- 「アーキテクチャの約束事」の `resources/` のテストの説明に、§5 の `SettingNamesTest`（3 つの版）を足す
- 「設定の約束事」の、設定を足すときに直す所に `common/settings.txt` を足す
- 「ユーザーとの進め方」に、仕様書の段階から PR を作るやり方を書く（この 1.13.6 から使う）。今の 1 行目（「相談」「ラフ」が付いていれば相談から始め、決まったら仕様書を書く）と 2 行目（下書きの仕様書は「実装」まで実装しない）は、下の形に置き換える。「ブランチと PR」の「`main` に渡せる状態になったら（リリースのあと。文書だけの変更でも）、Claude が `main` への PR を作ってよい」は、「仕様書の Draft の PR は、仕様書を push した時点で作る。それ以外の PR は `main` に渡せる状態になったら作る」に直す:
  - 「相談」のときは、案を表に並べ、おすすめを 1 つ示し、「決めてほしいこと」を番号付きで聞く（今のまま）
  - 「ラフ」のときは、理解のまとめだけで、ファイルは作らない
  - 「仕様書を書いて」と言われたら、`main` から切ったブランチに仕様書を push し、`main` への PR を **Draft** で作る。題は「Spec X.Y.Z: 〜」、本文は仕様書の要約と「決めてほしいこと」
  - レビューは PR の差分とコメント（またはユーザーが渡すレビューのファイル）で行い、直しは同じブランチに push する。PR のコメントは、ユーザーに「見て」と言われたときに読む（見張らない方針のまま）
  - 「実装」と言われたら、同じブランチに実装を積み、題を「Release X.Y.Z: 〜」、本文を今の形（変えたこと・試してほしいこと・直さなかった文書）に書き換えて、Draft を外す。取り込みは 1 回
  - 下書きの間は、仕様書の先頭に「下書き」の印を、`doc/spec/README.md` の行に「（下書き）」を付け、実装のときに外す

## 10. ユーザーに確認してもらうこと

- 3 つの版: 遊び方・見た目が 1.13.5 と同じであること（設定画面・統計画面・ガイドの本を開いて、いつもどおりに出ること）
- 3 つの版（マルチプレイ）: サーバーとクライアントの版が違うとき（例: クライアントが 1.13.5 のまま）、今までどおり「版が違う」お知らせが本人と管理者に出ること
- 1.12.2: 新しく受け取ったガイドの本が、今までどおり開けて読めること
- GitHub: PR の CI で、`scripts` ジョブが 1 回だけ流れ、3 つの版のビルドとテストが通ること（3 つの版で `SettingNamesTest` が流れていること）
