# 弱点破壊 Mod 追加仕様書（v1.11.9）

`SPEC_v1.11.8.md`（Mod 1.11.8）に対する、**文書だけ**の整理の仕様。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.11.9.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.11.8 がリリース済み
- 内容は、文書の肥大化のレビューの指摘（2〜9）のうち、ユーザーが「おすすめで」と決めたもの（§2〜§9）と、仕様書を残す期間の決まり（§10）
- PR は 1 つ（「文書の整理」）。この仕様書と `doc/spec/README.md` の 1 行もこの PR に入れる
- コード・遊び方・見た目・通信内容・設定・保存データは変えない

---

## 0. バージョンと互換性

- 文書だけの変更なので、**版は上げない**（`build.gradle` の `version` と `WeakSpotMod.VERSION` は 1.11.8 のまま。`CHANGELOG.md`・README の「最近の更新」・`weakspot.news.*`・`update.json` も直さない）
- 仕様書の名前は 1.11.9 とする（ユーザーの判断）

## 1. 共通の方針

- `doc/architecture.md` は「仕組み」（クラス名・イベント・データの置き場所・理由）だけにする
  - 遊んで分かること（色のコード・段階のしきい値・ボタンの流れ・画面の文言・ご褒美の段階など）は `doc/play.md` に任せる
  - `doc/play.md` に書いていない細かい数値も、コード（`ComboTier`・`ComboMilestones`・`TargetRules` などの定数）にあるので消すだけにする。**`doc/play.md` には移さない**（移すとガイドの本も直すことになるため）
- `CLAUDE.md` に書いてある決まり（「〜する」「〜しない」の指示の文）は `doc/architecture.md` で繰り返さない。仕組みの説明と、`CLAUDE.md` にない理由の文は残す
- 1.11.8 §3 の「残す」（`ServerFeatures.since` / `PlayerText.clientSince` の版、理由の注記、設定の移行の番号、メッセージの番号）はここでも残す
- 迷ったら残す。意味が変わる書き換えはしない

## 2. 遊び方と重なる細かい所を削る（指摘 2）

`doc/architecture.md` の次の項目から、遊び方の細かい所を消し、仕組みだけを残す。

| 項目 | 消すもの（例） | 残すもの（例） |
|---|---|---|
| ヒット音（`HitSounds`、L22） | プリセットの巡り方（左・右・Shift）、`MyPresetsScreen` の「上書き？」（3 秒で戻る）・「保存しました」、[ランダム] の位置、試聴の音数、和音を重ねる段階の数、音量の倍率 | 使うクラスと設定（`HitStreak`・`HitPitch`・`HitScale`・`SoundPreset`・`soundPresets` の形・`SoundTab.mouseClicked`・`sameSound`・`RANDOM_SOUNDS`・`playAccent` / `duckUntil` / `breakNotes`）と、理由（ピッチの上限 2.0 のため旋律より下に重ねる など） |
| コンボの表示（`ComboHud`、L23） | 色の段階・節目のしきい値の並び、段階ごとの演出（分散和音・花火の数・タイトルの文字）、和音の段階ごとの形、「MAX n」の tick 数 | 描くイベント、`ComboTier` / `ComboMilestones` / `ComboEffects` / `MilestoneEffects` の役割、種類の表示とゲージの出す条件とメソッド、機械・ディスペンサーの速さの求め方、全員への知らせの仕組み（`ServerStats.countStreak` → `MiningRewards.broadcastCombo`） |
| 落下（L62） | 色 `#EAF6FF`、「生存圏！」「ノーダメージ」、☠ の点滅、雲の粒と音 | 見込みの落下距離の求め方（`FallDamage.landingFall`）、両側で `fallDistance` を減らすこと、ダメージの式（バニラと同じ式）、九死に一生の判定（`closeCall`・10 tick・`LivingDeathEvent`）、`SyncedSettings` で送る項目 |
| 的当て（L65） | 的の見た目（赤白・✕ の色）、カウントダウンと時間の tick 数、+1 / −5、結果の板の秒数、配る個数、ガイドの本のページ数 | メッセージ（START / SCORE / END / RECORD）と中身、ラウンドの始まりと終わり（`HitGate.Leave`）、向きの持ち方（`anchorYaw` / `anchorPitch`）、保存のキー（`targetBest`・`targetRounds`・`targetsGiven`・`targetGiftVersion`）、段階を進捗と比べる理由、`KindSwitches.isEnabled` で他の種類を止めること |
| ご褒美（L66） | 銅 25・銀 50・金 75・虹 100 | `TargetRules.Tier`・`MarkerShape.STAR`・`MarkerLook.customColor`・`OwnHits.register`、届いていないワールドでは使わないこと、上位 10 人を `StatsMessage` の最後に足したこと |
| 「弱点マーカー」タブ（L74） | 12 色・◀ ▶ と打ち込み、K キーの画面の題、ホイールで送ること | `disabledKinds` → `SwitchMessage` → `ServerSwitches.isEnabled`、色と形が効く描画クラスの一覧（`MarkerLook.palette` / `shape`） |

