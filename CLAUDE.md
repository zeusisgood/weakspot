# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

Fortnite の「弱点（クリティカル）」採掘を Minecraft に持ち込む Mod。対象は **Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860**。
仕様書はすべて `doc/` にある。仕様の正本は `doc/SPEC_v1.0.md`（MVP。数値・挙動・MVP 完了条件 §12・スコープ外 §13）と、その差分を定める `doc/SPEC_v1.1.md`（Mod 1.1.0。スコープ外は §14、実装時の確認事項は §12）、`doc/SPEC_v1.1.1.md`（Mod 1.1.1。耐久回復の修正だけ）。v1.1 に書かれていないことは v1.0 と現行実装のまま、v1.1.1 に書かれていないことは v1.1 と現行実装のまま。仕様と食い違う実装をする場合はユーザーに確認する。

## 開発環境・コマンド

- devcontainer で **JDK 8** を使う。ビルドは公式 MDK ベースの **ForgeGradle 3 + Gradle 4.9**（仕様書の FG 2.3 ではない）。Gradle 5 以降の構文は使えない（依存は `compile` / `testCompile`）。
- ビルド + テスト: `./gradlew build` → 成果物は `build/libs/weakspot-<version>.jar`（reobf 済み）
- テストのみ: `./gradlew test`、1クラスだけ: `./gradlew test --tests com.example.weakspot.common.WeakSpotPlacerTest`
- 専用サーバー起動: `./gradlew runServer`（作業ディレクトリは `run/`、`nogui` 付き。`run/eula.txt` は同意済み）。止めるときはコンソールで `stop`。
  - パイプで `stop` を流しても Gradle 経由では届かない。Claude が起動を確かめるときは `timeout 150 ./gradlew runServer > ログ` で起動し、ログの `Done (` と `run/config/weakspot.cfg` を確認する。
  - 起動ログの `module-info.class ... IllegalArgumentException` は FG3 + 1.12 でいつも出るノイズで、無視してよい。
- クライアント確認: コンテナ内では画面を出せない。ビルドした jar をホスト側 Minecraft（Forge 1.12.2）の `mods` に入れて確認する。描画・ヒット判定・体感速度は Claude が検証できないので、ユーザーに確認を依頼する。
- Mod のバージョンは `build.gradle` の `version` と `WeakSpotMod.VERSION` の2か所にある。変えるときは両方を揃える。
- バージョンの方針（1.1.0 以降）:
  - 機能の追加・不具合の修正ごとに**パッチ**を上げる（1.1.0 → 1.1.1）。
  - 互換性を破るときは**マイナー**を上げる（1.1.x → 1.2.0）。迷ったらマイナー。互換性を破る変更とは、通信内容の変更（パケットの追加・削除・中身の変更）、古い版で読めなくなるサーバー保存データの形式変更、設定キーの削除や意味の変更。通信内容を変えたら必ずマイナーを上げる。
  - `@Mod` の `acceptableRemoteVersions` で、同じマイナー同士（例: `[1.1,1.2)`）なら接続できるようにする。マイナーを上げるときは、`build.gradle` と `WeakSpotMod.VERSION` に加えて、この範囲も新しいマイナーに書き換え、README の更新履歴に旧マイナーとは接続できないことを書く。
  - 現行は 1.1.0。範囲は `WeakSpotMod.ACCEPTED_VERSIONS = "[1.1,1.2)"`（Maven のバージョン範囲の書式。Forge の `VersionRange`）。
- リリースの流れ: README の「最新版」の行と「更新履歴」を更新 → コミット → 注釈付きタグ `vX.Y.Z` → `main` とタグを push。GitHub Release はユーザーが手動で作り、`build/libs/weakspot-X.Y.Z.jar` を添付する。

## アーキテクチャ

クライアントとサーバーの**両方に Mod が必要**（1.1.0 からは同じマイナー同士なら接続できる。1.0.x とは接続できない）。パッケージは `com.example.weakspot`。

弱点は3種類（`common/HitKind`）: **採掘**（左の長押し）、**成長**（素手で右クリックを押しっぱなし。成長できる `IGrowable`）、**機械**（しゃがんで両手が空のまま右クリックを押しっぱなし。`ITickable` の TE）。節目と耐久回復は採掘だけが対象。

