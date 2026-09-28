# 弱点破壊 Mod 追加仕様書（v1.10.0）

`SPEC_v1.9.6.md`（Mod 1.9.6）に対する**マイナー**の仕様。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.10.0.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.9.6 がリリース済み
- この仕様書の内容は、Mod のバージョン **1.10.0** として、リリースする

---

## 0. バージョンと互換性

- バージョンは 1.9.6 → **1.10.0**（`build.gradle` の `version` と `WeakSpotMod.VERSION`）。`ACCEPTED_VERSIONS` を `[1.10,1.11)` にする
- 弱点の種類を足すので、統計の送る並びと `SyncedSettings` が変わる（通信内容の変更）。**1.9.x とは接続できない**。サーバーと全員のクライアントを同時に更新する
- 統計の保存は項目の追加だけ（1.9.x のワールドはそのまま読める）
- 設定はカテゴリに分ける（2 の節）。古い `weakspot.cfg` は、初めて起動したときに自動で移行する
- パッケージ名の改名は 1.9.3 で先に済ませる（`SPEC_v1.9.3.md`）。設定のカテゴリ分けは、この版に入れる（2 の節）

## 1. 泳ぎの弱点（`HitKind.SWIM`）

### 1.1 やりたいこと（ユーザーの判断）

- 泳ぐ速さを、弱点で加速する
- 1.12.2 には泳ぐときのダッシュがなく、移動速度の値（`MOVEMENT_SPEED`）は水中の移動に効かない。はしごと同じく、移動の量を直接足す
- 相談の結果: 時間で続く加速（はしご・ダッシュ・乗り物と同じ遊び方）に、当てた瞬間の小さな突進を足す。倍率とコンボの上乗せはボート（乗り物）と同じにし、「同じだけ当てれば、ボートのほうが常に速い」関係を保つ（ボートが不要にならないように）。突進は小さく固定し、コンボで大きくしない

### 1.2 出すとき

- **水の中で動いている間**（`isInWater`、1 tick の移動が 0.03 ブロック以上）。浅い水を歩いているときも出す（水中の扱いで遅いため）
- 出さないとき: 乗り物に乗っている（ボートは乗り物の弱点が受け持つ）、はしご・ツタにつかまっている（はしごを優先）、エリトラで飛んでいる、溶岩の中、何かを使っている（弓・飲食。ほかの照準のまわりの弱点と同じ）
- 位置: **照準のまわり 10〜20 度**（`HudSpot.FREE`。水中は上下にも進めるので、少しずれても泳ぎが崩れにくい）
- 照準のまわりの弱点の共通の土台（`AimSpotKind` を継いだ `client/SwimSpot`、`AimSpots.KINDS`）を使う

### 1.3 当てたとき

- **時間で続く加速**: 倍率 = `swimBoostMultiplier`（初期値 1.5）× コンボの掛け数（`TimedBoostMath.multiplier`、上限 `swimBoostMaxMultiplier` は 0 = なし）、`swimBoostDurationTicks`（初期値 40 = 2 秒）続く。ヒットのたびに、この長さに戻す
  - クライアントが `ClientTickEvent` END で、その tick の移動（上下を含む）×(倍率 − 1) を `move` で足す（はしごと同じ形。1 tick の上限なし）
  - 水中歩行のエンチャントの速さにも、そのまま掛かる
- **突進**: 当てた瞬間、視線の方向に `motion` を足す。進む距離は約 `swimDashDistance` ブロック（初期値 **0.3**。水中の減速で止まるまでに進む距離。足す `motion` = 距離 × 0.2）。コンボでは大きくしない
- **乗り物に乗ったら、泳ぎの加速の残りを止める**（乗っている間に体へ移動を足さないように）
- 残り時間のゲージを照準の上に出す（`HudSpot.gauge`。`swimBoostBarEnabled`）
- 演出: 泡の粒子（`WATER_BUBBLE`）、水しぶきの音、視野の広がりは 1.3 倍まで（ダッシュと同じ）
- 酸素は回復しない（ユーザーの判断: 加速だけにする。あとから足せる）

### 1.4 速さの目安（バニラ 1.12.2 のおおよその値。調整の参考）

