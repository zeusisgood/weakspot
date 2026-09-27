# 弱点破壊 Mod 追加仕様書（v1.9.6）

`SPEC_v1.9.5.md`（Mod 1.9.5）に対する**パッチ**の仕様。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.9.6.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.9.5 がリリース済み
- この仕様書の内容は、Mod のバージョン **1.9.6** として、リリースする

---

## 0. バージョンと互換性

- バージョンは 1.9.5 → **1.9.6**（`build.gradle` の `version` と `WeakSpotMod.VERSION`）
- サーバーがプレイヤーに送る文の作り方だけを変える。通信内容（Mod のパケット）・保存データ・設定は変わらない。1.9.x 同士はそのまま接続できる
- 直るのは**サーバーを 1.9.6 以降にしたとき**（文を作るのはサーバーなので、古いサーバーの文はクライアントを上げても変わらない）

## 1. やりたいこと（ユーザーの報告と判断）

- 日本語のクライアント（1.9.5）で 1.9.2 の Forge サーバーに入ったら、版の違いの知らせ（`weakspot.version.serverOlder`）だけが英語で出た
- 今は、サーバーがプレイヤーの言語を内部の項目（`EntityPlayerMP.language`。リフレクション）から読み、サーバーで文にしている（`server/ServerLang`。1.5.1）。読めないと英語になる
- 相談の結果（ユーザーの判断: 案 D）: 文はクライアントに作らせる。サーバーは**翻訳キーと値**（`TextComponentTranslation`）を送り、クライアントが**自分の今の言語**で文にする（Minecraft 本体の仕組み）。プレイヤーが途中で言語を変えても合い、サーバーのソフトにも左右されない

## 2. 送り方

- `ServerLang` で文を作っていた所を、翻訳キーと値で送る形にする
  - 版の違いの知らせ（`VersionCheck`）: `weakspot.version.clientOlder` / `serverOlder` / `op`
  - 村人の繁殖の案内（`VillagerBreedHints`。アクションバー）: `weakspot.breed.line` の中に、理由（`weakspot.breed.<理由>`）を `weakspot.breed.separator` でつないで入れる（翻訳の中に翻訳を入れる）
- **相手が持っていない翻訳キーは、今の方法で送る**: 古いクライアントは新しい翻訳キーを持っていないので、キーのまま表示されてしまう（`ServerLang` を作った理由）
  - 共通の部品 `server/PlayerText`（仮の名前）: `PlayerText.of(player, since, key, args...)` が、相手のクライアントの版（接続のときに Forge が受け取った Mod の一覧。今 `VersionCheck` が読んでいるもの）が `since` 以上なら `TextComponentTranslation`、そうでなければ今の `ServerLang` の文（`TextComponentString`）を返す
  - `since` は、その翻訳キーを足した版。今のキーはどれも 1.9.0 より前からある（版の違いの知らせは 1.5.1、村人の繁殖の案内は 1.8.4）ので、同じマイナー（1.9.x）同士しか接続できない今は、いつも翻訳キーで送ることになる
  - 相手の版が分からない（Mod の一覧が読めない）ときは、今の方法
- 村人の繁殖の案内は、同じ文を続けて送らないように前の文と比べている（`LAST_TEXT`）。サーバーでは文にしなくなるので、**翻訳キーと値を並べた文字列**で比べる
- 値の書き方: 翻訳の値は今どれも `%s` だけなので、`TextComponentTranslation` でそのまま使える。今後も、サーバーから送る文の翻訳には `%s`（と `%1$s` の形）だけを使う（`%d` などはクライアントの翻訳で使えない）
- `ServerLang` は、`PlayerText` の「持っていないキー」のときだけ使う形で残す

## 2b. 不具合報告の案内（ログの文字化け。ユーザーの判断）

- 日本語の Windows の Java 8（ランチャーの Java）は、ログ（`logs/latest.log`）を Shift_JIS で書く。UTF-8 として開くと日本語が化ける（Mod の不具合ではない。Minecraft 本体の文も同じ）
- `doc/play.md` の不具合報告の節と、issue の雛形（`.github/ISSUE_TEMPLATE/bug_report.md`）に、「ログの日本語が化けていたら Shift_JIS で開き直す（VS Code なら右下の文字コード →「エンコード付きで再度開く」→ Japanese (Shift JIS)）。そのまま添付しても大丈夫」と書く

## 2c. Mod の名前の見え方（ユーザーの判断）

- チャットの頭を、日英とも **`[WeakSpot]`** にする（今は日本語 `[弱点]`・英語 `[Weak Spot]`。9 か所ずつ）。コマンド `/weakspot` と同じ綴りで、何の Mod の知らせか分かるように
- キー割り当ての分類（`key.categories.weakspot`）を、日本語は **`弱点破壊（Weak Spot Mining）`** にする（Mods の一覧・配布サイトの名前と結びつける）。英語は今のまま `Weak Spot Mining`
- K キーのメニューの題（`weakspot.stats.title`）とガイドの本の表紙は、今のまま `弱点破壊`（場所が狭いため）
- `resources/LangFilesTest` に、チャットの頭が `[WeakSpot]` で揃っていること（`[弱点]` `[Weak Spot]` が残っていないこと）の確認を足す
- `doc/play.md` などに `[弱点]` と書いた所があれば直す

## 3. テスト

- `common/` に、版の比べ方（`ModVersions` を使い、`since` と相手の版から「翻訳キーで送るか」を決める部分）の単体テスト
- `resources/LangFilesTest` に、サーバーから送る翻訳（`weakspot.version.*`・`weakspot.breed.*`）の値に `%s` / `%n$s` 以外の書式がないことの確認を足す

## 4. README・doc・お知らせ

- `doc/architecture.md`: `ServerLang` の項目を、`PlayerText` と翻訳キーで送る形に書き換える（持っていないキーのときだけ `ServerLang`）
- `CLAUDE.md`: 約束事に「サーバーからプレイヤーに送る文は `PlayerText`（翻訳キーで送る。キーを足した版を `since` に書く）」
- `CHANGELOG.md`・README の「最近の更新」: サーバーから届く案内（版の違い・村人の繁殖）が、プレイヤーの言語で表示されるように（サーバーの更新が必要）。チャットの頭を `[WeakSpot]` に、キー割り当ての分類に英語名。通信内容・保存データ・設定に変更なし
- 更新のお知らせ `weakspot.news.1.9.6`: 「サーバーからの案内がプレイヤーの言語で出るようにした」

## 5. `CLAUDE.md` に書くこと

- 現行を 1.9.6 に
- 上の約束事

## 6. ユーザーに確認してもらうこと

- サーバーを 1.9.6 にして、クライアントを別の版（例 1.9.5）で入ると、版の違いの知らせが日本語で出ること
- 村人に素手で右クリック（しゃがみ）したときの繁殖の案内が、日本語で出ること。ゲームの言語を英語に変えると英語で出ること
- チャットの知らせの頭が `[WeakSpot]` になっていること。キー割り当ての画面の分類が「弱点破壊（Weak Spot Mining）」になっていること
