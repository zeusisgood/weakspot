# 開発

[← README に戻る](../README.md)

対応する Minecraft の版ごとに、別々のビルドがあります。

| 版 | ローダー（開発・動作確認） | ビルド | JDK |
|---|---|---|---|
| 1.12.2 | **Forge 14.23.5.2860**（公式 MDK、ForgeGradle 3 + Gradle 4.9） | `mc1.12.2/` | 8 |
| 1.21.1 | **NeoForge 21.1.264**（ModDevGradle 2 + Gradle 8。名前は Mojang の名前） | `mc1.21.1/` | 21 |

1.12.2 版は Mod 側で Forge の版を指定していないため、1.12.2 用の Forge であれば読み込まれますが、古いビルドでの動作は確認していません。1.21.1 版は `neoforge.mods.toml` で NeoForge 21.1.264 以上を求めます。リポジトリの devcontainer を使うと、JDK 8 と Claude Code が入った環境で開発できます（1.21.1 には JDK 21 を足す）。

```sh
cd mc1.12.2
./gradlew build        # ビルドとテスト（common/ のテストも）。成果物は mc1.12.2/build/libs/weakspot-1.12.2-<版>.jar
./gradlew test         # 単体テストのみ
./gradlew runServer    # 開発用の専用サーバーを起動（mc1.12.2/run/ で起動。初回は run/eula.txt の同意が必要）

cd mc1.21.1            # JDK 21 で
./gradlew build        # 成果物は mc1.21.1/build/libs/weakspot-1.21.1-<版>.jar（common/ のテストも流れる）
./gradlew runServer    # mc1.21.1/run/ で起動
```

コンテナ内ではゲーム画面を表示できないため、クライアント側の動作は、ビルドした jar をホスト側の Minecraft の `mods` に入れて確認します。

リポジトリの形（別の Minecraft の版にも対応できるように、版ごとに別々の Gradle のビルドを置く）:

```
common/      版に依存しない計算とそのテスト（Java 8 の書き方）。各版のビルドが ../common をソースとして読む
lang/        翻訳の正本（en_us.json・ja_jp.json。読みやすさのため 1 行に 1 つの "キー": "値"）。1.12.2 はビルドのときに .lang を作る
mc1.12.2/    1.12.2 + Forge のビルド（Gradle 4.9、JDK 8）
mc1.21.1/    1.21.1 + NeoForge のビルド（Gradle 8、JDK 21）。翻訳は ../lang の json をそのまま jar に入れる
doc/ CHANGELOG.md README*.md LICENSE update.json   全部の版で共通
```

ソースのパッケージは `io.github.zeusisgood.weakspot`。パッケージごとの中身は [architecture.md](architecture.md) の「全体」を参照してください。

仕様書（1.11.0 以降の版ごとの差分）の一覧は [spec/README.md](spec/README.md)。各機能の仕組みの詳細は [architecture.md](architecture.md)、開発の約束事は [CLAUDE.md](../CLAUDE.md) を参照してください。

## ブランチとリリース

- 作業は `main` 以外のブランチで行い、`main` への PR を作ります。PR ごとに GitHub Actions（[`.github/workflows/ci.yml`](../.github/workflows/ci.yml)）がビルドと全テストを実行し、成功しないと Merge できません。
- テストには、翻訳の抜け（`resources/LangFilesTest`）と、版を上げたときに直すファイルの揃い具合（`resources/ReleaseFilesTest`）の確認も含まれます。配布サイトへ上げるスクリプトは、CI で `shellcheck` にかけます。
- ワークフローで使う部品（`actions/checkout` など）は、Dependabot（[`.github/dependabot.yml`](../.github/dependabot.yml)）が週に 1 回（更新があった週だけ）、更新の PR を作ります。
- ビルドは Minecraft の版ごと（`ci.yml` の matrix の `build-mc`。版を足すときは matrix と `.github/scripts/mc-versions.sh` に足す）。Merge に必須のチェック「`build`」は、全部の版が通ったときだけ成功するまとめのジョブ（ブランチの保護の設定は変えなくてよい）。
- `main` に取り込まれた時、`mc1.12.2/build.gradle` の `version` がまだ Release のない版なら、全部の版のビルドが通ったときだけ、同じワークフローがタグ `vX.Y.Z` と GitHub Release を 1 つ自動で作成します（本文は `CHANGELOG.md` のその版の節、全部の版の jar `weakspot-<Minecraft の版>-<版>.jar` を添付）。版を上げない変更（文書だけなど）では作成しません。
- 続けて、Release の jar を Minecraft の版ごとに Modrinth と CurseForge にも上げます（下の「配布サイト」）。

