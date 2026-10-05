# 設定

[← README に戻る](../README.md)

初回起動後に生成される `config/weakspot.cfg` で変更できます（サーバーの場合は、サーバーフォルダの `config/` に生成）。ゲーム内では、タイトル画面の「Mods」→「Weak Spot Mining」→「設定」、または統計画面（K キー）の「設定画面を開く」から変更できます。

- **サーバー**の項目は、**サーバーの値が優先**されます。ログイン時にサーバーからクライアントへ送信され、接続中はその値が使われます（切断すると自分の値に戻ります）。
  - 専用サーバーでは、`config/weakspot.cfg` を編集してから `/weakspot reload`（OP のみ）を実行するか、サーバーを再起動してください。
  - シングルプレイと LAN のホストは、Mods メニューで変更できます（変更すると、接続中の全員に再送信されます）。
  - 専用サーバーや他人の LAN に接続中は、自分の Mods メニューで変更したサーバー項目は使われません（設定画面にもその旨が表示されます）。
- 設定画面は、種類・用途のまとめ（下の見出しの順）で表示されます。サーバーの値が優先される項目は、説明の先頭に「マルチプレイ時、サーバー側設定が優先されます。」と表示されます。説明の最後の行は、ファイルのキーです。
- ファイルの中は 2 段のカテゴリです（上の段が `server`／`client`、その下が種類・用途）。下の表の「ファイルのキー」が `server.` で始まる項目がサーバー、`client.` で始まる項目がクライアントです。ファイルでは `server { mining { D:boostMultiplier=4.0 } }` の形です。エディタの検索（vi なら `/boostMultiplier`）で探せます。
- `general.configVersion`（設定ファイルの移行の目印）と `client.updates.lastSeenVersion`（最後に見た更新のお知らせの版）は、Mod が書き換える項目なので、設定画面には表示されません。
  - カテゴリ分けの前（1.9.x まで）の `weakspot.cfg` は、初回起動時に自動で新しい形へ移行されます（書き換えていた値はそのまま）。スクリプトなどで `general` の項目を書き換えている場合は、キーの直しが必要です。
- **クライアント**の項目は、各自の設定として保存されます（ヒット音、他プレイヤーのマークの表示、コンボ表示、弱点の移動演出、各種バー・ゲージ、自分の弱点の種類別のオン／オフ・色・形など）。接続中も自分の値が使われます。

## 共通

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 弱点の大きさ | `server.general.spotSize` | 1.0 | 全種類の弱点の大きさの倍率（見た目と当たり判定の両方。0.5〜2.0）。釣りはさらに 0.75 倍。的当てには効かない |
| 持ち物も素手と同じに | `server.general.heldItemsCountAsEmptyHand` | true | 右クリックで何もしないアイテム（ツルハシ・棒・インゴットなど）を持っていても、成長・収穫・機械・動物の弱点を出すか（自動判定。動物の餌などは除外） |
| 素手と同じに扱わない物 | `server.general.heldItemExcludes` | （空） | 素手と同じに扱わないアイテムの登録名（1 行に 1 つ。例 `modid:wrench`）。右クリックで何かするのに自動判定で漏れる Mod のアイテム用 |
| ガイドの本を渡す | `server.general.giveGuideBook` | true | 初回ログインのプレイヤーにガイドの本を渡すか（1 人 1 回）。**サーバー専用** |
| 更新のときに配る的の数 | `server.general.updateGiftTargets` | 1 | Mod を更新したとき、プレイヤーに配る弱点の的の数（版が変わるたびに 1 回。0 で配らない。`giveGuideBook` がオフなら配らない）。**サーバー専用** |
| 更新のお知らせ | `client.updates.showUpdateNotes` | true | バージョン更新後、初めてワールドに入った時にチャットで更新のお知らせを表示するか |
| 新しい版の通知 | `client.updates.checkForUpdates` | true | 新しいバージョンが公開されていたら、ワールドに入った時にチャットで通知するか（Forge の `versionCheck` が false の時は通知なし） |
| 通知しない版 | `client.updates.skippedUpdateVersion` | （空） | チャットの [この版は通知しない] を押したバージョン。このバージョンの通知は表示しない。空にすると再び通知 |

