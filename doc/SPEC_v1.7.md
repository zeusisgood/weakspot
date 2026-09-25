# 弱点破壊 Mod 追加仕様書（v1.7・下書き）

`SPEC_v1.6.4.md`（Mod 1.6.4）に対する**マイナー**の仕様。ここに書かれていないことは、それと現行実装のままとする。
仕様書は、リポジトリの `doc/` ディレクトリに置く（この仕様書は `doc/SPEC_v1.7.md`）。
仕様と食い違う実装をする場合は、`CLAUDE.md` のリリースの流れ（止まる条件）に従う。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.6.4 がリリース済み。1.6.4 の内容を土台にする
- この仕様書の内容は、Mod のバージョン **1.7.0** として、リリースする
- **下書き**。ユーザーが「実装」と言うまで実装しない。1.7.0 に入れるほかの機能は、決まったらこの仕様書に足す

---

## 0. バージョンと互換性

- バージョンは 1.6.4（またはその後のパッチ）→ **1.7.0**（`build.gradle` の `version` と `WeakSpotMod.VERSION` の2か所を、そろえて変える）
- **通信内容が変わる**（`HitKind` に `LADDER` を足す、`SyncedSettings` にはしごの設定を足す、`StatsMessage` に `ladderHits` を足す）。そのため 1.6.x とは接続できない
  - `WeakSpotMod.ACCEPTED_VERSIONS` を `[1.7,1.8)` にする
  - README の更新履歴に、1.6.x とは接続できないこと（サーバーとクライアントを同時に更新すること）を書く
- 1.6.x と接続できないので、はしごの弱点は `ServerFeatures.since` で囲まなくてよい（1.7.x のサーバーはすべて持つ）

## 1. はしごの弱点（新しい種類 `HitKind.LADDER`）

### 1.1 対象と出し方（クライアント、`client/LadderSpot`）

- 対象: **登れるブロック**にいる間。判定はバニラの `EntityLivingBase#isOnLadder`（Forge の `ForgeHooks.isLivingOnLadder`。はしご・ツタ・`Block#isLadder` を返す Mod のブロックがすべて入る）
- **登っている・降りている間だけ**出す: 1 tick の縦の移動の大きさ（`posY − prevPosY` の絶対値）が 0.05 ブロック以上のとき。しゃがんで止まっている・はしごの上で立ち止まっている間は出さない
- 次のときは出さない: 何かを使っている（弓を引く・食べる。そちらの弱点を出す）、乗り物に乗っている、クリエイティブ・スペクテイター、弱点の一時オフ（HOME キー）、設定 `ladderWeakSpotEnabled` が false
- 乗り物・食事と同じく、**HUD に画面上で一定の大きさの円**で出し（`client/HudSpot`）、**照準を合わせるだけでヒット**（クリックは要らない）
- 向き: **照準の真上か真下だけ**（`HudSpot` の「上下だけ」。馬・豚と同じ。pitch を 10〜20 度ずらし、yaw は毎フレームの視線に合わせる）
  - 理由: はしごは前に進むキーではしごに押し付けて登るので、左右に向きを変えると、はしごから横に外れてしまう。上下だけなら、狙っても進む向きは変わらない
  - ※ 相談では「照準から 10〜20 度（全方向）」で決めていた。上下だけにするかは、ユーザーに確認する（§5）
- 円の色は**木の茶色 `#C8A060`**（弓のオレンジ `#FF8C42`、乗り物の水色 `#55CCFF`、食事の緑 `#7CFC00` と見分ける）
- ヒットの間隔は `ladderMinHitIntervalTicks`（**[サーバー]**、`SyncedSettings`。初期値 6）

### 1.2 効果（加速）

