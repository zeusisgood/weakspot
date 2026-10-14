# 仕組み（開発者向け）

[← README に戻る](../README.md)

コードの地図と、コードを読んでも分からないこと（理由・バニラを調べた結果・互換性）だけを書きます。処理の中身（How）はコードとそのコメントを、遊び方は [play.md](play.md)、設定の一覧は [config.md](config.md)、約束事は [../CLAUDE.md](../CLAUDE.md) を見てください。仕組みを変えたら、ここも直します。


## 全体

- `common/`: Minecraft に依存しない純粋な計算。
- パッケージの直下（両側）: クライアントとサーバーが同じ条件（同期した設定）で判定するもの。対象の判定 `RightClickTargets`（成長・機械・収穫）・`AnimalTargets`・`VehicleTargets`・`MeleeTargets`、手 `HeldItems`、プレイヤー `PlayerRules`、使用時間と溜め `UseTimeCut`（`BowDraw`・`EatDraw`）・`HeldCharge`（`MeleeCharge`・`ThrowCharge`）、落下の見込み `FallDamage`、ガイドの本 `GuideBook`、的 `ItemTarget`、非公開のメンバー `Reflect`。
- `client/`: 弱点の状態・ヒット判定・描画・音・クライアント側のブースト。
- `server/`（論理サーバー）: ヒットの検証と効果・統計・節目・順位。
- `network/`: パケット（下の「通信」）。
- `config/`: 設定（下の「設定」）と、送る設定 `SyncedSettings`。

### 右クリックの弱点（成長・機械・収穫）

- 対象の判定は `RightClickTargets.classify`（機械 → 成長 → 収穫 → 自動の成長の順）。成長は `IGrowable` の `canGrow` と、追加リスト `growthExtraBlocks`（`common/BlockFilter` / `GrowthFilters`。状態の条件はプロパティの名前と値の名前で見るので、IC2 などのクラスに依存しない）。柱の植物（`COLUMN_BLOCKS`）はバニラの `updateTick` と同じ条件をコードで見る。機械は `ITickable` の TE と、予約された tick で動くレッドストーンの部品（`isScheduledMachine`）。
- 自動の成長（`isAutoGrowable`）: `getTickRandomly()` のブロックを、しゃがんでいるときだけ（Random Things の Time in a Bottle と同じ考え方。通常の右クリックを横取りしないため）。両側とも 1.11.2 以上のときだけ（`ServerFeatures.since` / `PlayerText.clientSince`）。`randomTickSpeed` が 0 以下なら効果をかけない（IC2 の `BlockFoam.randomTick` がそのとき例外を投げる）。
- 対象を右クリックしたら、`RightClickBlock` をメインハンドで SUCCESS にしてキャンセルし、通常動作（GUI・オフハンドの設置）を止める。クライアントでキャンセルしてもバニラは右クリックのパケットを送るので、サーバーでも発火し、そこで「直前に右クリックした」ことを記録する。押しっぱなしでバニラは 4 tick ごとに右クリックする。
- 検証と効果は `server/RightClickHits`（成長は `randomTick` を余分に呼ぶ。キノコは `randomTick` では広がるだけなので、確率で `grow`）。育たないときの知らせは `server/GrowthWarnings`・`common/GrowthStuck`。収穫は `server/HarvestHits`・`common/HarvestBonus`（`BreakEvent` で保護を確かめてから）。
- 機械の加速は `server/MachineAccelerator`（Time in a Bottle と同じく `update()` を余分に呼ぶ。例外はあえて捕まえない: クラッシュレポートで機械を特定し、`excludedBlocks` に足してもらう。ユーザーの判断）。レッドストーンの部品は `server/ScheduledTicks`・`common/ScheduledBoost`（予約の前倒し。`getPendingBlockUpdates` は X と Z だけで絞るので、同じ列のほかの予約は元の時刻のまま入れ直す。ディスペンサーの余分の発射は非公開の `dispense`）。
- 「手が空いている」は、空か右クリックで何もしないアイテム（`HeldItems.isFree`。クラスが右クリックの 4 つのメソッドを上書きしていないか）。動物では、さらにバニラの動物の側が反応する物（`isBreedingItem`・オオカミの骨・馬の小麦と砂糖）を外す。右クリックで動くアイテムは版ごとに違う（1.13 から斧で皮をはぐ、1.14 から鉄でゴーレムを直す、など）。

### 採掘

