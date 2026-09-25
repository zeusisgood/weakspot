# 設定

[← README に戻る](../README.md)

初回起動後に生成される `config/weakspot.cfg` で変更できます（サーバーでは、サーバーフォルダの `config/` に作られます）。ゲーム内では、タイトル画面の「Mods」→「Weak Spot Mining」→「設定」か、統計画面（K キー）の「設定画面を開く」で変えられます。

- **サーバー**の項目は、**サーバーの値が正**です。プレイヤーがログインしたときに、サーバーからクライアントへ送られ、接続している間はその値が使われます（切断すると自分の値に戻ります）。
  - 専用サーバーでは、サーバーの `config/weakspot.cfg` を書き換えて、`/weakspot reload`（OP のみ）を実行するか、サーバーを再起動してください。
  - シングルプレイと LAN のホストは、Mods メニューで変えられます（変えると、接続中の全員に送り直されます）。
  - 専用サーバーや他の人の LAN に接続している間は、自分の Mods メニューで変えたサーバーの項目は使われません（設定画面にもその旨が出ます）。
- **クライアント**の項目は、各自の設定として残ります。ヒット音の楽器・音量、他のプレイヤーのマークの表示・色・濃さ・形、コンボの表示、弱点の移動の演出、耐久バーと成長バー、弓の引きゲージです。接続中も、自分の値が使われます。

## サーバー

