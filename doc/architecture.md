# 仕組み（開発者向け）

[← README に戻る](../README.md)

コードの地図と、コードを読んでも分からないこと（理由・バニラを調べた結果・互換性）だけを書きます。処理の中身（How）はコードとそのコメントを、遊び方は [play.md](play.md)、設定の一覧は [config.md](config.md)、約束事は [../CLAUDE.md](../CLAUDE.md) を見てください。仕組みを変えたら、ここも直します。


## 全体

- ディレクトリ: `common/`（リポジトリ直下）は Minecraft に依存しない純粋な計算で、各版のビルドがソースで読む（Gradle 4.9 と移植先の Gradle 8 は 1 つのマルチプロジェクトにできないため）。翻訳の正本は `lang/*.json`、1.12.2 のビルドは `mc1.12.2/`（`.lang` は `generateLang` が作る）、1.20.1・1.21.1 のビルドは `mc1.20.1/`・`mc1.21.1/`（json をそのまま使う）。以下のパッケージ名は `io.github.zeusisgood.weakspot` からの相対。クラスの名前と分け方はどの版でも同じにしてある（読み比べやすくするため）。版で違う所は、下の「1.20.1 版・1.21.1 版」にまとめる。
- `common/`: Minecraft に依存しない純粋な計算。クライアントの部品でも Minecraft のクラスを使わないもの（`TimedBoost`・`QueryThrottle`）と、設定の値の型・表（`HitSound`・`ComboPosition`・`SettingGroups`）もここ。テストは各版のビルドで流す（同じテストが 2 回流れるのは、両方の JDK で通ることの確認になるので、そのままにする）。
- パッケージの直下（両側）: クライアントとサーバーが同じ条件（同期した設定）で判定するもの。対象の判定 `RightClickTargets`（成長・機械・収穫）・`AnimalTargets`・`VehicleTargets`・`MeleeTargets`、手 `HeldItems`、プレイヤー `PlayerRules`、使用時間と溜め `UseTimeCut`（`BowDraw`・`EatDraw`）・`HeldCharge`（`MeleeCharge`・`ThrowCharge`）、落下の見込み `FallDamage`、ガイドの本 `GuideBook`、的 `ItemTarget`、非公開のメンバー `Reflect`。
- `client/`: 弱点の状態・ヒット判定・描画・音・クライアント側のブースト。
- `server/`（論理サーバー）: ヒットの検証と効果・統計・節目・順位。
- `network/`: パケット（下の「通信」）。
- `config/`: 設定（下の「設定」）と、送る設定 `SyncedSettings`。

### ブロックの弱点の当たりと置き場所

- 採掘・成長・機械・収穫の弱点（`client/WeakSpot` のエンティティでないもの）は、外枠の箱（1.12.2 の `getSelectedBoundingBox`、1.20.1・1.21.1 の `getShape(...).bounds()`）の面に描き、当たりと面も、実際に当たった点・面ではなく、外枠の箱と照準の線（目から視線の向き）で決める（`ClientWeakSpotHandler.blockAim`・`common/FaceMath.rayEntryFace` / `rayPlane`）。凹凸のあるブロック（Mekanism の機械など）では、照準のわずかな動きで当たる面が変わり、採掘の弱点が出し直されて飛び回り、右クリックの弱点は面が合わず外れるため。線が箱に入らない（目が箱の中など）ときは、バニラの照準の面と点に戻す。ブロックへの照準そのもの（バニラがそのブロックを指しているか）は見るので、隣のブロックを狙って当たることはない。サーバーは面・点を見ていないので、クライアントだけの変更で済む。
- 置く・移る場所は、面の真正面の隣のブロックの形を面に写した長方形を避ける（`common/WeakSpotPlacer.place` の `blocked`。1.20.1・1.21.1 は `getShape(...).toAabbs()`、1.12.2 は当たり判定の箱、なければ選択の枠。パイプの本体と腕を別々に避けるため、箱ごとに分ける）。パイプの後ろの弱点は、バニラの照準が手前のパイプを指すので当てられないため。パイプ越しでも当たりにはしない（採掘ではパイプを掘ってしまい、右クリックはパイプへ行く。サーバーの検証も変える必要がある）。空気・1 マスの立方体（面が見えない）は避けない。全部の候補が重なるときは、避けずに置く。ほかのプレイヤーから届いたマークは、届いた位置のまま描く。

### 右クリックの弱点（成長・機械・収穫）

