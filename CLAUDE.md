# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

Fortnite の「弱点（クリティカル）」採掘を Minecraft に持ち込む Mod。対象は **Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860**。
仕様書は `doc/spec/` にある（**一覧と版ごとの 1 行の要約は `doc/spec/README.md`**。新しい版の仕様書を作ったら、そこの表の一番上に 1 行足す）。仕様の正本は `doc/spec/SPEC_v1.0.md`（MVP）と、版ごとの差分の仕様書。仕様書に書かれていないことは、その前の版と現行実装のまま。仕様と食い違う実装が必要な場合は、リリースの流れの「止まる条件」に従い、push せずにユーザーに確認する。

**文書の置き場所**（1.8.6 のあとに整理した）: `README.md` は入門だけ（ダウンロード・対応する版・操作・種類の一覧・最近の更新 3 件・文書へのリンク）。英語の入口は `README.en.md`（1.9.0 のあと。ダウンロード・対応する版・操作・種類の一覧だけ。更新履歴は載せない。ほかの文書は日本語のみ）。対応する版は「1.12.2 + Forge（開発・動作確認は 14.23.5.2860）」と書く（Mod は Forge の版を指定していない。ユーザーの判断）。詳しい遊び方は `doc/play.md`、仕組み（開発者向け）は `doc/architecture.md`、導入（導入先と更新時の注意だけ。Forge や Mod の入れ方の手順は書かない）は `doc/install.md`、設定の一覧と管理コマンドは `doc/config.md`、開発は `doc/development.md`、更新履歴は `CHANGELOG.md`、文書の案内は `doc/README.md`。遊び方の説明（`doc/play.md`）には版の注記（「（1.7.0）」など）を書かない（更新履歴に書く）。 README・`CHANGELOG.md`・`doc/` の文書の文章は、ひらがなに開きすぎず漢字を適度に使う（ユーザーの指示。例「当てると速くなる」→「当てると各動作が加速」、「変わっていない」→「変更なし」、更新履歴は体言止めでよい）。

## 開発環境・コマンド

- devcontainer で **JDK 8** を使う。ビルドは公式 MDK ベースの **ForgeGradle 3 + Gradle 4.9**（仕様書の FG 2.3 ではない）。Gradle 5 以降の構文は使えない（依存は `compile` / `testCompile`）。
- ビルド + テスト: `./gradlew build` → 成果物は `build/libs/weakspot-<version>.jar`（reobf 済み）
- テストのみ: `./gradlew test`、1クラスだけ: `./gradlew test --tests com.example.weakspot.common.WeakSpotPlacerTest`
- 専用サーバー起動: `./gradlew runServer`（作業ディレクトリは `run/`、`nogui` 付き。`run/eula.txt` は同意済み）。止めるときはコンソールで `stop`。
  - パイプで `stop` を流しても Gradle 経由では届かない。Claude が起動を確かめるときは `timeout 150 ./gradlew runServer > ログ` で起動し、ログの `Done (` と `run/config/weakspot.cfg` を確認する。
  - devcontainer が使えない環境（クラウドのセッションなど。`runServer` が FML の `NetworkRegistry.newChannel` の NPE で落ちる）では、`runServer` での確認はしなくてよい（ユーザーの指示）。ユーザーが jar をダウンロードして試す。止まる条件の「runServer が起動しない」にも当たらない。ビルドとテストは通すこと。
  - 起動ログの `module-info.class ... IllegalArgumentException`（`Unable to read a class file correctly`）は FG3 + 1.12 でいつも出るノイズで、無視してよい。`Missing English translation for weakspot: .../build/classes/java/main/assets/...` の WARN も、開発環境のリソースの置き場所によるいつものノイズ。