| コンボ | 泳ぎ | 泳ぎ + 水中歩行 III | ボート（同じだけ当てる） | ボート（加速なし） |
|---|---|---|---|---|
| 0 | 3 m/s | 6.5 m/s | 12 m/s | 8 m/s |
| 100 | 6 m/s | 13 m/s | 24 m/s | 8 m/s |
| 1000 | 12 m/s | 26 m/s | 48 m/s | 8 m/s |

### 1.5 サーバー

- ヒットの受付は `server/MoveHits`（はしご・ダッシュ・エリトラと同じ。前置きは `HitGate`）。受け付ける条件: 水の中にいる、乗り物に乗っていない
- 統計・コンボ・節目は、ほかの種類と同じ（`MiningStats` の配列は自動。保存のキーは `saveKey` の規則）

### 1.6 設定（`server.swim` と `client.hud`。2 の節）

| キー | 初期値 | 側 | 説明 |
|---|---|---|---|
| `swimWeakSpotEnabled` | true | サーバー | 泳ぎの弱点のオン／オフ |
| `swimBoostMultiplier` | 1.5 | サーバー | 泳ぎの弱点に当てた時の速さの倍率。コンボの掛け数を上乗せ |
| `swimBoostMaxMultiplier` | 0 | サーバー | 泳ぎの速さの倍率の上限。0 で上限なし |
| `swimBoostDurationTicks` | 40 | サーバー | 泳ぎの加速の持続時間（tick）。ヒットのたびにこの長さに戻る |
| `swimDashDistance` | 0.3 | サーバー | 当てた瞬間の突進で進む距離（ブロック、おおよそ） |
| `swimMinHitIntervalTicks` | 6 | サーバー | 泳ぎのヒットの最小受付間隔（tick） |
| `swimBoostBarEnabled` | true | クライアント | 泳ぎの加速中、照準の上に残り時間のゲージを表示するか |

- サーバーの項目は `SyncedSettings`（と `WIRE`）に入れて送る。オン・オフと間隔は名前の規則で種類の表（`SyncedSettings.enabled` / `minHitInterval`）に自動で入る
- `disabledKinds` の候補に `swim` を足す

### 1.7 見た目

- 呼び名: **泳ぎ**（英語 **Swim**）。設定・翻訳キーの名前は `swim`
- 色の初期値: **深い青 `#2E6BFF`**（乗り物 `#55CCFF`・エリトラ `#7FB2FF`・投擲物 `#2ED3B7` と見分けやすい）。ゲージの色も同じ
- コンボの掛け数の表示（「泳ぎ ×1.25」）に入れる
- 「弱点マーカー」タブ（`KindMask`）に入れる

## 2. 設定のカテゴリ分け

### 2.1 やりたいこと（ユーザーの判断）

- `general` に並んでいる約 140 項目が、設定画面で探しにくい。1.10.0 はマイナーなので、この版で分ける（`CLAUDE.md` の「キー名を変えない」方針を、この版だけ移行つきで破る）
- 相談の結果: **2 段**（上の段がどちらの値が効くか `server` / `client`、その下を種類・用途）。**項目の名前は今のまま**で、カテゴリだけを付ける（`SyncedSettings` の「同じ名前」、種類の表の `<種類>WeakSpotEnabled` / `<種類>MinHitIntervalTicks` の決まりをそのまま使う）

### 2.2 カテゴリと項目

設定ファイルでは `server { vehicle { D:vehicleBoostMultiplier=1.5 } }` の形になる（以下、`server.vehicle.vehicleBoostMultiplier` と書く）。

**server（サーバー）**

| カテゴリ | 項目 |
|---|---|
| `general`（共通） | `giveGuideBook` `markerShareRange` `markerSendMinIntervalTicks` |
| `mining`（採掘） | `miningWeakSpotEnabled` `miningComboBonus` `boostMultiplier` `boostDurationTicks` `weakSpotRadiusRatio` `edgeMargin` `minMoveDistance` `weakSpotMinRadius` `weakSpotMaxRadiusRatio` `minFaceSize` `lingerTicks` `minHitIntervalTicks` |
| `repair`（道具の修理） | `hitsPerRepair` `repairPerStep` `maxRepairPerBreak` |
| `milestones`（節目） | `milestones` `milestoneXp` `milestoneRepair` `milestoneRepeatInterval` `kindMilestonesEnabled` `totalMilestonesEnabled` `totalMilestones` `totalMilestoneXp` `totalMilestoneRepeatInterval` |
| `growth`（成長） | `growth…` の 8 項目、`mushroomGrowChance` |
| `machine`（機械） | `machine…` の 6 項目、`excludedBlocks` |
| `animal`（動物） | `animal…` `sheep…` `chicken…` `villager…` の 15 項目 |
| `fishing` `bow` `sleep` `eat` `vehicle` `ladder` `elytra` `enchant` `harvest` `throw` `sprint` `portal` `swim` | その種類の名前で始まる項目 |
| `melee`（近接） | `melee…` の 4 項目、`critsPerRepair` `critRepairPerStep` |

