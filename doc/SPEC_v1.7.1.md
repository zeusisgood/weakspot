# 弱点破壊 Mod 追加仕様書（v1.7.1）

`SPEC_v1.7.md`（Mod 1.7.0）に対する**パッチ**の仕様。ここに書かれていないことは、それと現行実装のままとする。
仕様書は、リポジトリの `doc/` ディレクトリに置く（この仕様書は `doc/SPEC_v1.7.1.md`）。
仕様と食い違う実装をする場合は、`CLAUDE.md` のリリースの流れ（止まる条件）に従う。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.7.0 がリリース済み
- この仕様書の内容は、Mod のバージョン **1.7.1** として、リリースする

---

## 0. バージョンと互換性

- バージョンは 1.7.0 → **1.7.1**（`build.gradle` の `version` と `WeakSpotMod.VERSION`）
- **クライアントだけの変更**。通信内容は変わらない（`SyncedSettings`・パケットは変えない）。1.7.x 同士は、そのまま接続できる。`ACCEPTED_VERSIONS` は `[1.7,1.8)` のまま
- 新しい設定は `[クライアント]` の 2 つだけ（§2.4）

## 1. 照準まわり・画面のマーカーの塗りが消えていたのを直す

### 1.1 不具合

- 照準まわりの弱点（弓・釣り・乗り物・食事・はしご・エリトラ・投げる物・走り）と、画面のマーカー（睡眠・エンチャント）が、塗りつぶしの丸ではなく、**輪郭だけの中空の丸**に見える（ユーザーの報告。採掘などブロックの弱点は塗りつぶしの丸）
- 「弱点」タブで輪・ひし形・四角を選んだときも、同じく塗りの部分が消える

### 1.2 原因

- HUD の図形（`ScreenProjection.fill` / `fillShape`）は、画面の上で時計回りに頂点を並べている。GUI の座標では、これが「裏向きの面」になる
- ブロックの弱点（`WeakSpotRenderer`）は描く前にカリング（裏向きの面を描かない）を切っているが、HUD の弱点（`HudSpot.beginOverlay`、`BowSpot`、`FishingSpot` の描く前の準備）は切っていない。そのため、塗り（`TRIANGLE_FAN` / `TRIANGLE_STRIP`）と中心の点が描かれず、輪郭の線（`LINE_LOOP`。カリングの対象外）だけが残る
- 1.6.x の弓・乗り物・食事でも同じだった

### 1.3 直し方

- HUD に図形を描く前の準備で**カリングを切り**（`GlStateManager.disableCull()`）、描いたあとで戻す（`enableCull()`）
  - `HudSpot.beginOverlay` / `endOverlay`（乗り物・食事・はしご・エリトラ・投げる物・走り・睡眠・エンチャント）
  - `BowSpot` と `FishingSpot` の、描く前の準備と後始末（それぞれ自分で GL の状態を整えている）
- 直したあとは、どのマーカーもブロックの弱点と同じ「半透明の塗り＋輪郭＋中心の点」になる（形を変えたときは、その形）

## 2. 更新のお知らせ（版が変わって初めてワールドに入ったとき）

### 2.1 出すとき

- **版が変わってから初めてワールドに入ったとき**に 1 回だけ、自分のチャットに出す（クライアントだけで判定する）
  - ソロでもマルチでも同じ。どのワールド・サーバーでも、1 回出したら、次の版までは出さない
  - 「最後に見た版」は、各自の設定 `lastSeenVersion` に覚える（§2.4）
- **初めて入れた人には出さない**（ガイドの本に任せる）。見分け方:
  - `lastSeenVersion` が空で、起動した時点で `config/weakspot.cfg` がまだなかった → 初めて入れた。何も出さずに、今の版を覚える
  - `lastSeenVersion` が空で、`weakspot.cfg` は前からあった → 1.7.0 以前からの更新。お知らせを出す
  - `weakspot.cfg` が前からあったかは、Mod のクラスを作るとき（Forge が設定ファイルを作るより前）に確かめて覚える
- 設定 `showUpdateNotes` が false なら出さない（「最後に見た版」は覚える）

### 2.2 中身（1 行の要約とリンク）

```
[弱点] 1.7.1 に更新: <要約>  [変更点を見る]
       不具合は /weakspot bug で報告の方法を案内します
```

- 1 行目: 金 `#FFAA00`（`TextFormatting.GOLD`。節目のチャットと同じ）。要約は翻訳キー `weakspot.news.<版>`（例 `weakspot.news.1.7.1`）。キーがない版では `weakspot.news.default`（「新しい版に更新しました」）
- `[変更点を見る]`: 水色 `#55FFFF`（`TextFormatting.AQUA`）に下線。クリックで GitHub の Release ページを開く（`https://github.com/zeusisgood/weakspot/releases/tag/v<版>`）
- 2 行目: 灰 `#AAAAAA`。`/weakspot bug` の部分は水色 `#55FFFF` に下線で、クリックするとチャット欄に `/weakspot bug` が入る（`ClickEvent.Action.SUGGEST_COMMAND`）
- 1.7.1 の要約: 「マーカーの塗りが消えていたのを直し、更新のお知らせと /weakspot bug を足した」 / "Fixed hollow weak spot markers; added update notes and /weakspot bug"

### 2.3 版ごとの要約

