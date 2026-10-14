# 弱点破壊 Mod 追加仕様書（v1.11.8）

`SPEC_v1.11.7.md`（Mod 1.11.7）に対する**パッチ**の仕様。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.11.8.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.11.7 がリリース済み
- 内容は、コード・文書の全体のレビュー（削れる所の洗い出し）の指摘のうち、ユーザーが「おすすめで」と決めたもの
- **PR を 2 つに分ける**（ユーザーの判断）
  - PR 1「文書の整理」: §3〜§5。版は上げない。この仕様書と `doc/spec/README.md` の 1 行もこちらに入れる
  - PR 2「1.11.8」: §1。PR 1 が `main` に取り込まれたあと、`main` から切ったブランチで作る
- 遊び方・見た目・通信内容・設定・保存データは変えない

---

## 0. バージョンと互換性

- PR 2 で、バージョンを 1.11.7 → **1.11.8** にする（`build.gradle` の `version` と `WeakSpotMod.VERSION`）
- §1 は内部の整理だけ。通信内容・設定・保存データ・翻訳キーは変わらない。`ServerFeatures.since` も要らない
- 1.11.x 同士はそのまま接続できる

## 1. コードの整理（PR 2）

### 1.1 `MiningProgress.find` を `Reflect.field` に置き換える

- `client/MiningProgress` の `find`（MCP 名 → SRG 名の順に試し、なければ警告）は `Reflect.field` と同じ処理
- `DAMAGE` / `BLOCK` を `Reflect.field(PlayerControllerMP.class, "the block health bar", "curBlockDamageMP", "field_78770_f")`（`BLOCK` は `"currentBlock", "field_178895_c"`）にし、`find` と、使わなくなる import（`LogManager` など）を消す
- 警告の文面は `Reflect` の形（`Cannot find field PlayerControllerMP.…; the block health bar is disabled`）に変わる。見つかったときの動作は同じ
- 約 17 行減

### 1.2 画面の弱点の置き場所を選ぶ処理を `ScreenSpotKind` にまとめる

- `client/SleepSpot.place` と `client/EnchantSpot.place` は、どちらも「範囲の中のランダムな点を N 回試し、前の位置から `minMove` 以上離れた点があればそれ、なければ一番離れた点」
- `ScreenSpotKind` に、次の形のメソッドを 1 つ置き、両方から呼ぶ（名前は実装で決める）
  - 引数: 範囲（`minX`・`maxX`・`minY`・`maxY`）、試す回数、`minMove`、前の位置、除外する点の条件（なければ `null`）
  - 結果: 置けたら `x` / `y` を書き換えて true。除外されずに残った点が 1 つもなければ、`x` / `y` を変えずに false
- 今の違いはそのまま残す
  - 睡眠: 40 回、範囲は `SleepSpotArea.area`、`minMove` は `SleepSpotArea.minMove`、除外なし。いつも true を返す（除外がないので、1 回試せば必ず置ける）
  - エンチャント: 80 回、範囲は画面の端から `EDGE`、`minMove` は画面の高さの 1/4、枠（`PANEL_*` と `PANEL_GAP`）の中は除外。範囲が空なら試さずに false、除外で全部外れたら false
- 乱数の引き方の順（x のあと y）を変えない
- 約 12 行減

### 1.3 `GrowthProgress.column` と `COLUMN_MAX_AGE` を消す

- 本体のコードから使われていない（呼んでいるのは `GrowthProgressTest` だけ）
- テストの `column` の部分もあわせて消す
- 約 12 行減

### 1.4 `SyncedSettings.ENABLED` の「項目がなければいつでもオン」をやめる

- 今は 19 種類すべてに `<key>WeakSpotEnabled` がある。`INTERVAL` と同じく、項目がなければ `IllegalStateException`（`"no " + 名前`）にする
- `enabled(kind)` の `field == null ||` を消す
- 表のコメントの「（ない種類 = 採掘・成長・機械・動物は、いつでもオン）」を消す（1.9.0 から全種類にある）
- 新しい種類を足して項目を忘れたら、起動時（クラスの読み込み時）に気づける
- 約 4 行減

### 1.5 `WeakSpotConfig.configuration()` と `cachedConfiguration()` を 1 つにする