- 対象: 壊せて、今の破壊速度で 1 tick に進む量（`getPlayerRelativeBlockHardness`）が 1.0 未満のブロック。1.0 以上はバニラの `PlayerControllerMP.clickBlock` がクリックした瞬間に壊し、「掘っている」状態にも入らない。
- クライアント: `ClientWeakSpotHandler`（照準と出し直し）・`MiningBoost`（ブースト・掘る前のヒット・次を掘るまでの待ち）・`WeakSpotRenderer`。サーバー: `ServerBoostTracker`（下の「重要」の節）・`MiningRewards`（耐久回復と節目）。
- 掘る前のヒット: 照準のブロックに薄く出し、左クリックを押している間だけ当たる（照準を合わせるだけで当たると、ブロックを交互に見るだけでコンボが増え続けるため）。サーバーは予約にして、掘り始めた瞬間に確定する（`ServerBoostTracker.RESERVED`。掘らずにコンボを稼げないように）。
- 次を掘るまでの待ち: 当てたブロックを壊したら、`PlayerControllerMP` の非公開の `blockHitDelay`（バニラは 5）を 0 にする。サーバーはこの待ちを確かめていない。
- 耐久回復はブロックを壊したときに精算する（`common/RepairSettlement`）。`BreakEvent` はツールの耐久が減る前に来るので、耐久が残り 1 でもツールが残る。
- 耐久バー（`common/BlockHealthBar`）の進み具合は、`client/MiningProgress` が `PlayerControllerMP` の非公開のフィールドを読む。

### 照準のまわり・画面の上の弱点

- 照準のまわり（乗り物・食事・弓・はしご・走り・エリトラ・投げる物・近接・ゲート・泳ぎ・落下）: 土台は `AimSpotKind` / `AimSpots`、描き方と当たり判定は `HudSpot`・`ScreenProjection`・`FishingMath.allowedAngle`、向きの計算は `common/BowMath`。種類ごとのクラスは条件・出し方・効果・ゲージだけを書く。
  - 動いている間に出す種類（走り・はしご・泳ぎ・エリトラ・乗り物）は、条件が一瞬外れても 10 tick 残す（サーバーの `MoveHits` の「直前 10 tick」と同じ。バニラは壁にこすれた瞬間に走りを一度切るので、2 マスの通路で弱点がちらついていた）。
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
- 統計画面（K キー）: `StatsScreen` と各タブ（`StatsTab`・`SoundTab`・`KindsTab`・`TargetTab`。土台 `StatsScreenTab`）。設定画面は `WeakSpotGuiFactory` が `config/SettingGroups` のまとめで組む（ファイルのカテゴリではなく、種類・用途で）。
- 更新のお知らせ `UpdateNotes`（`weakspot.news.<版>`）と、新しい版の通知 `UpdateCheckNotice`（Forge の `updateJSON`。`main` の `update.json`）。`/weakspot bug` は `BugCommand`・`common/BugReport`（`ClientCommandHandler` で受ける。サーバーの `/weakspot` は OP 権限が要り、ソロでチートがオフだと打てないため。古い Java は Windows 10・11 を「Windows 8.1」と答えるので注記する）。
- サーバーの版で機能を止める: `ServerFeatures.since("1.x.y")`（`SyncedSettings.serverVersion`）。古いサーバーにつなぐと、弱点が出るのにバニラの動作（GUI など）も起きる食い違いを防ぐ。今使っている版は 1.11.2（自動の成長・掘る前のヒット）と 1.11.3（本人のコンボの直し）。

### 的当て・記録・知らせ（サーバー）

- 的当て: `ItemTarget`・`server/TargetRounds`・`client/TargetPlay`・`common/TargetRules`。ラウンドの時間はサーバーの tick で数える。的の位置はクライアントが、始めたときの向きからのずれで持つ。記録は `MiningStats` の `targetBest`・`targetRounds`（「統計をリセット」では消さない。ご褒美の解放が消えるため）。ご褒美の段階は、自己ベストと取った進捗の高いほう（`TargetRecords.tier`。しきい値を上げる前に取った段階を残すため）。
- 統計: `ServerStats`（累計はプレイヤーの永続データ `PlayerPersisted` の `weakspot`、「今回」はメモリ）、`common/MiningStats`（保存のキーは `saveKey`。近接は古いキー `critHits` も読む）。節目は `MiningRewards`・`common/Milestones`。進捗は `WeakSpotAdvancements`（条件はどれも `minecraft:impossible` で、サーバーが与える。ログインで累計から判定し直す）。順位は `Leaderboard`（ワールドの保存データ `data/weakspot_leaderboard.dat`。同じ数は名前の順）。
- 配る物: `GuideBookGiver`（ガイドの本・的・ようこその案内。永続データの `guideGiven`・`targetsGiven`・`targetGiftVersion`（ない人は 1.11.0 とみなす））。
- ほかのプレイヤーへの知らせ: 節目・コンボの段階・的当ての 1 位（`MiningRewards.broadcast*`・`TargetRounds`）。
- サーバーからの文: 翻訳キーで送り、クライアントが自分の言語で文にする。パッチで足したキー（今は `weakspot.target.updateGift`）は `server/PlayerText.of` を通し、キーを持たない古いクライアントにだけ `server/ServerLang` が jar の言語ファイルから文章を作る。版の違いの知らせは `VersionCheck`（`common/ModVersions`）。
- コマンド: `WeakSpotCommand`（`stats` / `reset` は権限 2、`top` はだれでも、`reload` は `WeakSpotConfig.reloadFromFile`）。

### 共通の部品

