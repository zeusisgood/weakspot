# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

Fortnite の「弱点（クリティカル）」採掘を Minecraft に持ち込む Mod。対象は **Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860**。
仕様書はすべて `doc/` にある。仕様の正本は `doc/SPEC_v1.0.md`（MVP。数値・挙動・MVP 完了条件 §12・スコープ外 §13）と、その差分を定める `doc/SPEC_v1.1.md`（Mod 1.1.0。スコープ外は §14、実装時の確認事項は §12）、`doc/SPEC_v1.1.x.md`（各パッチ Mod 1.1.x の差分。内容は README の更新履歴を参照）、`doc/SPEC_v1.2.md`（Mod 1.2.0。マイナー。小さいブロックの弱点、設定の追加、動物・釣りの弱点、一時オフのサーバーへの通知。実装時の確認事項は §4）、`doc/SPEC_v1.2.1.md`（Mod 1.2.1。動物の弱点を体越しに薄く透かす）、`doc/SPEC_v1.2.2.md`（Mod 1.2.2。キノコを確率で巨大キノコに育てる）。v1.1 に書かれていないことは v1.0 と現行実装のまま、v1.1.1 以降のパッチの仕様に書かれていないことは、その前の版と現行実装のまま。仕様と食い違う実装が必要な場合は、リリースの流れの「止まる条件」に従い、push せずにユーザーに確認する。

## 開発環境・コマンド

- devcontainer で **JDK 8** を使う。ビルドは公式 MDK ベースの **ForgeGradle 3 + Gradle 4.9**（仕様書の FG 2.3 ではない）。Gradle 5 以降の構文は使えない（依存は `compile` / `testCompile`）。
- ビルド + テスト: `./gradlew build` → 成果物は `build/libs/weakspot-<version>.jar`（reobf 済み）
- テストのみ: `./gradlew test`、1クラスだけ: `./gradlew test --tests com.example.weakspot.common.WeakSpotPlacerTest`
- 専用サーバー起動: `./gradlew runServer`（作業ディレクトリは `run/`、`nogui` 付き。`run/eula.txt` は同意済み）。止めるときはコンソールで `stop`。
  - パイプで `stop` を流しても Gradle 経由では届かない。Claude が起動を確かめるときは `timeout 150 ./gradlew runServer > ログ` で起動し、ログの `Done (` と `run/config/weakspot.cfg` を確認する。
  - 起動ログの `module-info.class ... IllegalArgumentException`（`Unable to read a class file correctly`）は FG3 + 1.12 でいつも出るノイズで、無視してよい。`Missing English translation for weakspot: .../build/classes/java/main/assets/...` の WARN も、開発環境のリソースの置き場所によるいつものノイズ。
- クライアント確認: コンテナ内では画面を出せない。ビルドした jar をホスト側 Minecraft（Forge 1.12.2）の `mods` に入れて確認する。描画・ヒット判定・体感速度は Claude が検証できないので、ユーザーに確認を依頼する。
- Minecraft の非公開のフィールドは、アクセストランスフォーマーではなくリフレクションで読む。開発環境は MCP 名、実際の環境（reobf 後）は SRG 名なので、`getDeclaredField` で MCP 名 → SRG 名の順に試す（例: `client/MiningProgress`）。Forge の3引数の `ReflectionHelper.findField` は起動環境の判定で片方の名前しか試さず、非推奨でもあるので使わない。
  - SRG 名の調べ方: `~/.gradle/caches/forge_gradle/maven_downloader/de/oceanlabs/mcp/mcp_snapshot/20171003-1.12/mcp_snapshot-20171003-1.12.zip` の `fields.csv`（`methods.csv`）、または `~/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860/forge-1.12.2-14.23.5.2860-srg.jar` を `javap -p` で見る。
  - 画面を出さずに両方の環境で確かめるには、自分のクラスを小さなプログラムからリフレクションで呼ぶ（インスタンスは `Unsafe.allocateInstance`）。開発環境は `sourceSets.main.runtimeClasspath`（Gradle の init スクリプトで書き出す）、実際の環境はその中の `build/classes` `build/resources` と `..._mapped_snapshot_...` の jar を、`build/libs` の reobf 済み jar と上の `-srg.jar` に差し替えたクラスパスで動かす。
