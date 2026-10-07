# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

Fortnite の「弱点（クリティカル）」採掘を Minecraft に持ち込む Mod。対象は **Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860**。

- 仕様書は `doc/spec/`（一覧と版ごとの 1 行の要約は `doc/spec/README.md`。新しい版の仕様書を作ったら、表の一番上に 1 行足す）。正本は `SPEC_v1.0.md`（MVP）と版ごとの差分。仕様書に書かれていないことは、その前の版と現行実装のまま。
- **この節以外の詳しいことは `doc/` にある**（必要なときに読む）:

| 内容 | 文書 |
|---|---|
| 仕組み（各機能の中身・通信・共通の部品・弱点の種類を足すときに直す所） | `doc/architecture.md` |
| 開発環境の細かいこと（runServer・リフレクションと SRG 名）・**リリースの手順**・配布サイト・画像・ライセンス・ブランチの片付け | `doc/development.md` |
| 今後の予定・保留中の相談（1.11.0・別の版への対応・TODO） | `doc/roadmap.md` |
| 遊び方 / 設定の一覧 / 導入 / 更新履歴 | `doc/play.md` / `doc/config.md` / `doc/install.md` / `CHANGELOG.md` |

- `README.md` は入門だけ（ダウンロード・対応する版・操作・種類の一覧・最近の更新 3 件・文書へのリンク）。`README.en.md` は英語の入口（ダウンロード・対応する版・操作・種類の一覧だけ。ほかの文書は日本語のみ）。対応する版は「1.12.2 + Forge（開発・動作確認は 14.23.5.2860）」と書く。
- `doc/play.md` には版の注記（「（1.7.0）」など）を書かない（更新履歴に書く）。
- README・`CHANGELOG.md`・`doc/` の文章は、ひらがなに開きすぎず漢字を適度に使う（例「当てると速くなる」→「当てると各動作が加速」、「変わっていない」→「変更なし」。更新履歴は体言止めでよい）。

## 開発環境・コマンド

- **JDK 8**、ForgeGradle 3 + Gradle 4.9（Gradle 5 以降の構文は使えない。依存は `compile` / `testCompile`）。
- ビルド + テスト: `./gradlew build` → `build/libs/weakspot-<version>.jar`（reobf 済み）。テストのみ: `./gradlew test`。
- クラウドでは JDK 8 を `apt-get install -y openjdk-8-jdk-headless` で入れ、`JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH ./gradlew build -q`（Maven Central が 429 なら少し待って再実行）。
- `runServer` の確認は devcontainer でだけ行う（クラウドでは FML の NPE で落ちるので、しなくてよい。止まる条件にも当たらない）。やり方とノイズのログは `doc/development.md`。
- クライアントの描画・ヒット判定・体感速度は Claude が検証できない。jar をユーザーのホスト側 Minecraft で試してもらう。
- 非公開のフィールドは、アクセストランスフォーマーではなくリフレクションで、MCP 名 → SRG 名の順に試す（`Reflect`）。3 引数の `ReflectionHelper.findField` は使わない。SRG 名の調べ方と両環境での確かめ方は `doc/development.md`。

## バージョン

- 版は `build.gradle` の `version` と `WeakSpotMod.VERSION` の 2 か所。必ず揃える。
- 機能の追加・不具合の修正ごとに**パッチ**。互換性を破るとき（通信内容の変更、古い版で読めない保存データの形式変更、設定キーの削除・意味の変更）は**マイナー**。迷ったらマイナー。通信内容を変えたら必ずマイナー。
- `@Mod` の `acceptableRemoteVersions` = `WeakSpotMod.ACCEPTED_VERSIONS` で同じマイナー同士を接続可能にする。マイナーを上げたら範囲も書き換え、`CHANGELOG.md`（と README の「最近の更新」）に旧マイナーとは接続できないことを書く。
- 現行は **1.11.3**、範囲は `"[1.11,1.12)"`。
- タグと GitHub Release は、`main` に取り込まれたあとに Actions が作る。Claude はタグを付けない。