- 1 ヒットで、**2 秒（`ladderBoostDurationTicks`、初期値 40 tick）の間、登り降りの速さ ×1.5**（`ladderBoostMultiplier`、初期値 1.5）。残り時間は、ヒットのたびに 40 tick に戻す（乗り物と同じ）
- **コンボの上乗せあり**: 倍率 = `ladderBoostMultiplier` × コンボの掛け数（`MachineComboBoost.factor`。25 で ×1.25 … 1000 で ×4。初期値ならコンボ 1000 で **6 倍**）。計算は乗り物と同じ `VehicleBoostMath.multiplier` を使う
  - **上限は初期値ではなし**。設定 `ladderBoostMaxMultiplier`（初期値 **0 = 上限なし**、0 か 1.0〜100.0）に数を書くと、その倍率で抑える
- **登りと降りの両方**を速くする
- **速さはクライアントで足す**（プレイヤーの動きはクライアントが決めるので、ボートと同じ方式）: 加速中、`ClientTickEvent` END で、その tick の縦の移動（`motionY` ではなく、実際に動いた `posY − prevPosY`）に (倍率 − 1) を掛けた分を `EntityPlayerSP#move(MoverType.SELF, 0, dy, 0)` で足す
  - 登れるブロックにいて、登り降りしている（§1.1 の条件）tick だけ足す。はしごから外れたら足さない（加速の残り時間は残る。戻ればまた効く）
  - 1 tick に足す量は 2 ブロックまで（サーバーの「動きが速すぎる」の判定に引っかからないように。倍率 6 でも登りは約 0.6 ブロック/tick なので、ふつうは届かない）
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

## 2. README とガイドの本

- README に「はしご」の節を足す（乗り物の節にならう）。設定表の `[サーバー]` に 5 つ、`[クライアント]` に `ladderBoostBarEnabled` を足す。統計の一覧に「はしごヒット数」
- ガイドの本に 16 ページ目「はしご」を足す（`GuideBook.PAGES` を 16 に）。案:
  - 日本語: 「はしごやツタを登り降りしている間、照準の真上か真下に茶色の弱点が出ます。照準を合わせると登り降りが速くなり、コンボが続くほど速くなります。」
  - 英語: "While climbing up or down a ladder or vine, a brown weak spot appears straight above or below your crosshair. Aim at it to climb faster; the longer your combo, the faster you go."
- 「最新版」の行と「更新履歴」（1.6.x とは接続できないこと）、「開発」の節の仕様書へのリンク

## 3. `CLAUDE.md` に書くこと

- 「仕様の正本」の一覧の、この仕様書の「下書き」の注記を外す。現行のバージョンを 1.7.0、範囲を `[1.7,1.8)` に
- 「両方に Mod が必要」の説明に、1.7.0 で通信内容が変わったので 1.6.x と接続できないことを足す
- `HitKind` の一覧に `LADDER` を足す。はしごの弱点（`client/LadderSpot`・`server/LadderHits`）の説明を、乗り物の弱点の説明にならって足す（速さはクライアントで足す、サーバーは検証だけ、直前 10 tick の `isOnLadder`）
- 統計の一覧に `ladderHits`（1.7.0）を足す
- 「次の作業」の節を片付ける

## 4. 入れないこと

- 弓・食事の弱点のヒットで、はしごの加速を続ける（乗り物の騎射にあたるもの）: はしごの上で弓を引く・食べることは少ないので入れない
- 他のプレイヤーにはしごの弱点のマークを見せる: 乗り物・食事と同じく見せない

## 5. ユーザーに確認してもらうこと

- （下書きの確認）弱点の向きを「照準の真上か真下だけ」にしてよいか（相談では 10〜20 度の全方向）
- はしご・ツタを登り降りすると、照準の真上か真下に茶色の弱点が出て、照準を合わせると速くなること。止まると弱点が消えること
- コンボが上がるとさらに速くなること（初期値では上限なし。`ladderBoostMaxMultiplier` に 3 などを書くと、その倍率で止まること）
- 照準の上に茶色の残り時間のゲージが出ること。`ladderBoostBarEnabled` をオフにすると出ないこと
- 速く降りても、下で落下のダメージを受けないこと。マルチで、速く登っても引き戻されない・キックされないこと
- 統計画面と `/weakspot stats` に「はしごヒット数」が出ること
- 1.6.x のサーバー・クライアントとは接続できないこと