- リリースのたびに、`ja_jp.lang` と `en_us.lang` に `weakspot.news.<版>` を 1 行足す（日本語で 40 字くらいまで）。`CLAUDE.md` のリリースの流れに書く

### 2.4 設定（どちらも [クライアント]）

| キー | 初期値 | 説明 |
|---|---|---|
| `showUpdateNotes` | true | 版が変わって初めてワールドに入ったときに、チャットに更新のお知らせを出すか |
| `lastSeenVersion` | （空） | 最後にお知らせを見た版（Mod が書き換える。書き換えないでください） |

- `showUpdateNotes` の設定画面の説明（`weakspot.general.showupdatenotes.tooltip`）を両方の lang に足す。`lastSeenVersion` も説明を足す

## 3. `/weakspot bug`（不具合の報告の方法の案内）

### 3.1 仕組み（クライアントのコマンド）

- `/weakspot bug` は、**自分のクライアントが受け取って**、報告の方法を自分のチャットに出す（Forge の `ClientCommandHandler` に `weakspot` を登録する）
  - 理由: サーバーの `/weakspot` は OP 権限が要るので、ソロでチートがオフだと打てない。古いサーバー（1.7.0）にも無い
- `bug` 以外（`/weakspot`、`/weakspot stats …`、`reset`、`reload`）は、今までどおりサーバーに送る（打った文字をそのままチャットのパケットで送る）。サーバーのコマンドの動きは変わらない
- タブ補完: 1 つ目の引数に `bug` を出す（サーバーの `stats` / `reset` / `reload` の補完と合わさる）
- サーバーの使い方の表示（`weakspot.command.usage`）の末尾に「| /weakspot bug（不具合の報告の方法）」を足す

### 3.2 出す案内

```
[弱点] 不具合の報告の方法
 1. ここから報告できます: [GitHub の issue を開く]
 2. 書いてほしいこと: 何をしたか / 何が起きたか / どうなるはずだったか
 3. 今の環境（この画面を撮るか、書き写してください）
    Mod 1.7.1 / サーバーの Mod 1.7.1（ソロ） / Minecraft 1.12.2 / Forge 14.23.5.2860
 4. あると助かるもの: 落ちたときは crash-reports フォルダーの一番新しいファイル、
    落ちないときは logs/latest.log、画面の写真（F2）、ほかに入れている Mod の一覧
 5. 報告には GitHub のアカウントが要ります
```

- 見出しは金 `#FFAA00`、本文は白 `#FFFFFF`、補足（5）は灰 `#AAAAAA`
- `[GitHub の issue を開く]` は水色 `#55FFFF` に下線で、クリックで `https://github.com/zeusisgood/weakspot/issues/new` を開く
- 3 の環境は自動で埋める: Mod の版（`WeakSpotMod.VERSION`）、サーバーの Mod の版（`SyncedSettings.serverVersion`。ソロなら「（ソロ）」、マルチなら「（マルチ）」を付ける。まだ届いていなければ「不明」）、Minecraft の版、Forge の版（`ForgeVersion.getVersion()`）
- 文章は翻訳キー `weakspot.bug.*`（両方の lang）

## 4. README とガイドの本

- README: 「統計」の節の近くに「更新のお知らせ」と「不具合の報告（`/weakspot bug`）」の節を足す。設定表の `[クライアント]` に `showUpdateNotes` / `lastSeenVersion`。管理コマンドの表に `/weakspot bug`（誰でも使える。クライアントのコマンド）
- ガイドの本の 11 ページ目（キー）に「不具合は /weakspot bug で報告の方法が出ます」を足す（150 字くらいに収める）
- 「最新版」の行と「更新履歴」（クライアントだけの変更。1.7.x 同士は接続できる）、「開発」の節の仕様書へのリンク

## 5. `CLAUDE.md` に書くこと

- 仕様の一覧に、この仕様書を足す。現行のバージョンを 1.7.1 に
- HUD の図形は、描く前にカリングを切ること（`HudSpot.beginOverlay`、`BowSpot`、`FishingSpot`）。時計回りの頂点の並びなので、切らないと塗りが消える
- 更新のお知らせ（`client/UpdateNotes`。`lastSeenVersion`、初めて入れた人の見分け方）と、クライアントのコマンド `/weakspot bug`（`client/BugCommand`。`bug` 以外はサーバーへ送る）の説明
- リリースの流れに「`weakspot.news.<版>` の要約を両方の lang に 1 行足す」を足す

## 6. ユーザーに確認してもらうこと

- 走り・弓・乗り物・食事・はしご・エリトラ・投げる物の弱点と、睡眠・エンチャントのマーカーが、採掘の弱点と同じ塗りつぶしの丸に見えること。「弱点」タブで形を変えると、その形が塗られて見えること
- 1.7.0 から入れ替えて、最初にワールドに入ったときに、お知らせが 1 回だけ出ること。2 回目以降・別のワールドでは出ないこと。`[変更点を見る]` でブラウザーが開くこと。`/weakspot bug` をクリックするとチャット欄に入ること
- 新しく入れた環境（`config/weakspot.cfg` がない）では、お知らせが出ないこと
- ソロ（チートなし）とマルチの両方で `/weakspot bug` が使え、案内と今の環境が出ること。`[GitHub の issue を開く]` でブラウザーが開くこと
- `/weakspot stats <名前>` など、ほかの `/weakspot` が今までどおり動くこと（OP のとき）