## リリース

- **手順のチェックリストは `doc/development.md` の「リリースの手順」**（版・CHANGELOG・README の日英のリンクと「最近の更新」・`doc/spec/README.md`・`weakspot.news.<版>`・`update.json`・説明文・コミットの形・`build/release/`）。直し忘れは `ReleaseFilesTest` で CI が赤くなる。
- 仕様書にもとづく作業は、ユーザーの承認を待たずに実装から push と PR まで進める。ただし**止まる条件**（互換性を破る変更が必要、仕様の意図が読み取れない、ビルドやテストが通らない、runServer が起動しない）に当たったら、push せずに止まって報告する。Merge はユーザーが行う。
- マイナーを上げる仕様書に書かれた互換性の変更（通信内容、`SyncedSettings`・`StatsMessage` の項目の追加など）は、止まる条件に当たらない。仕様書にない互換性の変更（特に古いワールドの保存データが読めなくなる変更。項目の追加で古いデータを読めるならよい）は当たる。
- `doc/store/description.md` を直した PR では、本文と報告に「CurseForge の説明文を貼り替えてください」と書き、新しい全文を添付する。
- **遊び方（`doc/play.md`）を変えたら、ガイドの本の文章**（`en_us.lang` / `ja_jp.lang` の `weakspot.guide.<ページ>.title` / `.<小見出し>`）も直す。技術的なこと（設定の名前・値、通信、バージョン）は書かない。1 ページは 14 行・幅 116 ピクセル（日本語で 1 行 12 字くらい、英語で 20 字くらい。題・小見出し・「↩ 目次」を含む）。ページは `GuideBook.CONTENT` に足す（目次は `CONTENTS_PAGES` の章から自動。目次の 1 ページも 14 行まで）。

## アーキテクチャの約束事

**コードに触る前に `doc/architecture.md` の関係する所を読む。仕組みを変えたら、そちらを直す。**

**コードを変えたら、関係する文書を同じ PR で直す**（`doc/architecture.md`・`doc/diagrams/` の図（Mermaid の中身も）と注記・`doc/play.md`・`doc/config.md`・ガイドの本・`doc/roadmap.md`）。文章の注記だけでなく、図の矢印・状態も変える。直す必要がない文書は、PR の本文に「直さなかった文書と理由」を書く。