- Mod のバージョンは `build.gradle` の `version` と `WeakSpotMod.VERSION` の2か所にある。変えるときは両方を揃える。
- バージョンの方針（1.1.0 以降）:
  - 機能の追加・不具合の修正ごとに**パッチ**を上げる（1.1.0 → 1.1.1）。
  - 互換性を破るときは**マイナー**を上げる（1.1.x → 1.2.0）。迷ったらマイナー。互換性を破る変更とは、通信内容の変更（パケットの追加・削除・中身の変更）、古い版で読めなくなるサーバー保存データの形式変更、設定キーの削除や意味の変更。通信内容を変えたら必ずマイナーを上げる。
  - `@Mod` の `acceptableRemoteVersions` で、同じマイナー同士（例: `[1.1,1.2)`）なら接続できるようにする。マイナーを上げるときは、`build.gradle` と `WeakSpotMod.VERSION` に加えて、この範囲も新しいマイナーに書き換え、README の更新履歴に旧マイナーとは接続できないことを書く。
  - 現行は 1.2.2。範囲は `WeakSpotMod.ACCEPTED_VERSIONS = "[1.2,1.3)"`（Maven のバージョン範囲の書式。Forge の `VersionRange`）。
- リリースの流れ: README の「最新版」の行と「更新履歴」を更新 → コミット → 注釈付きタグ `vX.Y.Z` → `main` とタグを push。GitHub Release はユーザーが手動で作り、`build/libs/weakspot-X.Y.Z.jar` を添付する。
  - コミットの形: 仕様書を足す「Add the spec for X.Y.Z」→ 機能のコミット（1つ以上）→ バージョン・README・CLAUDE.md をまとめた「Release X.Y.Z: 〜」。タグのメッセージは「X.Y.Z: 〜」。リリースした jar は `build/release/` にも残す（ユーザーが試す版を取り出しやすくするため）。
  - 仕様書（`doc/SPEC_*.md`）にもとづく作業は、ユーザーの承認を待たずに、実装からタグと push まで進める。ただし、止まる条件（互換性を破る変更が必要、仕様の意図が読み取れない、ビルドやテストが通らない、runServer が起動しない）に当たったら、push せずに止まって報告する。GitHub Release の作成は、ユーザーが手動で行う。
  - マイナーを上げるときは、仕様書に書かれた互換性の変更（通信内容、`SyncedSettings`・`StatsMessage` の項目の追加など）は、止まる条件の「互換性を破る変更が必要」に当たらない。`ACCEPTED_VERSIONS` を新しいマイナーに書き換える。仕様書に書かれていない互換性の変更（特に、古い版で作ったワールドの保存データが読めなくなる変更。項目の追加で古いデータを読める形なら、よい）は、当たる。

## アーキテクチャ

クライアントとサーバーの**両方に Mod が必要**（1.1.0 からは同じマイナー同士なら接続できる。1.0.x とは接続できない。1.2.0 で通信内容が変わったので、1.1.x とも接続できない）。パッケージは `com.example.weakspot`。

弱点は3種類（`common/HitKind`）: **採掘**（左の長押し。対象は、壊せて、今の破壊速度で 1 tick に進む量 `getPlayerRelativeBlockHardness` が 1.0 未満のブロック。1.0 以上のブロックはバニラの `PlayerControllerMP.clickBlock` がクリックした瞬間に壊し、「掘っている」状態にも入らないので、弱点も耐久バーも出せない。素手の土などは対象）、**成長**（素手で右クリックを押しっぱなし。成長できる `IGrowable` と、サトウキビ・サボテン・ネザーウォート）、**機械**（しゃがんで両手が空のまま右クリックを押しっぱなし。`ITickable` の TE）。節目と耐久回復は採掘だけが対象。

