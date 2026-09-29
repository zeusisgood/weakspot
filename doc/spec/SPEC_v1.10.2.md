# 弱点破壊 Mod 追加仕様書（v1.10.2）

`SPEC_v1.10.1.md`（Mod 1.10.1）に対する**パッチ**の仕様。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.10.2.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.10.1 がリリース済み
- この仕様書の内容は、Mod のバージョン **1.10.2** として、リリースする

---

## 0. バージョンと互換性

- バージョンは 1.10.1 → **1.10.2**（`build.gradle` の `version` と `WeakSpotMod.VERSION`）
- 通信内容・保存データは変わらない。1.10.x 同士はそのまま接続できる
- 設定は、クライアントの項目を 1 つ足す（4 の `comboFactorGaugeEnabled`。クライアントだけが使い、送らない）。`weakspot.cfg` の形（`server`／`client` のカテゴリ）は変えない（移行なし）
- サーバーの変更（5 のリセット、7 の釣り）は、サーバーを 1.10.2 にしたときに効く。クライアントの変更（1〜4・6）は、1.10.0 / 1.10.1 のサーバーでも効く

## 1. 設定画面をまとめ直す（1.10.1 のあとの相談。ユーザーの判断: 案 B）

- 今の設定画面は、ファイルの形のまま「共通 / サーバー（21 のカテゴリ）/ クライアント（5 のカテゴリ）」の順にたどる。サーバーとクライアントの区別は、画面では要らない（ユーザーの判断）
- **設定画面だけ**を、種類・用途でまとめ直す。ファイル（`weakspot.cfg`）の中は今のまま `server.*` / `client.*`（移行なし。`/weakspot reload` とファイルを直接直す人のため）
- 作り方: `WeakSpotGuiFactory` が、`ConfigManager` の `Configuration` から項目（`Property`）を取り出し、下の表のまとめ（`DummyCategoryElement` と `ConfigElement`）を組んで `GuiConfig` に渡す。変えた値の保存と反映は今の流れのまま（`OnConfigChangedEvent` → `ConfigManager.sync` → サーバーの値の再送）
- まとめ（画面に出る順。項目は、今のカテゴリの中の順のまま足していく）:

| まとめ | 入る項目（今のカテゴリ） |
|---|---|
| 共通 | `server.general` の `giveGuideBook`、`client.updates`（`lastSeenVersion` を除く） |
| 音 | `client.sound` |
| マーカー | `client.markers`（`animalSpotSeeThrough` を除く）、`server.general` の `markerShareRange` `markerSendMinIntervalTicks` |
| コンボ | `client.combo`（`showOthersMilestones` を除く） |
| 節目 | `showOthersMilestones`、`server.milestones` |
| 採掘 | `server.mining`、`server.repair`、`client.hud` の `blockHealthBarEnabled` |
| 成長 | `server.growth`、`growthBarEnabled` |
| 収穫 | `server.harvest` |
| 機械 | `server.machine`、`machineBarEnabled` `machineParticlesVisible` |
| 動物 | `server.animal`、`animalSpotSeeThrough` |
| 釣り | `server.fishing` |
| 弓 | `server.bow`、`bowDrawBarEnabled` |
| 近接 | `server.melee`、`meleeChargeBarEnabled` |
| 投擲物 | `server.throw`、`throwChargeBarEnabled` |
| 飲食 | `server.eat` |
| 乗り物 | `server.vehicle`、`vehicleBoostBarEnabled` |
| はしご | `server.ladder`、`ladderBoostBarEnabled` |
| ダッシュ | `server.sprint`、`sprintBoostBarEnabled` |
| エリトラ | `server.elytra` |
| ネザーゲート | `server.portal` |
| 泳ぎ | `server.swim`、`swimBoostBarEnabled` |
| 睡眠 | `server.sleep` |
| エンチャント | `server.enchant` |

- **画面に出さない項目**: `configVersion`（移行の目印）、`lastSeenVersion`（Mod が書き換える。どちらも「書き換えないでください」の項目）。ファイルには残す
- まとめの名前は、新しい翻訳キー `weakspot.gui.<まとめ>`（と説明 `.tooltip`）を日英に足す。ファイルのカテゴリの名前（`weakspot.server.mining` など）は、ファイルと管理コマンドの説明用に残す
- サーバーで遊んでいる間の、画面の上の注意書き（`weakspot.config.remoteNotice`）を、説明の一文に合わせて「マルチプレイ中：説明に「サーバー側設定が優先」とある項目は、サーバーの値が使われます」に（ユーザーの確認で、今の文「（各自の設定を除く）」が分かりにくかったため）
- テスト（`resources/LangFilesTest` などに足す）: すべての設定が、ちょうど 1 つのまとめに入っていること（画面に出さない 2 つを除く）。すべてのまとめに名前と説明があること