- `common/`: Minecraft に依存しない純粋な計算（面の (u,v) 座標変換と一番大きい面、弱点の配置と最小半径、ブースト量、ヒット音の音階 `HitPitch`、統計 `MiningStats`、節目と耐久回復 `Milestones`、機械の加速 `MachineBoost`、マークの送信頻度 `MarkerSendPolicy`、色 `MarkerColor`）。単体テストはここだけにある。1.7.10 への移植を見込んで、MC クラスを持ち込まない。
- `RightClickTargets`（両側）: 右クリックの弱点の対象判定。クライアントとサーバーで同じ条件（同期した設定）を使う。対象を条件どおりに右クリックしたら、`RightClickBlock` をメインハンドで SUCCESS にしてキャンセルし、通常動作（GUI、オフハンドの設置など）を止める。クライアントでキャンセルしてもバニラは右クリックのパケットを送るので、サーバーでも発火し、そこで「直前に右クリックした」ことを記録する。右クリックを押しっぱなしにすると、バニラは 4 tick ごとに右クリックする。
- `client/`（`@EventBusSubscriber(value = Side.CLIENT)`。専用サーバーではロードされない）: 弱点の状態、ヒット判定、描画、ヒット音、クライアント側のブースト。弱点は一度に1つ（`ClientWeakSpotHandler.spot`）。
  - ヒット判定は `RenderWorldLastEvent` で**毎フレーム**行う（tick 単位だと素早い照準移動を取りこぼす）。右クリックの押しっぱなしは `keyBindUseItem.isKeyDown()` で見る。作物・苗木の弱点は一番大きい面（多くは上面）に出し、照準がその面に当たっているときだけヒットにする。育って当たり判定の箱が変わったら出し直す。
  - ブーストの時間枠は `ClientTickEvent` START で増える `clientTick` で数える。`PlayerControllerMP` は tick ごとに進捗を積算するので、枠内の tick だけ倍率を掛ければよい。
  - ヒット音（`HitSounds`）: 楽器と音量は各自の設定（`myHitSound` / `myHitVolume`、`othersHitSound` / `othersHitVolume`）。どれも「プレイヤー」のカテゴリ。自分の音と試聴は距離なし（`AttenuationType.NONE`）、他のプレイヤーの音はブロックの位置から。連続ヒット数（種類・ブロックをまたいで続き、40 tick ヒットがないとリセット）に応じて長音階を上がり、1オクターブで最初に戻る。
  - 統計画面（`StatsScreen`、K キー）: 「統計」タブはサーバーから届いた数字を表示するだけ（開いたとき・設定画面から戻ったときに要求）。「サウンド」タブは楽器・音量・試聴で、`WeakSpotConfig.save()`（`ConfigManager.sync`）で `weakspot.cfg` に保存する。「設定画面を開く」は `WeakSpotGuiFactory.create`（Mods メニューと同じ `GuiConfig`。他人のサーバーに接続中は2行目に注意書き）。
  - 他のプレイヤーのマーク（`OtherMarkers`）: 自分の採掘の弱点が出た・動いた・消えたときに送り（`markerSendMinIntervalTicks` で間引き、出ている間は 20 tick ごとに送り直す）、届いたマークは 60 tick 更新がなければ消す。描画は `WeakSpotRenderer` で、色と濃さは各自の設定。
  - 節目の演出（`MilestoneEffects`）: タイトル、チャット1行、花火、音階の駆け上がり。777 は虹色で派手にする。
  - 画面の文字列は `assets/weakspot/lang/en_us.lang` と `ja_jp.lang` の両方に追加すること。
  - キーバインドの登録は `@SidedProxy`（`CommonProxy` / `client.ClientProxy`）の `init` で行う。
- `network/`（`WeakSpotMod.preInit` で登録。番号は登録順）: パケットのハンドラーは専用サーバーでもインスタンス化されるので、クライアント行きのパケットは、クライアントのクラスに `proxy.onXxx` 経由でアクセスする。
  - `HitMessage`（C→S）: ヒット通知（種類、BlockPos、連続ヒット数）。
  - `OtherHitMessage`（S→C）: 受け付けた採掘ヒットを 16 ブロック以内の他のプレイヤーに転送する（ヒット音用）。
  - `SettingsMessage`（S→C）: サーバーの設定値（`SyncedSettings`）。ログイン時と、ホストが設定を変えたとき。
  - `StatsRequestMessage`（C→S、リセットの指示を含む）/ `StatsMessage`（S→C）: 統計。
  - `MilestoneMessage`（S→C）: 達成した節目。
  - `MarkerMessage`（C→S）/ `OtherMarkerMessage`（S→C）: 採掘の弱点マーク。サーバーは検証せずに転送する。