- `common/`: Minecraft に依存しない純粋な計算（動物のタイマー `AnimalTimers`、釣り `FishingMath`、プレイヤーごとのオン・オフ `PlayerSwitches`、面の (u,v) 座標変換と一番大きい面、弱点の配置と最小半径、ブースト量、連続ヒット数 `HitStreak`、ヒット音の音階 `HitPitch`、コンボの表示の計算 `ComboTier` / `ComboMilestones` / `ComboDisplay`、統計 `MiningStats`、節目 `Milestones`、耐久回復の精算 `RepairSettlement`、機械の加速 `MachineBoost`、マークの送信頻度 `MarkerSendPolicy`、色 `MarkerColor`、`IGrowable` でない植物の育てる余地 `GrowthRoom`、マーカーの移動と残像 `MarkerMotion`、耐久バーの形 `BlockHealthBar`）。単体テストはここだけにある。1.7.10 への移植を見込んで、MC クラスを持ち込まない。
- `RightClickTargets`（両側）: 右クリックの弱点の対象判定。クライアントとサーバーで同じ条件（同期した設定）を使う。
  - 成長の対象（`isGrowable`）: 設定 `growthExcludedBlocks`（除外リスト。草ブロック・草などは初期値で外す。キノコは 1.2.2 から対象）になく、`canGrow` が true の `IGrowable`、または 設定 `growthExtraBlocks`（追加リスト。1.2.0 から設定。サーバーの値を同期する）にあり、`RightClickTargets.EXTRA_GROWTH_BLOCKS`（育つ条件をコードで決めてあるブロックの表。サトウキビ・サボテン・ネザーウォートだけ）にもある植物で育てる余地があるもの。追加リストに他のブロックを足しても、弱点が出ないだけでエラーにはしない。除外リストは追加リストにも効く。
  - 追加リストの植物: サトウキビ・サボテンは柱の高さ < 3 で一番上のすぐ上が空気（高さ1のサトウキビは土台 `BlockReed#canBlockStay` も）、ネザーウォートは段階 < 3。バニラの `updateTick` の条件と同じ。育つのは柱の一番上の節だけなので、サーバーの効果は `growthTarget` で柱の一番上にかける。対象を条件どおりに右クリックしたら、`RightClickBlock` をメインハンドで SUCCESS にしてキャンセルし、通常動作（GUI、オフハンドの設置など）を止める。クライアントでキャンセルしてもバニラは右クリックのパケットを送るので、サーバーでも発火し、そこで「直前に右クリックした」ことを記録する。右クリックを押しっぱなしにすると、バニラは 4 tick ごとに右クリックする。
