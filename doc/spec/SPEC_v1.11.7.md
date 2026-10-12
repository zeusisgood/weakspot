# 弱点破壊 Mod 追加仕様書（v1.11.7）

`SPEC_v1.11.6.md`（Mod 1.11.6）に対する**パッチ**の仕様。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.11.7.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.11.6 がリリース済み
- この仕様書の内容は、Mod のバージョン **1.11.7** として、リリースする
- 内容は、過剰設計の監査（ponytail）の 5 件のうち 1 と 4（ユーザーの判断）と、マルチでエリトラが引き戻されるときの案内（§3。文書だけ）。次のパッチと一緒に出す予定だったが、これだけで 1.11.7 として出す（ユーザーの判断）
- 遊び方・見た目・通信内容は変えない

---

## 0. バージョンと互換性

- バージョンは 1.11.6 → **1.11.7**（`build.gradle` の `version` と `WeakSpotMod.VERSION`）
- この仕様書の §1・§2 は内部の整理だけ。通信内容・設定・保存データ・翻訳キーは変わらない。`ServerFeatures.since` も要らない
- 1.11.x 同士はそのまま接続できる

## 1. ヒットの間隔の確かめを、前の条件の `if` にまとめる

- 次の 9 つの `onHit` は、種類ごとの条件で `return` したあと、`if (!HitGate.ready(player, HitKind.X)) return;` をもう一段書いている。これを直前の `if` の条件の最後に `|| !HitGate.ready(player, HitKind.X)` として足す
  - `server/AnimalHits`・`BowHits`・`EatHits`・`EnchantHits`・`MeleeHits`・`PortalHits`・`SleepHits`・`ThrowHits`・`VehicleHits`
- `ready` は**最後**に置く（今と同じ順で、種類ごとの条件が外れたときは `HitBudget` を見ない）。`mark` の位置は変えない
- 9 か所とも、直前の `if` は `return` するだけ（確かめ済み）なので、動作は変わらない
- 約 27 行減

## 2. `BowMath.nextHorizontalYaw` の 4 引数の版を消す

- `nextHorizontalYaw(lookYaw, prevYaw, fovDegrees, random)` は `yawSign = 0`（左右はランダム）で 5 引数の版を呼ぶだけで、使っているのは `BowMathTest.horizontalYawIsBesideTheCrosshair` だけ
- テストを `BowMath.nextHorizontalYaw(170, prev, 0, 70, random)` に直し、4 引数の版を消す。説明のコメント（視線から左か右にずらす・前の yaw から離す・一周をまたぐ）は 5 引数の版のコメントに移す
- 約 4 行減

## 3. マルチでエリトラが引き戻されるときの案内（文書だけ）

- バニラのサーバー（`NetHandlerPlayServer#processPlayer`）は、1 回の移動が 1 tick あたり約 10 ブロック（エリトラで飛んでいる間は約 17.3 ブロック。距離の 2 乗で 100 / 300）を超えると、元の位置に引き戻す（ログに `moved too quickly!`）。シングルプレイ（と LAN のホスト本人）では確かめない
- エリトラの弱点は、1 ヒットで今の速さに `elytraBoostPower` × コンボの掛け数を足す（上限なし。初期値 1.5、コンボ 300 で +6）。続けて当てると、マルチで引き戻されることがある
- コードは変えない（速さの上限は付けない。ユーザーの方針）。サーバーの管理者向けに、`/gamerule disableElytraMovementCheck true`（エリトラで飛んでいる間だけ、この確かめを止めるバニラの設定。ワールドに保存される）を勧める文を書く。あわせて、`server.properties` の `allow-flight=true` ではこの引き戻しは止まらないことも書く（ユーザーの判断: 両方書く）
  - `doc/install.md` の「導入先」の専用サーバーの説明の下に 1 項目
  - `doc/play.md` のエリトラの節に 1 行（「マルチで速く飛ぶと引き戻されるときは、サーバーの管理者に…」）
- 1.12.2 の `server.properties` には、この確かめを止める項目はない（`allow-flight` は「浮いている」ことでキックされるのを止めるだけ）。Spigot・Paper の `moved-too-quickly-multiplier` は Forge のサーバーにはない
- ボート（足す量は 1 tick 8 ブロックまで）は、氷の上で 10 に近づくが、マルチで試してもらって引き戻されなかった（ユーザーの確認）ので書かない
- ガイドの本には書かない（サーバーの設定の話なので）

## 4. 見送ったもの（監査の残り）

- 3 件とも、行は減るが失うものがあるので直さない（ユーザーの判断）
  - `GrowthProgress.fraction` → `MachineProgress.ratio`: 中身は同じだが、作物のクラスが「機械」のクラスに頼る形になる
  - `MachineComboBoost.multiplier` → `TimedBoostMath.multiplier`: 今は同じ結果だが、機械に乗り物・はしご・走りの「上限 0 = なし」の意味が入り込む
  - `PlayerRules.canUse` を呼び出し側に書き込む: クライアントとサーバーで同じ条件を使うための置き場所がなくなる
- 0〜1 に収める `Math.max(0, Math.min(1, x))`（18 か所）も、どれも 1 行なのでまとめない
- 1.11.6 のあとの相談で見送ったもの（ユーザーの判断）:
  - 成長バーを `age` / `stage` 以外の整数の状態にも出す: 例の IC2 のゴムの木は列挙の状態なので効かず、耕地の水分などを間違ってバーにする心配が大きい。既知の問題のまま
  - 重さ: レッドストーンの部品の前倒し（`ScheduledTicks.boost`）は、予約を持つ部品ごとに 1 tick に 1 回、ディメンションの予約を全部調べる（バニラの `getPendingBlockUpdates`）。報告が来るまで何もしない。重いと分かったら、予約の時刻を覚えて全部調べるのを動かす瞬間だけにする（`doc/roadmap.md` にメモ）

## 5. README・doc

- `doc/roadmap.md` の「いつかやる」に、§4 の重さのメモ（レッドストーンの前倒しの直し方）を足す
- §3 の `doc/install.md`・`doc/play.md` のほかは、遊び方が変わらないので、ガイドの本・`doc/config.md`・`README.en.md`・説明文は直さない
- `doc/architecture.md`・`doc/diagrams/`: `nextHorizontalYaw` の 4 引数の版も、`*Hits` の `if` の並びも書いていないので直さない（`HitGate` の `ready` / `mark` の説明はそのまま正しい）
- リリースの手順どおり（`CHANGELOG.md`・README の「最近の更新」・`doc/spec/README.md`・`weakspot.news.1.11.7`・`update.json`）。§1・§2 は更新履歴に「内部の整理（小さな重複の削除）」とだけ書く。お知らせ（`weakspot.news.1.11.7`）は「内部の整理と、マルチでのエリトラの引き戻しの対処を説明に追記」

## 6. `CLAUDE.md` に書くこと

- 現行の版を 1.11.7 に。「次の作業」に 1.11.7（内部の整理・マルチでエリトラが引き戻されるときの gamerule の案内）

## 7. ユーザーに確認してもらうこと

- §1・§2 だけなら、遊んで分かる違いはない。CI（テスト）が通れば十分
- 念のため: 動物・弓・食事・エンチャント・近接・ネザーゲート・睡眠・投げる物・乗り物の弱点が、今どおり当たって効く
- 近接の弱点が、今どおり照準の左右に出る
- マルチで gamerule を `true` にしたサーバーで、コンボを高くしてエリトラの弱点を続けて当てても、引き戻されない