## バージョンの方針

[CLAUDE.md](../CLAUDE.md) の「バージョン」を参照してください。

## 開発環境の細かいこと

- テストの作業ディレクトリはリポジトリの直下（`mc1.12.2/build.gradle` の `test`）。テストを 1 クラスだけ: `./gradlew test --tests io.github.zeusisgood.weakspot.common.WeakSpotPlacerTest`
- クラウドのネットワーク: ForgeGradle と Forge は `maven.minecraftforge.net` から取るので、環境のネットワーク設定（セッションのタイトルバーのクラウド環境のメニュー → Edit → Network access）で Custom にし、Allowed domains にこのホストを足す（パッケージマネージャーの既定の一覧は残す。手順は https://code.claude.com/docs/en/cloud-environments#network-access ）。拒否されると `Received status code 403` で止まる。
- Maven Central の 429 の再実行（[CLAUDE.md](../CLAUDE.md) のとおり 30 秒ずつ 5 回）:
  ```
  for i in 1 2 3 4 5; do JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH ./gradlew build -q > build.log 2>&1 && break; grep -q 429 build.log || break; sleep $((i*30)); done
  ```
- Gradle が使えない間（ネットワークの拒否など）でも、`common/` は MC に依存しないので `javac` だけでテストできる（junit は Gradle 4.9 の配布物の `lib/plugins/` にある。リポジトリの直下で流す）。
  ```
  L=$(echo ~/.gradle/wrapper/dists/gradle-4.9-bin/*/gradle-4.9/lib/plugins); CP=$L/junit-4.12.jar:$L/hamcrest-core-1.3.jar; O=$(mktemp -d)
  javac -encoding UTF-8 -d $O -cp $CP common/src/main/java/io/github/zeusisgood/weakspot/common/*.java common/src/test/java/io/github/zeusisgood/weakspot/common/*.java
  java -cp $O:$CP org.junit.runner.JUnitCore $(cd common/src/test/java && ls io/github/zeusisgood/weakspot/common/*Test.java | sed 's/\.java//;s#/#.#g')
  ```
  `ReleaseFilesTest` も同じ形で流せる（`mc1.12.2/src/test/java/.../resources/ReleaseFilesTest.java` と `ResourceFiles.java` を足す）。MC に依存するコードのコンパイルはできないので、そのままでは push しない（CLAUDE.md の「リリース」）。
- `runServer`: 作業ディレクトリは `mc1.12.2/run/`、`nogui` 付き。`run/eula.txt` は同意済み。止めるときはコンソールで `stop`。
  - パイプで `stop` を流しても Gradle 経由では届かない。起動の確認は `timeout 150 ./gradlew runServer > ログ` で起動し、ログの `Done (` と `mc1.12.2/run/config/weakspot.cfg` を見る。
  - devcontainer が使えない環境（クラウドのセッションなど）では、FML の `NetworkRegistry.newChannel` の NPE で落ちる。そこでは `runServer` での確認はしない（ビルドとテストは通す。jar はユーザーが試す）。
  - 起動ログの `module-info.class ... IllegalArgumentException`（`Unable to read a class file correctly`）は FG3 + 1.12 でいつも出るノイズ。`Missing English translation for weakspot: .../build/classes/java/main/assets/...` の WARN も、開発環境のリソースの置き場所によるノイズ。