- ほかの項目にも同じ種類の細かい所があれば、同じ方針で消してよい
- 約 15K 字減の見込み

## 3. 「設定」の節を縮める（指摘 3）

`doc/architecture.md` の「設定」の節は、次だけを残す。

- 残す
  - 仕組みの説明: カテゴリ 2 段と入れ子のクラス（Forge の決まりで static でないフィールド）、`throw` → `throwing`（`@Config.Name("throw")`）、根の `general` には `configVersion` だけ、`WeakSpotConfig.setting(名前)` / `settings()`
  - 設定ファイルの移行（`configVersion` 6・7 の中身、`preInit` の間に保存できない理由、`init` でも呼ぶこと、`ConfigMigrationTest`）
  - 設定の読み直し（`ConfigManager.sync` の向きと `reloadFromFile`）
- 消す（`CLAUDE.md` の「設定の約束事」「バージョン」と重なる）
  - `[サーバー]` の扱い（`SyncedSettings` で送る・`WeakSpotConfig` に書き込まない・サーバーだけの項目は送らない）
  - `[クライアント]` の項目の列挙（古くなりやすい。一覧は `doc/config.md`）
  - 設定の足し方（`SettingGroups`・`doc/config.md`・lang のキーと説明の形）
  - パケット・保存形式を変えたらマイナーを上げること
- 約 2.5K 字減

## 4. `CLAUDE.md` の決まりの言い直しを消す（指摘 4）

`doc/architecture.md` の次の指示の文を消す。同じ行の仕組みの説明は残す。

| 行 | 消す | 残す |
|---|---|---|
| L10 | 「**両方に Mod が必要**」の決まりの言い方 | `ACCEPTED_VERSIONS` で同じマイナー同士が接続できる仕組みの説明 |
| L33 | 行ごと（画面の文字列を両方の lang に足す） | — |
| L52 | 太字の「パッチで…`ServerFeatures.since` で囲み」の指示の文 | `SyncedSettings.serverVersion` で版が届くこと、古いサーバーで弱点を出さない理由（弱点が出るのにバニラの動作も起きる食い違い）、`HitKind.byId` が null を返すこと |
| L53 | 「HUD に図形を描く処理を足すときも、`beginOverlay` を使う」 | 裏向きの面になる理由と `HudSpot.beginOverlay` / `endOverlay` |
| L63 の末尾 | 「**移植のときは版ごとに調べ直す**」の指示 | 版ごとの違いの例（1.13 の斧、1.14 の鉄）は、移植のときの手がかりとして残す |
| L79 の中 | 「最後の tick との比較を新しく書かない」の指示 | まとめて届いたヒットを落とす理由（issue #38）は、`HitBudget` の説明として残す |
| L118 の末尾 | 太字の「**採掘のブーストは、クリックのたびに両側で捨てる**」 | — |
| L130 の中 | 「**サーバーからの文の決まり**」の指示の言い方 | `PlayerText.of` / `ServerLang` / `PlayerText.clientVersion` / `ModVersions.hasKeysSince` の仕組み |