- クライアントとサーバーの**両方に Mod が必要**。パッケージは `io.github.zeusisgood.weakspot`。クラス名を文字列で書くのは `@Mod` の `guiFactory` と `@SidedProxy` だけ。
- 弱点の種類は `common/HitKind`（通信は番号なので、足すときは末尾に。`key()` が設定・翻訳キーの小文字の名前）。**種類を足すときに直す所の一覧は `doc/architecture.md`**。的当て（1.11.0）は種類ではなく別の仕組み（`TargetRounds` / `TargetPlay`。種類別の統計・設定・節目に入らない）。
- 成長・収穫・機械・動物の「手が空いている」は、空か、右クリックで何もしないアイテム（`HeldItems`。1.11.0）。**移植のときは、版ごとに右クリックで動くアイテムを調べ直す**。
- `common/` は Minecraft に依存しない純粋な計算だけ（MC クラスを持ち込まない）。単体テストはここと `compat/`・`config/` の golden テスト。`resources/` のテストはファイルの揃い具合: `LangFilesTest`（日英のキーの一致、設定の全項目に説明、全種類に名前、コードの翻訳キーが翻訳にある）と `ReleaseFilesTest`。
- `client/` は `@EventBusSubscriber(value = Side.CLIENT)`。パケットのハンドラーは専用サーバーでもインスタンス化されるので、クライアント行きのパケットは `proxy.onXxx` 経由でクライアントのクラスに触る。
- ヒット判定は `RenderWorldLastEvent` で**毎フレーム**（tick 単位だと素早い照準移動を取りこぼす）。
- **パッチで、サーバーとクライアントの両方が要る機能を足すときは、クライアント側を `ServerFeatures.since("1.x.y")` で囲む**。
- **HUD に図形を描くときは `HudSpot.beginOverlay` / `endOverlay` を使う**（カリングを切らないと塗りが消える）。
- 画面の文字列は `en_us.lang` と `ja_jp.lang` の両方に足す。チャットの頭は日英とも `[WeakSpot]`。
- **サーバーからプレイヤーに送る文は `server/PlayerText.of(player, since, key, args)`**（翻訳キーで送り、キーを持たない古いクライアントにだけ `ServerLang` の文章）。翻訳の値は `%s` / `%1$s` の形だけ（`LangFilesTest` が `weakspot.version.*`・`weakspot.breed.*`・`weakspot.milestone.broadcast.*` を確かめる。新しい種類の文を足したら、テストの対象にも足す）。
- 共通の部品（`HitGate`・`HitHandlers`・`AimSpotKind` / `AimSpots`・`ScreenSpotKind` / `ScreenSpots`・`UseTimeCut`・`SpeedModifier`・`QueryThrottle`・`ComboFactor`・`SyncedSettings.enabled` / `minHitInterval` / `server()`・`ScreenProjection.drawMarker`・`Reflect.lazyField`・`ServerThread` など）があるものは、それを使う。一覧は `doc/architecture.md` の「共通の部品」。
- **採掘のヒットは、掘る前のものは予約にして、掘り始めた瞬間に確定する**（1.11.2。予約の時点でコンボ・統計を数えない。`ServerBoostTracker`）。
- **コンボはサーバーの数が正**。クライアントの数がずれたら、サーバーが手の止まったときに本人宛ての `OtherComboMessage` で直す（1.11.3、`ComboSync`。片道だけで、サーバーはクライアントの数に合わせない）。
- **サーバー側の採掘のブーストは時間枠ではない**（破壊完了の瞬間に「今の速さ × (経過 tick + 1) ≥ 0.7」で判定するため、追加進捗を貯めて速さに換算する。`BoostMath`）。破壊速度まわりを変えるときは、クライアント（積算）とサーバー（瞬間判定）の結果が一致するかを確かめる。

### 設定の約束事

- `@Config`（`config/weakspot.cfg`）。カテゴリは 2 段（1.10.0）: `server`（`ServerConfig`）／`client`（`ClientConfig`）→ 種類・用途の入れ子のクラス。コードからは `WeakSpotConfig.server.mining.boostMultiplier` の形で読む。**項目の名前は変えない**（カテゴリを移すときは `configVersion` を上げて `WeakSpotConfig.migrate` で移す）。新しい種類の項目は、その種類のカテゴリに。名前から探すときは `WeakSpotConfig.setting(名前)`。コメントの先頭に `[サーバー]` / `[クライアント]` を書き、版の注記は書かない。
- `[サーバー]` の項目でクライアントが使うものは `SyncedSettings` に入れて送り、クライアントは接続中 `ClientSettings.get()` を読む。**受け取った値を `WeakSpotConfig` のフィールドに書き込まない**（`ConfigManager.sync` でクライアントの cfg に保存されてしまう）。サーバーだけが使う項目は送らない。**`SyncedSettings` に項目を足すと通信内容が変わる（マイナー）**。
- 設定画面は、ファイルのカテゴリではなく `config/SettingGroups` のまとめ（種類・用途）で出す（1.10.2）。設定を足すときは、`SettingGroups` のまとめ、`doc/config.md` のそのまとめの表、日英の lang の名前 `weakspot.<カテゴリ>.<キーを小文字にしたもの>` と説明 `.tooltip` を足す。説明は、サーバーの項目なら「マルチプレイ時、サーバー側設定が優先されます。」で始め、最後の行にファイルのキー（「ファイル: server.mining.boostMultiplier」）。クライアントの項目は断り書きなし（`LangFilesTest`・`SettingGroupsTest` が確かめる）。カテゴリを足したら、その名前 `weakspot.<カテゴリ>` と `.tooltip` も。
- 設定ファイルの移行は `WeakSpotConfig.migrate`（`configVersion`、今は 7。`init` と `serverStarting` で呼ぶ。値は `Property` に書いてから `ConfigManager.sync` する。`preInit` の間は保存できない）。
- パケットの中身の変更・追加・削除、統計の保存形式（NBT のキー）を古い版で読めなくする変更をしたら、マイナーを上げ、範囲を書き換え、`CHANGELOG.md` に書く。

