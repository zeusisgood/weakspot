# 開発

[← README に戻る](../README.md)

開発・動作確認には Minecraft 1.12.2 / **Forge 14.23.5.2860**（公式 MDK、ForgeGradle 3 + Gradle 4.9）を使っています。Mod 側では Forge の版を指定していないため、1.12.2 用の Forge であれば読み込まれますが、古いビルドでの動作は確認していません。

JDK 8 が必要です。リポジトリの devcontainer を使うと、JDK 8 と Claude Code が入った環境で開発できます。

```sh
./gradlew build        # ビルドとテスト。成果物は build/libs/
./gradlew test         # 単体テストのみ
./gradlew runServer    # 開発用の専用サーバーを起動（run/ で起動。初回は run/eula.txt の同意が必要）
```

コンテナ内ではゲーム画面を表示できないため、クライアント側の動作は、ビルドした jar をホスト側の Minecraft の `mods` に入れて確認します。

ソースは `io.github.zeusisgood.weakspot` 以下にあります。

| パッケージ | 内容 |
|---|---|
| `common/` | Minecraft に依存しない計算（動物のタイマー換算、釣りの待ち時間と許容角度、照準周辺の弱点の向き、面の座標変換、弱点の配置、ブースト量、連続ヒット数、ヒット音の音階、コンボ表示、マーカーの移動と残像、耐久バーの形状、統計の集計、節目、耐久回復の精算、機械の加速量、マーク送信頻度、成長の進行度、マークの形状、弱点のオン／オフ）。単体テストの対象 |
| `client/` | 弱点の状態管理、ヒット判定、描画（他プレイヤーのマーク・耐久バーを含む）、ヒット音、コンボ表示、統計画面（「弱点マーカー」タブを含む）、節目の演出 |
| `network/` | パケット（ヒット通知、設定、統計、節目、弱点マーク、弱点のオン／オフ、動物・釣り・機械の状態の問い合わせと応答） |
| `server/` | 管理コマンド、破壊速度ブースト、各種類のヒットの検証と効果、機械の加速、統計の記録、報酬、設定の送信、弱点マークの転送 |
| `config/` | 設定 |

仕様書（バージョンごとの差分）の一覧は [spec/README.md](spec/README.md)。各機能の仕組みの詳細は [architecture.md](architecture.md)、開発の約束事は [CLAUDE.md](../CLAUDE.md) を参照してください。

## ブランチとリリース

- 作業は `main` 以外のブランチで行い、`main` への PR を作ります。PR ごとに GitHub Actions（[`.github/workflows/ci.yml`](../.github/workflows/ci.yml)）がビルドと全テストを実行し、成功しないと Merge できません。
- テストには、翻訳の抜け（`resources/LangFilesTest`）と、版を上げたときに直すファイルの揃い具合（`resources/ReleaseFilesTest`）の確認も含まれます。配布サイトへ上げるスクリプトは、CI で `shellcheck` にかけます。
- ワークフローで使う部品（`actions/checkout` など）は、Dependabot（[`.github/dependabot.yml`](../.github/dependabot.yml)）が週に 1 回（更新があった週だけ）、更新の PR を作ります。
- `main` に取り込まれた時、`build.gradle` の `version` がまだ Release のない版なら、同じワークフローがタグ `vX.Y.Z` と GitHub Release（本文は `CHANGELOG.md` のその版の節、jar を添付）を自動で作成します。版を上げない変更（文書だけなど）では作成しません。
- 続けて、Release の jar を Modrinth と CurseForge にも上げます（[`.github/scripts/`](../.github/scripts/) の `publish-modrinth.sh`・`publish-curseforge.sh`。更新内容は英語のお知らせの 1 行と Release へのリンク）。Modrinth はその版がすでにあれば何もしません。CurseForge は上げ済みか確かめられないため、Release を作った時だけ上げます。失敗した時は、Actions の「Modrinth publish」「CurseForge publish」を手動で実行します（CurseForge は、Files に無いことを先に確かめる）。Modrinth の説明文は `doc/store/description.md` を変えると自動で反映されます（「Modrinth description」）。

## バージョンの方針

- **パッチ**（1.8.5 → 1.8.6）: 機能追加・不具合修正。通信内容や保存データの形式は変えないため、同じマイナー内であれば、サーバーとクライアントのパッチが違っても接続できます。
- **マイナー**（1.7.x → 1.8.0）: 互換性が壊れる変更（通信内容の変更、旧バージョンで読めない保存データの変更、設定キーの削除・意味の変更）。マイナーが違うと接続できないため、サーバーと全員のクライアントを同じマイナーに更新してください。
- 判断に迷う場合は、マイナーを上げます。

