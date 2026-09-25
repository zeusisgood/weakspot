# 弱点破壊 Mod 追加仕様書（v1.7）

`SPEC_v1.6.4.md`（Mod 1.6.4）に対する**マイナー**の仕様。ここに書かれていないことは、それと現行実装のままとする。
仕様書は、リポジトリの `doc/` ディレクトリに置く（この仕様書は `doc/spec/SPEC_v1.7.md`）。
仕様と食い違う実装をする場合は、`CLAUDE.md` のリリースの流れ（止まる条件）に従う。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.6.4 がリリース済み。1.6.4 の内容を土台にする
- この仕様書の内容は、Mod のバージョン **1.7.0** として、リリースする

---

## 0. バージョンと互換性

- バージョンは 1.6.4（またはその後のパッチ）→ **1.7.0**（`build.gradle` の `version` と `WeakSpotMod.VERSION` の2か所を、そろえて変える）
- **通信内容が変わる**（`HitKind` に `LADDER`・`ELYTRA`・`ENCHANT`・`HARVEST`・`THROW`・`SPRINT` を足す、`SyncedSettings` にはしご・エリトラ・エンチャント・収穫・投げる物・走りの設定を足す、`StatsMessage` に `ladderHits`・`elytraHits`・`enchantHits`・`harvestHits`・`throwHits`・`sprintHits` を足す、`MilestoneMessage` に節目の種類を足す、`SwitchMessage` にオフの種類を足す）。そのため 1.6.x とは接続できない
  - `WeakSpotMod.ACCEPTED_VERSIONS` を `[1.7,1.8)` にする
  - README の更新履歴に、1.6.x とは接続できないこと（サーバーとクライアントを同時に更新すること）を書く
- 1.6.x と接続できないので、この仕様書の機能は `ServerFeatures.since` で囲まなくてよい（1.7.x のサーバーはすべて持つ）
- 設定 `milestones` / `milestoneXp` / `milestoneRepair` の初期値を変える（§4）。古い初期値のままのファイルは、`WeakSpotConfig.migrate` の `configVersion` 5 で新しい初期値に置き換える（書き換えてある値はそのまま）

## 1. はしごの弱点（新しい種類 `HitKind.LADDER`）

### 1.1 対象と出し方（クライアント、`client/LadderSpot`）

- 対象: **登れるブロック**にいる間。判定はバニラの `EntityLivingBase#isOnLadder`（Forge の `ForgeHooks.isLivingOnLadder`。はしご・ツタ・`Block#isLadder` を返す Mod のブロックがすべて入る）
- **登っている・降りている間だけ**出す: 1 tick の縦の移動の大きさ（`posY − prevPosY` の絶対値）が 0.05 ブロック以上のとき。しゃがんで止まっている・はしごの上で立ち止まっている間は出さない
- 次のときは出さない: 何かを使っている（弓を引く・食べる。そちらの弱点を出す）、乗り物に乗っている、クリエイティブ・スペクテイター、弱点の一時オフ（HOME キー）、設定 `ladderWeakSpotEnabled` が false
- 乗り物・食事と同じく、**HUD に画面上で一定の大きさの円**で出し（`client/HudSpot`）、**照準を合わせるだけでヒット**（クリックは要らない）
- 向き: **照準の真上か真下だけ**（`HudSpot` の「上下だけ」。馬・豚と同じ。pitch を 10〜20 度ずらし、yaw は毎フレームの視線に合わせる）
  - 理由: はしごは前に進むキーではしごに押し付けて登るので、左右に向きを変えると、はしごから横に外れてしまう。上下だけなら、狙っても進む向きは変わらない
  - 相談では「照準から 10〜20 度（全方向）」だったが、この理由で上下だけにした（ユーザーの確認済み）
- 円の色は**木の茶色 `#C8A060`**（弓のオレンジ `#FF8C42`、乗り物の水色 `#55CCFF`、食事の緑 `#7CFC00` と見分ける）
- ヒットの間隔は `ladderMinHitIntervalTicks`（**[サーバー]**、`SyncedSettings`。初期値 6）

### 1.2 効果（加速）

- 1 ヒットで、**2 秒（`ladderBoostDurationTicks`、初期値 40 tick）の間、登り降りの速さ ×1.5**（`ladderBoostMultiplier`、初期値 1.5）。残り時間は、ヒットのたびに 40 tick に戻す（乗り物と同じ）
- **コンボの上乗せあり**: 倍率 = `ladderBoostMultiplier` × コンボの掛け数（`MachineComboBoost.factor`。25 で ×1.25 … 1000 で ×4。初期値ならコンボ 1000 で **6 倍**）。計算は乗り物と同じ `VehicleBoostMath.multiplier` を使う
  - **上限は初期値ではなし**。設定 `ladderBoostMaxMultiplier`（初期値 **0 = 上限なし**、0 か 1.0〜100.0）に数を書くと、その倍率で抑える
- **登りと降りの両方**を速くする
- **速さはクライアントで足す**（プレイヤーの動きはクライアントが決めるので、ボートと同じ方式）: 加速中、`ClientTickEvent` END で、その tick の縦の移動（`motionY` ではなく、実際に動いた `posY − prevPosY`）に (倍率 − 1) を掛けた分を `EntityPlayerSP#move(MoverType.SELF, 0, dy, 0)` で足す
  - 登れるブロックにいて、登り降りしている（§1.1 の条件）tick だけ足す。はしごから外れたら足さない（加速の残り時間は残る。戻ればまた効く）
  - 1 tick に足す量に上限は付けない（ユーザーの方針: 速さの上限は基本的に付けない。倍率 6 でも登りは約 0.6 ブロック/tick）
  - 天井・はしごの上の端では、`move` の当たり判定で止まる
- サーバーは効果をかけない。ヒットの検証、コンボ（`ServerStats.countStreak`）、統計、ヒット音の転送だけを受け持つ
- 照準の上に、**加速の残り時間のゲージ**（茶色 `#C8A060`、背景は乗り物と同じ半透明の黒 `#1E1E1E`）を、乗り物のゲージと同じ位置に出す（はしごと乗り物は同時に起きないので重ならない）。加速していないとき・はしごから外れているときは出さない。設定 `ladderBoostBarEnabled`（**[クライアント]**、初期値 true）

### 1.3 通信・検証（サーバー、`server/LadderHits`）