- クライアント確認: コンテナ内では画面を出せない。ビルドした jar をホスト側 Minecraft（Forge 1.12.2）の `mods` に入れて確認する。描画・ヒット判定・体感速度は Claude が検証できないので、ユーザーに確認を依頼する。
- Minecraft の非公開のフィールドは、アクセストランスフォーマーではなくリフレクションで読む。開発環境は MCP 名、実際の環境（reobf 後）は SRG 名なので、`getDeclaredField` で MCP 名 → SRG 名の順に試す（例: `client/MiningProgress`）。Forge の3引数の `ReflectionHelper.findField` は起動環境の判定で片方の名前しか試さず、非推奨でもあるので使わない。
  - SRG 名の調べ方: `~/.gradle/caches/forge_gradle/maven_downloader/de/oceanlabs/mcp/mcp_snapshot/20171003-1.12/mcp_snapshot-20171003-1.12.zip` の `fields.csv`（`methods.csv`）、または `~/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860/forge-1.12.2-14.23.5.2860-srg.jar` を `javap -p` で見る。
  - 画面を出さずに両方の環境で確かめるには、自分のクラスを小さなプログラムからリフレクションで呼ぶ（インスタンスは `Unsafe.allocateInstance`）。開発環境は `sourceSets.main.runtimeClasspath`（Gradle の init スクリプトで書き出す）、実際の環境はその中の `build/classes` `build/resources` と `..._mapped_snapshot_...` の jar を、`build/libs` の reobf 済み jar と上の `-srg.jar` に差し替えたクラスパスで動かす。
- Mod のバージョンは `build.gradle` の `version` と `WeakSpotMod.VERSION` の2か所にある。変えるときは両方を揃える。
- バージョンの方針（1.1.0 以降）:
  - 機能の追加・不具合の修正ごとに**パッチ**を上げる（1.1.0 → 1.1.1）。
  - 互換性を破るときは**マイナー**を上げる（1.1.x → 1.2.0）。迷ったらマイナー。互換性を破る変更とは、通信内容の変更（パケットの追加・削除・中身の変更）、古い版で読めなくなるサーバー保存データの形式変更、設定キーの削除や意味の変更。通信内容を変えたら必ずマイナーを上げる。
  - `@Mod` の `acceptableRemoteVersions` で、同じマイナー同士（例: `[1.1,1.2)`）なら接続できるようにする。マイナーを上げるときは、`build.gradle` と `WeakSpotMod.VERSION` に加えて、この範囲も新しいマイナーに書き換え、`CHANGELOG.md`（と README の「最近の更新」）に旧マイナーとは接続できないことを書く。
  - 現行は 1.9.0。範囲は `WeakSpotMod.ACCEPTED_VERSIONS = "[1.9,1.10)"`（Maven のバージョン範囲の書式。Forge の `VersionRange`）。