- `client/`（`@EventBusSubscriber(value = Side.CLIENT)`。専用サーバーではロードされない）: 弱点の状態、ヒット判定、描画、ヒット音、クライアント側のブースト。弱点は一度に1つ（`ClientWeakSpotHandler.spot`）。
  - ヒット判定は `RenderWorldLastEvent` で**毎フレーム**行う（tick 単位だと素早い照準移動を取りこぼす）。右クリックの押しっぱなしは `keyBindUseItem.isKeyDown()` で見る。成長の弱点は一番大きい面（多くは上面）に出し、照準がその面に当たっているときだけヒットにする。同じ大きさの側面が複数あるとき（サトウキビなど）は照準の側面を選び、今の面が見えている間は変えない（`FaceMath.growthFaceAxis`）。育って当たり判定の箱が変わったら出し直す。
  - ブーストの時間枠は `ClientTickEvent` START で増える `clientTick` で数える。`PlayerControllerMP` は tick ごとに進捗を積算するので、枠内の tick だけ倍率を掛ければよい。
  - ヒット音（`HitSounds`）: 楽器と音量は各自の設定（`myHitSound` / `myHitVolume`、`othersHitSound` / `othersHitVolume`）。どれも「プレイヤー」のカテゴリ。自分の音と試聴は距離なし（`AttenuationType.NONE`）、他のプレイヤーの音はブロックの位置から。連続ヒット数（`common/HitStreak`。`ClientWeakSpotHandler.STREAK`。種類・ブロックをまたいで続き、40 tick ヒットがないと途切れる。死亡・リスポーン・ディメンション移動・ワールドを出たときも 0 に戻る。数は戻らずに上がり続ける）に応じて長音階を上がり、1オクターブで最初に戻る（`HitPitch` が数から求める）。
  - コンボの表示（`ComboHud`）: 同じ連続ヒット数を HUD（`RenderGameOverlayEvent.Post` の `ALL`。F1 で隠しているときは描かない）に出す。2 以上で「12 HIT」、ヒットで弾む、色の段階（10 / 25 / 50 / 100。`ComboTier`）、途切れるまでの残り時間のバー。途切れたら薄くして消し、5 以上なら「MAX n」を 20 tick 残す。10 / 25 / 50 / 100 に達した瞬間に強調音（自分のヒット音の楽器の最高音）と光（`ComboMilestones`）。時間は `clientTick`（一時停止中は止まる）。設定は `[クライアント]` の `comboDisplayEnabled` / `comboScale` / `comboPosition` / `comboMilestoneEffects`。サーバーには何も送らない（統計の「最大連続ヒット数」は通信が変わるので 1.2.0）。
  - 統計画面（`StatsScreen`、K キー）: 「統計」タブはサーバーから届いた数字を表示するだけ（開いたとき・設定画面から戻ったときに要求）。「サウンド」タブは楽器・音量・試聴で、`WeakSpotConfig.save()`（`ConfigManager.sync`）で `weakspot.cfg` に保存する。「設定画面を開く」は `WeakSpotGuiFactory.create`（Mods メニューと同じ `GuiConfig`。他人のサーバーに接続中は2行目に注意書き）。
  - 他のプレイヤーのマーク（`OtherMarkers`）: 自分の採掘の弱点が出た・動いた・消えたときに送り（`markerSendMinIntervalTicks` で間引き、出ている間は 20 tick ごとに送り直す）、届いたマークは 60 tick 更新がなければ消す。描画は `WeakSpotRenderer` で、色と濃さは各自の設定。同じブロックの同じ面で位置が変わったマークは、前の `WeakSpot` を使い回して、最後に知っている位置から動かす（同じ位置の送り直しは動かさない）。
  - マーカーの表示位置と当たり判定の位置は別（1.1.4）。`WeakSpot.u` / `v` は当たり判定の位置で、ヒットの瞬間に移動先へ変わる。表示位置は `WeakSpot.motion`（`common/MarkerMotion`）で、実時間（`Minecraft.getSystemTime()`）の 80 ms で ease-out に動き、通った道に残像を 4 個（150 ms で消える）残す。移動中の次のヒットは、前の移動先から動き直す。出し直し（新しい弱点、別の面・ブロック）はその場で切り替える。設定 `weakSpotTrailEnabled`（`[クライアント]`）でオフ。`WeakSpotRenderer` は表示位置に描き、送信（`OtherMarkers.sendOwn`）とヒット判定は当たり判定の位置を使う。
  - 耐久バー（1.1.5）: 自分が掘っているブロックの残りの耐久（1 − 破壊の進み具合）を、採掘の弱点と同じ面の「下」の余白（下の辺から中心 0.05、太さ 0.06、面の幅の 80%。実際の当たり判定の箱の面）に、緑 `#3DDC84` と半透明の黒の背景で描く。側面は下の辺、上面・下面はプレイヤーに一番近い辺が「下」。左から伸び、プレイヤーの右手の側から縮む（形の計算は `common/BlockHealthBar`）。`WeakSpotRenderer` がマーカーより先に（下に）描く。出すのは、採掘の弱点がこのフレームの照準の面に出ていて、`PlayerControllerMP` が今そのブロックを掘っているときだけ（長押しをやめる・壊れると `getIsHittingBlock()` が false になり、すぐ消える）。設定 `blockHealthBarEnabled`（`[クライアント]`）。
    - 破壊の進み具合と掘っているブロックは、`PlayerControllerMP` の非公開のフィールドをリフレクションで読む（`client/MiningProgress`）。開発環境の MCP 名（`curBlockDamageMP` / `currentBlock`）と実際の環境の SRG 名（`field_78770_f` / `field_178895_c`）を順に試す。Forge の `ReflectionHelper.findField` の3引数の版は起動環境の判定で片方しか試さないので使わない。見つからなければ警告を1回出してバーを出さない。進み具合は tick ごとに積まれるので、`ClientTickEvent` START（その tick の進捗の前）で前の値を覚え、フレームごとに補間する。
  - 成長バー（1.1.6、`GrowthBar`）: 成長の弱点が今出ている作物の足元に、黄 `#FFD23F` のバーを描く。値は名前が `age` の整数プロパティ（なければ `stage`）÷ 最大。柱（サトウキビ・サボテン）は `growthTarget` の一番上の節の年齢。**描き方は汎用の部品 `WorldBar.draw`（中心のワールド座標・幅・太さ・値・色を渡すと、プレイヤーの方を向いた水平のバーを、左から伸ばして描く。深度テストは切る）。動物の足元のバー（1.2.0 の `AnimalBar`）が、これを流用している**。設定 `growthBarEnabled`。
  - 他のプレイヤーのマークの形（1.1.6）: `common/MarkerShape`（CIRCLE / RING / DIAMOND / SQUARE。頂点数・回転・半径の倍率を持つ）。設定 `otherMarkerShape`（初期値 RING）。`WeakSpotRenderer.drawMarker` が形を受け取る。自分のマークは CIRCLE 固定。
  - 弱点の一時オフ（1.1.6、`ToggleKeyHandler`、J キー、設定 `weakSpotsEnabled`）。オフの間に止める処理の一覧: ①弱点の表示・ヒット判定（`onRenderWorldLast` で `updateAim` を呼ばない）と通知（ヒットが起きないので `HitMessage` も出ない）、②自分のマークの送信（`spot` が null になり、`OtherMarkers.sendOwn` が「消えた」を送る）、③ブースト（`onBreakSpeed`、`boostPos`）、④耐久バー・成長バー（`onRenderWorldLast`）、⑤クライアント側の右クリックの抑止（`RightClickTargets.onRightClickBlock` が `world.isRemote` のときに返る）。まとめて `ClientWeakSpotHandler.stopOwnWeakSpots`。1.2.0 から、オン・オフはサーバーにも伝わる（`SwitchMessage`、C→S。ログイン時と、`weakSpotsEnabled` が変わったときに `ToggleKeyHandler.syncToServer` が毎 tick 比べて送る。設定画面での変更も拾う）。サーバーは `ServerSwitches`（`common/PlayerSwitches`。届く前はオン）で、オフのプレイヤーについて、右クリックの抑止（`RightClickTargets`・`AnimalTargets`）、釣りの左クリックの抑止、ヒットの受け付け（採掘・成長・機械・動物・釣り）、ブースト、マークの転送（オフにした瞬間に `MarkerRelay.onMarker(player, null)`）を止める。1.1.6 にあった「オフでも機械の GUI が開かない」制限は解消済み。
  - 節目の演出（`MilestoneEffects`）: タイトル、チャット1行、花火、音階の駆け上がり。777 は虹色で派手にする。
  - 画面の文字列は `assets/weakspot/lang/en_us.lang` と `ja_jp.lang` の両方に追加すること。
  - キーバインドの登録は `@SidedProxy`（`CommonProxy` / `client.ClientProxy`）の `init` で行う。
  - 小さい面の配置（1.2.0）: `WeakSpotPlacer.layout` が、面の大きさに合わせて半径（下限 `weakSpotMinRadius`、上限は短い辺 × `weakSpotMaxRadiusRatio`。食い違うときは上限を優先）、縁の余白（min(`edgeMargin`, 短い辺 × 0.12)）、最小移動距離（min(`minMoveDistance`, 動ける範囲の対角線 × 0.6)）を決める。短い辺が `minFaceSize` 未満の面には出さない（`WeakSpot.spawn` などが null を返し、`spot` が null になるので、ヒット通知もマークの送信も起きない）。成長の弱点の下限は max(`growthMinRadius`, `weakSpotMinRadius`)。採掘・成長・機械・動物・他のプレイヤーのマークで、同じ計算を使う。
  - 動物の弱点（1.2.0、`HitKind.ANIMAL`）: 対象・素手・しゃがみの判定は両側で同じ `AnimalTargets`（`isTarget`。両手が空。しゃがみが要るのは `AbstractHorse`・飼いならした `EntityTameable`・サドルの豚・村人・`animalSneakRequiredEntities`）。動物のタイマーはクライアントに届いていないので（`getGrowingAge` は ±1、卵のタイマー・村人の取引は同期されない）、押しっぱなしで動物に照準を合わせている間、`AnimalStates.query`（`AnimalQueryMessage` C→S、5 tick ごと。ヒットのたびに即）で問い合わせ、`AnimalStateMessage`（S→C。動いているタイマーの mask と進み具合）が返るまで弱点を出さない。弱点は `WeakSpot.spawnOnEntity`（(u, v) は面の左下の角からの位置。`follow` で、その tick の箱（当たり判定）と描く時点の箱（`renderBox`）に面の位置を合わせる）。足元のバーは `AnimalBar`（`WorldBar` を流用）。自分の動物の弱点は、体（模型が箱より外に出るニワトリなど）に隠れた部分も、深度テストを切って濃さ 0.35 倍で重ねて描く（1.2.1、`WeakSpotRenderer.seeThrough`、設定 `animalSpotSeeThrough`。他のプレイヤーのマークは透かさない）。他のプレイヤーへは `MarkerData.entityId`（-1 ならブロック）で送る。バニラの動作の抑止は `AnimalTargets` の `EntityInteract` / `EntityInteractSpecific`（HIGH）で、サーバーは自分でタイマーを調べて止め、クライアントは `AnimalStates` の返事を覚えているときだけ止める（クライアントが止めなくても、パケットはサーバーへ届き、サーバーが止める。最初の右クリックは返事の前なので、この経路になる）。
  - 動物の効果（サーバー、`server/AnimalHits`）: 子ども = `ageUp` / `setGrowingAge`、繁殖 = `setGrowingAge`、卵 = `EntityChicken#timeUntilNextEgg`（public。0 以下になったら 1 にして、バニラの次の tick の産卵に任せる）、羊毛 = `setSheared(false)`（羊ごとのヒット数は `WeakHashMap`）、取引 = `server/VillagerTrades`（非公開の `buyingList` と `populateBuyingList` をリフレクション。ロックは `MerchantRecipe#isRecipeDisabled`、上限の増え方はバニラの補充と同じ `increaseMaxTradeUses(rand(6)+rand(6)+2)`、新しい段階は `villagerResetUnlocksNewTier` のときだけ）。読めないときは、その対象が出ないだけ。
  - 非公開のメンバーの読み書きは `Reflect`（MCP 名 → SRG 名の順に試す。見つからなければ警告を1回出して null）。`VillagerTrades`（`buyingList`、`populateBuyingList`）と `FishingHits`（`ticksCaughtDelay`、`ticksCatchableDelay`、`ticksCatchable`、`currentState`）が使う。開発環境と reobf 後の両方で解決することは、小さいプログラムで確かめた（1.2.0）。
  - 釣りの弱点（1.2.0、`HitKind.FISHING`、`client/FishingSpot` と `server/FishingHits`）: 浮きの待ち時間の段階は、サーバーが「`ticksCaughtDelay` > 0 で、あとの2つが 0、浮きが水に浮いている（`BOBBING`）」で判定し、`FishingQueryMessage`（C→S）/ `FishingStateMessage`（S→C）で返す。ヒットは `ticksCaughtDelay` を減らす（0 にすると、バニラが待ち時間を引き直すので、最小 1）。待ち時間の始まりの長さは、毎 tick 見て覚える。クライアントは、浮きからの水平の差 (dx, dz) を弱点の位置とし、`RenderWorldLastEvent` の OpenGL の行列（モデルビュー、射影、ビューポート）で画面上の位置に変換し（視野角は射影行列の [1][1] から）、HUD（`RenderGameOverlayEvent.Post`）に画面上で一定の大きさで描く。当たり判定は `common/FishingMath.allowedAngle`（円の半径・視野角・画面の高さ）。左クリックは `MouseEvent`（HIGHEST）をキャンセルして止める（攻撃のキーが押された扱いにならない）。サーバーも、ヒット直後の `LeftClickBlock` / `AttackEntityEvent` を念のため止める。
  - 統計（1.2.0）: `maxStreak`（`ServerStats.countStreak`。受け付けたヒットのすべてで `HitStreak` をサーバーの tick で数える）、`animalHits`、`fishingHits`。保存は `PlayerPersisted` の `weakspot` の新しいキー（古い版は無視する）。