- `cachedConfiguration()` の中身を `configuration()` に移し、`reloadFromFile` からも `configuration()` を呼ぶ
- コメントは 2 つを合わせて 1 つにする（設定画面を組むためと、ファイルを読み直すため。`ConfigManager` の非公開の `CONFIGS` から取り出す。取り出せなければ null）
- 約 4 行減

### 1.6 引数なしの `Reflect.method` を消す

- 引数の型を省く版を使っているのは `server/VillagerTrades` の 1 か所だけ
- そこを `Reflect.method(EntityVillager.class, "unlocking new trade tiers", new Class<?>[0], "populateBuyingList", "func_175554_cu")` にし、省く版を消す
- 約 4 行減

## 2. 見送るもの（ユーザーの判断）

- `server/QueryHandlers` を switch にする: `HitHandlers` と同じ「種類 → 処理の表」の形で、`doc/architecture.md` もその形で説明している。揃っていたほうが、移植のときに読みやすい
- 手動用のワークフロー（`.github/workflows/modrinth-publish.yml` / `curseforge-publish.yml`）を 1 本にまとめる: 失敗したときに使う非常用で、Actions の画面で名前から選べたほうが間違えにくい。CurseForge だけ「二重に上がる」の注意書きがある
- `CLAUDE.md` の JDK 8 を入れる手順と 429 の再実行を `doc/development.md` へのリンクにする: クラウドのセッションで毎回使うので、開く文書を増やさない
- `TimedBoostMath.multiplier` と `MachineComboBoost.multiplier` の統合（レビューでも統合しないとした）: 上限が 0 以下のときの扱いが違う（1.11.7 の §4 と同じ）

## 3. `doc/architecture.md` の版の経緯を削る（PR 1）

今の仕組みの説明と、理由が要る注記だけを残す。経緯は `CHANGELOG.md` と仕様書に記録がある。

- **消す**
  - 冒頭の「**1.9.0 で変えたこと**」の段落（中身は下の各項目に書いてある。下の項目に古い名前が残っていたら、その項目を直す）
  - 冒頭の「（`CLAUDE.md` から 1.8.8 の時点で移し、以後の版で直している）」
  - 「全体」の、1.0 から 1.11 までの接続できない版を並べた鎖。「同じマイナー同士なら接続できる（`ACCEPTED_VERSIONS`、今は `[1.11,1.12)`）。マイナーが違うと接続できない」の形にする
  - 機能に付いた「（1.x.y）」だけの括弧
  - 「1.x までは…だった」「1.x で…に変えた」という、前の形の説明（例: `QueryMessage` の「1.8.x までは種類ごとに 6 個だった」）
- **残す**
  - `ServerFeatures.since` / `PlayerText.clientSince` で古いクライアントと分けている版（今は 1.11.2・1.11.3）。どの版から送る・使うかが分からないと、コードが読めないため
  - 理由が要る所の注記（issue #38 の見えない壁、採掘のブーストをクリックのたびに捨てる理由、ヒットの間隔を `HitBudget` にした理由など）。版の数字は消してよいが、理由の文は残す
  - 設定の移行（`configVersion` の番号と、各番号で何を移したか）。古い設定ファイルを読むときに要るため
  - 通信のメッセージの番号
- 迷ったら残す。意味が変わる書き換えはしない（版の括弧を消すだけ・前の形の説明を消すだけにとどめる）
- `CLAUDE.md` の「アーキテクチャの約束事」にある版の注記（「1.11.6。」など）は、この仕様書の対象外（変えない）

## 4. ほかの文書の重複を削る（PR 1）

- `doc/roadmap.md`
  - 「リリース済み」の節を消す。1.11.0 の行の「経緯: 的当ての得は、経験値・コンボをつなぐ・短い効果などを比べてミニゲームにし、記録は点数の案からヒット数にした（ユーザーの判断）」は、「1.11.x（パッチ）: 移植の前の仕上げ」の節の頭に移す
  - 冒頭の「（1.9.6 のあとに `CLAUDE.md` の「次の作業」から移した）」を消す
- `doc/diagrams/README.md`
  - 「コードと既存の文書の食い違い」の節（表 10 件と、コードのコメントの古い記述 2 件）を消す。どれも直してある
  - 「要確認」の節を消す。4 件とも「直した」か「ユーザーの判断で今のまま」で片付いている。今のままにした挙動（的当て中に死んで時間が尽きたら記録する、など）は、図の注記に書いてあるかを確かめ、なければ図の注記に移す
  - 冒頭の「1.11.2 の時点のコードから描いた」を「コードから描いた」にする。「決まり」の「下の一覧に書く」を「図をコードに合わせ、文書のほうを直す」にする
