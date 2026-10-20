# 弱点破壊 Mod 追加仕様書（v1.13.0）

`SPEC_v1.12.3.md`（Mod 1.12.3）に対する**マイナー**の仕様（Minecraft 1.20.1 + Forge への移植）。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.13.0.md`。

- 対象:
  - **Minecraft Java Edition 1.20.1 / Forge 47.4.28**（新規。`mc1.20.1/`）
  - Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860（今まで。`mc1.12.2/`）
  - Minecraft Java Edition 1.21.1 / NeoForge 21.1.264（今まで。`mc1.21.1/`）
- 前提: 1.12.3 が `main` に取り込み済み
- 経緯: `doc/roadmap.md` の「ほかの版の候補」。1.20.1 の Modpack はまだ多い（Better MC 4・All the Mods 9 など）
- ユーザーとの相談で決まったこと:
  - ローダーは **Forge 47**（1.20.1 の Modpack の大半が Forge。1.20.1 の NeoForge は使わない）
  - 版は **1.13.0（マイナー）**。1.12.0 と同じく「新しい Minecraft の版に対応する節目」のため（通信は変わらないが、決まりどおり接続できる範囲を新しいマイナーにする）
  - 対象は 1.21.1 版と同じ（下の §2）。1.20.1 にない物は出ない
  - 版が 3 つになる手間を受け入れる。**これからも 3 つの版で機能を揃え続け、いつも 3 つの jar を同じ番号で出す**
  - 移植は 1.21.1 版を手本にする（Mojang の名前・アクセストランスフォーマー・クラスの分け方が同じため）

---

## 0. バージョンと互換性

- バージョンを 1.12.3 → **1.13.0** にする。版を書く場所は 6 か所になる: `mc1.12.2/build.gradle`・1.12.2 の `WeakSpotMod.VERSION`・`mc1.20.1/gradle.properties` の `mod_version`・1.20.1 の `WeakSpotMod.VERSION`・`mc1.21.1/gradle.properties` の `mod_version`・1.21.1 の `WeakSpotMod.VERSION`（`ReleaseFilesTest` で確かめる）
- 接続できる範囲: 1.12.2 の `ACCEPTED_VERSIONS` を `"[1.13,1.14)"`、1.20.1・1.21.1 の `Network.PROTOCOL` を `"1.13"` にする。1.12.x とは接続できない（`CHANGELOG.md` に書く）
- Minecraft の版が違う jar 同士は、もともと接続できない（Mod の側では何もしない）
- マイナーを上げたときの決まりどおり、片付ける:
  - `PlayerText.of` の `since` が 1.13.0 以下のもの → `TextComponentTranslation`（1.21.1 は `Component.translatable`）
  - `ServerFeatures.since("1.12.x")`・`PlayerText.clientSince(player, "1.12.x")` → いつも true なので、条件を消す（1.12.3 のメイスなど 4 つの条件を含む）
  - 仕組み（`ServerFeatures`・`PlayerText`・`ServerLang`）そのものは残す
- 1.12.x の仕様書（`doc/spec/SPEC_v1.12.*.md`）を消す（`doc/development.md` の「リリースの手順」の 2.。消す前に、そこにしか書いていない今の挙動を `doc/play.md`・`doc/config.md`・`doc/architecture.md` に移す）

## 1. 1.20.1 版の土台

- ディレクトリ `mc1.20.1/`。**Java 17**・**Gradle 8**・**ForgeGradle 6**（Forge が 1.20.1 で勧めている MDK の形）。名前は Mojang の名前（`official` のマッピング。Parchment は使わない。許可するホストを増やさないため）
- Mod の ID・パッケージ・クラスの分け方（`client/`・`server/`・`config/`・`network/`）・クラスの名前は、1.21.1 版と同じにする（読み比べやすくするため）
- `../common/src/main/java` と `../common/src/test/java` をソースに足す。翻訳は `../lang/*.json` をそのまま `assets/weakspot/lang/` に入れる（1.20.1 も json）
- `META-INF/mods.toml`: 名前・説明・ロゴ・ライセンス（MIT）・`updateJSONURL`（同じ `update.json`）・クライアントとサーバーの両方に必要・Forge `[47.4,)`・Minecraft `[1.20.1]`
- jar は `weakspot-1.20.1-forge-<版>.jar`（`doc/development.md` の「版の表記」）。`LICENSE` を `LICENSE_weakspot` として入れる
- 非公開のフィールド・メソッドは**アクセストランスフォーマー**（`META-INF/accesstransformer.cfg`。1.21.1 版と同じ考え）
- クライアントだけのクラスは `Dist.CLIENT` の購読にし、パケットのハンドラーから直接触らない（`proxy.onXxx`。両方の版と同じ）

## 2. 対象の範囲（全部）

1.21.1 版でできることを全部、1.20.1 版でも同じように動かす（19 種類・コンボ・節目・音・他プレイヤーの表示・統計の画面・的当て・ガイドの本・設定画面・自分のプリセット・コマンド・お知らせ・進捗・順位）。数（倍率・時間・間隔・しきい値・色・音）は `common/` と設定の初期値で同じにする。

1.21.1 版だけの対象のうち、1.20.1 にある物は対象にし、ない物は出ない（どちらも 1.21.1 版の決まりのまま）:

| 物 | 1.20.1 |
|---|---|
| ラクダ・ストライダー・チェスト付きボート・いかだ（乗り物） | ある → 対象 |
| トライデント・クロスボウ（弓。1.12.3） | ある → 対象（クロスボウの過剰チャージは、アイテムの NBT に記録する。1.21.1 の `custom_data` と同じ中身） |
| 燻製器・溶鉱炉・焚き火（機械）・足場とツタ（はしご）・銅の錆び・アメジストの芽（しゃがんでの成長） | ある → 対象 |
| 村人の繁殖の案内（空いているベッド・食べ物の点数） | 1.21.1 と同じ条件 |
| アルマジロのウロコ・ウィンドチャージ・メイス・クラフター | ない |
| 試練のスポナー・宝物庫（機械の対象外） | ない（設定の初期値の対象外の一覧に書いてあっても害はない） |

- 「両手が空」の判定（`HeldItems`）は、決まりどおり 1.20.1 の右クリックで動くアイテムを調べ直す
- バニラを調べた結果に頼っている所（`doc/architecture.md` の「重要」の節・見えない壁・保留の破壊・エリトラなど）は、1.20.1 のバニラで調べ直し、違えば `doc/architecture.md` に版ごとに書く

## 3. 仕組みの置き換え（1.21.1 → 1.20.1）

置き換え先が見つからない・同じ動きにできないものは、止まる条件に当たる（§8）。

| 1.21.1 + NeoForge | 1.20.1 + Forge（目安。実装で確かめる） |
|---|---|
| `CustomPacketPayload`・`RegisterPayloadHandlersEvent` | `SimpleChannel`（`NetworkRegistry.newSimpleChannel`。プロトコルの印 `Network.PROTOCOL`）。メッセージの中身の並びは同じ |
| `ModConfigSpec` | `ForgeConfigSpec`（同じファイルの名前・同じ組み方） |
| データアタッチメント（プレイヤーの保存データ） | プレイヤーの `getPersistentData()` の `PlayerPersisted` の下の `weakspot`（1.12.2 と同じ。死亡でも引き継がれる） |
| データコンポーネント（ガイドの本・クロスボウ） | アイテムの NBT（ガイドの本は `pages`・`resolved` など 1.20.1 の形） |
| `RenderLevelStageEvent`・`RenderGuiEvent.Post`・`ScreenEvent.Render.Post` | 同じ名前のイベント（Forge 47 にもある。HUD は `RenderGuiOverlayEvent.Post` か `RenderGuiEvent.Post`） |
| `ClientTickEvent.Pre/Post` など NeoForge 21 のイベント | Forge 47 の `TickEvent`（`phase` で分ける） |
| `HelloMessage`（クライアントの版） | 同じ（Forge 47 の接続の情報からも版は読めないため、1.21.1 と同じメッセージを使う） |
| `VersionChecker`（NeoForge） | Forge の `VersionChecker` |

- `client/Draw`（色だけの図形）は、1.20.1 の `Tesselator`・`BufferBuilder` の書き方に直す（1.21.1 の書き方とは少し違う）
- ヒットの間隔・採掘のブーストを捨てる決まり・予約と確定・コンボはサーバーの数が正・サーバーはクライアントの数を正にしない、は同じ決まり
- **採掘の破壊速度**: クライアント（積算）とサーバー（瞬間判定）の結果が一致するかを、1.20.1 のバニラ（`MultiPlayerGameMode`・`ServerPlayerGameMode`）で確かめ直す。見えない壁が出ないことを確かめる

## 4. 設定

- 項目の名前・カテゴリ・初期値・範囲・説明は、両方の版と同じ
- ファイルは 1.21.1 と同じ: `config/weakspot-server.toml`（`ModConfig.Type.COMMON`）と `config/weakspot-client.toml`（`CLIENT`）
- 設定画面は 1.21.1 版と同じ `SettingGroups` のまとめの画面（Forge の `ConfigScreenHandler` で Mod の一覧の「設定」から開く）

## 5. 保存データ

- ほかの版のワールドからの引き継ぎはしない（統計は 0 から始まる。1.12.0 と同じ）
- これからの版で、1.20.1 版の古いデータを読めなくする変更をしない

## 6. 1.12.2 版・1.21.1 版の変更

- §0 の版・接続の範囲・マイナーの片付けだけ。遊び方・見た目・設定・保存データは変えない

## 7. 配布

- `ci.yml` の matrix に `mc: '1.20.1', java: '17'` を足す。`.github/scripts/mc-versions.sh` に 1.20.1 の行（`LOADER=forge`・`CF_LOADER=Forge`・`CF_JAVA="Java 17"`・`CF_TYPE=minecraft-1-20`）
- GitHub の Release に jar を 3 つ。Modrinth は `1.13.0+1.20.1`（`1.20.1`・`forge`）を足す。CurseForge も同じプロジェクトにファイルを足す
- `update.json` に `"1.20.1"` の節と、`promos` の `1.20.1-latest`・`1.20.1-recommended`
- `ReleaseFilesTest`: 版の 6 か所・`Network.PROTOCOL` の 2 か所・`update.json` の 3 つの節を確かめる
- 説明文 `doc/store/description.md` に、1.20.1 + Forge を足す（CurseForge は手で貼り替え）。CurseForge・Modrinth のプロジェクトの「対応する版・ローダー」に 1.20.1 を足す（ユーザーが画面で行う）

## 8. 進め方

- PR は 1 つ（「1.13.0」）。コミットは、土台 → 設定と通信 → 種類ごと → 種類をまたぐもの → マイナーの片付け → 「Release 1.13.0: …」の順を目安に分けてよい
- 途中のコミットは push してよい（PR は全部が揃ってから。ビルドが通らない状態で PR を作らない）
- **止まる条件**: 決まりどおり（互換性を破る変更が要る、仕様の意図が読み取れない、ビルドやテストが通らない、runServer が起動しない）に加えて、**ある種類を 1.20.1 で同じ動きにできない**と分かったとき（どの種類の何が、なぜできないか、代わりの案を添える）
- **ネットワーク**: `maven.minecraftforge.net` と Mojang の配布（`piston-meta.mojang.com`・`piston-data.mojang.com`・`libraries.minecraft.net`・`resources.download.minecraft.net`）。今の許可のままで足りる見込み
- runServer: 1.20.1 版がクラウドで起動できれば、クラウドでも確かめる

## 9. README・doc

- README（日英）: 「対応する版」に「1.20.1 + Forge（開発・動作確認は 47.4.28）」を足す。「最近の更新」に 1.13.0
- `CHANGELOG.md`: 「## 1.13.0」に「Minecraft 1.20.1 + Forge に対応（1.21.1 版と同じ機能。1.20.1 にない物（アルマジロ・ウィンドチャージ・メイス・クラフター）は対象外）」「1.12.2 版・1.21.1 版は版の番号だけ変更」「1.12.x とは接続不可（サーバーと全員を 1.13.x に）」
- お知らせ `weakspot.news.1.13.0`: 日本語「Minecraft 1.20.1（Forge）に対応しました」、英語「Now available for Minecraft 1.20.1 (Forge)」
- `doc/install.md`: 必要環境の表に 1.20.1 の行
- `doc/play.md`: 「1.21.1 版のみ」と書いた所のうち、1.20.1 にもある物を「1.20.1 版・1.21.1 版」に直す（アルマジロなどは「1.21.1 版のみ」のまま）
- `doc/config.md`: 版ごとのファイルの表に 1.20.1
- `doc/architecture.md`: 「1.21.1 版」の節を「1.20.1 版・1.21.1 版」にし、1.20.1 で違う所（通信・保存データ・NBT・イベント）だけを書き足す
- `doc/development.md`: 版の表に 1.20.1（JDK 17・ForgeGradle 6）、リリースの手順（版の 6 か所・`update.json` の 3 つの節・jar 3 つ）
- `doc/roadmap.md`: 「ほかの版の候補」の 1.20.1 を済みにする
- `doc/spec/README.md`: 表の一番上に 1.13.0。1.12.x の行は、タグへのリンクの 1 行にまとめる
- ガイドの本: 遊び方の文は版で変わらないので直さない

## 10. `CLAUDE.md` に書くこと

- 「プロジェクト概要」の対象に 1.20.1 / Forge 47.4.28 を足す（3 つ。機能は揃え続ける）
- 「開発環境・コマンド」: 1.20.1 のビルド（JDK 17・ForgeGradle 6）とコマンド
- 「バージョン」: 版を書く 6 か所・いつも 3 つの jar を同じ番号で出す・現行は 1.13.0、範囲は `"[1.13,1.14)"`（`PROTOCOL` は `"1.13"`）
- 「アーキテクチャの約束事」: 機能を足す・直すときは **3 つの版で同じ PR で**。1.21.1 の API の名前が出てくる決まりに、1.20.1 で対応するものを書き足す（`TickEvent` の `phase`・`SimpleChannel` など、違うものだけ）
- 「次の作業」: 1.13.0 のあと

## 11. ユーザーに確認してもらうこと

- **1.20.1 版**（`weakspot-1.20.1-forge-1.13.0.jar`。Forge 47.4.x を入れた 1.20.1 で）:
  - 19 種類の弱点が全部出て、当たると効くこと（特に、採掘で見えない壁が出ないこと・エリトラ・落下・泳ぎ）
  - ラクダ・ストライダー・チェスト付きボート・いかだ、トライデント・クロスボウ、燻製器・溶鉱炉、足場
  - コンボ・節目・音・統計の画面（K キー）・的当て・ガイドの本・設定画面・`/weakspot bug`
  - 専用サーバー: 他プレイヤーのマーク・設定がサーバーの値になること
  - Modpack と一緒に入れても動くこと（入れられれば。Better MC 4・All the Mods 9 など）
- **1.12.2 版・1.21.1 版**（1.13.0）: 今までどおり動くこと。1.12.x のサーバーにはつながらないこと
- Merge のあと: Release に jar が 3 つ、Modrinth・CurseForge に 3 つずつ上がること。CurseForge・Modrinth のページで 1.20.1 が選べること