- 対象の判定は `RightClickTargets.classify`（機械 → 成長 → 収穫 → 自動の成長の順）。成長は `IGrowable` の `canGrow` と、追加リスト `growthExtraBlocks`（`common/BlockFilter` / `GrowthFilters`。状態の条件はプロパティの名前と値の名前で見るので、IC2 などのクラスに依存しない）。柱の植物（`COLUMN_BLOCKS`）はバニラの `updateTick` と同じ条件をコードで見る。機械は `ITickable` の TE と、予約された tick で動くレッドストーンの部品（`isScheduledMachine`）。
- 自動の成長（`isAutoGrowable`）: `getTickRandomly()` のブロックを、しゃがんでいるときだけ（Random Things の Time in a Bottle と同じ考え方。通常の右クリックを横取りしないため）。追加リストのブロックと `IGrowable` は自動の対象にしない（書いた条件を満たさないとき・育ちきったときに、拾い直さないため）。`randomTickSpeed` が 0 以下なら効果をかけない（IC2 の `BlockFoam.randomTick` がそのとき例外を投げる）。
- 対象を右クリックしたら、`RightClickBlock` をメインハンドで SUCCESS にしてキャンセルし、通常動作（GUI・オフハンドの設置）を止める。クライアントでキャンセルしてもバニラは右クリックのパケットを送るので、サーバーでも発火し、そこで「直前に右クリックした」ことを記録する。押しっぱなしでバニラは 4 tick ごとに右クリックする。
- 検証と効果は `server/RightClickHits`（成長は `randomTick` を余分に呼ぶ。キノコは `randomTick` では広がるだけなので、確率で `grow`）。育たないときの知らせは `server/GrowthWarnings`・`common/GrowthStuck`。収穫は `server/HarvestHits`・`common/HarvestBonus`（`BreakEvent` で保護を確かめてから）。
- 機械の加速は `server/MachineAccelerator`（Time in a Bottle と同じく `update()` を余分に呼ぶ。例外はあえて捕まえない: クラッシュレポートで機械を特定し、`excludedBlocks` に足してもらう。ユーザーの判断）。レッドストーンの部品は `server/ScheduledTicks`・`common/ScheduledBoost`（予約の前倒し。`getPendingBlockUpdates` は X と Z だけで絞るので、同じ列のほかの予約は元の時刻のまま入れ直す。ディスペンサーの余分の発射は非公開の `dispense`）。
- 「手が空いている」は、空か右クリックで何もしないアイテム（`HeldItems.isFree`。クラスが右クリックの 4 つのメソッドを上書きしていないか）。動物では、さらにバニラの動物の側が反応する物（`isBreedingItem`・オオカミの骨・馬の小麦と砂糖）を外す。右クリックで動くアイテムは版ごとに違う（1.13 から斧で皮をはぐ、1.14 から鉄でゴーレムを直す、など）。

### 採掘

- 対象: 壊せて、今の破壊速度で 1 tick に進む量（`getPlayerRelativeBlockHardness`）が 1.0 未満のブロック。1.0 以上はバニラの `PlayerControllerMP.clickBlock` がクリックした瞬間に壊し、「掘っている」状態にも入らない。このブロックは掘る前の弱点だけを出し、掘る前に当てていれば、統計の「壊したブロック数」と耐久回復に数える（予約の確定は `LeftClickBlock` で、バニラが壊すより先）。
- クライアント: `ClientWeakSpotHandler`（照準と出し直し）・`MiningBoost`（ブースト・掘る前のヒット・次を掘るまでの待ち）・`WeakSpotRenderer`。サーバー: `ServerBoostTracker`（下の「重要」の節）・`MiningRewards`（耐久回復と節目）。
- 掘る前のヒット: 照準のブロックに薄く出し、左クリックを押している間だけ当たる（照準を合わせるだけで当たると、ブロックを交互に見るだけでコンボが増え続けるため）。サーバーは予約にして、掘り始めた瞬間に確定する（`ServerBoostTracker.RESERVED`。掘らずにコンボを稼げないように）。
- 次を掘るまでの待ち: 当てたブロックを壊したら、`PlayerControllerMP` の非公開の `blockHitDelay`（バニラは 5）を 0 にする。サーバーはこの待ちを確かめていない。
- 耐久回復はブロックを壊したときに精算する（`common/RepairSettlement`）。`BreakEvent` はツールの耐久が減る前に来るので、耐久が残り 1 でもツールが残る。
- 耐久バー（`common/BlockHealthBar`）の進み具合は、`client/MiningProgress` が `PlayerControllerMP` の非公開のフィールドを読む。

### 照準のまわり・画面の上の弱点

- 照準のまわり（乗り物・食事・弓・はしご・走り・エリトラ・投げる物・近接・ゲート・泳ぎ・落下）: 土台は `AimSpotKind` / `AimSpots`、描き方と当たり判定は `HudSpot`・`ScreenProjection`・`FishingMath.allowedAngle`、向きの計算は `common/BowMath`。種類ごとのクラスは条件・出し方・効果・ゲージだけを書く。
  - 動いている間に出す種類（走り・はしご・泳ぎ・エリトラ・乗り物）は、条件が一瞬外れても 10 tick 残す（サーバーの `MoveHits` の「直前 10 tick」と同じ。バニラは壁にこすれた瞬間に走りを一度切るので、2 マスの通路で弱点がちらついていた）。残している間は当たらない（走っていないときの加速を防ぐため）。遊び方として弱点を止めるとき（`allowed` が外れる。一時オフ・種類のオフなど）は、残さずにすぐ消す。
  - 近接は左右だけ（走りの上下と見分けるため）。pitch は ±60 度で止め、yaw のずれを 1 / cos で広げる（真上・真下では yaw をずらしても照準の中央になり、当て放題だった）。
  - 照準が水平から大きく上下を向いていたら、水平に戻る側に出す（当て続けても真上・真下まで行かないように）。
  - 移動の加速: はしご・泳ぎ・ボートはクライアントが `move` で足す（泳ぎは 1.12.2 の移動速度の値が水中に効かないため、ボートは乗っている人のクライアントが動きを決めるため）。馬・豚・走りはサーバーの `server/SpeedModifier`。トロッコはサーバーで `onUpdate` を余分に呼ぶ（レールの最高速度は `BlockRailBase` が決めていて上げられないため）。