- `doc/development.md`
  - パッケージの表を消し、「パッケージごとの中身は [architecture.md](architecture.md) の「全体」」の 1 行にする（表は古い）
  - 「JDK 8 がない環境」の行の「（Maven Central が 429 を返したら、少し待って再実行）」を消す（すぐ下の 429 の行と重なる）
  - 「（1.9.6 のあとに `CLAUDE.md` から移した。）」と、「リリースの手順」の「（1.9.6 のあとに `CLAUDE.md` から移した。…）」の経緯の部分を消す（「直し忘れの多くは `ReleaseFilesTest` で CI が赤くなる」は残す）。ほかにも同じ出どころの注記があれば消す
  - 「`Reflect`、`client/MiningProgress`」を「`Reflect`」にする（PR 2 の §1.1 で `MiningProgress` が `Reflect` を使うようになる。PR 1 の時点では `MiningProgress` はまだ手書きだが、約束事としては `Reflect` を指すのが正しいので先に直す）
- `CLAUDE.md` の「次の作業」を、次の形に縮める（リリース済みの版の並びは `CHANGELOG.md` と同じなので消す）
  - 「現行は 1.11.7。機能は止め、1.11.x のパッチを経て別の Minecraft の版への移植へ。候補と保留中の相談は `doc/roadmap.md`。」

## 5. 文書の誤りの修正（PR 1）

- `doc/spec/README.md` の表で、1.9.4 の行が 1.9.6 の行の上にある。新しい版が上なので、1.9.6 → 1.9.5 → 1.9.4 の順に直す（ほかの行の順も確かめる）
- （調べた結果、直さないもの）`README.en.md` の近接の「強いクリティカル」と、`doc/store/description.md` の「剣のなぎ払いも当たる」は食い違いではない。`doc/play.md` に両方書いてあり、description.md が詳しく書いているだけ

## 6. README・doc

- PR 1: §3〜§5 のとおり。`doc/spec/README.md` の表の一番上に 1.11.8 の行を足す（PR 1 の時点ではまだリリースしていないが、仕様書を足したら 1 行足す決まりなので先に入れる）
- PR 2: リリースの手順どおり（`CHANGELOG.md`・README の「最近の更新」・`weakspot.news.1.11.8`・`update.json`）。更新履歴は「内部の整理（使われていない部品の削除・重なった処理のまとめ）」とだけ書く。お知らせ（`weakspot.news.1.11.8`）は「内部の整理（遊び方は変更なし）」
- PR 2 では、遊び方が変わらないので、ガイドの本・`doc/play.md`・`doc/config.md`・`README.en.md`・説明文は直さない
- PR 2 の `doc/architecture.md`: 「共通の部品」の `ScreenSpots` / `ScreenSpotKind` の説明に、§1.2 のメソッド（置き場所の選び方）を足す。`Reflect` の説明に `MiningProgress` が書いてあれば直す。`GrowthProgress.column`・`cachedConfiguration`・引数なしの `Reflect.method` が書いてあれば消す
- `doc/diagrams/`: §1 の変更は図に出てこない（クラスの依存も呼ぶ順も変わらない）ので直さない。PR 2 の本文に理由を書く

## 7. `CLAUDE.md` に書くこと

- PR 1: §4 の「次の作業」の縮め
- PR 2: 現行の版を 1.11.8 に（「バージョン」の節と「次の作業」の 1 行）

## 8. ユーザーに確認してもらうこと

- PR 1: 文書だけ。`doc/architecture.md` で、残してほしい経緯が消えていないかを、差分で見てもらう
- PR 2: 遊んで分かる違いはない。CI（テスト）が通れば十分。念のため:
  - 採掘で、ブロックの耐久のバー（掘っている間の進み具合）が今どおり出る
  - ベッドで寝ている間の弱点と、エンチャント画面の弱点が、今どおりの範囲に出て、当てると次の位置に移る（エンチャントの枠の上には出ない）
  - 村人の取引上限のリセットで、設定 `villagerResetUnlocksNewTier` をオンにしたとき、今どおり新しい取引の段階も増える
  - 設定画面が今どおり開き、`/weakspot reload`（設定の読み直し）が今どおり効く