- 1.21.1: クラウドでは `apt-get install -y openjdk-21-jdk-headless`。ネットワークの許可に `maven.neoforged.net`・`piston-meta.mojang.com`・`piston-data.mojang.com`・`libraries.minecraft.net`・`resources.download.minecraft.net` も足す。初回のビルドは Minecraft の展開と再コンパイルで数分かかる。非公開のフィールド・メソッドは**アクセストランスフォーマー**（`mc1.21.1/src/main/resources/META-INF/accesstransformer.cfg`。`neoforge.mods.toml` の `[[accessTransformers]]` で読む）。名前を間違えるとビルドの「recompile」で止まる。上書きしているサブクラスがあれば、そちらも同じ公開範囲にする（例: ディスペンサーとドロッパーの `dispenseFrom`）。バニラの名前・中身は、ビルドが作るソースの jar（`~/.gradle/caches` の `neoforge-*-sources.jar`。IDE で開く）で調べる。
- 1.12.2: 非公開のフィールドはリフレクションで読む（アクセストランスフォーマーは使わない）。開発環境は MCP 名、reobf 後は SRG 名なので、`getDeclaredField` で MCP 名 → SRG 名の順に試す（`Reflect`）。Forge の 3 引数の `ReflectionHelper.findField` は起動環境の判定で片方の名前しか試さず、非推奨でもあるので使わない。
  - SRG 名の調べ方: `~/.gradle/caches/forge_gradle/maven_downloader/de/oceanlabs/mcp/mcp_snapshot/20171003-1.12/mcp_snapshot-20171003-1.12.zip` の `fields.csv`（`methods.csv`）、または `~/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860/forge-1.12.2-14.23.5.2860-srg.jar` を `javap -p` で見る。
  - 画面を出さずに両方の環境で確かめるには、自分のクラスを小さなプログラムからリフレクションで呼ぶ（インスタンスは `Unsafe.allocateInstance`）。開発環境は `sourceSets.main.runtimeClasspath`（Gradle の init スクリプトで書き出す）、実際の環境はその中の `build/classes` `build/resources` と `..._mapped_snapshot_...` の jar を、`build/libs` の reobf 済み jar と上の `-srg.jar` に差し替えたクラスパスで動かす。

## リリースの手順

（直し忘れの多くは `ReleaseFilesTest` で CI が赤くなる。）

1. 仕様書 `doc/spec/SPEC_vX.Y.Z.md` を足し、`doc/spec/README.md` の表の一番上に 1 行足す。
2. 版を書く 4 か所（`mc1.12.2/build.gradle` の `version`・1.12.2 の `WeakSpotMod.VERSION`・`mc1.21.1/gradle.properties` の `mod_version`・1.21.1 の `WeakSpotMod.VERSION`）を揃えて上げる。片方の版だけを直したときも、両方を上げる。マイナーを上げるときは 1.12.2 の `WeakSpotMod.ACCEPTED_VERSIONS` を新しいマイナーの範囲に（例 `[1.12,1.13)`）、1.21.1 の `Network.PROTOCOL` を新しいマイナーの番号に（例 `1.12`）。あわせて、前のマイナーの仕様書を `doc/spec/` から消す（タグに残る。消す前に、そこにしか書いていない今の挙動を `doc/play.md`・`doc/config.md`・`doc/architecture.md` に移し、コードのコメントからの参照をタグのリンクに直し、`doc/spec/README.md` の表の行をタグへのリンクの 1 行にまとめる）。
3. `CHANGELOG.md` の一番上に新しい版の節を足す（マイナーなら、旧マイナーとは接続できないことを書く）。
4. README の「最近の更新」（新しい 3 件。一番古いものを消す）を直す。`README.en.md` の「Recent updates」も同じ 3 件にし、各版は英語のお知らせ（6.）の 1 行をそのまま使う。README のダウンロードは配布サイトへのリンクだけなので、版ごとには直さない。
5. 遊び方が変わったら `doc/play.md` と、ガイドの本（`lang/en_us.json` / `lang/ja_jp.json` の `weakspot.guide.*`。1 ページは 14 行まで）を直す。操作が変わったら `README.en.md` の操作の表も（種類の説明は `doc/store/description.md` にだけ書く）。
6. 更新のお知らせの要約 `weakspot.news.<版>` を `lang/ja_jp.json` と `lang/en_us.json` に 1 行足す（日本語で 40 字くらいまで）。
7. 直下の `update.json` の `promos` の 4 つ（`1.12.2-latest` / `1.12.2-recommended` / `1.21.1-latest` / `1.21.1-recommended`）を新しい版にし、`"1.12.2"` と `"1.21.1"` の両方の節に英語のお知らせの 1 行を足す（`main` に入った時点で、古い版の人に通知が出始める）。
8. 種類・操作・導入の条件・主な機能が変わったら、配布サイトの説明文 `doc/store/description.md` を直す（下の「配布サイト」）。
9. コミットは「Add the spec for X.Y.Z」→ 機能のコミット（1 つ以上）→ バージョン・README・CHANGELOG.md・doc・CLAUDE.md をまとめた「Release X.Y.Z: 〜」。
10. 両方の jar を `mc1.12.2/build/release/` にもコピーし（Merge の前に試せるように）、作業用ブランチに push して `main` への PR を作る。

Merge のあとは、Actions がタグ・GitHub Release・Modrinth・CurseForge への公開を行う（上の「ブランチとリリース」）。

## 配布サイト