| キー | 初期値 | 説明 |
|---|---|---|
| `boostMultiplier` | 4.0 | ヒット時の破壊速度の倍率 |
| `boostDurationTicks` | 4 | 倍率を掛ける時間（tick） |
| `minHitIntervalTicks` | 6 | 採掘のヒットを受け付ける最小間隔（tick） |
| `weakSpotRadiusRatio` | 0.14 | 弱点の半径（面の短い辺に対する比率） |
| `weakSpotMinRadius` | 0.08 | 弱点の半径の下限（ブロック）。ボタンなどの小さい面でも当てやすくする。上限と食い違うときは上限を優先する |
| `weakSpotMaxRadiusRatio` | 0.35 | 弱点の半径の上限（面の短い辺に対する比率）。小さい面で弱点が面からはみ出さないようにする |
| `minFaceSize` | 0.15 | 弱点を出す面の、短い辺の最小の長さ（ブロック）。これより小さい面（カーペットの側面など）には出さない |
| `edgeMargin` | 0.1 | 弱点の外周とブロック面の縁の間に空ける最小の距離（ブロック）。大きいほど弱点が中央に寄る |
| `minMoveDistance` | 0.4 | ヒット後に弱点が移動する最小距離（ブロック） |
| `lingerTicks` | 40 | 長押しをやめた後に弱点が残る時間（tick） |
| `hitsPerRepair` | 5 | 壊したブロックで当てた採掘ヒット何回ごとに、手に持っているツールの耐久を回復するか（壊さずにやめたブロックのヒットは数えない。余りは次の破壊へ持ち越す）。0 で回復しない |
| `repairPerStep` | 1 | 1回で回復する耐久 |
| `maxRepairPerBreak` | 1 | 1回の破壊で回復する耐久の上限（上限を超えた分は持ち越さない）。0 で耐久回復が無効になる（節目の報酬は別） |
| `milestones` | 50, 100, 250, 500, 777, 1000, 1500 … 100000（30 個） | 節目にする採掘ヒットの回数。種類ごとの節目も同じ数字（1.7.0 で細かくした。1.6.x の初期値 100, 777, 1000, 10000 のままの設定ファイルは、自動で新しい数字になる） |
| `milestoneXp` | 5, 10, 15, 20, 77, 30, 40 … 1000（30 個） | 節目ごとの経験値（`milestones` の順に対応。足りない分は 0）。種類ごとの節目もこの値 |
| `milestoneRepair` | `milestoneXp` と同じ | 節目ごとの耐久回復（採掘の節目だけ。`milestones` の順に対応。足りない分は 0） |
| `milestoneRepeatInterval` | 50000 | `milestones` の一番大きい数より先は、この数ごとに節目にする（ごほうびは最後の値）。0 で繰り返さない。**サーバーだけが使う**（1.7.0） |
| `kindMilestonesEnabled` | true | 採掘以外の種類ごとのヒット数でも節目にするか（ごほうびは経験値だけ）。**サーバーだけが使う**（1.7.0） |
| `totalMilestonesEnabled` | true | すべての種類のヒット数の合計でも節目にするか。**サーバーだけが使う**（1.7.0） |
| `totalMilestones` | 1000, 5000, 7777, 10000, 25000, 50000, 77777, 100000, 250000, 500000, 777777, 1000000 | 合計の節目の数。**サーバーだけが使う**（1.7.0） |
| `totalMilestoneXp` | 100, 200, 777, 300, 500, 700, 7777, 1000, 1500, 2000, 7777, 5000 | 合計の節目ごとの経験値（`totalMilestones` の順に対応）。**サーバーだけが使う**（1.7.0） |
| `totalMilestoneRepeatInterval` | 500000 | `totalMilestones` の一番大きい数より先は、この数ごとに合計の節目にする。0 で繰り返さない。**サーバーだけが使う**（1.7.0） |
| `growthTicksPerHit` | 5 | 成長ヒット1回で、余分に進める成長の判定（randomTick）の回数 |
| `growthWarnings` | true | 作物・苗木の弱点に当てても育たないときに、チャットで知らせるか。サーバーだけが使う（1.4.3） |
| `growthStuckHits` | 30 | 何回続けて当てても変化がなかったら「何らかの外部要因で育たない」と知らせるか（5〜1000）。サーバーだけが使う（1.4.3） |
| `growthMinHitIntervalTicks` | 6 | 成長ヒットを受け付ける最小間隔（tick） |
| `growthMinRadius` | 0.08 | 作物・苗木などの弱点の最小の半径（ブロック） |
| `growthExcludedBlocks` | 草ブロック、草、背の高い草花 | 成長の弱点を出さないブロックの登録名（サトウキビ・サボテン・ネザーウォートにも効く） |
| `mushroomGrowChance` | 0.2 | キノコへの成長ヒット1回で、巨大キノコに育てようとする確率（0〜1）。**サーバーだけが使う**（クライアントには送らない） |
| `growthExtraBlocks` | サトウキビ、サボテン、ネザーウォート、IC2 のゴムの木 | 成長の弱点の対象に足すブロック（追加リスト）。1行に「登録名」か「登録名[プロパティ=条件,...]」（1.4.0。書き方は[成長の弱点](play.md#作物苗木などの成長素手で右クリックを押しっぱなし)を参照）。当てると randomTick を余分に呼んで早める |
| `machineBoostMultiplier` | 4.0 | 機械ヒット時に、機械の処理を毎 tick 何倍呼ぶか。コンボが続くと、これに掛け数（×1.25〜×4）を掛ける。クライアントにも送る（HUD の「機械 n倍速」。1.6.0） |
| `machineBoostParticles` | true | 加速中の機械のまわりに、速さに合わせた色の粒子を出すか（近くのプレイヤーに見える。各自の `machineParticlesVisible` がオフの人には送らない）。サーバーだけが使う（1.5.3） |
| `machineBoostMaxMultiplier` | 16.0 | 機械の倍率（コンボの掛け数を掛けたあと）の上限。機械 Mod の不具合やサーバーの負荷が気になるときに下げる。クライアントにも送る（HUD の「機械 n倍速」。1.6.0） |
| `machineBoostDurationTicks` | 6 | 機械の加速が続く時間（tick）。複数のプレイヤーが同じ機械を加速しても、効果は足さない（1.4.1 までの初期値は 4。4 のままの設定ファイルは、1.4.2 で自動で 6 に直す） |
| `machineMinHitIntervalTicks` | 6 | 機械ヒットを受け付ける最小間隔（tick） |
| `excludedBlocks` | 宝箱、トラップチェスト、エンダーチェスト、エンチャントテーブル、ビーコン、シュルカーボックス（16色） | 機械の加速の対象外にするブロックの登録名 |
| `animalBabyEnabled` | true | 動物の弱点で、子どもの成長を早めるか |
| `animalBreedingEnabled` | true | 動物の弱点で、繁殖の待ち時間を短くするか |
| `sheepWoolEnabled` | true | 羊の弱点で、羊毛の再生を早めるか |
| `chickenEggEnabled` | true | ニワトリの弱点で、次の卵までの時間を短くするか |
| `villagerTradeResetEnabled` | true | 村人の弱点で、ロックされた取引の上限をリセットするか |
| `animalBabyHits` | 40 | 子どもが大人になるまでの目安のヒット数（1ヒット = 24000 tick ÷ この数）。0 以下で無効 |
| `animalBreedingHits` | 10 | 繁殖の待ち時間が終わるまでの目安のヒット数（1ヒット = 6000 tick ÷ この数）。0 以下で無効 |
| `sheepWoolHits` | 10 | 毛を刈られた羊の毛が生えるまでのヒット数。0 以下で無効 |
| `chickenEggHits` | 15 | 次の卵までの目安のヒット数（1ヒット = 平均 9000 tick ÷ この数）。0 以下で無効 |
| `villagerTradeResetHits` | 10 | 村人の、ロックされた取引の上限がリセットされるまでのヒット数。0 以下で無効 |
| `animalMinHitIntervalTicks` | 6 | 動物のヒットを受け付ける最小間隔（tick） |
| `animalExcludedEntities` | （空） | 動物の弱点の対象外にするエンティティの ID（例: `minecraft:cow`） |
| `animalSneakRequiredEntities` | （空） | しゃがみ+素手のときだけ弱点を出す、他の Mod の動物のエンティティの ID（バニラの馬・飼いならしたオオカミなどは、コードで判定する） |
| `villagerResetUnlocksNewTier` | false | 村人の取引上限のリセットで、新しい取引の段階も解放するか（バニラの補充と同じ）。オフならロックの解除だけ。**サーバーだけが使う**（クライアントには送らない） |
| `fishingWeakSpotEnabled` | true | 釣りの弱点のオン・オフ |
| `fishingHits` | 6 | 釣りの、最長の待ち時間（600 tick）を 0 にするまでのヒット数（1ヒット = 600 tick ÷ この数）。0 以下で無効 |
| `fishingMinHitIntervalTicks` | 6 | 釣りのヒットを受け付ける最小間隔（tick） |
| `bowWeakSpotEnabled` | true | 弓の弱点のオン・オフ |
| `bowHitTicks` | 5 | 弓の弱点に1回当てるごとに進める、弓の引きの tick 数（バニラの弓は 20 tick で引き切る。引き切りは超えない）。0 で無効 |
| `bowMinHitIntervalTicks` | 4 | 弓のヒットを受け付ける最小間隔（tick） |
| `meleeWeakSpotEnabled` | true | 近接の弱点（敵を殴ってクリティカル）のオン・オフ |
| `meleeMinHitIntervalTicks` | 4 | 近接のヒットを受け付ける最小間隔（tick）。攻撃のゲージの条件もある |
| `meleeChargePerHit` | 0.25 | 近接の弱点に1回当てるごとに溜まる量（次の攻撃の倍率に足す。0.05〜10.0）。コンボの掛け数を上乗せする（1.8.0） |
| `meleeChargeMax` | 0 | 近接の溜めの上限。0 なら上限なし（初期値。1.8.0） |
| `portalWeakSpotEnabled` | true | ネザーゲートの弱点のオン・オフ（1.8.0） |
| `portalHitTicks` | 20 | ネザーゲートの弱点に1回当てるごとに縮める待ち時間（tick。1〜200）。コンボの掛け数を上乗せする（1.8.0） |
| `portalMinHitIntervalTicks` | 4 | ネザーゲートのヒットを受け付ける最小間隔（tick。1.8.0） |
| `vehicleWeakSpotEnabled` | true | 乗り物の弱点のオン・オフ（1.6.0） |
| `vehicleBoostMultiplier` | 1.5 | 乗り物の弱点に当てたときの速さの倍率。コンボの掛け数（25 で ×1.25 … 1000 で ×4）を上乗せする |
| `vehicleBoostMaxMultiplier` | 0 | 乗り物の速さの倍率の上限。0 なら上限なし（初期値。コンボ 1000 で 6 倍） |
| `vehicleBoostDurationTicks` | 40 | 乗り物の加速が続く時間（tick）。ヒットのたびにこの長さに戻す |
| `vehicleMinHitIntervalTicks` | 6 | 乗り物のヒットを受け付ける最小間隔（tick） |
| `eatWeakSpotEnabled` | true | 食事・飲み物の弱点のオン・オフ（1.6.0） |
| `eatHitTicks` | 16 | 食事・飲み物の弱点に1回当てるごとに縮める時間（tick）。16 なら 2 ヒットで食べ終わる |
| `eatMinHitIntervalTicks` | 4 | 食事・飲み物のヒットを受け付ける最小間隔（tick） |
| `sleepWeakSpotEnabled` | true | 寝ている間の弱点のオン・オフ（1.6.0） |
| `sleepHitTicks` | 200 | 寝ている間の弱点に1回当てるごとに進めるワールドの時刻（tick）。次の朝は越えない。**サーバーだけが使う** |
| `sleepMinHitIntervalTicks` | 6 | 寝ている間のヒットを受け付ける最小間隔（tick） |
| `ladderWeakSpotEnabled` | true | はしごの弱点のオン・オフ（1.7.0） |
| `ladderBoostMultiplier` | 1.5 | はしごの弱点に当てたときの速さの倍率。コンボの掛け数を上乗せする |
| `ladderBoostMaxMultiplier` | 0 | はしごの速さの倍率の上限。0 なら上限なし（初期値） |
| `ladderBoostDurationTicks` | 40 | はしごの加速が続く時間（tick）。ヒットのたびにこの長さに戻す |
| `ladderMinHitIntervalTicks` | 6 | はしごのヒットを受け付ける最小間隔（tick） |
| `elytraWeakSpotEnabled` | true | エリトラの弱点のオン・オフ（1.7.0） |
| `elytraBoostPower` | 1.5 | エリトラの弱点に1回当てるごとに足す速さ（ブロック/tick。0.1〜10.0）。コンボの掛け数を上乗せする。速さの上限はない |
| `elytraMinHitIntervalTicks` | 6 | エリトラのヒットを受け付ける最小間隔（tick） |
| `enchantWeakSpotEnabled` | true | エンチャントの弱点のオン・オフ（1.7.0） |
| `enchantMinHitIntervalTicks` | 6 | エンチャントのヒットを受け付ける最小間隔（tick） |
| `harvestWeakSpotEnabled` | true | 収穫の弱点のオン・オフ（1.7.0）。右クリックで収穫する Mod と重なるときはオフにする |
| `harvestMinHitIntervalTicks` | 4 | 収穫のヒットを受け付ける最小間隔（tick） |
| `harvestComboBonus` | true | 収穫で、コンボの掛け数だけ収穫物を増やすか（種は増やさない）。**サーバーだけが使う** |
| `throwWeakSpotEnabled` | true | 投げる物の弱点のオン・オフ（1.7.0） |
| `throwChargePerHit` | 0.5 | 投げる物の弱点に1回当てるごとに溜まる量（投げる速さの倍率に足す。0.1〜10.0）。コンボの掛け数を上乗せする。上限はない |
| `throwMinHitIntervalTicks` | 4 | 投げる物のヒットを受け付ける最小間隔（tick） |
| `sprintWeakSpotEnabled` | true | 走りの弱点のオン・オフ（1.7.0） |
| `sprintBoostMultiplier` | 1.5 | 走りの弱点に当てたときの速さの倍率。コンボの掛け数を上乗せする |
| `sprintBoostMaxMultiplier` | 0 | 走りの速さの倍率の上限。0 なら上限なし（初期値） |
| `sprintBoostDurationTicks` | 40 | 走りの加速が続く時間（tick）。ヒットのたびにこの長さに戻す |
| `sprintMinHitIntervalTicks` | 6 | 走りのヒットを受け付ける最小間隔（tick） |
| `critsPerRepair` | 5 | 近接の溜めた攻撃が何回当たるごとに（1.7.x まではクリティカル何回ごとに）、手に持っている物の耐久を回復するか（余りはログアウトまで持ち越す）。0 で回復しない。**サーバーだけが使う**（クライアントには送らない） |
| `critRepairPerStep` | 1 | `critsPerRepair` ごとに回復する耐久。**サーバーだけが使う**（クライアントには送らない） |
| `giveGuideBook` | true | 初めてログインしたプレイヤーに、遊び方のガイドの本を渡すか（1人1回）。**サーバーだけが使う**（クライアントには送らない） |
| `markerShareRange` | 16 | 他のプレイヤーの弱点マークが見える範囲（ブロック）。0 で見えなくなる |
| `markerSendMinIntervalTicks` | 2 | 弱点マークの状態を送る最小間隔（tick）。通信の多さを抑える |

`edgeMargin` を大きくすると、弱点が動ける範囲が狭くなり、`minMoveDistance` だけ離れた場所を取れないことがあります。その場合、弱点はできるだけ遠い場所へ移動します。

## クライアント

| キー | 初期値 | 説明 |
|---|---|---|
| `myHitSound` | PLING | 自分のヒット音の楽器。`XYLOPHONE` `CHIME` `BELL` `FLUTE` `GUITAR` `HARP` `BASS` `HAT` `SNARE` `BASEDRUM` `PLING` から選ぶ |
| `myHitVolume` | 0.25 | 自分のヒット音の音量（0〜1）。0 で聞こえなくなる |
| `othersHitSound` | XYLOPHONE | 他のプレイヤーのヒット音の楽器（選べるものは `myHitSound` と同じ） |
| `othersHitVolume` | 0.4 | 他のプレイヤーのヒット音の音量（0〜1）。0 で聞こえなくなる |
| `otherMarkerEnabled` | true | 他のプレイヤーの弱点マークを表示するか |
| `otherMarkerColor` | #3FA9FF | 他のプレイヤーの弱点マークの色（#RRGGBB）。読めない値のときは水色 |
| `otherMarkerAlpha` | 0.6 | 他のプレイヤーの弱点マークの濃さ（自分のマークの濃さに掛ける。1 で同じ） |
| `comboDisplayEnabled` | true | 連続ヒット（コンボ）の数を画面に表示するか |
| `comboScale` | 1.0 | コンボの数字の大きさの倍率（0.5〜2.0） |
| `comboPosition` | BELOW_CROSSHAIR | コンボの表示の位置。`BELOW_CROSSHAIR`（照準の下）、`RIGHT_OF_CROSSHAIR`（照準の右）、`TOP_CENTER`（画面の上の中央。ボスバーがあればその下）から選ぶ |
| `comboMilestoneEffects` | true | コンボが 10、25、50、100、250、500、1000（以降 1000 ごと）に達したときの演出（強調音・光・花火・タイトル）を出すか |
| `weakSpotTrailEnabled` | true | 弱点が移動するときの演出（古い位置から素早く動き、残像を残す）。見た目だけで、当たり判定は移動先で即時。オフにすると、その場で切り替わる。他のプレイヤーのマークにも効く |
| `growthBarEnabled` | true | 成長の弱点を出している作物の足元に、成長の進み具合を黄色のバーで表示するか |
| `otherMarkerShape` | RING | 他のプレイヤーの弱点マークの形。`CIRCLE`（塗りつぶした円）、`RING`（中抜きの輪）、`DIAMOND`（ひし形）、`SQUARE`（四角）から選ぶ。自分の弱点の形は `myMarkerShapes`（1.7.0） |
| `weakSpotsEnabled` | true | 自分の弱点のオン・オフ（HOME キーで切り替わる。1.3.3 までの初期値は J） |
| `blockHealthBarEnabled` | true | 掘っているブロックの残りの耐久を、面の下の余白に緑のバーで表示するか。自分が左クリックの長押しで掘っている、弱点が出るブロックだけに出る |
| `animalSpotSeeThrough` | true | 自分の動物の弱点が体に隠れたとき（ニワトリなど）、隠れた部分を薄く透かして表示するか。他のプレイヤーのマークは透かさない |
| `bowDrawBarEnabled` | true | 弓を引いている間、照準の下に引き具合のゲージを表示するか。弓の弱点や、弱点の一時オフ（HOME キー）に関係なく出る |
| `vehicleBoostBarEnabled` | true | 乗り物を加速している間、照準の上に残り時間のゲージ（水色）を表示するか（1.6.0） |
| `machineParticlesVisible` | true | 加速中の機械のまわりの色の粒子を、自分の画面に出すか（1.6.0。自分の機械の粒子も、ほかの人の機械の粒子も消える） |
| `machineBarEnabled` | true | かまど・醸造台・スポナーを叩いている間、上に進み具合（黄）と燃料（橙）のバーを表示するか（1.6.0） |
| `othersComboDisplay` | true | 近くのほかのプレイヤーのコンボ（10 以上）を、頭の上に「n HIT」と表示するか（1.6.0） |
| `showUpdateNotes` | true | 版が変わって初めてワールドに入ったときに、チャットに更新のお知らせを出すか（1.7.1） |
| `lastSeenVersion` | （空） | 最後に更新のお知らせを見た版（1.7.1）。Mod が書き換えるので、書き換えないでください |
| `ladderBoostBarEnabled` | true | はしごを加速している間、照準の上に残り時間のゲージ（茶色）を表示するか（1.7.0） |
| `meleeChargeBarEnabled` | true | 近接の溜めのゲージ（銀）を、照準の下に表示するか（1.8.0） |
| `throwChargeBarEnabled` | true | 投げる物の溜めのゲージ（青緑）を、照準の下に表示するか（1.7.0） |
| `sprintBoostBarEnabled` | true | 走りを加速している間、照準の上に残り時間のゲージ（赤）を表示するか（1.7.0） |
| `disabledKinds` | （空） | 自分でオフにした弱点の種類（1 行に 1 つ。`mining` `growth` `machine` `animal` `fishing` `bow` `melee` `vehicle` `eat` `sleep` `ladder` `elytra` `enchant` `harvest` `throw` `sprint` `portal`）。統計画面の「弱点マーカー」タブで変えられる（1.7.0） |
| `myMarkerColors` | （空） | 自分の弱点の色（1 行に `種類=#RRGGBB`）。書いていない種類は初期値。「弱点マーカー」タブで変えられる（1.7.0） |
| `myMarkerShapes` | （空） | 自分の弱点の形（1 行に `種類=circle`・`ring`・`diamond`・`square`）。書いていない種類は円。「弱点マーカー」タブで変えられる（1.7.0） |

## 作物の成長バー

成長の弱点を出している作物の足元に、成長の進み具合の**黄色のバー**が出ます（プレイヤーの方を向き、左から伸びます）。年齢（`age`）を持つ作物、苗木（`stage`）、サトウキビ・サボテン（弱点の効果がかかる一番上の節の年齢 ÷ 15）に対応します。設定 `growthBarEnabled`。

## 他のプレイヤーのマークの形

他のプレイヤーの弱点マークの形は、初期値で中抜きの**輪**です（自分のマークの塗りつぶした円と区別しやすくするため）。設定 `otherMarkerShape` で、円・ひし形・四角に変えられます。

## 管理コマンド（サーバー）

権限レベル 2（OP）以上が使えます。シングルプレイでは、チートがオンのときに使えます。対象はオンラインのプレイヤーだけです（オフラインのプレイヤーを指定すると案内が出ます）。プレイヤー名はタブキーで補完できます。

| コマンド | 内容 |
|---|---|
| `/weakspot` | 使い方を表示する |
| `/weakspot stats <プレイヤー>` | そのプレイヤーの統計（今回と累計）をチャットに表示する |
| `/weakspot reset <プレイヤー>` | そのプレイヤーの統計の累計を消す（統計画面の「累計をリセット」と同じ。節目も、もう一度受け取れるようになる） |
| `/weakspot reload` | `config/weakspot.cfg` を読み直し、接続中の全員に設定を送り直す（再起動せずに設定を反映する） |
| `/weakspot bug` | 不具合の報告の方法を出す（1.7.1。**誰でも使える**。自分のクライアントのコマンドで、サーバーには送らない） |

## ブーストの目安

1回のヒットで、通常の `(倍率 − 1) × 継続時間` tick 分だけ破壊が進みます（初期値では 12 tick = 約0.6秒分）。約0.6秒ごとにヒットすると通常の約2倍の速さになり、`minHitIntervalTicks` の制限により最大で約3倍になります。