## 開発環境の細かいこと

（1.9.6 のあとに `CLAUDE.md` から移した。）

- ビルドは公式 MDK ベースの ForgeGradle 3 + Gradle 4.9（1.0 の仕様書にある FG 2.3 ではない）。Gradle 5 以降の構文は使えない（依存は `compile` / `testCompile`）。
- テストを 1 クラスだけ: `./gradlew test --tests io.github.zeusisgood.weakspot.common.WeakSpotPlacerTest`
- JDK 8 がない環境（クラウドのセッションなど）: `apt-get install -y openjdk-8-jdk-headless` で入れ、`JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH ./gradlew build -q` で動かす（Maven Central が 429 を返したら、少し待って再実行）。
- `runServer`: 作業ディレクトリは `run/`、`nogui` 付き。`run/eula.txt` は同意済み。止めるときはコンソールで `stop`。
  - パイプで `stop` を流しても Gradle 経由では届かない。起動の確認は `timeout 150 ./gradlew runServer > ログ` で起動し、ログの `Done (` と `run/config/weakspot.cfg` を見る。
  - devcontainer が使えない環境（クラウドのセッションなど）では、FML の `NetworkRegistry.newChannel` の NPE で落ちる。そこでは `runServer` での確認はしない（ビルドとテストは通す。jar はユーザーが試す）。
  - 起動ログの `module-info.class ... IllegalArgumentException`（`Unable to read a class file correctly`）は FG3 + 1.12 でいつも出るノイズ。`Missing English translation for weakspot: .../build/classes/java/main/assets/...` の WARN も、開発環境のリソースの置き場所によるノイズ。
- 非公開のフィールドはリフレクションで読む（アクセストランスフォーマーは使わない）。開発環境は MCP 名、reobf 後は SRG 名なので、`getDeclaredField` で MCP 名 → SRG 名の順に試す（`Reflect`、`client/MiningProgress`）。Forge の 3 引数の `ReflectionHelper.findField` は起動環境の判定で片方の名前しか試さず、非推奨でもあるので使わない。
  - SRG 名の調べ方: `~/.gradle/caches/forge_gradle/maven_downloader/de/oceanlabs/mcp/mcp_snapshot/20171003-1.12/mcp_snapshot-20171003-1.12.zip` の `fields.csv`（`methods.csv`）、または `~/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860/forge-1.12.2-14.23.5.2860-srg.jar` を `javap -p` で見る。
  - 画面を出さずに両方の環境で確かめるには、自分のクラスを小さなプログラムからリフレクションで呼ぶ（インスタンスは `Unsafe.allocateInstance`）。開発環境は `sourceSets.main.runtimeClasspath`（Gradle の init スクリプトで書き出す）、実際の環境はその中の `build/classes` `build/resources` と `..._mapped_snapshot_...` の jar を、`build/libs` の reobf 済み jar と上の `-srg.jar` に差し替えたクラスパスで動かす。

## リリースの手順

（1.9.6 のあとに `CLAUDE.md` から移した。直し忘れの多くは `ReleaseFilesTest` で CI が赤くなる。）

1. 仕様書 `doc/spec/SPEC_vX.Y.Z.md` を足し、`doc/spec/README.md` の表の一番上に 1 行足す。
2. `build.gradle` の `version` と `WeakSpotMod.VERSION` を揃えて上げる。マイナーを上げるときは `WeakSpotMod.ACCEPTED_VERSIONS` も新しいマイナーの範囲に（例 `[1.10,1.11)`）。
3. `CHANGELOG.md` の一番上に新しい版の節を足す（マイナーなら、旧マイナーとは接続できないことを書く）。
4. README と `README.en.md` のダウンロードのリンク（`releases/download/vX.Y.Z/weakspot-X.Y.Z.jar` と「最新版 X.Y.Z」/「latest: X.Y.Z」）、README の「最近の更新」（新しい 3 件。一番古いものを消す）を直す。
5. 遊び方が変わったら `doc/play.md` と、ガイドの本（`en_us.lang` / `ja_jp.lang` の `weakspot.guide.*`）を直す。種類・操作が変わったら `README.en.md` の表も。
6. 更新のお知らせの要約 `weakspot.news.<版>` を `ja_jp.lang` と `en_us.lang` に 1 行足す（日本語で 40 字くらいまで）。
7. 直下の `update.json` の `promos` の 2 つ（`1.12.2-latest` / `1.12.2-recommended`）を新しい版にし、`"1.12.2"` に英語のお知らせの 1 行を足す（`main` に入った時点で、古い版の人に通知が出始める）。
8. 種類・操作・導入の条件・主な機能が変わったら、配布サイトの説明文 `doc/store/description.md` を直す（下の「配布サイト」）。
9. コミットは「Add the spec for X.Y.Z」→ 機能のコミット（1 つ以上）→ バージョン・README・CHANGELOG.md・doc・CLAUDE.md をまとめた「Release X.Y.Z: 〜」。
10. jar を `build/release/` にもコピーし（Merge の前に試せるように）、作業用ブランチに push して `main` への PR を作る。

