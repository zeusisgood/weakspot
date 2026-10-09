# 弱点破壊 Mod 追加仕様書（v1.11.5）

`SPEC_v1.11.4.md`（Mod 1.11.4）に対する**パッチ**の仕様。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.11.5.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.11.4 がリリース済み
- この仕様書の内容は、Mod のバージョン **1.11.5** として、リリースする
- 内容は、過剰設計の監査（ponytail-audit）で見つけた 13 件の整理を全部（ユーザーの判断）。遊び方・見た目は変えない

---

## 0. バージョンと互換性

- バージョンは 1.11.4 → **1.11.5**（`build.gradle` の `version` と `WeakSpotMod.VERSION`）
- 内部の整理だけ。通信内容・設定・保存データ・翻訳キーは変わらない。`ServerFeatures.since` も要らない
- 1.11.x 同士はそのまま接続できる

## 1. 1.11.0 より古いクライアント向けの文の分岐をやめる

- `ACCEPTED_VERSIONS = "[1.11,1.12)"` なので、1.11.0 より古いクライアントは接続できない。キーを足した版が 1.11.0 以下の文は、サーバーで文にする分岐（`ServerLang`）に入らない
- 次を `new TextComponentTranslation(...)` で送る（`PlayerText.of` を通さない）:
  - 村人の繁殖の案内（`VillagerBreedHints`。`KEYS_SINCE = "1.8.4"`・`serverText` を消す）
  - 節目の知らせ（`MiningRewards.broadcast`。`BROADCAST_SINCE = "1.10.0"`）と、コンボの段階の知らせ（`COMBO_BROADCAST_SINCE = "1.11.0"`）
  - 版の違いの知らせ（`VersionCheck`。`KEYS_SINCE = "1.5.1"`）
  - 的当てのルール・1 位の知らせ（`TargetRounds.SINCE = "1.11.0"`）と、ようこそのメッセージ（`GuideBookGiver.line`）
- 1.11.1 で足したアップデートの的の知らせ（`GIFT_SINCE`）は、1.11.0 のクライアントがキーを持たないので、今までどおり `PlayerText.of`。`ServerLang` と `PlayerText.of` は残す
- 版が分からない（同じ jar のシングルプレイ）ときも、クライアントはキーを持っているので、表示は変わらない

## 2. 弓の引きゲージを共通の部品で描く

- `BowSpot.drawGauge` のゲージを `HudSpot.gauge`（照準の下、引いている途中 `#FF8C42`・引き切った `#FFD23F`）で描く
- 過剰チャージの目盛り（`#FF4D4D`、高さ 2・間 1、上限の数だけ）を、溜めのゲージと同じ `ChargeGauge.drawMarks` で描く（`ChargeGauge.drawBars` もこれを使う）
- `BowSpot` の `BAR_*`・`OVERCHARGE*` の定数をやめる。大きさ・位置・色は今と同じ

## 3. 乗り物の加速を `TimedBoost` で持ち、ゲージを `HudSpot.gauge` で描く

- `VehicleSpot` の `multiplier`・`boostUntil`・`boostDuration`・`remaining` を、はしご・走りと同じ `TimedBoost` 1 つに
- ゲージは `HudSpot.gauge(..., 0x55CCFF, true)`（照準の上、水色 `#55CCFF` のまま。弱点の色を変えても、ゲージの色は今までどおり変わらない）。`BAR_*` の定数をやめる

## 4. 統計の送受信の口を `java.io.DataOutput` / `DataInput` に

- `MiningStats.Writer` / `Reader` をやめ、`writeTo(DataOutput)` / `readFrom(DataInput)` にする
- `StatsMessage` は netty の `ByteBufOutputStream` / `ByteBufInputStream` を渡す。並び（バイト列）は変わらない（`WireCompatTest` で確かめる）

## 5. 何も受け取らないクラスの `@Mod.EventBusSubscriber` を外す