- `network/`（`WeakSpotMod.preInit` で登録。番号は登録順）: パケットのハンドラーは専用サーバーでもインスタンス化されるので、クライアント行きのパケットは、クライアントのクラスに `proxy.onXxx` 経由でアクセスする。
  - `HitMessage`（C→S）: ヒット通知（種類、BlockPos、連続ヒット数、動物のエンティティ ID）。
  - `OtherHitMessage`（S→C）: 受け付けた採掘ヒットを 16 ブロック以内の他のプレイヤーに転送する（ヒット音用）。
  - `SettingsMessage`（S→C）: サーバーの設定値（`SyncedSettings`）。ログイン時と、ホストが設定を変えたとき。
  - `StatsRequestMessage`（C→S、リセットの指示を含む）/ `StatsMessage`（S→C）: 統計。
  - `MilestoneMessage`（S→C）: 達成した節目。
  - `MarkerMessage`（C→S）/ `OtherMarkerMessage`（S→C）: 採掘と動物の弱点マーク（`MarkerData.entityId`）。サーバーは検証せずに転送する（オフのプレイヤーのものは捨てる）。
  - `SwitchMessage`（C→S、8）/ `AnimalQueryMessage`（C→S、9）/ `AnimalStateMessage`（S→C、10）/ `FishingQueryMessage`（C→S、11）/ `FishingStateMessage`（S→C、12）: 1.2.0 で足した。