- 画面の上のマーカー（睡眠・エンチャント）: 土台は `ScreenSpotKind` / `ScreenSpots`。`DrawScreenEvent.Post` は、コンテナ画面（エンチャント台）ではアイテム用のライティングが有効のまま来るので、入口で切る（切らないと円と文字が暗くなる）。
- HUD の図形: `HudSpot.beginOverlay` / `endOverlay`（`ScreenProjection.fill` の頂点の並びが、GUI では裏向きの面になるため）。
- 釣り（`client/FishingSpot`・`server/FishingHits`）: 浮きの待ち時間は非公開のフィールド（`ticksCaughtDelay` など）。浮きのブロックが水で液体の高さ > 0 のときだけ待っているとみなす（浮きは一度水に入ると `BOBBING` のまま戻らないので、縁に引っかかると、減らしても魚が来ない）。
- 弓（`BowSpot`・`BowHits`・`BowDraw`）: 残り時間は `LivingEntityUseItemEvent.Tick` の `setDuration` で減らす。放つときは Stop が ArrowLoose より先に来るので、Stop では消さない。過剰チャージは `ArrowLooseEvent` で倍率を覚え、同じ tick の `EntityJoinWorldEvent` の矢に掛ける。
- 近接（`MeleeSpot`・`MeleeHits`・`MeleeCharge`）: 溜めを使う攻撃はクリティカルにしない（バニラの剣の薙ぎ払いは「クリティカルでない」ことが条件のため）。代わりに、その tick の同じプレイヤーのダメージ（正面と薙ぎ払いの周り）に `LivingHurtEvent` で倍率を掛ける。
- 投げる物（`ThrowSpot`・`ThrowHits`）: 溜めは `RightClickItem` で使い、同じ tick の `EntityJoinWorldEvent` の `EntityThrowable` の `motion` に掛ける（追跡の前なので見た目も同じ）。
- ゲート（`PortalSpot`・`PortalHits`）: 非公開の `portalCounter` を進める。エンチャント（`EnchantSpot`・`EnchantHits`）: 非公開の `xpSeed` を新しい乱数にして候補を引き直す。睡眠（`SleepSpot`・`SleepHits`）: `setWorldTime` で進め、すぐに `SPacketTimeUpdate` を送る。ゲートとエンチャントは、読めなければ `SyncedSettings` のその種類のオンを false で送る。
- エリトラ（`ElytraSpot`・`MoveHits`）: 当てた瞬間に `motion` を足す（上限なし。ユーザーの方針）。マルチでは、バニラのサーバー（`NetHandlerPlayServer#processPlayer`）が、1 tick の移動の距離の 2 乗が 100（エリトラで飛んでいる間は 300）を超えると元の位置に引き戻す（ログに `moved too quickly!`）。`/gamerule disableElytraMovementCheck true` を案内している（`doc/play.md`・`doc/install.md`）。
- 落下（`FallSpot`・`MoveHits`・`common/FallMath`・`FallDamage`）: 着地のダメージはサーバーが `fallDistance` から決めるので、当てたら両側で `fallDistance` を減らす（負にして着地まで持ち越す）。見込みのダメージはバニラの `EntityLivingBase#fall` と `applyPotionDamageCalculations` と同じ式（落下は防具の防御値では減らず、保護のエンチャントは効く）。真下の地面は 1 ブロックずつ調べる（形のないブロックを飛ばして探すと、水も素通りしていた）。進捗「九死に一生」は、着地から 10 tick 生きていたら与える（`MoveHits.State.closeCall`）。
- `server/MoveHits`（はしご・エリトラ・走り・泳ぎ・落下）: サーバーの位置は遅れて届くので、直前 10 tick のどこかで条件を満たしていたかで確かめる。

### 動物

- 判定は両側の `AnimalTargets`。動物のタイマーはクライアントに届いていない（`getGrowingAge` は ±1、卵のタイマー・村人の取引は同期されない）ので、`AnimalStates` が問い合わせ（`QueryMessage` / `StateMessage`）、返事が来るまで弱点を出さない。バニラの動作の抑止は、サーバーは自分で調べて止め、クライアントは返事を覚えているときだけ止める（最初の右クリックは返事の前なので、サーバーが止める）。
- 効果は `server/AnimalHits`。村人の取引は `server/VillagerTrades`（非公開の `buyingList`・`populateBuyingList`）。繁殖できない理由は `server/VillagerBreedHints`・`common/VillagerBreeding`（バニラの `EntityAIVillagerMate` と同じ条件。`getIsWillingToMate(true)` は食べ物を減らすので `false` で読む。バニラの `hasEnoughFoodToBreed` はビートルートも数えるが、その気にするときに減らさないので、その気にならない）。
- 自分の動物の弱点は、体に隠れた部分も深度テストを切って薄く重ねる（ニワトリなど、模型が当たり判定の箱より外に出るため）。