- `server/`（論理サーバー）:
  - `ServerBoostTracker`: `LeftClickBlock` で「今どのブロックを破壊中か」と開始 tick、弱点が出るブロックか（一瞬で壊れないか）を記録する。採掘ヒットはそのブロックと一致したときだけ受け付ける。「壊した」は `BreakEvent`（LOWEST、キャンセルされていないもの）で数える。アクセストランスフォーマーは使っていない。
  - `RightClickHits`: 成長・機械ヒットの検証（直前 10 tick 以内にそのブロックを右クリックしたか、届く距離か、間隔、対象か）と効果（成長は `randomTick` を余分に呼ぶ）。
  - `MachineAccelerator`: 機械ヒットの位置と残り時間をメモリにだけ持ち、`WorldTickEvent` END で `update()` を余分に呼ぶ（Time in a Bottle と同じ方式）。例外はあえて捕まえない（ユーザーの判断。クラッシュレポートで機械を特定し、`excludedBlocks` に足してもらう）。
  - `ServerStats`: 統計。累計はプレイヤーの永続データ（`PlayerPersisted` の `weakspot`。死亡・ディメンション移動で引き継がれる）に、「今回」はメモリに持つ。節目と耐久回復は、画面のリセットでは消えない別の累計 `rewardHits` で数える（報酬を取り直せないように）。
  - `MiningRewards`: 耐久回復と節目の報酬。
  - `SettingsSync` / `MarkerRelay`: 設定の送信、弱点マークの転送（毎 tick、マークから `markerShareRange` 以内のプレイヤーを計算し直し、入った人に現在の状態、出た人に「消えた」を送る）。

### 重要: サーバー側のブーストは時間枠ではない

1.12.2 のサーバー（`PlayerInteractionManager#blockRemoving`）は進捗を積算せず、破壊完了の通知を受けた瞬間に「**現在の**破壊速度 × (経過tick+1) ≥ 0.7」で判定する。そのため、仕様どおりサーバー側でも時間枠の間だけ倍率を掛けると、判定の瞬間が枠外のときにブーストが効かない。そうなるとブロックが戻り、後から通常速度で壊れる。
そこでサーバーは、ヒットごとに追加進捗 `(倍率-1)×継続tick` を貯め、そのブロックの `BreakSpeed` に `1 + 追加tick/(経過tick+1)` を掛ける（`BoostMath`）。こうすると積算した場合と同じ結果になる。破壊速度まわりを変えるときは、クライアント（積算）とサーバー（瞬間判定）の両方で結果が一致するかを確認すること。

- ヒット間隔の制限は、クライアントが `minHitIntervalTicks`（成長・機械はそれぞれの設定）、サーバーはネットワークの揺らぎを見込んで 2 tick 甘くしている。サーバーがヒットを拒否してクライアントだけブーストされると、ブロックが一度戻って見える。
- ブーストの目安: H tick ごとにヒットすると、速度は通常の `1 + (倍率-1)×継続/H` 倍。初期値なら `1 + 12/H` で、約 0.6 秒ごとのヒットで2倍、`minHitIntervalTicks=6` で最大3倍。

### 設定

`@Config`（`config/weakspot.cfg`）。キー名を変えないように、カテゴリは分けず `general` に並べる。コメントの先頭に、どちらの値が使われるかを書く。
- `[サーバー]`: サーバーの値が正。クライアントが使うものは `SyncedSettings` に入れて送り、クライアントは接続中 `ClientSettings.get()` を読む。**受け取った値を `WeakSpotConfig` の static フィールドに書き込まない**（書き込むと `ConfigManager.sync` でサーバーの値がクライアントの `weakspot.cfg` に保存されてしまう）。サーバーだけが使う項目（報酬、成長の回数、機械の倍率など）は送らない。
- `[クライアント]`: 音と、他のプレイヤーのマークの表示だけ。`WeakSpotConfig` をそのまま読む。
- 設定を追加するときは、README の設定表の該当する方にも追加すること。`SyncedSettings` に項目を足すと通信内容が変わる。
- パケットの中身を変えたり、パケットを追加・削除したりすると、古いバージョンとは通信できなくなる。マイナーを上げ、`acceptableRemoteVersions` を書き換え、README の更新履歴にそのことを書くこと。統計の保存形式（NBT のキー）を古い版で読めないように変えるときも同じ。