- ヒットは `HitMessage`（種類に `LADDER`、対象なし。`HitMessage.withoutTarget`）。`HitKind` の**末尾**に `LADDER` を足す
- サーバーは次を確かめて受け付ける:
  - 弱点のオン・オフ（`ServerSwitches.isEnabled`）、`ladderWeakSpotEnabled`、クリエイティブ・スペクテイターでない、乗り物に乗っていない
  - 登れるブロックにいる: サーバーの `isOnLadder` が、**直前 10 tick のどこかで** true だった（サーバーの位置はクライアントより遅れて届くので、今の tick だけで見ない）。サーバーは `PlayerTickEvent` で、プレイヤーごとに最後に登れるブロックにいた tick を覚える
  - 間隔（`ladderMinHitIntervalTicks` を、ほかの種類と同じく 2 tick 甘く）
- 受け付けたら `ServerStats.countStreak`、統計の `ladderHits` を足し、`ServerBoostTracker.notifyNearbyPlayers`（鳴らす位置はプレイヤーの位置）
- ヒット音・コンボ・他のプレイヤーのヒット音は、ほかの種類と同じ。他のプレイヤーにマークは見せない（乗り物・食事と同じ）

### 1.4 設定

| キー | 種類 | 初期値 | 説明 |
|---|---|---|---|
| `ladderWeakSpotEnabled` | [サーバー]（同期） | true | はしごの弱点を出すか |
| `ladderBoostMultiplier` | [サーバー]（同期） | 1.5 | 1 ヒットの倍率（コンボで上乗せ） |
| `ladderBoostMaxMultiplier` | [サーバー]（同期） | 0 | 倍率の上限。0 = 上限なし。0 か 1.0〜100.0 |
| `ladderBoostDurationTicks` | [サーバー]（同期） | 40 | 加速が続く tick 数（ヒットのたびに戻す） |
| `ladderMinHitIntervalTicks` | [サーバー]（同期） | 6 | ヒットの最小間隔（tick） |
| `ladderBoostBarEnabled` | [クライアント] | true | 残り時間のゲージを出すか |

- `[サーバー]` の 5 つは、速さをクライアントで足すので、すべて `SyncedSettings` で送る
- `ladderBoostBarEnabled` の設定画面の説明（`weakspot.general.ladderboostbarenabled.tooltip`）を `en_us.lang` と `ja_jp.lang` に足す

### 1.5 統計

- **はしごヒット数**（`ladderHits`）を足す（`StatsMessage`、保存は `PlayerPersisted` の `weakspot` の新しいキー。古い版は無視する）
- 統計画面と `/weakspot stats` にも出す（翻訳キー `weakspot.stats.ladderHits`: 「はしごヒット数」/「Ladder hits」）

### 1.6 実装時の確認事項

- 足した縦の移動で、サーバーの「動きが速すぎる」「動きがおかしい」（`moved too quickly` / `moved wrongly`）に引っかからないか。コンボ 1000（6 倍）で確かめる
- ぶら下がったツタ（壁に付いていない）で降りるとき、空中に浮いている判定（`floating too long` のキック）に引っかからないか
- 速く降りたとき、はしごの下で落下のダメージを受けないか（バニラははしごの上で `fallDistance` を 0 にするが、足した `move` の分がはしごを外れた tick に残らないか）
- Mod の登れるブロックで `isOnLadder` がクライアントとサーバーで同じ結果になるか

## 2. エリトラの弱点（新しい種類 `HitKind.ELYTRA`）

### 2.1 対象と出し方（クライアント、`client/ElytraSpot`）

- 対象: エリトラで滑空している間（`EntityLivingBase#isElytraFlying`）
- 次のときは出さない: 何かを使っている（弓を引く・食べる）、クリエイティブ・スペクテイター、弱点の一時オフ（HOME キー）、設定 `elytraWeakSpotEnabled` が false
- 乗り物・食事と同じく、**HUD に画面上で一定の大きさの円**で出し（`client/HudSpot`）、**照準を合わせるだけでヒット**
- 向き: 照準から **10〜20 度の全方向**（弓と同じ。`BowMath.offsetRange`）。エリトラは見ている向きに飛ぶので、狙うと飛ぶ向きが少しぶれる。これも操作のうちとする
- 円の色は**空の青 `#7FB2FF`**
- ヒットの間隔は `elytraMinHitIntervalTicks`（**[サーバー]**、`SyncedSettings`。初期値 6）

### 2.2 効果（トライデントの激流のような急加速）

- イメージは、1.13 以降のトライデントの「激流（Riptide）」: **当てた瞬間に、見ている向きへ一気に飛び出す**（時間で続く加速ではない）
- 足す速さ = `elytraBoostPower`（初期値 **1.5 ブロック/tick**。激流 I と同じ強さ）× コンボの掛け数（`MachineComboBoost.factor`。25 で ×1.25 … 1000 で ×4 = 6 ブロック/tick）
  - 今の速さに足す（激流と同じく `addVelocity` の形）。見ている向きと今の向きが違えば、そちらへ曲がる
  - **速さの上限は付けない**（ユーザーの方針）
  - 目安: バニラの花火は、しばらくかけて約 1.7 ブロック/tick まで上げる。初期値では 1 回で花火 1 本に近い速さが一瞬で出る
- **速さはクライアントで足す**（エリトラの動きは本人のクライアントが決めるので、ボート・はしごと同じ方式）: ヒットしたとき、`EntityPlayerSP` の `motionX/Y/Z` に足す。その後の減速はバニラのエリトラの空気抵抗に任せる
- 急加速の演出（自分の画面だけ）:
  - **視野が一瞬広がる**: `FOVUpdateEvent` で、ヒットから 10 tick かけて、視野を 1.25 倍から元に戻す（ease-out）。（1.12 には「視野の変化」の強さの設定がないので、いつも同じ強さ）
  - **粒子の尾**: ヒットから 10 tick、自分の後ろに雲の粒子（`CLOUD`）を毎 tick 2 個と、当てた瞬間に花火の火花（`FIREWORKS_SPARK`）を 8 個
  - **音**: 当てた瞬間に、花火の打ち上げの音（`ENTITY_FIREWORK_LAUNCH`）を少し低め（ピッチ 0.8）で鳴らす（1.12 にはトライデントの音がないため）。ヒット音（楽器）とは別に鳴らす
- 残り時間のゲージはない（時間で続く加速ではないため）
- 壁にぶつかったときのダメージ（バニラの「エリトラで壁に激突」）は、バニラのまま受ける（速いほど大きい）

### 2.3 通信・検証（サーバー、`server/ElytraHits`）