## 2. 設定の説明の書き方（ユーザーの判断）

- サーバーの値が優先される項目（`server.*`）の説明（翻訳の `.tooltip`）の先頭の「[サーバー]」を、**「マルチプレイ時、サーバー側設定が優先されます。」**（英語 "In multiplayer, the server's setting takes priority."）に替え、改行してから今の説明を続ける
- クライアントの項目（`client.*`）の説明の先頭の「[クライアント]」は外す（断り書きなし）
- ファイルの中の説明（`@Config.Comment`）は、今の「[サーバー]」「[クライアント]」のまま（ファイルを直接開く人には、こちらが分かりやすいため）
- テスト: `server.*` の説明がこの一文で始まり、`client.*` の説明が「[クライアント]」で始まらないこと

## 3. `configVersion` を画面に出さない

- 1 のとおり（ファイルには残す。移行の目印として必要）

## 3b. 項目の名前を日本語・英語に（ユーザーの判断）

- 今の設定画面は、項目の名前に設定のキー（`boostMultiplier` など）がそのまま出ている。Forge の設定画面は、項目の翻訳キー（`weakspot.<カテゴリ>.<キーを小文字にしたもの>`。説明の `.tooltip` と同じ形の、`.tooltip` のないもの）に翻訳があれば、それを名前に出すので、**すべての項目の名前を日英の lang に足す**
- 名前は短く（日本語 12 字・英語 25 字くらいまで。横に入力欄があるため）。詳しいことは説明に書く。例: `boostMultiplier` = 「採掘の速さの倍率」/ "Mining speed multiplier"、`swimBoostBarEnabled` = 「加速のゲージを表示」/ "Show the boost gauge"、`myHitVolume` = 「自分のヒット音の音量」/ "My hit volume"
- **説明の最後に、ファイルのキーを書く**（例: 「ファイル: server.mining.boostMultiplier」/ "File: server.mining.boostMultiplier"）。名前が日本語になっても、`weakspot.cfg` を直接直す人が探せるように
- テスト（`resources/LangFilesTest`）: すべての項目に日英の名前があること。説明の最後がファイルのキーであること

## 4. コンボのボーナスのゲージ（ユーザーの判断）

- 今は、掛け数が上がった瞬間に、コンボの数字の下の「ダッシュ ×1.25」などが光るだけで、次にいつ上がるかが見えない
- コンボの数字の下（種類の掛け数の表示の下）に、**次の段階までの進み具合**の細いゲージを出す
  - 段階は掛け数の表（`ComboFactor` の 25・50・100・250・500・1000）。前の段階から次の段階までの割合（例: コンボ 37 なら 25→50 の間の 12/25）
  - 色は、次の段階の色（25 まではオレンジ `#FFAA00`、50 までは赤 `#FF5555`、100 までは紫 `#AA55FF`、250 まではピンク `#FF55FF`、500 までは水色 `#55FFFF`、1000 までは金 `#FFD700`）。今の「途切れるまでの残り時間のバー」（白）と、色と位置で見分けられるようにする
  - 段階に届いた瞬間は、今の掛け数の表示と一緒に光る。最後の段階（1000）より先は出さない
  - 出すのは、掛け数を使う種類（`HitKind.usesComboFactor`）の弱点が出ていて、掛け数の表示と同じ条件のとき
- 各自の設定 `comboFactorGaugeEnabled`（`client.combo`、初期値 true）で消せる
- 共通の計算（次の段階・割合・色）は `common/` に置き、単体テストを足す
- コンボの段階を 300 までに詰める相談（`doc/roadmap.md`。保留）が決まったら、段階の表が変わるだけで、このゲージはそのまま使える

## 5. 統計のリセットで「今回」も消す（ユーザーの判断）

- 今は「累計をリセット」（K キーの統計画面と `/weakspot reset`）が累計だけを消し、「今回」はワールドを出るまで残る。リセットの直後に、今回が累計より大きく見える
- リセットで**今回も 0 に**する（`ServerStats.resetTotal` で両方を消す）
- ボタンの名前を「統計をリセット」（英語 "Reset stats"）に、確認の文とコマンドの返事も合わせる
- 節目は今までどおり累計で数えるので、リセットすればもう一度受け取れる

