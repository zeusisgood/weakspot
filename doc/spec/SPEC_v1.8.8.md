# 弱点破壊 Mod 追加仕様書（v1.8.8）

`SPEC_v1.8.7.md`（Mod 1.8.7）に対する**パッチ**の仕様。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.8.8.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.8.7 がリリース済み
- この仕様書の内容は、Mod のバージョン **1.8.8** として、リリースする

---

## 0. バージョンと互換性

- バージョンは 1.8.7 → **1.8.8**（`build.gradle` の `version` と `WeakSpotMod.VERSION`）
- **コードの整理だけ**。遊び方・通信内容・保存データ・設定は変わらない。1.8.x 同士は、そのまま接続できる
- `WireCompatTest`・`HitHandlersTest`・`SyncedSettingsTest` がそのまま通ること（期待値を作り直さない）

## 1. やりたいこと（ユーザーの判断: 1.8.7 の相談の候補 2〜5。候補 6 の釣りを `AimSpotKind` に載せるのは見送り）

同じ形の処理が何か所にもある所を、1 つの部品にまとめる。1.8.7（コンボ倍率）と分けて出すのは、不具合が出たときに、どちらのせいか見分けやすくするため。

## 2. 使う時間の短縮（`BowDraw`・`EatDraw`）

- 今: どちらも「ヒットで縮める tick 数をプレイヤーごとに覚え（`WeakHashMap`、両方のスレッドから触るので同期）、使い始め（`LivingEntityUseItemEvent.Start`）で捨て、次の `LivingEntityUseItemEvent.Tick` で `setDuration` で減らす」を持っている
- 共通の部品（例 `UseTimeCut`、両側）にする。持つもの: 覚えておく表、`add`、使い始めで捨てる、対象の物か（弓 = `ItemBow`、食事 = `EAT` / `DRINK`）、減らし方（食事 = 残り 1 まで、弓 = 引き切り 20 tick まで）
- 弓だけのもの（過剰チャージ、同じ tick に放った矢の `ArrowLooseEvent` の charge、`EntityJoinWorldEvent` の矢の威力）は `BowDraw` に残す
- 外から呼ぶ形（`BowDraw.add` / `EatDraw.add` など）は変えない

## 3. 移動速度の一時的な修正（`server/MoveHits` の走り・`server/VehicleHits` の馬・豚）

- 今: どちらも「固定の UUID の `MOVEMENT_SPEED` の修正を外し、倍率が 1 より大きければ `MULTIPLY_TOTAL`・保存しない（`setSaved(false)`）で付け直す」を持っている
- 共通の部品（例 `server/SpeedModifier`。UUID と名前を持ち、`set(生き物, 倍率)` / `clear(生き物)`）にする
- 残り時間の数え方は違う（走りはプレイヤーの tick、乗り物はワールドの tick でトロッコも進める）ので、それぞれに残す
- UUID と名前は今のまま（クライアントの視野の抑えが `MoveHits.SPRINT_MODIFIER` で見分けているため）

## 4. 画面上のマーカー（`client/SleepSpot`・`client/EnchantSpot`）

- 今: どちらも「`DrawScreenEvent.Post` で、`MarkerLook` の色・形、`MarkerMotion` の移動と残像でマーカーを描き、`MouseInputEvent.Pre` で左クリックが円の中ならキャンセルしてヒット（間隔・連続ヒット・通知）」を持っている
- 照準のまわりの弱点（`AimSpotKind` / `AimSpots`）と同じ形で、土台（例 `ScreenSpotKind` と、それを回す `ScreenSpots`）を作る
  - 土台が持つもの: 描く（色・形・残像・中心の点）、クリックの判定、ヒットの共通処理（`ClientWeakSpotHandler.canHitNow` / `registerHit`、`HitMessage` の送信）、出し直し
  - 種類ごとに書くもの: 出す画面と条件（寝ている画面で夜、エンチャントの画面で候補がある）、置ける範囲（睡眠は中央の 4 割、エンチャントは枠の外）、次の位置の離し方、ヒットの効果（なし・注釈など）、枠の上の注釈（エンチャント）
- 置く範囲・大きさ・色・注釈は今と同じ

## 5. サーバーへの問い合わせの間隔（`client/AnimalStates`・`client/MachineBars`・`client/FishingSpot`）

- 今: どれも「最後に問い合わせた tick と相手を覚え、n tick ごと（動物・釣り 5、機械 4）とヒットのとき（強制）に問い合わせる」を持っている
- 間隔の管理だけを小さな部品（例 `client/QueryThrottle`。間隔を持ち、`due(相手, tick, 強制)` が問い合わせてよいかを返して記録する、`reset()`）にする
- 問い合わせの中身・返事の扱い・補間は、それぞれに残す。間隔の値は今のまま

## 6. README・doc・ガイドの本・お知らせ

- 遊び方は変わらないので、`doc/play.md`・ガイドの本は直さない
- `CHANGELOG.md`・README の「最近の更新」: 内部の整理だけで、遊び方・通信内容・設定に変更なし。1.8.x 同士はそのまま接続可能
- 更新のお知らせ `weakspot.news.1.8.8`: 「内部の整理（遊び方に変更なし）」
- `doc/development.md` は、構成の説明があれば新しい部品を足す

## 7. `CLAUDE.md` に書くこと

- 現行を 1.8.8 に
- 共通の部品の一覧（1.8.6 で整理した共通の部品の節）に、`UseTimeCut`・`SpeedModifier`・`ScreenSpotKind` / `ScreenSpots`・`QueryThrottle` を足し、弓・食事・走り・乗り物・睡眠・エンチャント・動物・機械・釣りの説明の該当する所を直す
- 「弱点の種類を足すときに直す所」に、画面上のマーカーなら `ScreenSpotKind` を継いだクラスを足すことを書く

## 8. ユーザーに確認してもらうこと（今までと同じに動くこと）

- 弓の引きと過剰チャージ、食事・飲み物の短縮
- 走りと馬・豚の加速（切れたら元の速さに戻ること）
- 睡眠とエンチャントのマーカー（出る位置、色・形の変更、クリックで当たること、エンチャントの候補が変わること）
- 動物・機械・釣りの弱点とバー（問い合わせが止まらず、弱点が出ること）