### 表示と音（クライアント）

- 描画: ブロック・生き物の弱点は `WeakSpotRenderer`（目との間に水・溶岩があれば深度テストを切る。1.12.2 では水面も深度を書くので、水の中のブロックの弱点が隠れていた。照準の先のブロックは液体のほかにさえぎる物がないので、壁が透けることはない）。配置は `common/WeakSpotPlacer`、動きと残像は `common/MarkerMotion`（表示位置と当たり判定の位置は別）。バーは `WorldBar`（`GrowthBar`・`AnimalBar`・`MachineBars`）と `OwnSpotBars`。
- 音: `HitSounds`（音の高さは `common/HitStreak` の連続ヒット数から `HitPitch` / `HitScale`）。和音は旋律より下に重ねる（ピッチの上限 2.0 のため）。プリセットは `config/SoundPreset`、画面は `SoundTab`・`MyPresetsScreen`（バニラのボタンは右クリックを拾わないので、`SoundTab.mouseClicked` で受ける）。
- コンボの表示: `ComboHud`（段階は `common/ComboTier`・`ComboMilestones`・`ComboFactor`）、節目の演出は `ComboEffects`・`MilestoneEffects`（累計の節目のタイトルを優先する）。
- 自分の弱点の見た目と種類のオン・オフ: `MarkerLook`・`KindSwitches`・`common/KindMask`。オフの種類は `SwitchMessage` でサーバーにも送り、右クリックの抑止とヒットの受け付けを止める（バニラの動きに戻すため）。
- 一時オフ（`ToggleKeyHandler`。HOME キー。J は JourneyMap と重なるため）: まとめて止めるのは `ClientWeakSpotHandler.stopOwnWeakSpots`。サーバーにも `SwitchMessage` で伝え、`ServerSwitches` が右クリックの抑止・ヒット・ブースト・マークの転送を止める。
- 他のプレイヤー: マークは `OtherMarkers` → `MarkerRelay`（サーバーは検証せずに転送する）、音は `OtherHitMessage`、頭の上のコンボは `OtherCombos`、節目の知らせの非表示は `OthersMilestoneFilter`。
- 統計画面（K キー）: `StatsScreen` と各タブ（`StatsTab`・`SoundTab`・`KindsTab`・`TargetTab`。土台 `StatsScreenTab`）。設定画面は `WeakSpotGuiFactory` が `common/SettingGroups` のまとめで組む（ファイルのカテゴリではなく、種類・用途で）。
- 更新のお知らせ `UpdateNotes`（`weakspot.news.<版>`）と、新しい版の通知 `UpdateCheckNotice`（Forge の `updateJSON`。`main` の `update.json`）。`/weakspot bug` は `BugCommand`・`common/BugReport`（`ClientCommandHandler` で受ける。サーバーの `/weakspot` は OP 権限が要り、ソロでチートがオフだと打てないため。古い Java は Windows 10・11 を「Windows 8.1」と答えるので注記する）。
- サーバーの版で機能を止める: `ServerFeatures.since("1.x.y")`（`SyncedSettings.serverVersion`）。古いサーバーにつなぐと、弱点が出るのにバニラの動作（GUI など）も起きる食い違いを防ぐ。今は使っている所がない（1.11.x の条件は、接続できる下限が 1.12 になったので消した）。

### 的当て・記録・知らせ（サーバー）

- 的当て: `ItemTarget`・`server/TargetRounds`・`client/TargetPlay`・`common/TargetRules`。ラウンドの時間はサーバーの tick で数える。弱点の大きさの設定 `spotSize` は掛けない（記録を公平にするため）。的の位置はクライアントが、始めたときの向きからのずれで持つ。記録は `MiningStats` の `targetBest`・`targetRounds`（「統計をリセット」では消さない。ご褒美の解放が消えるため）。ご褒美の段階は、自己ベストと取った進捗の高いほう（`TargetRules.Tier.unlocked`。しきい値を上げる前に取った段階を残すため）。
- 統計: `ServerStats`（累計はプレイヤーの永続データ `PlayerPersisted` の `weakspot`、「今回」はメモリ）、`common/MiningStats`（保存のキーは `saveKey`。近接は古いキー `critHits` も読む）。節目は `MiningRewards`・`common/Milestones`。進捗は `WeakSpotAdvancements`（条件はどれも `minecraft:impossible` で、サーバーが与える。ログインで累計から判定し直す）。順位は `Leaderboard`（ワールドの保存データ `data/weakspot_leaderboard.dat`。同じ数は名前の順）。
- 配る物: `GuideBookGiver`（ガイドの本・的・ようこその案内。永続データの `guideGiven`・`targetsGiven`・`targetGiftVersion`（ない人は 1.11.0 とみなす））。
- ほかのプレイヤーへの知らせ: 節目・コンボの段階・的当ての 1 位（`MiningRewards.broadcast*`・`TargetRounds`）。
- サーバーからの文: 翻訳キーで送り、クライアントが自分の言語で文にする。パッチで足したキー（今は `weakspot.target.updateGift`）は `server/PlayerText.of` を通し、キーを持たない古いクライアントにだけ `server/ServerLang` が jar の言語ファイルから文章を作る。版の違いの知らせは `VersionCheck`（`common/ModVersions`）。
- コマンド: `WeakSpotCommand`（`stats` / `reset` は権限 2、`top` はだれでも、`reload` は `WeakSpotConfig.reloadFromFile`）。