**client（クライアント）**

| カテゴリ | 項目 |
|---|---|
| `sound`（音） | `myHitSound` `myHitVolume` `hitChordEnabled` `othersHitSound` `othersHitVolume` |
| `markers`（マーカー） | `weakSpotsEnabled` `disabledKinds` `myMarkerColors` `myMarkerShapes` `weakSpotTrailEnabled` `animalSpotSeeThrough` `otherMarkerEnabled` `otherMarkerColor` `otherMarkerAlpha` `otherMarkerShape` |
| `combo`（コンボ） | `comboDisplayEnabled` `comboScale` `comboPosition` `comboMilestoneEffects` `othersComboDisplay` `showOthersMilestones` |
| `hud`（ゲージ・表示） | `blockHealthBarEnabled` `machineBarEnabled` `growthBarEnabled` `machineParticlesVisible` `bowDrawBarEnabled` `vehicleBoostBarEnabled` `ladderBoostBarEnabled` `throwChargeBarEnabled` `meleeChargeBarEnabled` `sprintBoostBarEnabled` `swimBoostBarEnabled` |
| `updates`（更新） | `showUpdateNotes` `lastSeenVersion` `checkForUpdates` `skippedUpdateVersion` |

- `configVersion` は `general` に残す（古い形の設定ファイルを見分けるため。`general` にはこれだけが残る）
- 上の表にない項目が見つかったら、近いカテゴリに入れ、仕様書と `doc/config.md` に書き足す
- 作り方（目安）: `@Config` のクラスを `server` / `client` のカテゴリで分け、下の段は入れ子のオブジェクトにする。コードからの参照の形は実装で決める（項目の名前と意味は変えない）
- このとき `WeakSpotConfig`（今 745 行）を、カテゴリごとの入れ子のクラスに分けて整理する（1.9.4 のあとの相談。ユーザーの判断）。設定の説明の翻訳が揃っているかは `resources/LangFilesTest` が確かめるので、翻訳キーの付け替えと一緒に、テストの決まり（`weakspot.general.<項目>.tooltip`）も新しい形に直す

### 2.3 移行（`configVersion` 7）

- 1.10.0 を初めて起動したとき（`WeakSpotConfig.migrate`、`serverStarting`）、古い `general.<項目>` の値を新しいカテゴリの同じ名前の項目へ移し、古い項目を消す。書き換えていた値はそのまま引き継ぐ
- クライアントだけで起動した（サーバーを立てない）ときも、クライアントの項目が移るようにする（ワールドに入る前の、保存できる時点で行う）
- `configVersion` を 7 にする
- 手で直す必要はない。サーバーの管理者が `weakspot.cfg` をスクリプトなどで書き換えている場合は、キーの直しが要る（`CHANGELOG.md` に書く）
- 移行は golden テストで確かめる（1.9.x の形の設定ファイル → 1.10.0 の形。値が引き継がれること、古いキーが残らないこと）

### 2.4 設定画面・翻訳

- 設定画面は、`server` / `client` → カテゴリ → 項目の順にたどる形になる
- カテゴリの名前（上の表のかっこの中）と説明を `en_us.lang` / `ja_jp.lang` に足す
- 項目の説明の翻訳キー（今は `weakspot.general.<項目>.tooltip`）は、新しいカテゴリに合わせて機械的に付け替える（文章は変えない）
- `doc/config.md` の表を、カテゴリごとに並べ替える
- 設定の説明（`@Config.Comment` と翻訳の `.tooltip`）に付いている版の注記（「（1.9.0）」「(1.9.5)」など）を外す（1.9.6 のあとの相談。ユーザーの判断。版は更新履歴にあるため。説明の文そのものは変えない）

## 2b. ほかの内容

### 2b.1 統計の平均ヒット数の計算（1.9.4 のあとの相談。ユーザーの判断）