- ヒットは `HitMessage`（種類に `ELYTRA`、対象なし）
- サーバーは、オン・オフ、`elytraWeakSpotEnabled`、クリエイティブ・スペクテイターでない、**直前 10 tick のどこかで `isElytraFlying` だった**（はしごと同じく `PlayerTickEvent` で覚える）、間隔（2 tick 甘く）を確かめて受け付ける
- 受け付けたら `ServerStats.countStreak`、統計の `elytraHits`、`ServerBoostTracker.notifyNearbyPlayers`（鳴らす位置はプレイヤー）

### 2.4 設定

| キー | 種類 | 初期値 | 説明 |
|---|---|---|---|
| `elytraWeakSpotEnabled` | [サーバー]（同期） | true | エリトラの弱点を出すか |
| `elytraBoostPower` | [サーバー]（同期） | 1.5 | 1 ヒットで足す速さ（ブロック/tick。コンボで上乗せ）。0.1〜10.0 |
| `elytraMinHitIntervalTicks` | [サーバー]（同期） | 6 | ヒットの最小間隔（tick） |

### 2.5 実装時の確認事項

- マルチで、速く飛んでもサーバーに引き戻されない（`moved too quickly` / `moved wrongly`）か。コンボ 1000 で確かめる。バニラのサーバーは、エリトラでは 1 tick に約 17 ブロックを越える動きを引き戻す（シングルプレイのホストは対象外）。連続で当てて届くようなら、上限は付けずに、ユーザーに報告する（止まる条件）
- 速いとき、チャンクの読み込みが追いつくか（追いつかなくても上限は付けない。README の既知の問題に書く）

## 3. エンチャントの弱点（新しい種類 `HitKind.ENCHANT`）

### 3.1 出し方（クライアント、`client/EnchantSpot`）

- エンチャント台の画面（`GuiEnchantment`）で、**候補が出ている間**（台に物が置いてあり、3 つの候補のどれかにレベルが出ている）、画面にマーカーを出し、**クリックで当てる**（睡眠と同じ。`GuiScreenEvent.DrawScreenEvent.Post` で描き、`MouseInputEvent.Pre` で左クリックの押下が円の中ならキャンセルしてヒット）
- 位置: エンチャント台の画面の**枠（176×166）の外**のランダムな位置（枠の中に出すと、スロットや候補のクリックを邪魔するため）。枠から 8 GUI ピクセル以上離し、画面の端から 16 以上内側。当てたら、前の位置から画面の高さの 1/4 以上離れた所へ動かす（残像つき、`weakSpotTrailEnabled`）
- 大きさは睡眠と同じ。色は**紫 `#B070FF`**
- 弱点の一時オフ（HOME キー）・設定 `enchantWeakSpotEnabled` が false のときは出さない
- ヒットの間隔は `enchantMinHitIntervalTicks`（**[サーバー]**、`SyncedSettings`。初期値 6）

### 3.2 注釈（ユーザーの提案）

- エンチャント台の画面の枠のすぐ上に、紫 `#B070FF` の小さい文字で、1 行の注釈を出す:
  - 日本語: 「◎ に当てると、候補を引き直せます（n 回）」
  - 英語: "Hit the ◎ to reroll the offers (n)"
  - n は、この画面を開いてから引き直した回数。0 回のときは「（n 回）」を付けない
- 候補が出ていない間（台が空）は、「物を置くと、弱点で候補を引き直せます」を出す（マーカーは出さない）
- 翻訳キーは `weakspot.enchant.hint` / `weakspot.enchant.hintCount` / `weakspot.enchant.hintEmpty`
- 設定は足さない（弱点の一時オフの間は、注釈も出さない）

### 3.3 効果（候補の引き直し）

- 1 ヒットで、**3 つの候補を引き直す**。**何もいらない**（ラピスラズリも経験値も減らない）。必要なレベルは下げない
- 仕組み（サーバー、`server/EnchantHits`）: バニラの候補は、プレイヤーの「エンチャントの種」（`EntityPlayer` の非公開の `xpSeed`。バニラはエンチャントするたびに引き直す）と、台のまわりの本棚の数で決まる。ヒットで
  1. プレイヤーの `xpSeed` を新しい乱数にする（`Reflect.field`。MCP `xpSeed`、SRG は実装時に `fields.csv` で調べる）
  2. 開いているコンテナ（`ContainerEnchantment`）の `xpSeed` も同じ値にする
  3. `onCraftMatrixChanged(tableInventory)` を呼んで、候補を計算し直す（バニラの送信でクライアントの画面に届く）
- 種は保存されるプレイヤーの値なので、画面を閉じても引き直したまま（バニラでエンチャントしたあとと同じ）
- 読めない（リフレクションに失敗した）ときは、エンチャントの弱点だけ出さない（サーバーは受け付けない。クライアントは、`SyncedSettings` の `enchantWeakSpotEnabled` をサーバーが false にして送る）

### 3.4 通信・検証

- ヒットは `HitMessage`（種類に `ENCHANT`、対象なし）
- サーバーは、オン・オフ、`enchantWeakSpotEnabled`、開いているコンテナが `ContainerEnchantment` で台に物が置いてあること、間隔（2 tick 甘く）を確かめる
- 受け付けたら `ServerStats.countStreak`、統計の `enchantHits`、`ServerBoostTracker.notifyNearbyPlayers`（鳴らす位置はエンチャント台）

### 3.5 設定

| キー | 種類 | 初期値 | 説明 |
|---|---|---|---|
| `enchantWeakSpotEnabled` | [サーバー]（同期） | true | エンチャントの弱点を出すか |
| `enchantMinHitIntervalTicks` | [サーバー]（同期） | 6 | ヒットの最小間隔（tick） |

## 4. 節目を増やす

### 4.1 3 つの節目

| 節目 | 数えるもの | 数字 | ごほうび |
|---|---|---|---|
| **採掘**（今まであるもの） | 累計の採掘ヒット数（`hits`） | `milestones`（初期値を §4.2 に変える） | 経験値（`milestoneXp`）とツールの耐久回復（`milestoneRepair`）。今までどおり |
| **種類ごと**（新しい） | それぞれの累計のヒット数: 成長 `growthHits`・機械 `machineHits`・動物 `animalHits`・釣り `fishingHits`・弓 `bowHits`・近接 `critHits`・乗り物 `vehicleHits`・食事 `eatHits`・睡眠 `sleepHits`・はしご `ladderHits`・エリトラ `elytraHits`・エンチャント `enchantHits`・収穫 `harvestHits`・投げる物 `throwHits`・走り `sprintHits` | **採掘と同じ** `milestones` | **経験値だけ**（`milestoneXp`） |
| **合計**（新しい） | すべての種類の累計のヒット数の合計 | `totalMilestones`（§4.2） | 経験値（`totalMilestoneXp`） |