- どちらも、Release に付いている jar の Minecraft の版ごとに 1 回ずつ上げる。版ごとの値（ローダー・Java・CurseForge の版の種類）は `.github/scripts/mc-versions.sh` の表。
- **Modrinth**: Release の jar を `.github/scripts/publish-modrinth.sh <版> <Minecraft の版>` で上げる（更新内容は `changelog.sh` が作る、英語のお知らせ `weakspot.news.<版>` の 1 行と Release へのリンク）。バージョンの番号は `<版>+<Minecraft の版>`（例 `1.11.12+1.12.2`。1.11.11 までは `<版>` だけ）。その番号がもうあれば何もしない。失敗したら Actions の「Modrinth publish」を手動で流す（Minecraft の版を空にすると、Release に付いている全部の版）。
- **CurseForge**: `publish-curseforge.sh <版> <Minecraft の版>`（更新内容は同じ `changelog.sh`。表示名は `Weak Spot Mining <版> (<Minecraft の版>)`）。上げ済みの版を確かめられないので、Release を今作ったときだけ流す。失敗したら、CurseForge の Files に無いことを確かめてから「CurseForge publish」を手動で、失敗した Minecraft の版だけを指定して流す。Secrets の `CURSEFORGE_TOKEN`、Variables の `CURSEFORGE_PROJECT_ID` が要る（どちらかがなければ何もせず成功で終わる）。
- **説明文**: 正本は `doc/store/description.md`（英語の Markdown。版の番号は書かない。リンクは `https://…` の完全な形）。
  - Modrinth は、`main` でこのファイルが変わると `.github/workflows/modrinth-description.yml` が API（`PATCH /v2/project/<ID>`）で反映する。Secrets の `MODRINTH_TOKEN`（スコープ「プロジェクトを書く」）と Variables の `MODRINTH_PROJECT_ID` が要る（どちらかがなければ何もせず成功で終わる。Actions の画面から手動でも流せる）。
  - CurseForge は API で説明を変えられないので手動。このファイルを直した PR では、本文と報告に「CurseForge の説明文を貼り替えてください」と書き、新しい全文を添付する。
- **画像**: アイコンの正本は `doc/images/icon.png`（512×512、背景は紺 `#1E2230`。配布サイト・GitHub のソーシャルプレビュー用）と、背景を透過した `doc/images/icon-transparent.png`（README とゲーム内のロゴ `mc1.12.2/src/main/resources/logo.png`（256×256。`mcmod.info` の `logoFile`）用）。AI 生成なので、Modrinth の開示の「資産」に入れてある。README の冒頭のスクショは `doc/images/mining-combo.jpg`。
- **配布サイトのページ**: CurseForge は公開済み（https://www.curseforge.com/minecraft/mc-mods/weakspot、Project ID 1714015）。README（日英）の「⬇ ダウンロード」・冒頭のバッジ（ダウンロード数）・ライセンスの節の案内、`doc/install.md`、`update.json` の `homepage` は CurseForge を指す。GitHub の Releases は README に載せない（要る人は自分で見つける。ユーザーの判断）が、jar 付きで続ける（自動化の起点のため）。ゲーム内の [変更点を見る]・[ダウンロード] は CurseForge のファイルの一覧を指す。

## ライセンスと issue

- ライセンスは MIT（`LICENSE`、`Copyright (c) 2026 zeusisgood`）。Modpack 歓迎。再配布は止めず、README で CurseForge のページへのリンクを「助かります」とお願いするだけ（Modpack 歓迎の行とぶつからないように、「再アップロードしないで」とは書かない）。jar にも `LICENSE_weakspot` として入れる（`mc1.12.2/build.gradle` の `jar`）。
- issue の雛形は [`.github/ISSUE_TEMPLATE/`](../.github/ISSUE_TEMPLATE/)。不具合報告は `/weakspot bug` の本文と見出しを揃えているので Markdown のまま。白紙の issue は許可したまま（ゲームからのリンクの本文が消えないように）。雛形のラベルはリポジトリに先に作っておく（無いと付かない）。

## ブランチの片付け

- 自動で消えなかったブランチ（取り込んだブランチは、GitHub の設定「Automatically delete head branches」で消える）を消してよいのは、中身がすべて `main` に入っているものだけ（`git merge-base --is-ancestor origin/<ブランチ> origin/main`）。クラウドの取得は履歴が浅いので、先に `git fetch --unshallow` をする（しないと判定を誤る）。`main` にないコミットがあるブランチと、作業中のセッションのブランチは残す。
- タグはブランチと別なので、ブランチを消しても残る。