- 今の平均（`MiningStats.averageHitsPerBlock()`）は、採掘ヒット数（壊しきらなかったブロックへのヒットも含む）÷ 壊したブロック数で、「最多ヒット数」（壊したブロックだけ）と食い違って見える。1.9.4 では名前だけを中身どおりにした（`SPEC_v1.9.4.md`）
- この版で、壊したブロックへのヒット数（`hitsOnBrokenBlocks`。`recordBlockBroken(hitsOnBlock)` で足す）を新しく数え、平均を「壊したブロックへのヒット数 ÷ 壊したブロック数」にする。名前は「1 ブロックあたりの平均ヒット数」に戻す
- 統計の送る内容（`MiningStats.writeTo` / `readFrom`）と保存（NBT のキー `hitsOnBrokenBlocks`。項目の追加なので 1.9.x のワールドも読める。古いワールドの値は 0 から数え始める）が増える。1.10.0 はマイナーなので、ここに入れる

### 2b.2 ほかのプレイヤーの節目をチャットで全員に（1.9.6 のあとの相談。ユーザーの判断）

- 誰かが節目（採掘・種類ごと・全種類の合計。`Milestones` の判定そのまま）に届いたら、**本人以外の全員**のチャットに知らせる（本人には今までどおりの演出とチャット）
  - 例: `[WeakSpot] zeusisgood さんの採掘の弱点ヒットが 1000 回に達しました！`（採掘・種類ごと・合計で文を分ける。種類の名前は翻訳キー `weakspot.kind.<key>` を入れ子に）
- **すべての節目を流す**（ユーザーの判断: 範囲はしぼらない。サーバーの設定も作らない）
- 送り方: サーバーが `PlayerText`（1.9.6）で翻訳キーと値を送る。翻訳キーは `weakspot.milestone.broadcast.<mining|kind|total>`（`since` は 1.10.0。1.10.0 同士しか接続できないので、いつも翻訳キーで送ることになる）
- 色: 金 `#FFAA00`（チャットの `GOLD`）。7 が並ぶ数（`Milestones.isLucky`）はピンク `#FF55FF`（`LIGHT_PURPLE`）
- 各プレイヤーの設定 `showOthersMilestones`（`[クライアント]`、初期値 true。`client.combo`）: オフなら、ほかの人の節目のチャットを表示しない（クライアントが `ClientChatReceivedEvent` で、翻訳キーが `weakspot.milestone.broadcast.` で始まるものを取り消す）
- サーバーから送る文なので、`LangFilesTest` の書式の確認（`%s` だけ）の対象に `weakspot.milestone.broadcast.*` を足す
- ユーザーに確認してもらうこと: 2 人以上で、片方が節目に届くと、もう片方のチャットに知らせが出ること。本人には出ないこと。`showOthersMilestones` をオフにすると出なくなること

## 3. README・doc・お知らせ

- `README.md`・`README.en.md` の種類の一覧、`doc/play.md`（泳ぎの節）、`doc/config.md`（1.6 の表）、`doc/architecture.md`（泳ぎの弱点の項目）
- ガイドの本（`GuideBook.CONTENT`）に泳ぎのページ
- `CHANGELOG.md`・README の「最近の更新」: 1.9.x と接続不可であること
- 配布サイトの説明文 `doc/store/description.md`: 種類の一覧に泳ぎを足す（CurseForge は手動で貼り替え）
- `update.json`、`weakspot.news.1.10.0`

## 4. `CLAUDE.md` に書くこと

- 現行を 1.10.0 に、`ACCEPTED_VERSIONS` を `[1.10,1.11)` に

## 5. ユーザーに確認してもらうこと

- 水の中で泳ぐと、照準のまわりに泳ぎの弱点（深い青）が出て、当てると少し前に突き出し、2 秒ほど速く泳げること。当て続けるとコンボで速くなること
- ボートに乗ると泳ぎの弱点が消え、加速も止まること。ボートの弱点は今までどおり出ること
- 水中のはしごでは、はしごの弱点だけが出ること
- 速さの体感（ボートより遅いと感じるか、突進の大きさ）
- 1.9.x で使っていた `weakspot.cfg`（色・形・音などを変えたもの）のまま 1.10.0 を起動すると、設定がそのまま効いていること。ファイルが `server { … }` / `client { … }` の形に変わっていること
- 設定画面で、サーバー／クライアント → カテゴリ → 項目の順にたどれること