- 1 回のヒットで、種類ごとの節目と合計の節目に同時に達したら、両方のごほうびをもらう。演出は合計のほうを出し、チャットは両方
- 判定は今と同じ `Milestones.reached`（増えたあとの値が節目の数字と一致したか）。どの累計も 1 ヒットで 1 ずつ増える
- 統計の累計のリセット（`/weakspot reset`、統計画面）で、種類ごと・合計の節目も受け取り直せる（1.6.1 の採掘と同じ）
- 設定 `kindMilestonesEnabled`（[サーバー]、サーバーだけ。初期値 true）と `totalMilestonesEnabled`（同じく true）で止められる

### 4.2 数字（「もっと刻む」）

- **`milestones`（採掘と種類ごと）** の初期値（30 個）:
  50 / 100 / 250 / 500 / **777** / 1000 / 1500 / 2000 / 2500 / 3000 / 4000 / 5000 / 6000 / 7000 / **7777** / 8000 / 9000 / 10000 / 15000 / 20000 / 25000 / 30000 / 40000 / 50000 / 60000 / 70000 / **77777** / 80000 / 90000 / 100000
  - **100000 より先は 50000 ごと**（150000、200000 …）。新しい設定 `milestoneRepeatInterval`（[サーバー]、サーバーだけ。初期値 50000、0 = 繰り返さない）。ごほうびは `milestoneXp` / `milestoneRepair` の最後の値
- **`milestoneXp` / `milestoneRepair`** の初期値（同じ順。2 つとも同じ値）:
  5 / 10 / 15 / 20 / **77** / 30 / 40 / 40 / 40 / 40 / 40 / 50 / 50 / 50 / **777** / 50 / 50 / 100 / 100 / 100 / 150 / 150 / 150 / 200 / 200 / 200 / **7777** / 200 / 200 / 1000（繰り返しの分も 1000）
- **`totalMilestones`（合計）** の初期値: 1000 / 5000 / **7777** / 10000 / 25000 / 50000 / **77777** / 100000 / 250000 / 500000 / **777777** / 1000000。1000000 より先は 500000 ごと（`totalMilestoneRepeatInterval`、初期値 500000）
- **`totalMilestoneXp`** の初期値: 100 / 200 / **777** / 300 / 500 / 700 / **7777** / 1000 / 1500 / 2000 / **7777** / 5000（繰り返しの分も 5000）
- 配列の長さが合わないときは、今と同じく起動時にログで警告する（足りない分のごほうびは 0）

### 4.3 演出（クライアント、`MilestoneEffects`）

- 今の演出（タイトル、チャット 1 行、花火、音階の駆け上がり）を、3 つの節目で使う。タイトルの文字:
  - 採掘: 今までどおり「n ヒット！」
  - 種類ごと: 「**弓 n ヒット！**」（種類の名前を付ける。翻訳キー `weakspot.kind.<種類>`）
  - 合計: 「**合計 n ヒット！**」。花火を 3 発、タイトルを金 `#FFD700` にして長く出す（コンボ 1000 と同じくらい派手に）
- **7 だけが並ぶ数（777、7777、77777、777777）は虹色**で派手にする（今の 777 と同じ。数字で判定する）
- `MilestoneMessage` に節目の種類（採掘・種類ごと（`HitKind`）・合計）を足す

### 4.4 README・ガイドの本

- ガイドの本の 3 ページ目「耐久回復と節目」の「100、777、1000、10000回」を、「50 回から 10 万回まで細かく、その先も」「採掘以外の種類と、全部の合計にも節目がある」に書き直す

## 5. 収穫の弱点（新しい種類 `HitKind.HARVEST`）

### 5.1 対象

- **実った作物**:
  - `BlockCrops` を継承したもの（小麦・ニンジン・ジャガイモ・ビートルートと、Mod の作物）で `isMaxAge` が true
  - ネザーウォート（`age` = 3）、カカオ豆（`age` = 2）
- 対象外: カボチャ・スイカ（茎は残して実だけ取るので、バニラどおり壊す）、サトウキビ・サボテン、`growthExtraBlocks` の作物（実った状態の決まった読み方がないため）
- まだ育つ作物は、今までどおり成長の弱点の対象（実ったものとは重ならない）

### 5.2 出し方（クライアント）

- 成長の弱点と同じ操作: **素手（メインハンドが空）**で、実った作物に右クリックを**押しっぱなし**にすると出る。種・骨粉などを持っているときは出さない（バニラどおり）
- 出す面・大きさ・当たり判定は成長の弱点と同じ（`FaceMath.growthFaceAxis`、`growthMinRadius`）。照準を合わせるだけでヒット（毎フレーム）
- 色は**マゼンタ `#FF3DCB`**（輪と中心は、ほかのマークと同じく白に寄せる `MarkerColor.towardWhite`）
  - 選んだ理由: バニラの作物は、黄（小麦）・橙（ニンジン）・黄土色（ジャガイモ）・赤（ビートルート・ネザーウォート）・茶（カカオ豆）と葉の緑。ピンク系の作物はないので、どれの上でもはっきり見える。成長の弱点（橙赤）とも見分けられ、「育てる」と「収穫する」がひと目で分かる
- **押しっぱなしで続けて収穫してよい**（ユーザーの判断）: 収穫して植え直すと、その場で成長の弱点（年齢 0 の作物）に切り替わる。クライアントは、毎フレーム、そのブロックの状態から種類（成長・収穫）を決め、変わったら出し直す
- 対象を右クリックしたら、成長と同じく `RightClickBlock` をキャンセルして、バニラの動作を止める（両側、`RightClickTargets`）
- 他のプレイヤーには、成長の弱点と同じくブロックのマークとして見せる（`OtherMarkers`）
- ヒットの間隔は `harvestMinHitIntervalTicks`（**[サーバー]**、`SyncedSettings`。初期値 4）

### 5.3 効果（サーバー、`server/HarvestHits`）