## 音

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 自分のヒット音 | `client.sound.myHitSound` | PLING | 自分のヒット音の楽器。`XYLOPHONE` `CHIME` `BELL` `FLUTE` `GUITAR` `HARP` `BASS` `HAT` `SNARE` `BASEDRUM` `PLING` から選択 |
| 自分のヒット音の音量 | `client.sound.myHitVolume` | 0.25 | 自分のヒット音の音量（0〜1）。0 で無音 |
| コンボで和音にする | `client.sound.hitChordEnabled` | true | 自分のヒット音に、コンボで音を重ねるか（25 から 2 音、100 から 3 音）。オフで 1 音のまま |
| 音階の動き | `client.sound.hitScaleDirection` | UP | `UP`（上がりきったら最低音に戻る）／`UP_DOWN`（上がったら下がる往復）。他プレイヤーのヒット音にも使う |
| 音階の種類 | `client.sound.hitScaleType` | MAJOR | `MAJOR`（長音階）／`PENTATONIC`（ペンタトニック）／`MINOR`（短調）。節目の駆け上がりと途切れの音にも使う |
| 音域 | `client.sound.hitScaleOctaves` | 1 | 1〜2 オクターブ。2 なら 1 つ下のオクターブ（ピッチ 0.5）から |
| マイプリセット | `client.sound.soundPresets` | （空） | 音のマイプリセット 1〜3。K キーの「サウンド」タブの [マイプリセット…] の画面で書かれる（1 行に「番号=楽器,音階の種類,動き,音域,和音」）。手で書き換えなくてよい |
| ほかの人のヒット音 | `client.sound.othersHitSound` | XYLOPHONE | 他プレイヤーのヒット音の楽器（選択肢は `myHitSound` と同じ） |
| ほかの人の音量 | `client.sound.othersHitVolume` | 0.4 | 他プレイヤーのヒット音の音量（0〜1）。0 で無音 |

## マーカー

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 自分の弱点を出す | `client.markers.weakSpotsEnabled` | true | 自分の弱点のオン／オフ（HOME キーで切り替え） |
| オフにした種類 | `client.markers.disabledKinds` | （空） | 自分でオフにした弱点の種類（1 行に 1 つ。`mining` `growth` `machine` `animal` `fishing` `bow` `melee` `vehicle` `eat` `sleep` `ladder` `elytra` `enchant` `harvest` `throw` `sprint` `portal`）。統計画面の「弱点マーカー」タブで変更可能 |
| 自分の弱点の色 | `client.markers.myMarkerColors` | （空） | 自分の弱点の色（1 行に `種類=#RRGGBB`）。未記入の種類は初期値。「弱点マーカー」タブで変更可能 |
| 自分の弱点の形 | `client.markers.myMarkerShapes` | （空） | 自分の弱点の形（1 行に `種類=circle`・`ring`・`diamond`・`square`・`star`（的当ての銅で解放））。未記入の種類は円。「弱点マーカー」タブで変更可能 |
| 金の粒 | `client.markers.goldHitParticles` | false | 弱点に当てた時に金の粒を散らすか（的当ての金で解放。届いていないワールドでは出ない）。K キーの「的当て」タブでも切り替え可能 |
| 移動の残像 | `client.markers.weakSpotTrailEnabled` | true | 弱点の移動演出（元の位置から素早く動き、残像を残す）。見た目のみで、当たり判定は即座に移動先。オフでその場で切り替わる。他プレイヤーのマークにも有効 |
| ほかの人のマーク | `client.markers.otherMarkerEnabled` | true | 他プレイヤーの弱点マークを表示するか |
| ほかの人のマークの色 | `client.markers.otherMarkerColor` | #3FA9FF | 他プレイヤーの弱点マークの色（#RRGGBB）。読めない値の場合は水色 |
| ほかの人のマークの濃さ | `client.markers.otherMarkerAlpha` | 0.6 | 他プレイヤーの弱点マークの濃さ（自分のマークの濃さに乗算。1 で同じ） |
| ほかの人のマークの形 | `client.markers.otherMarkerShape` | RING | 他プレイヤーの弱点マークの形。`CIRCLE`（円）、`RING`（輪）、`DIAMOND`（ひし形）、`SQUARE`（四角）から選択。自分の弱点の形は `myMarkerShapes` |
| マークを共有する範囲 | `server.general.markerShareRange` | 16 | 他プレイヤーの弱点マークが見える範囲（ブロック）。0 で非表示 |
| マークの送信の間隔 | `server.general.markerSendMinIntervalTicks` | 2 | 弱点マークの状態を送信する最小間隔（tick）。通信量を抑える |