Merge のあとは、Actions がタグ・GitHub Release・Modrinth・CurseForge への公開を行う（上の「ブランチとリリース」）。

## 配布サイト

- **Modrinth**: Release の jar を `.github/scripts/publish-modrinth.sh` で上げる（更新内容は `changelog.sh` が作る、英語のお知らせ `weakspot.news.<版>` の 1 行と Release へのリンク）。その版がもうあれば何もしない。失敗したら Actions の「Modrinth publish」を手動で流す。
- **CurseForge**: `publish-curseforge.sh`（更新内容は同じ `changelog.sh`）。上げ済みの版を確かめられないので、Release を今作ったときだけ流す。失敗したら、CurseForge の Files に無いことを確かめてから「CurseForge publish」を手動で流す。Secrets の `CURSEFORGE_TOKEN`、Variables の `CURSEFORGE_PROJECT_ID` が要る（どちらかがなければ何もせず成功で終わる）。
- **説明文**（1.9.1 のあと）: 正本は `doc/store/description.md`（英語の Markdown。版の番号は書かない。リンクは `https://…` の完全な形）。
  - Modrinth は、`main` でこのファイルが変わると `.github/workflows/modrinth-description.yml` が API（`PATCH /v2/project/<ID>`）で反映する。Secrets の `MODRINTH_TOKEN`（スコープ「プロジェクトを書く」）と Variables の `MODRINTH_PROJECT_ID` が要る（どちらかがなければ何もせず成功で終わる。Actions の画面から手動でも流せる）。
  - CurseForge は API で説明を変えられないので手動。このファイルを直した PR では、本文と報告に「CurseForge の説明文を貼り替えてください」と書き、新しい全文を添付する。
- **画像**（1.9.3 のあと）: アイコンの正本は `doc/images/icon.png`（512×512、背景は紺 `#1E2230`。配布サイト・GitHub のソーシャルプレビュー用）と、背景を透過した `doc/images/icon-transparent.png`（README とゲーム内のロゴ `src/main/resources/logo.png`（256×256。`mcmod.info` の `logoFile`。1.9.4）用）。AI 生成なので、Modrinth の開示の「資産」に入れてある。README の冒頭のスクショは `doc/images/mining-combo.jpg`。
- 配布サイトのページが公開されたら: README の「⬇ ダウンロード」を CurseForge → Modrinth → GitHub の順に並べ、案内のリンク（「このページまたは Releases」）を配布サイトに差し替える。GitHub の Release は jar 付きで続ける（ゲーム内の [変更点を見る] と自動化の起点のため）。

## ライセンスと issue

- ライセンスは MIT（1.9.1 のあと。`LICENSE`、`Copyright (c) 2026 zeusisgood`）。Modpack 歓迎。再配布は止めず、README で Releases へのリンクをお願いするだけ。jar にも `LICENSE_weakspot` として入れる（`build.gradle` の `jar`）。
- issue の雛形は、不具合報告 `bug_report.md`・要望 `feature_request.md`・Modpack の報告 `modpack.md`。README のライセンスの節で、Modpack に入れたら issue で「教えてもらえると励みになります」（任意）と書く。

## ブランチの片付け

- 取り込んだブランチは GitHub の設定「Automatically delete head branches」で自動で消える。取り込まれたあとの作業は、同じ名前のブランチを `main` から作り直し、新しい PR にする。
- 自動で消えなかったブランチを消してよいのは、中身がすべて `main` に入っているものだけ（`git merge-base --is-ancestor origin/<ブランチ> origin/main`）。クラウドの取得は履歴が浅いので、先に `git fetch --unshallow` をする（しないと判定を誤る）。`main` にないコミットがあるブランチと、作業中のセッションのブランチは残す。
- タグはブランチと別なので、ブランチを消しても残る。