- 検証は成長と同じ（`RightClickHits`: 直前 10 tick 以内にそのブロックを右クリックしたか、届く距離か、間隔、オン・オフ、`harvestWeakSpotEnabled`、今も実っているか）
- 受け付けたら、次の順に行う:
  1. `BlockEvent.BreakEvent` を出す。キャンセルされたら（保護の Mod などで）収穫しない
  2. 収穫物を作る: `Block#getDrops`（幸運 0）と、`ForgeEventFactory.fireBlockHarvesting`（ほかの Mod が収穫物を変えられるように）
  3. **植え直し**: 収穫物から「種」を 1 つ取り除き、作物を年齢 0 に戻す（`BlockCrops#withAge(0)`、ネザーウォート・カカオ豆は `age` を 0 に。カカオ豆は向きを保つ）
     - 「種」は、そのブロックをピックしたときの物（Forge の `Block#getPickBlock`。小麦なら小麦の種、ニンジンならニンジン）
     - 収穫物に種がなかったとき（バニラの小麦は 1 割くらいで種が出ない）は、**取り除かずにそのまま植え直す**（おまかせで決めた。不運で畑に穴が空かないように）
  4. **コンボのおまけ**: 種を除いた収穫物の数に、コンボの掛け数（`MachineComboBoost.factor(コンボ)`。25 で ×1.25 … 1000 で ×4）を掛ける。端数は確率で 1 つ足す（×1.25 なら 4 回に 1 回 +1）。上限はない
     - 増やすのは収穫物だけ。小麦の種・ビートルートの種のように、収穫物と別の種は増やさない（持ち物が種であふれないように）。ニンジン・ジャガイモ・ネザーウォート・カカオ豆は、植え直しに使った 1 つを除いた残りを増やす
     - 計算（数と掛け数から、増やしたあとの数を決める。乱数を渡す）は `common/HarvestBonus` に置き、単体テストを書く
  5. 収穫物をブロックの位置に落とす（`Block.spawnAsEntity`）。壊れる音と粒子（`World#playEvent(2001, …)`）を出す
- `ServerStats.countStreak`（コンボ）を先に呼び、その数でおまけを決める。統計の `harvestHits` を足し、`ServerBoostTracker.notifyNearbyPlayers`（鳴らす位置は作物）

### 5.4 設定

| キー | 種類 | 初期値 | 説明 |
|---|---|---|---|
| `harvestWeakSpotEnabled` | [サーバー]（同期） | true | 収穫の弱点を出すか。Quark など、右クリックで収穫する Mod と重なるときはオフに |
| `harvestMinHitIntervalTicks` | [サーバー]（同期） | 4 | ヒットの最小間隔（tick） |
| `harvestComboBonus` | [サーバー]（サーバーだけ） | true | コンボのおまけで収穫物を増やすか |

### 5.5 統計・節目

- **収穫ヒット数**（`harvestHits`）を足す（`StatsMessage`、保存は `PlayerPersisted` の `weakspot` の新しいキー）。統計画面と `/weakspot stats` にも出す
- 種類ごとの節目（§4）の対象にする

### 5.6 実装時の確認事項

- Quark など、右クリックで収穫する Mod と一緒に入れたとき、二重に収穫しないか（どちらが先に動くか）
- Mod の作物（`BlockCrops` を継承したもの）で、ピックした物が種になっているか。なっていない作物は、種を取り除かずに植え直す（上の「種がなかったとき」と同じ）

## 6. 投げる物の弱点（新しい種類 `HitKind.THROW`）

### 6.1 対象と出し方（クライアント、`client/ThrowSpot`）

- 対象: メインハンドに**投げる物**を持っている間。エンダーパール・雪玉・卵・スプラッシュポーション・残留ポーション・エンチャントの瓶（`ItemEnderPearl` / `ItemSnowball` / `ItemEgg` / `ItemSplashPotion` / `ItemLingeringPotion` / `ItemExpBottle` を継承したもの）
- 弓と同じく、**HUD に画面上で一定の大きさの円**で出し（`client/HudSpot`）、**照準を合わせるだけでヒット**。向きは照準から 10〜20 度（`BowMath.offsetRange`）。馬・豚に乗っているときは上下だけ（騎射と同じ）
- 色は**ティール `#2ED3B7`**
- 次のときは出さない: 何かを使っている（弓を引く・食べる）、クリエイティブ・スペクテイター、弱点の一時オフ、この種類のオフ（§8）、`throwWeakSpotEnabled` が false
- ヒットの間隔は `throwMinHitIntervalTicks`（**[サーバー]**、`SyncedSettings`。初期値 4）

### 6.2 溜め（ユーザーの提案: 弓のようにゲージを溜め、持ち替えるまで保つ）

- 1 ヒットごとに、**次の 1 投の溜め**が増える。1 ヒットで足す量 = `throwChargePerHit`（初期値 0.5）× コンボの掛け数（`MachineComboBoost.factor`。当てたときのコンボ）
- 投げる速さの倍率 = 1 + 溜め。**上限はない**（4 ヒットで ×3 くらい。そこから先も溜まる）
- **溜めは、持ち替えるまで残る**: 選んでいるスロットが変わる、またはメインハンドの物の種類が変わったら 0 に戻す（投げて数が減るのは、持ち替えではない）。死亡・ディメンション移動・ワールドを出たときも 0
- **投げたら使い切る**（次の 1 投で全部使い、0 に戻る。当てる → 溜める → 投げる、のリズム）
- 両側で溜めを覚える（`ThrowCharge`。弓の `BowDraw` と同じく、クライアントはサーバーの返事を待たずにゲージを進める。サーバーは受け付けたヒットで溜める）

### 6.3 ゲージ（クライアント）

- 照準の下に、弓の引きゲージと同じ位置・大きさで出す（弓と同時には出ない）。溜めが 0 なら出さない
- ゲージの 1 本分 = 溜め 2.0（×3）。色はティール `#2ED3B7`、背景は半透明の黒 `#1E1E1E`
- 1 本を越えた分は、弓の過剰チャージと同じく、ゲージの下に赤 `#FF4D4D` の目盛りを 1 本分ごとに 1 つ足す。目盛りが 5 つを越えたら「×n」の数字で出す（上限がないため）
- ゲージの右に今の倍率「×3.0」を小さく出す
- 設定 `throwChargeBarEnabled`（**[クライアント]**、初期値 true）。設定画面の説明を `en_us.lang` と `ja_jp.lang` に足す

### 6.4 効果（サーバー、`server/ThrowHits`）

- 投げた物が出たとき（`EntityJoinWorldEvent` で、`EntityThrowable`（パール・雪玉・卵・ポーション・瓶）の投げた人がそのプレイヤーで、同じ tick に出たもの）、**速さ（`motionX/Y/Z`）に倍率を掛ける**。遠くまで、速く飛ぶ。重力は変えない
- 掛けたら溜めを 0 にする。クライアントにも投げたことは分かるので、自分でも 0 にする
- ポーションの効く範囲、雪玉・卵の当たったときの効果は変えない（エンダーパールは遠くへ飛ぶので、遠くへワープできる）
- サーバーは、ヒットを受け付けるとき、メインハンドが投げる物であること・間隔・オン/オフを確かめる。受け付けたら `ServerStats.countStreak`、統計の `throwHits`、`ServerBoostTracker.notifyNearbyPlayers`（鳴らす位置はプレイヤー）
- 乗り物に乗っているときは、騎射と同じく乗り物の加速も続ける（`VehicleHits.boostFromRider`）