## コンボ

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| コンボを表示 | `client.combo.comboDisplayEnabled` | true | コンボ数を画面に表示するか |
| コンボの大きさ | `client.combo.comboScale` | 1.0 | コンボの数字の大きさの倍率（0.5〜2.0） |
| コンボの位置 | `client.combo.comboPosition` | BELOW_CROSSHAIR | コンボの表示位置。`BELOW_CROSSHAIR`（照準の下）、`RIGHT_OF_CROSSHAIR`（照準の右）、`TOP_CENTER`（画面上部中央。ボスバーがあればその下）から選択 |
| コンボの節目の演出 | `client.combo.comboMilestoneEffects` | true | コンボが 10・25・50・75・100・150・200・250・300（以降 100 ごと）に達した時の演出（強調音・光・花火・タイトル）を出すか |
| ほかの人のコンボ | `client.combo.othersComboDisplay` | true | 近くの他プレイヤーのコンボ（10 以上）を、頭上に「n HIT」と表示するか |
| 次の倍率のゲージ | `client.combo.comboFactorGaugeEnabled` | true | コンボの数字の下に、次に掛け数が上がる段階（25・50・100…）までの進み具合のゲージを表示するか。掛け数を使う種類の弱点が出ている間だけ出る。色は次の段階の色 |

## 節目

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| ほかの人の節目を表示 | `client.combo.showOthersMilestones` | true | ほかのプレイヤーが節目に届いたときの知らせ（チャット）を表示するか。自分の節目の演出とチャットは、オフでも出る |
| 採掘の節目 | `server.milestones.milestones` | 50, 100, 250, 500, 777, 1000, 1500 … 100000（30 個） | 節目となる採掘ヒット数。種類別の節目も同じ数値 |
| 節目の経験値 | `server.milestones.milestoneXp` | 5, 10, 15, 20, 77, 30, 40 … 1000（30 個） | 節目ごとの経験値（`milestones` の順に対応。不足分は 0）。種類別の節目もこの値 |
| 節目の耐久回復 | `server.milestones.milestoneRepair` | `milestoneXp` と同じ | 節目ごとの耐久回復量（採掘の節目のみ。`milestones` の順に対応。不足分は 0） |
| 節目の繰り返し間隔 | `server.milestones.milestoneRepeatInterval` | 50000 | `milestones` の最大値より先は、この数ごとに節目とする（報酬は最後の値）。0 で繰り返しなし。**サーバー専用** |
| 種類ごとの節目 | `server.milestones.kindMilestonesEnabled` | true | 採掘以外の種類別ヒット数でも節目とするか（報酬は経験値のみ）。**サーバー専用** |
| 合計の節目 | `server.milestones.totalMilestonesEnabled` | true | 全種類の合計ヒット数でも節目とするか。**サーバー専用** |
| 合計の節目の数 | `server.milestones.totalMilestones` | 1000, 5000, 7777, 10000, 25000, 50000, 77777, 100000, 250000, 500000, 777777, 1000000 | 合計の節目となる数。**サーバー専用** |
| 合計の節目の経験値 | `server.milestones.totalMilestoneXp` | 100, 200, 777, 300, 500, 700, 7777, 1000, 1500, 2000, 7777, 5000 | 合計の節目ごとの経験値（`totalMilestones` の順に対応）。**サーバー専用** |
| 合計の繰り返し間隔 | `server.milestones.totalMilestoneRepeatInterval` | 500000 | `totalMilestones` の最大値より先は、この数ごとに合計の節目とする。0 で繰り返しなし。**サーバー専用** |