### 共通の部品

`mc1.12.2/src/test/.../compat/WireCompatTest` が、統計・設定の送る中身と保存のキーがパッチの間に 1 バイトも変わらないことを確かめる（通信・保存の形を変えるマイナーでは、期待値を作り直す）。

- ヒットの受け付け: `server/HitHandlers`（種類 → 処理の表。`HitHandlersTest` が全種類の登録を確かめる）→ `server/HitGate`（`allowed` / `ready` / `mark` / `take` / `accept`。間隔は `common/HitBudget`。受け付けたあとの統計・節目・コンボ・ほかの人への音は `accept` にまとめる）。
- 後片付け: `HitGate.forgetAll`（ログアウト・リスポーン・ディメンション移動。プレイヤーごとの記憶を持つクラスはここに並ぶ）。
- 種類の表: `HitKind`（初期の色・コンボの掛け数を使うか）、`SyncedSettings.enabled(kind)` / `minHitInterval(kind)`（フィールドの名前の規則 `<key>WeakSpotEnabled` / `<key>MinHitIntervalTicks` で表を作る）、`SyncedSettings.server()`（サーバーの自分の設定。`fromConfig()` はリフレクションで約 80 項目を集めるので、ヒットのたびに呼ばない）。
- コンボ: `common/ComboFactor`（掛け数）、`ServerStats.countStreak`、本人のコンボの直し `server/ComboRelay.checkOwn`・`common/ComboSync`。
- 照準のまわり・画面の上: `AimSpotKind` / `AimSpots`、`ScreenSpotKind` / `ScreenSpots`（置き場所の選び方は `placeRandom`）、`HudSpot.gauge`・`ChargeGauge`、`ScreenProjection.drawMarker` / `edgeArrow`、`common/TimedBoost`・`common/TimedBoostMath`。
- そのほか: `UseTimeCut`、`server/SpeedModifier`、`common/QueryThrottle`、`server/QueryHandlers`（状態を問い合わせる種類を足すときは、表に足す。パケットは足さない）、`Reflect` / `Reflect.lazyField`（名前の試し方は [development.md](development.md) の「開発環境の細かいこと」）、`network/ServerThread.run`。
- **弱点の種類を足すときに直す所**: `HitKind`（末尾に）、`server/HitHandlers` とその種類の `*Hits`、照準のまわりなら `AimSpotKind` を継いだクラスと `AimSpots.KINDS`、画面の上なら `ScreenSpotKind` を継いだクラスと `ScreenSpots.KINDS`、統計（`MiningStats` の配列は自動。送る並びに入るので通信が変わる → マイナー）、`SyncedSettings` のフィールドと `WIRE`・`WeakSpotConfig`（同じ名前。名前の規則に合わせると種類の表に自動で入る。上の「種類の表」）、プレイヤーごとの記憶を持つなら `HitGate.forgetAll`、`KindMask`・「弱点マーカー」タブ、lang（統計・設定の説明・節目）、`README.md` の種類の一覧・`README.en.md` の種類の名前・`doc/store/description.md` の種類の表・`doc/play.md`・`doc/config.md`・この文書、ガイドの本（`GuideBook.CONTENT`）。

### 通信

`network/`（1.12.2 は `WeakSpotMod.preInit` で登録し、番号は登録順。1.21.1 は `Network.register` で、ID は `weakspot:<名前>`。1.20.1 は `Network.register` で `SimpleChannel`（`weakspot:main`）に登録し、番号は登録順。中身の並びはどの版でも同じ）。

