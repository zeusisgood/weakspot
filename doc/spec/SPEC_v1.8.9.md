# 弱点破壊 Mod 追加仕様書（v1.8.9）

`SPEC_v1.8.8.md`（Mod 1.8.8）に対する**パッチ**の仕様。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.8.9.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.8.8 がリリース済み
- この仕様書の内容は、Mod のバージョン **1.8.9** として、リリースする

---

## 0. バージョンと互換性

- バージョンは 1.8.8 → **1.8.9**（`build.gradle` の `version` と `WeakSpotMod.VERSION`）
- **主にコードの整理**。通信内容・保存データ・設定キーは変わらない。1.8.x 同士は、そのまま接続できる
- 見た目で変わるのは、種類の名前（5）と、設定画面の説明（6）だけ
- `WireCompatTest`・`HitHandlersTest`・`SyncedSettingsTest` がそのまま通ること（期待値を作り直さない）

## 1. やりたいこと（ユーザーの判断: 1.8.8 のあとの相談の 1・2・4、A〜E、G〜I、クラスの分割）

通信を変えずにできる整理を、1.9.0（通信を変える整理）の前にまとめて行う。

## 2. 種類の表（相談の 1・A）

- **種類ごとの変わらない情報は `common/HitKind` に持つ**: 初期の色（`defaultColor()`。今の `MarkerLook.defaultColor` の値）と、コンボの掛け数を使うか（`usesComboFactor()`。今の `ComboHud.FACTOR_KINDS`。機械は実際の速さを出すので別扱いのまま）
  - 収穫の色の特別扱い（ブロックの弱点の中で収穫だけマゼンタ）もここに寄せる
- **種類ごとの設定は `SyncedSettings` から 1 つの表で引く**: `enabled(kind)`（`…WeakSpotEnabled`。設定のない採掘・成長・機械・動物は true）と `minHitInterval(kind)`（`…MinHitIntervalTicks`）
  - 表は種類 → フィールド名の一覧（`WIRE` と同じ考え方）。クライアントとサーバーで同じ表を使う
  - 置き換える所: `ClientWeakSpotHandler.minHitInterval`、`MoveHits.minHitInterval`・`isEnabledOnServer`、`RightClickHits.minHitInterval`、`AimSpotKind` / `ScreenSpotKind` の `minHitInterval` の上書き（13 か所。土台で表から引く）、各 `*Hits` の `WeakSpotConfig.xxxWeakSpotEnabled` / `xxxMinHitIntervalTicks` の直読み
- 単体テスト: すべての種類に間隔の設定があること、`enabled` / `minHitInterval` が今の値と同じになること、色の値が 1.8.8 と同じこと

## 3. サーバーの設定の使い回し（相談の 2）

- 今: `SyncedSettings.fromConfig()`（約 80 項目をリフレクションで集め、`growthExtraBlocks` を読み直す）を、右クリックのたび（`RightClickTargets`）・ヒットのたび（`RightClickHits`・`AnimalHits`・`FishingHits`）に呼んでいる
- サーバー用の 1 つ（例 `SyncedSettings.server()`）を使い回す。作り直すのは、サーバーの起動時と、設定を送り直すとき（`SettingsSync.resendToAll`。`/weakspot reload` と、設定画面での変更 `onConfigChanged` から呼ばれる）
- 送る中身は今と同じ（`SettingsMessage` もこれを使ってよい）

## 4. 小さな共通化（相談の 4・B〜E）

- **4 改名**: `common/VehicleBoostMath` → `TimedBoostMath`（乗り物・はしご・走りの倍率。テストも改名）
- **B 死亡・ディメンション移動の後片付け**: 今 8 クラス（`ServerBoostTracker`・`RightClickHits`・`MoveHits`・`MeleeHits`・`ThrowHits`・`MarkerRelay`・`ServerStats`・`GrowthWarnings`）がそれぞれ `PlayerRespawnEvent` / `PlayerChangedDimensionEvent` を受けている。ログアウトと同じく `HitGate` の 1 か所で受け、各クラスの `forget(player, 理由)`（理由 = ログアウト・リスポーン・ディメンション移動）を呼ぶ。**各クラスで消すものは今と同じ**（理由ごとの違いは、各クラスの `forget` の中で分ける）
- **C 画面上のマーカーの描き方**: 「塗り・輪郭・中心の点・残像」を描く処理（`HudSpot`・`FishingSpot`・`ScreenSpots`）を `ScreenProjection` の 1 つの関数にする（濃さ・色は引数で渡し、見た目は今と同じ）
- **D 遅れて探すリフレクション**: `PortalHits` / `EnchantHits` の「初めて使うときに 1 回だけ探す」を `Reflect` の遅れて探す版（例 `Reflect.lazyField`）にする
- **E パケットの受け取り**: サーバーのスレッドで実行する同じ書き方（7 か所）を、小さな補助の関数にする

