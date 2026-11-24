# 弱点破壊 Mod 追加仕様書（v1.13.9）

> **下書き**（ユーザーが「実装」と言うまで実装しない）

`SPEC_v1.13.8.md`（Mod 1.13.8）に対する**パッチ**の仕様（外部の点検の残り 3 件。1.20.1・1.21.1 の同じファイルの共通化、新しい 2 版のテスト、統計の保存の共通化）。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.13.9.md`。

- 対象: Minecraft 1.12.2 / Forge・1.20.1 / Forge・1.21.1 / NeoForge の 3 つ（決まりどおり同じ PR で）
- 前提: 1.13.8 が `main` に取り込み済み（この仕様書のブランチは、取り込まれたあとに `main` を取り込んでから実装する）
- 経緯: 9481b06 の点検の 7 件のうち、1.13.8 で行わなかった 3 件。順番は §1 → §2 → §3（§1 で共通の置き場所を作ると、§2 のテストを 1 か所に置ける）
- 決めたこと:
  - 共通の置き場所の名前は `mojmap/`（1.20.1・1.21.1 は、どちらも Mojang の名前でコードを書くため）
  - 遊び方・通信内容・保存データの形は変えない。中身の整理だけ

---

## 0. バージョンと互換性

- バージョンを 1.13.8 → **1.13.9**（パッチ）。版を書く 6 か所を揃える
- 通信内容・保存データ（NBT のキーと型）・設定の項目は変わらない。1.13.x 同士は接続できる（範囲 `"[1.13,1.14)"`・`PROTOCOL` `"1.13"` のまま）。1.13.8 までのワールドの統計は、そのまま読める
- 遊び方は変わらない

## 1. 1.20.1・1.21.1 で同じファイルを `mojmap/` にまとめる

### 1.1 今

- 1.20.1 と 1.21.1 で、中身が 1 文字も違わない `.java` が 53 本ある（`diff -rqs` で確かめた。`client/` 31・`server/` 12・直下 6・`config/` 3・`network/` 1）。どちらの版も同じ変更を手で 2 回入れていて、片方だけ直し忘れても見つける仕組みがない
- テストの `config/SettingNamesTest` も、2 版で同じ中身

### 1.2 変更

- リポジトリの直下に `mojmap/src/main/java` と `mojmap/src/test/java` を作る。パッケージは今のまま（`io.github.zeusisgood.weakspot.…`）
- 移すもの: 上の 53 本と `SettingNamesTest`（`git mv` で 1.20.1 から移し、1.21.1 の同じファイルを消す）。実装のときにもう一度 `diff -rqs` を取り、その時点で同じファイルを全部移す（数が変わっていたら PR の本文に書く）
- 移さないもの: 1 文字でも違うファイル、`src/main/resources` の全部（AT・`mods.toml`／`neoforge.mods.toml`・アイコンなど）
- 読み込み: 1.20.1・1.21.1 の `build.gradle` の `sourceSets` に、`common` の 2 行の下へ次の 2 行を足す（Gradle のビルドは今のまま別々）
  - `main.java.srcDir '../mojmap/src/main/java'`
  - `test.java.srcDir '../mojmap/src/test/java'`
- `mojmap/` の約束（`doc/architecture.md` と `CLAUDE.md` に書く）:
  - Minecraft のクラスを使ってよい（`common/` との違い）。ただし、1.20.1（Forge・JDK 17）と 1.21.1（NeoForge・JDK 21）の**どちらでもそのままビルドが通る**ものだけを置く。Java の書き方は 17 まで
  - 版ごとに違いが要るようになったら、そのファイルを `mojmap/` から両方の版のディレクトリに戻して、それぞれ直す（`mojmap/` の中で版を分ける仕組みは作らない）
  - 名前で探すリフレクションを置くときは、SRG 名と Mojang の名前の両方を見る（1.20.1 は動いているとき SRG 名のため）
  - 機能を足す・直すときは、まず `mojmap/` にあるかを見る（あれば 1 回の変更で 2 版に効く）
- 1.12.2 は今のまま（MCP の名前で、API も違う）
- テスト: `LangFilesTest.keysWrittenInTheCodeExist` の見る所に `mojmap/src/main/java` を足す（1.13.8 で足した 2 版のディレクトリはそのまま）。`ReleaseFilesTest` などがファイルの場所を決め打ちしていれば、`mojmap/` も見るように直す
- CI（`ci.yml`）は、パスで絞っていないので変えない（各版のビルドが `mojmap/` を読む）

## 2. 新しい 2 版に、種類の登録忘れのテストを置く

### 2.1 今

- `HitHandlers.has` と `QueryHandlers.handles` は「テスト用」だが、呼ぶテストは 1.12.2 にしかない。1.20.1・1.21.1 のテストは `SettingNamesTest` だけ
- 新しい `HitKind` を足して、新しい版だけ `HitHandlers` への登録を忘れると、その種類のヒットは黙って捨てられ、CI は緑のまま。`SyncedSettings` の項目の書き忘れは、起動したときの static の初期化で初めて分かる

### 2.2 変更

- 1.12.2 の 3 本のテストを、新しい 2 版にも置く。置き場所は、テストするクラスが `mojmap/` にあるか（§1 の後）で決める:

| テスト | 中身 | 置き場所 |
|---|---|---|
| `compat/HitHandlersTest` | 全部の `HitKind` に `HitHandlers` の登録がある | `mojmap/src/test/java`（`HitHandlers` は 2 版で同じ） |
| `compat/QueryMessagesTest` の登録の部分 | 問い合わせる種類（動物・釣り・機械）に `QueryHandlers` の登録がある | `mojmap/src/test/java`（`QueryHandlers` は 2 版で同じ） |
| `compat/QueryMessagesTest` の往復の部分 | `QueryMessage`・`StateMessage` を書いて読むと同じ中身に戻る | 各版の `src/test/java`（通信の部品が版で違う） |
| `config/SyncedSettingsTest` | 送る一覧（`WIRE`）と項目が一致する・全部の項目に設定がある・全部の種類に間隔がある | 各版の `src/test/java`（`SyncedSettings`・`WeakSpotConfig` が版で違う） |

- 書き方は各版の API に合わせる（1.12.2 の MCP の名前・`IMessage` を、Mojang の名前・各版の通信の部品にする）。確かめる中身とテストの名前は 1.12.2 と同じにする
- テストの中で Minecraft の初期化（レジストリなど）が要って落ちるときは、確かめる中身を変えずに、初期化の要らない形にする（例: 登録の一覧だけを読む）。それでも置けないテストは置かず、PR の本文に理由を書く
- 確かめ方: 実装のときに、`HitHandlers` の登録を 1 つ消すと 2 版の CI（`./gradlew test`）が赤くなることを一度試す（コミットしない）

## 3. 統計の保存を `common/MiningStats` の 1 か所にまとめる

### 3.1 今

- 統計を NBT に書いて読む処理（`server/ServerStats.read` / `write`）が 3 つの版に手書きである。項目のキー・型・古いキー `critHits` の読み替えが 3 か所にある
- `MiningStats` に項目を足して、ある版の `write` だけ書き忘れると、その版では値がメモリーでは増えても、再ログインで 0 に戻る（保存データが消える）。どのテストも見ていない。通信の形（`writeTo` / `readFrom`）は `common/` に 1 か所あるのに、保存の形だけ 3 か所に分かれている

### 3.2 変更

- `common/MiningStats` に、保存先を表す小さなインターフェースと、2 つのメソッドを足す（Java 8 の書き方）:
  - `interface Store { boolean has(String key); long getLong(String key); double getDouble(String key); void putLong(String key, long value); void putDouble(String key, double value); }`
  - `void save(Store out)`: 今の `write` と同じキー・同じ型で全部の項目を書く（`savedTicks` だけ double）
  - `static MiningStats load(Store in)`: 今の `read` と同じ。近接は、新しいキー（`meleeHits`）がなく古いキー（`critHits`）があれば古いキーを読む（`legacySaveKey` の扱いをここへ移す）。ないキーは 0
- 各版の `ServerStats.read` / `write` は、NBT（1.12.2 は `NBTTagCompound`、1.20.1・1.21.1 は `CompoundTag`）を `Store` に包んで `load` / `save` を呼ぶだけにする（数行）。統計の置き場所（1.12.2・1.20.1 は持ち越されるプレイヤーのデータ、1.21.1 は死亡で消えないデータの添付）は変えない
- キーの名前と型は今と同じ（古いワールドの統計はそのまま読め、1.13.9 で保存した統計は 1.13.8 でも読める）
- `common/` にテストを足す（`Store` は `HashMap` で作る）:
  - 全部の項目（全部の種類のヒット数・`savedTicks`・的当てを含む）に違う値を入れて `save` → `load` すると、同じ値に戻る
  - キーの一覧が今の形と同じ（`blocksBroken`・`hits`・`meleeHits`・`savedTicks` など。キーを変えると古いワールドが読めなくなるので、決め打ちで確かめる）
  - `critHits` だけがあるデータを読むと、近接の数になる。`meleeHits` もあれば `meleeHits` を読む
- 項目を足すときは `MiningStats` の `save` / `load`（と `writeTo` / `readFrom`）だけを直せばよくなる。`doc/architecture.md` の「種類を足すときに直す所」から、各版の `ServerStats` の読み書きを外す

## 4. README・doc

- `CHANGELOG.md`: 「## 1.13.9」に「中身の整理（遊び方は変更なし）」として §1〜§3 の要約と、通信内容・保存データに変更がないこと
- README（日英）の「最近の更新」に 1.13.9（新しい 3 件にする）。お知らせ `weakspot.news.1.13.9`: 日本語「中身を整理しました（遊び方は変わりません）」、英語「Internal cleanup; no gameplay changes」
- `doc/spec/README.md`: 表の一番上の 1.13.9 の行から「（下書き）」を外す
- `update.json` は「リリースの手順」どおり
- `doc/architecture.md`: リポジトリの形に `mojmap/` と §1.2 の約束を足す。「種類を足すときに直す所」のファイルの場所を、`mojmap/` に移ったものは `mojmap/` に直す。「共通の部品」に `MiningStats.save` / `load` を足す（統計の保存の正本）
- `doc/development.md`: 冒頭のリポジトリの形に `mojmap/` を足す
- 直さない文書: `doc/play.md`・ガイドの本（遊び方は変わらない）・`doc/config.md`（設定の項目は変わらない）・`doc/roadmap.md`・`doc/store/description.md`・`doc/install.md`

## 5. `CLAUDE.md` に書くこと

- 「バージョン」の現行と「次の作業」を 1.13.9 にする（範囲は変えない）
- 「開発環境・コマンド」のリポジトリの形に足す: 「`mojmap/`（1.20.1・1.21.1 で同じ中身のコード。両方のビルドがソースで読む。Minecraft のクラスを使ってよいが、両方でそのままビルドが通るものだけ。違いが要るようになったら両方の版に戻す）」
- 「アーキテクチャの約束事」の `common/` の行の近くに 1 行足す: 「統計の保存（NBT のキー）は `common/MiningStats.save` / `load` が正本。各版の `ServerStats` は受け渡すだけ。キーの名前を変えない」

## 6. ユーザーに確認してもらうこと

- 3 つの版: 1.13.8 で遊んだワールドを 1.13.9 で開いて、統計画面の値（累計・種類ごとのヒット数・的当ての記録）がそのまま残っていること。少し遊んでから入り直しても、増えた分が残ること
- 1.20.1・1.21.1: ひととおりの弱点（採掘・成長・機械・動物・近接・弓など）が今までどおり出て効くこと。設定画面・統計画面・音のタブが今までどおり開くこと