- タグと GitHub Release は、`main` に取り込まれたあとに GitHub Actions（`.github/workflows/ci.yml` の `release`）が作る。Claude はタグを付けない（クラウドのセッションはタグを push できない）。
- **遊び方（プレイヤーから見える動き。`doc/play.md`）を変えたときは、ガイドの本の文章（`en_us.lang` と `ja_jp.lang` の `weakspot.guide.<ページ>.title` / `.<小見出し>`。1.8.5）も合わせて直す**（ユーザーの指示）。技術的なこと（設定の名前・値、通信、バージョン）は書かない。本の 1 ページは 14 行・幅 116 ピクセル（日本語で 1 行 12 字くらい、英語で 20 字くらい）で、題・小見出し・「↩ 目次」を含めてはみ出さないこと。ページを足すときは `GuideBook.CONTENT` に足す（目次は `CONTENTS_PAGES` の章から自動で作る。目次の 1 ページも 14 行まで）。
- リリースの流れ: `CHANGELOG.md` の一番上に新しい版の節を足し、README と `README.en.md` の**ダウンロードのリンク（jar の直リンクの版 `releases/download/vX.Y.Z/weakspot-X.Y.Z.jar` と文字の「最新版 X.Y.Z」/「latest: X.Y.Z」）**、README の「最近の更新」（新しい 3 件。一番古いものを消す）を直し、`doc/spec/README.md` の表に 1 行足し（まだなら）、遊び方が変わったら `doc/play.md` も直し（種類・操作が変わったら `README.en.md` の表も）、更新のお知らせの要約 `weakspot.news.<版>` を `ja_jp.lang` と `en_us.lang` に 1 行足す（日本語で 40 字くらいまで。1.7.1）→ コミット → 作業用ブランチに push → `main` への PR を作る（下の「ブランチと PR の約束」）。ユーザーが Merge すると、Actions がタグ `vX.Y.Z` と GitHub Release（本文は `CHANGELOG.md` のその版の節、jar を添付）を作る。
  - コミットの形: 仕様書を足す「Add the spec for X.Y.Z」→ 機能のコミット（1つ以上）→ バージョン・README・CHANGELOG.md・doc・CLAUDE.md をまとめた「Release X.Y.Z: 〜」。リリースした jar は `build/release/` にも残す（ユーザーが試す版を取り出しやすくするため）。
  - 仕様書（`doc/spec/SPEC_*.md`）にもとづく作業は、ユーザーの承認を待たずに、実装から push と PR まで進める。ただし、止まる条件（互換性を破る変更が必要、仕様の意図が読み取れない、ビルドやテストが通らない、runServer が起動しない）に当たったら、push せずに止まって報告する。Merge はユーザーが行う。
  - マイナーを上げるときは、仕様書に書かれた互換性の変更（通信内容、`SyncedSettings`・`StatsMessage` の項目の追加など）は、止まる条件の「互換性を破る変更が必要」に当たらない。`ACCEPTED_VERSIONS` を新しいマイナーに書き換える。仕様書に書かれていない互換性の変更（特に、古い版で作ったワールドの保存データが読めなくなる変更。項目の追加で古いデータを読める形なら、よい）は、当たる。

## アーキテクチャ

**仕組みの詳細（各機能の中身・通信・サーバーの各クラス・設定の移行など）は `doc/architecture.md`**（1.8.8 のあとに `CLAUDE.md` から移した）。コードに触る前に、関係する所を読むこと。仕組みを変えたら、そちらを直す（この節は約束事だけ）。

- クライアントとサーバーの**両方に Mod が必要**（同じマイナー同士なら接続できる）。パッケージは `com.example.weakspot`。
- 弱点の種類は `common/HitKind`（通信は番号なので、足すときは末尾に。`key()` が設定・翻訳キーの小文字の名前）。
- `common/` は Minecraft に依存しない純粋な計算だけ（1.7.10 への移植を見込んで、MC クラスを持ち込まない）。単体テストはここ（と `compat/`・`config/` の golden テスト）にある。
- `client/` は `@EventBusSubscriber(value = Side.CLIENT)`（専用サーバーではロードされない）。パケットのハンドラーは専用サーバーでもインスタンス化されるので、クライアント行きのパケットは `proxy.onXxx` 経由でクライアントのクラスに触る。
- ヒット判定は `RenderWorldLastEvent` で**毎フレーム**行う（tick 単位だと素早い照準移動を取りこぼす）。
- **パッチで、サーバーとクライアントの両方が要る機能を足すときは、クライアント側を `ServerFeatures.since("1.x.y")` で囲み、古いサーバーでは出さない**。
- **HUD に図形を描くときは `HudSpot.beginOverlay` / `endOverlay` を使う**（カリングを切らないと、塗りが消える）。
- 画面の文字列は `assets/weakspot/lang/en_us.lang` と `ja_jp.lang` の両方に足す。
- 共通の部品（`HitGate`・`HitGate.accept`・`HitHandlers`・`AimSpotKind` / `AimSpots`・`ScreenSpotKind` / `ScreenSpots`・`UseTimeCut`・`SpeedModifier`・`QueryThrottle`・`ComboFactor`・`SyncedSettings.enabled` / `minHitInterval` / `server()`・`ScreenProjection.drawMarker`・`Reflect.lazyField`・`ServerThread` など）があるものは、それを使う。一覧は `doc/architecture.md` の「共通の部品」。
- **サーバー側の採掘のブーストは時間枠ではない**（1.12.2 のサーバーは破壊完了の瞬間に「今の速さ × (経過 tick + 1) ≥ 0.7」で判定するため、追加進捗を貯めて速さに換算する。`BoostMath`）。破壊速度まわりを変えるときは、クライアント（積算）とサーバー（瞬間判定）の結果が一致するかを確かめる。詳細は `doc/architecture.md`。
- **弱点の種類を足すときに直す所**: `HitKind`（末尾に）、`server/HitHandlers`（と、その種類の `*Hits`。前置きは `HitGate`）、照準のまわりなら `AimSpotKind` を継いだクラスと `AimSpots.KINDS`、画面の上のマーカーなら `ScreenSpotKind` を継いだクラスと `ScreenSpots.KINDS`、統計（`MiningStats` の配列は自動。保存のキーは `saveKey` の規則。送る並びに入るので通信が変わる → マイナー）、`SyncedSettings` のフィールドと `WIRE`・`WeakSpotConfig`（同じ名前。オン・オフは `<key>WeakSpotEnabled`、間隔は `<key>MinHitIntervalTicks` にすると、種類の表 `SyncedSettings.enabled` / `minHitInterval` に自動で入る）、プレイヤーごとの記憶を持つなら `HitGate.forgetAll`、`KindMask`・「弱点マーカー」タブ と、その `HitKind` の初期の色・コンボの掛け数を使うか（`HitKind` の引数）、lang（統計・設定の説明・節目）、`README.md` の種類の一覧・`doc/play.md`・`doc/config.md`・`doc/architecture.md`、ガイドの本（`GuideBook.CONTENT`）。