| 番号 | メッセージ | 向き | 中身 |
|---|---|---|---|
| 0 | `HitMessage` | C→S | ヒット（種類・位置・連続ヒット数・エンティティ ID） |
| 1 | `OtherHitMessage` | S→C | 受け付けたヒットを近くのプレイヤーへ（音。的当ての ✕ は `miss`） |
| 2 | `SettingsMessage` | S→C | `SyncedSettings`（並びは `SyncedSettings.WIRE`） |
| 3 / 4 | `StatsRequestMessage` / `StatsMessage` | C→S / S→C | 統計の要求（リセットを含む）と統計（最後に的当ての上位） |
| 5 | `MilestoneMessage` | S→C | 節目 |
| 6 / 7 | `MarkerMessage` / `OtherMarkerMessage` | C→S / S→C | 弱点のマーク |
| 8 | `SwitchMessage` | C→S | 一時オフ・機械の粒子・オフの種類のビット |
| 9 / 10 | `QueryMessage` / `StateMessage` | C→S / S→C | 状態の問い合わせと返事（動物・釣り・機械） |
| 11 | `OtherComboMessage` | S→C | 頭の上のコンボと、本人のコンボの直し |
| 12 / 13 | `TargetMessage` / `TargetActionMessage` | S→C / C→S | 的当て |
| 14 | `OtherTargetMessage` | S→C | 近くのプレイヤーの的当てのヒット数 |
| -（1.20.1・1.21.1 版のみ） | `HelloMessage` | C→S | 接続直後に自分の Mod の版（Forge 47・NeoForge は相手の Mod の版を教えてくれないため。`PlayerText.clientSince`・`VersionCheck` が使う） |

## クライアントを信じる範囲

不正対策の方針（ユーザーの判断）。

- **サーバーが正**: コンボの数（掛け数・節目・告知・進捗）、統計、間隔、種類ごとの条件、的当ての時間。クライアントから来た数（`HitMessage` の `streak` など）は、ほかの人の音の高さと、ずれの確認（`ComboRelay.checkOwn`）にしか使わず、サーバーの数をそれに合わせない。
- **クライアントを信じている所**: 「照準が弱点に合った」こと（全種類）。改造クライアントなら、合わせずに当てたと送れる。ただし間隔と条件はサーバーが確かめるので（間隔は 4 回分まで貯められる `HitBudget`）、得られるのは普通に当て続けたときと同じまで（最初の 4 回分を一度に使える程度）で、バニラ用のチートツールでもできる程度。**塞がない**。
- **的当て**: 当てた・✕ もクライアントの申告なので、ハズレを無視して記録・順位・告知を荒らせる。対策（弱点の位置をサーバーが決めて向きを確かめる。[roadmap.md](roadmap.md) の案 A）は、公開サーバーで遊ぶ予定が出るまで保留。
- サーバーからクライアントへ送るものは間引く（1 人あたりの送信の間隔を決める）。でたらめな通知を大量に送られても、サーバーの送信が増えないように。
- 接続のときの Mod の一覧の版（`PlayerText.clientVersion`）は改造クライアントなら書き換えられる。これで機能を切り替えてよいのは、ごまかしても本人だけが困るものに限る。

## 重要: サーバー側のブーストは時間枠ではない

1.12.2 のサーバー（`PlayerInteractionManager#blockRemoving`）は進捗を積算せず、破壊完了の通知を受けた瞬間に「**現在の**破壊速度 × (経過tick+1) ≥ 0.7」で判定する。そのため、サーバー側でも時間枠の間だけ倍率を掛けると、判定の瞬間が枠外のときにブーストが効かない。そうなるとブロックが戻り、後から通常速度で壊れる。
そこでサーバーは、ヒットごとに追加進捗 `(倍率-1)×継続tick×コンボの掛け数`（`BoostMath.extraTicksPerHit`）を貯め、そのブロックの `BreakSpeed` に `1 + 追加tick/(経過tick+1)` を掛ける（`BoostMath`）。こうすると積算した場合と同じ結果になる。

- ヒット間隔の制限は、クライアントが `minHitIntervalTicks`（成長・機械はそれぞれの設定）、サーバーは `HitBudget`。サーバーがヒットを拒否してクライアントだけブーストされると、ブロックが一度戻って見える。
- 採掘のコンボ倍率: クライアントは時間枠の中の速さに `BoostMath.clientMultiplier` を掛ける（掛け数は自分のコンボ数から）。サーバーは `HitGate.accept` が返すコンボ数から。境目で一瞬ずれても、サーバーは進み具合 70% で壊れたことを認めるので吸収される。

### 見えない壁（ゴーストブロック）の対策（issue #38）

バニラ（`NetHandlerPlayServer.processPlayerDigging`）は、掘り終わり（STOP）と中断（ABORT）のあとはブロックが空気でなければ `SPacketBlockChange` を返すが、**掘り始め（START）には返さない**。クライアントの `PlayerControllerMP.clickBlock` は START を送ってから「1 tick で壊れるか」を判定し、壊れるならその場で壊す（STOP は送らない）。そのため、クライアントのブーストがクリックし直しても続くと、サーバーで戻ったブロックをクリックし直したとき、クライアントだけでその場で壊れ、サーバーには残る。対策は 3 つ:

- クリックのたびにブーストを捨てる（`MiningBoost.onLeftClickBlock`・`ServerBoostTracker`。`LeftClickBlock` は、その場で壊れるかの判定より先に来る。例外: 掘る前のヒットと、待ちを 0 にするための当てたブロックは残す）。
- サーバーは、クリックした座標と前の座標の今の状態を、その tick の終わりに本人にだけ `SPacketBlockChange` で送り直す（`ServerBoostTracker.queueResend`）。古いクライアントや、ほかの Mod で速さがずれたときも、同じ tick のうちに戻る。
- 保留の破壊: バニラの `blockRemoving` は、進捗が 0.7 未満で掘り終わりが届くと破壊を保留にする（`receivedFinishDiggingPacket` / `delayedDestroyPos`）。**保留は 1 枠だけ**で、埋まっていると次の不足は捨てられて STOP の返事でブロックが戻る。保留中のブロックは「今の速さ × 経過 tick ≥ 1.0」で壊れる。違うブロックを掘り始めても、保留中のブロックには追加進捗を掛け続ける（`ServerBoostTracker.DELAYED`）。保留のあとに壊れたら、統計・耐久回復に数える。

## 1.20.1 版・1.21.1 版

共通の考え方（上の各節）は同じで、Minecraft・ローダーの API で違う所だけを書く。1.20.1 版は 1.21.1 版を手本に移したもの。下の各項目は 1.21.1 版のことで、1.20.1 版も同じ（違う所だけを、最後の「1.20.1 版で違う所」に書く）。

- 入口: `WeakSpotMod`（`@Mod`。設定・通信・登録・サーバーのイベント）。物理クライアントでは `client.ClientProxy` を名前で作る（専用サーバーでクライアントのクラスを読まないため。1.12.2 の `@SidedProxy` と同じ考え）。アイテムと保存データの登録は `Registration`。
- 非公開のメンバーは `accesstransformer.cfg`（1.12.2 の `Reflect` に当たる）。読めないときの代わりの動き（1.12.2 の「読めなければ弱点を出さない」）は要らないので、`EnchantHits`・`PortalHits` の `isAvailable` はいつも true。
- 毎フレームのヒット判定と、ワールドへの描画: `RenderLevelStageEvent` の `AFTER_WEATHER`（モデルビューにカメラの向きが入っている最後の段）。頂点はカメラ（目）からの相対座標。HUD の弱点の投影に使う行列も、ここで `ScreenProjection.onFrame` に渡す。
- HUD は `RenderGuiEvent.Post`、GUI の上は `ScreenEvent.Render.Post`。色だけの図形は `client/Draw`（1.21 には `GL_LINE_LOOP` がないので、線の輪は最初の点に戻して閉じる）。
- 採掘: クライアントの `MultiPlayerGameMode` とサーバーの `ServerPlayerGameMode` の判定（積算／「破壊速度 × (経過tick+1) ≥ 0.7」・保留 1 枠）は 1.12.2 と同じなので、`BoostMath` をそのまま使う。NeoForge は `LeftClickBlock` を、クライアントでは押している間毎 tick（`CLIENT_HOLD`）、サーバーでは掘り終わり・中断でも送るので、ブーストを捨てるのは `START` だけ。サーバーが状態を送り直すのは `ClientboundBlockUpdatePacket`。
- 機械: かまどなどの ticker はサーバーにしかないので、毎 tick 動くブロックの一覧はサーバーが作って `SyncedSettings.tickingBlocks` で送る（1.21.1 版だけの項目）。加速は ticker を余分に呼ぶ。レッドストーン部品の予約は、チャンクの `LevelChunkTicks` から探して早め、同じ位置・ブロックの予約を消してから入れ直す（予約は 1 つまでのため）。クラフターは ticker も持つが（作る見た目の動きだけ）、ディスペンサーと同じ「1 回の信号で複数回」の部品として扱う（`RightClickTargets.isBurstMachine`。2 回目からは予約の処理 `state.tick` を呼ぶ）。
- 弓（`BowDraw`）: トライデント・クロスボウも同じ弱点。引き切りの tick は `BowDraw.fullTicks`（クロスボウは高速装填を含む）、1 ヒットの量は `BowMath.scaledHitTicks`（弓の `bowHitTicks` と同じ割合。端数は持ち越す）。どちらもバニラが離したときの残り時間で引きを決めるので、まだ減らしていない分は `LivingEntityUseItemEvent.Stop` で `useItemRemaining` に入れる。過剰チャージ: トライデントは出た `ThrownTrident`、クロスボウは撃った矢（`setBaseDamage`）・花火に掛ける。トライデント・花火は威力が baseDamage で決まらないので、persistent data に倍率を書いて `LivingIncomingDamageEvent` で掛ける。激流は、バニラが押すのと同じ向きに倍率の残りの分を両側で押す。クロスボウの過剰チャージは、装填したとき（Stop）にアイテムの `custom_data`（`weakspot_overcharge`）に書き（矢がなく装填できないときは書かない）、撃つ直前（`RightClickItem`）に読んで消す（持ち替えても残すため。バニラの欄なので、Mod を外しても読める）。
- 近接: メイスも対象（`MeleeCharge.isWeapon`）。落下の上乗せはバニラがクリティカルの倍率より先に足すので、溜めの倍率は落下の分にも掛かる。
- 動物: アルマジロのウロコは、ニワトリの卵と同じ間隔（6000〜11999 tick）なので、卵の設定と `AnimalTimers.eggAfterHit` を使い、`Timer.SCUTE` で別のバーにする。村人の繁殖の案内は、1.14 以降の条件（空いているベッド・食べ物の点数）で `VillagerBreeding.reasonsWithBeds`。取引の段階の解放は、職業レベルを 1 つ上げる。
- ネザーゲート: 待ちは `Entity.portalProcess`（`PortalProcessor.portalTime`）。ネザーゲートだけ（エンドのゲートは待たない）。
- 収穫: 1.21 には `HarvestDropsEvent` がないので、収穫物は `CommonHooks.handleBlockDrops`（`BlockDropsEvent`）を通す。壊すときの許可は `CommonHooks.fireBlockBreak`。
- 保存データ: プレイヤーはデータアタッチメント `Registration.PLAYER_DATA`（死んでも残す。中身のキーは 1.12.2 の `PlayerPersisted.weakspot` と同じ）、順位はワールドの `SavedData`（`weakspot_leaderboard`）。
- 設定: `ModConfigSpec` を、`ServerConfig`・`ClientConfig` のフィールドと注釈（`config/Config`）からリフレクションで組む（項目の名前・範囲・説明を 1.12.2 と 1 か所で揃えるため）。ファイルを保存すると NeoForge が読み直し（`ModConfigEvent.Reloading`）、サーバーの項目なら全員に送り直す。設定画面は `WeakSpotGuiFactory`（NeoForge の標準の画面はファイルのカテゴリの並びになるので、`SettingGroups` のまとめで自作）。
- コマンド: サーバーの `/weakspot` は Brigadier。クライアントの `/weakspot bug`・`skipupdate` は `RegisterClientCommandsEvent`（クライアントに無いサブコマンドは、NeoForge がそのままサーバーへ送る）。