`src/test/.../compat/WireCompatTest` が、統計・設定の送る中身と保存のキーがパッチの間に 1 バイトも変わらないことを確かめる（通信・保存の形を変えるマイナーでは、期待値を作り直す）。

- ヒットの受け付け: `server/HitHandlers`（種類 → 処理の表。`HitHandlersTest` が全種類の登録を確かめる）→ `server/HitGate`（`allowed` / `ready` / `mark` / `take` / `accept`。間隔は `common/HitBudget`。受け付けたあとの統計・節目・コンボ・ほかの人への音は `accept` にまとめる）。
- 後片付け: `HitGate.forgetAll`（ログアウト・リスポーン・ディメンション移動。プレイヤーごとの記憶を持つクラスはここに並ぶ）。
- 種類の表: `HitKind`（初期の色・コンボの掛け数を使うか）、`SyncedSettings.enabled(kind)` / `minHitInterval(kind)`（フィールドの名前の規則 `<key>WeakSpotEnabled` / `<key>MinHitIntervalTicks` で表を作る）、`SyncedSettings.server()`（サーバーの自分の設定。`fromConfig()` はリフレクションで約 80 項目を集めるので、ヒットのたびに呼ばない）。
- コンボ: `common/ComboFactor`（掛け数）、`ServerStats.countStreak`、本人のコンボの直し `server/ComboRelay.checkOwn`・`common/ComboSync`。
- 照準のまわり・画面の上: `AimSpotKind` / `AimSpots`、`ScreenSpotKind` / `ScreenSpots`（置き場所の選び方は `placeRandom`）、`HudSpot.gauge`・`ChargeGauge`、`ScreenProjection.drawMarker` / `edgeArrow`、`client/TimedBoost`・`common/TimedBoostMath`。
- そのほか: `UseTimeCut`、`server/SpeedModifier`、`client/QueryThrottle`、`server/QueryHandlers`（状態を問い合わせる種類を足すときは、表に足す。パケットは足さない）、`Reflect` / `Reflect.lazyField`（名前の試し方は [development.md](development.md) の「開発環境の細かいこと」）、`network/ServerThread.run`。
- **弱点の種類を足すときに直す所**: `HitKind`（末尾に）、`server/HitHandlers` とその種類の `*Hits`、照準のまわりなら `AimSpotKind` を継いだクラスと `AimSpots.KINDS`、画面の上なら `ScreenSpotKind` を継いだクラスと `ScreenSpots.KINDS`、統計（`MiningStats` の配列は自動。送る並びに入るので通信が変わる → マイナー）、`SyncedSettings` のフィールドと `WIRE`・`WeakSpotConfig`（同じ名前。名前の規則に合わせると種類の表に自動で入る。上の「種類の表」）、プレイヤーごとの記憶を持つなら `HitGate.forgetAll`、`KindMask`・「弱点マーカー」タブ、lang（統計・設定の説明・節目）、`README.md` の種類の一覧・`README.en.md` の種類の名前・`doc/store/description.md` の種類の表・`doc/play.md`・`doc/config.md`・この文書、ガイドの本（`GuideBook.CONTENT`）。

### 通信

`network/`（`WeakSpotMod.preInit` で登録。番号は登録順）。

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

- クリックのたびにブーストを捨てる（`MiningBoost.onLeftClickBlock`・`ServerBoostTracker`。`LeftClickBlock` は、その場で壊れるかの判定より先に来る）。
- サーバーは、クリックした座標と前の座標の今の状態を、その tick の終わりに本人にだけ `SPacketBlockChange` で送り直す（`ServerBoostTracker.queueResend`）。古いクライアントや、ほかの Mod で速さがずれたときも、同じ tick のうちに戻る。
- 保留の破壊: バニラの `blockRemoving` は、進捗が 0.7 未満で掘り終わりが届くと破壊を保留にする（`receivedFinishDiggingPacket` / `delayedDestroyPos`）。**保留は 1 枠だけ**で、埋まっていると次の不足は捨てられて STOP の返事でブロックが戻る。保留中のブロックは「今の速さ × 経過 tick ≥ 1.0」で壊れる。違うブロックを掘り始めても、保留中のブロックには追加進捗を掛け続ける（`ServerBoostTracker.DELAYED`）。保留のあとに壊れたら、統計・耐久回復に数える。

## 設定

`[サーバー]` / `[クライアント]` の扱い・カテゴリ・設定の足し方は [CLAUDE.md](../CLAUDE.md) の「設定の約束事」、項目の一覧は [config.md](config.md)。

- `throw` は Java の予約語なので、フィールドは `throwing`（`@Config.Name("throw")`）。根の `general` には `configVersion` だけを置く。
- 移行（`WeakSpotConfig.migrate`）: `preInit` の間の `ConfigManager.sync` は「読み込み」（ファイルの値でフィールドを上書きする）になるので、そこでは保存できない。
- 読み直し（`reloadFromFile`）: 起動後の `sync` は `hasChanged` の項目だけファイル → フィールドの向きになるので、読み込んだあと全項目に `set` をし直す。