- **L150 は残す**（「重要」の節の理由の文。「START だけ送るので、サーバーは何も返さない」は `CLAUDE.md` にない）
- 約 1.5K 字減

## 5. 古くなる一覧を消す（指摘 5）

- L12 の `HitKind` の名前の並び（「ほかに動物 ANIMAL・…・落下 FALL」）を消す。一覧はコード（`HitKind.java`）にある
- L14 の `common/` のクラスの並びを、「Minecraft に依存しない純粋な計算。クラスの一覧は `common/` を見る。単体テストは主にここ」の形の 1 行にする（「1.7.10 への移植を見込んで」も消す。単体テストの置き場所の説明は残す）
- 約 1.5K 字減

## 6. 後片付けの二重の説明を消す（指摘 6）

- L79 の「**ログアウトの後片付けは `HitGate.onLogout` の 1 か所**（…）」を消す。L89 の「後片付けの 1 か所」（`forgetAll`。ログアウト・リスポーン・ディメンション移動）を残す

## 7. リフレクションの説明を 1 か所にする（指摘 7）

- 詳しい説明（MCP 名と SRG 名、3 引数の `ReflectionHelper.findField` を使わない理由、SRG 名の調べ方、両方の環境での確かめ方）は `doc/development.md` だけに置く
- `doc/architecture.md`
  - L28（`MiningProgress`）: 「`Reflect.field` で読む（見つからなければバーを出さない）」と、tick ごとに積まれるので補間する説明だけにする
  - L40（`Reflect`）: 使うクラスとフィールド・メソッドの名前（`VillagerTrades`・`FishingHits`・`MiningProgress` など）と、「名前の試し方は `doc/development.md`」の 1 文にする
- `CLAUDE.md` の 1 行（「非公開のフィールドは…`Reflect`」）は変えない

## 8. `doc/development.md` の重複を消す（指摘 8）

- 「バージョンの方針」の節: 本文を「細かい決まりは [CLAUDE.md](../CLAUDE.md) の「バージョン」」の 1 行にする（見出しは残す）
- 「開発環境の細かいこと」
  - Gradle 4.9（`compile` / `testCompile`）の箇条を消す（冒頭の段落と `CLAUDE.md` にある）
  - JDK 8 を入れる箇条を消す
  - 429 の箇条は、説明の文を消して、再実行のループのコマンドは残す（「Maven Central の 429 の再実行（`CLAUDE.md` のとおり 30 秒ずつ 5 回）:」＋ コマンド、の形）
- 「ブランチの片付け」の 1 つ目の箇条（自動で消える・新しい名前で切る）を消す
- `README.md` の文書の表の development.md の説明「開発（ビルド・バージョン方針・リリースの手順）」から「バージョン方針」を外す
- 約 1K 字減

## 9. クライアントのクラスの分割を書き直す（指摘 9）

- L94 の経緯の書き方（「…から…を分けた」）を、1 クラス 1 行の役割に書き直す
  - `ClientWeakSpotHandler`: ブロック・動物の弱点の照準と出し直し
  - `MiningBoost`: 採掘のブースト（`onBreakSpeed`・`comboFactor`・瞬間破壊の判定）
  - `OwnHits`: 自分の連続ヒット（`STREAK`・`register`・`canHit`）
  - `OwnSpotBars`: 耐久・機械・成長・動物のバーの値
  - `ComboEffects`: コンボの段階の音・花火・タイトル
  - 統計画面のタブ: `StatsScreen` と各タブのクラス
- 消さない（`OwnSpotBars` の説明がここにしかないため）

## 10. 仕様書を残す期間（追加の提案）

ユーザーの判断: 案 A（マイナーで区切る）。本数の上限は付けず、マイナーが変わるまで待つ。

### 10.1 決まり