### 6.5 設定

| キー | 種類 | 初期値 | 説明 |
|---|---|---|---|
| `throwWeakSpotEnabled` | [サーバー]（同期） | true | 投げる物の弱点を出すか |
| `throwChargePerHit` | [サーバー]（同期） | 0.5 | 1 ヒットで溜まる量（倍率に足す。コンボで上乗せ）。0.1〜10.0 |
| `throwMinHitIntervalTicks` | [サーバー]（同期） | 4 | ヒットの最小間隔（tick） |
| `throwChargeBarEnabled` | [クライアント] | true | 溜めのゲージを出すか |

### 6.6 実装時の確認事項

- 速いエンダーパールが、読み込まれていないチャンクに入ったときにどうなるか（消える・止まる）。上限は付けない。困る動きなら README の既知の問題に書き、ユーザーに報告する
- 同じ tick に出た投げる物を、そのプレイヤーのものと見分けられるか（`getThrower`）

## 7. 走りの弱点（新しい種類 `HitKind.SPRINT`）

### 7.1 対象と出し方（クライアント、`client/SprintSpot`）

- 対象: **地面の上を走っている（ダッシュ中の）**間（`isSprinting`、乗り物・エリトラ・水中を除く。1 tick の水平の移動が 0.05 ブロック以上）
- HUD に円で出し、照準を合わせるだけでヒット。向きは**照準の真上か真下だけ**（`HudSpot` の「上下だけ」。左右を向くと進む向きがぶれるため。馬と同じ）
- 色は**赤 `#FF5A5F`**
- 次のときは出さない: 何かを使っている、クリエイティブ・スペクテイター、弱点の一時オフ、この種類のオフ（§8）、`sprintWeakSpotEnabled` が false
- ヒットの間隔は `sprintMinHitIntervalTicks`（**[サーバー]**、`SyncedSettings`。初期値 6）

### 7.2 効果（加速）

- 1 ヒットで、**2 秒（`sprintBoostDurationTicks`、初期値 40 tick）の間、速さ ×1.5**（`sprintBoostMultiplier`、初期値 1.5）。ヒットのたびに残り時間を戻す
- コンボの上乗せあり（`VehicleBoostMath.multiplier`。1000 で 6 倍）。上限は初期値ではなし（`sprintBoostMaxMultiplier`、0 = なし）
- かけ方: **サーバーで**プレイヤーの移動速度（`MOVEMENT_SPEED`）に一時的な修正（決まった UUID、合計に掛ける、`setSaved(false)`）をかけ、時間が来たら外す（馬と同じ）。プレイヤー自身の値はクライアントにも届き、動きの計算に効く。壁の当たり判定はバニラのまま
- 走るのをやめても、残り時間の間は歩く速さにも効く（修正は走りの修正と別なので）
- **視野の広がりは、走りのときの 1.3 倍まで**に抑える（`FOVUpdateEvent`。速さには上限を付けず、見た目だけ抑える。画面が広がりすぎて酔うのを防ぐ）
- 空腹の減りは**バニラのまま**（走った距離で減るので、速いほど早く減る。ユーザーの判断）
- 照準の上に残り時間のゲージ（赤 `#FF5A5F`、乗り物・はしごと同じ位置。同時には起きない）。設定 `sprintBoostBarEnabled`（**[クライアント]**、初期値 true）

### 7.3 通信・検証（サーバー、`server/SprintHits`）

- ヒットは `HitMessage`（種類に `SPRINT`、対象なし）
- サーバーは、オン/オフ、`sprintWeakSpotEnabled`、**直前 10 tick のどこかで走っていた**（`PlayerTickEvent` で覚える）、乗り物に乗っていない、間隔を確かめて受け付ける
- 受け付けたら `ServerStats.countStreak`、統計の `sprintHits`、`ServerBoostTracker.notifyNearbyPlayers`（鳴らす位置はプレイヤー）
- ログアウト・死亡・サーバー停止で修正を外す（保存しないので、残らない）

### 7.4 設定

| キー | 種類 | 初期値 | 説明 |
|---|---|---|---|
| `sprintWeakSpotEnabled` | [サーバー]（同期） | true | 走りの弱点を出すか |
| `sprintBoostMultiplier` | [サーバー]（同期） | 1.5 | 1 ヒットの倍率（コンボで上乗せ） |
| `sprintBoostMaxMultiplier` | [サーバー]（同期） | 0 | 倍率の上限。0 = 上限なし |
| `sprintBoostDurationTicks` | [サーバー]（同期） | 40 | 加速が続く tick 数 |
| `sprintMinHitIntervalTicks` | [サーバー]（同期） | 6 | ヒットの最小間隔（tick） |
| `sprintBoostBarEnabled` | [クライアント] | true | 残り時間のゲージを出すか |

### 7.5 実装時の確認事項

- 速い移動で、マルチのサーバーに引き戻されないか（`moved too quickly`。バニラは歩き・走りで 1 tick に約 10 ブロックを越えると引き戻す）。コンボ 1000（6 倍、約 1.7 ブロック/tick）なら届かない見込み。届くようなら、上限は付けずにユーザーに報告する

## 8. 「弱点」タブ: 種類ごとのオン・オフと見た目（ユーザーの提案）

### 8.1 画面（クライアント、`StatsScreen` に「弱点」タブを足す）

- 統計画面（K キー）の「統計」「サウンド」の隣に「**弱点**」タブを足す
- 弱点の種類ごとに 1 行（全 16 種類。多いのでスクロールする）: 種類の名前 / オン・オフのボタン / 色 / 形

```
 種類       オン/オフ   色                          形
 採掘       [ON ]      [■] [#FF5926   ] [▶]       [● 丸 ▶]
 収穫       [OFF]      [■] [#FF3DCB   ] [▶]       [● 丸 ▶]
 弓         [ON ]      [■] [初期値    ] [▶]       [◇ ひし形 ▶]
 乗り物     サーバーで無効
```

- サーバーの設定（`xxxWeakSpotEnabled`）で無効な種類は、灰色で「サーバーで無効」と出し、ボタンを押せなくする
- 変えたらすぐに `WeakSpotConfig.save()` で `weakspot.cfg` に保存する（「サウンド」タブと同じ）
- HOME キーの一時オフ（全部まとめて）は今のまま残す。一時オフの間は、タブの上に「一時オフ中」と出す