- `server/`（論理サーバー）:
  - `WeakSpotCommand`（1.1.6）: `/weakspot [stats|reset] <プレイヤー>`（権限レベル 2、オンラインのプレイヤーだけ）。`WeakSpotMod.serverStarting` で登録。表示は翻訳キー。`reset` は `ServerStats.resetTotal`。
  - `ServerBoostTracker`: `LeftClickBlock` で「今どのブロックを破壊中か」と開始 tick、弱点が出るブロックか（一瞬で壊れないか）を記録する。採掘ヒットはそのブロックと一致したときだけ受け付ける。「壊した」は `BreakEvent`（LOWEST、キャンセルされていないもの）で数える。アクセストランスフォーマーは使っていない。
  - `RightClickHits`: 成長・機械ヒットの検証（直前 10 tick 以内にそのブロックを右クリックしたか、届く距離か、間隔、対象か）と効果（成長は `randomTick` を余分に呼ぶ。サトウキビ・サボテンは柱の一番上の節に。キノコ `BlockMushroom` は `randomTick` では広がるだけなので、代わりに確率 `mushroomGrowChance` で骨粉と同じ `grow` を呼ぶ）。
  - `MachineAccelerator`: 機械ヒットの位置と残り時間をメモリにだけ持ち、`WorldTickEvent` END で `update()` を余分に呼ぶ（Time in a Bottle と同じ方式）。例外はあえて捕まえない（ユーザーの判断。クラッシュレポートで機械を特定し、`excludedBlocks` に足してもらう）。
  - `ServerStats`: 統計。累計はプレイヤーの永続データ（`PlayerPersisted` の `weakspot`。死亡・ディメンション移動で引き継がれる）に、「今回」はメモリに持つ。節目は、画面のリセットでは消えない別の累計 `rewardHits` で数える（報酬を取り直せないように）。
  - `MiningRewards`: 耐久回復と節目の報酬。節目はヒットごと（`rewardHits`）に判定する。耐久回復はヒットのときではなく、ブロックを壊したときに精算する（`common/RepairSettlement`）。
    - 採掘ヒットは `ServerBoostTracker.Mining.hits` に未確定として持つ。`BreakEvent`（LOWEST、キャンセルされていないもの）でそのブロックと一致したときだけ確定し、`MiningRewards.onBlockBroken` に渡す。長押しをやめた（ABORT には Forge のイベントがない）・別のブロックに移ったときは、次の `LeftClickBlock` で `Mining` が作り直されるので、未確定のヒットは捨てられる。
    - `BreakEvent` はツールの耐久が減る前（`tryHarvestBlock` の先頭）に来るので、回復してから壊れる（耐久が残り1でもツールが残る）。
    - 回復の数え方の余りはメモリだけに持つ（ログアウトで 0）。1回の破壊での回復は `maxRepairPerBreak`（サーバーだけが使う。送らない）まで。
  - `SettingsSync` / `MarkerRelay`: 設定の送信、弱点マークの転送（毎 tick、マークから `markerShareRange` 以内のプレイヤーを計算し直し、入った人に現在の状態、出た人に「消えた」を送る）。

