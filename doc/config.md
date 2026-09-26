# 設定

[← README に戻る](../README.md)

初回起動後に生成される `config/weakspot.cfg` で変更できます（サーバーの場合は、サーバーフォルダの `config/` に生成）。ゲーム内では、タイトル画面の「Mods」→「Weak Spot Mining」→「設定」、または統計画面（K キー）の「設定画面を開く」から変更できます。

- **サーバー**の項目は、**サーバーの値が優先**されます。ログイン時にサーバーからクライアントへ送信され、接続中はその値が使われます（切断すると自分の値に戻ります）。
  - 専用サーバーでは、`config/weakspot.cfg` を編集してから `/weakspot reload`（OP のみ）を実行するか、サーバーを再起動してください。
  - シングルプレイと LAN のホストは、Mods メニューで変更できます（変更すると、接続中の全員に再送信されます）。
  - 専用サーバーや他人の LAN に接続中は、自分の Mods メニューで変更したサーバー項目は使われません（設定画面にもその旨が表示されます）。
- **クライアント**の項目は、各自の設定として保存されます（ヒット音、他プレイヤーのマークの表示、コンボ表示、弱点の移動演出、各種バー・ゲージ、自分の弱点の種類別のオン／オフ・色・形など）。接続中も自分の値が使われます。

## サーバー