### 8.2 オン・オフ

- オフにした種類は、その人の画面に弱点が出ず、**バニラの動きに戻る**（例: 収穫をオフ → 実った作物の右クリックは何もしない。動物をオフ → 村人の取引画面が開く。機械をオフ → しゃがんでも機械の画面が開く）
- サーバーもバニラの動きを止めないように、オフの種類の一覧（`HitKind` の番号のビット）を `SwitchMessage` に足して送る（ログイン時と、変わったとき。今の `weakSpotsEnabled` と同じく毎 tick 比べる）。サーバーは `ServerSwitches` で、その種類について、右クリックの抑止・ヒットの受け付け・効果（ブースト）を止める
- **統計と節目は、オフにしても今までの数を残す**（ユーザーの判断）。オンに戻すと続きから数える
- 設定 `disabledKinds`（**[クライアント]**、初期値は空）。種類の名前を小文字で 1 行ずつ（例: `harvest`）

### 8.3 見た目（色と形）

- **自分の弱点**の色と形を、種類ごとに選べる（他のプレイヤーのマークは今の `otherMarkerColor` / `otherMarkerShape` のまま）
- 色（ユーザーの判断: 選ぶのと打ち込むのの両方）:
  - ▶ を押すたびに、「初期値」→ 12 色 → 「初期値」と順に切り替える: `#FF5926` 橙赤 / `#FF3DCB` マゼンタ / `#FF8C42` オレンジ / `#FFE14D` 黄 / `#7CFC00` 黄緑 / `#2ED3B7` ティール / `#55CCFF` 水色 / `#7FB2FF` 空色 / `#B070FF` 紫 / `#C8A060` 木の茶 / `#F0F0F0` 白 / `#FF5A5F` 赤
  - 入力欄にカラーコード（`#RRGGBB`）を打ち込んでも選べる。読めない値は受け付けない（前の値のまま、枠を赤くする）
  - 左の [■] は今の色の見本
- 形: ▶ を押すたびに、丸・輪・ひし形・四角を順に切り替える（`common/MarkerShape`。初期値は丸）
- 色から、輪と中心の色は今と同じく白に寄せて作る（`MarkerColor.towardWhite`）
- 「初期値」の色は、今の色のまま（ブロック・生き物の弱点は今の橙赤と黄の組み合わせ、HUD の弱点は種類ごとの色）
- 画面上の弱点（弓・乗り物・食事・はしご・エリトラ・投げる物・走り）と、画面のマーカー（睡眠・エンチャント）も、選んだ形で描く（`ScreenProjection` に形の描き方を足す）
- 設定 `myMarkerColors`（**[クライアント]**、1 行 `種類=#RRGGBB`）と `myMarkerShapes`（**[クライアント]**、1 行 `種類=circle|ring|diamond|square`）。書いていない種類は初期値
- 翻訳キー: 種類の名前 `weakspot.kind.<種類>`（節目のタイトルと共通）、タブ `weakspot.stats.tab.kinds`、ボタン・注記 `weakspot.kinds.*`

## 9. README とガイドの本

- README に「はしご」「エリトラ」「エンチャント」「収穫」「投げる物」「走り」の節を足す（乗り物・睡眠の節にならう）。統計画面の説明に「弱点」タブ（種類ごとのオン・オフ、色、形）を足す。節目の節を §4 に合わせて書き直す
- 設定表の `[サーバー]` に、はしご 5 つ・エリトラ 3 つ・エンチャント 2 つ・収穫 3 つ・投げる物 3 つ・走り 5 つ・節目の `kindMilestonesEnabled` / `totalMilestonesEnabled` / `milestoneRepeatInterval` / `totalMilestones` / `totalMilestoneXp` / `totalMilestoneRepeatInterval` を足し、`milestones` / `milestoneXp` / `milestoneRepair` の初期値を直す。`[クライアント]` に `ladderBoostBarEnabled`・`throwChargeBarEnabled`・`sprintBoostBarEnabled`・`disabledKinds`・`myMarkerColors`・`myMarkerShapes`（設定画面の説明も両方の lang に）
- 統計の一覧に「はしごヒット数」「エリトラヒット数」「エンチャントヒット数」「収穫ヒット数」「投げる物ヒット数」「走りヒット数」
- ガイドの本に 6 ページ足す（`GuideBook.PAGES` を 21 に）。案:
  - 16「はしご」: 「はしごやツタを登り降りしている間、照準の真上か真下に茶色の弱点が出ます。照準を合わせると登り降りが速くなり、コンボが続くほど速くなります。」 / "While climbing up or down a ladder or vine, a brown weak spot appears straight above or below your crosshair. Aim at it to climb faster; the longer your combo, the faster you go."
  - 17「エリトラ」: 「エリトラで飛んでいる間、照準の近くに青い弱点が出ます。照準を合わせると、見ている向きへ一気に飛び出します。コンボが続くほど勢いが増します。壁にぶつからないように。」 / "While gliding with an elytra, a blue weak spot appears near your crosshair. Aim at it to dash toward where you look; the longer your combo, the harder you dash. Mind the walls."
  - 18「エンチャント」: 「エンチャント台に物を置くと、画面のまわりに紫のマーカーが出ます。クリックで当てるたびに、3つの候補が引き直されます。ラピスラズリも経験値も減りません。」 / "Put an item in an enchanting table and a purple marker appears around the screen. Click it to reroll the three offers. It costs no lapis and no experience."
  - 19「収穫」: 「実った作物を素手で右クリックしたままにすると、ピンクの弱点が出ます。当てると収穫して植え直し、すぐに育てる弱点に変わります。コンボが続くほど収穫が増えます。」 / "Hold right-click on a ripe crop with an empty hand and a pink weak spot appears. Hit it to harvest and replant; it turns right back into a growth weak spot. The longer your combo, the bigger the harvest."
  - 20「投げる物」: 「エンダーパールや雪玉などを持つと、照準の近くに青緑の弱点が出ます。当てるたびにゲージが溜まり、次に投げた物が速く遠くへ飛びます。持ち替えると溜めは消えます。」 / "Hold an ender pearl, snowball or other throwable and a teal weak spot appears near your crosshair. Each hit charges the gauge, and your next throw flies faster and farther. Switching items clears the charge."
  - 21「走り」: 「走っている間、照準の真上か真下に赤い弱点が出ます。照準を合わせると走る速さが上がり、コンボが続くほど速くなります。」 / "While sprinting, a red weak spot appears straight above or below your crosshair. Aim at it to run faster; the longer your combo, the faster you go."
  - 11「キー」の統計の画面の説明に「弱点の種類ごとのオン・オフと色も、ここで変えられます」を足す
  - 3「耐久回復と節目」を §4.4 のとおり直す