## 採掘

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 採掘の弱点 | `server.mining.miningWeakSpotEnabled` | true | 採掘の弱点のオン／オフ（オフで全員がバニラの採掘に戻る） |
| コンボの倍率 | `server.mining.miningComboBonus` | true | 採掘のコンボ倍率のオン／オフ（オンで 1 回に進む量にコンボの掛け数を乗算） |
| 採掘の速さの倍率 | `server.mining.boostMultiplier` | 4.0 | ヒット時の破壊速度の倍率（1 回で進む上乗せ分に、コンボの掛け数を掛ける） |
| 倍率が続く時間 | `server.mining.boostDurationTicks` | 4 | 倍率を掛ける時間（tick） |
| 弱点の半径の比率 | `server.mining.weakSpotRadiusRatio` | 0.14 | 弱点の半径（面の短辺に対する比率） |
| 縁からの余白 | `server.mining.edgeMargin` | 0.1 | 弱点の外周と面の縁の最小間隔（ブロック）。大きいほど弱点が中央に寄る |
| 移動の最小距離 | `server.mining.minMoveDistance` | 0.4 | ヒット後に弱点が移動する最小距離（ブロック） |
| 弱点の最小半径 | `server.mining.weakSpotMinRadius` | 0.08 | 弱点の半径の下限（ブロック）。ボタンなど小さい面でも当てやすくする。上限と矛盾する場合は上限を優先 |
| 弱点の最大半径の比率 | `server.mining.weakSpotMaxRadiusRatio` | 0.35 | 弱点の半径の上限（面の短辺に対する比率）。小さい面で弱点がはみ出さないようにする |
| 弱点を出す面の最小 | `server.mining.minFaceSize` | 0.15 | 弱点を出す面の短辺の最小長（ブロック）。これより小さい面（カーペットの側面など）には出さない |
| 弱点が残る時間 | `server.mining.lingerTicks` | 40 | 長押しを止めた後に弱点が残る時間（tick） |
| ヒットの最小間隔 | `server.mining.minHitIntervalTicks` | 6 | 採掘ヒットの最小受付間隔（tick） |
| 修理までのヒット数 | `server.repair.hitsPerRepair` | 5 | 破壊したブロックでの採掘ヒット何回ごとに、手に持った道具の耐久値を回復するか（壊さずに止めたブロックのヒットは数えない。余りは次の破壊へ持ち越し）。0 で回復なし |
| 1 回の修理量 | `server.repair.repairPerStep` | 1 | 1 回の回復量 |
| 1 回の破壊の修理上限 | `server.repair.maxRepairPerBreak` | 1 | 1 回の破壊での回復量の上限（超過分は持ち越さない）。0 で耐久回復を無効化（節目の報酬は別） |
| ブロックの耐久バー | `client.hud.blockHealthBarEnabled` | true | 採掘中のブロックの残り耐久を、面の下部に緑のバーで表示するか |