- 新しいマイナー（例: 移植の 1.12.0）の仕様書を書いたら、前のマイナーの仕様書を `doc/spec/` から消す（タグ `v<前のマイナーの最後の版>` の `doc/spec/` に残る。1.10.2 のときと同じ形）
  - 消す前に、消す仕様書にしか書いていない今の挙動を、`doc/play.md`・`doc/config.md`・`doc/architecture.md` に移す
  - コードのコメントからの参照（今は `HeldItems.java`・`HeldItemsTest.java`・`TargetRules.java` の 3 か所が `SPEC_v1.11.0.md` を指す）を、タグのリンクか `doc/architecture.md` に直す
  - `doc/spec/README.md` の表から消した版の行を消し、タグへのリンクの 1 行を足す（今の 1.10.2 の行と同じ形）
- 同じマイナーの間は、本数が増えても消さない
- 今の版の仕様書は消さない（`ReleaseFilesTest` が一覧にあることを確かめている）
- **この PR では仕様書を消さない**（1.11.x の間なので）。決まりを書くだけ

### 10.2 正本の決まりを変える

- 今の挙動の正本は `doc/play.md`・`doc/config.md`・`doc/architecture.md`（とコード）だけにする。置いてある仕様書は「今のマイナーの経緯と決定の記録」とする
- 「仕様書に書かれていないことは、その前の版と現行実装のまま」と「置いてある仕様書が実装と違うと分かったら、本文は書き換えずに（注: 実装では…）を足す」は残す（記録としての扱い）
- 今回の作業で、1.11.0〜1.11.8 の仕様書にしか書いていない今の挙動がないかを確かめる
  - 仕組み（クラス・通信・保存のキー・理由）なら `doc/architecture.md` に足す
  - 遊んで分かること（`doc/play.md` にないもの）が見つかったら、足さずに一覧にして PR の本文で報告し、ユーザーに聞く（`doc/play.md` を直すとガイドの本も直すことになるため。§1 の方針と同じ）
  - 数値でコードの定数にあるものは、足さなくてよい

### 10.3 書き換える所

- `CLAUDE.md` の「プロジェクト概要」の仕様書の箇条（L9）: 正本の文を 10.2 の形にし、10.1 の決まり（マイナーが変わったら前のマイナーの仕様書を消す・消す前に移す）を足す
- `doc/spec/README.md` の冒頭の箇条: 正本の文を 10.2 の形にし、「新しいマイナーを始めたら、前のマイナーの仕様書はタグに移す」を足す
- `doc/development.md` の「リリースの手順」: マイナーを上げるときの項目に、10.1 の作業を 1 行足す

## 11. README・doc

- `doc/spec/README.md` の表の一番上に `| [1.11.9](SPEC_v1.11.9.md) | 文書のみ |` を足す
- 直すのは `doc/architecture.md`・`doc/development.md`・`README.md`（§8 の 1 か所）・`CLAUDE.md` と `doc/spec/README.md`（§10.3）だけ
- 直さない文書: `doc/play.md`・`doc/config.md`・ガイドの本・`README.en.md`・`doc/store/description.md`・`CHANGELOG.md`（遊び方・設定・版が変わらないため）、`doc/roadmap.md`（予定が変わらないため）、`doc/diagrams/`（クラスの関係も呼ぶ順も変わらないため）。PR の本文にこの理由を書く
- 全部で約 2 万字減（`doc/architecture.md` は約 5.4 万字 → 約 3.4 万字）の見込み

## 12. `CLAUDE.md` に書くこと

- §10.3 のとおり（仕様書の箇条の正本の文と、残す期間の決まり）。それ以外の決まりは変えない（§4 では `CLAUDE.md` を正本として残す側）

## 13. ユーザーに確認してもらうこと

- 文書だけ。`doc/architecture.md` の差分で、仕組みの説明や理由が消えていないかを見てもらう
- §10.2 で、仕様書にしか書いていない遊んで分かることが見つかったら、PR の本文の一覧を見て、`doc/play.md` に足すかを決めてもらう
- 特に §2 で消した数値（しきい値・色など）は、コードの定数を見ないと分からなくなる。それでよいか