### 1.20.1 版で違う所

- ローダーは Forge 47（ForgeGradle 6）。動いているときの名前は **SRG 名**なので、`accesstransformer.cfg` は SRG 名で書き、名前で探すリフレクション（`HeldItems` の右クリックのメソッド）は Mojang の名前と SRG 名の両方を見る（1.21.1 は動いているときも Mojang の名前）。
- 通信: `SimpleChannel`（`Network.PROTOCOL` が同じ相手だけ。ハンドラーはメインのスレッドへ回す）。メッセージの中身は 1.21.1 と同じ。
- イベント: tick は `TickEvent`（`phase` で前後を分ける）、設定の読み込みは `ModConfigEvent.Loading`・`Reloading`、近接の倍率は `LivingHurtEvent`（1.21.1 の `LivingIncomingDamageEvent` に当たる）。`RenderLevelStageEvent` の `AFTER_WEATHER` では、イベントのポーズスタック（カメラの向き）がモデルビューにもう掛かっているので、頂点はカメラからの相対座標のまま渡し、`ScreenProjection.onFrame` にはポーズスタックの行列を渡す。
- 保存データ: プレイヤーの統計は `getPersistentData()` の `PlayerPersisted` の下の `weakspot`（1.12.2 と同じ。死んでも残る）。ガイドの本・クロスボウの過剰チャージ（`weakspot_overcharge`）はアイテムの NBT。
- 届く距離: 生き物は Forge の `canReach(entity, 3.0)`、ブロックは `canReach(pos, 1.5)`（どちらも Forge 47.1.32 の右クリックの受け付けと同じ。47.4 の `canReachRaw` は下限の版にないので使わない）。
- 移動速度の修正は UUID で付け外しする（1.12.2 と同じ UUID）。
- 収穫: 壊すときの許可は `ForgeHooks.onBlockBreakEvent`、収穫物はそのままワールドに出す（1.20.1 の Forge に `BlockDropsEvent` はない）。
- ネザーゲート: 待ちは `Entity.portalTime`（ネザーゲートの中でだけ増える）と `getPortalWaitTime()`。
- 1.20.1 にない物（アルマジロ・ウィンドチャージ・メイス・クラフター）の処理はない。機械の「1 回の信号で複数回」はディスペンサー・ドロッパーだけ。
- 採掘の判定（積算／「破壊速度 × (経過tick+1) ≥ 0.7」・保留 1 枠）と `LeftClickBlock` の `START`・`CLIENT_HOLD` は 1.21.1 と同じ。

## 設定

`[サーバー]` / `[クライアント]` の扱い・カテゴリ・設定の足し方は [CLAUDE.md](../CLAUDE.md) の「設定の約束事」、項目の一覧は [config.md](config.md)。

- `throw` は Java の予約語なので、フィールドは `throwing`（`@Config.Name("throw")`）。根の `general` には `configVersion` だけを置く。
- 移行（`WeakSpotConfig.migrate`）: `init` でも呼ぶ（クライアントだけで遊ぶときも移るように）。Forge は使われなくなった項目を自分では消さないので、6 で消す。`preInit` の間の `ConfigManager.sync` は「読み込み」になるので保存できない。値は `Property` に書いてから `sync` するので、`AVAILABLE` より前でも動く。
- 読み直し（`reloadFromFile`）: 起動後の `sync` は `hasChanged` の項目だけファイル → フィールドの向きになるので、読み込んだあと全項目に `set` をし直す。