### 重要: サーバー側のブーストは時間枠ではない

1.12.2 のサーバー（`PlayerInteractionManager#blockRemoving`）は進捗を積算せず、破壊完了の通知を受けた瞬間に「**現在の**破壊速度 × (経過tick+1) ≥ 0.7」で判定する。そのため、仕様どおりサーバー側でも時間枠の間だけ倍率を掛けると、判定の瞬間が枠外のときにブーストが効かない。そうなるとブロックが戻り、後から通常速度で壊れる。
そこでサーバーは、ヒットごとに追加進捗 `(倍率-1)×継続tick` を貯め、そのブロックの `BreakSpeed` に `1 + 追加tick/(経過tick+1)` を掛ける（`BoostMath`）。こうすると積算した場合と同じ結果になる。破壊速度まわりを変えるときは、クライアント（積算）とサーバー（瞬間判定）の両方で結果が一致するかを確認すること。

- ヒット間隔の制限は、クライアントが `minHitIntervalTicks`（成長・機械はそれぞれの設定）、サーバーはネットワークの揺らぎを見込んで 2 tick 甘くしている。サーバーがヒットを拒否してクライアントだけブーストされると、ブロックが一度戻って見える。
- ブーストの目安: H tick ごとにヒットすると、速度は通常の `1 + (倍率-1)×継続/H` 倍。初期値なら `1 + 12/H` で、約 0.6 秒ごとのヒットで2倍、`minHitIntervalTicks=6` で最大3倍。