### 設定の約束事

- `@Config`（`config/weakspot.cfg`）。**キー名を変えないように、カテゴリは分けず `general` に並べる**。コメントの先頭に、どちらの値が使われるか（`[サーバー]` / `[クライアント]`）を書く。
- `[サーバー]` の項目でクライアントが使うものは `SyncedSettings` に入れて送り、クライアントは接続中 `ClientSettings.get()` を読む。**受け取った値を `WeakSpotConfig` の static フィールドに書き込まない**（`ConfigManager.sync` でクライアントの `weakspot.cfg` に保存されてしまう）。サーバーだけが使う項目は送らない。**`SyncedSettings` に項目を足すと通信内容が変わる（マイナー）**。
- 設定を足すときは `doc/config.md` の表にも足す。設定画面の説明は `weakspot.general.<キーを小文字にしたもの>.tooltip` を `en_us.lang` と `ja_jp.lang` の両方に足す。
- 設定ファイルの移行は `WeakSpotConfig.migrate`（`configVersion`。`serverStarting` で呼ぶ。`preInit` の間は保存できない）。
- パケットの中身を変えたり、パケットを追加・削除したり、統計の保存形式（NBT のキー）を古い版で読めないように変えたりしたら、マイナーを上げ、`acceptableRemoteVersions` を書き換え、`CHANGELOG.md` にそのことを書く。

## ユーザーとの進め方

