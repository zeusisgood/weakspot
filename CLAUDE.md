# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

Fortnite の「弱点（クリティカル）」採掘を Minecraft に持ち込む Mod。対象は **Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860**。
仕様の正本は `SPEC_v1.0.md`。数値・挙動・MVP 完了条件（§12）・スコープ外（§13）はそちらを参照し、仕様と食い違う実装をする場合はユーザーに確認する。

## 開発環境・コマンド

- devcontainer で **JDK 8** を使う。ビルドは公式 MDK ベースの **ForgeGradle 3 + Gradle 4.9**（仕様書の FG 2.3 ではない）。Gradle 5 以降の構文は使えない（依存は `compile` / `testCompile`）。
- ビルド + テスト: `./gradlew build` → 成果物は `build/libs/weakspot-<version>.jar`（reobf 済み）
- テストのみ: `./gradlew test`、1クラスだけ: `./gradlew test --tests com.example.weakspot.common.WeakSpotPlacerTest`
- 専用サーバー起動: `./gradlew runServer`（作業ディレクトリは `run/`、`nogui` 付き。`run/eula.txt` は同意済み）。止めるときはコンソールで `stop`。
  - 起動ログの `module-info.class ... IllegalArgumentException` は FG3 + 1.12 でいつも出るノイズで、無視してよい。
- クライアント確認: コンテナ内では画面を出せない。ビルドした jar をホスト側 Minecraft（Forge 1.12.2）の `mods` に入れて確認する。描画・ヒット判定・体感速度は Claude が検証できないので、ユーザーに確認を依頼する。
- Mod のバージョンは `build.gradle` の `version` と `WeakSpotMod.VERSION` の2か所にある。変えるときは両方を揃える。

## アーキテクチャ

クライアントとサーバーの**両方に Mod が必要**（`acceptableRemoteVersions` 未指定なので同じバージョンが必要）。パッケージは `com.example.weakspot`。

- `common/`: Minecraft に依存しない純粋な計算（面の (u,v) 座標変換、弱点の配置、ブースト量）。単体テストはここだけにある。1.7.10 への移植を見込んで、MC クラスを持ち込まない。
- `client/`（`@EventBusSubscriber(value = Side.CLIENT)`。専用サーバーではロードされない）: 弱点の状態、ヒット判定、描画、ヒット音、クライアント側のブースト。
  - ヒット判定は `RenderWorldLastEvent` で**毎フレーム**行う（tick 単位だと素早い照準移動を取りこぼす）。ブーストの時間枠は `ClientTickEvent` START で増える `clientTick` で数える。`PlayerControllerMP` は tick ごとに進捗を積算するので、枠内の tick だけ倍率を掛ければよい。
- `network/HitMessage`: クライアント→サーバーのヒット通知（BlockPos のみ）。唯一のパケット。
- `server/ServerBoostTracker`: 論理サーバー側。`LeftClickBlock` で「今どのブロックを破壊中か」と開始 tick を記録し、ヒット通知はそのブロックと一致したときだけ受け付ける。アクセストランスフォーマーは使っていない。

### 重要: サーバー側のブーストは時間枠ではない

1.12.2 のサーバー（`PlayerInteractionManager#blockRemoving`）は進捗を積算せず、破壊完了の通知を受けた瞬間に「**現在の**破壊速度 × (経過tick+1) ≥ 0.7」で判定する。そのため、仕様どおりサーバー側でも時間枠の間だけ倍率を掛けると、判定の瞬間が枠外のときにブーストが効かない。そうなるとブロックが戻り、後から通常速度で壊れる。
そこでサーバーは、ヒットごとに追加進捗 `(倍率-1)×継続tick` を貯め、そのブロックの `BreakSpeed` に `1 + 追加tick/(経過tick+1)` を掛ける（`BoostMath`）。こうすると積算した場合と同じ結果になる。破壊速度まわりを変えるときは、クライアント（積算）とサーバー（瞬間判定）の両方で結果が一致するかを確認すること。

- ヒット間隔の制限は、クライアントが `minHitIntervalTicks`、サーバーはネットワークの揺らぎを見込んで 2 tick 甘くしている。サーバーがヒットを拒否してクライアントだけブーストされると、ブロックが一度戻って見える。
- ブーストの目安: H tick ごとにヒットすると、速度は通常の `1 + (倍率-1)×継続/H` 倍。初期値なら `1 + 12/H` で、約 0.6 秒ごとのヒットで2倍、`minHitIntervalTicks=6` で最大3倍。

設定は `@Config`（`config/weakspot.cfg`）。MVP ではサーバーとクライアントで同じ値を使う前提で、同期はしていない。