## 成長

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 成長の弱点 | `server.growth.growthWeakSpotEnabled` | true | 作物・苗木の成長の弱点のオン／オフ |
| 1 ヒットの成長の回数 | `server.growth.growthTicksPerHit` | 5 | 成長ヒット 1 回で追加実行する成長判定（randomTick）の回数 |
| 育たない時の知らせ | `server.growth.growthWarnings` | true | 作物・苗木の弱点に当てても育たない時に、チャットで通知するか。**サーバー専用** |
| 知らせるまでのヒット数 | `server.growth.growthStuckHits` | 30 | 連続何回当てても変化がない場合に「外部要因で育たない」と通知するか（5〜1000）。**サーバー専用** |
| ヒットの最小間隔 | `server.growth.growthMinHitIntervalTicks` | 6 | 成長ヒットの最小受付間隔（tick） |
| 弱点の最小半径 | `server.growth.growthMinRadius` | 0.08 | 作物・苗木等の弱点の最小半径（ブロック） |
| 対象外のブロック | `server.growth.growthExcludedBlocks` | 草ブロック、草、背の高い草花 | 成長の弱点を出さないブロックの登録名（サトウキビ・サボテン・ネザーウォートにも有効） |
| 追加のブロック | `server.growth.growthExtraBlocks` | サトウキビ、サボテン、ネザーウォート、IC2 のゴムの木 | 成長の弱点の対象に追加するブロック（追加リスト）。1 行に「登録名」または「登録名[プロパティ=条件,...]」（書式は[成長の弱点](play.md#作物苗木の成長素手で右クリック長押し)を参照）。当てると randomTick を追加実行して成長を早める |
| キノコが育つ確率 | `server.growth.mushroomGrowChance` | 0.2 | キノコへの成長ヒット 1 回で、巨大キノコへの成長を試みる確率（0〜1）。**サーバー専用** |
| 成長のバー | `client.hud.growthBarEnabled` | true | 成長の弱点を出している作物の足元に、成長度を黄色のバーで表示するか |

## 収穫

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 収穫の弱点 | `server.harvest.harvestWeakSpotEnabled` | true | 収穫の弱点のオン／オフ。右クリック収穫系の Mod と競合する場合はオフに |
| ヒットの最小間隔 | `server.harvest.harvestMinHitIntervalTicks` | 4 | 収穫ヒットの最小受付間隔（tick） |
| コンボで収穫を増やす | `server.harvest.harvestComboBonus` | true | 収穫時、コンボの掛け数だけ収穫物を増やすか（種は増やさない）。**サーバー専用** |

## 機械

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 機械の弱点 | `server.machine.machineWeakSpotEnabled` | true | 機械の弱点のオン／オフ（オフでしゃがんで素手の右クリックは GUI を開く） |
| 機械の速さの倍率 | `server.machine.machineBoostMultiplier` | 4.0 | 機械ヒット時に、機械の処理を毎 tick 何倍実行するか。コンボ継続でさらに掛け数（×1.25〜、300 で ×4、以降も上昇）を乗算。クライアントにも送信（HUD の「機械 n倍速」） |
| 倍率の上限 | `server.machine.machineBoostMaxMultiplier` | 16.0 | 機械の倍率（コンボの掛け数を乗算した後）の上限。機械 Mod の不具合やサーバー負荷が気になる時に下げる。クライアントにも送信 |
| 加速の粒子を出す | `server.machine.machineBoostParticles` | true | 加速中の機械の周囲に、速度に応じた色の粒子を出すか（近くのプレイヤーに表示。各自の `machineParticlesVisible` がオフの人には送信しない）。**サーバー専用** |
| 加速が続く時間 | `server.machine.machineBoostDurationTicks` | 6 | 機械の加速の持続時間（tick）。複数プレイヤーが同じ機械を加速しても効果は重複しない |
| ヒットの最小間隔 | `server.machine.machineMinHitIntervalTicks` | 6 | 機械ヒットの最小受付間隔（tick） |
| 対象外のブロック | `server.machine.excludedBlocks` | 宝箱、トラップチェスト、エンダーチェスト、エンチャントテーブル、ビーコン、シュルカーボックス（16 色） | 機械の加速の対象外とするブロックの登録名 |
| 機械のバー | `client.hud.machineBarEnabled` | true | かまど・醸造台・スポナーを叩いている間、上に進行度（黄）と燃料（橙）のバーを表示するか |
| 粒子を表示 | `client.hud.machineParticlesVisible` | true | 加速中の機械の周囲の色の粒子を、自分の画面に表示するか（自分の機械・他人の機械とも） |

## 動物

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 動物の弱点 | `server.animal.animalWeakSpotEnabled` | true | 動物の弱点のオン／オフ |
| 子どもの成長 | `server.animal.animalBabyEnabled` | true | 動物の弱点で、子供の成長を早めるか |
| 繁殖の待ち時間 | `server.animal.animalBreedingEnabled` | true | 動物の弱点で、繁殖の待ち時間を短縮するか |
| 羊毛の再生 | `server.animal.sheepWoolEnabled` | true | 羊の弱点で、羊毛の再生を早めるか |
| ニワトリの卵 | `server.animal.chickenEggEnabled` | true | ニワトリの弱点で、次の産卵までの時間を短縮するか |
| 村人の取引リセット | `server.animal.villagerTradeResetEnabled` | true | 村人の弱点で、ロックされた取引の上限をリセットするか |
| 成長までのヒット数 | `server.animal.animalBabyHits` | 40 | 子供が大人になるまでの目安のヒット数（1 ヒット = 24000 tick ÷ この数）。0 以下で無効 |
| 繁殖までのヒット数 | `server.animal.animalBreedingHits` | 10 | 繁殖の待ち時間が終わるまでの目安のヒット数（1 ヒット = 6000 tick ÷ この数）。0 以下で無効 |
| 羊毛までのヒット数 | `server.animal.sheepWoolHits` | 10 | 毛を刈られた羊の毛が再生するまでのヒット数。0 以下で無効 |
| 卵までのヒット数 | `server.animal.chickenEggHits` | 15 | 次の産卵までの目安のヒット数（1 ヒット = 平均 9000 tick ÷ この数）。0 以下で無効 |
| リセットのヒット数 | `server.animal.villagerTradeResetHits` | 10 | 村人のロックされた取引の上限がリセットされるまでのヒット数。0 以下で無効 |
| ヒットの最小間隔 | `server.animal.animalMinHitIntervalTicks` | 6 | 動物ヒットの最小受付間隔（tick） |
| 対象外の生き物 | `server.animal.animalExcludedEntities` | （空） | 動物の弱点の対象外とするエンティティ ID（例: `minecraft:cow`） |
| しゃがみが要る生き物 | `server.animal.animalSneakRequiredEntities` | （空） | しゃがみ＋素手の時だけ弱点を出す、他 Mod の動物のエンティティ ID（バニラの馬・飼いならしたオオカミ等はコードで判定） |
| 新しい取引も解放 | `server.animal.villagerResetUnlocksNewTier` | false | 村人の取引上限のリセットで、新しい取引段階も解放するか（バニラの補充と同じ）。オフならロック解除のみ。**サーバー専用** |
| 隠れた弱点を透かす | `client.markers.animalSpotSeeThrough` | true | 自分の動物の弱点が体に隠れた時（ニワトリ等）、隠れた部分を薄く透かして表示するか。他プレイヤーのマークは透かさない |

## 釣り

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 釣りの弱点 | `server.fishing.fishingWeakSpotEnabled` | true | 釣りの弱点のオン／オフ |
| 待ち時間 0 のヒット数 | `server.fishing.fishingHits` | 6 | 最長の待ち時間（600 tick）を 0 にするまでのヒット数（1 ヒット = 600 tick ÷ この数）。0 以下で無効 |
| ヒットの最小間隔 | `server.fishing.fishingMinHitIntervalTicks` | 6 | 釣りヒットの最小受付間隔（tick） |

## 弓

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 弓の弱点 | `server.bow.bowWeakSpotEnabled` | true | 弓の弱点のオン／オフ |
| 1 ヒットで進む引き | `server.bow.bowHitTicks` | 5 | 1 ヒットごとに進める弓の引き絞りの tick 数（バニラの弓は 20 tick で引き切り。引き切りは超えない）。0 で無効 |
| ヒットの最小間隔 | `server.bow.bowMinHitIntervalTicks` | 4 | 弓ヒットの最小受付間隔（tick） |
| 引き具合のゲージ | `client.hud.bowDrawBarEnabled` | true | 弓を引いている間、照準の下に引き具合のゲージを表示するか。弓の弱点や HOME キーのオン／オフに関係なく表示 |

## 近接

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 近接の弱点 | `server.melee.meleeWeakSpotEnabled` | true | 近接の弱点のオン／オフ |
| ヒットの最小間隔 | `server.melee.meleeMinHitIntervalTicks` | 4 | 近接ヒットの最小受付間隔（tick） |
| 1 ヒットの溜め | `server.melee.meleeChargePerHit` | 0.25 | 1 ヒットごとの溜め量（次の攻撃の倍率に加算。0.05〜10.0）。コンボの掛け数を上乗せ |
| 溜めの上限 | `server.melee.meleeChargeMax` | 0 | 近接の溜めの上限。0 で上限なし（初期値） |
| 修理までの攻撃回数 | `server.melee.critsPerRepair` | 5 | 近接の溜めた攻撃が何回命中するごとに、手に持った物の耐久値を回復するか（余りはログアウトまで持ち越し）。0 で回復なし。**サーバー専用** |
| 1 回の修理量 | `server.melee.critRepairPerStep` | 1 | `critsPerRepair` ごとの回復量。**サーバー専用** |
| 溜めのゲージ | `client.hud.meleeChargeBarEnabled` | true | 近接の溜めのゲージ（銀）を照準の下に表示するか |

## 投擲物

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 投擲物の弱点 | `server.throw.throwWeakSpotEnabled` | true | 投擲物の弱点のオン／オフ |
| 1 ヒットの溜め | `server.throw.throwChargePerHit` | 0.5 | 1 ヒットごとの溜め量（投擲速度の倍率に加算。0.1〜10.0）。コンボの掛け数を上乗せ。上限なし |
| ヒットの最小間隔 | `server.throw.throwMinHitIntervalTicks` | 4 | 投擲物ヒットの最小受付間隔（tick） |
| 溜めのゲージ | `client.hud.throwChargeBarEnabled` | true | 投擲物の溜めのゲージ（青緑）を照準の下に表示するか |

## 飲食

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 飲食の弱点 | `server.eat.eatWeakSpotEnabled` | true | 飲食の弱点のオン／オフ |
| 1 ヒットで縮む時間 | `server.eat.eatHitTicks` | 16 | 1 ヒットごとに短縮する飲食時間（tick）。16 なら 2 ヒットで完了 |
| ヒットの最小間隔 | `server.eat.eatMinHitIntervalTicks` | 4 | 飲食ヒットの最小受付間隔（tick） |

## 乗り物

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 乗り物の弱点 | `server.vehicle.vehicleWeakSpotEnabled` | true | 乗り物の弱点のオン／オフ |
| 速さの倍率 | `server.vehicle.vehicleBoostMultiplier` | 1.5 | 乗り物の弱点に当てた時の速度倍率。コンボの掛け数（25 で ×1.25 … 300 で ×4、以降も上昇）を上乗せ |
| 倍率の上限 | `server.vehicle.vehicleBoostMaxMultiplier` | 0 | 乗り物の速度倍率の上限。0 で上限なし（初期値。コンボ 300 で 6 倍、以降も上昇） |
| 加速が続く時間 | `server.vehicle.vehicleBoostDurationTicks` | 40 | 乗り物の加速の持続時間（tick）。ヒットのたびにこの長さに戻る |
| ヒットの最小間隔 | `server.vehicle.vehicleMinHitIntervalTicks` | 6 | 乗り物ヒットの最小受付間隔（tick） |
| 加速のゲージ | `client.hud.vehicleBoostBarEnabled` | true | 乗り物の加速中、照準の上に残り時間のゲージ（水色）を表示するか |

## はしご

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| はしごの弱点 | `server.ladder.ladderWeakSpotEnabled` | true | はしごの弱点のオン／オフ |
| 速さの倍率 | `server.ladder.ladderBoostMultiplier` | 1.5 | はしごの弱点に当てた時の速度倍率。コンボの掛け数を上乗せ |
| 倍率の上限 | `server.ladder.ladderBoostMaxMultiplier` | 0 | はしごの速度倍率の上限。0 で上限なし（初期値） |
| 加速が続く時間 | `server.ladder.ladderBoostDurationTicks` | 40 | はしごの加速の持続時間（tick）。ヒットのたびにこの長さに戻る |
| ヒットの最小間隔 | `server.ladder.ladderMinHitIntervalTicks` | 6 | はしごヒットの最小受付間隔（tick） |
| 加速のゲージ | `client.hud.ladderBoostBarEnabled` | true | はしごの加速中、照準の上に残り時間のゲージ（茶色）を表示するか |

## ダッシュ

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| ダッシュの弱点 | `server.sprint.sprintWeakSpotEnabled` | true | ダッシュの弱点のオン／オフ |
| 速さの倍率 | `server.sprint.sprintBoostMultiplier` | 1.5 | ダッシュの弱点に当てた時の速度倍率。コンボの掛け数を上乗せ |
| 倍率の上限 | `server.sprint.sprintBoostMaxMultiplier` | 0 | ダッシュの速度倍率の上限。0 で上限なし（初期値） |
| 加速が続く時間 | `server.sprint.sprintBoostDurationTicks` | 40 | ダッシュの加速の持続時間（tick）。ヒットのたびにこの長さに戻る |
| ヒットの最小間隔 | `server.sprint.sprintMinHitIntervalTicks` | 6 | ダッシュヒットの最小受付間隔（tick） |
| 加速のゲージ | `client.hud.sprintBoostBarEnabled` | true | ダッシュの加速中、照準の上に残り時間のゲージ（赤）を表示するか |

## エリトラ

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| エリトラの弱点 | `server.elytra.elytraWeakSpotEnabled` | true | エリトラの弱点のオン／オフ |
| 1 ヒットで足す速さ | `server.elytra.elytraBoostPower` | 1.5 | 1 ヒットごとに加算する速度（ブロック/tick。0.1〜10.0）。コンボの掛け数を上乗せ。速度の上限なし |
| ヒットの最小間隔 | `server.elytra.elytraMinHitIntervalTicks` | 6 | エリトラヒットの最小受付間隔（tick） |

## ネザーゲート

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| ネザーゲートの弱点 | `server.portal.portalWeakSpotEnabled` | true | ネザーゲートの弱点のオン／オフ |
| 1 ヒットで縮む時間 | `server.portal.portalHitTicks` | 20 | 1 ヒットごとに短縮する転移の待ち時間（tick。1〜200）。コンボの掛け数を上乗せ |
| ヒットの最小間隔 | `server.portal.portalMinHitIntervalTicks` | 4 | ネザーゲートヒットの最小受付間隔（tick） |

## 泳ぎ

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 泳ぎの弱点 | `server.swim.swimWeakSpotEnabled` | true | 泳ぎの弱点のオン／オフ |
| 速さの倍率 | `server.swim.swimBoostMultiplier` | 1.5 | 泳ぎの弱点に当てた時の泳ぐ速さの倍率（コンボの掛け数を上乗せ）。乗り物と同じ値なら、同じだけ当てればボートの方が常に速い |
| 倍率の上限 | `server.swim.swimBoostMaxMultiplier` | 0 | 泳ぐ速さの倍率の上限。0 で上限なし |
| 加速が続く時間 | `server.swim.swimBoostDurationTicks` | 40 | 泳ぎの加速の持続時間（tick）。ヒットのたびにこの長さに戻る |
| 突進の距離 | `server.swim.swimDashDistance` | 0.3 | 当てた瞬間の突進で進む距離（ブロック、おおよそ）。コンボでは伸びない |
| ヒットの最小間隔 | `server.swim.swimMinHitIntervalTicks` | 6 | 泳ぎヒットの最小受付間隔（tick） |
| 加速のゲージ | `client.hud.swimBoostBarEnabled` | true | 泳ぎの加速中に、照準の上に残り時間のゲージ（深い青）を表示するか |

## 落下

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 落下の弱点 | `server.fall.fallWeakSpotEnabled` | true | 落下の弱点のオン／オフ |
| 出し始める距離 | `server.fall.fallMinDistance` | 3.0 | 着地した時の見込みの落下距離（今までの落下 + 地面までの高さ。ブロック）がこれを超えると、落ち始めから弱点を出す。3 を超えるとダメージを受ける |
| 1 ヒットで減らす距離 | `server.fall.fallReduceBlocks` | 3.0 | 1 回当てるごとに減らす落下距離（ブロック。コンボの掛け数を上乗せ）。着地のダメージは残りの距離から決まる |
| ヒットの最小間隔 | `server.fall.fallMinHitIntervalTicks` | 6 | 落下ヒットの最小受付間隔（tick） |
| 落下ダメージの見込み | `client.hud.fallDamageHintEnabled` | true | 落ちている間、照準の上に着地した時の見込みのダメージ（ハート）を表示するか |

## 睡眠

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| 睡眠の弱点 | `server.sleep.sleepWeakSpotEnabled` | true | 睡眠中の弱点のオン／オフ |
| 1 ヒットで進む時刻 | `server.sleep.sleepHitTicks` | 200 | 1 ヒットごとに進めるワールドの時刻（tick）。次の朝は越えない。**サーバー専用** |
| ヒットの最小間隔 | `server.sleep.sleepMinHitIntervalTicks` | 6 | 睡眠ヒットの最小受付間隔（tick） |

## エンチャント

| 名前 | ファイルのキー | 初期値 | 説明 |
|---|---|---|---|
| エンチャントの弱点 | `server.enchant.enchantWeakSpotEnabled` | true | エンチャントの弱点のオン／オフ |
| ヒットの最小間隔 | `server.enchant.enchantMinHitIntervalTicks` | 6 | エンチャントヒットの最小受付間隔（tick） |

## 作物の成長バー

成長の弱点を出している作物の足元に、成長度を示す**黄色のバー**が表示されます（プレイヤーの方を向き、左から伸びます）。年齢（`age`）を持つ作物、苗木（`stage`）、サトウキビ・サボテン（効果がかかる最上段の年齢 ÷ 15）に対応しています。設定は `growthBarEnabled`。

## 他プレイヤーのマークの形

他プレイヤーの弱点マークは、初期値では中抜きの**輪**です（自分の塗りつぶしの円と区別しやすくするため）。設定 `otherMarkerShape` で、円・ひし形・四角に変更できます。

## 管理コマンド（サーバー）

`top` と `bug` 以外は、権限レベル 2（OP）以上で使用できます。シングルプレイではチート有効時に使用可能です。対象はオンラインのプレイヤーのみです（オフラインのプレイヤーを指定すると案内が表示されます）。プレイヤー名は Tab キーで補完できます。

| コマンド | 内容 |
|---|---|
| `/weakspot` | 使い方を表示 |
| `/weakspot stats <プレイヤー>` | そのプレイヤーの統計（今回・累計）をチャットに表示 |
| `/weakspot reset <プレイヤー>` | そのプレイヤーの統計（今回と累計）を消去（統計画面の「統計をリセット」と同じ。節目も再度受け取り可能になる） |
| `/weakspot top [mining\|combo\|target\|total]` | サーバー内の上位 10 人を表示（mining: 採掘ヒット数、combo: 最大コンボ、target: 的当ての自己ベスト、total: 全種類のヒット数。省略時は mining。オフラインの人も含む）。**誰でも使用可能** |
| `/weakspot reload` | `config/weakspot.cfg` を再読み込みし、接続中の全員に設定を再送信（再起動せずに設定を反映） |
| `/weakspot bug` | 不具合の報告方法を表示（**誰でも使用可能**。クライアント側のコマンドで、サーバーには送信しない） |

## ブーストの目安

1 回のヒットで、通常の `(倍率 − 1) × 継続時間` tick 分だけ破壊が進みます（初期値では 12 tick ＝ 約 0.6 秒分）。約 0.6 秒ごとにヒットすると通常の約 2 倍の速さになり、`minHitIntervalTicks` の制限により最大で約 3 倍になります。