## 5. 種類のゲーム内の名前（相談の G。ユーザーの判断: 文書に揃える）

- 日本語のゲーム内の名前を、README・`doc/` と同じにする: **食事 → 飲食、投げる物 → 投擲物、走り → ダッシュ**
  - 対象: `ja_jp.lang` の `weakspot.kind.*`、ガイドの本の題・本文、統計・「弱点マーカー」タブ、設定の説明、節目の文言など、この 3 つを呼んでいる所すべて（本のページからはみ出さないこと）
  - 英語はそのまま（Eating / Throwing / Sprint）
- コード上の名前（`HitKind.EAT` / `THROW` / `SPRINT`、`key()`）と設定キーは変えない
- `doc/play.md` の「走り ×1.25」などの例も「ダッシュ ×1.25」に

## 6. 設定画面の英語の説明（相談の H）

- 1.1.3 より前からある項目など、翻訳キーの説明（`weakspot.general.<キー>.tooltip`）がない項目すべてに、英語と日本語の説明を足す（日本語は今の `@Config.Comment` の文を元にする）。英語の画面で日本語のコメントが出なくなる
- `@Config.Comment` は残す（`weakspot.cfg` のファイルの中のコメント）

## 7. クラスの分割（相談の I と、1.9.0 のメモから前倒し）

- **`ComboHud`（413 行）**: 演出（段階の音・花火・「1000 COMBO!」のタイトル）を `ComboEffects` に分ける。`ComboHud` は数字・バー・種類の表示
- **`ClientWeakSpotHandler`（約 480 行）**: 責任ごとに分ける（例: ブロックの弱点（採掘・成長・機械・収穫）の照準と出し直し、動物の弱点、採掘のブースト（`onBreakSpeed`）、連続ヒットと共通のヒット処理（`registerHit`・`canHitNow`・`clientTick`））。外から呼ばれている名前は、なるべく残すか一度に直す
- **`StatsScreen`（約 650 行）**: タブごとのクラス（統計・サウンド・弱点マーカー）と、共通の枠（タブ・ボタン・ホイールの送り）に分ける
- 見た目・操作は変えない

## 8. README・doc・ガイドの本・お知らせ

- `doc/play.md`: 5 の名前の例
- `doc/architecture.md`: 新しい部品（種類の表・`SyncedSettings.server()`・`TimedBoostMath`・後片付け・分けたクラス）
- `CHANGELOG.md`・README の「最近の更新」: 内部の整理、種類の名前を統一（飲食・投擲物・ダッシュ）、設定画面の英語の説明を追加。通信内容・保存データ・設定キーに変更なし。1.8.x 同士はそのまま接続可能
- 更新のお知らせ `weakspot.news.1.8.9`: 「種類の名前を統一し、内部を整理した」

## 9. `CLAUDE.md` に書くこと

- 現行を 1.8.9 に
- 「弱点の種類を足すときに直す所」を、種類の表（`HitKind` の色・掛け数、`SyncedSettings` の表）に合わせて短くする

## 10. ユーザーに確認してもらうこと（今までと同じに動くこと）

- すべての種類の弱点が出て当たり、効果が出ること（特に、設定のオン・オフとヒット間隔。サーバーの設定でオフにした種類が出ないこと）
- 死亡・ディメンション移動・ログアウトのあとも、弱点・加速・溜めがおかしくならないこと（走りの加速が残らない、溜めが消えるなど今までどおり）
- 睡眠・エンチャント・照準のまわり・釣りのマーカーの見た目が変わっていないこと
- コンボの演出（10・25…1000 の音・花火・タイトル）と、種類の表示
- K キーの画面（統計・サウンド・弱点マーカーの各タブ、ホイールの送り、色・形の変更、ガイド・設定画面のボタン）
- 画面・本・統計で「飲食・投擲物・ダッシュ」になっていること
- 英語にしたときの設定画面の説明が英語で出ること
- `/weakspot reload` で設定を変えると、すぐに効くこと（サーバーの設定の使い回し）