## ユーザーとの進め方

- ユーザーの要望は短い。「相談」「ラフ」と付いていれば、実装せずに相談から始める。相談では、案を表に並べ、おすすめを 1 つ示し、「決めてほしいこと」を番号付きで聞く。決まったら仕様書 `doc/spec/SPEC_vX.Y.Z.md` を書く（形は既存と同じ: 0. バージョンと制約 / 1.〜 内容 / README・doc / CLAUDE.md に書くこと / ユーザーに確認してもらうこと）。
- 下書きの仕様書は、ユーザーが「実装」と言うまで実装しない。
- 色を提案するときは、必ずカラーコードを付ける（例 `#55CCFF`）。
- **速さの上限は基本的に付けない（ユーザーの方針）**。上限が要りそうなときは、黙って付けずにユーザーに聞く。
- ユーザーは vi に慣れていない。conf などを直してもらうときは、vi の使い方を簡単に添える（`/キー名` で検索 → `n` で次へ、`cw` で単語を書き換え → `Esc`、`:wq` で保存して終わる、`:q!` で保存せずに終わる）。
- **ブランチと PR**:
  - 長く残すブランチは `main` だけ。作業は `claude/…` のブランチで。`main` への直接の push は禁止（Claude は push しない）。
  - `main` に渡せる状態になったら（リリースのあと。文書だけの変更でも）、Claude が `main` への PR を作ってよい。本文は、変えたこと・試してほしいこと。
  - CI（`ci.yml` の `build`）の成功が Merge に必須。**自分が作った PR の CI が赤くなったら、原因を直して push する**。
  - ユーザーが「Create a merge commit」で取り込み、取り込んだブランチは GitHub が自動で消す（想定どおり）。**作業ごとに、始めるときに `main` から新しい名前のブランチ（`claude/…`）を切る**。取り込まれたブランチを同じ名前で作り直したり、取り込み済みの PR を使い回したりしない。
  - Dependabot の更新 PR は、CI が緑ならユーザーが Merge してよい。赤なら Claude が原因を調べる。
  - ブランチを消すのはユーザー。Claude は候補の一覧と削除のコマンドを渡す（判定の仕方は `doc/development.md` の「ブランチの片付け」）。
- リリースしたら、jar を `build/release/` にコピーしてユーザーに添付し、PR のリンクと「試してほしいこと」の箇条書きを渡す。

## 次の作業

1.11.0（的当てとご褒美・落下の弱点・持ち物も素手と同じに・弱点の大きさ・コンボの段階を 300 までに・`/weakspot top`・進捗）をリリースした。1.11.1（ヒット音の音階とプリセット・的当てのしきい値を 25・50・75・100 に・不具合の修正）。1.11.2（掘る前から出る採掘の弱点と、当てて壊したら次を掘るまでの待ちを 0 に・しゃがんで右クリックでランダム tick のブロックを自動で成長の対象に）。1.11.3（クライアントとサーバーのコンボを揃える）。このあとは機能を止め、1.11.x のパッチを経て別の Minecraft の版への移植へ。1.10.2（設定画面のまとめ直し・コンボのゲージ）。候補と保留中の相談は `doc/roadmap.md`。
