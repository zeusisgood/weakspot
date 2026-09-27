# 弱点破壊 Mod 追加仕様書（v1.9.3）

`SPEC_v1.9.2.md`（Mod 1.9.2）に対する**パッチ**の仕様。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.9.3.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.9.2 がリリース済み
- この仕様書の内容は、Mod のバージョン **1.9.3** として、リリースする

---

## 0. バージョンと互換性

- バージョンは 1.9.2 → **1.9.3**（`build.gradle` の `version` と `WeakSpotMod.VERSION`）
- パッケージ名だけを変える。Mod の ID（`weakspot`）・通信のチャンネル・パケットの番号と中身・統計の保存（NBT のキー）・設定ファイル（`config/weakspot.cfg` とキー）・翻訳キーは変えない。**1.9.x 同士はそのまま接続できる**
- 実装中に、クラス名が通信・保存データ・設定に入っている所が見つかったら（互換を破る）、push せずに止まって報告する

## 1. やりたいこと（ユーザーの判断）

- `CLAUDE.md` の「1.10.0 に向けたメモ」のパッケージ名の改名を、メモどおり**ほかの整理と重ならない単独の版**として、1.10.0 の前に出す
- 1.10.0 で足す泳ぎの弱点などの新しいファイルを、最初から新しいパッケージで書けるようにする

## 2. パッケージ名

- `com.example.weakspot` → **`io.github.zeusisgood.weakspot`**（下のパッケージ `client` / `common` / `server` / `network` / `config` / `compat` などの並びは変えない）
- `src/main/java` と `src/test/java` のディレクトリを移し、`package` と `import` を書き換える
- 文字列で書いたクラス名も直す: `@Mod` の `guiFactory`、`@SidedProxy` の `clientSide` / `serverSide`、ほかに `"com.example.weakspot` を含む文字列があれば同じく
- `build.gradle` の `group` を `io.github.zeusisgood` にする（`archivesBaseName` と jar の名前 `weakspot-<版>.jar` は変えない）
- テストの指定（`./gradlew test --tests io.github.zeusisgood.weakspot.common.…`）が変わる

## 3. README・doc・お知らせ

- `CLAUDE.md`・`doc/architecture.md`・`doc/development.md` の `com.example.weakspot` を新しい名前に（`CLAUDE.md` の「1.10.0 に向けたメモ」からパッケージ名の改名を消し、済んだことを書く）
- `CHANGELOG.md`・README の「最近の更新」: 内部のパッケージ名を変更。遊び方・通信内容・保存データ・設定に変更なし。1.9.x 同士はそのまま接続可能
- 更新のお知らせ `weakspot.news.1.9.3`: 「内部の名前を整理した（遊び方は変わらない）」／「Internal cleanup (no gameplay changes)」
- `update.json` を 1.9.3 に

## 4. ユーザーに確認してもらうこと

- 1.9.2 で遊んだワールドを 1.9.3 で開いて、統計（K キー）がそのまま残っていること
- `config/weakspot.cfg` の設定がそのまま効いていること（色・形などを変えていれば、そのまま）
- 設定画面（Mod 一覧の Config）が開くこと
- 1.9.2 のサーバーに 1.9.3 のクライアントで（またはその逆で）接続できること
- 1.9.2 の人に、ワールドに入ったとき 1.9.3 の新しい版の通知が出ること