- `@SubscribeEvent` を持たないのに付いている 12 個（`EatHits`・`BowHits`・`ThrowHits`・`SleepHits`・`MeleeHits`・`AnimalHits`・`EnchantHits`・`PortalHits`・`RightClickHits`・`GrowthWarnings`・`MiningRewards`・`ServerSwitches`）から外す

## 6. 新しい版の通知と更新のお知らせの待ちを 1 つに

- `UpdateCheckNotice` と `UpdateNotes` が別々に持っていた「ワールドに入ってからの tick を数え（ワールドを出たら 0 に戻す）、起動中に 1 回だけ出す」tick の処理を、`UpdateNotes.onClientTick` の 1 つにまとめる。`UpdateCheckNotice` は `@SubscribeEvent` を持たず、数えた tick を受け取る
- 出す時と条件は今と同じ: 更新のお知らせは 40 tick、新しい版の通知は 60 tick（Forge の確認がまだ終わっていなければ、さらに 200 tick まで毎 tick 確かめ直す）

## 7〜12. 使われていない・小さすぎる部品を消す

- 7: `WeakSpotSwitch`（`!on` を返すだけ）とテスト。`ToggleKeyHandler` で直接 `!` にする
- 8: `PlayerSwitches<K>` とテスト。`ServerSwitches` の中の、オフのプレイヤーの `Set<UUID>` 2 つにする
- 9: `ScheduledBoost.dispenseCountForFactor`（テストだけが使う）
- 10: `GrowthRoom.hasStageRoom`（テストだけが使う）
- 11: `MachineComboBoost.label`（`String.valueOf` と同じ）。`ComboHud` で `String.valueOf` を使う
- 12: `GuideBook.PAGES`（どこからも使われない）

## 13. 溜めを `HeldCharge` のまま公開する

- `MeleeCharge` / `ThrowCharge` の、`HeldCharge` に渡すだけのメソッド（持っているか・溜める・量・消す）をやめ、`public static final HeldCharge CHARGES` を呼ぶ側が直接使う
- 近接の上限（`meleeChargeMax`）・投げる物の上限なし（0）は、呼ぶ側で `CHARGES.add(player, amount, max)` に渡す

## 14. README・doc

- 遊び方は変わらないので、`doc/play.md`・ガイドの本・`doc/config.md`・`README.en.md`・説明文は直さない
- `doc/architecture.md` の `PlayerSwitches`・`MachineComboBoost.label`・`UpdateCheckNotice`・`PlayerText` / `ServerLang`・ゲージの部品の記述を、今の形に直す
- `doc/diagrams/`: 該当する図はないので直さない
- リリースの手順どおり（`CHANGELOG.md`・README の「最近の更新」・`doc/spec/README.md`・`weakspot.news.1.11.5`・`update.json`）

## 15. `CLAUDE.md` に書くこと

- 現行の版を 1.11.5 に。「次の作業」に 1.11.5（内部の整理）
- サーバーからプレイヤーに送る文の約束事: 接続できる一番古い版（`ACCEPTED_VERSIONS` の下限）より前に足したキーは `TextComponentTranslation` で送り、パッチで足したキーだけ `PlayerText.of`

## 16. ユーザーに確認してもらうこと

- 弓を引くと、照準の下のゲージが今までどおり（途中はオレンジ、引き切ると黄、過剰チャージの赤い目盛り 5 つ）
- 乗り物（ボート・馬など）で弱点に当てると、照準の上に水色のゲージ。ボートの加速も今までどおり
- 剣・斧の溜め、投げる物の溜めのゲージと「×n」が今までどおり。持ち替えると溜めが消える
- HOME キーで弱点のオン・オフ、K キーの「弱点マーカー」タブの種類ごとのオン・オフが今までどおり
- K キーの統計が開く（他プレイヤーとの統計・ランキングも）
- マルチプレイで、節目・コンボの段階・的当ての 1 位の知らせ、村人の繁殖の案内（しゃがんで素手で右クリック長押し）が自分の言語で出る
- 版が上がったので、ワールドに入ると更新のお知らせと、弱点の的 1 つが届く