## 6. 泳ぎの弱点を上下だけに（ユーザーの判断: 案 A）

- 出し方を、ダッシュ・はしごと同じ「上下だけ＋水平に戻す決まり」（`HudSpot.VERTICAL`）にする（今は照準のまわりのどこにでも出る `FREE`）
- 突進の向きは今のまま、当てた瞬間の視線の方向

## 7. 釣り: 浮きが水にないときは弱点を出さない（不具合の修正。ユーザーの判断: 案 A）

- 不具合: 水面ぎりぎりやブロックの縁に浮きが引っかかると、弱点は出続けるのに魚が来ない
- 原因: バニラの浮きは、一度水に入ると「浮いている状態」（`BOBBING`）のまま戻らないが、魚が来るまでの待ち時間を減らす処理（`catchingFish`）は、**浮きのいる位置のブロックが水のとき**（液体の高さ > 0）しか動かない。この Mod は「浮いている状態で待ち時間が残っている」だけで弱点を出し、当てても待ち時間を最小 1 までしか減らさない（0 にするとバニラが引き直すため）ので、1 から先に進まない
- 直し方: サーバーの「待っているか」の判定（`FishingHits.isWaiting`）に、バニラと同じ条件（浮きの位置のブロックが水で、液体の高さ > 0）を足す。満たさなければ、弱点を出さず、ヒットも受け付けない
- 知らせは出さない（ユーザーの確認のあとで判断: 弱点が出なければ投げ直せばよいため。最初は「2 秒続いたらチャットで 1 回」の予定だったが、最初から縁に乗った場合は待ち時間が始まらず当たらないなど、効き目が小さかった）

## 8. README・doc・ガイドの本・お知らせ

- `doc/config.md`: 表を 1 のまとめの順に並べ替える（キーの欄に、ファイルの中のカテゴリも書く。例 `server.mining.boostMultiplier`）。`comboFactorGaugeEnabled` を足す。設定画面の説明の書き方を書く
- `doc/play.md`: 泳ぎ（上下だけ）・照準周辺の弱点の位置・コンボ（ゲージ）・統計（リセット）・釣り（浮きが水にないとき）
- ガイドの本: 統計のリセットに触れている所があれば直す。泳ぎのページは、出る位置に触れていなければそのまま
- `doc/architecture.md`: 設定画面の組み立て（`WeakSpotGuiFactory`）、コンボのゲージ、釣りの判定
- `CHANGELOG.md`・README の「最近の更新」: 上の 1〜7。通信内容・保存データに変更なし（設定を 1 つ追加）。1.10.x 同士は接続可能
- 更新のお知らせ `weakspot.news.1.10.2`、`update.json`

## 9. `CLAUDE.md` に書くこと

- 現行を 1.10.2 に
- 設定の約束事に: 設定画面はファイルのカテゴリではなく、`WeakSpotGuiFactory` のまとめで出す。設定を足したら、まとめの表にも入れ、日英の名前（`weakspot.<カテゴリ>.<小文字のキー>`）も足す（テストが確かめる）。説明の先頭は、サーバーの項目なら「マルチプレイ時、サーバー側設定が優先されます。」

## 10. ユーザーに確認してもらうこと

- 設定画面が種類・用途のまとめで出て（コンボと節目は別）、泳ぎの設定と泳ぎのゲージの表示が同じ所にあること。`configVersion` と `lastSeenVersion` が出ないこと
- 項目の名前が日本語（英語の設定なら英語）で出て、はみ出していないこと。説明の最後にファイルのキーが出ること
- 設定画面で値を変えて閉じ、ゲームを再起動しても残っていること。シングルプレイでサーバーの項目（例: 採掘の倍率）を変えると、すぐ効くこと
- サーバーの項目の説明が「マルチプレイ時、サーバー側設定が優先されます。」で始まること
- コンボの数字の下に、次の段階までのゲージが出て、25・50・100 で光ること。設定で消せること
- 統計の「統計をリセット」で、今回も累計も 0 になること
- 泳ぎの弱点が照準の上下だけに出ること
- 釣りで、浮きを水面ぎりぎりのブロックの縁に引っかけると、弱点が出ないこと。普通に水に浮いているときは今までどおり