| キー | 初期値 | 説明 |
|---|---|---|
| `miningWeakSpotEnabled` | true | 採掘の弱点のオン／オフ（オフで全員がバニラの採掘に戻る） |
| `miningComboBonus` | true | 採掘のコンボ倍率のオン／オフ（オンで 1 回に進む量にコンボの掛け数を乗算） |
| `boostMultiplier` | 4.0 | ヒット時の破壊速度の倍率（1 回で進む上乗せ分に、コンボの掛け数を掛ける） |
| `boostDurationTicks` | 4 | 倍率を掛ける時間（tick） |
| `minHitIntervalTicks` | 6 | 採掘ヒットの最小受付間隔（tick） |
| `weakSpotRadiusRatio` | 0.14 | 弱点の半径（面の短辺に対する比率） |
| `weakSpotMinRadius` | 0.08 | 弱点の半径の下限（ブロック）。ボタンなど小さい面でも当てやすくする。上限と矛盾する場合は上限を優先 |
| `weakSpotMaxRadiusRatio` | 0.35 | 弱点の半径の上限（面の短辺に対する比率）。小さい面で弱点がはみ出さないようにする |
| `minFaceSize` | 0.15 | 弱点を出す面の短辺の最小長（ブロック）。これより小さい面（カーペットの側面など）には出さない |
| `edgeMargin` | 0.1 | 弱点の外周と面の縁の最小間隔（ブロック）。大きいほど弱点が中央に寄る |
| `minMoveDistance` | 0.4 | ヒット後に弱点が移動する最小距離（ブロック） |
| `lingerTicks` | 40 | 長押しを止めた後に弱点が残る時間（tick） |
| `hitsPerRepair` | 5 | 破壊したブロックでの採掘ヒット何回ごとに、手に持った道具の耐久値を回復するか（壊さずに止めたブロックのヒットは数えない。余りは次の破壊へ持ち越し）。0 で回復なし |
| `repairPerStep` | 1 | 1 回の回復量 |
| `maxRepairPerBreak` | 1 | 1 回の破壊での回復量の上限（超過分は持ち越さない）。0 で耐久回復を無効化（節目の報酬は別） |
| `milestones` | 50, 100, 250, 500, 777, 1000, 1500 … 100000（30 個） | 節目となる採掘ヒット数。種類別の節目も同じ数値 |
| `milestoneXp` | 5, 10, 15, 20, 77, 30, 40 … 1000（30 個） | 節目ごとの経験値（`milestones` の順に対応。不足分は 0）。種類別の節目もこの値 |
| `milestoneRepair` | `milestoneXp` と同じ | 節目ごとの耐久回復量（採掘の節目のみ。`milestones` の順に対応。不足分は 0） |
| `milestoneRepeatInterval` | 50000 | `milestones` の最大値より先は、この数ごとに節目とする（報酬は最後の値）。0 で繰り返しなし。**サーバー専用** |
| `kindMilestonesEnabled` | true | 採掘以外の種類別ヒット数でも節目とするか（報酬は経験値のみ）。**サーバー専用** |
| `totalMilestonesEnabled` | true | 全種類の合計ヒット数でも節目とするか。**サーバー専用** |
| `totalMilestones` | 1000, 5000, 7777, 10000, 25000, 50000, 77777, 100000, 250000, 500000, 777777, 1000000 | 合計の節目となる数。**サーバー専用** |
| `totalMilestoneXp` | 100, 200, 777, 300, 500, 700, 7777, 1000, 1500, 2000, 7777, 5000 | 合計の節目ごとの経験値（`totalMilestones` の順に対応）。**サーバー専用** |
| `totalMilestoneRepeatInterval` | 500000 | `totalMilestones` の最大値より先は、この数ごとに合計の節目とする。0 で繰り返しなし。**サーバー専用** |
| `growthWeakSpotEnabled` | true | 作物・苗木の成長の弱点のオン／オフ |
| `growthTicksPerHit` | 5 | 成長ヒット 1 回で追加実行する成長判定（randomTick）の回数 |
| `growthWarnings` | true | 作物・苗木の弱点に当てても育たない時に、チャットで通知するか。**サーバー専用** |
| `growthStuckHits` | 30 | 連続何回当てても変化がない場合に「外部要因で育たない」と通知するか（5〜1000）。**サーバー専用** |
| `growthMinHitIntervalTicks` | 6 | 成長ヒットの最小受付間隔（tick） |
| `growthMinRadius` | 0.08 | 作物・苗木等の弱点の最小半径（ブロック） |
| `growthExcludedBlocks` | 草ブロック、草、背の高い草花 | 成長の弱点を出さないブロックの登録名（サトウキビ・サボテン・ネザーウォートにも有効） |
| `mushroomGrowChance` | 0.2 | キノコへの成長ヒット 1 回で、巨大キノコへの成長を試みる確率（0〜1）。**サーバー専用** |
| `growthExtraBlocks` | サトウキビ、サボテン、ネザーウォート、IC2 のゴムの木 | 成長の弱点の対象に追加するブロック（追加リスト）。1 行に「登録名」または「登録名[プロパティ=条件,...]」（書式は[成長の弱点](play.md#作物苗木の成長素手で右クリック長押し)を参照）。当てると randomTick を追加実行して成長を早める |
| `machineWeakSpotEnabled` | true | 機械の弱点のオン／オフ（オフでしゃがんで素手の右クリックは GUI を開く） |
| `machineBoostMultiplier` | 4.0 | 機械ヒット時に、機械の処理を毎 tick 何倍実行するか。コンボ継続でさらに掛け数（×1.25〜×4）を乗算。クライアントにも送信（HUD の「機械 n倍速」） |
| `machineBoostParticles` | true | 加速中の機械の周囲に、速度に応じた色の粒子を出すか（近くのプレイヤーに表示。各自の `machineParticlesVisible` がオフの人には送信しない）。**サーバー専用** |
| `machineBoostMaxMultiplier` | 16.0 | 機械の倍率（コンボの掛け数を乗算した後）の上限。機械 Mod の不具合やサーバー負荷が気になる時に下げる。クライアントにも送信 |
| `machineBoostDurationTicks` | 6 | 機械の加速の持続時間（tick）。複数プレイヤーが同じ機械を加速しても効果は重複しない |
| `machineMinHitIntervalTicks` | 6 | 機械ヒットの最小受付間隔（tick） |
| `excludedBlocks` | 宝箱、トラップチェスト、エンダーチェスト、エンチャントテーブル、ビーコン、シュルカーボックス（16 色） | 機械の加速の対象外とするブロックの登録名 |
| `animalWeakSpotEnabled` | true | 動物の弱点のオン／オフ |
| `animalBabyEnabled` | true | 動物の弱点で、子供の成長を早めるか |
| `animalBreedingEnabled` | true | 動物の弱点で、繁殖の待ち時間を短縮するか |
| `sheepWoolEnabled` | true | 羊の弱点で、羊毛の再生を早めるか |
| `chickenEggEnabled` | true | ニワトリの弱点で、次の産卵までの時間を短縮するか |
| `villagerTradeResetEnabled` | true | 村人の弱点で、ロックされた取引の上限をリセットするか |
| `animalBabyHits` | 40 | 子供が大人になるまでの目安のヒット数（1 ヒット = 24000 tick ÷ この数）。0 以下で無効 |
| `animalBreedingHits` | 10 | 繁殖の待ち時間が終わるまでの目安のヒット数（1 ヒット = 6000 tick ÷ この数）。0 以下で無効 |
| `sheepWoolHits` | 10 | 毛を刈られた羊の毛が再生するまでのヒット数。0 以下で無効 |
| `chickenEggHits` | 15 | 次の産卵までの目安のヒット数（1 ヒット = 平均 9000 tick ÷ この数）。0 以下で無効 |
| `villagerTradeResetHits` | 10 | 村人のロックされた取引の上限がリセットされるまでのヒット数。0 以下で無効 |
| `animalMinHitIntervalTicks` | 6 | 動物ヒットの最小受付間隔（tick） |
| `animalExcludedEntities` | （空） | 動物の弱点の対象外とするエンティティ ID（例: `minecraft:cow`） |
| `animalSneakRequiredEntities` | （空） | しゃがみ＋素手の時だけ弱点を出す、他 Mod の動物のエンティティ ID（バニラの馬・飼いならしたオオカミ等はコードで判定） |
| `villagerResetUnlocksNewTier` | false | 村人の取引上限のリセットで、新しい取引段階も解放するか（バニラの補充と同じ）。オフならロック解除のみ。**サーバー専用** |
| `fishingWeakSpotEnabled` | true | 釣りの弱点のオン／オフ |
| `fishingHits` | 6 | 最長の待ち時間（600 tick）を 0 にするまでのヒット数（1 ヒット = 600 tick ÷ この数）。0 以下で無効 |
| `fishingMinHitIntervalTicks` | 6 | 釣りヒットの最小受付間隔（tick） |
| `bowWeakSpotEnabled` | true | 弓の弱点のオン／オフ |
| `bowHitTicks` | 5 | 1 ヒットごとに進める弓の引き絞りの tick 数（バニラの弓は 20 tick で引き切り。引き切りは超えない）。0 で無効 |
| `bowMinHitIntervalTicks` | 4 | 弓ヒットの最小受付間隔（tick） |
| `meleeWeakSpotEnabled` | true | 近接の弱点のオン／オフ |
| `meleeMinHitIntervalTicks` | 4 | 近接ヒットの最小受付間隔（tick） |
| `meleeChargePerHit` | 0.25 | 1 ヒットごとの溜め量（次の攻撃の倍率に加算。0.05〜10.0）。コンボの掛け数を上乗せ |
| `meleeChargeMax` | 0 | 近接の溜めの上限。0 で上限なし（初期値） |
| `portalWeakSpotEnabled` | true | ネザーゲートの弱点のオン／オフ |
| `portalHitTicks` | 20 | 1 ヒットごとに短縮する転移の待ち時間（tick。1〜200）。コンボの掛け数を上乗せ |
| `portalMinHitIntervalTicks` | 4 | ネザーゲートヒットの最小受付間隔（tick） |
| `vehicleWeakSpotEnabled` | true | 乗り物の弱点のオン／オフ |
| `vehicleBoostMultiplier` | 1.5 | 乗り物の弱点に当てた時の速度倍率。コンボの掛け数（25 で ×1.25 … 1000 で ×4）を上乗せ |
| `vehicleBoostMaxMultiplier` | 0 | 乗り物の速度倍率の上限。0 で上限なし（初期値。コンボ 1000 で 6 倍） |
| `vehicleBoostDurationTicks` | 40 | 乗り物の加速の持続時間（tick）。ヒットのたびにこの長さに戻る |
| `vehicleMinHitIntervalTicks` | 6 | 乗り物ヒットの最小受付間隔（tick） |
| `eatWeakSpotEnabled` | true | 飲食の弱点のオン／オフ |
| `eatHitTicks` | 16 | 1 ヒットごとに短縮する飲食時間（tick）。16 なら 2 ヒットで完了 |
| `eatMinHitIntervalTicks` | 4 | 飲食ヒットの最小受付間隔（tick） |
| `sleepWeakSpotEnabled` | true | 睡眠中の弱点のオン／オフ |
| `sleepHitTicks` | 200 | 1 ヒットごとに進めるワールドの時刻（tick）。次の朝は越えない。**サーバー専用** |
| `sleepMinHitIntervalTicks` | 6 | 睡眠ヒットの最小受付間隔（tick） |
| `ladderWeakSpotEnabled` | true | はしごの弱点のオン／オフ |
| `ladderBoostMultiplier` | 1.5 | はしごの弱点に当てた時の速度倍率。コンボの掛け数を上乗せ |
| `ladderBoostMaxMultiplier` | 0 | はしごの速度倍率の上限。0 で上限なし（初期値） |
| `ladderBoostDurationTicks` | 40 | はしごの加速の持続時間（tick）。ヒットのたびにこの長さに戻る |
| `ladderMinHitIntervalTicks` | 6 | はしごヒットの最小受付間隔（tick） |
| `elytraWeakSpotEnabled` | true | エリトラの弱点のオン／オフ |
| `elytraBoostPower` | 1.5 | 1 ヒットごとに加算する速度（ブロック/tick。0.1〜10.0）。コンボの掛け数を上乗せ。速度の上限なし |
| `elytraMinHitIntervalTicks` | 6 | エリトラヒットの最小受付間隔（tick） |
| `enchantWeakSpotEnabled` | true | エンチャントの弱点のオン／オフ |
| `enchantMinHitIntervalTicks` | 6 | エンチャントヒットの最小受付間隔（tick） |
| `harvestWeakSpotEnabled` | true | 収穫の弱点のオン／オフ。右クリック収穫系の Mod と競合する場合はオフに |
| `harvestMinHitIntervalTicks` | 4 | 収穫ヒットの最小受付間隔（tick） |
| `harvestComboBonus` | true | 収穫時、コンボの掛け数だけ収穫物を増やすか（種は増やさない）。**サーバー専用** |
| `throwWeakSpotEnabled` | true | 投擲物の弱点のオン／オフ |
| `throwChargePerHit` | 0.5 | 1 ヒットごとの溜め量（投擲速度の倍率に加算。0.1〜10.0）。コンボの掛け数を上乗せ。上限なし |
| `throwMinHitIntervalTicks` | 4 | 投擲物ヒットの最小受付間隔（tick） |
| `sprintWeakSpotEnabled` | true | ダッシュの弱点のオン／オフ |
| `sprintBoostMultiplier` | 1.5 | ダッシュの弱点に当てた時の速度倍率。コンボの掛け数を上乗せ |
| `sprintBoostMaxMultiplier` | 0 | ダッシュの速度倍率の上限。0 で上限なし（初期値） |
| `sprintBoostDurationTicks` | 40 | ダッシュの加速の持続時間（tick）。ヒットのたびにこの長さに戻る |
| `sprintMinHitIntervalTicks` | 6 | ダッシュヒットの最小受付間隔（tick） |
| `critsPerRepair` | 5 | 近接の溜めた攻撃が何回命中するごとに、手に持った物の耐久値を回復するか（余りはログアウトまで持ち越し）。0 で回復なし。**サーバー専用** |
| `critRepairPerStep` | 1 | `critsPerRepair` ごとの回復量。**サーバー専用** |
| `giveGuideBook` | true | 初回ログインのプレイヤーにガイドの本を渡すか（1 人 1 回）。**サーバー専用** |
| `markerShareRange` | 16 | 他プレイヤーの弱点マークが見える範囲（ブロック）。0 で非表示 |
| `markerSendMinIntervalTicks` | 2 | 弱点マークの状態を送信する最小間隔（tick）。通信量を抑える |

「**サーバー専用**」の項目はクライアントに送信されません。

`edgeMargin` を大きくすると弱点の移動範囲が狭くなり、`minMoveDistance` だけ離れた位置を確保できない場合があります。その場合、弱点はできるだけ遠い位置へ移動します。

## クライアント

| キー | 初期値 | 説明 |
|---|---|---|
| `myHitSound` | PLING | 自分のヒット音の楽器。`XYLOPHONE` `CHIME` `BELL` `FLUTE` `GUITAR` `HARP` `BASS` `HAT` `SNARE` `BASEDRUM` `PLING` から選択 |
| `myHitVolume` | 0.25 | 自分のヒット音の音量（0〜1）。0 で無音 |
| `othersHitSound` | XYLOPHONE | 他プレイヤーのヒット音の楽器（選択肢は `myHitSound` と同じ） |
| `othersHitVolume` | 0.4 | 他プレイヤーのヒット音の音量（0〜1）。0 で無音 |
| `otherMarkerEnabled` | true | 他プレイヤーの弱点マークを表示するか |
| `otherMarkerColor` | #3FA9FF | 他プレイヤーの弱点マークの色（#RRGGBB）。読めない値の場合は水色 |
| `otherMarkerAlpha` | 0.6 | 他プレイヤーの弱点マークの濃さ（自分のマークの濃さに乗算。1 で同じ） |
| `otherMarkerShape` | RING | 他プレイヤーの弱点マークの形。`CIRCLE`（円）、`RING`（輪）、`DIAMOND`（ひし形）、`SQUARE`（四角）から選択。自分の弱点の形は `myMarkerShapes` |
| `comboDisplayEnabled` | true | コンボ数を画面に表示するか |
| `comboScale` | 1.0 | コンボの数字の大きさの倍率（0.5〜2.0） |
| `comboPosition` | BELOW_CROSSHAIR | コンボの表示位置。`BELOW_CROSSHAIR`（照準の下）、`RIGHT_OF_CROSSHAIR`（照準の右）、`TOP_CENTER`（画面上部中央。ボスバーがあればその下）から選択 |
| `comboMilestoneEffects` | true | コンボが 10・25・50・100・250・500・1000（以降 1000 ごと）に達した時の演出（強調音・光・花火・タイトル）を出すか |
| `weakSpotTrailEnabled` | true | 弱点の移動演出（元の位置から素早く動き、残像を残す）。見た目のみで、当たり判定は即座に移動先。オフでその場で切り替わる。他プレイヤーのマークにも有効 |
| `growthBarEnabled` | true | 成長の弱点を出している作物の足元に、成長度を黄色のバーで表示するか |
| `weakSpotsEnabled` | true | 自分の弱点のオン／オフ（HOME キーで切り替え） |
| `blockHealthBarEnabled` | true | 採掘中のブロックの残り耐久を、面の下部に緑のバーで表示するか |
| `animalSpotSeeThrough` | true | 自分の動物の弱点が体に隠れた時（ニワトリ等）、隠れた部分を薄く透かして表示するか。他プレイヤーのマークは透かさない |
| `bowDrawBarEnabled` | true | 弓を引いている間、照準の下に引き具合のゲージを表示するか。弓の弱点や HOME キーのオン／オフに関係なく表示 |
| `vehicleBoostBarEnabled` | true | 乗り物の加速中、照準の上に残り時間のゲージ（水色）を表示するか |
| `ladderBoostBarEnabled` | true | はしごの加速中、照準の上に残り時間のゲージ（茶色）を表示するか |
| `sprintBoostBarEnabled` | true | ダッシュの加速中、照準の上に残り時間のゲージ（赤）を表示するか |
| `meleeChargeBarEnabled` | true | 近接の溜めのゲージ（銀）を照準の下に表示するか |
| `throwChargeBarEnabled` | true | 投擲物の溜めのゲージ（青緑）を照準の下に表示するか |
| `machineParticlesVisible` | true | 加速中の機械の周囲の色の粒子を、自分の画面に表示するか（自分の機械・他人の機械とも） |
| `machineBarEnabled` | true | かまど・醸造台・スポナーを叩いている間、上に進行度（黄）と燃料（橙）のバーを表示するか |
| `othersComboDisplay` | true | 近くの他プレイヤーのコンボ（10 以上）を、頭上に「n HIT」と表示するか |
| `showUpdateNotes` | true | バージョン更新後、初めてワールドに入った時にチャットで更新のお知らせを表示するか |
| `lastSeenVersion` | （空） | 最後に更新のお知らせを見たバージョン。Mod が書き換えるため、編集しないでください |
| `disabledKinds` | （空） | 自分でオフにした弱点の種類（1 行に 1 つ。`mining` `growth` `machine` `animal` `fishing` `bow` `melee` `vehicle` `eat` `sleep` `ladder` `elytra` `enchant` `harvest` `throw` `sprint` `portal`）。統計画面の「弱点マーカー」タブで変更可能 |
| `myMarkerColors` | （空） | 自分の弱点の色（1 行に `種類=#RRGGBB`）。未記入の種類は初期値。「弱点マーカー」タブで変更可能 |
| `myMarkerShapes` | （空） | 自分の弱点の形（1 行に `種類=circle`・`ring`・`diamond`・`square`）。未記入の種類は円。「弱点マーカー」タブで変更可能 |

## 作物の成長バー

成長の弱点を出している作物の足元に、成長度を示す**黄色のバー**が表示されます（プレイヤーの方を向き、左から伸びます）。年齢（`age`）を持つ作物、苗木（`stage`）、サトウキビ・サボテン（効果がかかる最上段の年齢 ÷ 15）に対応しています。設定は `growthBarEnabled`。

## 他プレイヤーのマークの形

他プレイヤーの弱点マークは、初期値では中抜きの**輪**です（自分の塗りつぶしの円と区別しやすくするため）。設定 `otherMarkerShape` で、円・ひし形・四角に変更できます。

## 管理コマンド（サーバー）

権限レベル 2（OP）以上で使用できます。シングルプレイではチート有効時に使用可能です。対象はオンラインのプレイヤーのみです（オフラインのプレイヤーを指定すると案内が表示されます）。プレイヤー名は Tab キーで補完できます。

| コマンド | 内容 |
|---|---|
| `/weakspot` | 使い方を表示 |
| `/weakspot stats <プレイヤー>` | そのプレイヤーの統計（今回・累計）をチャットに表示 |
| `/weakspot reset <プレイヤー>` | そのプレイヤーの統計の累計を消去（統計画面の「累計をリセット」と同じ。節目も再度受け取り可能になる） |
| `/weakspot reload` | `config/weakspot.cfg` を再読み込みし、接続中の全員に設定を再送信（再起動せずに設定を反映） |
| `/weakspot bug` | 不具合の報告方法を表示（**誰でも使用可能**。クライアント側のコマンドで、サーバーには送信しない） |

## ブーストの目安

1 回のヒットで、通常の `(倍率 − 1) × 継続時間` tick 分だけ破壊が進みます（初期値では 12 tick ＝ 約 0.6 秒分）。約 0.6 秒ごとにヒットすると通常の約 2 倍の速さになり、`minHitIntervalTicks` の制限により最大で約 3 倍になります。