### 設定

`@Config`（`config/weakspot.cfg`）。キー名を変えないように、カテゴリは分けず `general` に並べる。コメントの先頭に、どちらの値が使われるかを書く。
- `[サーバー]`: サーバーの値が正。1.2.0 で `weakSpotMinRadius` / `weakSpotMaxRadiusRatio` / `minFaceSize` / `growthExtraBlocks` / 動物と釣りの項目を足した（`villagerResetUnlocksNewTier` だけは、サーバーだけが使うので同期しない）。クライアントが使うものは `SyncedSettings` に入れて送り、クライアントは接続中 `ClientSettings.get()` を読む。**受け取った値を `WeakSpotConfig` の static フィールドに書き込まない**（書き込むと `ConfigManager.sync` でサーバーの値がクライアントの `weakspot.cfg` に保存されてしまう）。サーバーだけが使う項目（報酬、成長の回数、機械の倍率など）は送らない。
- 設定ファイルの移行（1.2.2）: `WeakSpotConfig.migrate` を `serverStarting` で呼び、`configVersion`（初期値 0）が古ければ1回だけ直して保存する（1: `growthExcludedBlocks` からキノコを取り除く）。`preInit` の間の `ConfigManager.sync` は「読み込み」（ファイルの値でフィールドを上書きする）になるので、そこでは保存できない。
- `[クライアント]`: 音と、他のプレイヤーのマークの表示と、コンボの表示と、弱点の移動の演出と、耐久バーと、動物の弱点の透かしだけ（見た目と音だけに関わるもの）。`WeakSpotConfig` をそのまま読む。
- 設定を追加するときは、README の設定表の該当する方にも追加すること。設定画面の説明は `weakspot.general.<キーを小文字にしたもの>.tooltip` を `en_us.lang` と `ja_jp.lang` の両方に足す（1.1.3 以降の `[クライアント]` の項目。それより前の項目は `@Config.Comment` の日本語のまま）。`SyncedSettings` に項目を足すと通信内容が変わる。
- パケットの中身を変えたり、パケットを追加・削除したりすると、古いバージョンとは通信できなくなる。マイナーを上げ、`acceptableRemoteVersions` を書き換え、README の更新履歴にそのことを書くこと。統計の保存形式（NBT のキー）を古い版で読めないように変えるときも同じ。