- ユーザーの要望は短い。「相談」「ラフ」と付いていれば、実装せずに相談から始める。相談では、案を表に並べ、おすすめを 1 つ示し、「決めてほしいこと」を番号付きで聞く。決まったら仕様書 `doc/spec/SPEC_vX.Y.Z.md` を書く（形は既存の仕様書と同じ: 0. バージョンと制約 / 1.〜 内容 / README・doc / CLAUDE.md に書くこと / ユーザーに確認してもらうこと）。
- 下書きの仕様書は、ユーザーが「実装」と言うまで実装しない。
- 色を提案するときは、必ずカラーコードを付ける（例 `#55CCFF`）。
- **速さの上限は基本的に付けない（ユーザーの方針）**。サーバーに引き戻されるなど上限が要りそうなときは、黙って付けずにユーザーに聞く。
- ユーザーは vi に慣れていない。conf などを直してもらうときは、vi の使い方を簡単に添える（`/キー名` で検索 → `n` で次へ、`cw` で単語を書き換え → `Esc`、`:wq` で保存して終わる、`:q!` で保存せずに終わる）。
- クラウドでのビルド: JDK 8 がなければ `apt-get install -y openjdk-8-jdk-headless` で入れ、`JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH ./gradlew build -q` で動かす（Maven Central が 429 を返したら、少し待って再実行する）。
- **ブランチと PR の約束**（1.9.0 のあとに決めた）:
  - 長く残すブランチは `main` だけ。セッションごとの作業は `claude/…` のブランチで行う。
  - `main` に渡せる状態になったら（主にリリースのあと。文書だけの変更でも）、Claude が作業用ブランチから `main` への PR を作る（ユーザーの指示。頼まれるたびではなく、この約束で作ってよい）。本文は、変えたこと・試してほしいこと。
  - CI（`.github/workflows/ci.yml` の `build`。まっさらな環境で `./gradlew build`）が PR で走る。`main` のルールで CI の成功が必須なので、赤いままでは Merge できない。**自分が作った PR の CI が赤くなったら、原因を直して push する**（Claude のコンテナで通っても、まっさらな環境で落ちることがある）。
  - ユーザーが GitHub で「Create a merge commit」で取り込む。取り込んだブランチは自動で消える（設定「Automatically delete head branches」）。取り込まれたあとの作業は、同じ名前のブランチを `main` から作り直し、新しい PR にする（取り込み済みの PR は使い回さない）。
  - `main` への直接の push は、ルールで禁止（例外はユーザーだけ）。Claude は `main` に push しない。
  - Dependabot（`.github/dependabot.yml`）が、ワークフローの部品の更新 PR を週に 1 回作る（更新があった週だけ）。CI が緑なら、ユーザーが Merge してよい。赤なら Claude が原因を調べる。
  - 自動で消えなかったブランチを消すとき: 消してよいのは、中身がすべて `main` に入っているブランチだけ（`git merge-base --is-ancestor origin/<ブランチ> origin/main`）。クラウドの取得は履歴が浅いので、先に `git fetch --unshallow` をする（しないと判定を誤る）。`main` にないコミットがあるブランチは、消さずにユーザーに聞く。作業中のセッションのブランチは残す。消すのはユーザーで、Claude は候補の一覧と削除のコマンドを渡す。タグはブランチと別なので、ブランチを消しても残る。
- リリースしたら、jar を `build/release/` にコピーしてユーザーに添付する（Merge の前に試せるように）。PR のリンクと、「試してほしいこと」の箇条書きを渡す。

## 次の作業

`doc/spec/SPEC_v1.9.1.md`（不具合報告の文章の整理・古い Java での OS 名の注記・issue の雛形）の下書き。ユーザーが「実装」と言うまで実装しない。

### TODO（ユーザーの判断: いつかやる。やるときに相談する）

- **Modrinth / CurseForge への自動公開**: Release を作るとき（`.github/workflows/ci.yml` の `release`）に、jar を配布サイトにも上げる。配布サイトのアカウントと、トークンをリポジトリの Secrets に登録してもらう必要がある。公開するかどうか、どちらのサイトにするかを先に決める。

### 1.10.0 に向けたメモ（ユーザーの判断: 1.8.8 のあとの相談で、いったん見送り。入れる目星だけ付けた）

- **パッケージ名の改名**（`com.example.weakspot` → 例 `io.github.zeusisgood.weakspot`）: 通信・保存データはクラス名を使っていないので互換は保てるが、全ファイルに触る。ほかの整理と重ならない時期（1.9.0 が落ち着いたあと、または 1.7.10 への移植を始める前）に、単独の版で行う。
- **設定のカテゴリ分け**（138 項目が `general` に並んでいて、設定画面で探しにくい）: カテゴリを分けると設定キー（`general.xxx`）が変わるので、設定ファイルの移行（`configVersion` 7。古いキーの値を新しいカテゴリへ移して消す）と一緒に、マイナーで行う。「キー名を変えない」方針を、この版だけ移行つきで破ることになるので、行うときに改めて相談する。