- 「最新版」の行と「更新履歴」（1.6.x とは接続できないこと）、「開発」の節の仕様書へのリンク

## 10. `CLAUDE.md` に書くこと

- 「仕様の正本」の一覧の、この仕様書の「下書き」の注記を外す。現行のバージョンを 1.7.0、範囲を `[1.7,1.8)` に
- 「両方に Mod が必要」の説明に、1.7.0 で通信内容が変わったので 1.6.x と接続できないことを足す
- `HitKind` の一覧に `LADDER`・`ELYTRA`・`ENCHANT`・`HARVEST` を足す。収穫（`server/HarvestHits`・`common/HarvestBonus`。成長と同じ操作・検証、実った作物で種類を切り替える、植え直し、コンボのおまけ）の説明を成長の弱点の説明のそばに足す。はしご（`client/LadderSpot`・`server/LadderHits`）・エリトラ（`client/ElytraSpot`・`server/ElytraHits`）・エンチャント（`client/EnchantSpot`・`server/EnchantHits`）の説明を、乗り物・睡眠の弱点の説明にならって足す（はしご・エリトラは速さをクライアントで足し、サーバーは検証だけ。エンチャントは `xpSeed` を引き直す）
- 統計の一覧に `ladderHits`・`elytraHits`・`enchantHits`・`harvestHits`・`throwHits`・`sprintHits`（1.7.0）を足す
- 投げる物（`client/ThrowSpot`・`server/ThrowHits`・両側の `ThrowCharge`。溜めは持ち替えるまで残り、投げたら使い切る。`EntityJoinWorldEvent` で速さに掛ける）、走り（`client/SprintSpot`・`server/SprintHits`。移動速度の一時的な修正、視野の広がりは 1.3 倍まで）の説明を足す
- 「弱点」タブ（種類ごとのオン・オフ `disabledKinds` と `SwitchMessage` のビット、自分の弱点の色 `myMarkerColors` と形 `myMarkerShapes`）を、弱点の一時オフの説明のそばに足す。オフの種類で止める処理の一覧も書く
- 節目の説明（`ServerStats`・`MiningRewards`・`MilestoneEffects`）に、種類ごと・合計の節目と、繰り返しの節目、7 並びの虹色を足す。設定の移行に `configVersion` 5（節目の初期値）を足す
- 「次の作業」の節を片付ける

## 11. 入れないこと

- 弓・食事の弱点のヒットで、はしごの加速を続ける（乗り物の騎射にあたるもの）: はしごの上で弓を引く・食べることは少ないので入れない
- 他のプレイヤーにはしご・エリトラ・エンチャントの弱点のマークを見せる: 乗り物・食事と同じく見せない（収穫は、成長と同じく見せる）
- 右クリックですぐ収穫する形（弱点なし）: 収穫をヒットにして、コンボを切らさずに畑を回せるほうを選んだ（ユーザーの判断）
- カボチャ・スイカの収穫: 茎を残して実を壊すだけなので、バニラどおり
- エンチャントの必要なレベルを下げる: ユーザーの判断で、引き直しだけにする
- 引き直しにラピスラズリを使う・回数を限る: ユーザーの判断で、何もいらない・回数の制限なし
- サーバーのランキング（`/weakspot top`）: 1.7.0 には入れない
- 投げる物の溜めを時間で消す: ユーザーの提案で、持ち替えるまで残す
- 走りの空腹の減りを抑える: ユーザーの判断で、バニラのまま
- 種類ごとのキー: 種類が多く、キーが足りないので、統計画面のタブにした

## 12. ユーザーに確認してもらうこと

- はしご・ツタを登り降りすると、照準の真上か真下に茶色の弱点が出て、照準を合わせると速くなること。止まると弱点が消えること。照準の上に茶色のゲージが出ること
- はしごでコンボが上がるとさらに速くなること（`ladderBoostMaxMultiplier` に 3 などを書くと、その倍率で止まること）。速く降りても落下のダメージを受けないこと
- エリトラで飛ぶと、照準の近くに青い弱点が出て、当てると見ている向きへ一気に飛び出すこと（視野が一瞬広がり、雲の尾と打ち上げの音が出る）。コンボが上がると強く飛び出すこと。マルチで引き戻されない・キックされないこと
- エンチャント台に物を置くと、画面の枠の外に紫のマーカーと、枠の上に注釈が出ること。当てるたびに候補が変わり、ラピスも経験値も減らないこと。回数が注釈に出ること
- 弓などを 50 回当てると「弓 50 ヒット！」の節目が出て、経験値がもらえること。全種類の合計 1000 回で「合計 1000 ヒット！」が派手に出ること。7777 などが虹色になること
- 古い `weakspot.cfg`（節目が 100 / 777 / 1000 / 10000 のまま）が、新しい初期値に置き換わること。自分で書き換えた値は、そのまま残ること
- 実った小麦などを素手で右クリックしたままにすると、ピンクの弱点が出て、当てると収穫・植え直しされ、すぐに成長の弱点に変わること。押しっぱなしのまま、育てる・収穫するを続けられること
- コンボが上がると、収穫物（小麦・ニンジンなど）が増えること。種は増えないこと。保護された土地では収穫できないこと
- エンダーパールなどを持つと、照準の近くにティールの弱点が出て、当てるたびに照準の下のゲージが溜まること。1 本を越えると赤い目盛りが増えること。投げると速く遠くへ飛び、ゲージが空になること。持ち替えるとゲージが消えること
- 走ると照準の真上か真下に赤い弱点が出て、当てると速くなり、照準の上に赤いゲージが出ること。速くなっても画面が広がりすぎないこと
- K キーの統計画面の「弱点」タブで、種類ごとにオフにすると弱点が出なくなり、バニラの動き（村人の取引画面など）に戻ること。オンに戻すと統計が続きから数えられること
- 「弱点」タブで色を ▶ で切り替える・`#00FF00` のように打ち込むと、自分の弱点の色が変わること。形を切り替えると、ブロック・画面上の弱点の形が変わること。ゲームを入れ直しても残ること
- 統計画面と `/weakspot stats` に「はしごヒット数」「エリトラヒット数」「エンチャントヒット数」「収穫ヒット数」「投げる物ヒット数」「走りヒット数」が出ること
- 1.6.x のサーバー・クライアントとは接続できないこと
